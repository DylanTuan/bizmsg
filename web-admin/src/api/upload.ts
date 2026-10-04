import { request } from '@/api/request'
import type { ReceiptResponse, UploadReceiptRequest } from '@/types/api'

/** 直连上传服务提交报文，用于本地历史的「重新上传」。 */
export function uploadReceipt(data: UploadReceiptRequest) {
  return request<ReceiptResponse>({
    url: '/upload/receipt',
    method: 'post',
    data
  })
}
