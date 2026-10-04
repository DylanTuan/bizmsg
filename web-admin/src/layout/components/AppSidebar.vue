<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { APP_TITLE } from '@/constants'
import { constantRoutes } from '@/router/routes'
import { useAppStore } from '@/stores/app'

interface MenuNode {
  path: string
  title: string
  icon: string
}

interface MenuGroup extends MenuNode {
  /** 只有一个可见子路由时压平成一级菜单，避免出现「套一层」的子菜单 */
  single: boolean
  children: MenuNode[]
}

const route = useRoute()
const appStore = useAppStore()

function joinPath(parent: string, child: string): string {
  if (child.startsWith('/')) {
    return child
  }
  return `${parent.replace(/\/$/, '')}/${child}`
}

/** 菜单完全由路由表推导，新增页面只需改 router/routes.ts。 */
const menuGroups = computed<MenuGroup[]>(() => {
  const groups: MenuGroup[] = []
  for (const item of constantRoutes) {
    if (item.meta?.hidden) {
      continue
    }
    const visibleChildren = (item.children ?? []).filter((child) => !child.meta?.hidden)
    if (visibleChildren.length === 0) {
      continue
    }
    const single = visibleChildren.length === 1 && !item.meta?.alwaysShow
    groups.push({
      path: single ? joinPath(item.path, visibleChildren[0].path) : item.path,
      title: (single ? visibleChildren[0].meta?.title : item.meta?.title) ?? item.meta?.title ?? '',
      icon: (single ? visibleChildren[0].meta?.icon : item.meta?.icon) ?? item.meta?.icon ?? 'Menu',
      single,
      children: visibleChildren.map((child) => ({
        path: joinPath(item.path, child.path),
        title: child.meta?.title ?? '',
        icon: child.meta?.icon ?? 'Menu'
      }))
    })
  }
  return groups
})
</script>

<template>
  <div class="app-sidebar">
    <div class="app-sidebar__logo">
      <img class="app-sidebar__logo-img" src="/favicon.svg" alt="logo" />
      <span v-show="!appStore.sidebarCollapsed" class="app-sidebar__logo-text">{{ APP_TITLE }}</span>
    </div>

    <el-menu
      class="app-sidebar__menu"
      :default-active="route.path"
      :collapse="appStore.sidebarCollapsed"
      :collapse-transition="false"
      background-color="#1f2d3d"
      text-color="#c0c8d2"
      active-text-color="#ffffff"
      router
    >
      <template v-for="group in menuGroups" :key="group.path">
        <el-menu-item v-if="group.single" :index="group.path">
          <el-icon><component :is="group.icon" /></el-icon>
          <template #title>{{ group.title }}</template>
        </el-menu-item>

        <el-sub-menu v-else :index="group.path">
          <template #title>
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.title }}</span>
          </template>
          <el-menu-item v-for="child in group.children" :key="child.path" :index="child.path">
            <el-icon><component :is="child.icon" /></el-icon>
            <template #title>{{ child.title }}</template>
          </el-menu-item>
        </el-sub-menu>
      </template>
    </el-menu>
  </div>
</template>

<style scoped>
.app-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.app-sidebar__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: var(--layout-header-height);
  padding: 0 16px;
  flex-shrink: 0;
}

.app-sidebar__logo-img {
  width: 26px;
  height: 26px;
}

.app-sidebar__logo-text {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.app-sidebar__menu {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  border-right: none;
}

.app-sidebar__menu :deep(.el-menu-item.is-active) {
  background-color: var(--brand-color);
}
</style>
