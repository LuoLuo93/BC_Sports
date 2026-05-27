import request from './request'

export function getOnlineUserList() {
  return request.get('/api/online-user/list')
}

export function kickOnlineUser(username) {
  return request.post(`/api/online-user/kick/${username}`)
}