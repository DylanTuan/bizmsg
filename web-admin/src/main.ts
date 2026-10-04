import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { createPinia } from 'pinia'
import { createApp } from 'vue'

import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'
import '@/assets/styles/index.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

/**
 * 全局错误兜底：组件渲染/生命周期里未捕获的异常若不处理，页面会直接白屏且看不到任何线索。
 * 这里统一打到控制台并给出可见提示，便于快速定位（而不是留下一片空白让人猜）。
 */
app.config.errorHandler = (error, _instance, info) => {
  console.error('[bizmsg] 组件异常:', error, info)
  ElMessage.error(`页面出错：${error instanceof Error ? error.message : String(error)}`)
}

window.addEventListener('unhandledrejection', (event) => {
  console.error('[bizmsg] 未处理的 Promise 异常:', event.reason)
})

// 全局注册 Element Plus 图标：菜单与按钮可直接用 <component :is="'User'" /> 按名字渲染
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.use(createPinia())
app.use(router)
// 统一使用中文语言包（分页、日期、校验提示等文案）
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
