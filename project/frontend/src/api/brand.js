import request from './request'

export function getBrandPage(params) {
  return request.get('/api/brand/page', { params })
}

export function getBrandList() {
  return request.get('/api/brand/list')
}

export function getBrand(id) {
  return request.get(`/api/brand/${id}`)
}

export function createBrand(data) {
  return request.post('/api/brand', data)
}

export function updateBrand(id, data) {
  return request.put(`/api/brand/${id}`, data)
}

export function deleteBrand(id) {
  return request.delete(`/api/brand/${id}`)
}
