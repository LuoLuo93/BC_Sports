import { defineStore } from 'pinia'
import { getUserMenuTree } from '@/api/menu'

const CACHE_KEY = 'bc_menu_cache'

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
      menuTree: state.menuTree,
      sidebarCollapsed: state.sidebarCollapsed,
      loaded: state.loaded
    }))
  } catch {
    // localStorage full or unavailable — silently ignore
  }
}

function clearCache() {
  localStorage.removeItem(CACHE_KEY)
}

const cached = loadCache()

export const useMenuStore = defineStore('menu', {
  state: () => ({
    menuTree: cached.menuTree || [],
    sidebarCollapsed: cached.sidebarCollapsed || false,
    loaded: cached.loaded || false
  }),

  actions: {
    async loadMenuTree() {
      if (this.loaded && this.menuTree.length > 0) return
      try {
        const res = await getUserMenuTree()
        this.menuTree = res.data || []
        this.loaded = true
        saveCache(this.$state)
      } catch {
        this.menuTree = []
        this.loaded = false
      }
    },

    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      saveCache(this.$state)
    },

    clearMenu() {
      this.menuTree = []
      this.loaded = false
      clearCache()
    }
  }
})