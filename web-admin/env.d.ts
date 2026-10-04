/// <reference types="vite/client" />

/** 环境变量声明：与 .env.development / .env.production 中的键保持一致。 */
interface ImportMetaEnv {
  /** 后端接口前缀，开发环境经 Vite 代理转发到网关，生产环境由 Nginx 反向代理。 */
  readonly VITE_API_BASE_URL: string
  /** 应用标题 */
  readonly VITE_APP_TITLE: string
  /** 各服务 Actuator 健康检查地址（网关未路由 actuator，因此单独配置） */
  readonly VITE_GATEWAY_HEALTH_URL: string
  readonly VITE_MESSAGE_HEALTH_URL: string
  /** 网关路由表地址 */
  readonly VITE_GATEWAY_ROUTES_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
