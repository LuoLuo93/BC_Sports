import { defineStore } from 'pinia'

const MAX_TABS = 15

export const useTabStore = defineStore('tab', {
  state: () => ({
    tabs: [],
    activeTab: '/'
  }),

  actions: {
    clearAll() {
      this.tabs = []
      this.activeTab = '/'
    },

    addTab(tab) {
      const existing = this.tabs.find(t => t.path === tab.path)
      if (existing) {
        existing.title = tab.title || existing.title
        existing.icon = tab.icon || existing.icon
        return
      }
      this.tabs.push({
        path: tab.path,
        title: tab.title || tab.path,
        icon: tab.icon || ''
      })
      while (this.tabs.length > MAX_TABS) {
        const removableIndex = this.tabs.findIndex(
          t => t.path !== '/' && t.path !== this.activeTab
        )
        if (removableIndex === -1) break
        this.tabs.splice(removableIndex, 1)
      }
    },

    removeTab(path) {
      const index = this.tabs.findIndex(t => t.path === path)
      if (index === -1) return null
      const removed = this.tabs[index]
      this.tabs.splice(index, 1)
      return removed
    },

    setActiveTab(path) {
      this.activeTab = path
      const index = this.tabs.findIndex(t => t.path === path)
      if (index > -1 && index < this.tabs.length - 1) {
        const [tab] = this.tabs.splice(index, 1)
        this.tabs.push(tab)
      }
    },

    initDashboard() {
      if (!this.tabs.find(t => t.path === '/')) {
        this.tabs.unshift({ path: '/', title: '仪表盘', icon: 'Odometer' })
      }
      this.activeTab = '/'
    }
  }
})
