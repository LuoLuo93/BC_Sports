# 旧版HK ERP直写模块移植说明

> 本模块把源项目 `interfaceForHK` 的「HR → 旧版HK ERP」职员同步能力**原样移植**到 BC体育数据管理系统，作为独立模块与现有的「伯俊 ERP HTTP API」链路并存。

## ⭐ 数据源切换说明（重要更新）

**初始移植**阶段，HK 链路的人员名单来源与源项目一致，直读 HKERP 库的人员表（`Bas_Personnel_Base_enrollIn` 等）。

**当前版本已重构**：HK 链路的人员数据**完全切换到 `BC_SPORTS_IHR` 库**（与伯俊链路共用同一份员工数据），并应用统一的部门过滤：

| 维度 | 切换前（初始移植） | 切换后（当前版本） |
|---|---|---|
| 人员数据来源 | HKERP 库 `Bas_Personnel_Base_enrollIn`/`_leave`/`NeedUpdateUser` | **BC_SPORTS_IHR 库** `employee_additions`/`employee_modifications`/`employee_information` |
| 部门过滤 | 无（全量） | **终端店铺递归**（`department` 表 CTE，与伯俊链路一致） |
| 排除名单 | 无 | **复用** `ihr_employee_exclusion`（type=1 入职 / type=2 离职） |
| 同步状态记录 | 无（仅 findPersonnel 去重） | **新建独立表** `hk_erp_sync_status`（HK_ 前缀，含"数据更新后重置未同步"机制） |
| HKERP 库职责 | 人员来源 + 写入 + 映射 | **仅写入**（`Bas_Personnel`）+ 映射查询（`Bas_DepartMent`/`Bas_Shop`）+ token |

**切换原因**：统一全系统的员工数据口径，避免两条链路因读不同库导致人员名单不一致；并让 HK 链路也享受伯俊链路那套"终端店铺过滤 + 排除名单 + 同步状态追踪"的成熟机制。

**同步类型常量**：`HK_ONBOARDING` / `HK_UPDATE` / `HK_LEAVING`（在 `hk_erp_sync_status.sync_type` 字段，与伯俊链路的 `ONBOARDING`/`UPDATE`/`LEAVING` 隔离）。

**跨数据源事务**：Service 层用编程式事务分别绑定 `ihrTransactionManager`（读人员/读写同步状态）和 `hkerpTransactionManager`（写 `Bas_Personnel`/查映射），不能用单一 `@Transactional`。

### ⭐ Token 逻辑已删除

经核实，源项目 `interfaceForHK` 的 token 逻辑（向 `hkitcloud.net` 取 `access_token` 存 `HK_access_token` 表）是**只写不读的死代码**——全项目没有任何地方读取该 token 用于鉴权。HK ERP 链路的实际鉴权是 **SQL Server 数据库账号密码**（JDBC 连接 `HKERP` 库，配置在 `spring.datasource.hkerp`），与 token 无关。

因此已删除全部 token 相关代码：
- `task/hkerp/HkTokenTask.java`
- `hkerpmapper/HkTokenMapper.java` + `mapper/hkerp/HkTokenMapper.xml`
- `entity/hkerp/HkAccessToken.java`
- `ScheduleTaskRegistry` 的 `hkerp.token.refresh` 任务注册
- `HkPersonnelSyncController` 的 `/refresh-token` 接口
- `application.yml` 的 `hkerp.token` 配置
- 前端 `api/hk-personnel.js` 的 `refreshHkToken`

> 下方历史章节中关于 token / `HK_access_token` / `hkerp.token.refresh` 的描述均已失效，仅作为初始移植版本的记录保留。


> 下方为初始移植版本的完整说明，业务规则（入职/变更/离职的字段映射、PersonnelID 生成、二次入职、满30天离职收尾等）仍然适用。

---



## 一、模块定位与背景

BC体育数据管理系统存在**两条独立的 ERP 人员同步链路**：

| 维度 | 伯俊 ERP 链路（已有） | 旧版 HK ERP 链路（本模块，移植自 interfaceForHK） |
|---|---|---|
| 数据目的地 | 伯俊 ERP（表 14630，HTTP API） | SQL Server `HKERP` 库（直写 `Bas_Personnel` 等表） |
| 写入方式 | `BjErpApiClient` 调 REST API | MyBatis-Plus 直连数据库 |
| HR 数据来源 | iHR360 OpenAPI | HKERP 库内的入职/离职/变更表 |
| 入口类 | `task/erp/ErpEmployeeSyncTask` | `task/hkerp/HkPersonnelSyncTask` |
| 数据源 | `bjerp`（Oracle） | `hkerp`（SQL Server） |
| 菜单 | ERP员工管理 | HK ERP同步 |

