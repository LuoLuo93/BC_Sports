# 伯俊ERP人员同步逻辑

> 对应调度任务：`bjerp.employee.sync`（伯俊ERP-人员同步）
> 入口：`ErpEmployeeSyncTask.syncAll()`
> 数据流：IHR 员工数据 → 伯俊ERP（表 12462）

## 总览

该任务将 IHR 员工数据同步到伯俊 ERP，支持三类操作：

| 操作 | sync_type | 伯俊命令 | 说明 |
|---|---|---|---|
| 入职 | `ONBOARDING` | `ObjectCreate` | 新增员工到 ERP |
| 变更 | `UPDATE` | `ObjectModify` | 用工号 ak 定位记录，增量更新字段 |
| 离职 | `LEAVING` | `ObjectModify` | 同变更，参数带离职状态 |

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

ERP 的待同步查询**不是按日期窗口**，而是查**有效同步状态 ∈ {未同步(0), 失败(2)}** 的全部人员：

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

---

## 入职同步 `syncOne("ONBOARDING", ...)`

### 逐员工处理流程
```
syncOne("ONBOARDING", employeeId, staffName, staffNo)
├─ 查 employee_information 详情 (selectById)
│   └─ 查不到 → markSyncSkipped(3) ⭐ 受状态不降级保护
├─ syncOnboarding(detail):
│   ├─ IhrToBjErpConverter.toCreateParams(detail)  → 转换 IHR 字段为伯俊参数
│   ├─ buildTransactions("ObjectCreate", data)     → 表 12462, 新增命令
│   └─ bjErpApiClient.call(transactions)           → 调伯俊 ERP
│       └─ 失败(resp 非成功) → throw RuntimeException(错误信息)
│       └─ 成功 → 返回 erpObjectId(伯俊记录 ID)
├─ 成功 → markSyncSuccess(1, erpObjectId) ✅
└─ 异常 → markSyncFailed(2, 错误信息)
```

---

## 变更同步 `syncOne("UPDATE", ...)`

### 逐员工处理流程
```
syncOne("UPDATE", employeeId, staffName, staffNo)
├─ 查 employee_information 详情 (selectById)
│   └─ 查不到 → markSyncSkipped(3) ⭐ 受状态不降级保护
├─ syncUpdate(detail):
│   ├─ IhrToBjErpConverter.toModifyParams(detail)  → 转换 IHR 字段为伯俊参数
│   ├─ buildTransactions("ObjectModify", data)     → 表 12462, 工号 ak 定位 + 增量更新
│   └─ bjErpApiClient.call(transactions)           → 调伯俊 ERP
│       └─ 失败 → throw RuntimeException(错误信息)
│       └─ 成功 → 返回 erpObjectId
├─ 成功 → markSyncSuccess(1, erpObjectId) ✅
└─ 异常 → markSyncFailed(2, 错误信息)
```

> 入职用 `ObjectCreate`（新增），变更用 `ObjectModify`（用工号 `ak` 定位记录，增量更新字段）。区别只在 command 和参数转换器不同。

---

## 状态表 `erp_employee_sync_status`

**一张表**用 `sync_type` 区分，按 `employee_id + sync_type` 联合键 MERGE 覆盖，**每个员工每个类型一行**（最多 3 行：入职/变更/离职各一），不保留历史轨迹。

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
| `2` | 失败 | ERP 接口报错 |
| `3` | 已跳过 | 查不到员工详情等"软跳过" |

---

## 过期重置逻辑（显示层 effectiveSyncStatus）

页面查询时，状态表里的 `sync_status` 经 CASE 重新计算：**同步时间早于业务日期 → 强制显示为"未同步(0)"**（表示员工数据在那之后变更过，需重新同步）。

| 类型 | 过期判断基准 | 说明 |
|---|---|---|
| 入职 | `sync_time < enroll_in_date`（入职日期） | 同步早于入职日 → 未同步 |
| 变更 | `sync_time < sync_date`（最近修改日） | 变动语义=有新修改需重做，故用推数日期 |
| 离职 | 无（`ISNULL(sync_status, 0)`） | 未启用 |

> **历史问题**：入职此前误用 `sync_time < sync_date`（IHR 推数日期）作为基准。IHR 数据重推会刷新 `sync_date` 为今天，导致已同步记录被强制重置成未同步 → 重新进入待同步队列 → 重跑 ObjectModify。现已改回业务日期 `enroll_in_date`。

---

## 状态不降级保护

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

> ERP 的 skip 只在"查不到员工详情"时触发，触发频率低，但同样加了保护以防万一。

---

## 入职 vs 变更 对照

| 维度 | 入职 ONBOARDING | 变更 UPDATE |
|---|---|---|
| 数据源 | `employee_additions` | `employee_modifications` |
| 伯俊命令 | `ObjectCreate`（新增） | `ObjectModify`（工号 ak 定位 + 增量更新） |
| 参数转换 | `IhrToBjErpConverter.toCreateParams` | `IhrToBjErpConverter.toModifyParams` |
| 过期基准 | `enroll_in_date`（入职日期） | `sync_date`（最近修改日） |
| 额外过滤 | 排除已离职 `staff_status != 'LEAVED'` | 去重（同日已入职不重复处理） |
| 部门范围 | 终端店铺部门树 | 终端店铺部门树 |

两者其余逻辑（待同步筛选、syncOne 分发、状态写入、不降级保护）完全一致。

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
| 调度注册 | `task/ScheduleTaskRegistry.java:78` |
