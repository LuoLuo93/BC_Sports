<template>
  <div class="sidebar-menu-items">
    <!-- Dashboard -->
    <div
      v-if="level === 0"
      class="menu-item"
      :class="{ active: isActive('/') }"
      @click="navigate('/')"
    >
      <el-icon><Odometer /></el-icon>
      <span v-show="!collapsed">仪表盘</span>
    </div>

    <template v-for="(menu, idx) in visibleMenus" :key="menu.id">
      <!-- Section divider between top-level groups -->
      <div v-if="level === 0 && idx > 0" class="menu-section-divider"></div>
      <!-- Directory: has visible children -->
      <template v-if="isDirectory(menu)">
        <div
          class="menu-item directory"
          :class="{ active: isMenuActive(menu), open: openMenus.includes(menu.id) }"
          @click="toggleMenu(menu)"
        >
          <el-icon :style="iconStyle(menu)">
            <component :is="getMenuIcon(menu.icon)" />
          </el-icon>
          <span v-show="!collapsed" class="menu-text">{{ menu.menuName }}</span>
          <el-icon v-show="!collapsed" class="arrow" :class="{ rotated: openMenus.includes(menu.id) }">
            <ArrowDown />
          </el-icon>
        </div>
        <transition name="submenu">
          <div class="submenu" v-show="openMenus.includes(menu.id) && !collapsed">
            <sidebar-menu
              :menus="menu.children"
              :collapsed="collapsed"
              :level="level + 1"
              :parent-path="menu.path"
            />
          </div>
        </transition>
      </template>

      <!-- Leaf menu item (menuType=1) -->
      <div
        v-else
        class="menu-item"
        :class="{ active: isMenuActive(menu) }"
        :style="level > 0 ? { paddingLeft: (20 + level * 16) + 'px' } : {}"
        @click="navigate(menu.path)"
      >
        <el-icon :style="iconStyle(menu)">
          <component :is="getMenuIcon(menu.icon)" />
        </el-icon>
        <span v-show="!collapsed" class="menu-text">{{ menu.menuName }}</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Odometer, ArrowDown } from '@element-plus/icons-vue'
import { getMenuIcon, getIconColorStyle } from '@/utils/iconMap'

const props = defineProps({
  menus: { type: Array, default: () => [] },
  collapsed: { type: Boolean, default: false },
  level: { type: Number, default: 0 },
  parentPath: { type: String, default: '' }
})

const router = useRouter()
const route = useRoute()
const openMenus = ref([])

const currentPath = computed(() => route.path)

// Filter: only show visible items, exclude buttons (menuType=2)
const visibleMenus = computed(() => {
  return props.menus.filter(m => m.visible === 1 && m.menuType !== 2)
})

function iconStyle(menu) {
  return getIconColorStyle(menu.iconColor)
}

function isDirectory(menu) {
  if (menu.menuType === 0) return true  // Explicit directory type
  // Also treat as directory if it has visible children
  if (menu.children?.some(c => c.visible === 1 && c.menuType !== 2)) return true
  return false
}

function isActive(path) {
  const current = currentPath.value
  return current === path || (current === '/index' && path === '/')
}

function isMenuActive(menu) {
  if (!menu.path || menu.path === '#') return false
  const current = currentPath.value
  if (current === menu.path) return true
  if (current.startsWith(menu.path + '/') || current.startsWith(menu.path + '?')) return true
  return false
}

function toggleMenu(menu) {
  const idx = openMenus.value.indexOf(menu.id)
  if (idx >= 0) {
    openMenus.value.splice(idx, 1)
  } else {
    openMenus.value.push(menu.id)
  }
}

// Auto-expand parent menus when current route matches a child
function autoExpandForRoute() {
  const current = route.path
  const findAndExpand = (menus, ancestors = []) => {
    for (const menu of menus) {
      if (menu.visible !== 1 || menu.menuType === 2) continue

      const isDirectMatch = menu.path && (
        current === menu.path ||
        current.startsWith(menu.path + '/') ||
        current.startsWith(menu.path + '?')
      )

      if (isDirectMatch) {
        // Expand all ancestors
        for (const ancestor of ancestors) {
          if (!openMenus.value.includes(ancestor.id)) {
            openMenus.value.push(ancestor.id)
          }
        }
        return true
      }

      if (menu.children?.length) {
        const found = findAndExpand(menu.children, [...ancestors, menu])
        if (found) return true
      }
    }
    return false
  }

  findAndExpand(props.menus)
}

function navigate(path) {
  if (!path || path === '#') return
  if (path !== route.path) {
    router.push(path)
  }
}

// Watch route changes to auto-expand
watch(() => route.path, () => {
  nextTick(() => autoExpandForRoute())
}, { immediate: true })
</script>

<style scoped>
.sidebar-menu-items {
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 16px;
  margin: 1px 8px;
  color: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  font-size: 0.8125rem;
  font-weight: 500;
  transition: background 0.15s ease, border-left-color 0.15s ease;
  white-space: nowrap;
  border-radius: 8px;
  user-select: none;
  border-left: 3px solid transparent;
  position: relative;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.92);
}

.menu-item.active {
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
  border-left-color: #3b82f6;
  font-weight: 600;
  box-shadow: inset 3px 0 12px -4px rgba(59, 130, 246, 0.4);
}

.menu-item .menu-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-item .el-icon {
  transition: transform 0.2s ease;
}

.menu-item:hover .el-icon {
  transform: scale(1.08);
}

.menu-item.directory .arrow {
  margin-left: auto;
  transition: transform 0.25s ease;
  flex-shrink: 0;
  font-size: 12px;
}

.menu-item.directory .arrow.rotated {
  transform: rotate(180deg);
}

/* Submenu transition */
.submenu {
  overflow: hidden;
}

.submenu-enter-active {
  animation: submenu-slide 0.25s ease-out;
}

.submenu-leave-active {
  animation: submenu-slide 0.2s ease-in reverse;
}

@keyframes submenu-slide {
  from {
    max-height: 0;
    opacity: 0;
  }
  to {
    max-height: 500px;
    opacity: 1;
  }
}

.menu-section-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.06);
  margin: 8px 16px;
}

/* Light sidebar overrides */
.sidebar-light .sidebar-menu-items .menu-item {
  color: rgba(30, 41, 59, 0.65);
}

.sidebar-light .sidebar-menu-items .menu-item:hover {
  background: rgba(30, 41, 59, 0.06);
  color: rgba(30, 41, 59, 0.9);
}

.sidebar-light .sidebar-menu-items .menu-item.active {
  background: rgba(29, 78, 216, 0.08);
  color: var(--bc-primary);
  border-left-color: var(--bc-primary);
  box-shadow: inset 3px 0 12px -4px rgba(29, 78, 216, 0.25);
}

.sidebar-light .sidebar-menu-items .menu-section-divider {
  background: rgba(30, 41, 59, 0.08);
}

.sidebar-light .sidebar-menu-items .menu-item .arrow {
  color: rgba(30, 41, 59, 0.5);
}
</style>
