# 伯俊ERP人员同步逻辑

> 对应调度任务：`bjerp.employee.sync`（伯俊ERP-人员同步）
> 入口：`ErpEmployeeSyncTask.syncAll()`
> 数据流：IHR 员工数据 → 伯俊ERP（表 12462）
> 状态表：`erp_employee_sync_status`（单表，按 `employee_id + sync_type` 区分，每员工每类型一行）

## 总览

该任务将 IHR 员工数据同步到伯俊 ERP，支持三类操作：

| 操作 | sync_type | 伯俊命令 | 说明 | 启用 |
|---|---|---|---|---|
| 入职 | `ONBOARDING` | `ObjectCreate` | 新增员工到 ERP | ✅ |
| 变更 | `UPDATE` | `ObjectModify` | REST v3：按工号查现值得记录 ID，`id`+`partial_update=true` 只更新差异字段 | ✅ |
| 离职 | `LEAVING` | `ObjectModify` | 同变更，参数带离职状态 | ⚠️ 已注释（后期改库） |

```
syncAll()  [synchronized + volatile 防重入]
├─ ① selectPendingOnboardings() → 逐个 syncOne("ONBOARDING")
├─ ② selectPendingUpdates()     → 逐个 syncOne("UPDATE")
└─ ③ selectPendingLeavings()    → ⚠️ 已注释，未启用（ERP离职接口暂未提供，后期改库）
```

---

## 与企微同步的关键区别

| 维度 | 企微同步 | ERP同步 |
|---|---|---|
| 待同步人员来源 | 按日期窗口（昨天+今天 / 3天） | **查有效状态 ∈ {未同步(0), 失败(2)} 的全部人员**（不限时间） |
| 状态表 | 3 张独立表 | **1 张表** `erp_employee_sync_status`，用 `sync_type` 区分 |
| 业务键 | 各类型不同列名 | `employee_id + sync_type` 联合 |
| 部门过滤 | 无 | **只同步"终端店铺"部门树下的人员**（递归 CTE 查子部门） |
| 离职 | 已实现 | 代码已写但被注释（后期直接改库） |
| 重复处理缓解 | 无（已跳过会再被捞） | **天然缓解**（待同步不含 3 已跳过） |

---

## 待同步人员怎么来（核心机制）

ERP 的待同步查询**不是按日期窗口**，而是查**有效同步状态 ∈ {未同步(0), 失败(2)}** 的全部终端店铺人员：

### 入职 `selectPendingOnboardings`
```sql
FROM employee_additions ea_latest (GROUP BY employees_id, MAX(sync_date))
JOIN employee_information ei
LEFT JOIN erp_employee_sync_status es ON ... AND es.sync_type = 'ONBOARDING'
WHERE effectiveStatus IN (0, 2)              -- 未同步 或 失败
  AND ei.staff_status != 'LEAVED'           -- 排除已离职
  AND ei.department_id IN (终端店铺部门树)   -- 只同步终端店铺
```

### 变更 `selectPendingUpdates`
```sql
FROM employee_modifications em_latest (GROUP BY staff_id, MAX(sync_date))
JOIN employee_information ei
LEFT JOIN erp_employee_sync_status es ON ... AND es.sync_type = 'UPDATE'
WHERE effectiveStatus IN (0, 2)              -- 未同步 或 失败
  AND NOT EXISTS (                           -- 去重：同日已入职的不重复处理
      SELECT 1 FROM employee_additions ea
      WHERE ea.employees_id = em_latest.staff_id
      AND CONVERT(DATE, ea.sync_date) = CONVERT(DATE, em_latest.sync_date)
  )
  AND ei.department_id IN (终端店铺部门树)
```

### 离职 `selectPendingLeavings`（已注释，未启用）
```sql
FROM employee_information ei
LEFT JOIN erp_employee_sync_status es ON ... AND es.sync_type = 'LEAVING'
WHERE ei.leave_date IS NOT NULL
  AND ISNULL(es.sync_status, 0) IN (0, 2)
  AND ei.department_id IN (终端店铺部门树)
```

> 🔑 三类都只同步**"终端店铺"部门树**下的人员（递归 CTE 查子部门），非终端店铺不进 ERP。

---

## 入职同步 `ONBOARDING`

