import { request } from '@/api/request'
import type { ReportGenerateRequest, ReportUploadResult } from '@/types/api'

/** 连通性探测：网关路由 /api/report/** 是否生效。 */
export function pingReportService() {
  return request<{ service: string; status: string }>({
    url: '/report/ping',
    method: 'get'
  })
}

/**
 * 生成 XML 报文并上传。
 * 注意：后端上传失败时会降级返回 200 + uploaded=false，因此调用方必须判断 uploaded 字段，
 * 不能只看请求是否成功。
 */
export function generateReport(data: ReportGenerateRequest) {
  return request<ReportUploadResult>({
    url: '/report/generate',
    method: 'post',
    data
  })
}
