<script setup lang="ts">
import { Check, Download, House, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { reactive, ref } from 'vue'

import { completeTransfer, createTransfer } from '@/api/business'
import { fetchReceipt, fetchReceiptXml } from '@/api/report'
import type { ReceiptView, TransferCreateRequest, TransferView } from '@/types/api'
import { downloadTextFile } from '@/utils/download'

/**
 * 报文是 MQ 异步生成的，办结后前端轮询回执；
 * 轮询间隔 × 最大次数 = 页面「等待报文」的耐心上限。
 */
const POLL_INTERVAL_MS = 1500
const POLL_MAX_ATTEMPTS = 8

const formRef = ref<FormInstance>()
const submitting = ref(false)
const completing = ref(false)
const waitingReceipt = ref(false)

const business = ref<TransferView | null>(null)
const receipt = ref<ReceiptView | null>(null)
const xml = ref('')
const receiptHint = ref('')

const form = reactive<TransferCreateRequest>({
  sellerName: '张三',
  sellerIdNo: '110101199001011234',
  buyerName: '李四',
  buyerIdNo: '110202199003034567',
  houseAddress: '北京市朝阳区某小区 1 号楼 101',
  houseArea: 88.5,
  housePrice: 3200000
})

const rules: FormRules<typeof form> = {
  sellerName: [{ required: true, message: '请输入卖方姓名', trigger: 'blur' }],
  sellerIdNo: [{ required: true, message: '请输入卖方证件号', trigger: 'blur' }],
  buyerName: [{ required: true, message: '请输入买方姓名', trigger: 'blur' }],
  buyerIdNo: [{ required: true, message: '请输入买方证件号', trigger: 'blur' }],
  houseAddress: [{ required: true, message: '请输入房屋坐落', trigger: 'blur' }],
  houseArea: [{ required: true, message: '请输入建筑面积', trigger: 'blur' }],
  housePrice: [{ required: true, message: '请输入成交价', trigger: 'blur' }]
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** 受理：登记买卖双方与房屋信息，拿到业务号 */
async function handleAccept(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    business.value = await createTransfer({ ...form })
    receipt.value = null
    xml.value = ''
    receiptHint.value = ''
    ElMessage.success(`受理成功，业务号 ${business.value.businessId}`)
  } catch {
    // 失败提示已由 axios 拦截器统一处理，这里只负责收尾
  } finally {
    submitting.value = false
  }
}

/** 办结：后端投递 MQ 事件并等待 broker confirm，成功后才推进状态 */
async function handleComplete(): Promise<void> {
  if (!business.value) {
    return
  }
  completing.value = true
  try {
    business.value = await completeTransfer(business.value.businessId)
    ElMessage.success('业务已办结，办结事件已投递到 RabbitMQ')
    await pollReceipt(business.value.businessId)
  } catch {
    // 503 表示「消息没投出去、状态未变更」，拦截器已提示可重试
  } finally {
    completing.value = false
  }
}

/**
 * 轮询回执，直到报文生成或超时。
 * 未生成时后端返回 404，属正常中间态，因此不用 ElMessage 报错，只更新页面提示。
 */
async function pollReceipt(businessId: string): Promise<void> {
  waitingReceipt.value = true
  receipt.value = null
  xml.value = ''
  receiptHint.value = ''

  try {
    for (let attempt = 1; attempt <= POLL_MAX_ATTEMPTS; attempt += 1) {
      try {
        receipt.value = await fetchReceipt(businessId)
        xml.value = (await fetchReceiptXml(businessId)).xml
        receiptHint.value = ''
        return
      } catch {
        if (attempt < POLL_MAX_ATTEMPTS) {
          receiptHint.value = `报文生成中…（已查询 ${attempt} 次）`
          await sleep(POLL_INTERVAL_MS)
        }
      }
    }
    receiptHint.value = `等待 ${(POLL_MAX_ATTEMPTS * POLL_INTERVAL_MS) / 1000}s 仍未查到报文：`
      + '请确认 RabbitMQ 已启动、message-service 正在消费，且消息没有落到死信队列'
  } finally {
    waitingReceipt.value = false
  }
}

async function handleQueryReceipt(): Promise<void> {
  if (business.value) {
    await pollReceipt(business.value.businessId)
  }
}

function downloadXml(): void {
  if (business.value && xml.value) {
    downloadTextFile(`${business.value.businessId}.xml`, xml.value)
  }
}

function handleReset(): void {
  formRef.value?.resetFields()
  business.value = null
  receipt.value = null
  xml.value = ''
  receiptHint.value = ''
}
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :md="10">
      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>商品房转移受理</span>
            <el-button link type="primary" @click="handleReset">重置</el-button>
          </div>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="卖方姓名" prop="sellerName">
            <el-input v-model="form.sellerName" placeholder="房屋出售方" />
          </el-form-item>
          <el-form-item label="卖方证件" prop="sellerIdNo">
            <el-input v-model="form.sellerIdNo" placeholder="身份证号" />
          </el-form-item>
          <el-form-item label="买方姓名" prop="buyerName">
            <el-input v-model="form.buyerName" placeholder="房屋购买方" />
          </el-form-item>
          <el-form-item label="买方证件" prop="buyerIdNo">
            <el-input v-model="form.buyerIdNo" placeholder="身份证号" />
          </el-form-item>
          <el-form-item label="房屋坐落" prop="houseAddress">
            <el-input v-model="form.houseAddress" type="textarea" :rows="2" maxlength="128" show-word-limit />
          </el-form-item>
          <el-form-item label="建筑面积" prop="houseArea">
            <el-input-number v-model="form.houseArea" :min="0.01" :precision="2" :step="1" class="full-width" />
          </el-form-item>
          <el-form-item label="成交价" prop="housePrice">
            <el-input-number v-model="form.housePrice" :min="0.01" :precision="2" :step="10000" class="full-width" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleAccept">
              <el-icon><House /></el-icon>受理业务
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert type="info" :closable="false" show-icon title="调用链路">
          <div class="tip">
            前端 → 网关 :8080 <code>/api/business/transfer</code> → business-service 受理；
            再 <code>/transfer/{businessId}/complete</code> 办结，此时才向 RabbitMQ 投递办结事件，
            由 message-service 异步消费并生成 XML 报文落盘。<strong>报文不是办结接口同步返回的</strong>，
            所以下方会轮询 <code>/api/report/receipt/{businessId}</code> 等待它出现。
          </div>
        </el-alert>
      </el-card>
    </el-col>

    <el-col :xs="24" :md="14">
      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>业务状态</span>
            <div v-if="business" class="card-header__actions">
              <el-button
                v-if="business.status === 'ACCEPTED'"
                type="primary"
                :loading="completing"
                @click="handleComplete"
              >
                <el-icon><Check /></el-icon>办结并发报文
              </el-button>
              <el-button v-else link type="primary" :loading="waitingReceipt" @click="handleQueryReceipt">
                <el-icon><Refresh /></el-icon>重新查询报文
              </el-button>
            </div>
          </div>
        </template>

        <el-empty v-if="!business" description="先受理一笔商品房转移业务" />

        <template v-else>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="业务号" :span="2">{{ business.businessId }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="business.status === 'COMPLETED' ? 'success' : 'warning'" size="small">
                {{ business.statusLabel }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="MQ 消息 ID">{{ business.messageId ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="受理时间">{{ business.acceptedAt }}</el-descriptions-item>
            <el-descriptions-item label="办结时间">{{ business.completedAt ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="房屋坐落" :span="2">{{ business.houseAddress }}</el-descriptions-item>
            <el-descriptions-item label="建筑面积">{{ business.houseArea }} ㎡</el-descriptions-item>
            <el-descriptions-item label="成交价">{{ business.housePrice }} 元</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-card>

      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>报文结果</span>
            <el-button v-if="xml" link type="primary" @click="downloadXml">
              <el-icon><Download /></el-icon>下载 XML
            </el-button>
          </div>
        </template>

        <el-alert v-if="receiptHint" type="warning" :closable="false" show-icon :title="receiptHint" />

        <el-empty
          v-else-if="!receipt"
          :description="business && business.status === 'COMPLETED' ? '报文尚未生成' : '业务办结后在这里查看生成的报文'"
        />

        <template v-else>
          <el-alert
            type="success"
            :closable="false"
            show-icon
            title="报文已由 message-service 异步生成并落盘"
            :description="`回执编号：${receipt.receiptNo}`"
          />
          <el-descriptions class="mt-16" :column="1" border size="small">
            <el-descriptions-item label="落盘路径">{{ receipt.storedPath }}</el-descriptions-item>
            <el-descriptions-item label="回执时间">{{ receipt.receivedAt }}</el-descriptions-item>
          </el-descriptions>
          <div class="xml-title">
            <el-icon><Search /></el-icon>XML 报文
          </div>
          <pre class="code-block">{{ xml }}</pre>
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
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
