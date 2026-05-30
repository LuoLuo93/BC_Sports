import { defineStore } from 'pinia'
import { getDictDataList } from '@/api/dict'

const CACHE_KEY = 'bc_dict_cache'
const TTL_MS = 60 * 60 * 1000 // 1 小时

function loadCache() {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    // 检查整体缓存是否过期
    if (parsed._ts && Date.now() - parsed._ts > TTL_MS) {
      localStorage.removeItem(CACHE_KEY)
      return {}
    }
    return parsed._ts ? (parsed.data || {}) : parsed
  } catch {
    return {}
  }
}

function saveCache(data) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({ data, _ts: Date.now() }))
  } catch {
    // localStorage full or unavailable — silently ignore
  }
}

export const useDictStore = defineStore('dict', {
  state: () => ({
    dictMap: loadCache()
  }),

  getters: {
    getDictData: (state) => {
      return (dictType) => state.dictMap[dictType] || []
    }
  },

  actions: {
    async loadDict(dictType) {
      if (this.dictMap[dictType]) return this.dictMap[dictType]
      try {
        const res = await getDictDataList({ dictType })
        const data = res.data || []
        this.dictMap[dictType] = data
        saveCache(this.dictMap)
        return data
      } catch {
        return []
      }
    },

    clearCache() {
      this.dictMap = {}
      localStorage.removeItem(CACHE_KEY)
    }
  }
})