import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTabStore } from '@/stores/tab'

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
      { path: '', name: 'Dashboard', component: () => import('@/views/dashboard/Index.vue'), meta: { pageTitle: '数据驾驶舱' } },
      // System
      { path: 'system/menu', name: 'MenuManagement', component: () => import('@/views/system/Menu.vue'), meta: { pageTitle: '菜单管理' } },
      { path: 'system/user', name: 'UserManagement', component: () => import('@/views/system/User.vue'), meta: { pageTitle: '用户管理' } },
      { path: 'system/role', name: 'RoleManagement', component: () => import('@/views/system/Role.vue'), meta: { pageTitle: '角色管理' } },
      { path: 'system/dept', name: 'DeptManagement', component: () => import('@/views/system/Dept.vue'), meta: { pageTitle: '部门管理' } },
      { path: 'system/dict', name: 'DictManagement', component: () => import('@/views/system/Dict.vue'), meta: { pageTitle: '字典管理' } },
      { path: 'system/online-user', name: 'OnlineUser', component: () => import('@/views/system/OnlineUser.vue'), meta: { pageTitle: '在线用户' } },
      // BI
      { path: 'bi/management', name: 'BiManagement', component: () => import('@/views/bi/Management.vue'), meta: { pageTitle: '品牌与渠道' } },
      { path: 'bi/entity-channel', name: 'EntityChannel', component: () => import('@/views/bi/EntityChannel.vue'), meta: { pageTitle: '实体渠道' } },
      { path: 'bi/entity-channel/form', name: 'EntityChannelForm', component: () => import('@/views/bi/EntityChannelForm.vue'), meta: { pageTitle: '渠道表单' } },
      { path: 'bi/erp-shop', name: 'ErpShop', component: () => import('@/views/bi/ErpShop.vue'), meta: { pageTitle: '店铺管理' } },
      { path: 'bi/erp-warehouse', name: 'ErpWarehouse', component: () => import('@/views/bi/ErpWarehouse.vue'), meta: { pageTitle: '仓库管理' } },
      { path: 'bi/erp-customer', name: 'ErpCustomer', component: () => import('@/views/bi/ErpCustomer.vue'), meta: { pageTitle: '客户管理' } },
      // IHR
      { path: 'ihr/employee-management', name: 'IhrEmployee', component: () => import('@/views/ihr/EmployeeManagement.vue'), meta: { pageTitle: '员工管理' } },
      { path: 'ihr/onboarding-management', name: 'IhrOnboarding', component: () => import('@/views/ihr/OnboardingManagement.vue'), meta: { pageTitle: '入职管理' } },
      { path: 'ihr/adjustment-management', name: 'IhrAdjustment', component: () => import('@/views/ihr/AdjustmentManagement.vue'), meta: { pageTitle: '调岗管理' } },
      { path: 'ihr/leaving-management', name: 'IhrLeaving', component: () => import('@/views/ihr/LeavingManagement.vue'), meta: { pageTitle: '离职管理' } },
      { path: 'ihr/onboarding-exclusion', name: 'IhrOnboardingExclusion', component: () => import('@/views/ihr/Exclusion.vue'), meta: { exclusionType: 1, pageTitle: '入职排除' } },
      { path: 'ihr/leaving-exclusion', name: 'IhrLeavingExclusion', component: () => import('@/views/ihr/Exclusion.vue'), meta: { exclusionType: 2, pageTitle: '离职排除' } },
      // QYWX
      { path: 'qywx/customer-tag', name: 'CustomerTag', component: () => import('@/views/qywx/CustomerTag.vue'), meta: { pageTitle: '客户标签' } },
      // NxCRM
      { path: 'nxcrm/customer-tag', name: 'NxcrmCustomerTag', component: () => import('@/views/nxcrm/CustomerTag.vue'), meta: { pageTitle: '南讯打标签' } },
      { path: 'nxcrm/member-tag', name: 'NxcrmMemberTag', component: () => import('@/views/nxcrm/MemberTagManagement.vue'), meta: { pageTitle: '会员标签管理' } },
      // Sticker
      { path: 'sticker/print', name: 'StickerPrintList', component: () => import('@/views/sticker/PrintApplication.vue'), meta: { pageTitle: '贴纸打印' } },
      { path: 'sticker/print/:orderId', name: 'StickerPrintDetail', component: () => import('@/views/sticker/PrintDetail.vue'), meta: { pageTitle: '申请单详情' } },
      // Monitor
      { path: 'monitor/schedule', name: 'Schedule', component: () => import('@/views/monitor/Schedule.vue'), meta: { pageTitle: '定时任务' } },
      { path: 'monitor/system', name: 'SystemMonitor', component: () => import('@/views/monitor/SystemMonitor.vue'), meta: { pageTitle: '系统监控' } },
      // Statistics
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/statistics/Statistics.vue'), meta: { pageTitle: '数据统计' } },
      { path: 'report', name: 'Report', component: () => import('@/views/statistics/Report.vue'), meta: { pageTitle: '报表中心' } },
      // ERP
      { path: 'erp/employee-management', name: 'ErpEmployee', component: () => import('@/views/erp/EmployeeManagement.vue'), meta: { pageTitle: 'ERP员工' } },
      // User
      { path: 'profile', name: 'Profile', component: () => import('@/views/user/Profile.vue'), meta: { pageTitle: '个人中心' } },
      { path: 'settings', name: 'Settings', component: () => import('@/views/user/Settings.vue'), meta: { pageTitle: '系统设置' } }
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
      useTabStore().clearAll()
      next('/login')
    }
  } else {
    next()
  }
})

export default router
