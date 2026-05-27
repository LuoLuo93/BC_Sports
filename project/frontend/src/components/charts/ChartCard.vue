<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <div class="card-header">
        <span class="card-header-title">
          <el-icon v-if="icon" :size="16"><component :is="icon" /></el-icon>
          {{ title }}
        </span>
        <slot name="header-actions" />
      </div>
    </template>
    <div class="chart-container" :style="{ height: height + 'px' }">
      <slot>
        <v-chart v-if="option" :option="option" autoresize />
      </slot>
    </div>
  </el-card>
</template>

<script setup>
import VChart from 'vue-echarts'

defineProps({
  title: { type: String, default: '' },
  icon: { type: [String, Object], default: null },
  option: { type: Object, default: null },
  height: { type: Number, default: 350 }
})
</script>

<style scoped>
.chart-card {
  border-radius: 12px;
  border: none;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-header-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 0.9375rem;
}

.chart-container {
  width: 100%;
}

.chart-container :deep(.echarts) {
  width: 100% !important;
}
</style>
