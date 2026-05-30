import { defineStore } from 'pinia'
import { getDeptList } from '@/api/dept'
import { getRoleList } from '@/api/role'
import { getMenuTree } from '@/api/menu'

const DEPT_KEY = 'bc_ref_dept'
const ROLE_KEY = 'bc_ref_role'
const MENU_KEY = 'bc_ref_menu_tree'
const TTL_MS = 60 * 60 * 1000 // 1 小时

function load(key) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    // 兼容旧格式（无时间戳）
    if (parsed && parsed._ts && Date.now() - parsed._ts > TTL_MS) {
      localStorage.removeItem(key)
      return null
    }
    return parsed && parsed._ts ? parsed.data : parsed
  } catch { return null }
}

function save(key, data) {
  try { localStorage.setItem(key, JSON.stringify({ data, _ts: Date.now() })) } catch { /* ignore */ }
}

function remove(key) {
  localStorage.removeItem(key)
}

export const useRefStore = defineStore('ref', {
  state: () => ({
    deptTree: load(DEPT_KEY),
    roleList: load(ROLE_KEY),
    fullMenuTree: load(MENU_KEY)
  }),

  getters: {
    getDeptTree: (state) => state.deptTree || [],
    getRoleList: (state) => state.roleList || [],
    getFullMenuTree: (state) => state.fullMenuTree || []
  },

  actions: {
    async loadDeptTree(force = false) {
      if (!force && this.deptTree) return this.deptTree
      try {
        const res = await getDeptList()
        this.deptTree = res.data || []
        save(DEPT_KEY, this.deptTree)
        return this.deptTree
      } catch { return [] }
    },

    async loadRoleList(force = false) {
      if (!force && this.roleList) return this.roleList
      try {
        const res = await getRoleList()
        this.roleList = res.data || []
        save(ROLE_KEY, this.roleList)
        return this.roleList
      } catch { return [] }
    },

    async loadFullMenuTree(force = false) {
      if (!force && this.fullMenuTree) return this.fullMenuTree
      try {
        const res = await getMenuTree()
        this.fullMenuTree = res.data || []
        save(MENU_KEY, this.fullMenuTree)
        return this.fullMenuTree
      } catch { return [] }
    },

    clearCache() {
      this.deptTree = null
      this.roleList = null
      this.fullMenuTree = null
      remove(DEPT_KEY)
      remove(ROLE_KEY)
      remove(MENU_KEY)
    }
  }
})