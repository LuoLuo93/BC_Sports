import request from './request'

export function login(data) {
  return request.post('/doLogin', data)
}

export function logout() {
  return request.post('/doLogout')
}

export function getSessionInfo() {
  return request.get('/api/session/info')
}

export function checkSession() {
  return request.get('/api/session/check')
}

export function getCaptcha() {
  return request.get('/api/captcha')
}
