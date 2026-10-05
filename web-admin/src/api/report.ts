import { request } from '@/api/request'
import type { ReceiptView, ReportGenerateRequest, ReportUploadResult } from '@/types/api'

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

/**
 * 服务端报文回执列表（按回执时间倒序）。
 * 「报文记录」页用它列出服务端真实生成过的报文——业务模块经 MQ 异步生成的报文也在其中，
 * 因此不再只依赖浏览器本地的生成记录。
 */
export function listReceipts() {
  return request<ReceiptView[]>({
    url: '/report/receipts',
    method: 'get'
  })
}

/**
 * 按业务号查询报文回执，用来确认「异步报文已生成」。
 * 报文还没生成时后端返回 404，这里关掉全局错误弹窗（skipErrorMessage），
 * 由调用页面自己渲染「等待生成」状态——这是异步链路正常的中间态，不该弹红色报错。
 */
export function fetchReceipt(businessId: string) {
  return request<ReceiptView>({
    url: `/report/receipt/${businessId}`,
    method: 'get',
    skipErrorMessage: true
  })
}

/** 拉取某笔业务的报文原文（服务端回执记录按需读取，列表接口不返回 XML 内容）。 */
export function fetchReceiptXml(businessId: string) {
  return request<{ businessId: string; xml: string }>({
    url: `/report/receipt/${businessId}/xml`,
    method: 'get',
    skipErrorMessage: true
  })
}
