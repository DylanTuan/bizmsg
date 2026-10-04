<script setup lang="ts">
// 静态使用的图标显式导入（动态菜单图标走 main.ts 的全局注册）
import { ArrowDown, User } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

import { LOGIN_PATH } from '@/router/routes'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

import AppBreadcrumb from './AppBreadcrumb.vue'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()

/** 退出前二次确认，避免误点丢失当前页面上下文。 */
async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
  } catch {
    // 用户点了取消，什么都不做
    return
  }
  await userStore.logout()
  router.push({ path: LOGIN_PATH })
}
</script>

<template>
  <div class="app-navbar">
    <div class="app-navbar__left">
      <el-icon class="app-navbar__collapse" :title="appStore.sidebarCollapsed ? '展开菜单' : '收起菜单'" @click="appStore.toggleSidebar()">
        <component :is="appStore.sidebarCollapsed ? 'Expand' : 'Fold'" />
      </el-icon>
      <app-breadcrumb />
    </div>

    <div class="app-navbar__right">
      <el-tag size="small" type="info" effect="plain">网关 :8080</el-tag>
      <el-dropdown>
        <span class="app-navbar__user">
          <el-icon><User /></el-icon>
          {{ userStore.nickname }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>{{ userStore.userInfo?.roles?.join('、') || '未分配角色' }}</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped>
.app-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 16px;
}

.app-navbar__left,
.app-navbar__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-navbar__collapse {
  font-size: 18px;
  cursor: pointer;
  color: #606266;
}

.app-navbar__collapse:hover {
  color: var(--brand-color);
}

.app-navbar__user {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #303133;
  cursor: pointer;
  outline: none;
}
</style>
