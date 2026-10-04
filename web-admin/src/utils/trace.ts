/**
 * 请求 ID：与网关 RequestIdGlobalFilter 约定的 Header 同名，
 * 前端先生成再透传，出现问题时可用它反查网关与下游服务的全链路日志。
 */
export const REQUEST_ID_HEADER = 'X-Request-Id'

export function generateRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '')
  }
  return Math.random().toString(16).slice(2) + Date.now().toString(16)
}