### 逐员工处理流程
```
syncOne("ONBOARDING", employeeId, staffName, staffNo)
├─ 查 employee_information 详情 (selectById)
│   └─ 查不到 → markSyncSkipped(3) ⭐ 受状态不降级保护
├─ syncOnboarding(detail):
│   ├─ IhrToBjErpConverter.toCreateParams(detail)
│   │    字段: NO=工号, NAME=姓名, C_CUSTOMER_ID__NAME="边城体育"(固定),
│   │         C_STORE_ID__NAME=部门名(实时), INCUMBENCY_STS=在职状况,
│   │         ISSALER="Y"(营业员), BEGIN_DATE=入职日期, HANDSET=手机, EMAIL=邮箱
│   ├─ buildTransactions("ObjectCreate", data)  → 表 12462 新增命令
│   └─ bjErpApiClient.call(transactions)
├─ 成功 → markSyncSuccess(1, erpObjectId) ✅
├─ 报"已存在" → markSyncSkipped(3) ⭐ 受保护
└─ 其他异常 → markSyncFailed(2, 错误信息)
```

> 店仓 `C_STORE_ID__NAME` 取 IHR 员工部门名（实时），不再写死。经销商 `C_CUSTOMER_ID__NAME` 固定为 `"边城体育"`（IHR 无该字段）。营业员 `ISSALER='Y'`（伯俊 HR_EMPLOYEE 表：Y=营业员2745人/N=非营业员880人）。不传部门字段 `C_DEPARTMENT_ID__NAME`（伯俊 C_DEPARTMENT 表为空，传了会报"部门不存在"）。

---

## 变更同步 `UPDATE`（REST v3：id 定位 + partial_update 最小化发送）

> **协议变更史（2026-10-16 排查定案）**：伯俊 2026-08-18 11:12 升级 REST v3，`ObjectModify` 的 `ak` 定位参数废弃（AD_TABLE.AK_COLUMN_ID 同期从工号列 25983 改为姓名列 25982，备份表 AD_TABLE_BK 可证），官方现行以 `id`(记录数字主键, Number) 定位并新增 `partial_update`(Boolean, 必传 true，否则未传字段被 ERP 清空——实测验证)。ak 自 8-19 起对所有人报"未找到对象"，旧逻辑降级 ObjectCreate 撞工号唯一索引报"已存在"被标跳过(3)静默丢失，8-19~10-16 共 451 人变动未落 ERP。官方文档接口（免登录）：`http://bos.bc-sports.com.cn:8876/bos/p/doc/v3/restapi/table?tableId=14630&operation=Modify`

### 逐员工处理流程
```
syncOne("UPDATE", employeeId, staffName, staffNo)
├─ 查 employee_information 详情 (selectById)
│   └─ 查不到 → markSyncSkipped(3) ⭐ 受状态不降级保护
├─ syncUpdate(detail):
│   ├─ bjerpEmployeeMapper.selectByStaffNo(工号) → HR_EMPLOYEE 现值快照(ID/姓名/店仓/状态/手机/营业员/店长)
│   ├─ 查到 → doModify: ObjectModify {table, id=记录ID, partial_update:true} + 只发差异字段
│   │    · NAME: 姓名不同才发
│   │    · C_STORE_ID__NAME: 仅调店时发（先校验目标店名在 C_STORE 精确存在，
│   │      不存在→失败(2)提示"IHR部门名[x]在ERP店仓不存在(疑似店名漂移或新店未建)"等人工核正；
│   │      店名未变不发 → 淮南/济南类漂移不阻塞手机号/状态等其他变动）
│   │    · INCUMBENCY_STS: 仅 ERP=D(离职) 而 IHR 在职/试用时拉回（ERP 罕见码 E 保守不动）
│   │    · ISSALER: 现值非 Y 时拉齐为 Y
│   │    · ISSTOREMAN: 职位=店长→Y 否则空串，与现值不同才发
│   │    · HANDSET: IHR 非空且不同才发；IHR 为空不碰 ERP 已有手机号
│   │    · 全部无差异 → 不调 ERP 直接成功，日志请求体记"(无差异,未调用ERP)"
│   └─ 查不到（ERP 无此工号）→ 兜底：降级 syncOnboarding 用 ObjectCreate 创建；
│       创建报"已存在"→重查一次现值，查到转走修改；仍查不到→失败上抛(2)
├─ 成功 → markSyncSuccess(1, erpId) ✅（修改响应无 objectid，用现值查询的 ID 回填 erp_object_id）
├─ 报"已存在"且为入职(ONBOARDING) → markSyncSkipped(3) ⭐ 幂等防重（变更不再走跳过，防静默丢失）
└─ 其他异常 → markSyncFailed(2, 错误信息)
```

> 入职用 `ObjectCreate`（协议未变，NO=工号）；变更用 `ObjectModify`（id 定位 + partial_update=true 只更新差异字段）。变更不传部门字段 `C_DEPARTMENT_ID__NAME`（伯俊 C_DEPARTMENT 表为空，传了会报"部门不存在"）。

