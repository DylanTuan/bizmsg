import NProgress from 'nprogress'
import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory } from 'vue-router'

import { APP_TITLE, NPROGRESS_OPTIONS } from '@/constants'
import { useUserStore } from '@/stores/user'
import { getStorage, setStorage } from '@/utils/storage'

import { HOME_PATH, LOGIN_PATH, NOT_FOUND_PATH, constantRoutes } from './routes'

NProgress.configure(NPROGRESS_OPTIONS)

const router = createRouter({
  // history 模式：生产环境需要 Nginx 配置 try_files 回退到 index.html
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

/** 免登录白名单。 */
const WHITE_LIST: string[] = [LOGIN_PATH, NOT_FOUND_PATH]

router.beforeEach((to) => {
  NProgress.start()
  const userStore = useUserStore()

  if (!userStore.isLoggedIn) {
    if (WHITE_LIST.includes(to.path)) {
      return true
    }
    // 记录来源地址，登录成功后原路返回
    return { path: LOGIN_PATH, query: to.fullPath === HOME_PATH ? {} : { redirect: to.fullPath } }
  }

  if (to.path === LOGIN_PATH) {
    return { path: HOME_PATH }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - ${APP_TITLE}` : APP_TITLE
  NProgress.done()
})

/**
 * 懒加载路由文件加载失败的兜底。
 * 常见诱因：开发服务器重新优化依赖/重启后 URL 失效、发版后旧 chunk 404、HMR 状态错乱。
 * 这类失败下路由组件拿不到，内容区会一直空白，刷新一次即可恢复，因此做一次性自动刷新。
 */
const CHUNK_LOAD_ERROR = /dynamically imported module|Importing a module script failed|Failed to fetch|Loading chunk|Loading CSS chunk/i
const RELOAD_AT_KEY = 'chunkReloadAt'
const RELOAD_COOLDOWN = 10_000

router.onError((error) => {
  // 出错也要收起进度条，否则会一直卡在顶部
  NProgress.done()

  const message = error instanceof Error ? error.message : String(error)
  if (CHUNK_LOAD_ERROR.test(message)) {
    const lastReloadAt = getStorage<number>(RELOAD_AT_KEY, 0)
    // 冷却期内不重复刷新，避免真的加载不到时陷入刷新死循环
    if (Date.now() - lastReloadAt > RELOAD_COOLDOWN) {
      setStorage(RELOAD_AT_KEY, Date.now())
      window.location.reload()
      return
    }
  }
  ElMessage.error(`页面加载失败：${message}`)
})

export default router
