import request from './request'

export function getDeptList(params) {
  return request.get('/api/dept/list', { params })
}

export function getDept(id) {
  return request.get(`/api/dept/${id}`)
}

export function createDept(data) {
  return request.post('/api/dept', data)
}

export function updateDept(id, data) {
  return request.put(`/api/dept/${id}`, data)
}

export function deleteDept(id) {
  return request.delete(`/api/dept/${id}`)
}
