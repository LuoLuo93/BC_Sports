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
      const authStore = useAuthStore()
      authStore.clearAuth()
      if (!isLoggingOut && router.currentRoute.value.path !== '/login') {
        ElMessage.error(res.message || '登录已过期，请重新登录')
      }
      router.push('/login')
      return Promise.reject(new Error(res.message || '未登录'))
    }
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const authStore = useAuthStore()
        authStore.clearAuth()
        if (!isLoggingOut && router.currentRoute.value.path !== '/login') {
          ElMessage.error('登录已过期，请重新登录')
        }
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('没有操作权限')
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
