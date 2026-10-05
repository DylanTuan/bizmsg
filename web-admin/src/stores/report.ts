import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import type { ReceiptResponse, ReportHistoryItem, ReportUploadResult } from '@/types/api'
import { STORAGE_KEYS, getStorage, setStorage } from '@/utils/storage'

/** 本地历史最多保留的条数，防止 localStorage 无限增长。 */
const MAX_RECORDS = 50

function toHistoryItem(result: ReportUploadResult): ReportHistoryItem {
  return {
    id: `${result.businessId}-${result.createdAt}`,
    businessId: result.businessId,
    messageType: result.messageType,
    createdAt: result.createdAt,
    recordedAt: new Date().toLocaleString('zh-CN', { hour12: false }),
    xml: result.xml,
    uploaded: result.uploaded,
    receiptNo: result.receiptNo,
    storedPath: result.storedPath,
    degradeReason: result.degradeReason,
    source: 'local'
  }
}

/**
 * 报文生成记录：本地留存「重新上传」所需的 XML 与降级原因。
 * 列表页会把它与 message-service 的服务端回执合并展示——业务模块经 MQ 异步生成的报文
 * 不会写进这里，只能从服务端列表拿到（见 views/report/history.vue）。
 */
export const useReportStore = defineStore('report', () => {
  const history = ref<ReportHistoryItem[]>(getStorage<ReportHistoryItem[]>(STORAGE_KEYS.reportHistory, []))

  const total = computed(() => history.value.length)
  const uploadedCount = computed(() => history.value.filter((item) => item.uploaded).length)
  const degradedCount = computed(() => total.value - uploadedCount.value)

  function persist(): void {
    setStorage(STORAGE_KEYS.reportHistory, history.value)
  }

  function addRecord(result: ReportUploadResult): ReportHistoryItem {
    const item = toHistoryItem(result)
    // 同一 businessId 重复生成时只保留最新一条，避免列表出现重复流水号
    history.value = [item, ...history.value.filter((record) => record.businessId !== result.businessId)].slice(
      0,
      MAX_RECORDS
    )
    persist()
    return item
  }

  /** 重新上传成功后更新原记录的上传状态与回执信息。 */
  function markReuploaded(id: string, receipt: ReceiptResponse): void {
    const record = history.value.find((item) => item.id === id)
    if (!record) {
      return
    }
    record.uploaded = receipt.success
    record.receiptNo = receipt.receiptNo
    record.storedPath = receipt.storedPath
    record.degradeReason = receipt.success ? null : receipt.message
    persist()
  }

  function removeRecord(id: string): void {
    history.value = history.value.filter((item) => item.id !== id)
    persist()
  }

  function clearHistory(): void {
    history.value = []
    persist()
  }

  return { history, total, uploadedCount, degradedCount, addRecord, markReuploaded, removeRecord, clearHistory }
})
