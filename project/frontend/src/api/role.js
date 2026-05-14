import request from './request'

export function getRolePage(params) {
  return request.get('/api/role/page', { params })
}

export function getRoleList() {
  return request.get('/api/role/list')
}

export function getRole(id) {
  return request.get(`/api/role/${id}`)
}

export function createRole(data) {
  return request.post('/api/role', data)
}

export function updateRole(id, data) {
  return request.put(`/api/role/${id}`, data)
}

export function deleteRole(id) {
  return request.delete(`/api/role/${id}`)
}

export function getRolePermissions(id) {
  return request.get(`/api/role/${id}/permissions`)
}

export function updateRolePermissions(id, menuIds) {
  return request.put(`/api/role/${id}/permissions`, menuIds)
}
