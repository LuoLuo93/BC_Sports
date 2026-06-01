import { ref, reactive } from 'vue'
import { defaultPageSize } from '@/utils/appConfig'

export function usePageQuery(apiFn, defaultQuery = {}) {
  const loading = ref(false)
  const tableData = ref([])
  const total = ref(0)

  const query = reactive({ ...defaultQuery, pageNum: 1, pageSize: defaultPageSize.value })

  let requestId = 0

  async function loadData() {
    const id = ++requestId
    loading.value = true
    try {
      const res = await apiFn(query)
      if (id !== requestId) return // 过期响应，丢弃
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    } finally {
      if (id === requestId) loading.value = false
    }
  }

  function handleSearch() {
    query.pageNum = 1
    loadData()
  }

  function resetQuery() {
    Object.keys(defaultQuery).forEach(key => {
      query[key] = defaultQuery[key]
    })
    query.pageNum = 1
    loadData()
  }

  return { loading, tableData, total, query, loadData, handleSearch, resetQuery }
}
