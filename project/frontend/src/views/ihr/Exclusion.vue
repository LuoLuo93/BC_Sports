<template>
  <div class="page-placeholder">
    <el-empty :description="(route.meta.pageTitle || '排除管理') + ' - 开发中'" />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
