import { useAuthStore } from '@/stores/auth'

export const vPermission = {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const required = binding.value
    if (required && !authStore.hasPermission(required)) {
      el.parentNode?.removeChild(el)
    }
  }
}
