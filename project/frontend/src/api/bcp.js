import request from './request'

// BC好玩家 - 运动积分
export function getSportPointsPage(params) {
  return request.get('/api/bcp/sport-points/page', { params })
}

export function importSportPoints(data) {
  return request.post('/api/bcp/sport-points/import', data, { timeout: 600000 })
}

export function getSportPointsTemplate() {
  return request.get('/api/bcp/sport-points/template', { responseType: 'blob' })
}

export function getSportPointsImportLogPage(params) {
  return request.get('/api/bcp/sport-points/import-log/page', { params })
}

export function updateSportPoints(id, data) {
  return request.put(`/api/bcp/sport-points/${id}`, data)
}

// 管理员代传自定义头像（FormData，multipart）
export function uploadSportPointsAvatar(id, data) {
  return request.post(`/api/bcp/sport-points/${id}/avatar`, data, { timeout: 60000 })
}

// 清除自定义头像（回退移动端默认动物emoji）
export function clearSportPointsAvatar(id) {
  return request.delete(`/api/bcp/sport-points/${id}/avatar`)
}
