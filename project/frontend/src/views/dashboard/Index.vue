<template>
  <div class="cockpit" ref="cockpitRef">
    <!-- ============ Header ============ -->
    <header class="cockpit-header">
      <div class="header-left">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
          </svg>
        </div>
        <h1 class="header-title">数据驾驶舱</h1>
      </div>
      <div class="header-right">
        <span class="header-time">{{ currentTime }}</span>
        <button class="icon-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
          <svg v-if="!isFullscreen" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 3H5a2 2 0 0 0-2 2v3"/><path d="M21 8V5a2 2 0 0 0-2-2h-3"/>
            <path d="M3 16v3a2 2 0 0 0 2 2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-3"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 14h6v6"/><path d="M20 10h-6V4"/><path d="M14 10l7-7"/><path d="M3 21l7-7"/>
          </svg>
        </button>
      </div>
    </header>

    <!-- ============ 第一行：KPI 卡片 ============ -->
    <section class="kpi-row">
      <div
        v-for="(kpi, idx) in kpis"
        :key="kpi.key"
        class="kpi-card"
        :style="{ '--accent': kpiColors[idx], animationDelay: 0.06 * idx + 's' }"
      >
        <div class="kpi-bar"></div>
        <div class="kpi-body">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value">
            <span class="kpi-prefix" v-if="kpi.prefix">{{ kpi.prefix }}</span>
            <span class="kpi-num">{{ formatKpi(kpi) }}</span>
            <span class="kpi-unit" v-if="kpi.unit">{{ kpi.unit }}</span>
          </div>
          <div class="kpi-trend" :class="kpi.trend >= 0 ? 'up' : 'down'">
            <span class="trend-arrow">{{ kpi.trend >= 0 ? '▲' : '▼' }}</span>
            <span>{{ Math.abs(kpi.trend) }}%</span>
            <span class="trend-label">{{ kpi.trendLabel }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 第二行：销售趋势 + 目标达成 ============ -->
    <section class="row-2">
      <div class="card trend-card">
        <div class="card-header">
          <span class="card-title">销售趋势</span>
          <span class="card-badge">近12个月</span>
          <div class="card-legend">
            <span class="legend-item"><i style="background:#3b82f6"></i>销售额(万)</span>
            <span class="legend-item"><i style="background:#22c55e"></i>订单量</span>
          </div>
        </div>
        <div class="card-body">
          <v-chart :option="salesTrendOption" autoresize />
        </div>
      </div>

      <div class="card goal-card">
        <div class="card-header">
          <span class="card-title">本月目标达成</span>
          <span class="card-badge">June</span>
        </div>
        <div class="goal-body">
          <div class="goal-ring-wrap">
            <svg viewBox="0 0 160 160" class="goal-ring">
              <circle cx="80" cy="80" r="66" fill="none" stroke="#f1f5f9" stroke-width="12" />
              <circle
                cx="80" cy="80" r="66" fill="none"
                stroke="#22c55e" stroke-width="12"
                stroke-linecap="round"
                :stroke-dasharray="ringCircumference"
                :stroke-dashoffset="ringDashOffset"
                transform="rotate(-90 80 80)"
                class="ring-progress"
              />
            </svg>
            <div class="goal-center">
              <div class="goal-percent">{{ goal.percent }}<span>%</span></div>
              <div class="goal-label">达成率</div>
            </div>
          </div>
          <div class="goal-stats">
            <div class="goal-stat">
              <span class="gs-label">已完成</span>
              <span class="gs-value">{{ goal.current }}<small>万</small></span>
            </div>
            <div class="goal-stat">
              <span class="gs-label">目标</span>
              <span class="gs-value">{{ goal.target }}<small>万</small></span>
            </div>
            <div class="goal-stat">
              <span class="gs-label">差额</span>
              <span class="gs-value accent">{{ goal.remaining }}<small>万</small></span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 第三行：4 个分析面板 ============ -->
    <section class="row-3">
      <div class="card">
        <div class="card-header">
          <span class="card-title">渠道分布</span>
        </div>
        <div class="card-body">
          <v-chart :option="channelOption" autoresize />
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">品类销售排行</span>
          <span class="card-badge">TOP 5</span>
        </div>
        <div class="card-body">
          <v-chart :option="categoryOption" autoresize />
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">门店销售排行</span>
          <span class="card-badge">TOP 5</span>
        </div>
        <div class="card-body">
          <v-chart :option="shopOption" autoresize />
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">会员增长</span>
          <span class="card-badge">近12月</span>
        </div>
        <div class="card-body">
          <v-chart :option="memberOption" autoresize />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
defineOptions({ name: 'Dashboard' })
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import '@/utils/echarts'
import VChart from 'vue-echarts'

// ============ 实时时钟 ============
const currentTime = ref('')
let clockTimer = null
function updateTime() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  currentTime.value =
    `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ` +
    `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

// ============ 全屏切换 ============
const cockpitRef = ref(null)
const isFullscreen = ref(false)
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    cockpitRef.value?.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}
function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
  setTimeout(() => window.dispatchEvent(new Event('resize')), 120)
}

// ============ KPI 数据 ============
const kpiColors = ['#3b82f6', '#8b5cf6', '#22c55e', '#f97316']

const kpis = reactive([
  { key: 'todaySales', label: '今日销售额', value: 186500, prefix: '¥', format: 'comma', unit: '', trend: 12.4, trendLabel: '同比昨日' },
  { key: 'monthSales', label: '本月销售额', value: 3680000, prefix: '¥', format: 'wan', unit: '万', trend: 8.2, trendLabel: '环比上月' },
  { key: 'todayOrders', label: '今日订单', value: 862, prefix: '', format: 'comma', unit: '单', trend: 6.7, trendLabel: '同比昨日' },
  { key: 'activeMembers', label: '活跃会员', value: 23456, prefix: '', format: 'comma', unit: '人', trend: -1.2, trendLabel: '环比昨日' },
])

const goal = reactive({ current: 568, target: 728, percent: 78, remaining: 160 })

const salesData = ref([286, 312, 345, 398, 420, 468, 512, 488, 536, 502, 548, 568])
const orderData = ref([1820, 2100, 2350, 2680, 2810, 3120, 3480, 3260, 3580, 3320, 3680, 3850])
const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

const channelData = ref([
  { name: '线上商城', value: 38 },
  { name: '线下门店', value: 32 },
  { name: '经销商', value: 18 },
  { name: '企业客户', value: 12 },
])

const categoryData = ref([
  { name: '跑步装备', value: 42 },
  { name: '篮球装备', value: 28 },
  { name: '户外服饰', value: 18 },
  { name: '运动鞋', value: 8 },
  { name: '健身器材', value: 4 },
])

const shopData = ref([
  { name: '杭州旗舰', value: 86 },
  { name: '成都太古里', value: 72 },
  { name: '北京三里屯', value: 65 },
  { name: '长春万达', value: 52 },
  { name: '哈尔滨中央大街', value: 46 },
])

const memberData = ref([820, 932, 901, 1034, 1290, 1330, 1520, 1450, 1680, 1590, 1820, 1950])

// ============ 数字滚动动画 ============
const animatedValues = reactive({})
function formatKpi(kpi) {
  const v = animatedValues[kpi.key]
  if (v === undefined) return '0'
  if (kpi.format === 'wan') return (v / 10000).toFixed(1)
  return Math.round(v).toLocaleString('en-US')
}

function animateValue(key, to, duration = 1200) {
  const from = animatedValues[key] ?? 0
  const start = performance.now()
  function step(now) {
    const t = Math.min(1, (now - start) / duration)
    const eased = 1 - Math.pow(1 - t, 3)
    animatedValues[key] = from + (to - from) * eased
    if (t < 1) requestAnimationFrame(step)
    else animatedValues[key] = to
  }
  requestAnimationFrame(step)
}

function playKpiAnimations() {
  kpis.forEach((kpi) => animateValue(kpi.key, kpi.value))
}

// ============ 目标达成环形进度 ============
const ringCircumference = 2 * Math.PI * 66
const ringDashOffset = computed(() => ringCircumference * (1 - Math.max(0, Math.min(100, goal.percent)) / 100))

// ============ ECharts 配置 ============
const colors = { blue: '#3b82f6', green: '#22c55e', orange: '#f97316', violet: '#8b5cf6', cyan: '#06b6d4', slate: '#64748b', slateLight: '#94a3b8' }

const tooltipStyle = {
  backgroundColor: '#fff',
  borderColor: '#e2e8f0',
  borderWidth: 1,
  padding: [10, 14],
  textStyle: { color: '#1e293b', fontSize: 12, fontFamily: 'Inter, sans-serif' },
  extraCssText: 'border-radius: 10px; box-shadow: 0 4px 16px rgba(0,0,0,0.08);',
}

const salesTrendOption = computed(() => ({
  tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: 'rgba(59,130,246,0.2)', type: 'dashed' } } },
  legend: { show: false },
  grid: { left: 44, right: 48, top: 18, bottom: 28 },
  xAxis: {
    type: 'category', data: months, boundaryGap: false,
    axisLine: { lineStyle: { color: '#e2e8f0' } }, axisTick: { show: false },
    axisLabel: { color: colors.slate, fontSize: 11 },
  },
  yAxis: [
    { type: 'value', name: '万', nameTextStyle: { color: colors.slateLight, fontSize: 10 }, splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }, axisLabel: { color: colors.slate, fontSize: 11 } },
    { type: 'value', name: '单', nameTextStyle: { color: colors.slateLight, fontSize: 10 }, splitLine: { show: false }, axisLabel: { color: colors.slate, fontSize: 11 } },
  ],
  series: [
    {
      name: '销售额(万)', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6, showSymbol: false,
      data: salesData.value,
      lineStyle: { width: 2.5, color: colors.blue },
      itemStyle: { color: colors.blue },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(59,130,246,0.15)' }, { offset: 1, color: 'rgba(59,130,246,0)' }] } },
    },
    {
      name: '订单量', type: 'line', yAxisIndex: 1, smooth: true, symbol: 'circle', symbolSize: 5, showSymbol: false,
      data: orderData.value,
      lineStyle: { width: 2, color: colors.green },
      itemStyle: { color: colors.green },
    },
  ],
}))

const channelOption = computed(() => ({
  tooltip: { ...tooltipStyle, trigger: 'item', formatter: '{b}: {c}%' },
  legend: { orient: 'vertical', right: 6, top: 'center', icon: 'circle', itemWidth: 8, itemHeight: 8, itemGap: 10, textStyle: { color: colors.slate, fontSize: 11 } },
  series: [{
    type: 'pie', radius: ['46%', '70%'], center: ['38%', '50%'], avoidLabelOverlap: true,
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { show: false }, labelLine: { show: false },
    emphasis: { scale: true, scaleSize: 6, label: { show: true, position: 'center', formatter: '{b}\n{c}%', color: '#1e293b', fontSize: 13, fontWeight: 600 } },
    data: channelData.value.map((d, i) => ({ ...d, itemStyle: { color: [colors.blue, colors.green, colors.orange, colors.cyan][i] } })),
  }],
}))

const categoryOption = computed(() => {
  const max = Math.max(...categoryData.value.map((d) => d.value))
  return {
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c}%' },
    grid: { left: 8, right: 36, top: 10, bottom: 8, containLabel: true },
    xAxis: { type: 'value', max, show: false },
    yAxis: { type: 'category', data: categoryData.value.map((d) => d.name), inverse: true, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: colors.slate, fontSize: 11.5, margin: 12 } },
    series: [{
      type: 'bar', barWidth: '54%',
      data: categoryData.value.map((d) => ({
        value: d.value,
        itemStyle: { borderRadius: [0, 7, 7, 0], color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: 'rgba(59,130,246,0.4)' }, { offset: 1, color: colors.blue }] } },
      })),
      label: { show: true, position: 'right', formatter: '{c}%', color: colors.blue, fontSize: 11.5, fontWeight: 600 },
    }],
  }
})

const shopOption = computed(() => {
  const max = Math.max(...shopData.value.map((d) => d.value))
  return {
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c}万' },
    grid: { left: 8, right: 40, top: 10, bottom: 8, containLabel: true },
    xAxis: { type: 'value', max, show: false },
    yAxis: { type: 'category', data: shopData.value.map((d) => d.name), inverse: true, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: colors.slate, fontSize: 11.5, margin: 12 } },
    series: [{
      type: 'bar', barWidth: '54%',
      data: shopData.value.map((d) => ({
        value: d.value,
        itemStyle: { borderRadius: [0, 7, 7, 0], color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: 'rgba(34,197,94,0.4)' }, { offset: 1, color: colors.green }] } },
      })),
      label: { show: true, position: 'right', formatter: '{c}万', color: colors.green, fontSize: 11.5, fontWeight: 600 },
      showBackground: true, backgroundStyle: { color: '#f8fafc', borderRadius: [0, 7, 7, 0] },
    }],
  }
})

const memberOption = computed(() => ({
  tooltip: { ...tooltipStyle, trigger: 'axis', formatter: (params) => `${params[0].name}<br/>新增会员 <b>${params[0].value}</b> 人` },
  grid: { left: 8, right: 12, top: 18, bottom: 28, containLabel: true },
  xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisTick: { show: false }, axisLabel: { color: colors.slate, fontSize: 10.5, interval: 1 } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }, axisLabel: { color: colors.slate, fontSize: 11 } },
  series: [{
    type: 'bar', barWidth: '48%',
    data: memberData.value.map((v) => ({
      value: v,
      itemStyle: { borderRadius: [6, 6, 0, 0], color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: colors.violet }, { offset: 1, color: 'rgba(139,92,246,0.3)' }] } },
    })),
  }],
}))

// ============ 生命周期 ============
onMounted(() => {
  updateTime()
  clockTimer = setInterval(updateTime, 1000)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  nextTick(() => {
    playKpiAnimations()
    setTimeout(() => window.dispatchEvent(new Event('resize')), 120)
  })
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');

.cockpit {
  position: relative;
  width: 100%;
  height: calc(100vh - var(--layout-header-h) - var(--layout-tab-h) - var(--layout-footer-h) - var(--layout-pad-y) * 2);
  min-height: 680px;
  padding: 20px 24px 16px;
  margin: calc(-1 * var(--layout-pad-y)) calc(-1 * var(--layout-pad-x));
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
  background: #f5f7fa;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  color: #1e293b;
}

.cockpit:fullscreen {
  height: 100vh;
  padding: 24px 28px 20px;
  margin: 0;
}

/* ============ Header ============ */
.cockpit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #1e3a5f;
  color: #22c55e;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.02em;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-time {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  color: #3b82f6;
  border-color: #bfdbfe;
  background: #eff6ff;
}

/* ============ KPI 卡片 ============ */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  flex-shrink: 0;
}

.kpi-card {
  display: flex;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  animation: fadeUp 0.5s cubic-bezier(0.22, 1, 0.36, 1) backwards;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.kpi-bar {
  width: 4px;
  flex-shrink: 0;
  background: var(--accent);
}

.kpi-body {
  flex: 1;
  padding: 18px 20px;
  min-width: 0;
}

.kpi-label {
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  letter-spacing: 0.04em;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.1;
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.kpi-prefix {
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
}

.kpi-unit {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
  margin-left: 3px;
}

.kpi-trend {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.kpi-trend.up {
  color: #16a34a;
  background: #f0fdf4;
}

.kpi-trend.down {
  color: #ea580c;
  background: #fff7ed;
}

.trend-arrow {
  font-size: 8px;
}

.trend-label {
  font-weight: 500;
  color: #94a3b8;
  margin-left: 2px;
}

/* ============ 通用卡片 ============ */
.card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: fadeUp 0.6s cubic-bezier(0.22, 1, 0.36, 1) backwards;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px 0;
  flex-shrink: 0;
}

.card-title {
  font-size: 14.5px;
  font-weight: 600;
  color: #1e293b;
}

.card-badge {
  font-size: 10px;
  font-weight: 600;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 6px;
  letter-spacing: 0.04em;
}

.card-legend {
  margin-left: auto;
  display: flex;
  gap: 14px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #94a3b8;
}

.legend-item i {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.card-body {
  flex: 1;
  min-height: 0;
  padding: 8px 12px 12px;
}

.card-body :deep(.echarts) {
  width: 100% !important;
  height: 100% !important;
}

/* ============ 第二行 ============ */
.row-2 {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.trend-card .card-body {
  padding: 8px 12px 12px;
}

/* 目标达成 */
.goal-card {
  display: flex;
  flex-direction: column;
}

.goal-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 20px 20px;
  gap: 16px;
}

.goal-ring-wrap {
  position: relative;
  width: 140px;
  height: 140px;
}

.goal-ring {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 4px 12px rgba(34, 197, 94, 0.2));
}

.ring-progress {
  transition: stroke-dashoffset 1.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.goal-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.goal-percent {
  font-size: 36px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1;
}

.goal-percent span {
  font-size: 18px;
  color: #94a3b8;
  margin-left: 1px;
}

.goal-label {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
  letter-spacing: 0.06em;
}

.goal-stats {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
}

.goal-stat {
  text-align: center;
}

.gs-label {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.gs-value {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  font-variant-numeric: tabular-nums;
}

.gs-value.accent {
  color: #3b82f6;
}

.gs-value small {
  font-size: 10px;
  font-weight: 500;
  color: #94a3b8;
  margin-left: 1px;
}

/* ============ 第三行 ============ */
.row-3 {
  display: grid;
  grid-template-columns: 1fr 1.1fr 1.1fr 1.2fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

/* ============ 入场动画 ============ */
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============ 响应式 ============ */
@media (max-width: 1400px) {
  .row-3 { grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; }
  .kpi-value { font-size: 24px; }
}

@media (max-width: 1200px) {
  .cockpit { height: auto; min-height: 0; overflow-y: auto; }
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .row-2 { grid-template-columns: 1fr; flex: none; }
  .trend-card { min-height: 300px; }
  .goal-card { min-height: 260px; }
  .row-3 { grid-template-columns: 1fr 1fr; flex: none; }
  .row-3 > .card { min-height: 260px; }
}

@media (max-width: 768px) {
  .cockpit { padding: 14px 12px; }
  .kpi-row { grid-template-columns: 1fr; }
  .row-3 { grid-template-columns: 1fr; }
  .header-title { font-size: 16px; }
  .kpi-value { font-size: 22px; }
}
</style>
