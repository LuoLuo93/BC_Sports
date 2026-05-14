<template>
  <div class="sidebar-menu-items">
    <!-- Dashboard -->
    <div
      class="menu-item"
      :class="{ active: isActive('/') }"
      @click="navigate('/')"
    >
      <el-icon><Odometer /></el-icon>
      <span v-show="!collapsed">仪表盘</span>
    </div>

    <template v-for="menu in visibleMenus" :key="menu.id">
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
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Odometer, ArrowDown,
  Menu as MenuIcon, Setting, User, Key, Lock,
  Document, Folder, FolderOpened, DataBoard,
  List, Grid, Histogram, TrendCharts, Timer,
  Connection, OfficeBuilding, House, Shop,
  HomeFilled, Location, MapLocation, Edit, Delete,
  Plus, Search, Download, Upload, Bell,
  Calendar, ChatDotRound, Management,
  Monitor, DataAnalysis, Opportunity, Avatar, Tools
} from '@element-plus/icons-vue'

const props = defineProps({
  menus: { type: Array, default: () => [] },
  collapsed: { type: Boolean, default: false },
  level: { type: Number, default: 0 },
  parentPath: { type: String, default: '' }
})

const router = useRouter()
const route = useRoute()
const openMenus = ref([])

// Filter: only show visible items, exclude buttons (menuType=2)
const visibleMenus = computed(() => {
  return props.menus.filter(m => m.visible === 1 && m.menuType !== 2)
})

// Map Bootstrap Icons (bi-*) to Element Plus icons
const biIconMap = {
  'bi-speedometer2': Odometer,
  'bi-compass': Location,
  'bi-people': User,
  'bi-person': User,
  'bi-person-circle': Avatar,
  'bi-key': Key,
  'bi-lock': Lock,
  'bi-shield-lock': Lock,
  'bi-gear': Setting,
  'bi-sliders': Tools,
  'bi-folder': Folder,
  'bi-folder-open': FolderOpened,
  'bi-file-earmark': Document,
  'bi-file-text': Document,
  'bi-list': List,
  'bi-grid': Grid,
  'bi-house': HomeFilled,
  'bi-building': OfficeBuilding,
  'bi-shop': Shop,
  'bi-cart': Shop,
  'bi-geo-alt': MapLocation,
  'bi-bar-chart': Histogram,
  'bi-bar-chart-line': TrendCharts,
  'bi-graph-up': TrendCharts,
  'bi-activity': DataAnalysis,
  'bi-clock-history': Timer,
  'bi-bell': Bell,
  'bi-calendar': Calendar,
  'bi-chat-dots': ChatDotRound,
  'bi-clipboard-data': DataBoard,
  'bi-diagram-3': Management,
  'bi-display': Monitor,
  'bi-lightning': Opportunity,
  'bi-puzzle': Grid,
  'bi-tree': List,
  'bi-database': DataAnalysis,
  'bi-hdd': DataBoard,
  'bi-cloud': Connection,
  'bi-globe': MapLocation,
  'bi-envelope': ChatDotRound,
  'bi-tag': List,
  'bi-tags': List,
  'bi-bookmark': List,
  'bi-eye': Search,
  'bi-pencil': Edit,
  'bi-trash': Delete,
  'bi-plus': Plus,
  'bi-search': Search,
  'bi-download': Download,
  'bi-upload': Upload,
  'bi-arrow-repeat': Connection,
  'bi-exclamation-triangle': Opportunity,
  'bi-info-circle': ChatDotRound,
  'bi-question-circle': ChatDotRound,
  'bi-box-arrow-right': Connection,
  'bi-menu-button-wide': MenuIcon,
  'bi-layout-sidebar': MenuIcon,
  'bi-table': Grid,
  'bi-card-list': List
}

function getMenuIcon(icon) {
  if (!icon) return Document
  const mapped = biIconMap[icon]
  if (mapped) return mapped
  // Strip "bi-" prefix and try to find a close match
  return Document
}

function iconStyle(menu) {
  if (!menu.iconColor) return {}
  const colorMap = {
    'primary': '#3b82f6',
    'success': '#10b981',
    'warning': '#f59e0b',
    'danger': '#ef4444',
    'info': '#6b7280',
    'blue': '#3b82f6',
    'green': '#10b981',
    'yellow': '#f59e0b',
    'red': '#ef4444',
    'orange': '#f97316',
    'purple': '#8b5cf6',
    'pink': '#ec4899',
    'cyan': '#06b6d4'
  }
  const color = colorMap[menu.iconColor] || menu.iconColor
  return { color }
}

function isDirectory(menu) {
  if (menu.menuType === 0) return true  // Explicit directory type
  // Also treat as directory if it has visible children
  if (menu.children?.some(c => c.visible === 1 && c.menuType !== 2)) return true
  return false
}

function isActive(path) {
  const current = route.path
  return current === path || (current === '/index' && path === '/')
}

function isMenuActive(menu) {
  if (!menu.path || menu.path === '#') return false
  const current = route.path
  if (current === menu.path) return true
  // Exact prefix match (avoid /bi/brand matching /bi/brand-something)
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
  autoExpandForRoute()
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
  padding: 10px 20px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
  border-left: 3px solid transparent;
  user-select: none;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.9);
}

.menu-item.active {
  background: rgba(29, 78, 216, 0.15);
  color: #60a5fa;
  border-left-color: #3b82f6;
  font-weight: 600;
}

.menu-item .menu-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-item.directory .arrow {
  margin-left: auto;
  transition: transform 0.25s ease;
  flex-shrink: 0;
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
</style>
