import request from './request'

export function getMenuTree() {
  return request.get('/api/menu/tree')
}

export function getUserMenuTree() {
  return request.get('/api/menu/userTree')
}

export function getMenuChildren(parentId) {
  return request.get('/api/menu/children', { params: { parentId } })
}

export function getMenu(id) {
  return request.get(`/api/menu/${id}`)
}

export function createMenu(data) {
  return request.post('/api/menu', data)
}

export function updateMenu(id, data) {
  return request.put(`/api/menu/${id}`, data)
}

export function deleteMenu(id) {
  return request.delete(`/api/menu/${id}`)
}
