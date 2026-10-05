import { request } from '@/api/request'
import type { TransferCreateRequest, TransferView } from '@/types/api'

/** 受理商品房转移业务，返回业务号与「已受理」状态。 */
export function createTransfer(data: TransferCreateRequest) {
  return request<TransferView>({
    url: '/business/transfer',
    method: 'post',
    data
  })
}

/**
 * 办结业务：后端先向 RabbitMQ 投递办结事件（等待 broker confirm），成功后状态才推进为「已办结」。
 * 报文是异步生成的，办结成功后需要再查回执（见 fetchReceipt）。
 * 重复调用是幂等的，不会重复发消息。
 */
export function completeTransfer(businessId: string) {
  return request<TransferView>({
    url: `/business/transfer/${businessId}/complete`,
    method: 'post'
  })
}

/** 查询单笔业务。 */
export function getTransfer(businessId: string) {
  return request<TransferView>({
    url: `/business/transfer/${businessId}`,
    method: 'get'
  })
}

/** 业务列表（按受理时间倒序）。 */
export function listTransfers() {
  return request<TransferView[]>({
    url: '/business/transfer',
    method: 'get'
  })
}
