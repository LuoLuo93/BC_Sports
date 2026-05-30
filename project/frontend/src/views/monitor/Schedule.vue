<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="schedule-tabs">
      <el-tab-pane label="任务管理" name="job">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="任务名称">
          <el-input v-model="query.jobName" placeholder="请输入任务名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable >
            <el-option label="运行中" :value="1" />
            <el-option label="已暂停" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务模块">
          <el-select v-model="query.module" placeholder="全部" clearable >
            <el-option v-for="m in moduleOptions" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">定时任务管理</span>
          <el-button v-if="hasPermission('monitor:schedule:add')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增任务</el-button>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column prop="jobName" label="任务名称" min-width="160" />
            <el-table-column prop="beanName" label="Bean名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="cronExpression" label="Cron表达式" min-width="130" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'danger' : 'warning'" size="small">
                {{ row.status === 1 ? '运行中' : row.status === 0 ? '已暂停' : '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="任务模块" width="120" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.module" :type="moduleTagType(row.module)" size="small">{{ getModuleLabel(row.module) }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column prop="remark" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="300" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1 && hasPermission('monitor:schedule:edit')" type="warning" plain size="small" :disabled="runningJobs.has(row.id)" @click="handlePause(row)">暂停</el-button>
              <el-button v-if="row.status === 0 && hasPermission('monitor:schedule:edit')" type="success" plain size="small" :disabled="runningJobs.has(row.id)" @click="handleResume(row)">恢复</el-button>
              <el-button v-if="hasPermission('monitor:schedule:edit')" type="warning" plain size="small" :disabled="runningJobs.has(row.id)" @click="handleRun(row)">
                {{ runningJobs.has(row.id) ? '执行中...' : '执行一次' }}
              </el-button>
              <el-button v-if="hasPermission('monitor:schedule:edit')" type="primary" plain size="small" :disabled="runningJobs.has(row.id)" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('monitor:schedule:delete')" type="danger" plain size="small" :disabled="runningJobs.has(row.id)" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSearch" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新增任务'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <!-- 基本信息 -->
        <el-form-item label="任务名称" prop="jobName"><el-input v-model="form.jobName" placeholder="请输入任务名称" /></el-form-item>
        <el-form-item label="任务模块">
          <el-tag v-if="form.module" :type="moduleTagType(form.module)" effect="light" round>{{ getModuleLabel(form.module) }}</el-tag>
          <span v-else class="text-placeholder">选择预设任务后自动填充</span>
        </el-form-item>

        <el-form-item label="预设任务" prop="taskKey">
          <el-select v-model="form.taskKey" placeholder="请选择预设任务" filterable clearable class="w-full" @change="onTaskSelect">
            <el-option-group v-for="group in taskGroups" :key="group.module" :label="group.label">
              <el-option v-for="t in group.tasks" :key="t.taskKey" :label="t.name" :value="t.taskKey">
                <span>{{ t.name }}</span>
                <span style="float: right; color: var(--el-text-color-secondary); font-size: 12px">{{ t.description }}</span>
              </el-option>
            </el-option-group>
          </el-select>
        </el-form-item>

        <!-- 任务详情信息卡 -->
        <div v-if="selectedTask" class="task-info-card">
          <div class="task-info-row">
            <span class="task-info-label">Bean名称</span>
            <el-tag effect="plain" round>{{ selectedTask.beanName }}</el-tag>
          </div>
          <div class="task-info-row">
            <span class="task-info-label">方法名称</span>
            <el-tag type="info" effect="plain" round>{{ selectedTask.methodName }}()</el-tag>
          </div>
          <div v-if="selectedTask.description" class="task-info-row">
            <span class="task-info-label">任务说明</span>
            <span class="task-info-desc">{{ selectedTask.description }}</span>
          </div>
        </div>

        <el-divider content-position="left">执行配置</el-divider>

        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="如 0 0/5 * * * ?">
            <template #append>
              <el-button :icon="Check" @click="handleValidateCron" :loading="cronValidating">校验</el-button>
            </template>
          </el-input>
          <div v-if="cronNextTime" class="cron-hint">下次执行时间：{{ cronNextTime }}</div>
        </el-form-item>

        <el-form-item label="描述"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入描述" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="9999" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '确认创建' }}</el-button>
        </div>
      </template>
    </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="执行日志" name="log">
        <el-card shadow="never" class="search-card">
          <el-form :model="logQuery" inline>
            <el-form-item label="任务名称">
              <el-input v-model="logQuery.jobName" placeholder="请输入任务名称" clearable @keyup.enter="handleLogSearch" />
            </el-form-item>
            <el-form-item label="触发类型">
              <el-select v-model="logQuery.triggerType" placeholder="全部" clearable >
                <el-option label="CRON定时" value="CRON" />
                <el-option label="手动触发" value="MANUAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="执行状态">
              <el-select v-model="logQuery.status" placeholder="全部" clearable >
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
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">执行日志</span>
            </div>
          </template>
          <div class="table-responsive">
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
            <el-table-column prop="executeTime" label="执行时间" width="200" align="center" show-overflow-tooltip />
            <el-table-column label="耗时" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.duration != null">{{ row.duration < 1000 ? row.duration + 'ms' : (row.duration / 1000).toFixed(1) + 's' }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="errorMsg" label="异常信息" min-width="200" show-overflow-tooltip />
          </el-table>
          </div>

          <div class="pagination-wrapper">
            <el-pagination v-model:current-page="logQuery.pageNum" v-model:page-size="logQuery.pageSize" :total="logTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next, jumper" @size-change="handleLogSearch" @current-change="loadLogData" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 任务参数输入对话框 -->
    <el-dialog v-model="paramDialogVisible" title="任务参数" width="400px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item v-for="pd in currentParamDefs" :key="pd.key" :label="pd.label">
          <el-date-picker
            v-if="pd.type === 'date'"
            v-model="paramForm[pd.key]"
            type="datetime"
            placeholder="请选择日期时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
          <el-input v-else v-model="paramForm[pd.key]" :placeholder="'请输入' + pd.label" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paramDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRunWithParams">确认执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'Schedule' })
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getScheduleJobPage, getScheduleJob, createScheduleJob, updateScheduleJob, deleteScheduleJob, pauseScheduleJob, resumeScheduleJob, runScheduleJob, getScheduleTasks, validateCron, getScheduleLogPage, cleanScheduleLog, getScheduleRunStatus } from '@/api/schedule'
import { Plus, Search, RefreshRight, Check } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/constants'
import { useDictStore } from '@/stores/dict'

const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getScheduleJobPage, { jobName: '', status: undefined, module: '' })

