import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '../api'
import pagesConfig from '../pages.json'

// 页面配置单一来源：src/pages.json（对齐参考工程 Taro app.config 的 pages / tabBar / window 组织方式）
const modules = import.meta.glob('../pages/*.vue')
const componentOf = (name) => modules[`../pages/${name}.vue`]

const routes = pagesConfig.pages
  .filter((p) => componentOf(p.path))
  .map((p) => ({
    path: p.route,
    component: componentOf(p.path),
    meta: { title: p.title, tab: !!p.tab, auth: !!p.auth }
  }))

routes.push({ path: '/:pathMatch(.*)*', redirect: '/' })

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 登录拦截：商品浏览与登录页除外
router.beforeEach((to) => {
  if (to.meta.auth && !getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + pagesConfig.window.navigationBarTitleText
})

export default router
export { pagesConfig }
