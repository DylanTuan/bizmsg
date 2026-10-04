import { computed, ref } from 'vue'

import { fetchGatewayHealth, fetchReportHealth, fetchUploadHealth } from '@/api/monitor'

export interface ServiceStatus {
  key: string
  name: string
  port: number
  /** Actuator 健康检查地址（来自环境变量，网关未被路由到 actuator） */
  endpoint: string
  status: 'UP' | 'DOWN' | 'UNKNOWN'
  detail: string
}

interface ServiceDefinition {
  key: string
  name: string
  port: number
  endpoint: string
  probe: () => Promise<{ status: string; components?: Record<string, unknown> }>
}

function definitions(): ServiceDefinition[] {
  return [
    {
      key: 'gateway-service',
      name: '网关服务',
      port: 8080,
      endpoint: import.meta.env.VITE_GATEWAY_HEALTH_URL,
      probe: fetchGatewayHealth
    },
    {
      key: 'report-service',
      name: '报文生成服务',
      port: 8081,
      endpoint: import.meta.env.VITE_REPORT_HEALTH_URL,
      probe: fetchReportHealth
    },
    {
      key: 'upload-service',
      name: '上传服务',
      port: 8082,
      endpoint: import.meta.env.VITE_UPLOAD_HEALTH_URL,
      probe: fetchUploadHealth
    }
  ]
}

/**
 * 服务健康检查：工作台与服务监控页共用。
 * 每个服务单独探测、失败互不影响，因此用 allSettled 而不是 all。
 */
export function useServiceHealth() {
  const loading = ref(false)
  const services = ref<ServiceStatus[]>(
    definitions().map((item) => ({
      key: item.key,
      name: item.name,
      port: item.port,
      endpoint: item.endpoint,
      status: 'UNKNOWN' as const,
      detail: '尚未检测'
    }))
  )

  const onlineCount = computed(() => services.value.filter((item) => item.status === 'UP').length)

  async function refresh(): Promise<void> {
    loading.value = true
    try {
      const definitionsList = definitions()
      const results = await Promise.allSettled(definitionsList.map((item) => item.probe()))
      services.value = results.map((result, index) => {
        const definition = definitionsList[index]
        if (result.status === 'fulfilled') {
          const componentCount = Object.keys(result.value.components ?? {}).length
          return {
            key: definition.key,
            name: definition.name,
            port: definition.port,
            endpoint: definition.endpoint,
            status: result.value.status.toUpperCase() === 'UP' ? ('UP' as const) : ('DOWN' as const),
            detail: componentCount > 0 ? `${componentCount} 个健康组件正常` : '健康检查通过'
          }
        }
        return {
          key: definition.key,
          name: definition.name,
          port: definition.port,
          endpoint: definition.endpoint,
          status: 'DOWN' as const,
          detail: result.reason instanceof Error ? result.reason.message : '健康检查失败'
        }
      })
    } finally {
      loading.value = false
    }
  }

  return { loading, services, onlineCount, refresh }
}
