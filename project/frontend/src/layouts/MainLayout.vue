<template>
  <div class="layout">
    <!-- Mobile sidebar backdrop -->
    <div v-if="mobileMenuOpen && isMobile" class="sidebar-backdrop" @click="closeMobileMenu"></div>

    <!-- Sidebar -->
    <aside class="sidebar" :class="{
      collapsed: menuStore.sidebarCollapsed,
      'sidebar-light': themeStore.sidebarStyle === 'light',
      'mobile-open': mobileMenuOpen && isMobile,
      'mobile-hidden': isMobile && !mobileMenuOpen
    }">
      <div class="sidebar-header">
        <img v-if="themeStore.logoUrl" :src="logoFullUrl" class="sidebar-logo" />
        <el-icon v-else :size="20"><Compass /></el-icon>
        <span v-show="!menuStore.sidebarCollapsed">B.C.SPORTS</span>
      </div>
      <div class="sidebar-menu">
        <sidebar-menu :menus="menuStore.menuTree" :collapsed="menuStore.sidebarCollapsed" />
      </div>
    </aside>

    <!-- Main -->
    <div class="main-content" :class="{ expanded: menuStore.sidebarCollapsed }">
      <!-- Top Navbar -->
      <header class="top-navbar">
        <div class="navbar-left">
          <el-button v-if="isMobile" :icon="Fold" text @click="toggleMobileMenu" />
          <el-button v-else :icon="Fold" text @click="menuStore.toggleSidebar()" />
        </div>
        <div class="navbar-center">
          <breadcrumb v-if="!isMobile" />
          <template v-else>
            <el-icon v-if="currentIcon" class="title-icon"><component :is="currentIcon" /></el-icon>
            <span class="page-title">{{ currentTitle }}</span>
          </template>
        </div>
        <div class="navbar-right">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-dropdown">
              <el-avatar :size="32" class="user-avatar">
                {{ authStore.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-info" v-if="!isMobile">
                <div class="user-name">{{ authStore.username || '用户' }}</div>
                <small>{{ authStore.nickname || '昵称' }}</small>
              </div>
              <el-icon v-else :size="16" class="mobile-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div class="dropdown-user-info">
                    <el-avatar :size="36" class="user-avatar">{{ authStore.nickname?.charAt(0) || 'U' }}</el-avatar>
                    <div>
                      <div class="dropdown-name">{{ authStore.nickname || '用户' }}</div>
                      <div class="dropdown-id">@{{ authStore.username }}</div>
                    </div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item v-if="hasPermission('system:config:query')" command="settings">
                  <el-icon><Setting /></el-icon>系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Tab Bar -->
      <div class="tab-bar" v-if="tabStore.tabs.length > 0">
        <div class="tab-bar-scroll" ref="tabScrollRef">
          <div
            v-for="tab in tabStore.tabs"
            :key="tab.path"
            class="tab-item"
            :class="{
              active: isTabActive(tab),
              pinned: tab.path === '/'
            }"
            @click="navigateTo(tab.path)"
          >
            <el-icon v-if="tab.icon" :size="12"><component :is="resolveTabIcon(tab.icon)" /></el-icon>
            <span>{{ tab.title }}</span>
            <el-icon
              v-if="tab.path !== '/'"
              :size="12"
              class="tab-close"
              @click.stop="closeTab(tab.path)"
            >
              <Close />
            </el-icon>
          </div>
        </div>
        <div class="tab-bar-actions" v-if="tabStore.tabs.length > 1">
          <el-button size="small" text @click="closeAllTabs">
            <el-icon :size="12"><CircleClose /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- Content -->
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <keep-alive :include="cachedViews">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </router-view>
      </div>

      <!-- Footer -->
      <footer class="footer">
        <span>&copy; 2026 {{ systemName }}</span>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, watch, onMounted, ref, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useTabStore } from '@/stores/tab'
import { useThemeStore } from '@/stores/theme'
import { usePermission } from '@/composables/usePermission'
import { useSessionCheck } from '@/composables/useSessionCheck'
import { setLoggingOut } from '@/api/request'
import { useResponsive } from '@/composables/useResponsive'
import { applyPublicConfig, systemName } from '@/utils/appConfig'
import { getPublicConfig } from '@/api/config'
import { ElMessageBox } from 'element-plus'
import { Fold, User, Setting, SwitchButton, Close, Compass, CircleClose, ArrowDown, Odometer } from '@element-plus/icons-vue'
import SidebarMenu from '@/components/SidebarMenu.vue'
import Breadcrumb from '@/components/Breadcrumb.vue'
import { getMenuIcon } from '@/utils/iconMap'

const iconMap = { Odometer }

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const menuStore = useMenuStore()
const tabStore = useTabStore()
const themeStore = useThemeStore()
const { hasPermission } = usePermission()
const { startCheck, stopCheck } = useSessionCheck()
const { isMobile, isTablet } = useResponsive()

const mobileMenuOpen = ref(false)
const tabScrollRef = ref(null)

