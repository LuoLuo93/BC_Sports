import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { ElMessage } from 'element-plus'

const request = axios.create({
  timeout: 60000,
  withCredentials: true
})

let isLoggingOut = false

export function setLoggingOut(value) {
  isLoggingOut = value
}

// CSRF Token 缓存（仅存储在内存中，不使用 localStorage）
let csrfToken = null

/**
 * 刷新 CSRF Token
 */
export async function refreshCsrfToken() {
  try {
    const res = await request.get('/api/csrf')
    if (res.code === 200 && res.data) {
      csrfToken = res.data
    }
  } catch (e) {
    console.warn('获取 CSRF Token 失败:', e)
  }
  return csrfToken
}

/**
 * 清除 CSRF Token（登出时调用）
 */
export function clearCsrfToken() {
  csrfToken = null
}

request.interceptors.request.use(config => {
  if (!(config.data instanceof FormData)) {
    config.headers['Content-Type'] = 'application/json'
  }
  // 注入 CSRF Token
  if (csrfToken) {
    config.headers['X-CSRF-Token'] = csrfToken
  }
  return config
})

request.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      const authStore = useAuthStore()
      authStore.clearAuth()
      clearCsrfToken() // 清除 CSRF Token
      if (!isLoggingOut && router.currentRoute.value.path !== '/login') {
        ElMessage.error(res.message || '登录已过期，请重新登录')
      }
      router.push('/login')
      return Promise.reject(new Error(res.message || '未登录'))
    }
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  async error => {  // 修复：添加 async 关键字
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const authStore = useAuthStore()
        authStore.clearAuth()
        clearCsrfToken() // 清除 CSRF Token
        if (!isLoggingOut && router.currentRoute.value.path !== '/login') {
          ElMessage.error('登录已过期，请重新登录')
        }
        router.push('/login')
      } else if (status === 403) {
        // 检查是否是 CSRF Token 错误
        const errorMsg = error.response.data?.message || ''
        if (errorMsg.includes('CSRF')) {
          // 刷新 CSRF Token 并提示用户重试
          await refreshCsrfToken()
          ElMessage.warning('CSRF Token 已刷新，请重试操作')
        } else {
          ElMessage.error('没有操作权限')
        }
      } else {
        const msg = error.response.data?.message || error.response.data?.msg || '网络请求失败'
        ElMessage.error(msg)
      }
    } else {
      ElMessage.error('网络连接异常')
    }
    return Promise.reject(error)
  }
)

export default request
