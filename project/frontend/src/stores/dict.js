import { defineStore } from 'pinia'
import { getDictDataList } from '@/api/dict'

const CACHE_KEY = 'bc_dict_cache'

function loadCache() {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function saveCache(data) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(data))
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