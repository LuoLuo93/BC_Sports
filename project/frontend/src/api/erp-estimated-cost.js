import request from './request'

export function getEstimatedCostPage(params) {
  return request.get('/api/erp/estimated-cost/page', { params })
}

export function updateEstimatedCost(data) {
  return request.put('/api/erp/estimated-cost/precost', data)
}

export function importEstimatedCost(data) {
  return request.post('/api/erp/estimated-cost/import', data, { timeout: 600000 })
}

export function getEstimatedCostTemplate() {
  return request.get('/api/erp/estimated-cost/template', { responseType: 'blob' })
}

export function getEstimatedCostImportLogPage(params) {
  return request.get('/api/erp/estimated-cost/import-log/page', { params })
}
