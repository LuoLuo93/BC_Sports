import request from './request'

export function getRegionTree() {
  return request.get('/api/region/tree')
}

export function getRegionPage(params) {
  return request.get('/api/region/page', { params })
}

export function getRegion(id) {
  return request.get(`/api/region/${id}`)
}

export function createRegion(data) {
  return request.post('/api/region', data)
}

export function updateRegion(id, data) {
  return request.put(`/api/region/${id}`, data)
}

export function deleteRegion(id) {
  return request.delete(`/api/region/${id}`)
}
