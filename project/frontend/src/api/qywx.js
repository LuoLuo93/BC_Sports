import request from './request'

export function getCorpTags() {
  return request.get('/api/qywx/tag/corp-tags')
}

export function syncQywxTags() {
  return request.post('/api/qywx/tag/sync')
}

export function getTagTemplate() {
  return request.get('/api/qywx/tag/template')
}

export function uploadTagData(data) {
  return request.post('/api/qywx/tag/upload', data)
}

export function getTagRecords() {
  return request.get('/api/qywx/tag/records')
}
