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

// 店铺日预算
export function getShopDailyBudgetPage(params) {
  return request.get('/api/bi/shop-daily-budget/page', { params })
}

export function importShopDailyBudget(data) {
  return request.post('/api/bi/shop-daily-budget/import', data, { timeout: 600000 })
}

export function getShopDailyBudgetTemplate() {
  return request.get('/api/bi/shop-daily-budget/template', { responseType: 'blob' })
}

export function getShopDailyBudgetImportLogPage(params) {
  return request.get('/api/bi/shop-daily-budget/import-log/page', { params })
}

// 数仓销售查看
export function getDwSalesMainPage(params) {
  return request.get('/api/bi/dw-sales/page', { params })
}

export function updateDwSalesMain(data) {
  return request.post('/api/bi/dw-sales/update', data)
}