const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const activeTab = ref('job')
const runningJobs = ref(new Set())
let runPollTimer = null
const formRef = ref(null)

const cronValidating = ref(false)
const cronNextTime = ref('')

const paramDialogVisible = ref(false)
const currentParamDefs = ref([])
const currentRunRow = ref(null)
const paramForm = reactive({})

const moduleOptions = ref([])

const taskOptions = ref([])
const selectedTask = ref(null)
const taskGroups = computed(() => {
  const groupMap = {}
  for (const t of taskOptions.value) {
    const mod = t.module || 'OTHER'
    if (!groupMap[mod]) groupMap[mod] = { module: mod, label: getModuleLabel(mod), tasks: [] }
    groupMap[mod].tasks.push(t)
  }
  return Object.values(groupMap).sort((a, b) => a.module.localeCompare(b.module))
})

const dictStore = useDictStore()

async function loadModuleOptions() {
  const data = await dictStore.loadDict('sys_schedule_module')
  moduleOptions.value = data.map(d => ({ value: d.dictValue, label: d.dictLabel }))
}

function getModuleLabel(val) {
  return moduleOptions.value.find(m => m.value === val)?.label || val
}

function moduleTagType(module) {
  const map = { IHR: '', QW: 'success', YDKL: 'warning', DEMO: 'info', OTHER: 'info' }
  return map[module] || 'info'
}

async function handleValidateCron() {
  if (!form.cronExpression) { ElMessage.warning('请先输入Cron表达式'); return }
  cronValidating.value = true
  try {
    const res = await validateCron(form.cronExpression)
    if (res.code === 200) { cronNextTime.value = res.data || ''; ElMessage.success('Cron表达式有效') }
    else { cronNextTime.value = '' }
  } catch { cronNextTime.value = '' } finally { cronValidating.value = false }
}

function onTaskSelect(taskKey) {
  if (!taskKey) {
    selectedTask.value = null
    form.module = ''
    return
  }
  const task = taskOptions.value.find(t => t.taskKey === taskKey)
  selectedTask.value = task || null
  if (task) {
    form.module = task.module
    form.remark = task.description
    if (task.sort != null) form.sort = task.sort
  }
}

