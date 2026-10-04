<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, ref } from 'vue'

import { uploadReceipt } from '@/api/upload'
import { useReportStore } from '@/stores/report'
import type { ReportHistoryItem } from '@/types/api'
import { downloadTextFile } from '@/utils/download'
import { timestampForFilename } from '@/utils/format'

const reportStore = useReportStore()

const keyword = ref('')
const statusFilter = ref<'ALL' | 'UPLOADED' | 'DEGRADED'>('ALL')
const detailVisible = ref(false)
const current = ref<ReportHistoryItem | null>(null)
const reuploadingId = ref('')

const filteredHistory = computed(() =>
  reportStore.history.filter((item) => {
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

function openDetail(row: ReportHistoryItem): void {
  current.value = row
  detailVisible.value = true
}

function handleDownload(row: ReportHistoryItem): void {
  downloadTextFile(`${row.businessId}-${timestampForFilename()}.xml`, row.xml)
}

/** 降级记录可以直接拿本地存的 XML 重新提交给上传服务。 */
async function handleReupload(row: ReportHistoryItem): Promise<void> {
  reuploadingId.value = row.id
  try {
    const receipt = await uploadReceipt({
      businessId: row.businessId,
      messageType: row.messageType,
      xml: row.xml
    })
    reportStore.markReuploaded(row.id, receipt)
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
  <!-- 单一根节点：<transition> 只支持单个子元素，多根（如把 el-drawer 并列在外）会导致抽屉被丢弃 -->
  <div>
  <el-card class="page-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span>生成记录（{{ filteredHistory.length }} / {{ reportStore.total }}）</span>
        <div class="card-header__actions">
          <el-input v-model="keyword" class="search" placeholder="流水号 / 类型 / 回执号" clearable :prefix-icon="'Search'" />
          <el-select v-model="statusFilter" class="filter" placeholder="上传状态">
            <el-option label="全部状态" value="ALL" />
            <el-option label="已上传" value="UPLOADED" />
            <el-option label="已降级" value="DEGRADED" />
          </el-select>
          <el-button type="danger" plain :disabled="reportStore.total === 0" @click="handleClear">清空</el-button>
        </div>
      </div>
    </template>

    <el-alert
      class="page-card"
      type="info"
      :closable="false"
      show-icon
      title="记录保存在浏览器本地：后端目前只提供生成/上传接口，没有历史查询接口，接入真实列表接口后可替换为服务端分页。"
    />

    <el-table :data="filteredHistory" empty-text="暂无记录" border>
      <el-table-column prop="businessId" label="业务流水号" min-width="220" show-overflow-tooltip />
      <el-table-column prop="messageType" label="报文类型" width="110" />
      <el-table-column prop="createdAt" label="生成时间" width="170" />
      <el-table-column label="上传状态" width="110">
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
          <el-button link type="danger" size="small" @click="handleDelete(row as ReportHistoryItem)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-drawer v-model="detailVisible" title="报文详情" size="640px">
    <template v-if="current">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="业务流水号">{{ current.businessId }}</el-descriptions-item>
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

      <div class="xml-title">XML 报文</div>
      <pre class="code-block">{{ current.xml }}</pre>
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
