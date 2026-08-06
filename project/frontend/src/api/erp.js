import request from './request'

// ERP Store (店仓 - 数据源: bjerp C_STORE)
export function getErpStorePage(params) {
  return request.get('/api/erp-store/page', { params })
}

// 渠道配置表单专用：精简分页查询店仓，返回 CODE/NAME
export function getErpStoreSimplePage(params) {
  return request.get('/api/erp-store/simple-page', { params })
}

export function getErpStoreListAll() {
  return request.get('/api/erp-store/list-all')
}

// 店仓品牌下拉（C_STOREATTRIBVALUE DIM5）
export function getErpStoreBrands() {
  return request.get('/api/erp-store/brands')
}

// 店仓零售督导下拉（C_STOREATTRIBVALUE DIM6）
export function getErpStoreSupervisors() {
  return request.get('/api/erp-store/supervisors')
}

// 编辑店仓品牌/督导归属，写回 ERP C_STORE
export function updateErpStoreAttrib(data) {
  return request.put('/api/erp-store/attrib', data)
}

// ERP Shop
export function getErpShopPage(params) {
  return request.get('/api/erp-shop/page', { params })
}

export function getErpShop(id) {
  return request.get(`/api/erp-shop/${id}`)
}

export function createErpShop(data) {
  return request.post('/api/erp-shop', data)
}

export function updateErpShop(id, data) {
  return request.put(`/api/erp-shop/${id}`, data)
}

export function deleteErpShop(id) {
  return request.delete(`/api/erp-shop/${id}`)
}

export function getEnabledErpShopList() {
  return request.get('/api/erp-shop/list-enabled')
}

// ERP Warehouse
export function getErpWarehousePage(params) {
  return request.get('/api/erp-warehouse/page', { params })
}

export function getErpWarehouse(id) {
  return request.get(`/api/erp-warehouse/${id}`)
}

export function createErpWarehouse(data) {
  return request.post('/api/erp-warehouse', data)
}

export function updateErpWarehouse(id, data) {
  return request.put(`/api/erp-warehouse/${id}`, data)
}

export function deleteErpWarehouse(id) {
  return request.delete(`/api/erp-warehouse/${id}`)
}

export function getEnabledErpWarehouseList() {
  return request.get('/api/erp-warehouse/list-enabled')
}

// ERP Customer (bjerp WMS_CUSTOMER)
export function getErpCustomerPage(params) {
  return request.get('/api/erp-customer/page', { params })
}

export function getErpCustomerListAll() {
  return request.get('/api/erp-customer/list-all')
}

// 揽众客户押金资料 (bjerp LZCUSTOMERINFOR)
export function getLzCustomerPage(params) {
  return request.get('/api/lz-customer/page', { params })
}

export function getLzCustomer(id) {
  return request.get(`/api/lz-customer/${id}`)
}

export function createLzCustomer(data) {
  return request.post('/api/lz-customer', data)
}

export function updateLzCustomer(id, data) {
  return request.put(`/api/lz-customer/${id}`, data)
}

export function deleteLzCustomer(id) {
  return request.delete(`/api/lz-customer/${id}`)
}

export function importLzCustomer(data) {
  return request.post('/api/lz-customer/import', data, { timeout: 600000 })
}

export function getLzCustomerTemplate() {
  return request.get('/api/lz-customer/template', { responseType: 'blob' })
}
