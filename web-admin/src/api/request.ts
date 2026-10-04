import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

import { STORAGE_KEYS, getStorage, removeStorage } from '@/utils/storage'
import { REQUEST_ID_HEADER, generateRequestId } from '@/utils/trace'

/**
 * 请求配置扩展：
 * - skipErrorMessage：关闭默认的错误弹窗，由调用方自行展示（如健康检查页面渲染状态卡片）
 */
export interface RequestConfig extends AxiosRequestConfig {
  skipErrorMessage?: boolean
}

const service: AxiosInstance = axios.create({
  // 开发环境走 Vite 代理、生产环境走 Nginx，前端只认 /api 前缀，真实地址不写死
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getStorage<string>(STORAGE_KEYS.token, '')
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  if (!config.headers.get(REQUEST_ID_HEADER)) {
    config.headers.set(REQUEST_ID_HEADER, generateRequestId())
  }
  return config
})

/** 401 时不引入 router（会形成循环依赖），直接用地址跳转并刷新状态。 */
function redirectToLogin(): void {
  removeStorage(STORAGE_KEYS.token)
  removeStorage(STORAGE_KEYS.userInfo)
  const { pathname, search, hash } = window.location
  const redirect = encodeURIComponent(`${pathname}${search}${hash}`)
  if (!pathname.startsWith('/login')) {
    window.location.href = `/login?redirect=${redirect}`
  }
}

/** HTTP 状态码 -> 中文提示：后端服务没起、网关连不上下游是这个项目最常见的故障。 */
function messageOfStatus(status: number): string {
  switch (status) {
    case 400:
      return '请求参数不合法（400）'
    case 401:
      return '登录状态已失效，请重新登录（401）'
    case 403:
      return '没有访问权限（403）'
    case 404:
      return '接口不存在（404）'
    case 405:
      return '请求方法不被支持（405）'
    case 500:
      return '服务端处理失败（500）'
    case 502:
    case 503:
      return '网关无法连接目标服务（502/503），请确认后端服务已启动并注册到 Nacos'
    case 504:
      return '网关等待下游超时（504）'
    default:
      return `请求失败（${status}）`
  }
}

service.interceptors.response.use(
  // 后端未做统一响应包装，body 就是业务 DTO，这里直接剥掉 AxiosResponse 外壳
  (response) => response.data,
  (error: AxiosError<{ success?: string; message?: string }>) => {
    const config = error.config as RequestConfig | undefined
    let message: string

    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      message = '请求超时，请稍后重试'
    } else if (!error.response) {
      message = '无法连接网关（127.0.0.1:8080），请确认服务已启动'
    } else {
      const { status, data } = error.response
      // 优先使用后端异常出口给的 message（如「报文不是格式良好的 XML」）
      message = data?.message || messageOfStatus(status)
      if (status === 401) {
        redirectToLogin()
      }
    }

    if (!config?.skipErrorMessage) {
      ElMessage.error(message)
    }
    return Promise.reject(new Error(message))
  }
)

/** 统一出口：拦截器已剥壳，因此泛型 T 直接是业务数据类型。 */
export function request<T>(config: RequestConfig): Promise<T> {
  // axios 1.20 起 request 的返回类型是条件类型，无法表达「响应拦截器已剥掉 AxiosResponse」这一约定，
  // 因此在唯一出口处收敛为 T：调用方拿到的就是业务 DTO。
  return service.request<T, T>(config) as unknown as Promise<T>
}

export default request
