import request from './request'

// Channel Type
export function getChannelTypeTree() {
  return request.get('/api/channel-type/tree')
}

export function getChannelTypePage(params) {
  return request.get('/api/channel-type/page', { params })
}

export function getChannelType(id) {
  return request.get(`/api/channel-type/${id}`)
}

export function createChannelType(data) {
  return request.post('/api/channel-type', data)
}

export function updateChannelType(id, data) {
  return request.put(`/api/channel-type/${id}`, data)
}

export function deleteChannelType(id) {
  return request.delete(`/api/channel-type/${id}`)
}

// Channel Nature
export function getChannelNatureTree() {
  return request.get('/api/channel-nature/tree')
}

export function getChannelNaturePage(params) {
  return request.get('/api/channel-nature/page', { params })
}

export function getChannelNature(id) {
  return request.get(`/api/channel-nature/${id}`)
}

export function createChannelNature(data) {
  return request.post('/api/channel-nature', data)
}

export function updateChannelNature(id, data) {
  return request.put(`/api/channel-nature/${id}`, data)
}

export function deleteChannelNature(id) {
  return request.delete(`/api/channel-nature/${id}`)
}

// Entity Channel
export function getEntityChannelPage(params) {
  return request.get('/api/entity-channel/page', { params })
}

export function getEntityChannel(id) {
  return request.get(`/api/entity-channel/${id}`)
}

export function createEntityChannel(data) {
  return request.post('/api/entity-channel', data)
}

export function updateEntityChannel(id, data) {
  return request.put(`/api/entity-channel/${id}`, data)
}

export function deleteEntityChannel(id) {
  return request.delete(`/api/entity-channel/${id}`)
}

export function getEntityChannelListByEntity(externalId, entityType) {
  return request.get('/api/entity-channel/list-by-entity', { params: { externalId, entityType } })
}

export function batchSaveEntityChannel(externalId, entityType, data) {
  return request.post('/api/entity-channel/batch-save', data, { params: { externalId, entityType } })
}

export function getEntityChannelTemplate() {
  return request.get('/api/entity-channel/template', { responseType: 'blob' })
}

export function importEntityChannel(data) {
  return request.post('/api/entity-channel/import', data, { timeout: 300000 })
}
