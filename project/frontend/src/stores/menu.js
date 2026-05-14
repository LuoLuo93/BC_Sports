import { defineStore } from 'pinia'
import { getUserMenuTree } from '@/api/menu'

export const useMenuStore = defineStore('menu', {
  state: () => ({
    menuTree: [],
    sidebarCollapsed: false,
    loaded: false
  }),

  actions: {
    async loadMenuTree() {
      if (this.loaded) return
      try {
        const res = await getUserMenuTree()
        this.menuTree = res.data || []
        this.loaded = true
      } catch {
        this.menuTree = []
      }
    },

    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },

    clearMenu() {
      this.menuTree = []
      this.loaded = false
    }
  }
})
