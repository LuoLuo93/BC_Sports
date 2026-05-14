import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getSessionInfo } from '@/api/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isAuthenticated: false,
    username: '',
    nickname: '',
    userId: '',
    permissions: []
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
        this.permissions = data.permissions || []
        this.isAuthenticated = true
      } catch {
        this.clearAuth()
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
      this.permissions = []
    }
  }
})