async function loadTaskOptions() {
  const res = await getScheduleTasks()
  taskOptions.value = res.data || []
}

const defaultForm = () => ({ jobName: '', taskKey: '', cronExpression: '', sort: 0, remark: '' })
const form = reactive(defaultForm())
const rules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taskKey: [{ required: true, message: '请选择预设任务', trigger: 'change' }],
  cronExpression: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }]
}

const { loading: logLoading, tableData: logData, total: logTotal, query: logQuery, loadData: loadLogData, handleSearch: handleLogSearch, resetQuery: resetLogQuery } = usePageQuery(getScheduleLogPage, { jobName: '', triggerType: '', status: undefined })

async function handleCleanLog() {
  await ElMessageBox.confirm('确定清理 30 天前的执行日志？', '清理确认', { type: 'warning' })
  try { await cleanScheduleLog({ params: { keepDays: 30 } }); ElMessage.success('清理完成'); loadLogData() } catch { ElMessage.error('清理失败') }
}

watch(activeTab, (val) => { if (val === 'log') loadLogData() })

function handleAdd() { isEdit.value = false; editId.value = null; Object.assign(form, defaultForm()); selectedTask.value = null; cronNextTime.value = ''; dialogVisible.value = true }
async function handleEdit(row) {
  const res = await getScheduleJob(row.id)
  isEdit.value = true; editId.value = row.id
  Object.assign(form, res.data)
  if (form.taskKey) {
    selectedTask.value = taskOptions.value.find(t => t.taskKey === form.taskKey) || null
  } else {
    selectedTask.value = null
  }
  dialogVisible.value = true
}
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除任务「${row.jobName}」？`, '提示', { type: 'warning' })
  await deleteScheduleJob(row.id); ElMessage.success('删除成功'); loadData()
}
async function handlePause(row) { await pauseScheduleJob(row.id); ElMessage.success('已暂停'); loadData() }
async function handleResume(row) { await resumeScheduleJob(row.id); ElMessage.success('已恢复'); loadData() }
async function handleRun(row) {
  if (row.paramDefs && row.paramDefs.length > 0) {
    currentRunRow.value = row
    currentParamDefs.value = row.paramDefs
    for (const pd of row.paramDefs) {
      paramForm[pd.key] = ''
    }
    paramDialogVisible.value = true
    return
  }
  await doRun(row.id)
}

async function confirmRunWithParams() {
  const params = {}
  for (const pd of currentParamDefs.value) {
    if (paramForm[pd.key]) {
      params[pd.key] = paramForm[pd.key]
    }
  }
  paramDialogVisible.value = false
  await doRun(currentRunRow.value.id, params)
}

async function doRun(id, params) {
  runningJobs.value.add(id)
  try {
    await runScheduleJob(id, params)
    ElMessage.success('已触发执行')
    const row = tableData.value.find(r => r.id === id)
    startRunPoll(id, row?.jobName || id)
  } catch (e) {
    runningJobs.value.delete(id)
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

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) { await updateScheduleJob(editId.value, { ...form }) } else { await createScheduleJob({ ...form }) }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功'); dialogVisible.value = false; loadData()
  } finally { submitting.value = false }
}

async function restoreRunningState() {
  try {
    const res = await getScheduleRunStatus()
    const ids = res.data?.runningJobIds
    if (ids?.length) {
      for (const id of ids) {
        runningJobs.value.add(id)
        const row = tableData.value.find(r => String(r.id) === String(id))
        startRunPoll(id, row?.jobName || id)
      }
    }
  } catch {}
}

onMounted(() => { loadData().then(restoreRunningState); loadModuleOptions(); loadTaskOptions() })
onUnmounted(() => { if (runPollTimer) clearInterval(runPollTimer) })
</script>

<style scoped>
.schedule-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.task-info-card {
  background: linear-gradient(135deg, #f0f4ff 0%, #f5f3ff 100%);
  border: 1px solid rgba(29, 78, 216, 0.08);
  border-radius: 14px;
  padding: 16px 20px;
  margin-bottom: 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.task-info-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.task-info-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--bc-text-muted);
  min-width: 70px;
  flex-shrink: 0;
}
.task-info-desc {
  font-size: 13px;
  color: var(--bc-text);
}
.text-placeholder {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}
.cron-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-color-success);
}
.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
