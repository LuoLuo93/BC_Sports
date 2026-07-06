import request from './request'

/**
 * 旧版HK ERP 职员同步 API
 * 对应后端 HkPersonnelSyncController (/api/hk-personnel)
 */

// ==================== 分页查询 ====================

export function getHkOnboardingPage(params) {
  return request.get('/api/hk-personnel/onboarding/page', { params })
}

export function getHkUpdatePage(params) {
  return request.get('/api/hk-personnel/update/page', { params })
}

export function getHkLeavingPage(params) {
  return request.get('/api/hk-personnel/leaving/page', { params })
}

// ==================== 手动触发 ====================

export function syncHkOnboarding() {
  return request.post('/api/hk-personnel/sync-onboarding')
}

export function syncHkUpdate() {
  return request.post('/api/hk-personnel/sync-update')
}

export function syncHkByType(syncType, employeeId) {
  return request.post(`/api/hk-personnel/sync-erp/${syncType}/${employeeId}`)
}

export function getHkSyncStatus() {
  return request.get('/api/hk-personnel/sync-status')
}
