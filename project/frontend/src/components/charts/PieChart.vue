<template>
  <v-chart :option="computedOption" autoresize />
</template>

<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { tooltip, legend } from './chartTheme'

const props = defineProps({
  data: { type: Array, default: () => [] },
  roseType: { type: [String, Boolean], default: false },
  radius: { type: Array, default: () => ['40%', '70%'] },
  colors: { type: Array, default: () => ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4'] }
})

const computedOption = computed(() => ({
  color: props.colors,
  tooltip: { ...tooltip, trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: legend(props.data.map(d => d.name)),
  series: [{
    type: 'pie',
    radius: props.radius,
    center: ['50%', '45%'],
    roseType: props.roseType || undefined,
    avoidLabelOverlap: true,
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { show: true, formatter: '{b}\n{d}%', fontSize: 12, color: '#78716c' },
    emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
    data: props.data
  }]
}))
</script>
