import { ref, reactive } from 'vue'

export function usePageQuery(apiFn, defaultQuery = {}) {
  const loading = ref(false)
  const tableData = ref([])
  const total = ref(0)

  const query = reactive({ ...defaultQuery, pageNum: 1, pageSize: 10 })

  async function loadData() {
    loading.value = true
    try {
      const res = await apiFn(query)
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    } finally {
      loading.value = false
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
