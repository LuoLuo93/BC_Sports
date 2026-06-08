import request, { refreshCsrfToken, clearCsrfToken } from './request'

export async function login(data) {
  const res = await request.post('/doLogin', data)
  // 登录成功后获取 CSRF Token
  await refreshCsrfToken()
  return res
}

export async function logout() {
  try {
    await request.post('/doLogout')
  } finally {
    // 登出时清除 CSRF Token
    clearCsrfToken()
  }
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

export function getCsrfToken() {
  return request.get('/api/csrf')
}
