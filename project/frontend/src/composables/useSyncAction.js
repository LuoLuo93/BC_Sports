import { ref, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const POLL_INTERVAL = 3000
/** 轮询超时上限（600次×3秒=30分钟）：后端若一直返回 syncing=true 则不再无限轮询 */
const MAX_POLLS = 600

export function useSyncAction(syncFn, refreshFn, confirmMsg = '确定执行同步操作？', statusFn = null, statusField = 'syncing') {
  const syncLoading = ref(false)
  let timer = null
  let pollCount = 0

  function stopPolling() {
    if (timer) { clearInterval(timer); timer = null }
    pollCount = 0
  }

  function startPolling() {
    if (!statusFn || timer) return
    pollCount = 0
    timer = setInterval(async () => {
      if (++pollCount > MAX_POLLS) {
        stopPolling()
        syncLoading.value = false
        ElMessage.warning('同步轮询超时(30分钟)，请手动刷新查看结果')
        refreshFn?.()
        return
      }
      try {
        const res = await statusFn()
        if (!res.data?.[statusField]) {
          stopPolling()
          syncLoading.value = false
          refreshFn?.()
        }
      } catch {
        // keep polling on transient errors
      }
    }, POLL_INTERVAL)
  }

  async function handleSync() {
    await ElMessageBox.confirm(confirmMsg, '提示', { type: 'warning' })
    syncLoading.value = true
    try {
      await syncFn()
      if (statusFn) {
        ElMessage.success('同步任务已触发')
        startPolling()
      } else {
        ElMessage.success('同步任务已触发')
        syncLoading.value = false
        refreshFn?.()
      }
    } catch {
      syncLoading.value = false
    }
  }

  async function checkStatus() {
    if (!statusFn) return
    try {
      const res = await statusFn()
      if (res.data?.[statusField]) {
        syncLoading.value = true
        startPolling()
      }
    } catch {
      // ignore
    }
  }

  onUnmounted(() => stopPolling())

  return { syncLoading, handleSync, checkStatus }
}
