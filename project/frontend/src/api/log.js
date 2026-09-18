import request from './request'

export function getLogPage(params) {
  return request.get('/api/log/page', { params })
}

export function getLogModules() {
  return request.get('/api/log/modules')
}

export function cleanLogs(days) {
  return request.delete('/api/log/clean', { params: { days } })
}
