<template>
  <div class="page-container">
    <!-- API Key 管理卡片 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <span style="font-size:16px;font-weight:600">Agent 配置</span>
      </div>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="API Key">
          <div style="display:flex;align-items:center;gap:8px">
            <code style="user-select:all;padding:2px 8px;background:#f5f7fa;border-radius:4px;min-width:200px">
              {{ keyRevealed ? apiKey : maskKey(apiKey) }}
            </code>
            <el-button size="small" @click="keyRevealed = !keyRevealed">
              {{ keyRevealed ? '隐藏' : '显示' }}
            </el-button>
            <el-button size="small" @click="copyKey">复制</el-button>
            <el-button size="small" type="warning" @click="regenerateKey">重新生成</el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:8px;color:#909399;font-size:12px">
        C# 客户端（StickerPrintAgent）通过 X-API-Key 请求头携带此密钥。重新生成后所有已注册 Agent 需要更新密钥。
      </div>
    </el-card>

    <!-- Agent 监控表格 -->
    <el-card shadow="never">
      <div style="display:flex;justify-content:space-between;margin-bottom:16px">
        <span style="font-size:16px;font-weight:600">Agent 监控面板</span>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="agentList" border stripe>
        <el-table-column prop="agentId" label="Agent ID" width="120" />
        <el-table-column prop="agentName" label="名称" width="150" />
        <el-table-column prop="ipAddress" label="IP 地址" width="140" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { getConfigs, updateConfigs } from '@/api/config'

// ========== Agent 监控 ==========
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

// ========== API Key 管理 ==========
const apiKey = ref('')
const keyRevealed = ref(false)

function maskKey(key) {
  if (!key) return '（未配置）'
  if (key.length <= 8) return '****'
  return key.substring(0, 4) + '****' + key.substring(key.length - 4)
}

async function loadApiKey() {
  try {
    const res = await getConfigs()
    const configs = res.data || res || []
    const found = Array.isArray(configs)
      ? configs.find(c => c.configKey === 'agent.api-key')
      : null
    apiKey.value = found?.configValue || ''
  } catch {
    apiKey.value = ''
  }
}

async function copyKey() {
  if (!apiKey.value) {
    ElMessage.warning('暂无 API Key 可复制')
    return
  }
  try {
    await navigator.clipboard.writeText(apiKey.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    // fallback
    const ta = document.createElement('textarea')
    ta.value = apiKey.value
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('已复制到剪贴板')
  }
}

function generateRandomKey() {
  return crypto.randomUUID ? crypto.randomUUID().replace(/-/g, '') :
    'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/x/g, () => Math.floor(Math.random() * 16).toString(16))
}

async function regenerateKey() {
  try {
    await ElMessageBox.confirm(
      '重新生成后所有已注册 Agent 需要更新密钥才能继续工作，确定吗？',
      '确认重新生成',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }

  const newKey = generateRandomKey()
  try {
    await updateConfigs({ 'agent.api-key': newKey })
    apiKey.value = newKey
    keyRevealed.value = true
    ElMessage.success('API Key 已重新生成，请通知各 Agent 更新密钥')
  } catch (e) {
    ElMessage.error('重新生成失败: ' + (e.message || '未知错误'))
  }
}

// ========== 初始化 ==========
onMounted(() => {
  loadData()
  loadApiKey()
})
</script>
