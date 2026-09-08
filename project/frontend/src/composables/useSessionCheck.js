import { ref, onMounted, onUnmounted } from 'vue'
import { checkSession } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useTabStore } from '@/stores/tab'
import router from '@/router'
import { ElMessageBox } from 'element-plus'

export function useSessionCheck() {
  const authStore = useAuthStore()
  const tabStore = useTabStore()
  let timer = null
  let failCount = 0
  let kickedOut = false
  let inFlight = false

  function onVisibilityChange() {
    if (document.hidden) {
      stopCheck()
    } else if (authStore.isAuthenticated) {
      startCheck()
    }
  }

  function handleKickedOut(msg) {
    if (kickedOut) return
    kickedOut = true
    authStore.clearAuth()
    tabStore.clearAll()
    ElMessageBox.alert(msg || '您的账号已在其他设备登录', '登录提示', {
      confirmButtonText: '重新登录',
      type: 'warning'
    }).then(() => {
      router.push('/login?kicked=1')
    })
  }

  function startCheck(interval = 10000) {
    if (timer) return
    timer = setInterval(async () => {
      if (inFlight) return
      inFlight = true
      try {
        const res = await checkSession()
        failCount = 0
        if (res.code !== 200) {
          handleKickedOut(res.message)
        }
      } catch {
        failCount++
        if (failCount >= 3) {
          handleKickedOut('网络连接异常，请重新登录')
        }
      } finally {
        inFlight = false
      }
    }, interval)
  }

  function stopCheck() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  onMounted(() => {
    if (authStore.isAuthenticated) {
      startCheck()
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
  })

  onUnmounted(() => {
    stopCheck()
    document.removeEventListener('visibilitychange', onVisibilityChange)
  })

  return { startCheck, stopCheck }
}
