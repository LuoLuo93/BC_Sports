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
          <el-table-column prop="agentName" label="名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="ipAddress" label="IP 地址" width="140" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="printers" label="打印机" min-width="140" show-overflow-tooltip />
          <el-table-column label="最后心跳" width="160">
            <template #default="{ row }">
              {{ formatTime(row.lastHeartbeat) }}
            </template>
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
    <el-dialog v-model="taskDialogVisible" :title="`${currentAgent} - 打印任务`" width="1200px">
      <el-table :data="taskList" border size="small">
        <el-table-column label="任务ID" width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.taskId }}</span>
            <el-tag v-if="row.isReprint === 1" type="warning" size="small" effect="plain" style="margin-left:4px">补</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="申请单号" width="190" show-overflow-tooltip />
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
          layout="total, sizes, prev, pager, next"
          size="small"
          @size-change="() => { taskQuery.pageNum = 1; loadTasks() }"
          @current-change="loadTasks"
        />
      </div>
    </el-dialog>

    <!-- 任务详情弹窗 -->
    <el-dialog v-model="taskDetailVisible" title="任务详情" width="720px" append-to-body>
      <el-descriptions v-if="currentTask" :column="2" border size="small">
        <el-descriptions-item label="任务ID" :span="2">{{ currentTask.taskId || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentTask.isReprint === 1" label="补打任务">
          <el-tag type="warning" size="small">是</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentTask.isReprint === 1" label="原任务ID">{{ currentTask.sourceTaskId || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentTask.isReprint === 1 && currentTask.reprintReason" label="补打原因" :span="2">{{ currentTask.reprintReason }}</el-descriptions-item>
        <el-descriptions-item label="申请单号">{{ currentTask.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentTask.status)" size="small">{{ statusLabel(currentTask.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="货号">{{ currentTask.materialNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="款号">{{ currentTask.styleNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="货品名称" :span="2">{{ currentTask.materialName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ currentTask.brandName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类别">{{ currentTask.kindName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="颜色">{{ currentTask.color || '-' }}</el-descriptions-item>
        <el-descriptions-item label="尺码">{{ currentTask.sizeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="打印数量">{{ currentTask.printQty ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ currentTask.retryCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="模板文件" :span="2">{{ currentTask.templateFile || '-' }}</el-descriptions-item>
        <el-descriptions-item label="打印机">{{ currentTask.printerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Agent ID">{{ currentTask.agentId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(currentTask.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="派发时间">{{ formatTime(currentTask.dispatchTime) }}</el-descriptions-item>
        <el-descriptions-item label="打印时间" :span="2">{{ formatTime(currentTask.printTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentTask.errorMsg" label="错误信息" :span="2">
          <span style="color:#dc2626">{{ currentTask.errorMsg }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="打印数据(JSON)" :span="2">
          <pre style="margin:0;max-height:200px;overflow:auto;font-size:12px;background:#f5f5f5;padding:8px;border-radius:4px">{{ formatPrintData(currentTask.printData) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="taskDetailVisible = false">关闭</el-button>
          <el-button v-if="currentTask && (currentTask.status === 2 || currentTask.status === 3)"
            type="warning" @click="openReprint">补打</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 补打 · 选择 Agent -->
    <el-dialog v-model="reprintDialogVisible" title="补打 · 选择 Agent" width="720px" append-to-body :close-on-click-modal="false">
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
import { Search, RefreshRight } from '@element-plus/icons-vue'
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
const filteredData = computed(() => {
  if (statusFilter.value === null) return tableData.value
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
const taskQuery = reactive({ pageNum: 1, pageSize: 20 })

// 任务详情
const taskDetailVisible = ref(false)
const currentTask = ref(null)

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
  taskDialogVisible.value = true
  await loadTasks()
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
</style>
