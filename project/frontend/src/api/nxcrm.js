import request from './request'

export function getNxcrmTagTasks(params) {
  return request.get('/api/nxcrm/tag/tasks', { params })
}

export function getNxcrmTagTask(taskId) {
  return request.get(`/api/nxcrm/tag/tasks/${taskId}`)
}

export function createNxcrmTagTask(data) {
  return request.post('/api/nxcrm/tag/tasks', data)
}

export function executeNxcrmTagTask(taskId) {
  return request.post(`/api/nxcrm/tag/tasks/${taskId}/execute`)
}

export function getNxcrmTaskStatus(taskId) {
  return request.get(`/api/nxcrm/tag/tasks/${taskId}/status`)
}

export function getNxcrmTaskDetails(taskId, params) {
  return request.get(`/api/nxcrm/tag/tasks/${taskId}/details`, { params })
}

export function getNxcrmMemberTags(params) {
  return request.get('/api/nxcrm/tag/member-tags', { params })
}

export function fillNxcrmShopId(batchNo) {
  return request.post('/api/nxcrm/tag/member-tags/fill-shop', null, { params: { batchNo } })
}

export function getNxcrmMemberDetail(params) {
  return request.get('/api/nxcrm/member-tag-mgmt/member-detail', { params })
}

export function getNxcrmTagList(params) {
  return request.get('/api/nxcrm/member-tag-mgmt/tag-list', { params })
}

export function syncNxcrmTags() {
  return request.post('/api/nxcrm/member-tag-mgmt/sync-tags')
}

export function syncNxcrmMemberTags() {
  return request.post('/api/nxcrm/member-tag-mgmt/sync-member-tags')
}

export function getNxcrmSyncStatus() {
  return request.get('/api/nxcrm/member-tag-mgmt/sync-status')
}
