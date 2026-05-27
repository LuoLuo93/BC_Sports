import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { ElMessage } from 'element-plus'

const request = axios.create({
  timeout: 60000,
  withCredentials: true
})

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
    // Business error (400, 401, 403, 404, 500 with code)
    if (res.code === 401) {
      const authStore = useAuthStore()
      authStore.clearAuth()
      router.push('/login')
      ElMessage.error(res.message || '登录已过期，请重新登录')
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
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error('没有操作权限')
      } else {
        ElMessage.error('网络请求失败')
      }
    } else {
      ElMessage.error('网络连接异常')
    }
    return Promise.reject(error)
  }
)

export default request
