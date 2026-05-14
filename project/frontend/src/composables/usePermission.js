import { useAuthStore } from '@/stores/auth'

export function usePermission() {
  const authStore = useAuthStore()

  const hasPermission = (perm) => {
    return authStore.hasPermission(perm)
  }

  return { hasPermission }
}
