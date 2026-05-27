import request from './request'

export function getDictTypeList(params) {
  return request.get('/api/dict/type/list', { params })
}

export function getDictType(id) {
  return request.get(`/api/dict/type/${id}`)
}

export function createDictType(data) {
  return request.post('/api/dict/type', data)
}

export function updateDictType(data) {
  return request.put('/api/dict/type', data)
}

export function deleteDictType(id) {
  return request.delete(`/api/dict/type/${id}`)
}

export function getDictDataList(params) {
  return request.get('/api/dict/data/list', { params })
}

export function getDictDataPage(params) {
  return request.get('/api/dict/data/page', { params })
}

export function getDictData(id) {
  return request.get(`/api/dict/data/${id}`)
}

export function createDictData(data) {
  return request.post('/api/dict/data', data)
}

export function updateDictData(data) {
  return request.put('/api/dict/data', data)
}

export function deleteDictData(id) {
  return request.delete(`/api/dict/data/${id}`)
}
