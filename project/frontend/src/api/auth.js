import request from './request'

export async function login(data) {
  const res = await request.post('/doLogin', data)
  return res
}

export async function logout() {
  try {
    await request.post('/doLogout')
  } finally {
    // 登出处理由 auth store 完成
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
