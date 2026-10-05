import { computed, onMounted, ref } from 'vue'

import { fetchReceiptXml, listReceipts } from '@/api/report'
import { useReportStore } from '@/stores/report'
import type { ReceiptView, ReportHistoryItem } from '@/types/api'

/**
 * 服务端回执 → 列表项。
 * message-service 记录了它真实生成过的每一份报文，因此业务模块经 RabbitMQ 异步生成的报文
 * 会从这里出现；XML 不进列表（可能很大），点开详情/下载时再按需拉取。
 */
function toServerRecord(receipt: ReceiptView): ReportHistoryItem {
  return {
    id: `server-${receipt.businessId}`,
    businessId: receipt.businessId,
    messageType: receipt.messageType ?? '',
    createdAt: receipt.receivedAt,
    recordedAt: receipt.receivedAt,
    xml: '',
    uploaded: receipt.success,
    receiptNo: receipt.receiptNo,
    storedPath: receipt.storedPath,
    degradeReason: receipt.success ? null : receipt.message,
    source: 'server'
  }
}

/**
 * 报文记录：合并「服务端回执」与「浏览器本地留存」两个来源，工作台与报文记录页共用。
 *
 * - 服务端回执来自 message-service，**包含业务模块经 MQ 异步生成的报文**，只读；
 * - 本地留存带 XML 与降级原因，支持重新上传/删除；
 * - 同一业务号以本地为准（信息更全），避免出现重复行；
 * - 统一按生成时间**倒序**，保证「最近生成的报文」真的是最近的。
 */
export function useReportRecords() {
  const reportStore = useReportStore()
  const serverRecords = ref<ReportHistoryItem[]>([])
  const loading = ref(false)

  const records = computed<ReportHistoryItem[]>(() => {
    const localIds = new Set(reportStore.history.map((item) => item.businessId))
    const merged = [...reportStore.history, ...serverRecords.value.filter((item) => !localIds.has(item.businessId))]
    // 两个来源各自有序，合并后必须重新按时间排一次
    return merged.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  })

  const uploadedCount = computed(() => records.value.filter((item) => item.uploaded).length)
  const degradedCount = computed(() => records.value.length - uploadedCount.value)

  /** 拉取服务端回执；失败不阻塞页面，本地记录仍然可见。 */
  async function load(): Promise<void> {
    loading.value = true
    try {
      serverRecords.value = (await listReceipts()).map(toServerRecord)
    } catch {
      serverRecords.value = []
    } finally {
      loading.value = false
    }
  }

  /** 服务端记录的 XML 按需拉取并缓存到行上，避免列表接口传输大量报文。 */
  async function ensureXml(row: ReportHistoryItem): Promise<string> {
    if (row.xml) {
      return row.xml
    }
    const detail = await fetchReceiptXml(row.businessId)
    row.xml = detail.xml
    return detail.xml
  }

  onMounted(load)

  return { records, loading, uploadedCount, degradedCount, load, ensureXml }
}
