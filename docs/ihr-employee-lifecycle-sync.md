# 企微员工生命周期同步逻辑

> 对应调度任务：`qywx.employee.lifecycle.sync`（QW-员工生命周期整合同步）
> 入口：`QywxEmployeeLifecycleSyncTask.syncAll()`

## 总览

该任务是**编排器（Orchestrator）**，按固定顺序串行执行 4 个子任务，每个子任务独立 try/catch，单个失败不中断后续，但任一失败最终会抛出异常：

```
syncAll()
├─ ① QywxFullSyncTask.syncAll()         同步企微基础信息
├─ ② QywxNewEmployeeSyncTask.sync()     入职：创建企微通讯录成员
├─ ③ QywxEmployeeUpdateSyncTask.sync()  变动：更新企微通讯录信息
└─ ④ QywxEmployeeLeaveSyncTask.sync()   离职：删除企微通讯录成员
```

> 注：三个子任务（入职/变动/离职）在调度表里也有独立注册，但默认被注释（`ScheduleTaskRegistry.java:50-52`），生产环境统一走编排器 `syncAll`。

---

## 数据来源与时间窗口

三个子任务**都从 IHR 增量表按日期窗口捞人**，不查询同步状态表来决定"谁还没同步"。状态表只写不读（仅页面查询时读取展示）。

| 子任务 | 数据来源 | 时间窗口 | 查询方法 |
|---|---|---|---|
| 入职 | `employee_additions`（IHR 入职增量表） | 昨天 + 今天（2 天） | `additionMapper.selectBySyncDates` |
| 变动 | `employee_modifications`（IHR 变动增量表） | 昨天 + 今天（2 天） | `modificationMapper.selectBySyncDates` |
| 离职 | `employee_information`（`leave_date IS NOT NULL`） | 今天往前 3 天 | `employeeDetailMapper.selectRecentLeaved` |

入职/变动拿到 `employees_id`/`staff_id` 列表后，按批次（每批 100）查询 `employee_information` 详情再逐个处理。

---

## 入职同步 `QywxNewEmployeeSyncTask.sync()`

### 预处理
1. 先同步企微部门（失败不中断，用现有部门数据）
2. 预加载入职排除名单（`exclusionMapper.selectActiveExclusions(1)`）→ 组成 `姓名|工号` Set 用于 O(1) 查找

### 逐员工处理流程

```
对每个员工 employee：
├─ ① 手机号为空 / 为 "null"？
│     └─ 是 → markSyncSkipped(3)「无手机号」
├─ ② 在入职排除名单 (姓名|工号)？
│     └─ 是 → markSyncSkipped(3)「排除列表」
├─ ③ 企微已存在该手机号？(apiClient.getUserIdByMobile)
│     └─ 是 → markSyncSkipped(3)「已在企微」⭐ 受状态不降级保护
├─ ④ 匹配部门 ID (resolveDepartId：按部门名 → 多同名走祖先链消歧 → 兜底 1997)
├─ ⑤ 调用企微创建用户 (createUser)
│     ├─ errcode=0            → markSyncSuccess(1) ✅
│     ├─ errcode=60102(已存在) → markSyncSkipped(3) ⭐ 受保护
│     └─ 其他 errcode         → markSyncFailed(2, "errcode=x, errmsg")
└─ 异常 → markSyncFailed(2, 异常信息)
```

---

## 变动同步 `QywxEmployeeUpdateSyncTask.sync()`

### 预处理
1. 预加载变动排除名单（`selectActiveExclusions(1)`）
2. 预加载 `工号 → 企微userid` 映射

### 逐员工处理流程

```
对每个员工 employee：
├─ ① 在排除名单 (姓名|工号)？
│     └─ 是 → markSyncSkipped(3) ⭐ 受保护
├─ ② 查 userid：先用工号映射，查不到兜底用手机号查企微
├─ ③ userid 为空（企微不存在该员工）？
│     └─ 走「自动入职」：匹配部门 → 创建用户
│         ├─ errcode=0 或 60102 → markSyncSuccess(1) ✅
│         └─ 其他 → markSyncFailed(2, "自动入职失败:...")
├─ ④ userid 存在：匹配部门 → 调用企微更新用户 (updateUser)
│     └─ errcode=60106(邮箱被占用)？→ 去掉邮箱重试一次
│         ├─ errcode=0 → markSyncSuccess(1) ✅
│         └─ 其他 → markSyncFailed(2, "errcode=x, errmsg")
└─ 异常 → markSyncFailed(2, 异常信息)
```

### 去重
显示层 SQL 会排除「同一天既在 `employee_additions` 又在 `employee_modifications`」的员工，避免入职+变动重复处理。

---

## 离职同步 `QywxEmployeeLeaveSyncTask.sync()`

### 预处理
1. 预加载离职排除名单（`selectActiveExclusions(2)`，类型 2）
2. 预加载 `工号 → 企微userid` 映射
3. 预加载 `userid → 企微姓名` 映射（用于姓名校验）

### 逐员工处理流程

