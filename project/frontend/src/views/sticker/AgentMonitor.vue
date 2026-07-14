<template>
  <div class="page-container">
    <!-- Agent 监控表格 -->
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="Agent 名称">
          <el-input v-model="query.agentName" placeholder="搜索名称" clearable style="min-width:140px;max-width:180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="min-width:100px;max-width:120px">
            <el-option label="在线" :value="1" />
            <el-option label="离线" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="handleReset">重置</el-button>
          <el-button @click="refreshAll">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">Agent 监控面板</span>
          <div class="header-actions">
            <div style="display:flex;gap:16px">
              <el-statistic title="在线" :value="onlineCount" />
              <el-statistic title="离线" :value="offlineCount" />
            </div>
          </div>
        </div>
      </template>
      <div class="table-responsive">
        <el-table v-loading="loading" :data="filteredData" border stripe>
          <el-table-column prop="agentId" label="Agent ID" width="120" />
          <el-table-column prop="agentName" label="名称" width="130" show-overflow-tooltip />
          <el-table-column prop="ipAddress" label="IP 地址" width="140" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="printers" label="打印机" min-width="140" show-overflow-tooltip />
          <el-table-column label="最后心跳" width="180">
            <template #default="{ row }">{{ formatTime(row.lastHeartbeat) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button type="primary" plain size="small" @click="viewTasks(row)">任务记录</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          @size-change="() => { query.pageNum = 1; loadData() }"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 任务记录弹窗 -->
    <el-dialog v-model="taskDialogVisible" :title="`${currentAgent} - 打印任务`" width="1300px" class="task-dialog">
      <div class="task-filter-bar">
        <span class="task-filter-label">批次查询</span>
        <el-input
          v-model="taskQuery.batchId"
          placeholder="输入批次号（支持前缀）"
          clearable
          size="small"
          style="width:240px"
          @keyup.enter="onBatchSearch"
          @clear="onBatchSearch"
        />
        <el-button type="primary" size="small" @click="onBatchSearch">查询</el-button>
        <el-button v-if="taskQuery.batchId" size="small" link type="primary" @click="clearBatch">清除</el-button>
      </div>
      <el-table :data="taskList" border size="small" :max-height="taskTableMaxHeight">
        <el-table-column label="任务ID" width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.taskId }}</span>
            <el-tag v-if="row.isReprint === 1" type="warning" size="small" effect="plain" style="margin-left:4px">补</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="批次" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.batchId" class="batch-tag clickable" @click="filterByBatch(row.batchId)" title="点击筛选该批次">{{ row.batchId.substring(0, 8) }}</span>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="申请单号" width="180" show-overflow-tooltip />
        <el-table-column prop="materialName" label="货品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sizeName" label="尺码" width="70" align="center" />
        <el-table-column prop="printQty" label="数量" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.status === 3 && row.errorMsg" :content="row.errorMsg" placement="top">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </el-tooltip>
            <el-tag v-else :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="打印时间" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatTime(row.printTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain size="small" @click="viewTaskDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper--sm" v-if="taskTotal > 0">
        <el-pagination
          v-model:current-page="taskQuery.pageNum"
          v-model:page-size="taskQuery.pageSize"
          :total="taskTotal"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="() => { taskQuery.pageNum = 1; loadTasks() }"
          @current-change="loadTasks"
        />
      </div>
    </el-dialog>

    <!-- 任务详情弹窗 -->
    <el-dialog v-model="taskDetailVisible" title="任务详情" width="860px">
      <template v-if="currentTask">
        <!-- 顶部状态栏 -->
        <div class="detail-header">
          <div class="detail-header-left">
            <span class="detail-task-id">{{ currentTask.taskId || '-' }}</span>
            <el-tag :type="statusTagType(currentTask.status)" size="small" effect="dark" class="detail-status-tag">{{ statusLabel(currentTask.status) }}</el-tag>
            <el-tag v-if="currentTask.isReprint === 1" type="warning" size="small" effect="plain">补打</el-tag>
          </div>
          <el-button v-if="currentTask.status === 2 || currentTask.status === 3"
            type="warning" size="small" @click="openReprint">补打任务</el-button>
        </div>

        <!-- 信息区域：左右两列 -->
        <div class="detail-body">
          <!-- 左列 -->
          <div class="detail-col">
            <!-- 基本信息 -->
            <div class="detail-section">
              <div class="detail-section-title">基本信息</div>
              <div class="detail-field-grid">
                <div class="detail-field" v-if="currentTask.batchId">
                  <span class="detail-label">打印批次</span>
                  <span class="detail-value mono">{{ currentTask.batchId }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">申请单号</span>
                  <span class="detail-value">{{ currentTask.orderNo || '-' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">Agent ID</span>
                  <span class="detail-value mono">{{ currentTask.agentId || '-' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">打印机</span>
                  <span class="detail-value">{{ currentTask.printerName || '-' }}</span>
                </div>
              </div>
            </div>

            <!-- 补打信息 -->
            <div class="detail-section" v-if="currentTask.isReprint === 1">
              <div class="detail-section-title">补打信息</div>
              <div class="detail-field-grid">
                <div class="detail-field">
                  <span class="detail-label">原任务ID</span>
                  <span class="detail-value mono">{{ currentTask.sourceTaskId || '-' }}</span>
                </div>
                <div class="detail-field" v-if="currentTask.reprintReason">
                  <span class="detail-label">补打原因</span>
                  <span class="detail-value">{{ currentTask.reprintReason }}</span>
                </div>
              </div>
            </div>

            <!-- 货品信息 -->
            <div class="detail-section">
              <div class="detail-section-title">货品信息</div>
              <div class="detail-field-grid">
                <div class="detail-field">
                  <span class="detail-label">货品名称</span>
                  <span class="detail-value">{{ currentTask.materialName || '-' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">货号</span>
                  <span class="detail-value">{{ currentTask.materialNumber || '-' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">款号</span>
                  <span class="detail-value">{{ currentTask.styleNumber || '-' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">品牌</span>
                  <span class="detail-value">{{ currentTask.brandName || '-' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">类别</span>
                  <span class="detail-value">{{ currentTask.kindName || '-' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">颜色</span>
                  <span class="detail-value">{{ currentTask.color || '-' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">尺码</span>
                  <span class="detail-value">{{ currentTask.sizeName || '-' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 右列 -->
          <div class="detail-col">
            <!-- 打印信息 -->
            <div class="detail-section">
              <div class="detail-section-title">打印信息</div>
              <div class="detail-field-grid">
                <div class="detail-field">
                  <span class="detail-label">打印数量</span>
                  <span class="detail-value">{{ currentTask.printQty ?? '-' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">重试次数</span>
                  <span class="detail-value">{{ currentTask.retryCount ?? 0 }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">模板文件</span>
                  <span class="detail-value">{{ currentTask.templateFile || '-' }}</span>
                </div>
              </div>
            </div>

            <!-- 时间信息 -->
            <div class="detail-section">
              <div class="detail-section-title">时间信息</div>
              <div class="detail-timeline">
                <div class="timeline-item" :class="{ 'timeline-active': currentTask.createTime }">
                  <span class="timeline-label">创建时间</span>
                  <span class="timeline-value">{{ formatTime(currentTask.createTime) || '-' }}</span>
                </div>
                <div class="timeline-item" :class="{ 'timeline-active': currentTask.dispatchTime }">
                  <span class="timeline-label">派发时间</span>
                  <span class="timeline-value">{{ formatTime(currentTask.dispatchTime) || '-' }}</span>
                </div>
                <div class="timeline-item" :class="{ 'timeline-active': currentTask.printTime }">
                  <span class="timeline-label">打印时间</span>
                  <span class="timeline-value">{{ formatTime(currentTask.printTime) || '-' }}</span>
                </div>
              </div>
            </div>

            <!-- 错误信息 -->
            <div class="detail-section" v-if="currentTask.errorMsg">
              <div class="detail-section-title" style="color:#dc2626">错误信息</div>
              <div class="detail-error-box">{{ currentTask.errorMsg }}</div>
            </div>
          </div>
        </div>

        <!-- 打印数据 JSON（可折叠） -->
        <div class="detail-json-section">
          <div class="detail-json-header" @click="jsonExpanded = !jsonExpanded">
            <span class="detail-section-title" style="margin:0">打印数据 (JSON)</span>
            <el-icon class="detail-json-arrow" :class="{ expanded: jsonExpanded }"><ArrowDown /></el-icon>
          </div>
          <transition name="el-zoom-in-top">
            <pre v-show="jsonExpanded" class="detail-json-pre">{{ formatPrintData(currentTask.printData) }}</pre>
          </transition>
        </div>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="taskDetailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 补打 · 选择 Agent -->
    <el-dialog v-model="reprintDialogVisible" title="补打 · 选择 Agent" width="720px" :close-on-click-modal="false">
      <p style="margin-bottom:12px;color:#606266;font-size:13px">
        选择一个在线的 Agent 下发补打任务（共 <b>{{ currentTask?.printQty ?? 0 }}</b> 张）
      </p>
      <el-table :data="reprintAgentList" border size="small">
        <el-table-column label="选择" width="55" align="center">
          <template #default="{ row }">
            <el-radio v-model="reprintAgentId" :value="row.agentId" :disabled="row.status !== 1">&nbsp;</el-radio>
          </template>
        </el-table-column>
        <el-table-column prop="agentId" label="Agent ID" width="140" />
        <el-table-column prop="agentName" label="名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP 地址" width="140" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-form style="margin-top:14px">
        <el-form-item label="补打原因">
          <el-input v-model="reprintReason" type="textarea" :rows="2" placeholder="选填，如：标签损坏/丢失" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="reprintDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="reprintLoading" :disabled="!reprintAgentId" @click="confirmReprint">下发补打</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { Search, RefreshRight, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { usePageQuery } from '@/composables/usePageQuery'
import { getAgentPage, getAgentList, getAgentTasksPage, reprintTask } from '@/api/sticker'
import { PAGE_SIZES } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'

// ========== Agent 监控 ==========
const { loading, tableData, total, query, loadData, handleSearch } = usePageQuery(
  getAgentPage,
  { agentName: '' }
)

// 全部 Agent（用于全局在线/离线统计，状态由服务端计算）
const allAgents = ref([])

// 状态筛选（前端过滤当前页）
const statusFilter = ref(null)

// 按状态筛选后的数据（基于服务端返回的 status 字段）
// el-select clearable 清除后值可能是 undefined，用 == null 同时覆盖 null 和 undefined
const filteredData = computed(() => {
  if (statusFilter.value == null || statusFilter.value === '') return tableData.value
  return tableData.value.filter(row => row.status === statusFilter.value)
})

// 全局在线/离线计数（基于 getAgentList，服务端统一计算状态）
const onlineCount = computed(() => allAgents.value.filter(a => a.status === 1).length)
const offlineCount = computed(() => allAgents.value.filter(a => a.status !== 1).length)

async function loadAllAgents() {
  try {
    const { data } = await getAgentList()
    allAgents.value = data || []
  } catch {
    allAgents.value = []
  }
}

function refreshAll() {
  loadData()
  loadAllAgents()
}

function handleReset() {
  query.agentName = ''
  statusFilter.value = null
  query.pageNum = 1
  refreshAll()
}

const STATUS_MAP = { 0: '待打印', 1: '打印中', 2: '成功', 3: '失败' }
const STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
const statusLabel = (s) => STATUS_MAP[s] || '未知'
const statusTagType = (s) => STATUS_TAG[s] || 'info'

// ========== 任务记录 ==========
const taskDialogVisible = ref(false)
const taskList = ref([])
const taskTotal = ref(0)
const currentAgent = ref('')
const currentAgentId = ref('')
const taskTableMaxHeight = ref(400)
const taskQuery = reactive({ pageNum: 1, pageSize: 50, batchId: '' })

// 任务详情
const taskDetailVisible = ref(false)
const currentTask = ref(null)
const jsonExpanded = ref(false)

// 格式化打印数据 JSON（折叠展开后端下发的 printData）
function formatPrintData(raw) {
  if (!raw) return '-'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

async function viewTasks(row) {
  currentAgent.value = row.agentName || row.agentId
  currentAgentId.value = row.agentId
  taskQuery.pageNum = 1
  taskQuery.batchId = ''
  // 弹窗高度：标题(~55) + 筛选栏(~48) + 分页(~52) + 内边距(~40) ≈ 195
  taskTableMaxHeight.value = window.innerHeight - 195 - 120 // 120 = 弹窗上下边距
  taskDialogVisible.value = true
  await loadTasks()
}

// 批次查询
function onBatchSearch() {
  taskQuery.pageNum = 1
  loadTasks()
}

function clearBatch() {
  taskQuery.batchId = ''
  taskQuery.pageNum = 1
  loadTasks()
}

// 点击批次标签快速筛选该批次
function filterByBatch(batchId) {
  taskQuery.batchId = batchId
  taskQuery.pageNum = 1
  loadTasks()
}

async function loadTasks() {
  try {
    const { data } = await getAgentTasksPage(currentAgentId.value, taskQuery)
    taskList.value = data.records || []
    taskTotal.value = data.total || 0
  } catch {
    taskList.value = []
    taskTotal.value = 0
  }
}

function viewTaskDetail(row) {
  currentTask.value = row
  jsonExpanded.value = false
  taskDetailVisible.value = true
}

// ========== 补打 ==========
const reprintDialogVisible = ref(false)
const reprintAgentList = ref([])
const reprintAgentId = ref('')
const reprintReason = ref('')
const reprintLoading = ref(false)

async function openReprint() {
  reprintAgentId.value = ''
  reprintReason.value = ''
  reprintDialogVisible.value = true
  try {
    const { data } = await getAgentList()
    reprintAgentList.value = data || []
    // 默认选中当前 Agent（若在线），补打到同一台更便捷
    const current = reprintAgentList.value.find(a => a.agentId === currentAgentId.value && a.status === 1)
    if (current) reprintAgentId.value = current.agentId
  } catch {
    reprintAgentList.value = []
  }
}

async function confirmReprint() {
  if (!currentTask.value || !reprintAgentId.value) return
  reprintLoading.value = true
  try {
    await reprintTask({
      taskId: currentTask.value.taskId,
      agentId: reprintAgentId.value,
      reason: reprintReason.value || undefined
    })
    ElMessage.success('补打任务已下发')
    reprintDialogVisible.value = false
    taskDetailVisible.value = false
    loadTasks()  // 刷新任务列表，能看到新补打任务（status=0）
  } catch {
    /* 拦截器已提示错误 */
  } finally {
    reprintLoading.value = false
  }
}

// ========== 初始化 ==========
// 监控面板自动轮询（30 秒刷新，状态由服务端计算）
const POLL_INTERVAL = 30000
let pollTimer = null

onMounted(() => {
  refreshAll()
  pollTimer = setInterval(refreshAll, POLL_INTERVAL)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
/* 表格单元格内容不换行，超长由 show-overflow-tooltip 以省略号+气泡展示 */
:deep(.el-table .el-table__cell) {
  white-space: nowrap;
}

/* 任务记录弹窗：筛选栏+分页固定，表格内部滚动 */


/* 任务记录筛选栏 */
.task-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.task-filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

/* 批次标签：等宽字体 + 浅底，相同批次一目了然 */
.batch-tag {
  display: inline-block;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
  color: #475569;
  background: #f1f5f9;
  padding: 1px 8px;
  border-radius: 4px;
  letter-spacing: 0.04em;
}
.batch-tag.clickable {
  cursor: pointer;
  transition: all 0.15s;
}
.batch-tag.clickable:hover {
  background: #dbeafe;
  color: #2563eb;
}

/* ========== 任务详情弹窗 ========== */

/* 顶部状态栏 */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 16px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 20px;
}
.detail-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.detail-task-id {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  letter-spacing: 0.02em;
}
.detail-status-tag {
  flex-shrink: 0;
}

/* 两列布局 */
.detail-body {
  display: flex;
  gap: 20px;
}
.detail-col {
  flex: 1;
  min-width: 0;
}

/* 弹窗内部滚动，防止撑开页面 */
:deep(.el-dialog) {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 40px);
  margin: 20px auto;
}
:deep(.el-dialog__body) {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 16px 20px;
}

/* 分组标题 */
.detail-section {
  margin-bottom: 18px;
}
.detail-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
  line-height: 1;
}

/* 字段网格：2列 */
.detail-field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 16px;
}
.detail-field {
  display: flex;
  flex-direction: column;
  padding: 6px 0;
}
.detail-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
  white-space: nowrap;
}
.detail-value {
  font-size: 13px;
  color: #303133;
  word-break: break-all;
  line-height: 1.5;
}
.detail-value.mono {
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
}

/* 时间线 */
.detail-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
  position: relative;
  padding-left: 14px;
}
.detail-timeline::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: #e4e7ed;
  border-radius: 1px;
}
.timeline-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 0;
  position: relative;
}
.timeline-item::before {
  content: '';
  position: absolute;
  left: -14px;
  top: 50%;
  transform: translateY(-50%);
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dcdfe6;
  border: 2px solid #fff;
  box-sizing: content-box;
}
.timeline-item.timeline-active::before {
  background: #409eff;
}
.timeline-label {
  font-size: 12px;
  color: #909399;
}
.timeline-value {
  font-size: 13px;
  color: #303133;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
}

/* 错误信息 */
.detail-error-box {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  color: #dc2626;
  line-height: 1.6;
  word-break: break-all;
}

/* 打印数据 JSON */
.detail-json-section {
  margin-top: 6px;
  border-top: 1px solid #ebeef5;
  padding-top: 14px;
}
.detail-json-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  user-select: none;
  padding: 4px 0;
}
.detail-json-header:hover .detail-section-title {
  color: #409eff;
}
.detail-json-arrow {
  font-size: 14px;
  color: #909399;
  transition: transform 0.25s;
}
.detail-json-arrow.expanded {
  transform: rotate(180deg);
}
.detail-json-pre {
  margin: 8px 0 0;
  max-height: 240px;
  overflow: auto;
  font-size: 12px;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  background: #f5f7fa;
  color: #475569;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  line-height: 1.6;
}
</style>

<!-- 全局样式：弹窗 DOM 在 body 层级，scoped 无法穿透，需用非 scoped 块 -->
<style>
.task-dialog.el-dialog {
  display: flex;
  flex-direction: column;
  margin-top: 8vh !important;
  margin-bottom: 4vh !important;
  max-height: calc(100vh - 12vh);
}
.task-dialog .el-dialog__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 12px 20px;
}
.task-dialog .el-dialog__body > .task-filter-bar {
  flex-shrink: 0;
}
.task-dialog .el-dialog__body > .el-table {
  flex: 1;
  min-height: 0;
}
.task-dialog .el-dialog__body > .pagination-wrapper--sm {
  flex-shrink: 0;
}
</style>
