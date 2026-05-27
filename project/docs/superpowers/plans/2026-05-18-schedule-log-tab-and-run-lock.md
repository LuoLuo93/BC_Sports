# 定时任务日志页签 + 执行防重 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在定时任务管理页新增「执行日志」页签展示执行历史，并强化手动执行时的防重复触发体验（按钮禁用 + 实时状态轮询 + 自动刷新）。

**Architecture:** 纯前端改动。后端 API 已完备（`/api/schedule/log/page`、`/api/schedule/run-status`、`/api/schedule/log/clean`），只需在 `Schedule.vue` 中用 `el-tabs` 拆分为「任务管理」和「执行日志」两个 tab，并在手动执行时增加防重 UI 逻辑。

**Tech Stack:** Vue 3 + Element Plus + 已有 schedule.js API

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `frontend/src/views/monitor/Schedule.vue` | **Modify** | 加入 el-tabs、日志表格、日志搜索、清理功能、执行防重 UI |
| `frontend/src/api/schedule.js` | No change | 已有 `getScheduleLogPage`、`cleanScheduleLog`、`getScheduleRunStatus` |
| Backend | No change | 所有 API 和防重逻辑已完备 |

---

### Task 1: 用 el-tabs 包裹现有内容，新增「执行日志」页签骨架

**Files:**
- Modify: `frontend/src/views/monitor/Schedule.vue`

- [ ] **Step 1: 在 `<template>` 中用 `el-tabs` 包裹现有内容**

将现有的搜索栏 + 任务表格 + 弹窗整体包裹在 `el-tabs` 中，新增日志 tab 骨架。

在 `<div class="page-container">` 内部、搜索栏之前插入 `el-tabs`，将搜索栏到弹窗的内容移入第一个 tab-pane，新增第二个 tab-pane：

```html
<el-tabs v-model="activeTab" class="schedule-tabs">
  <el-tab-pane label="任务管理" name="job">
    <!-- 原有搜索栏 + 表格 + 弹窗 全部移到这里 -->
  </el-tab-pane>
  <el-tab-pane label="执行日志" name="log">
    <!-- 日志内容将在后续步骤中添加 -->
  </el-tab-pane>
</el-tabs>
```

具体操作：在 `<div class="page-container">` 下，第 4 行 `<el-card shadow="never" class="search-card">` 之前插入：

```html
    <el-tabs v-model="activeTab" class="schedule-tabs">
      <el-tab-pane label="任务管理" name="job">
```

在第 132 行 `</el-dialog>` 之后、`</div>` (page-container) 之前关闭 tab-pane 并添加日志 tab：

```html
      </el-tab-pane>

      <el-tab-pane label="执行日志" name="log">
        <!-- 日志内容待填充 -->
      </el-tab-pane>
    </el-tabs>
```

- [ ] **Step 2: 在 script 中添加 `activeTab` ref**

在 `const editId = ref(null)` 之后添加：

```js
const activeTab = ref('job')
```

- [ ] **Step 3: 在 style 中添加 tabs 样式**

在 `<style scoped>` 末尾添加：

```css
.schedule-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
```

- [ ] **Step 4: 验证页面能正常渲染，两个 tab 可切换**

---

### Task 2: 实现执行日志页签内容

**Files:**
- Modify: `frontend/src/views/monitor/Schedule.vue`

- [ ] **Step 1: 在日志 tab-pane 中添加搜索栏和表格**

替换 `<!-- 日志内容待填充 -->` 为：

```html
        <el-card shadow="never" class="search-card">
          <el-form :model="logQuery" inline>
            <el-form-item label="任务名称">
              <el-input v-model="logQuery.jobName" placeholder="请输入任务名称" clearable @keyup.enter="handleLogSearch" />
            </el-form-item>
            <el-form-item label="触发类型">
              <el-select v-model="logQuery.triggerType" placeholder="全部" clearable class="w-120">
                <el-option label="CRON定时" value="CRON" />
                <el-option label="手动触发" value="MANUAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="执行状态">
              <el-select v-model="logQuery.status" placeholder="全部" clearable class="w-120">
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleLogSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetLogQuery">重置</el-button>
              <el-button v-if="hasPermission('monitor:schedule:delete')" type="danger" plain @click="handleCleanLog">清理日志</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <el-table v-loading="logLoading" :data="logData" border stripe empty-text="暂无执行日志">
            <el-table-column prop="jobName" label="任务名称" min-width="150" />
            <el-table-column label="触发类型" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="row.triggerType === 'MANUAL' ? 'warning' : 'info'" size="small">{{ row.triggerType === 'MANUAL' ? '手动触发' : 'CRON定时' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="执行状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executeTime" label="执行时间" width="170" />
            <el-table-column label="耗时" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.duration != null">{{ row.duration < 1000 ? row.duration + 'ms' : (row.duration / 1000).toFixed(1) + 's' }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="errorMsg" label="异常信息" min-width="200" show-overflow-tooltip />
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination v-model:current-page="logQuery.pageNum" v-model:page-size="logQuery.pageSize" :total="logTotal" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next, jumper" @size-change="handleLogSearch" @current-change="loadLogData" />
          </div>
        </el-card>
```

- [ ] **Step 2: 在 script 中添加日志相关的 API 导入、数据和函数**

在 import 行添加 `getScheduleLogPage`、`cleanScheduleLog`：

```js
import { getScheduleJobPage, getScheduleJob, createScheduleJob, updateScheduleJob, deleteScheduleJob, pauseScheduleJob, resumeScheduleJob, runScheduleJob, getScheduleTasks, validateCron, getScheduleLogPage, cleanScheduleLog } from '@/api/schedule'
```

