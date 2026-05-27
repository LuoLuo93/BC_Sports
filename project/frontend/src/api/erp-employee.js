import request from './request'

export function getErpEmployeeOnboardingPage(params) {
  return request.get('/api/erp-employee/onboarding/page', { params })
}

export function getErpEmployeeUpdatePage(params) {
  return request.get('/api/erp-employee/update/page', { params })
}

export function getErpEmployeeLeavingPage(params) {
  return request.get('/api/erp-employee/leaving/page', { params })
}

export function syncErpEmployeeIhr() {
  return request.post('/api/erp-employee/sync-ihr')
}

export function syncErpEmployee() {
  return request.post('/api/erp-employee/sync-erp')
}

export function syncErpEmployeeByType(syncType, employeeId) {
  return request.post(`/api/erp-employee/sync-erp/${syncType}/${employeeId}`)
}

export function getErpSyncStatus() {
  return request.get('/api/erp-employee/sync-status')
}
