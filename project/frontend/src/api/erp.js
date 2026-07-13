import request from './request'

// ERP Store (店仓 - 数据源: bjerp C_STORE)
export function getErpStorePage(params) {
  return request.get('/api/erp-store/page', { params })
}

export function getErpStoreListAll() {
  return request.get('/api/erp-store/list-all')
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