两条链路**互不影响**，可同时运行。若旧版 HKERP 已不再使用，可只保留伯俊链路；本模块的代码独立，删除时只需移除 `hkerp`/`hkerpmapper`/`task/hkerp` 包及相关配置。

## 二、数据源配置

`application.yml` 新增 `spring.datasource.hkerp`：

```yaml
spring:
  datasource:
    hkerp:
      url: jdbc:sqlserver://${HKERP_DB_HOST:10.10.0.25}:${HKERP_DB_PORT:1433};DatabaseName=${HKERP_DB_NAME:HKERP};...
      username: ${HKERP_DB_USERNAME:sa}
      password: ${HKERP_DB_PASSWORD:BCsport)(*&.}
```

- 配置类：`config/HkErpDataSourceConfig.java`（仿 `IhrDataSourceConfig`）
- Mapper 扫描包：`com.bcsport.admin.hkerpmapper`
- XML 位置：`classpath*:mapper/hkerp/*.xml`
- 事务管理器 Bean：`hkerpTransactionManager`
- 密码通过环境变量 `HKERP_DB_PASSWORD` 覆盖，**不写死明文**（修复源项目明文密码问题）

## 三、涉及的数据库表（HKERP 库）

| 表名 | 用途 | 读/写 |
|---|---|---|
| `Bas_Personnel` | ERP 职员资料主表（核心写入表） | 读写 |
| `Bas_Personnel_Base_enrollIn` | HR 入职员工表（数据源） | 只读 |
| `Bas_Personnel_Base_leave` | HR 离职员工表（数据源） | 只读 |
| `NeedUpdateUser` | 待变更员工表（数据源） | 只读 |
| `Bas_DepartMent` | ERP 部门表 | 只读 |
| `Bas_Shop` | ERP 店铺表（ShopID/StockID/SportCityID） | 只读 |
| `HK_access_token` | HK Token 存储 | 读写 |

## 四、业务逻辑（与源项目保持一致）

### 1. 新员工入职同步 `syncOnboarding`
源项目原 cron：`0 30 11,12,18 * * ?`

流程：
1. 查询近 7 天新入职员工（`Bas_Personnel_Base_enrollIn`）
2. 按 姓名+手机号 判断是否已在职（`AllowUsed=1`）→ 跳过
3. 二次入职判断（`AllowUsed=0` 的旧记录数 > 0）→ 修改在职状态 + 调整到新部门/店铺/仓库
4. 全新员工 → 生成新 PersonnelID（基于 `Max(PersonnelID)` 递增）+ 部门映射 + 店铺映射 → 批量 insert
5. 销售人员（有店铺信息）自动开启收银员/营业员/仓管员等角色标识

### 2. 员工变更与离职同步 `syncUpdate`
源项目原 cron：`0 10 12,6,0 * * ?`

流程：
1. **信息变更**：查 `NeedUpdateUser`（7 天内），更新手机号、部门、ShopID/StockID
2. **离职处理（重点）**：查 `Bas_Personnel_Base_leave`（7 天内），将 `AllowUsed=0`、`PersonnelStatus=2`、更新 `ModifyDTM`
3. **离职收尾**：查 `AllowUsed=0` 且 `PersonnelStatus=0` 且 修改满 30 天 的记录，改为离职状态
   > ⚠️ **与源项目的差异**：源项目此段代码被注释掉，本模块**启用**它，作为离职流程的收尾环节（满30天自动转离职）。

### 3. HK Token 刷新 `refresh`
向 `hkitcloud.net` 取 `access_token`，先清空 `HK_access_token` 表再写入。

## 五、定时任务注册

在 `ScheduleTaskRegistry` 注册了 3 个任务（模块常量 `MODULE_HKERP = "HKERP"`）：

| taskKey | 名称 | bean.method |
|---|---|---|
| `hkerp.token.refresh` | HKERP-刷新Token | `hkTokenTask.refresh` |
| `hkerp.onboarding.sync` | HKERP-新员工入职同步 | `hkPersonnelSyncTask.syncOnboarding` |
| `hkerp.update.sync` | HKERP-员工变更与离职同步 | `hkPersonnelSyncTask.syncUpdate` |

> cron 不写死在 `@Scheduled` 注解里，而是通过「运维监控 > 定时任务」页面的 `schedule_job` 表配置，符合本系统动态调度约定。建议 cron 沿用源项目（见上方各任务）。

## 六、手动触发

### 方式一：前端页面
菜单「HK ERP同步」（路径 `/hkerp/personnel-sync`，需执行 `sql/init_hkerp_menu.sql` 初始化菜单）。页面提供：
- 新员工入职同步 按钮
- 员工变更与离职同步 按钮
- 刷新 HK Token 按钮
- 最近同步结果展示（含统计明细）

