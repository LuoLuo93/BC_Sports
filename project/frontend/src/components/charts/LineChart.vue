<template>
  <v-chart :option="computedOption" autoresize />
</template>

<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { tooltip, axisLabel, valueAxis, categoryAxis, grid, legend } from './chartTheme'

const props = defineProps({
  xAxisData: { type: Array, default: () => [] },
  series: { type: Array, default: () => [] },
  smooth: { type: Boolean, default: true },
  areaStyle: { type: Boolean, default: true },
  colors: { type: Array, default: () => ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'] }
})

const hasMultipleSeries = computed(() => props.series.length > 1)

const computedOption = computed(() => ({
  color: props.colors,
  tooltip: { ...tooltip, trigger: 'axis', axisPointer: { type: 'cross', crossStyle: { color: '#999' } } },
  legend: hasMultipleSeries.value ? legend(props.series.map(s => s.name)) : undefined,
  grid: grid(hasMultipleSeries.value ? { bottom: '12%' } : {}),
  xAxis: { ...categoryAxis(props.xAxisData), boundaryGap: false },
  yAxis: valueAxis,
  series: props.series.map(s => ({
    type: 'line',
    smooth: props.smooth,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { width: 2.5 },
    areaStyle: props.areaStyle ? {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: s.areaColor || 'rgba(59, 130, 246, 0.15)' },
          { offset: 1, color: 'rgba(59, 130, 246, 0.01)' }
        ]
      }
    } : undefined,
    emphasis: { focus: 'series' },
    ...s
  }))
}))
</script>
