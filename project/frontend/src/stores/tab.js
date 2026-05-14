import { defineStore } from 'pinia'

export const useTabStore = defineStore('tab', {
  state: () => ({
    tabs: [],
    activeTab: '/'
  }),

  actions: {
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
      if (this.tabs.length > 20) {
        this.tabs.shift()
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
    },

    initDashboard() {
      if (!this.tabs.find(t => t.path === '/')) {
        this.tabs.unshift({ path: '/', title: '仪表盘', icon: 'Odometer' })
      }
      this.activeTab = '/'
    }
  }
})