在 `const activeTab = ref('job')` 之后添加日志数据：

```js
const logLoading = ref(false)
const logData = ref([])
const logTotal = ref(0)
const logQuery = reactive({ jobName: '', triggerType: '', status: undefined, pageNum: 1, pageSize: 10 })
```

在 `resetQuery` 函数之后添加日志函数：

```js
async function loadLogData() {
  logLoading.value = true
  try { const res = await getScheduleLogPage(logQuery); logData.value = res.data?.records || []; logTotal.value = res.data?.total || 0 } finally { logLoading.value = false }
}

function handleLogSearch() { logQuery.pageNum = 1; loadLogData() }
function resetLogQuery() { logQuery.jobName = ''; logQuery.triggerType = ''; logQuery.status = undefined; logQuery.pageNum = 1; loadLogData() }

async function handleCleanLog() {
  await ElMessageBox.confirm('确定清理 30 天前的执行日志？', '清理确认', { type: 'warning' })
  await cleanScheduleLog({ params: { keepDays: 30 } })
  ElMessage.success('清理完成')
  loadLogData()
}
```

- [ ] **Step 3: 切换到日志 tab 时自动加载数据**

用 `watch` 监听 `activeTab`，在 `onMounted` 之前添加：

```js
import { ref, reactive, computed, watch, onMounted } from 'vue'
```

在 `onMounted` 之前添加：

```js
watch(activeTab, (val) => { if (val === 'log') loadLogData() })
```

- [ ] **Step 4: 验证日志 tab 搜索、分页、清理功能正常**

---

### Task 3: 手动执行防重复触发 — 按钮禁用 + 实时状态轮询 + 自动刷新

**Files:**
- Modify: `frontend/src/views/monitor/Schedule.vue`

后端已有完整的防重机制：`ScheduleConfig.executeJobImmediately()` 用 `runningJobIds.add()` 原子操作阻止重复执行，Controller 层 `isJobRunning()` 二次拦截。现在在前端增强体验：点击执行后禁用按钮、轮询状态、完成后自动刷新。

- [ ] **Step 1: 在 API 导入中添加 `getScheduleRunStatus`**

在 import 行中添加 `getScheduleRunStatus`：

```js
import { ..., getScheduleLogPage, cleanScheduleLog, getScheduleRunStatus } from '@/api/schedule'
```

- [ ] **Step 2: 添加运行中任务追踪状态**

在 `const activeTab = ref('job')` 之后添加：

```js
const runningJobs = ref(new Set())
let runPollTimer = null
```

- [ ] **Step 3: 修改操作列中「执行一次」按钮，加入禁用逻辑**

将第 58 行的执行按钮：

```html
<el-button v-if="hasPermission('monitor:schedule:run')" type="primary" link size="small" @click="handleRun(row)">执行一次</el-button>
```

替换为：

```html
<el-button v-if="hasPermission('monitor:schedule:run')" type="primary" link size="small" :disabled="runningJobs.has(row.id)" @click="handleRun(row)">
  {{ runningJobs.has(row.id) ? '执行中...' : '执行一次' }}
</el-button>
```

- [ ] **Step 4: 改写 `handleRun` 函数，增加状态追踪和轮询**

将现有的 `handleRun` 函数：

```js
async function handleRun(row) { await runScheduleJob(row.id); ElMessage.success('已触发执行') }
```

替换为：

```js
async function handleRun(row) {
  runningJobs.value.add(row.id)
  try {
    await runScheduleJob(row.id)
    ElMessage.success('已触发执行')
    startRunPoll(row.id, row.jobName)
  } catch (e) {
    runningJobs.value.delete(row.id)
  }
}

function startRunPoll(jobId, jobName) {
  if (runPollTimer) clearInterval(runPollTimer)
  runPollTimer = setInterval(async () => {
    try {
      const res = await getScheduleRunStatus({ params: { jobId } })
      if (!res.data?.running) {
        clearInterval(runPollTimer)
        runPollTimer = null
        runningJobs.value.delete(jobId)
        loadData()
        if (activeTab.value === 'log') loadLogData()
        ElMessage.success(`任务「${jobName}」执行完成`)
      }
    } catch {
      clearInterval(runPollTimer)
      runPollTimer = null
      runningJobs.value.delete(jobId)
    }
  }, 2000)
}
```

- [ ] **Step 5: 在 onMounted 中清理轮询定时器**

将 `onMounted` 改为：

```js
onMounted(() => { loadData(); loadModuleOptions(); loadTaskOptions() })
```

在 `onMounted` 之后添加清理：

```js
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
```

```js
onUnmounted(() => { if (runPollTimer) clearInterval(runPollTimer) })
```

- [ ] **Step 6: 验证防重效果**

1. 点击「执行一次」→ 按钮变为「执行中...」并禁用
2. 再次点击 → 按钮仍禁用，无法重复触发
3. 等待执行完成 → 按钮恢复「执行一次」，任务列表自动刷新
4. 切换到日志 tab → 能看到新产生的执行记录

---

## Verification

1. 打开 `/monitor/schedule` 页面，确认有两个 tab：「任务管理」「执行日志」
2. 任务管理 tab 功能不受影响（新增、编辑、删除、暂停、恢复）
3. 切换到执行日志 tab，能看到日志列表，搜索/分页正常
4. 清理日志功能弹出确认框，确认后清理 30 天前日志
5. 点击「执行一次」→ 按钮立即变为「执行中...」禁用态
6. 执行期间无法重复点击
7. 执行完成后按钮自动恢复，列表自动刷新，日志 tab 也有新记录
