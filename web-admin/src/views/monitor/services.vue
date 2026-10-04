<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'

import { fetchGatewayRoutes } from '@/api/monitor'
import { useServiceHealth } from '@/composables/useServiceHealth'
import type { GatewayRouteView } from '@/types/api'
import { healthTagType } from '@/utils/format'

const { loading, services, onlineCount, refresh } = useServiceHealth()

const routes = ref<GatewayRouteView[]>([])
const routesLoading = ref(false)
const routesError = ref('')

/** 网关路由表来自 Actuator，是验证 Nacos 配置是否生效最直接的方式。 */
async function loadRoutes(): Promise<void> {
  routesLoading.value = true
  routesError.value = ''
  try {
    routes.value = await fetchGatewayRoutes()
  } catch (error) {
    routes.value = []
    routesError.value = error instanceof Error ? error.message : '网关路由加载失败'
  } finally {
    routesLoading.value = false
  }
}

async function refreshAll(): Promise<void> {
  // 两者互不依赖，并发拉取更快
  await Promise.all([refresh(), loadRoutes()])
}

onMounted(() => {
  void refreshAll()
})
</script>

<template>
  <div>
    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>
            服务健康
            <el-tag class="online-tag" :type="onlineCount === services.length ? 'success' : 'warning'" size="small">
              {{ onlineCount }} / {{ services.length }} 在线
            </el-tag>
          </span>
          <el-button type="primary" :loading="loading" @click="refreshAll">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </template>

      <el-row v-loading="loading" :gutter="16">
        <el-col v-for="service in services" :key="service.key" :xs="24" :md="12">
          <div class="service" :class="{ 'service--down': service.status !== 'UP' }">
            <div class="service__head">
              <span class="service__name">{{ service.name }}</span>
              <el-tag :type="healthTagType(service.status)" size="small" effect="dark">{{ service.status }}</el-tag>
            </div>
            <div class="service__row text-muted">服务名：{{ service.key }}</div>
            <div class="service__row text-muted">端口：{{ service.port }}</div>
            <div class="service__row text-muted">健康地址：{{ service.endpoint }}</div>
            <div class="service__row">{{ service.detail }}</div>
          </div>
        </el-col>
      </el-row>

      <el-alert
        class="mt-16"
        type="info"
        :closable="false"
        show-icon
        title="指标为 0 时优先确认：Nacos 是否已启动（127.0.0.1:8848）、服务是否注册到 bizmsg 命名空间、开发服务器代理是否指向 8080/8081。"
      />
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>网关路由表（Nacos：gateway-service.yml）</span>
          <el-button type="primary" link :loading="routesLoading" @click="loadRoutes">重新加载</el-button>
        </div>
      </template>

      <el-alert v-if="routesError" class="page-card" type="warning" :closable="false" show-icon :title="routesError" />

      <el-table v-loading="routesLoading" :data="routes" empty-text="暂无路由，请确认网关已启动" border>
        <el-table-column prop="id" label="路由 ID" width="200" />
        <el-table-column prop="uri" label="目标 URI（lb:// 走服务发现）" width="240" />
        <el-table-column label="断言" min-width="280">
          <template #default="{ row }">
            <el-tag v-for="predicate in row.predicates" :key="predicate" class="predicate-tag" size="small" type="info">
              {{ predicate }}
            </el-tag>
            <span v-if="row.predicates.length === 0" class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="order" label="顺序" width="90" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.online-tag {
  margin-left: 8px;
}

.service {
  padding: 14px;
  margin-bottom: 12px;
  border: 1px solid #e6e8eb;
  border-left: 4px solid #2ea121;
  border-radius: 8px;
}

.service--down {
  border-left-color: #f56c6c;
}

.service__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.service__name {
  font-weight: 600;
}

.service__row {
  font-size: 12px;
  line-height: 1.9;
  word-break: break-all;
}

.predicate-tag {
  margin: 2px 4px 2px 0;
}

.mt-16 {
  margin-top: 16px;
}
</style>
