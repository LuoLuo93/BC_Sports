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
