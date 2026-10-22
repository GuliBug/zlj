import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', breadcrumb: '工作台' }
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/product/index.vue'),
        meta: { title: '商品列表', breadcrumb: '商品管理 / 商品列表' }
      },
      {
        path: 'products/new',
        name: 'ProductNew',
        component: () => import('@/views/product/edit.vue'),
        meta: { title: '新增商品', breadcrumb: '商品管理 / 新增商品' }
      },
      {
        path: 'products/:id/edit',
        name: 'ProductEdit',
        component: () => import('@/views/product/edit.vue'),
        meta: { title: '编辑商品', breadcrumb: '商品管理 / 编辑商品' }
      },
      {
        path: 'stock',
        name: 'Stock',
        component: () => import('@/views/stock/index.vue'),
        meta: { title: '库存管理', breadcrumb: '库存管理' }
      },
      {
        path: 'stores',
        name: 'Stores',
        component: () => import('@/views/store/index.vue'),
        meta: { title: '门店管理', breadcrumb: '门店管理' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单管理', breadcrumb: '订单管理' }
      },
      {
        path: 'aftersales',
        name: 'Aftersales',
        component: () => import('@/views/aftersale/index.vue'),
        meta: { title: '售后管理', breadcrumb: '售后管理' }
      },
      {
        path: 'members',
        name: 'Members',
        component: () => import('@/views/member/index.vue'),
        meta: { title: '会员管理', breadcrumb: '会员管理' }
      },
      {
        path: 'audit-alerts',
        name: 'AuditAlerts',
        component: () => import('@/views/audit/index.vue'),
        meta: { title: '审计与告警', breadcrumb: '审计与告警' }
      }
    ]
  },
  { path: '/audit', redirect: '/audit-alerts' },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：无 token 跳 /login
router.beforeEach((to) => {
  const token = getToken()
  if (to.path !== '/login' && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  document.title = to.meta.title ? to.meta.title + ' - 正隆記 · 门店运营后台' : '正隆記 · 门店运营后台'
  return true
})

export default router
