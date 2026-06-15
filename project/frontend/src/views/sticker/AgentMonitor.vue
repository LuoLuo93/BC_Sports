<template>
  <div class="page-container">
    <el-card shadow="never">
      <div style="display:flex;justify-content:space-between;margin-bottom:16px">
        <span style="font-size:16px;font-weight:600">Agent 监控面板</span>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="agentList" border stripe>
        <el-table-column prop="agentId" label="Agent ID" width="120" />
        <el-table-column prop="agentName" label="名称" width="150" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="printers" label="打印机" min-width="200" show-overflow-tooltip />
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="170" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" plain size="small" @click="viewTasks(row)">任务记录</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;gap:20px">
        <el-statistic title="在线 Agent" :value="onlineCount" />
        <el-statistic title="离线 Agent" :value="offlineCount" />
      </div>
    </el-card>

    <!-- 任务记录弹窗 -->
    <el-dialog v-model="taskDialogVisible" :title="`${currentAgent} - 打印任务`" width="800px">
      <el-table :data="taskList" border size="small" max-height="400">
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
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/api/request'

const loading = ref(false)
const agentList = ref([])
const taskDialogVisible = ref(false)
const taskList = ref([])
const currentAgent = ref('')

const onlineCount = computed(() => agentList.value.filter(a => a.status === 1).length)
const offlineCount = computed(() => agentList.value.filter(a => a.status === 0).length)

const STATUS_MAP = { 0: '待打印', 1: '打印中', 2: '成功', 3: '失败' }
const STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
const statusLabel = (s) => STATUS_MAP[s] || '未知'
const statusTagType = (s) => STATUS_TAG[s] || 'info'

async function loadData() {
  loading.value = true
  try {
    const { data } = await request.get('/api/agent/list')
    agentList.value = data || []
  } finally {
    loading.value = false
  }
}

async function viewTasks(agent) {
  currentAgent.value = agent.agentName || agent.agentId
  try {
    const { data } = await request.get(`/api/agent/${agent.agentId}/tasks`)
    taskList.value = data || []
  } catch {
    taskList.value = []
  }
  taskDialogVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>
