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

      <div class="table-responsive">
      <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无在线用户" :row-class-name="rowClassName">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="username" label="用户名" min-width="160" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="140" show-overflow-tooltip />
        <el-table-column label="登录时间" min-width="180">
          <template #default="{ row }">
            {{ row.loginTime ? formatTimeLocal(row.loginTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="最后活跃" min-width="180">
          <template #default="{ row }">
            {{ row.lastAccessTime ? formatTimeLocal(row.lastAccessTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="活跃状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.lastAccessTime" size="small" :type="activityType(row)" effect="light">
              {{ activityText(row) }}
            </el-tag>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="IP地址" min-width="140" class-name="mono-cell" />
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
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'OnlineUser' })
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getOnlineUserList, kickOnlineUser } from '@/api/online-user'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const kicking = ref('')
const tableData = ref([])
let pollTimer = null

/** 格式化时间：兼容数字时间戳、ISO字符串、数组(Shiro序列化) */
function formatTimeLocal(t) {
  if (!t) return '-'
  let date
  if (Array.isArray(t)) {
    date = new Date(t[0], (t[1] || 1) - 1, t[2] || 1, t[3] || 0, t[4] || 0, t[5] || 0)
  } else if (typeof t === 'number') {
    date = new Date(t)
  } else {
    const num = Number(t)
    if (!isNaN(num) && t.toString().length >= 10) {
      date = new Date(num)
    } else {
      date = new Date(String(t).replace('T', ' ').replace(/-/g, '/'))
    }
  }
  if (isNaN(date.getTime())) return String(t)
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function formatTimeout(ms) {
  if (!ms || ms <= 0) return '永不过期'
  const min = Math.floor(ms / 60000)
  if (min < 60) return `${min}分钟`
  return `${Math.floor(min / 60)}小时${min % 60}分钟`
}

/** 最后活跃距今的分钟数（无活跃时间返回 null） */
function minutesAgo(row) {
  if (!row.lastAccessTime) return null
  const t = new Date(row.lastAccessTime).getTime()
  if (isNaN(t)) return null
  return Math.max(0, Math.floor((Date.now() - t) / 60000))
}

/** 活跃状态分档：< 5min 活跃(绿)，5~30min 一般(蓝)，> 30min 闲置(灰) */
function activityType(row) {
  const m = minutesAgo(row)
  if (m === null) return 'info'
  if (m < 5) return 'success'
  if (m < 30) return 'primary'
  return 'info'
}

/** 相对时间文案：刚刚 / N分钟前 / N小时前 */
function activityText(row) {
  const m = minutesAgo(row)
  if (m === null) return '-'
  if (m < 1) return '刚刚'
  if (m < 60) return `${m}分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}小时前`
  return `${Math.floor(h / 24)}天前`
}

/** 闲置 > 30min 的行整行变灰，一眼看出僵尸会话 */
function rowClassName({ row }) {
  const m = minutesAgo(row)
  return m !== null && m >= 30 ? 'row-idle' : ''
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
/* IP 地址列等宽字体 */
:deep(.mono-cell) {
  font-family: 'Cascadia Code', 'Consolas', monospace;
}
/* 闲置 > 30min 的行整行变灰，强调僵尸会话 */
:deep(.el-table__row.row-idle) {
  color: #b6b9c2;
}
:deep(.el-table__row.row-idle td) {
  background-color: #fafafa !important;
}
</style>