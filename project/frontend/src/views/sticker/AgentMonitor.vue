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
              {{ fmtTime(row.lastHeartbeat) }}
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
    <el-dialog v-model="taskDialogVisible" :title="`${currentAgent} - 打印任务`" width="900px">
      <el-table :data="taskList" border size="small">
        <el-table-column prop="taskNo" label="任务ID" width="100" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="申请单号" width="150" />
        <el-table-column prop="materialNumber" label="货号" width="120" />
        <el-table-column prop="materialName" label="货品名称" width="150" />
        <el-table-column prop="sizeName" label="尺码" width="60" />
        <el-table-column prop="printQty" label="数量" width="60" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="printTime" label="打印时间" width="170" />
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { getAgentPage, getAgentList, getAgentTasksPage } from '@/api/sticker'
import { PAGE_SIZES } from '@/utils/appConfig'

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

// 最后心跳相对时间（刚刚 / X分钟前），配合 30s 自动轮询动态刷新；
// 状态标签以服务端 row.status 为准，相对时间仅作直观展示。
function fmtTime(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d)) return val
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ========== 任务记录 ==========
const taskDialogVisible = ref(false)
const taskList = ref([])
const taskTotal = ref(0)
const currentAgent = ref('')
const currentAgentId = ref('')
const taskQuery = reactive({ pageNum: 1, pageSize: 20 })

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
