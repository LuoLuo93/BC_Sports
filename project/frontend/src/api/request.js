import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/bcsports',
  timeout: 60000,
  withCredentials: true
})

let isLoggingOut = false
// 登录流程中标志：避免 /doLogin 后的请求（如拉用户信息）偶发 401 时，拦截器抢着把用户打回 /login
let isLoggingIn = false
// 会话过期单飞标志：页面加载时 N 个并发请求同时 401，只弹一次提示、只跳一次登录页
let redirecting401 = false

function handleSessionExpired(message) {
  const authStore = useAuthStore()
  authStore.clearAuth()
  if (!isLoggingOut && router.currentRoute.value.path !== '/login') {
    if (!redirecting401) {
      redirecting401 = true
      ElMessage.error(message || '登录已过期，请重新登录')
      router.push('/login').finally(() => { redirecting401 = false })
    }
  }
}

export function setLoggingOut(value) {
  isLoggingOut = value
}

export function setLoggingIn(value) {
  isLoggingIn = value
}

// CSRF 防护由会话 Cookie 的 SameSite=STRICT 承担（后端 ShiroConfig 配置），
// 已移除前端 token 机制与后端 CsrfFilter（该过滤器 URL 模式带 context-path 前缀，从未生效）

request.interceptors.request.use(config => {
  if (!(config.data instanceof FormData)) {
    config.headers['Content-Type'] = 'application/json'
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
      // 登录流程中的 401 不抢跳转（交给 auth store 的重试/异常处理），避免首次登录被踢回登录页
      if (isLoggingIn) {
        return Promise.reject(new Error(res.message || '未登录'))
      }
      handleSessionExpired(res.message)
      return Promise.reject(new Error(res.message || '未登录'))
    }
    // grouping: 多个并发请求同时失败时合并为一条提示，不再连弹 N 个 toast
    ElMessage({ type: 'error', message: res.message || '操作失败', grouping: true })
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  async error => {  // 修复：添加 async 关键字
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        if (!isLoggingIn) {
          handleSessionExpired()
        }
      } else if (status === 403) {
        ElMessage({ type: 'error', message: '没有操作权限', grouping: true })
      } else {
        const msg = error.response.data?.message || error.response.data?.msg || '网络请求失败'
        ElMessage({ type: 'error', message: msg, grouping: true })
      }
    } else {
      ElMessage.error('网络连接异常')
    }
    return Promise.reject(error)
  }
)

export default request
