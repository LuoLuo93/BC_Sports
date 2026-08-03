import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getSessionInfo } from '@/api/auth'
import router from '@/router'
import { useRefStore } from '@/stores/reference'
import { setLoggingIn } from '@/api/request'

const CACHE_KEY = 'bc_auth_cache'

function loadCache() {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function saveCache(state) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({
      isAuthenticated: state.isAuthenticated,
      username: state.username,
      nickname: state.nickname,
      userId: state.userId,
      deptName: state.deptName,
      permissions: state.permissions
    }))
  } catch {
    // localStorage full or unavailable — silently ignore
  }
}

function clearCache() {
  localStorage.removeItem(CACHE_KEY)
}

const cached = loadCache()

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isAuthenticated: cached.isAuthenticated || false,
    username: cached.username || '',
    nickname: cached.nickname || '',
    userId: cached.userId || '',
    deptName: cached.deptName || '',
    permissions: cached.permissions || []
  }),

  getters: {
    hasPermission: (state) => {
      return (perm) => {
        if (!state.permissions || state.permissions.length === 0) return false
        if (state.permissions.includes('*')) return true
        return state.permissions.includes(perm)
      }
    }
  },

  actions: {
    /** 把会话信息 data 填入 store 并持久化（login/fetchSessionInfo 复用） */
    applySessionInfo(data) {
      this.username = data.username
      this.nickname = data.nickname
      this.userId = data.userId
      this.deptName = data.deptName || ''
      this.permissions = data.permissions || []
      this.isAuthenticated = true
      saveCache(this.$state)
    },

    async login(loginForm) {
      // 标记"登录中"，防止 request 拦截器在登录流程内的 401 抢着跳 /login
      setLoggingIn(true)
      try {
        const res = await loginApi(loginForm)
        // 优先用 /doLogin 响应体里直接返回的用户信息（后端已合并返回）
        if (res && res.data && res.data.username) {
          this.applySessionInfo(res.data)
        } else {
          // 兜底：旧后端未返回用户信息时，再单独拉一次（登录竞态场景，失败重试一次）
          await this.fetchSessionInfo(true)
        }
        return res
      } finally {
        setLoggingIn(false)
      }
    },

    /**
     * 拉取当前登录用户信息。
     * @param retryOnFail 首次失败是否重试一次（带 300ms 延时）。
     *   仅登录流程兜底场景传 true（兼容 JSESSIONID Cookie 刚写入、首个请求来不及带上而 401 的竞态）；
     *   路由守卫刷新页面场景传 false（用户本就未登录时不应无谓重试）。
     */
    async fetchSessionInfo(retryOnFail = false) {
      try {
        const res = await getSessionInfo()
        this.applySessionInfo(res.data)
        return
      } catch {
        if (!retryOnFail) throw new Error('获取用户信息失败')
        // 首次失败：等 Cookie 落地后重试一次
      }
      try {
        await new Promise(r => setTimeout(r, 300))
        const res = await getSessionInfo()
        this.applySessionInfo(res.data)
      } catch {
        this.clearAuth()
        throw new Error('获取用户信息失败')
      }
    },

    async logout() {
      try {
        await logoutApi()
      } finally {
        this.clearAuth()
        router.push('/login')
      }
    },

    clearAuth() {
      this.isAuthenticated = false
      this.username = ''
      this.nickname = ''
      this.userId = ''
      this.deptName = ''
      this.permissions = []
      clearCache()
      try { useRefStore().clearCache() } catch { /* store may not be ready */ }
    }
  }
})