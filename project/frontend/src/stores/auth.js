import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getSessionInfo } from '@/api/auth'
import router from '@/router'
import { useRefStore } from '@/stores/reference'

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
    async login(loginForm) {
      const res = await loginApi(loginForm)
      this.isAuthenticated = true
      await this.fetchSessionInfo()
      return res
    },

    async fetchSessionInfo() {
      try {
        const res = await getSessionInfo()
        const data = res.data
        this.username = data.username
        this.nickname = data.nickname
        this.userId = data.userId
        this.deptName = data.deptName || ''
        this.permissions = data.permissions || []
        this.isAuthenticated = true
        saveCache(this.$state)
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