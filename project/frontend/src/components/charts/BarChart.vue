<template>
  <v-chart :option="computedOption" autoresize />
</template>

<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { tooltip, valueAxis, categoryAxis, grid } from './chartTheme'

const props = defineProps({
  xAxisData: { type: Array, default: () => [] },
  series: { type: Array, default: () => [] },
  horizontal: { type: Boolean, default: false },
  colors: { type: Array, default: () => ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'] }
})

const computedOption = computed(() => ({
  color: props.colors,
  tooltip: { ...tooltip, trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: grid(),
  xAxis: props.horizontal ? valueAxis : categoryAxis(props.xAxisData),
  yAxis: props.horizontal ? categoryAxis(props.xAxisData) : valueAxis,
  series: props.series.map(s => ({
    type: 'bar',
    barWidth: '45%',
    itemStyle: { borderRadius: [4, 4, 0, 0] },
    ...s
  }))
}))
</script>
