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