### 方式二：HTTP API
```
POST /bcsports/api/hk-personnel/sync-onboarding    # 入职同步
POST /bcsports/api/hk-personnel/sync-update        # 变更离职同步
POST /bcsports/api/hk-personnel/refresh-token      # 刷新Token
GET  /bcsports/api/hk-personnel/sync-status        # 查询同步状态
```
权限：`hk:personnel:sync`

### 方式三：定时任务页面
「运维监控 > 定时任务」中新增任务，选择上述预设任务并配置 cron。

## 七、文件清单

### 新增文件（后端 14）
```
src/main/java/com/bcsport/admin/
├── config/HkErpDataSourceConfig.java
├── entity/hkerp/
│   ├── HkBasPersonnel.java          (Bas_Personnel)
│   ├── HkEmployeeInformation.java   (Bas_Personnel_Base_enrollIn)
│   ├── HkBasPersonnelLeave.java     (Bas_Personnel_Base_leave)
│   ├── HkNeedUpdateUser.java        (NeedUpdateUser)
│   ├── HkShopStockSportCity.java    (Bas_Shop)
│   ├── HkBasDepartment.java         (Bas_DepartMent)
│   └── HkAccessToken.java           (HK_access_token)
├── hkerpmapper/
│   ├── HkBasPersonnelMapper.java
│   └── HkTokenMapper.java
├── service/HkPersonnelSyncService.java
├── service/impl/HkPersonnelSyncServiceImpl.java
├── task/hkerp/
│   ├── HkPersonnelSyncTask.java
│   └── HkTokenTask.java
├── controller/HkPersonnelSyncController.java
└── vo/HkSyncStatsVO.java

src/main/resources/mapper/hkerp/
├── HkBasPersonnelMapper.xml
└── HkTokenMapper.xml
```

### 新增文件（前端 2）
```
frontend/src/api/hk-personnel.js
frontend/src/views/hkerp/PersonnelSync.vue
```

### 新增文件（SQL/文档 2）
```
sql/init_hkerp_menu.sql
docs/旧版HK_ERP直写模块移植说明.md   (本文档)
```

### 改动文件（3）
- `application.yml` — 新增 `spring.datasource.hkerp` + `hkerp.token` 配置
- `task/ScheduleTaskRegistry.java` — 新增 `MODULE_HKERP` + 3 个任务注册
- `controller/AuthController.java` — `vuePage()` 路由枚举增加 `/hkerp/personnel-sync`
- `frontend/src/router/index.js` — 增加 `hkerp/personnel-sync` 前端路由

## 八、源项目已知 bug 的修复（移植时一并修正）

源项目 `interfaceForHK` 存在以下问题，本模块已修复：

1. **XML 开头多余字符**：`BasPersonnelDao.xml` 第 5 行有游离的 `fv` 字符 → 已移除
2. **错误的 namespace**：源 XML namespace 写成 `dao.com.wangluo.learnJava8.BasPersonnelDao`（与 Java 包不符）→ 改为 `com.bcsport.admin.hkerpmapper.HkBasPersonnelMapper`
3. **错误的 resultType**：源 XML 用 `entity.com.wangluo.learnJava8.*` 路径 → 改为正确的全限定类名
4. **明文数据库密码**：`application.yml` 明文 `BCsport)(*&.` → 改用 `${HKERP_DB_PASSWORD}` 环境变量
5. **Token query 的 SQL 问题**：源 `tokenDao.xml` 的 `query` 用 `select *` 返回 String（MyBatis 无法整行映射）→ 改为 `select access_token`
6. **type-aliases-package 错误**：源项目 `application.yml` 写成 `com.ihr.interfaceforhk`（实际是 `com.hk.interfaceforhk`）→ 本模块直接用全限定类名，不依赖别名

## 九、启用步骤

1. **数据库**：执行 `sql/init_hkerp_menu.sql`（Oracle，幂等）初始化菜单和权限
2. **配置**：设置环境变量 `HKERP_DB_USERNAME` / `HKERP_DB_PASSWORD`（或修改 `application.yml`）
3. **后端**：重新编译启动（`mvn compile`）
4. **前端**：进入 `frontend/` 执行 `npm run build`（产物输出到 `src/main/resources/static`）
5. **定时任务**（可选）：在「运维监控 > 定时任务」配置 3 个 HKERP 任务的 cron

## 十、注意事项

- 本模块**不改动**伯俊 ERP 链路任何代码，两条链路完全独立
- 若 HKERP 库的 `Bas_Personnel` 表结构与源项目时期有变化，需相应调整 `HkBasPersonnel` 实体和 XML
- 离职收尾（满30天）逻辑在源项目被注释，本模块启用——如不希望启用，注释 `HkPersonnelSyncServiceImpl#syncPersonnelUpdate` 的第 3 段即可
- PersonnelID 生成逻辑依赖 `Max(PersonnelID)` + 序号递增，并发场景下可能冲突（源项目同样存在此问题），建议避免同时多次触发入职同步（已加互斥锁）
