import request from './request'

// IHR Sync Status
export function getIhrSyncStatus() {
  return request.get('/api/ihr/sync-status')
}

// IHR Onboarding
export function getIhrOnboardingPage(params) {
  return request.get('/api/ihr-onboarding/page', { params })
}

export function syncIhrOnboarding() {
  return request.post('/api/ihr-onboarding/sync-ihr')
}

export function syncQywxOnboarding() {
  return request.post('/api/ihr-onboarding/sync-qywx')
}

export function syncQywxOnboardingByEmployee(employeeId) {
  return request.post(`/api/ihr-onboarding/sync-qywx/${employeeId}`)
}

// IHR Update (Adjustment)
export function getIhrUpdatePage(params) {
  return request.get('/api/ihr-update/page', { params })
}

export function syncIhrUpdate() {
  return request.post('/api/ihr-update/sync-ihr')
}

export function syncQywxUpdate(staffId) {
  return request.post(`/api/ihr-update/sync-qywx/${staffId}`)
}

// IHR Leaving
export function getIhrLeavingPage(params) {
  return request.get('/api/ihr-leaving/page', { params })
}

export function syncIhrLeaving() {
  return request.post('/api/ihr-leaving/sync-ihr')
}

export function syncQywxLeaving(employeeId) {
  return request.post(`/api/ihr-leaving/sync-qywx/${employeeId}`)
}

// IHR Exclusion
export function getIhrExclusionPage(params) {
  return request.get('/api/ihr-exclusion/page', { params })
}

export function getIhrExclusion(id) {
  return request.get(`/api/ihr-exclusion/${id}`)
}

export function createIhrExclusion(data) {
  return request.post('/api/ihr-exclusion', data)
}

export function updateIhrExclusion(id, data) {
  return request.put(`/api/ihr-exclusion/${id}`, data)
}

export function deleteIhrExclusion(id) {
  return request.delete(`/api/ihr-exclusion/${id}`)
}

export function batchDeleteIhrExclusion(ids) {
  return request.delete('/api/ihr-exclusion/batch', { data: ids })
}

export function batchUpdateIhrExclusionStatus(data) {
  return request.put('/api/ihr-exclusion/batch/status', data)
}
