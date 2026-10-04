<script setup lang="ts">
import { CopyDocument, Download, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { reactive, ref } from 'vue'

import { generateReport } from '@/api/report'
import { DEFAULT_MESSAGE_TYPE, MESSAGE_TYPE_OPTIONS } from '@/constants'
import { useReportStore } from '@/stores/report'
import type { ReportUploadResult } from '@/types/api'
import { downloadTextFile } from '@/utils/download'
import { timestampForFilename } from '@/utils/format'

const reportStore = useReportStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const result = ref<ReportUploadResult | null>(null)

const form = reactive({
  businessId: '',
  messageType: DEFAULT_MESSAGE_TYPE as string,
  payload: '开户受理'
})

const rules: FormRules<typeof form> = {
  messageType: [{ required: true, message: '请选择报文类型', trigger: 'change' }],
  payload: [{ required: true, message: '请输入报文业务内容', trigger: 'blur' }]
}

/** 流水号可由用户指定（下游据此做幂等），也可以留空让后端生成 32 位 UUID。 */
function fillRandomBusinessId(): void {
  form.businessId = `BIZ${Date.now()}${Math.floor(Math.random() * 1000)}`
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const data = await generateReport({
      businessId: form.businessId || undefined,
      messageType: form.messageType,
      payload: form.payload
    })
    result.value = data
    // 本地留痕，便于「生成记录」页回溯
    reportStore.addRecord(data)
    if (data.uploaded) {
      ElMessage.success(`生成并上传成功，回执号 ${data.receiptNo}`)
    } else {
      ElMessage.warning('报文已生成，但下游上传失败（后端已降级返回）')
    }
  } catch {
    // 失败提示已由 axios 拦截器统一处理，这里只负责收尾
  } finally {
    submitting.value = false
  }
}

function handleReset(): void {
  formRef.value?.resetFields()
  result.value = null
}

async function copyXml(): Promise<void> {
  if (!result.value) {
    return
  }
  try {
    await navigator.clipboard.writeText(result.value.xml)
    ElMessage.success('XML 报文已复制到剪贴板')
  } catch {
    ElMessage.error('浏览器拒绝了剪贴板访问，请手动选择文本复制')
  }
}

function downloadXml(): void {
  if (!result.value) {
    return
  }
  downloadTextFile(`${result.value.businessId}-${timestampForFilename()}.xml`, result.value.xml)
}
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :md="10">
      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>报文参数</span>
            <el-button link type="primary" @click="fillRandomBusinessId">生成随机流水号</el-button>
          </div>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="业务流水号" prop="businessId">
            <el-input v-model="form.businessId" placeholder="留空则由后端生成 32 位 UUID" clearable />
          </el-form-item>
          <el-form-item label="报文类型" prop="messageType">
            <el-select v-model="form.messageType" class="full-width" placeholder="请选择">
              <el-option v-for="item in MESSAGE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="业务内容" prop="payload">
            <el-input
              v-model="form.payload"
              type="textarea"
              :rows="6"
              maxlength="500"
              show-word-limit
              placeholder="报文 payload，含中文无需转义，后端 JAXB 会做 XML 转义"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              <el-icon><MagicStick /></el-icon>生成并上传
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert type="info" :closable="false" show-icon title="调用链路">
          <div class="tip">
            前端 → 网关 :8080 <code>/api/report/generate</code> → message-service 生成 XML 并在同一进程内落盘、
            生成回执（合并前这一步是一次 OpenFeign 远程调用）。落盘失败时后端会降级返回（HTTP 200 +
            <code>uploaded=false</code>），因此这里必须看 <code>uploaded</code> 字段而不是只看请求是否成功。
          </div>
        </el-alert>
      </el-card>
    </el-col>

    <el-col :xs="24" :md="14">
      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>生成结果</span>
            <div v-if="result" class="card-header__actions">
              <el-button link type="primary" @click="copyXml">
                <el-icon><CopyDocument /></el-icon>复制
              </el-button>
              <el-button link type="primary" @click="downloadXml">
                <el-icon><Download /></el-icon>下载 XML
              </el-button>
            </div>
          </div>
        </template>

        <el-empty v-if="!result" description="提交后在这里查看 XML 报文与上传回执" />

        <template v-else>
          <el-alert
            class="page-card"
            :type="result.uploaded ? 'success' : 'warning'"
            :closable="false"
            show-icon
            :title="result.uploaded ? '报文已生成并成功上传' : '报文已生成，但上传失败（后端降级返回）'"
            :description="result.uploaded ? `回执编号：${result.receiptNo}` : String(result.degradeReason ?? '请检查报文落盘目录是否可写')"
          />

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="业务流水号" :span="2">{{ result.businessId }}</el-descriptions-item>
            <el-descriptions-item label="报文类型">{{ result.messageType }}</el-descriptions-item>
            <el-descriptions-item label="生成时间">{{ result.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="回执编号" :span="2">{{ result.receiptNo ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="落盘路径" :span="2">{{ result.storedPath ?? '—' }}</el-descriptions-item>
          </el-descriptions>

          <div class="xml-title">XML 报文</div>
          <pre class="code-block">{{ result.xml }}</pre>
        </template>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header__actions {
  display: flex;
  gap: 12px;
}

.full-width {
  width: 100%;
}

.tip {
  font-size: 12px;
  line-height: 1.7;
}

.xml-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
