import { ref } from 'vue'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { getIhrSyncStatus } from '@/api/ihr'

export function useSyncPolling() {
  const loading = ref(false)
  const elapsedSeconds = ref(0)
  let timer = null
  let pollTimer = null
  let loadingInstance = null

  function triggerSync(url, name, refreshCallback) {
    ElMessageBox.confirm(
      `确定要执行"${name}"吗？任务将在后台执行，同步期间您无法进行其他操作。`,
      '确认执行',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    ).then(async () => {
      loading.value = true
      elapsedSeconds.value = 0
      loadingInstance = ElLoading.service({ text: `${name}执行中，请稍候... 已运行: 0秒`, lock: true })

      timer = setInterval(() => {
        elapsedSeconds.value++
        if (loadingInstance) {
          loadingInstance.setText(`${name}执行中，请稍候... 已运行: ${elapsedSeconds.value}秒`)
        }
      }, 1000)

      try {
        const { default: request } = await import('@/api/request')
        const res = await request.post(url)
        ElMessage.success(res.message || `${name}已触发`)
        pollStatus(name, refreshCallback)
      } catch (e) {
        ElMessage.error(`${name}触发失败`)
        cleanup()
      }
    }).catch(() => {})
  }

  function pollStatus(name, refreshCallback, maxPolls = 900) {
    let pollCount = 0
    pollTimer = setInterval(async () => {
      try {
        const res = await getIhrSyncStatus()
        if (res.data && res.data.syncing) {
          pollCount++
          if (res.data.elapsedSeconds !== undefined) {
            elapsedSeconds.value = res.data.elapsedSeconds
          }
          if (loadingInstance) {
            loadingInstance.setText(`${name}执行中，请稍候... 已运行: ${elapsedSeconds.value}秒`)
          }
          if (pollCount >= maxPolls) {
            ElMessage.warning(`${name}执行时间过长，请稍后刷新页面查看结果`)
            cleanup()
            refreshCallback?.()
          }
        } else {
          ElMessage.success(`${name}执行完成`)
          cleanup()
          refreshCallback?.()
        }
      } catch {
        pollCount++
        if (pollCount >= maxPolls) {
          ElMessage.warning(`${name}执行时间过长，请稍后刷新页面查看结果`)
          cleanup()
          refreshCallback?.()
        }
      }
    }, 5000)
  }

  function cleanup() {
    loading.value = false
    if (timer) { clearInterval(timer); timer = null }
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
    if (loadingInstance) { loadingInstance.close(); loadingInstance = null }
  }

  return { loading, elapsedSeconds, triggerSync }
}