const apiBase = import.meta.env.VITE_API_BASE || ''
const logoTimestamp = ref(Date.now())

const logoFullUrl = computed(() => {
  const url = themeStore.logoUrl
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:')) return url
  return apiBase + url + '?t=' + logoTimestamp.value
})

watch(() => themeStore.logoUrl, () => { logoTimestamp.value = Date.now() })

const pathToName = {
  '/': 'Dashboard',
  '/system/menu': 'MenuManagement',
  '/system/user': 'UserManagement',
  '/system/role': 'RoleManagement',
  '/system/dept': 'DeptManagement',
  '/system/dict': 'DictManagement',
  '/bi/management': 'BiManagement',
  '/bi/entity-channel': 'EntityChannel',
  '/bi/entity-channel/form': 'EntityChannelForm',
  '/bi/erp-shop': 'ErpShop',
  '/bi/erp-warehouse': 'ErpWarehouse',
  '/bi/erp-customer': 'ErpCustomer',
  '/ihr/employee-management': 'IhrEmployee',
  '/ihr/onboarding-management': 'IhrOnboarding',
  '/ihr/adjustment-management': 'IhrAdjustment',
  '/ihr/leaving-management': 'IhrLeaving',
  '/ihr/onboarding-exclusion': 'IhrExclusion',
  '/ihr/leaving-exclusion': 'IhrExclusion',
  '/qywx/customer-tag': 'CustomerTag',
  '/monitor/schedule': 'Schedule',
  '/statistics': 'Statistics',
  '/report': 'Report',
  '/erp/employee-management': 'ErpEmployee',
  '/profile': 'Profile',
  '/settings': 'Settings'
}

const cachedViews = computed(() => {
  return tabStore.tabs
    .map(t => pathToName[t.path])
    .filter(Boolean)
})

onMounted(async () => {
  await menuStore.loadMenuTree()
  startCheck()
  themeStore.applyTheme()
  try {
    const res = await getPublicConfig()
    if (res.code === 200 && res.data) {
      applyPublicConfig(res.data)
      const d = res.data
      if (d['sys.themeMode']) themeStore.themeMode = d['sys.themeMode']
      if (d['sys.primaryColor']) themeStore.primaryColor = d['sys.primaryColor']
      if (d['sys.sidebarStyle']) themeStore.sidebarStyle = d['sys.sidebarStyle']
      if (d['sys.logoUrl']) themeStore.logoUrl = d['sys.logoUrl']
      themeStore.applyTheme()
    }
  } catch { /* ignore */ }
})

const currentTitle = computed(() => {
  return route.meta?.pageTitle || route.name || '巅峰探索 · 后台中心'
})

const currentIcon = computed(() => {
  const icon = route.meta?.icon
  return icon ? (iconMap[icon] || getMenuIcon(icon)) : null
})

function resolveTabIcon(icon) {
  if (!icon) return null
  return iconMap[icon] || getMenuIcon(icon)
}

watch(() => isTablet.value, (tablet) => {
  if (tablet) menuStore.sidebarCollapsed = true
})

watch(() => route.path, (path) => {
  if (path === '/login') return
  const title = route.meta?.pageTitle || route.name || path
  const icon = route.meta?.icon || ''
  tabStore.addTab({ path, title, icon })
  tabStore.setActiveTab(path)

  if (isMobile.value) {
    mobileMenuOpen.value = false
    menuStore.sidebarCollapsed = true
  }

  nextTick(() => {
    const active = tabScrollRef.value?.querySelector('.tab-item.active')
    if (active) {
      active.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' })
    }
  })
}, { immediate: true })

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
  if (mobileMenuOpen.value) {
    menuStore.sidebarCollapsed = false
  }
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
  if (isMobile.value) {
    menuStore.sidebarCollapsed = true
  }
}

function navigateTo(path) {
  if (path !== route.path) {
    router.push(path)
  }
}

function closeTab(path) {
  tabStore.removeTab(path)
  if (path === route.path) {
    const remaining = tabStore.tabs
    if (remaining.length > 0) {
      router.push(remaining[remaining.length - 1].path)
    } else {
      router.push('/')
    }
  }
}

function closeAllTabs() {
  const home = tabStore.tabs.find(t => t.path === '/')
  tabStore.tabs = home ? [home] : []
  tabStore.activeTab = '/'
  router.push('/')
}

function isTabActive(tab) {
  return tab.path === route.path
    || (route.path === '/' && tab.path === '/')
    || (route.path === '/index' && tab.path === '/')
}

function handleUserCommand(command) {
  if (command === 'profile') router.push('/profile')
  else if (command === 'settings') router.push('/settings')
  else if (command === 'logout') handleLogout()
}

function handleLogout() {
  ElMessageBox.confirm('您确定要退出当前账号并离开系统吗？', '确认退出', {
    confirmButtonText: '确认退出',
    cancelButtonText: '我再想想',
    type: 'info'
  }).then(() => {
    setLoggingOut(true)
    stopCheck()
    authStore.logout()
    menuStore.clearMenu()
    tabStore.clearAll()
  }).catch(() => {})
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bc-bg);
}

