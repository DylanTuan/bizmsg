import { request } from '@/api/request'
import type { GatewayRouteDefinition, GatewayRouteRaw, GatewayRouteView, HealthResponse } from '@/types/api'

/**
 * Actuator 端点没有被网关路由（网关只转发 /api/report/** 与 /api/upload/**，
 * 合并后这两条路由都指向 message-service），所以这里用 baseURL='' 直连各服务，地址由环境变量给出。
 * skipErrorMessage：页面自己渲染每个服务的状态，不需要再弹全局错误。
 */
const healthOptions = { baseURL: '', method: 'get', skipErrorMessage: true, timeout: 5000 } as const

export function fetchGatewayHealth() {
  return request<HealthResponse>({ ...healthOptions, url: import.meta.env.VITE_GATEWAY_HEALTH_URL })
}

export function fetchMessageHealth() {
  return request<HealthResponse>({ ...healthOptions, url: import.meta.env.VITE_MESSAGE_HEALTH_URL })
}

/** 新版返回 { route_id, route_definition }，旧版直接平铺字段，统一取定义体。 */
function resolveDefinition(raw: GatewayRouteRaw): GatewayRouteDefinition {
  return raw.route_definition ?? raw
}

function formatPredicates(raw: GatewayRouteRaw): string[] {
  const definition = resolveDefinition(raw)
  return (definition.predicates ?? []).map((predicate) => {
    const args = Object.values(predicate.args ?? {}).join(', ')
    return args ? `${predicate.name}=${args}` : predicate.name
  })
}

/** 拉取网关路由表，用来直观验证 Nacos 中的路由配置是否已生效。 */
export async function fetchGatewayRoutes(): Promise<GatewayRouteView[]> {
  const rawList = await request<GatewayRouteRaw[] | Record<string, GatewayRouteRaw>>({
    ...healthOptions,
    url: import.meta.env.VITE_GATEWAY_ROUTES_URL
  })
  // 个别版本返回的是「路由 ID -> 定义」的对象，这里统一成数组，避免 .map 直接抛错
  const list = Array.isArray(rawList) ? rawList : Object.values(rawList ?? {})
  return list.map((raw, index) => {
    const definition = resolveDefinition(raw)
    return {
      id: definition.id ?? raw.route_id ?? `route-${index + 1}`,
      uri: definition.uri ?? '',
      order: definition.order ?? 0,
      predicates: formatPredicates(raw)
    }
  })
}
