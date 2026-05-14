<template>
  <div class="layout">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ collapsed: menuStore.sidebarCollapsed }">
      <div class="sidebar-header">
        <el-icon :size="20"><Compass /></el-icon>
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
          <el-button :icon="Fold" text @click="menuStore.toggleSidebar()" />
        </div>
        <div class="navbar-center">
          <el-icon v-if="currentIcon" class="title-icon"><component :is="currentIcon" /></el-icon>
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="navbar-right">
          <el-dropdown trigger="click">
            <div class="user-dropdown">
              <el-avatar :size="32" class="user-avatar">
                {{ authStore.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ authStore.username || '用户' }}</div>
                <small>{{ authStore.nickname || '昵称' }}</small>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/settings')">
                  <el-icon><Setting /></el-icon>系统设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Tab Bar -->
      <div class="tab-bar" v-if="tabStore.tabs.length > 0">
        <div class="tab-bar-scroll">
          <div
            v-for="tab in tabStore.tabs"
            :key="tab.path"
            class="tab-item"
            :class="{
              active: tab.path === route.path || (route.path === '/' && tab.path === '/') || (route.path === '/index' && tab.path === '/'),
              pinned: tab.path === '/'
            }"
            @click="navigateTo(tab.path)"
          >
            <el-icon v-if="tab.icon" :size="12"><component :is="tab.icon" /></el-icon>
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
      </div>

      <!-- Content -->
      <div class="content-wrapper">
        <router-view />
      </div>

      <!-- Footer -->
      <footer class="footer">
        <span>&copy; 2026 BC体育 & 巅峰探索.</span>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useTabStore } from '@/stores/tab'
import { useSessionCheck } from '@/composables/useSessionCheck'
import { ElMessageBox } from 'element-plus'
import { Fold, User, Setting, SwitchButton, Close, Compass } from '@element-plus/icons-vue'
import SidebarMenu from '@/components/SidebarMenu.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const menuStore = useMenuStore()
const tabStore = useTabStore()
const { startCheck } = useSessionCheck()

onMounted(async () => {
  await menuStore.loadMenuTree()
  startCheck()
})

const currentTitle = computed(() => {
  return route.meta?.pageTitle || route.name || '巅峰探索 · 后台中心'
})

const currentIcon = computed(() => {
  return route.meta?.icon || null
})

watch(() => route.path, (path) => {
  if (path === '/login') return
  const title = route.meta?.pageTitle || route.name || path
  const icon = route.meta?.icon || ''
  tabStore.addTab({ path, title, icon })
  tabStore.setActiveTab(path)
}, { immediate: true })

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

function handleLogout() {
  ElMessageBox.confirm('您确定要退出当前账号并离开系统吗？', '确认退出', {
    confirmButtonText: '确认退出',
    cancelButtonText: '我再想想',
    type: 'info'
  }).then(() => {
    authStore.logout()
    menuStore.clearMenu()
  }).catch(() => {})
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #f5f5f4;
}

/* Sidebar */
.sidebar {
  width: 260px;
  min-width: 260px;
  background: #1c1917;
  color: white;
  display: flex;
  flex-direction: column;
  transition: width 0.3s, min-width 0.3s;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1rem;
  letter-spacing: 0.05em;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  padding: 0 16px;
  white-space: nowrap;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

/* Main Content */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: margin-left 0.3s;
}

/* Top Navbar */
.top-navbar {
  height: 64px;
  background: white;
  border-bottom: 1px solid #e7e5e4;
  display: flex;
  align-items: center;
  padding: 0 20px;
  flex-shrink: 0;
}

.navbar-left { flex: 0 0 auto; }
.navbar-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 700;
  font-size: 1rem;
  color: #1c1917;
}
.navbar-right { flex: 0 0 auto; }

.title-icon { color: #1d4ed8; }

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-dropdown:hover { background: #f5f5f4; }

.user-avatar { background: linear-gradient(135deg, #1d4ed8, #7c3aed); color: white; font-weight: 700; }

.user-info { text-align: left; line-height: 1.2; }
.user-name { font-weight: 700; font-size: 0.875rem; }
.user-info small { color: #78716c; font-size: 0.75rem; }

/* Tab Bar */
.tab-bar {
  height: 40px;
  background: white;
  border-bottom: 1px solid #e7e5e4;
  display: flex;
  align-items: center;
  padding: 0 8px;
  flex-shrink: 0;
}

.tab-bar-scroll {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
}

.tab-bar-scroll::-webkit-scrollbar { display: none; }

.tab-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #78716c;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  background: transparent;
  border: 1px solid transparent;
}

.tab-item:hover { background: #f5f5f4; color: #1c1917; }

.tab-item.active {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #bfdbfe;
  font-weight: 600;
}

.tab-close {
  margin-left: 4px;
  border-radius: 50%;
  padding: 2px;
  color: #a8a29e;
  transition: all 0.2s;
}

.tab-close:hover { background: #fee2e2; color: #dc2626; }

/* Content */
.content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* Footer */
.footer {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  color: #a8a29e;
  border-top: 1px solid #e7e5e4;
  background: white;
  flex-shrink: 0;
}
</style>
