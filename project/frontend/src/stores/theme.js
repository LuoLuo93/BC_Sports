import { defineStore } from 'pinia'
import { ref } from 'vue'

const CACHE_KEY = 'bc_theme_cache'

function loadCache() {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

const cached = loadCache()

export const useThemeStore = defineStore('theme', () => {
  const themeMode = ref(cached.themeMode || 'light')
  const primaryColor = ref(cached.primaryColor || '#1d4ed8')
  const sidebarStyle = ref(cached.sidebarStyle || 'dark')
  const logoUrl = ref(cached.logoUrl || '')

  function saveCache() {
    try {
      localStorage.setItem(CACHE_KEY, JSON.stringify({
        themeMode: themeMode.value,
        primaryColor: primaryColor.value,
        sidebarStyle: sidebarStyle.value,
        logoUrl: logoUrl.value
      }))
    } catch { /* ignore */ }
  }

  function applyTheme() {
    let mode = themeMode.value
    if (mode === 'auto') {
      mode = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }
    document.documentElement.setAttribute('data-theme', mode)
    applyPrimaryColor(primaryColor.value)
    saveCache()
  }

  return { themeMode, primaryColor, sidebarStyle, logoUrl, applyTheme, saveCache }
})

export function applyPrimaryColor(color) {
  if (!color) return
  const root = document.documentElement
  root.style.setProperty('--el-color-primary', color)

  const r = parseInt(color.slice(1, 3), 16)
  const g = parseInt(color.slice(3, 5), 16)
  const b = parseInt(color.slice(5, 7), 16)

  function lighten(pct) {
    return '#' + [r, g, b].map(c =>
      Math.min(255, c + Math.round((255 - c) * pct)).toString(16).padStart(2, '0')
    ).join('')
  }

  function darken(pct) {
    return '#' + [r, g, b].map(c =>
      Math.max(0, Math.round(c * (1 - pct))).toString(16).padStart(2, '0')
    ).join('')
  }

  root.style.setProperty('--el-color-primary-light-3', lighten(0.3))
  root.style.setProperty('--el-color-primary-light-5', lighten(0.5))
  root.style.setProperty('--el-color-primary-light-7', lighten(0.7))
  root.style.setProperty('--el-color-primary-light-9', lighten(0.9))
  root.style.setProperty('--el-color-primary-dark-2', darken(0.2))
  root.style.setProperty('--bc-primary', color)
  root.style.setProperty('--bc-primary-light', lighten(0.2))
}
