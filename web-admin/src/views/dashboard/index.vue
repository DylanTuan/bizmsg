<script setup lang="ts">
import { CircleCheck, Clock, Connection, MagicStick, Tickets, Warning } from '@element-plus/icons-vue'
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { useReportRecords } from '@/composables/useReportRecords'
import { useServiceHealth } from '@/composables/useServiceHealth'
import { healthTagType } from '@/utils/format'

const router = useRouter()
const { loading: healthLoading, services, onlineCount, refresh } = useServiceHealth()

/**
 * 与「报文记录」页共用同一份数据：服务端回执（含业务模块经 MQ 异步生成的报文）+ 浏览器本地留存，
 * 已按生成时间倒序。之前这里只读本地 store，导致 MQ 异步生成的报文不出现、显示的反而是更早的记录。
 */
const { records, loading: recordsLoading, uploadedCount, degradedCount } = useReportRecords()

/** 最近 5 条，完整列表在「报文中心 - 报文记录」。 */
const recentRecords = computed(() => records.value.slice(0, 5))

// useReportRecords 内部已自动加载；服务健康需要显式触发一次
onMounted(() => {
  void refresh()
})
</script>

<template>
  <div class="dashboard">
    <el-row :gutter="16" class="dashboard__stats">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat">
            <el-icon class="stat__icon stat__icon--blue"><Connection /></el-icon>
            <div>
              <div class="stat__value">{{ onlineCount }} / {{ services.length }}</div>
              <div class="stat__label">在线服务</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat">
            <el-icon class="stat__icon stat__icon--purple"><Tickets /></el-icon>
            <div>
              <div class="stat__value">{{ records.length }}</div>
              <div class="stat__label">报文记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat">
            <el-icon class="stat__icon stat__icon--green"><CircleCheck /></el-icon>
            <div>
              <div class="stat__value">{{ uploadedCount }}</div>
              <div class="stat__label">上传成功</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat">
            <el-icon class="stat__icon stat__icon--orange"><Warning /></el-icon>
            <div>
              <div class="stat__value">{{ degradedCount }}</div>
              <div class="stat__label">降级未上传</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>服务状态</span>
          <div class="card-header__actions">
            <el-button type="primary" link @click="refresh()">刷新</el-button>
            <el-button type="primary" link @click="router.push('/monitor/services')">查看监控</el-button>
          </div>
        </div>
      </template>
      <el-row v-loading="healthLoading" :gutter="16">
        <el-col v-for="service in services" :key="service.key" :xs="24" :md="8">
          <div class="service">
            <div class="service__head">
              <span class="service__name">{{ service.name }}</span>
              <el-tag :type="healthTagType(service.status)" size="small" effect="dark">
                {{ service.status }}
              </el-tag>
            </div>
            <div class="service__meta">端口 {{ service.port }}</div>
            <div class="service__detail text-muted">{{ service.detail }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>快捷操作</span>
        </div>
      </template>
      <el-space wrap>
        <el-button type="primary" @click="router.push('/report/generate')">
          <el-icon><MagicStick /></el-icon>生成并上传报文
        </el-button>
        <el-button @click="router.push('/report/history')">
          <el-icon><Clock /></el-icon>查看报文记录
        </el-button>
      </el-space>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>最近生成的报文</span>
          <el-button type="primary" link @click="router.push('/report/history')">全部记录</el-button>
        </div>
      </template>
      <el-table v-loading="recordsLoading" :data="recentRecords" empty-text="暂无记录，去「商品房转移」办一笔业务或「报文生成」试一次">
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
        <el-table-column label="上传状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.uploaded ? 'success' : 'warning'" size="small">
              {{ row.uploaded ? '已上传' : '已降级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiptNo" label="回执编号" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard__stats :deep(.el-card) {
  margin-bottom: 16px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat__icon {
  padding: 10px;
  border-radius: 8px;
  font-size: 22px;
  color: #fff;
}

.stat__icon--blue {
  background: #1f6feb;
}

.stat__icon--purple {
  background: #7c5cff;
}

.stat__icon--green {
  background: #2ea121;
}

.stat__icon--orange {
  background: #e6a23c;
}

.stat__value {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
}

.stat__label {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header__actions {
  display: flex;
  gap: 12px;
}

.service {
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid #e6e8eb;
  border-radius: 8px;
}

.service__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.service__name {
  font-weight: 600;
}

.service__meta,
.service__detail {
  margin-top: 6px;
  font-size: 12px;
}
</style>
