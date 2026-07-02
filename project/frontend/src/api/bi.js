import request from './request'

// 货品资料
export function getGoodsDataPage(params) {
  return request.get('/api/bi/goods-data/page', { params })
}

export function importGoodsData(data) {
  return request.post('/api/bi/goods-data/import', data, { timeout: 600000 })
}

export function getGoodsDataTemplate() {
  return request.get('/api/bi/goods-data/template', { responseType: 'blob' })
}

export function getGoodsImportLogPage(params) {
  return request.get('/api/bi/goods-data/import-log/page', { params })
}