### 变更兜底：工号在 ERP 不存在时自动创建

`syncUpdate` 先按工号查 HR_EMPLOYEE 现值，查不到（真新人/ERP漏建）降级复用入职创建（`toCreateParams` + `ObjectCreate`），与企微变动同步"查不到人自动入职"的行为对齐：

| 降级创建结果 | 最终状态 |
|---|---|
| ObjectCreate 成功 | 已同步(1)，日志请求体可见 ObjectCreate 命令 |
| 创建报"已存在"（工号带空格/竞态等） | 重查现值命中 → 转走修改；仍查不到 → 失败(2)（不再静默跳过） |
| 创建报其他错误 | 失败(2)，携带创建的请求/响应体 |

- 批量 `syncAll` 与手动 `syncSingle` 都走 `syncUpdate`，兜底同时生效。
- 状态自愈：兜底建人后该员工的 ONBOARDING 行可能仍是未同步(0)，下一轮入职同步对其 ObjectCreate 会报"已存在"→标已跳过(3)，无需人工处理。

---

## 离职同步 `LEAVING`（⚠️ 已注释未启用）

### 处理逻辑（代码已写，syncAll 中被注释）
```
syncOne("LEAVING", employeeId, staffName, staffNo)
├─ 查 employee_information 详情
│   └─ 查不到 → markSyncSkipped(3) ⭐ 受保护
├─ syncLeaving(detail):
│   ├─ IhrToBjErpConverter.toLeavingParams(detail, erpId)
│   │    字段: id=记录ID(定位), partial_update=true, INCUMBENCY_STS="离职", END_DATE=离职日期
│   ├─ buildTransactions("ObjectModify", data)  → 表 14630 id 定位 + 改离职状态
│   └─ bjErpApiClient.call(transactions)
├─ 成功 → markSyncSuccess(1, erpObjectId) ✅
├─ 报"已存在" → markSyncSkipped(3) ⭐ 受保护
└─ 其他异常 → markSyncFailed(2, 错误信息)
```

> 离职暂未启用（ERP离职接口暂未提供），后期可能直接改库。手动单条 `syncSingle("LEAVING")` 会返回"ERP离职同步接口暂未提供"。

---

## 统一的异常处理（syncOne + syncSingle 共用）

```
catch (Exception e):
├─ 错误信息含"已存在"？(BjErpApiClient.isAlreadyExists)
│   └─ 是 → markSyncSkipped(3)   ⭐ 受状态不降级保护（已是已同步则不覆盖）
└─ 否 → markSyncFailed(2, 错误信息)
```

| 错误类型 | 处理 | 典型场景 |
|---|---|---|
| "已存在"（编号重复） | 标已跳过(3) | 入职时 ERP 已有该工号 |
| "已存在"（编号冲突） | 入职：幂等防重标跳过(3)；变更：重查现值转修改，查不到标失败(2) | 入职重复提交；变更=工号在 ERP 但按工号查不到现值（罕见） |
| "IHR部门名[x]在ERP店仓不存在" | 变更：失败(2)，等人工核正店名漂移/建新店 | 调店目标店名与 ERP C_STORE 漂移（如淮南/济南） |
| 其他业务错误 | 标失败(2) | 接口报错 |

> 入职/变更/离职的单条推送（列表"同步ERP"按钮）和批量推送（syncAll）**都走同一个 `syncSingle`/`syncOne` 方法**，异常处理逻辑完全一致。

---

## 状态机制

### 状态表 `erp_employee_sync_status`（单表，单行覆盖）
按 `employee_id + sync_type` 联合键 MERGE，**每个员工每个类型一行**（最多 3 行：入职/变更/离职各一），不保留历史轨迹。

| 字段 | 说明 |
|---|---|
| `id` | 自增主键 |
| `employee_id + sync_type` | 联合业务键，MERGE 去重 |
| `sync_type` | `ONBOARDING` / `UPDATE` / `LEAVING` |
| `staff_name` / `staff_no` | 姓名、工号 |
| `sync_status` | 0/1/2/3，每次覆盖 |
| `erp_object_id` | 伯俊返回的记录 ID，供后续修改定位 |
| `sync_time` | 每次覆盖成最新时间 |
| `error_message` | 失败时覆盖，成功/跳过时覆盖成对应值 |
| `create_time` / `update_time` | 创建/更新时间 |

### 状态码定义

