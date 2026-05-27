import { ref, computed, onMounted, onUnmounted } from 'vue'

const width = ref(window.innerWidth)
let listeners = 0

function onResize() {
  width.value = window.innerWidth
}

const isMobile = computed(() => width.value < 768)
const isTablet = computed(() => width.value >= 768 && width.value < 1024)
const isDesktop = computed(() => width.value >= 1024)

export function useResponsive() {
  onMounted(() => {
    listeners++
    if (listeners === 1) window.addEventListener('resize', onResize)
  })

  onUnmounted(() => {
    listeners--
    if (listeners === 0) window.removeEventListener('resize', onResize)
  })

  return { width, isMobile, isTablet, isDesktop }
}
