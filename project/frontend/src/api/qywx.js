import request from './request'

export function getCorpTags(params) {
  return request.get('/api/qywx/tag/corp-tags', { params })
}

export function syncQywxTags() {
  return request.post('/api/qywx/tag/sync')
}

export function getQywxTagSyncStatus() {
  return request.get('/api/qywx/tag/sync-status')
}

export function getTagTemplate() {
  return request.get('/api/qywx/tag/template', { responseType: 'blob' })
}

export function uploadTagData(data) {
  return request.post('/api/qywx/tag/upload', data)
}

export function getTagRecords(params) {
  return request.get('/api/qywx/tag/records', { params })
}

export function addCorpTagGroup(data) {
  return request.post('/api/qywx/tag/add-corp-tag', data)
}

export function editCorpTagGroup(data) {
  return request.post('/api/qywx/tag/edit-corp-tag', data)
}

export function deleteCorpTagGroup(data) {
  return request.post('/api/qywx/tag/delete-corp-tag', data)
}

// ===== 客户联系成员 =====

export function getFollowUserPage(params) {
  return request.get('/api/qywx/follow-user/page', { params })
}

export function getFollowUserCustomers(params) {
  return request.get('/api/qywx/follow-user/customers', { params })
}

export function getFollowUserGroupStats(params) {
  return request.get('/api/qywx/follow-user/group-stats', { params })
}

export function syncFollowUsers() {
  return request.post('/api/qywx/follow-user/sync')
}

export function getFollowUserSyncStatus() {
  return request.get('/api/qywx/follow-user/sync-status')
}

// ===== 群聊管理 =====

export function getGroupChatPage(params) {
  return request.get('/api/qywx/group-chat/page', { params })
}

export function getGroupChatMembers(chatId) {
  return request.get('/api/qywx/group-chat/members', { params: { chatId } })
}

export function syncGroupChats() {
  return request.post('/api/qywx/group-chat/sync')
}

export function getGroupChatSyncStatus() {
  return request.get('/api/qywx/group-chat/sync-status')
}

// ===== 朋友圈管理 =====

export function getMomentPage(params) {
  return request.get('/api/qywx/moment/page', { params })
}

export function syncMoments() {
  return request.post('/api/qywx/moment/sync')
}

export function getMomentSyncStatus() {
  return request.get('/api/qywx/moment/sync-status')
}