| 码值 | 含义 | 写入场景 |
|---|---|---|
| `null` / `0` | 未同步 | 从未同步过 / 被过期重置（同步时间早于业务日期） |
| `1` | 已同步 | ERP 创建/修改成功 |
| `2` | 失败 | ERP 接口报错（非已存在） |
| `3` | 已跳过 | 查不到员工详情 / 编号已存在 |

### 过期重置逻辑（显示层 effectiveSyncStatus）

页面查询时，状态表里的 `sync_status` 经 CASE 重新计算：**同步时间早于业务日期 → 强制显示为"未同步(0)"**。

| 类型 | 过期判断基准 | 说明 |
|---|---|---|
| 入职 | `sync_time < enroll_in_date`（入职日期） | 同步早于入职日 → 未同步 |
| 变更 | `sync_time < sync_date`（最近修改日） | 变动语义=有新修改需重做，故用推数日期 |
| 离职 | 无（`ISNULL(sync_status, 0)`） | 未启用 |

> **历史问题**：入职此前误用 `sync_time < sync_date`（IHR 推数日期）作为基准。IHR 数据重推会刷新 `sync_date` 为今天，导致已同步记录被强制重置成未同步 → 重新进入待同步队列 → 重跑 ObjectModify。现已改回业务日期 `enroll_in_date`。

### 状态不降级保护

`markSyncSkipped` 写状态前先查当前状态，若已是"已同步(1)"则不覆盖，直接返回：

```java
public void markSyncSkipped(String syncType, String employeeId, String staffName, String staffNo) {
    Integer current = mapper.selectSyncStatusByKey(employeeId, syncType);  // 查当前状态
    if (current != null && current == 1) {                                 // 已是"已同步(1)"
        return;                                                            // ⭐ 不覆盖，直接返回
    }
    // ... 否则正常写入"已跳过(3)"
}
```

| 调用 | 是否保护 | 说明 |
|---|---|---|
| `markSyncSkipped(3)` 已跳过 | ✅ **保护** | 已同步(1)不会被降级成已跳过 |
| `markSyncSuccess(1)` 成功 | ❌ 正常覆盖 | 真成功要更新 sync_time 和 erp_object_id |
| `markSyncFailed(2)` 失败 | ❌ 正常覆盖 | 真失败要记录错误 |

---

## 入职 vs 变更 vs 离职 对照

| 维度 | 入职 ONBOARDING | 变更 UPDATE | 离职 LEAVING |
|---|---|---|---|
| 数据源 | `employee_additions` | `employee_modifications` | `employee_information` |
| 伯俊命令 | `ObjectCreate`（新增，NO=工号） | `ObjectModify`（id 定位 + partial_update 差异更新） | `ObjectModify`（id 定位 + 改离职状态） |
| 参数转换 | `IhrToBjErpConverter.toCreateParams` | `IhrToBjErpConverter.toModifyParams` | `IhrToBjErpConverter.toLeavingParams` |
| 过期基准 | `enroll_in_date`（入职日期） | `sync_date`（最近修改日） | 无 |
| 额外过滤 | 排除已离职 `staff_status != 'LEAVED'` | 去重（同日已入职不重复处理） | — |
| 经销商 | `"边城体育"`（固定） | `"边城体育"`（固定） | 无 |
| 店仓 `C_STORE_ID__NAME` | IHR 部门名（实时） | IHR 部门名（实时） | 无 |
| 营业员 `ISSALER` | `"Y"` | `"Y"` | 无 |
| 部门 `C_DEPARTMENT_ID__NAME` | ❌ 不传（伯俊表空） | ❌ 不传（伯俊表空） | 无 |
| 会报"已存在" | ✅ 会（编号重复） | ❌ Modify 不会；降级创建时可能（撞上已有编号） | ❌ 不会 |
| 启用状态 | ✅ 启用 | ✅ 启用 | ⚠️ 已注释（后期改库） |

---

## 相关文件

| 类型 | 文件 |
|---|---|
| 任务 | `task/erp/ErpEmployeeSyncTask.java` |
| Service | `service/impl/ErpEmployeeSyncServiceImpl.java` |
| Mapper 接口 | `ihrmapper/ErpEmployeeSyncStatusMapper.java` |
| Mapper XML | `mapper/ihr/ErpEmployeeSyncStatusMapper.xml` |
| 状态实体 | `entity/ihr/ErpEmployeeSyncStatus.java` |
| ERP API 客户端 | `task/erp/BjErpApiClient.java` |
| 字段转换器 | `task/erp/IhrToBjErpConverter.java`（IHR 字段 → 伯俊参数） |
| Controller | `controller/ErpEmployeeController.java`（页面查询 + 同步触发） |
| 调度注册 | `task/ScheduleTaskRegistry.java:78` |
