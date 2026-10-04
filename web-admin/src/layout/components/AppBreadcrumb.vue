<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { HOME_PATH } from '@/router/routes'

const route = useRoute()

/** 取 matched 中有 title 的层级，最后一级不可点击。 */
const crumbs = computed(() =>
  route.matched
    .filter((item) => item.meta?.title)
    .map((item) => ({ path: item.path, title: item.meta.title as string }))
)
</script>

<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item :to="{ path: HOME_PATH }">首页</el-breadcrumb-item>
    <el-breadcrumb-item
      v-for="(item, index) in crumbs"
      :key="item.path"
      :to="index === crumbs.length - 1 ? undefined : { path: item.path }"
    >
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>
