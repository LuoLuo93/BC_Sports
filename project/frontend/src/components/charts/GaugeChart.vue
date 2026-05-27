<template>
  <v-chart :option="computedOption" autoresize />
</template>

<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 100 },
  title: { type: String, default: '' },
  color: { type: Array, default: () => ['#3b82f6', '#10b981'] }
})

const computedOption = computed(() => ({
  series: [{
    type: 'gauge',
    startAngle: 225,
    endAngle: -45,
    min: 0,
    max: props.max,
    progress: {
      show: true,
      width: 10,
      roundCap: true,
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: props.color.map((c, i) => ({
            offset: props.color.length === 1 ? 1 : i / (props.color.length - 1),
            color: c
          }))
        },
        shadowColor: props.color[0],
        shadowBlur: 6
      }
    },
    axisLine: {
      lineStyle: { width: 10, color: [[1, 'rgba(15,23,42,0.06)']] }
    },
    axisTick: { show: false },
    splitLine: { show: false },
    axisLabel: { show: false },
    pointer: { show: false },
    anchor: { show: false },
    title: {
      show: true,
      offsetCenter: [0, '75%'],
      fontSize: 11,
      color: '#64748b',
      fontWeight: 500
    },
    detail: {
      valueAnimation: true,
      offsetCenter: [0, '35%'],
      fontSize: 22,
      fontWeight: 700,
      color: '#0f172a',
      fontFamily: '"Outfit", monospace',
      formatter: `{value}${props.max === 100 ? '%' : ''}`
    },
    data: [{ value: props.value, name: props.title }]
  }]
}))
</script>
