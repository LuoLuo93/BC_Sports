<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">在线用户</span>
          <div class="header-actions">
            <el-button type="primary" size="small" :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无在线用户">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column label="登录时间" width="180">
          <template #default="{ row }">
            {{ row.loginTime ? formatTime(row.loginTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="最后活跃" width="180">
          <template #default="{ row }">
            {{ row.lastAccessTime ? formatTime(row.lastAccessTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="host" label="IP地址" width="140" />
        <el-table-column label="超时时间" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ formatTimeout(row.timeout) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'system:online:kick'"
              type="danger" plain size="small"
              :loading="kicking === row.sessionId"
              @click="handleKick(row)"
            >强制下线</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'OnlineUser' })
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getOnlineUserList, kickOnlineUser } from '@/api/online-user'

const loading = ref(false)
const kicking = ref('')
const tableData = ref([])
let pollTimer = null

function formatTime(ts) {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function formatTimeout(ms) {
  if (!ms || ms <= 0) return '永不过期'
  const min = Math.floor(ms / 60000)
  if (min < 60) return `${min}分钟`
  return `${Math.floor(min / 60)}小时${min % 60}分钟`
}

async function loadData() {
  loading.value = true
  try {
    const res = await getOnlineUserList()
    tableData.value = res.data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function handleKick(row) {
  try {
    await ElMessageBox.confirm(`确定将用户「${row.nickname || row.username}」强制下线？`, '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }

  kicking.value = row.sessionId
  try {
    await kickOnlineUser(row.username)
    ElMessage.success(`${row.nickname || row.username} 已强制下线`)
    await loadData()
  } catch {
    // Error handled by request interceptor
  } finally {
    kicking.value = ''
  }
}

onMounted(() => {
  loadData()
  // Auto-refresh every 30 seconds
  pollTimer = setInterval(loadData, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 8px;
}
</style>