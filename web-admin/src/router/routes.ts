import type { RouteRecordRaw } from 'vue-router'

/**
 * 布局组件懒加载。这里不用静态 import 有两个原因：
 * 1) 避免 routes.ts ↔ layout/components/AppSidebar.vue（需要读路由表生成菜单）形成循环依赖，
 *    循环依赖在 dev 的 ESM 逐模块加载下容易出现初始化顺序问题；
 * 2) 布局只在首次进入时下载一次，之后由 vue-router 缓存。
 */
const Layout = () => import('@/layout/index.vue')

export const LOGIN_PATH = '/login'
export const HOME_PATH = '/dashboard'
export const NOT_FOUND_PATH = '/404'

/** 路由 meta 约定，扩展字段需在此声明，TS 才有提示。 */
declare module 'vue-router' {
  interface RouteMeta {
    /** 菜单与面包屑标题，同时用于浏览器标题 */
    title?: string
    /** Element Plus 图标组件名（已全局注册） */
    icon?: string
    /** 不在侧边菜单中展示 */
    hidden?: boolean
    /** 只有一个子路由时仍以子菜单形式展示 */
    alwaysShow?: boolean
  }
}

/**
 * 静态路由表：侧边菜单直接由它生成（见 layout/components/AppSidebar.vue），
 * 新增页面只要在这里加一条记录，无需再改菜单组件。
 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: LOGIN_PATH,
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: HOME_PATH,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled' }
      }
    ]
  },
  {
    path: '/report',
    component: Layout,
    redirect: '/report/generate',
    meta: { title: '报文中心', icon: 'Tickets' },
    children: [
      {
        path: 'generate',
        name: 'ReportGenerate',
        component: () => import('@/views/report/generate.vue'),
        meta: { title: '报文生成', icon: 'MagicStick' }
      },
      {
        path: 'history',
        name: 'ReportHistory',
        component: () => import('@/views/report/history.vue'),
        meta: { title: '生成记录', icon: 'Clock' }
      }
    ]
  },
  {
    path: '/monitor',
    component: Layout,
    redirect: '/monitor/services',
    meta: { title: '服务监控', icon: 'Monitor' },
    children: [
      {
        path: 'services',
        name: 'ServiceMonitor',
        component: () => import('@/views/monitor/services.vue'),
        meta: { title: '服务监控', icon: 'Monitor' }
      }
    ]
  },
  {
    path: NOT_FOUND_PATH,
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: NOT_FOUND_PATH,
    meta: { hidden: true }
  }
]
