import request from './request'

export function getPublicConfig() {
  return request.get('/api/config/public')
}

export function getConfigs() {
  return request.get('/api/config')
}

export function updateConfigs(data) {
  return request.put('/api/config', data)
}
