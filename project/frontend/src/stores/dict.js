import { defineStore } from 'pinia'
import { getDictDataList } from '@/api/dict'

const CACHE_KEY = 'bc_dict_cache_v2'
const TTL_MS = 60 * 60 * 1000 // 1 小时

function loadCache() {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return {}
    return JSON.parse(raw)
  } catch {
    return {}
  }
}

function saveCache(cache) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(cache))
  } catch {
    // localStorage full — silently ignore
  }
}

function isExpired(entry) {
  return !entry || !entry._ts || Date.now() - entry._ts > TTL_MS
}

const cache = loadCache()

export const useDictStore = defineStore('dict', {
  state: () => ({
    dictMap: cache
  }),

  getters: {
    getDictData: (state) => {
      return (dictType) => {
        const entry = state.dictMap[dictType]
        return entry && !isExpired(entry) ? entry.data : []
      }
    }
  },

  actions: {
    async loadDict(dictType) {
      const entry = this.dictMap[dictType]
      if (entry && !isExpired(entry)) return entry.data

      // stale-while-revalidate: 如果有旧数据先返回，后台刷新
      const staleData = entry?.data || []

      try {
        const res = await getDictDataList({ dictType })
        const data = res.data || []
        this.dictMap[dictType] = { data, _ts: Date.now() }
        saveCache(this.dictMap)
        return data
      } catch {
        return staleData
      }
    },

    clearCache() {
      this.dictMap = {}
      localStorage.removeItem(CACHE_KEY)
    }
  }
})
