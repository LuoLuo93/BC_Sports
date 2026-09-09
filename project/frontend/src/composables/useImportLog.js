import { ref, reactive } from 'vue'
import { defaultPageSize } from '@/utils/appConfig'

/**
 * 导入日志查询逻辑（F70：原 6 个导入页各拷贝一份的 ~40 行收口于此）。
 * @param {Function} fetchLogPage (params) => Promise，各模块自己的 /import-log/page 接口函数
 */
export function useImportLog(fetchLogPage) {
  const logLoading = ref(false)
  const logData = ref([])
  const logTotal = ref(0)
  const logQuery = reactive({ pageNum: 1, pageSize: defaultPageSize.value })

  const errorDialogVisible = ref(false)
  const errorDialogContent = ref('')

  async function loadLogData() {
    logLoading.value = true
    try {
      const res = await fetchLogPage({ pageNum: logQuery.pageNum, pageSize: logQuery.pageSize })
      logData.value = res.data?.records || []
      logTotal.value = res.data?.total || 0
    } finally {
      logLoading.value = false
    }
  }

  function viewErrors(row) {
    errorDialogContent.value = row.errorMsg || ''
    errorDialogVisible.value = true
  }

  function formatSize(bytes) {
    if (!bytes) return '-'
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / 1024 / 1024).toFixed(2) + ' MB'
  }

  function statusLabel(s) {
    return { SUCCESS: '全部成功', PARTIAL: '部分失败', FAILED: '失败' }[s] || s
  }

  function statusTagType(s) {
    return { SUCCESS: 'success', PARTIAL: 'warning', FAILED: 'danger' }[s] || 'info'
  }

  return {
    logLoading, logData, logTotal, logQuery,
    loadLogData, viewErrors,
    errorDialogVisible, errorDialogContent,
    formatSize, statusLabel, statusTagType,
  }
}
