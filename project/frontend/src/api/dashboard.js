import request from './request'

export function getDashboardStats() {
  return request.get('/api/dashboard/stats')
}

export function getDashboardSyncStatus() {
  return request.get('/api/dashboard/sync-status')
}

export function getDashboardSystemHealth() {
  return request.get('/api/dashboard/system-health')
}
