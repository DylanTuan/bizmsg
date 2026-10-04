<script setup lang="ts">
import { computed } from 'vue'

import { useAppStore } from '@/stores/app'

import AppNavbar from './components/AppNavbar.vue'
import AppSidebar from './components/AppSidebar.vue'

const appStore = useAppStore()

/** 折叠时只保留图标宽度，用 CSS 变量统一控制，避免魔法数字散落各处。 */
const asideWidth = computed(() =>
  appStore.sidebarCollapsed ? 'var(--sidebar-collapse-width)' : 'var(--sidebar-width)'
)
</script>

<template>
  <el-container class="app-layout">
    <el-aside :width="asideWidth" class="app-layout__aside">
      <app-sidebar />
    </el-aside>

    <el-container class="app-layout__body">
      <el-header class="app-layout__header" height="var(--layout-header-height)">
        <app-navbar />
      </el-header>

      <el-main class="app-layout__main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide" mode="out-in">
            <!-- key 用路由路径：保证每个页面拿到独立的组件实例，切换行为可预期 -->
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  height: 100%;
}

.app-layout__aside {
  background: #1f2d3d;
  transition: width 0.24s ease;
}

.app-layout__body {
  min-width: 0;
}

.app-layout__header {
  padding: 0;
  background: #fff;
  border-bottom: 1px solid #e6e8eb;
}

.app-layout__main {
  padding: 16px;
  overflow-y: auto;
  background: var(--page-bg);
}

/* 页面切换动画：位移小一点，避免后台系统频繁跳动 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
