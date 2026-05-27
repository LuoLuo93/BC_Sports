<template>
  <div class="breadcrumb-nav">
    <router-link to="/" class="breadcrumb-item" :class="{ active: route.path === '/' || route.path === '/index' }">
      <el-icon :size="14"><HomeFilled /></el-icon>
    </router-link>
    <template v-if="pageTitle">
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">{{ pageTitle }}</span>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled } from '@element-plus/icons-vue'

const route = useRoute()

const pageTitle = computed(() => {
  if (route.path === '/' || route.path === '/index') return ''
  const matched = route.matched[route.matched.length - 1]
  return matched?.meta?.pageTitle || ''
})
</script>

<style scoped>
.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8125rem;
  color: rgba(255, 255, 255, 0.6);
}
.breadcrumb-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  transition: color 0.2s;
  white-space: nowrap;
}
.breadcrumb-item:hover:not(.active) {
  color: rgba(255, 255, 255, 0.85);
}
.breadcrumb-item.active {
  color: rgba(255, 255, 255, 0.92);
  font-weight: 600;
  cursor: default;
}
.breadcrumb-sep {
  color: rgba(255, 255, 255, 0.3);
  font-size: 0.75rem;
}
</style>
