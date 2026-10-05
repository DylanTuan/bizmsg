import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

/** 后端服务端口，与 nacos/config/*.yml 保持一致。 */
const GATEWAY_ORIGIN = 'http://127.0.0.1:8080'
const MESSAGE_ORIGIN = 'http://127.0.0.1:8081'
const BUSINESS_ORIGIN = 'http://127.0.0.1:8082'

/** 去掉代理前缀后转发到目标服务，用于访问网关未路由的 Actuator 端点。 */
function rewritePrefix(prefix: string) {
  return (path: string) => path.replace(new RegExp(`^${prefix}`), '')
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const proxyPaths = [
    env.VITE_GATEWAY_HEALTH_URL,
    env.VITE_BUSINESS_HEALTH_URL,
    env.VITE_MESSAGE_HEALTH_URL,
    env.VITE_GATEWAY_ROUTES_URL
  ].filter(Boolean)

  // 开发环境把 /api 与 /proxy/** 交给本地后端，避免浏览器跨域；生产环境由 Nginx 做同样的事
  const proxy: Record<string, { target: string; changeOrigin: boolean; rewrite?: (p: string) => string }> = {
    [env.VITE_API_BASE_URL]: { target: GATEWAY_ORIGIN, changeOrigin: true }
  }
  for (const path of proxyPaths) {
    const prefix = path.split('/').slice(0, 3).join('/')
    // 业务与报文服务各自一个端口，网关仍是 8080
    const target = path.includes('business-service')
      ? BUSINESS_ORIGIN
      : path.includes('message-service')
        ? MESSAGE_ORIGIN
        : GATEWAY_ORIGIN
    proxy[prefix] = { target, changeOrigin: true, rewrite: rewritePrefix(prefix) }
  }

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      open: false,
      proxy
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 1500
    }
  }
})
