import request from './request'

export function getUserPage(params) {
  return request.get('/api/user/page', { params })
}

export function getUser(id) {
  return request.get(`/api/user/${id}`)
}

export function getProfile() {
  return request.get('/api/user/profile')
}

export function updateProfile(data) {
  return request.put('/api/user/profile', data)
}

export function createUser(data) {
  return request.post('/api/user', data)
}

export function updateUser(id, data) {
  return request.put(`/api/user/${id}`, data)
}

export function deleteUser(id) {
  return request.delete(`/api/user/${id}`)
}

export function resetPassword(id, data) {
  return request.put(`/api/user/${id}/resetPassword`, data)
}

export function changePassword(id, data) {
  return request.put(`/api/user/${id}/changePassword`, data)
}

export function getUserRoles(id) {
  return request.get(`/api/user/${id}/roles`)
}

export function updateUserRoles(id, roleIds) {
  return request.put(`/api/user/${id}/roles`, roleIds)
}
