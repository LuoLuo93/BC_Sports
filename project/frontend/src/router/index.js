import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/dashboard/Index.vue') },
      // System
      { path: 'system/menu', name: 'MenuManagement', component: () => import('@/views/system/Menu.vue') },
      { path: 'system/user', name: 'UserManagement', component: () => import('@/views/system/User.vue') },
      { path: 'system/role', name: 'RoleManagement', component: () => import('@/views/system/Role.vue') },
      { path: 'system/dept', name: 'DeptManagement', component: () => import('@/views/system/Dept.vue') },
      { path: 'system/dict', name: 'DictManagement', component: () => import('@/views/system/Dict.vue') },
      // BI
      { path: 'bi/management', name: 'BiManagement', component: () => import('@/views/bi/Management.vue') },
      { path: 'bi/entity-channel', name: 'EntityChannel', component: () => import('@/views/bi/EntityChannel.vue') },
      { path: 'bi/entity-channel/form', name: 'EntityChannelForm', component: () => import('@/views/bi/EntityChannelForm.vue') },
      { path: 'bi/erp-shop', name: 'ErpShop', component: () => import('@/views/bi/ErpShop.vue') },
      { path: 'bi/erp-warehouse', name: 'ErpWarehouse', component: () => import('@/views/bi/ErpWarehouse.vue') },
      { path: 'bi/erp-customer', name: 'ErpCustomer', component: () => import('@/views/bi/ErpCustomer.vue') },
      // IHR
      { path: 'ihr/employee-management', name: 'IhrEmployee', component: () => import('@/views/ihr/EmployeeManagement.vue') },
      { path: 'ihr/onboarding-management', name: 'IhrOnboarding', component: () => import('@/views/ihr/OnboardingManagement.vue') },
      { path: 'ihr/adjustment-management', name: 'IhrAdjustment', component: () => import('@/views/ihr/AdjustmentManagement.vue') },
      { path: 'ihr/leaving-management', name: 'IhrLeaving', component: () => import('@/views/ihr/LeavingManagement.vue') },
      { path: 'ihr/onboarding-exclusion', name: 'IhrOnboardingExclusion', component: () => import('@/views/ihr/Exclusion.vue'), meta: { exclusionType: 1, pageTitle: '入职排除' } },
      { path: 'ihr/leaving-exclusion', name: 'IhrLeavingExclusion', component: () => import('@/views/ihr/Exclusion.vue'), meta: { exclusionType: 2, pageTitle: '离职排除' } },
      // QYWX
      { path: 'qywx/customer-tag', name: 'CustomerTag', component: () => import('@/views/qywx/CustomerTag.vue') },
      // Monitor
      { path: 'monitor/schedule', name: 'Schedule', component: () => import('@/views/monitor/Schedule.vue') },
      // Statistics
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/statistics/Statistics.vue') },
      { path: 'report', name: 'Report', component: () => import('@/views/statistics/Report.vue') },
      // ERP
      { path: 'erp/employee-management', name: 'ErpEmployee', component: () => import('@/views/erp/EmployeeManagement.vue') },
      // User
      { path: 'profile', name: 'Profile', component: () => import('@/views/user/Profile.vue') },
      { path: 'settings', name: 'Settings', component: () => import('@/views/user/Settings.vue') }
    ]
  },
  { path: '/403', name: 'Forbidden', component: () => import('@/views/error/403.vue'), meta: { requiresAuth: false } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  if (!authStore.isAuthenticated) {
    try {
      await authStore.fetchSessionInfo()
      next()
    } catch {
      next('/login')
    }
  } else {
    next()
  }
})

export default router