/* ===== Sidebar ===== */
.sidebar {
  width: 240px;
  min-width: 240px;
  background: linear-gradient(180deg, #111827 0%, #1e293b 100%);
  color: white;
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1), min-width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.08);
}

.sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 0.9375rem;
  letter-spacing: 0.06em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding: 0 16px;
  white-space: nowrap;
  flex-shrink: 0;
}

.sidebar-header span {
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-logo {
  height: 28px;
  max-width: 160px;
  object-fit: contain;
}

.sidebar.collapsed .sidebar-logo {
  height: 24px;
  max-width: 32px;
}

.sidebar.collapsed .sidebar-header .el-icon {
  color: #60a5fa;
}

/* Light sidebar variant */
.sidebar.sidebar-light {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  color: #1e293b;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
}

.sidebar.sidebar-light .sidebar-header {
  border-bottom-color: #e5e7eb;
}

.sidebar.sidebar-light .sidebar-header span {
  background: linear-gradient(135deg, #1d4ed8, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar.sidebar-light .sidebar-header .el-icon {
  color: var(--bc-primary);
}

.sidebar.sidebar-light .sidebar-menu::after {
  background: linear-gradient(transparent, rgba(248, 250, 252, 0.3));
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  position: relative;
  scrollbar-width: none;
}
.sidebar-menu::-webkit-scrollbar {
  display: none;
}
.sidebar-menu::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: linear-gradient(transparent, rgba(17, 24, 39, 0.3));
  pointer-events: none;
  border-radius: 0 0 8px 8px;
}

/* ===== Mobile Sidebar ===== */
.sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: var(--bc-z-overlay, 300);
  transition: opacity 0.3s;
}

.sidebar.mobile-hidden {
  position: fixed;
  left: -260px;
  z-index: var(--bc-z-sidebar, 200);
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar.mobile-open {
  position: fixed;
  left: 0;
  z-index: var(--bc-z-sidebar, 200);
  width: 240px;
  min-width: 240px;
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ===== Main Content ===== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: margin-left 0.3s;
}

/* ===== Top Navbar ===== */
.top-navbar {
  height: 56px;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  padding: 0 20px;
  flex-shrink: 0;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.1);
}

.navbar-left {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
}

.navbar-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 700;
  font-size: 0.9375rem;
  color: rgba(255, 255, 255, 0.92);
  min-width: 0;
}
.navbar-right { flex: 0 0 auto; }

.title-icon { color: #60a5fa; }

.mobile-arrow { color: rgba(255, 255, 255, 0.7); }

.navbar-left :deep(.el-button) {
  color: rgba(255, 255, 255, 0.8);
}
.navbar-left :deep(.el-button:hover) {
  color: #ffffff;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 10px;
  transition: all 0.2s;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.user-dropdown:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.12);
}

.user-avatar {
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  color: white;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(96, 165, 250, 0.3);
}

.user-info { text-align: left; line-height: 1.2; }
.user-name { font-weight: 600; font-size: 0.8125rem; color: rgba(255, 255, 255, 0.92); }
.user-info small { color: rgba(255, 255, 255, 0.5); font-size: 0.6875rem; }

/* ===== Dropdown User Info ===== */
.dropdown-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.dropdown-name {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--bc-text);
}

.dropdown-id {
  font-size: 0.75rem;
  color: var(--bc-text-muted);
}

/* ===== Tab Bar ===== */
.tab-bar {
  height: 38px;
  background: var(--bc-bg-white);
  border-bottom: 1px solid var(--bc-border);
  display: flex;
  align-items: center;
  padding: 0 12px;
  flex-shrink: 0;
}

.tab-bar-scroll {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
  flex: 1;
}

.tab-bar-scroll::-webkit-scrollbar { display: none; }

.tab-bar-actions {
  margin-left: auto;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 14px;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--bc-text-muted);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  background: transparent;
  border: 1px solid transparent;
  position: relative;
}

.tab-item:hover {
  background: var(--bc-bg);
  color: var(--bc-text);
}

.tab-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--bc-primary);
  border-color: var(--el-color-primary-light-7);
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background: var(--bc-primary);
  border-radius: 1px;
}

.tab-close {
  margin-left: 4px;
  border-radius: 50%;
  padding: 2px;
  color: #ef4444;
  transition: all 0.2s;
  font-size: 10px;
  font-weight: 900;
}

.tab-close:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* ===== Content ===== */
.content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: var(--el-bg-color-page);
  display: flex;
  flex-direction: column;
}

/* ===== Footer ===== */
.footer {
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.6875rem;
  color: var(--bc-text-lighter);
  border-top: 1px solid var(--bc-border);
  background: var(--el-fill-color-light);
  flex-shrink: 0;
}

/* ===== Mobile Responsive ===== */
@media (max-width: 768px) {
  .main-content {
    margin-left: 0 !important;
  }
  .content-wrapper {
    padding: 12px !important;
  }
  .top-navbar {
    padding: 0 12px;
  }
}
</style>
