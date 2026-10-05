<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, ref } from 'vue'

import { uploadReceipt } from '@/api/upload'
import { useReportRecords } from '@/composables/useReportRecords'
import { useReportStore } from '@/stores/report'
import type { ReportHistoryItem } from '@/types/api'
import { downloadTextFile } from '@/utils/download'
import { timestampForFilename } from '@/utils/format'

const reportStore = useReportStore()
// 服务端回执 + 本地留存，按时间倒序；工作台「最近生成的报文」共用同一份数据
const { records, loading, load, ensureXml } = useReportRecords()

const keyword = ref('')
const statusFilter = ref<'ALL' | 'UPLOADED' | 'DEGRADED'>('ALL')
const detailVisible = ref(false)
const current = ref<ReportHistoryItem | null>(null)
const reuploadingId = ref('')
const detailError = ref('')

const filteredHistory = computed(() =>
  records.value.filter((item) => {
    const statusMatched =
      statusFilter.value === 'ALL' ||
      (statusFilter.value === 'UPLOADED' && item.uploaded) ||
      (statusFilter.value === 'DEGRADED' && !item.uploaded)
    const text = keyword.value.trim().toLowerCase()
    const keywordMatched =
      !text ||
      item.businessId.toLowerCase().includes(text) ||
      item.messageType.toLowerCase().includes(text) ||
      (item.receiptNo ?? '').toLowerCase().includes(text)
    return statusMatched && keywordMatched
  })
)

async function openDetail(row: ReportHistoryItem): Promise<void> {
  current.value = row
  detailError.value = ''
  detailVisible.value = true
  try {
    await ensureXml(row)
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : '报文读取失败'
  }
}

async function handleDownload(row: ReportHistoryItem): Promise<void> {
  try {
    const xml = await ensureXml(row)
    downloadTextFile(`${row.businessId}-${timestampForFilename()}.xml`, xml)
  } catch {
    ElMessage.error('读取报文失败，请确认报文文件仍在磁盘上')
  }
}

/** 重新上传：本地记录直接用缓存 XML，服务端记录先按需拉取。 */
async function handleReupload(row: ReportHistoryItem): Promise<void> {
  reuploadingId.value = row.id
  try {
    const xml = await ensureXml(row)
    const receipt = await uploadReceipt({
      businessId: row.businessId,
      messageType: row.messageType,
      xml
    })
    if (row.source === 'local') {
      reportStore.markReuploaded(row.id, receipt)
    }
    ElMessage.success(`重新上传成功，回执号 ${receipt.receiptNo}`)
  } catch {
    // 失败提示由 axios 拦截器统一给出
  } finally {
    reuploadingId.value = ''
  }
}

async function handleDelete(row: ReportHistoryItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除流水号 ${row.businessId} 的本地记录吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  reportStore.removeRecord(row.id)
  if (current.value?.id === row.id) {
    detailVisible.value = false
  }
}

async function handleClear(): Promise<void> {
  try {
    await ElMessageBox.confirm('确认清空全部本地记录吗？该操作不可恢复。', '提示', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  reportStore.clearHistory()
  detailVisible.value = false
  ElMessage.success('本地记录已清空')
}
</script>

<template>
  <div>
  <!-- 注意：注释必须放在根元素内部！放在 <template> 顶层会让组件变成「多根 Fragment」，
       <transition mode="out-in"> 要求单一子元素，离开阶段会卡住导致切换后内容区永久空白 -->
  <el-card class="page-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span>报文记录（{{ filteredHistory.length }} / {{ records.length }}）</span>
        <div class="card-header__actions">
          <el-input v-model="keyword" class="search" placeholder="流水号 / 类型 / 回执号" clearable :prefix-icon="'Search'" />
          <el-select v-model="statusFilter" class="filter" placeholder="上传状态">
            <el-option label="全部状态" value="ALL" />
            <el-option label="已上传" value="UPLOADED" />
            <el-option label="已降级" value="DEGRADED" />
          </el-select>
          <el-button :loading="loading" @click="load">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
          <el-button type="danger" plain :disabled="reportStore.total === 0" @click="handleClear">清空本地</el-button>
        </div>
      </div>
    </template>

    <el-alert
      class="page-card"
      type="info"
      :closable="false"
      show-icon
      title="记录来自两处：服务端回执（message-service 真实生成过、含业务模块经 RabbitMQ 异步生成的报文，只读）与浏览器本地留存（含 XML，可重新上传/删除）。"
    />

    <el-table v-loading="loading" :data="filteredHistory" empty-text="暂无记录" border>
      <el-table-column prop="businessId" label="业务流水号" min-width="220" show-overflow-tooltip />
      <el-table-column label="来源" width="90">
        <template #default="{ row }">
          <el-tag :type="row.source === 'local' ? 'info' : 'success'" size="small" effect="plain">
            {{ row.source === 'local' ? '本地' : '服务端' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="messageType" label="报文类型" width="130" />
      <el-table-column prop="createdAt" label="生成时间" width="170" />
      <el-table-column label="上传状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.uploaded ? 'success' : 'warning'" size="small">
            {{ row.uploaded ? '已上传' : '已降级' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="receiptNo" label="回执编号" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.receiptNo ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <!-- el-table 插槽给出的行类型是 DefaultRow（Record<PropertyKey, any>），
             这里在调用处收敛为业务类型，避免把表格组件的宽松类型扩散到页面方法上 -->
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as ReportHistoryItem)">详情</el-button>
          <el-button link type="primary" size="small" @click="handleDownload(row as ReportHistoryItem)">下载</el-button>
          <el-button
            link
            type="primary"
            size="small"
            :loading="reuploadingId === row.id"
            @click="handleReupload(row as ReportHistoryItem)"
          >
            重新上传
          </el-button>
          <!-- 服务端回执不归本地管理，只能看不能删 -->
          <el-button
            v-if="row.source === 'local'"
            link
            type="danger"
            size="small"
            @click="handleDelete(row as ReportHistoryItem)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-drawer v-model="detailVisible" title="报文详情" size="640px">
    <template v-if="current">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="业务流水号">{{ current.businessId }}</el-descriptions-item>
        <el-descriptions-item label="记录来源">{{ current.source === 'local' ? '浏览器本地' : '服务端回执' }}</el-descriptions-item>
        <el-descriptions-item label="报文类型">{{ current.messageType }}</el-descriptions-item>
        <el-descriptions-item label="生成时间">{{ current.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="上传状态">
          <el-tag :type="current.uploaded ? 'success' : 'warning'" size="small">
            {{ current.uploaded ? '已上传' : '已降级' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="回执编号">{{ current.receiptNo ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="落盘路径">{{ current.storedPath ?? '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="current.degradeReason" label="降级原因">{{ current.degradeReason }}</el-descriptions-item>
        <el-descriptions-item label="本地记录时间">{{ current.recordedAt }}</el-descriptions-item>
      </el-descriptions>

      <el-alert v-if="detailError" class="mt-16" type="warning" :closable="false" show-icon :title="detailError" />

      <div class="xml-title">XML 报文</div>
      <pre class="code-block">{{ current.xml || '（正在读取…）' }}</pre>
    </template>

    <template #footer>
      <el-button v-if="current" @click="handleDownload(current)">下载 XML</el-button>
      <el-button v-if="current" type="primary" :loading="reuploadingId === current.id" @click="handleReupload(current)">
        重新上传
      </el-button>
    </template>
  </el-drawer>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search {
  width: 220px;
}

.filter {
  width: 130px;
}

.xml-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