```
对每个离职员工 employee：
├─ ① 在离职排除名单 (姓名|工号)？
│     └─ 是 → markSyncSkipped(3) ⭐ 受保护
├─ ② 工号为空 / 无效？
│     └─ 是 → markSyncSkipped(3) ⭐ 受保护
├─ ③ 按工号查不到企微 userid？
│     └─ 是 → markSyncSkipped(3)「未找到企微userid」⭐ 受保护
├─ ④ 姓名+工号双重校验：企微姓名 ≠ IHR姓名？
│     └─ 是 → markSyncSkipped(3)「姓名不匹配」⭐ 受保护（安全门）
├─ ⑤ 校验通过，调用企微删除用户 (deleteUser)
│     ├─ errcode=0 → markSyncSuccess(1) ✅
│     ├─ errcode=60111/46004(用户已不存在) → markSyncSkipped(3) ⭐ 受保护
│     └─ 其他 → markSyncFailed(2, "errcode=x, errmsg")
└─ 异常 → markSyncFailed(2, 异常信息)
```

---

## 状态表与状态码

三个子任务各自对应一张状态表，**每个员工只有一行**，同步时通过 SQL `MERGE` 覆盖更新（按业务键存在则 UPDATE，不存在则 INSERT），不会新增多行。

| 子任务 | 状态表 | 业务键列 | MERGE 语句 |
|---|---|---|---|
| 入职 | `ihr_employee_onboarding_status` | `employees_id` | `upsertByEmployeesId` |
| 变动 | `ihr_employee_update_status` | `staff_id` | `upsertByStaffId` |
| 离职 | `ihr_employee_leaving_status` | `employee_id` | `upsertByEmployeeId` |

### 状态码定义

| 码值 | 含义 | 写入场景 |
|---|---|---|
| `null` / `0` | 未同步 | 从未同步过 / 被过期重置（同步时间早于业务日期） |
| `1` | 已同步 | 企微创建/更新/删除成功（errcode=0） |
| `2` | 失败 | 企微 API 返回非预期错误码 |
| `3` | 已跳过 | 无手机号/排除名单/企微已存在/姓名不匹配等"软跳过" |

### 状态表字段

| 字段 | 说明 |
|---|---|
| `id` | 自增主键 |
| `employees_id` / `staff_id` / `employee_id` | 业务键（员工的 IHR id），MERGE 按它去重 |
| `staff_name` / `staff_no` | 姓名、工号 |
| `sync_status` | 1/2/3，每次覆盖 |
| `sync_time` | 每次覆盖成最新时间 |
| `error_message` | 失败时覆盖，成功/跳过时覆盖成对应值 |
| `create_time` / `update_time` | 创建/更新时间 |

> 单行覆盖意味着**不保留同步历史轨迹**。当前需求只要求"状态正确不降级"，无需历史留痕。

---

## 过期重置逻辑（显示层 `effectiveSyncStatus`）

页面查询时，状态表里的 `sync_status` 不会原样返回，而是经过 `effectiveSyncStatus` CASE 重新计算：**如果同步时间早于业务日期，强制显示为"未同步(0)"**（表示员工数据在那之后变更过，需要重新同步）。

| 子任务 | 过期判断基准 | 说明 |
|---|---|---|
| 入职 | `sync_time < enroll_in_date`（入职日期） | 同步早于入职日 → 未同步 |
| 变动 | `sync_time < sync_date`（最近修改日） | 变动语义=有新修改需重做，故用推数日期 |
| 离职 | `sync_time < leave_date`（离职日期） | 同步早于离职日 → 未同步 |

> **历史问题**：入职此前误用 `sync_time < sync_date`（IHR 推数日期）作为基准。IHR 数据重推会刷新 `sync_date` 为今天，导致已同步记录被强制重置成未同步 → 任务重跑发现企微已存在 → 覆盖成已跳过。现已改回业务日期。

---

## 状态不降级保护

所有标 ⭐ 的 `markSyncSkipped(3)` 调用点，在 service 层统一加了同一道保护：**写状态前先查当前状态，若已是"已同步(1)"则不覆盖，直接返回**。

```java
public void markSyncSkipped(String id, String staffName, String staffNo) {
    Integer current = mapper.selectSyncStatusByKey(id);  // 查当前状态
    if (current != null && current == 1) {               // 已是"已同步(1)"
        return;                                           // ⭐ 不覆盖，直接返回
    }
    // ... 否则正常写入"已跳过(3)"
}
```

| 调用 | 是否保护 | 说明 |
|---|---|---|
| `markSyncSkipped(3)` 已跳过 | ✅ **保护** | 已同步(1)不会被降级成已跳过 |
| `markSyncSuccess(1)` 成功 | ❌ 正常覆盖 | 真成功要更新 sync_time |
| `markSyncFailed(2)` 失败 | ❌ 正常覆盖 | 真失败要记录错误 |

---

## 相关文件

| 类型 | 文件 |
|---|---|
| 编排器 | `task/qywx/QywxEmployeeLifecycleSyncTask.java` |
| 入职任务 | `task/qywx/QywxNewEmployeeSyncTask.java` |
| 变动任务 | `task/qywx/QywxEmployeeUpdateSyncTask.java` |
| 离职任务 | `task/qywx/QywxEmployeeLeaveSyncTask.java` |
| 入职 service | `service/impl/IhrEmployeeOnboardingServiceImpl.java` |
| 变动 service | `service/impl/IhrEmployeeUpdateServiceImpl.java` |
| 离职 service | `service/impl/IhrEmployeeLeavingServiceImpl.java` |
| 入职 mapper | `mapper/ihr/IhrEmployeeOnboardingStatusMapper.xml` |
| 变动 mapper | `mapper/ihr/IhrEmployeeUpdateStatusMapper.xml` |
| 离职 mapper | `mapper/ihr/IhrEmployeeLeavingStatusMapper.xml` |
| 调度注册 | `task/ScheduleTaskRegistry.java:49` |
