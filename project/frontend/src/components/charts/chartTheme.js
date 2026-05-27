export const tooltip = {
  backgroundColor: 'rgba(255, 255, 255, 0.96)',
  borderColor: '#e5e7eb',
  borderWidth: 1,
  textStyle: { color: '#1c1917', fontSize: 13 }
}

export const axisLabel = { color: '#78716c', fontSize: 12 }

export const valueAxis = {
  type: 'value',
  axisLine: { show: false },
  axisTick: { show: false },
  splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
  axisLabel
}

export const categoryAxis = (data) => ({
  type: 'category',
  data,
  axisLine: { lineStyle: { color: '#e5e7eb' } },
  axisLabel,
  axisTick: { show: false }
})

export const grid = (overrides = {}) => ({
  left: '3%',
  right: '4%',
  bottom: '3%',
  top: '20px',
  containLabel: true,
  ...overrides
})

export const legend = (data) => ({
  data,
  bottom: 0,
  textStyle: { color: '#78716c', fontSize: 12 },
  itemWidth: 10,
  itemHeight: 10,
  itemGap: 16
})
