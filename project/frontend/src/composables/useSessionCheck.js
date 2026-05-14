import { ref, onMounted, onUnmounted } from 'vue'
import { checkSession } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { ElMessageBox } from 'element-plus'

export function useSessionCheck() {
  const authStore = useAuthStore()
  let timer = null
  let failCount = 0
  let kickedOut = false

  function handleKickedOut(msg) {
    if (kickedOut) return
    kickedOut = true
    authStore.clearAuth()
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
  })

  onUnmounted(() => {
    stopCheck()
  })

  return { startCheck, stopCheck }
}
