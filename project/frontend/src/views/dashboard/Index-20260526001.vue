<template>
  <div class="cockpit">
    <!-- Background Effects -->
    <div class="grid-bg"></div>
    <div class="scan-line"></div>
    <div class="particles">
      <span v-for="n in 30" :key="n" class="particle" :style="particleStyle(n)"></span>
    </div>

    <!-- Header -->
    <header class="cockpit-header">
      <div class="header-deco left-deco">
        <span class="deco-line"></span>
        <span class="deco-dot"></span>
      </div>
      <div class="header-center">
        <h1 class="header-title">BC Sport 数据驾驶舱</h1>
        <div class="header-sub">
          <span class="pulse-dot"></span>
          <span>实时监控 · 数据大屏</span>
          <span class="header-time">{{ currentTime }}</span>
        </div>
      </div>
      <div class="header-deco right-deco">
        <span class="deco-dot"></span>
        <span class="deco-line"></span>
      </div>
      <button class="fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
        <svg v-if="!isFullscreen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3H5a2 2 0 0 0-2 2v3"/><path d="M21 8V5a2 2 0 0 0-2-2h-3"/><path d="M3 16v3a2 2 0 0 0 2 2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-3"/></svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 14h6v6"/><path d="M20 10h-6V4"/><path d="M14 10l7-7"/><path d="M3 21l7-7"/></svg>
      </button>
    </header>

    <!-- KPI Row -->
    <div class="kpi-row">
      <div v-for="kpi in kpis" :key="kpi.label" class="kpi-card">
        <div class="kpi-corner tl"></div>
        <div class="kpi-corner tr"></div>
        <div class="kpi-corner bl"></div>
        <div class="kpi-corner br"></div>
        <div class="kpi-label">{{ kpi.label }}</div>
        <div class="kpi-value">
          <span class="kpi-number" :data-target="kpi.target">{{ kpi.prefix }}{{ animatedValues[kpi.label] }}{{ kpi.suffix }}</span>
        </div>
        <div class="kpi-trend" :class="kpi.trendClass">
          <span class="trend-arrow">{{ kpi.trendClass === 'up' ? '▲' : kpi.trendClass === 'down' ? '▼' : '●' }}</span>
          {{ kpi.trend }}
          <span class="trend-sub">{{ kpi.trendSub }}</span>
        </div>
      </div>
    </div>

    <!-- Charts Grid -->
    <div class="charts-grid">
      <!-- Row 1: Revenue Trend + Channel Pie -->
      <div class="panel panel-lg">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">营收趋势分析</span>
        </div>
        <div class="panel-body">
          <v-chart :option="revenueOption" autoresize />
        </div>
      </div>
      <div class="panel panel-sm">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">渠道分布</span>
        </div>
        <div class="panel-body">
          <v-chart :option="channelOption" autoresize />
        </div>
      </div>

      <!-- Row 2: Category Ranking + Shop TOP5 -->
      <div class="panel panel-sm">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">品类销售排行</span>
        </div>
        <div class="panel-body">
          <v-chart :option="categoryOption" autoresize />
        </div>
      </div>
      <div class="panel panel-sm">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">门店 TOP5</span>
        </div>
        <div class="panel-body">
          <v-chart :option="shopOption" autoresize />
        </div>
      </div>

      <!-- Row 3: Customer Growth + 3 Gauges -->
      <div class="panel panel-sm">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">客户增长趋势</span>
        </div>
        <div class="panel-body">
          <v-chart :option="customerOption" autoresize />
        </div>
      </div>
      <div class="panel panel-lg">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">运营指标</span>
        </div>
        <div class="panel-body gauges">
          <div v-for="g in gauges" :key="g.title" class="gauge-item">
            <gauge-chart :value="g.value" :max="g.max" :title="g.title" :color="g.color" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'Dashboard' })
import { ref, onMounted, onUnmounted, computed } from 'vue'
import VChart from 'vue-echarts'
import GaugeChart from '@/components/charts/GaugeChart.vue'

// ─── Fullscreen ────────────────────────────────────────
const isFullscreen = ref(false)
function toggleFullscreen() {
  const el = document.querySelector('.cockpit')
  if (!document.fullscreenElement) {
    el.requestFullscreen().then(() => { isFullscreen.value = true })
  } else {
    document.exitFullscreen().then(() => { isFullscreen.value = false })
  }
}
onMounted(() => {
  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement
  })
})

// ─── Clock ────────────────────────────────────────────
const currentTime = ref('')
let timer = null
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}
onMounted(() => { updateTime(); timer = setInterval(updateTime, 1000) })
onUnmounted(() => clearInterval(timer))

// ─── Particles ─────────────────────────────────────────
function particleStyle(n) {
  const s = 1 + Math.random() * 2
  return {
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    width: s + 'px', height: s + 'px',
    animationDelay: Math.random() * 6 + 's',
    animationDuration: 4 + Math.random() * 4 + 's'
  }
}

// ─── KPIs ──────────────────────────────────────────────
const kpis = ref([
  { label: '今日营收', target: 284600, prefix: '¥', suffix: '', trend: '+12.5%', trendClass: 'up', trendSub: '较昨日' },
  { label: '今日订单', target: 3472, prefix: '', suffix: '', trend: '+8.3%', trendClass: 'up', trendSub: '较昨日' },
  { label: '活跃客户', target: 15680, prefix: '', suffix: '', trend: '+3.2%', trendClass: 'up', trendSub: '较上周' },
  { label: '库存周转率', target: 94, prefix: '', suffix: '%', trend: '-1.5%', trendClass: 'down', trendSub: '较上月' },
  { label: '门店数量', target: 128, prefix: '', suffix: '家', trend: '+5', trendClass: 'up', trendSub: '本季新增' },
  { label: '客单价', target: 328, prefix: '¥', suffix: '', trend: '+6.8%', trendClass: 'up', trendSub: '较上月' },
  { label: '退货率', target: 2.3, prefix: '', suffix: '%', trend: '-0.4%', trendClass: 'down', trendSub: '较上月' }
])

const animatedValues = ref({})
function playKpiAnimations() {
  kpis.value.forEach(kpi => {
    animateValue(kpi.label, kpi.target, kpi.suffix === '%' && kpi.target < 10 ? 1000 : 0)
  })
}
onMounted(() => playKpiAnimations())
function animateValue(key, target, decimals = 0) {
  const duration = 2000
  const start = performance.now()
  function tick(now) {
    const progress = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    const current = eased * target
    if (target >= 10000) {
      animatedValues.value[key] = (current / 10000).toFixed(1) + '万'
    } else if (decimals > 0) {
      animatedValues.value[key] = current.toFixed(decimals)
    } else {
      animatedValues.value[key] = Math.round(current).toLocaleString()
    }
    if (progress < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

// ─── Gauges ────────────────────────────────────────────
const gauges = ref([
  { value: 94, max: 100, title: '库存周转', color: ['#06b6d4', '#3b82f6'] },
  { value: 87, max: 100, title: '订单完成率', color: ['#10b981', '#34d399'] },
  { value: 15680, max: 20000, title: '会员总数', color: ['#8b5cf6', '#a78bfa'] }
])

// ─── Chart Colors ──────────────────────────────────────
const cyan = '#06b6d4'
const green = '#10b981'
const blue = '#3b82f6'
const purple = '#8b5cf6'
const amber = '#f59e0b'
const rose = '#f43f5e'

const darkTooltip = {
  backgroundColor: 'rgba(10,15,30,0.92)',
  borderColor: 'rgba(6,182,212,0.3)',
  borderWidth: 1,
  textStyle: { color: '#e2e8f0', fontSize: 12 }
}

const darkAxis = {
  axisLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
  axisTick: { show: false },
  axisLabel: { color: 'rgba(255,255,255,0.35)', fontSize: 10 },
  splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }
}

// ─── Chart Data (reactive for polling) ─────────────────
const months = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']

const revenueData = ref({
  orders: [1200, 1890, 1650, 2380, 2130, 2910, 2690, 3250, 3580, 3140, 3810, 4250],
  revenue: [86, 128, 95, 198, 165, 241, 219, 274, 296, 252, 318, 362]
})
const channelData = ref([
  { value: 735, name: '线上渠道' },
  { value: 580, name: '线下门店' },
  { value: 284, name: '经销商' },
  { value: 148, name: '大客户' },
  { value: 96, name: '其他' }
])
const categoryData = ref([186, 324, 456, 578, 842])
const shopData = ref([156, 198, 245, 312, 389])
const customerData = ref([820, 960, 1050, 1280, 1150, 1420, 1360, 1580, 1690, 1520, 1780, 1950])

// ─── Revenue Trend ─────────────────────────────────────
const revenueOption = computed(() => ({
  color: [cyan, green],
  tooltip: { ...darkTooltip, trigger: 'axis' },
  legend: {
    data: ['订单量', '营收(万)'],
    textStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 11 },
    top: 0, right: 0,
    itemWidth: 12, itemHeight: 8
  },
  grid: { left: '3%', right: '3%', bottom: '8%', top: '14%', containLabel: true },
  xAxis: { type: 'category', data: months, boundaryGap: false, ...darkAxis },
  yAxis: { type: 'value', ...darkAxis },
  series: [
    {
      name: '订单量', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
      lineStyle: { width: 2, shadowColor: cyan, shadowBlur: 6 },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(6,182,212,0.25)' }, { offset: 1, color: 'rgba(6,182,212,0)' }]
        }
      },
      data: revenueData.value.orders
    },
    {
      name: '营收(万)', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
      lineStyle: { width: 2, shadowColor: green, shadowBlur: 6 },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(16,185,129,0.2)' }, { offset: 1, color: 'rgba(16,185,129,0)' }]
        }
      },
      data: revenueData.value.revenue
    }
  ]
}))

// ─── Channel Pie ───────────────────────────────────────
const channelOption = computed(() => ({
  color: [cyan, green, purple, amber, rose],
  tooltip: { ...darkTooltip, trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  series: [{
    type: 'pie', radius: ['45%', '72%'], center: ['50%', '48%'],
    itemStyle: { borderColor: 'rgba(6,12,28,0.8)', borderWidth: 2, borderRadius: 4 },
    label: { color: 'rgba(255,255,255,0.55)', fontSize: 11, formatter: '{b}\n{d}%' },
    labelLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } },
    emphasis: { label: { fontSize: 13, fontWeight: 'bold', color: '#fff' } },
    data: channelData.value
  }]
}))

// ─── Category Ranking ──────────────────────────────────
const categoryOption = computed(() => ({
  color: [cyan],
  tooltip: { ...darkTooltip, trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '12%', bottom: '6%', top: '6%', containLabel: true },
  xAxis: { type: 'value', ...darkAxis },
  yAxis: { type: 'category', data: ['配件', '外套', '裤装', '鞋类', '上装'], ...darkAxis, inverse: true },
  series: [{
    type: 'bar', barWidth: 14,
    itemStyle: {
      borderRadius: [0, 3, 3, 0],
      color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
        colorStops: [{ offset: 0, color: 'rgba(6,182,212,0.3)' }, { offset: 1, color: cyan }]
      }
    },
    data: categoryData.value
  }]
}))

// ─── Shop TOP5 ─────────────────────────────────────────
const shopOption = computed(() => ({
  color: [green],
  tooltip: { ...darkTooltip, trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '12%', bottom: '6%', top: '6%', containLabel: true },
  xAxis: { type: 'value', ...darkAxis },
  yAxis: { type: 'category', data: ['杭州旗舰', '南京德基', '上海环贸', '成都太古', '北京三里'], ...darkAxis, inverse: true },
  series: [{
    type: 'bar', barWidth: 14,
    itemStyle: {
      borderRadius: [0, 3, 3, 0],
      color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
        colorStops: [{ offset: 0, color: 'rgba(16,185,129,0.3)' }, { offset: 1, color: green }]
      }
    },
    data: shopData.value
  }]
}))

// ─── Customer Growth ───────────────────────────────────
const customerOption = computed(() => ({
  color: [purple],
  tooltip: { ...darkTooltip, trigger: 'axis' },
  grid: { left: '3%', right: '3%', bottom: '8%', top: '8%', containLabel: true },
  xAxis: { type: 'category', data: months, ...darkAxis },
  yAxis: { type: 'value', ...darkAxis },
  series: [{
    type: 'bar', barWidth: 16,
    itemStyle: {
      borderRadius: [3, 3, 0, 0],
      color: { type: 'linear', x: 0, y: 1, x2: 0, y2: 0,
        colorStops: [{ offset: 0, color: 'rgba(139,92,246,0.2)' }, { offset: 1, color: purple }]
      }
    },
    data: customerData.value
  }]
}))

// ─── Polling (30s interval, replace with API calls later) ──
let pollTimer = null

function jitter(base, pct) {
  return Math.round(base * (1 + (Math.random() - 0.5) * 2 * pct))
}

async function refreshData() {
  // TODO: Replace with actual API calls when data is integrated
  // Example: const res = await fetch('/api/dashboard/kpi')
  kpis.value = [
    { label: '今日营收', target: jitter(284600, 0.05), prefix: '¥', suffix: '', trend: '+' + (10 + Math.random() * 5).toFixed(1) + '%', trendClass: 'up', trendSub: '较昨日' },
    { label: '今日订单', target: jitter(3472, 0.08), prefix: '', suffix: '', trend: '+' + (5 + Math.random() * 6).toFixed(1) + '%', trendClass: 'up', trendSub: '较昨日' },
    { label: '活跃客户', target: jitter(15680, 0.03), prefix: '', suffix: '', trend: '+' + (1 + Math.random() * 4).toFixed(1) + '%', trendClass: 'up', trendSub: '较上周' },
    { label: '库存周转率', target: jitter(94, 0.02), prefix: '', suffix: '%', trend: '-' + (Math.random() * 2).toFixed(1) + '%', trendClass: 'down', trendSub: '较上月' },
    { label: '门店数量', target: 128, prefix: '', suffix: '家', trend: '+5', trendClass: 'up', trendSub: '本季新增' },
    { label: '客单价', target: jitter(328, 0.06), prefix: '¥', suffix: '', trend: '+' + (4 + Math.random() * 5).toFixed(1) + '%', trendClass: 'up', trendSub: '较上月' },
    { label: '退货率', target: +(1.5 + Math.random() * 1.5).toFixed(1), prefix: '', suffix: '%', trend: '-' + (Math.random() * 0.5).toFixed(1) + '%', trendClass: 'down', trendSub: '较上月' }
  ]
  playKpiAnimations()

  revenueData.value = {
    orders: [1200, 1890, 1650, 2380, 2130, 2910, 2690, 3250, 3580, 3140, 3810, jitter(4250, 0.05)],
    revenue: [86, 128, 95, 198, 165, 241, 219, 274, 296, 252, 318, jitter(362, 0.05)]
  }
  channelData.value = [
    { value: jitter(735, 0.05), name: '线上渠道' },
    { value: jitter(580, 0.05), name: '线下门店' },
    { value: jitter(284, 0.05), name: '经销商' },
    { value: jitter(148, 0.05), name: '大客户' },
    { value: jitter(96, 0.05), name: '其他' }
  ]
  categoryData.value = categoryData.value.map(v => jitter(v, 0.04))
  shopData.value = shopData.value.map(v => jitter(v, 0.04))
  customerData.value = customerData.value.map((v, i) => i === customerData.value.length - 1 ? jitter(v, 0.06) : v)

  gauges.value = [
    { value: jitter(94, 0.02), max: 100, title: '库存周转', color: ['#06b6d4', '#3b82f6'] },
    { value: jitter(87, 0.03), max: 100, title: '订单完成率', color: ['#10b981', '#34d399'] },
    { value: jitter(15680, 0.03), max: 20000, title: '会员总数', color: ['#8b5cf6', '#a78bfa'] }
  ]
}

onMounted(() => { pollTimer = setInterval(refreshData, 30000) })
onUnmounted(() => { clearInterval(pollTimer) })
</script>

<style scoped>
/* ─── Base ──────────────────────────────── */
.cockpit {
  --bg: #060c1c;
  --panel-bg: rgba(6, 18, 42, 0.85);
  --border-glow: rgba(6, 182, 212, 0.25);
  --cyan: #06b6d4;
  --green: #10b981;
  position: relative;
  height: calc(100vh - 56px - 38px - 32px);
  background: var(--bg);
  padding: 16px 20px 24px;
  overflow: hidden;
  color: #e2e8f0;
  margin: -20px -24px;
  display: flex;
  flex-direction: column;
}
.cockpit:fullscreen {
  height: 100vh;
}

/* ─── Animated Grid Background ─────────── */
.grid-bg {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(6,182,212,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(6,182,212,0.04) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
  z-index: 0;
}

/* ─── Scan Line ─────────────────────────── */
.scan-line {
  position: fixed;
  left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, var(--cyan) 50%, transparent 100%);
  opacity: 0.15;
  animation: scanDown 8s linear infinite;
  pointer-events: none;
  z-index: 1;
}
@keyframes scanDown {
  0% { top: -2px; }
  100% { top: 100%; }
}

/* ─── Particles ─────────────────────────── */
.particles { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.particle {
  position: absolute;
  background: var(--cyan);
  border-radius: 50%;
  opacity: 0;
  animation: particleFade 6s ease-in-out infinite;
}
@keyframes particleFade {
  0%, 100% { opacity: 0; transform: translateY(0); }
  50% { opacity: 0.6; transform: translateY(-20px); }
}

/* ─── Header ────────────────────────────── */
.cockpit-header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(6,182,212,0.12);
}

.fullscreen-btn {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(6,182,212,0.1);
  border: 1px solid rgba(6,182,212,0.3);
  border-radius: 6px;
  color: rgba(6,182,212,0.7);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  padding: 0;
}
.fullscreen-btn:hover {
  background: rgba(6,182,212,0.2);
  border-color: rgba(6,182,212,0.6);
  color: var(--cyan);
  box-shadow: 0 0 12px rgba(6,182,212,0.3);
}
.fullscreen-btn svg {
  width: 16px;
  height: 16px;
}
.header-center { text-align: center; }
.header-title {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 6px;
  background: linear-gradient(90deg, var(--cyan), #60a5fa, var(--cyan));
  background-size: 200% 100%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: titleShimmer 4s ease-in-out infinite;
  margin: 0;
  text-shadow: 0 0 30px rgba(6,182,212,0.3);
}
@keyframes titleShimmer {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}
.header-sub {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(255,255,255,0.35);
  margin-top: 6px;
}
.pulse-dot {
  width: 6px; height: 6px;
  background: var(--green);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
  box-shadow: 0 0 6px var(--green);
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.7); }
}
.header-time { font-family: "Outfit", monospace; letter-spacing: 1px; }
.header-deco {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  padding: 0 20px;
}
.left-deco { justify-content: flex-end; }
.right-deco { justify-content: flex-start; }
.deco-line {
  width: 120px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--cyan));
}
.right-deco .deco-line {
  background: linear-gradient(90deg, var(--cyan), transparent);
}
.deco-dot {
  width: 4px; height: 4px;
  background: var(--cyan);
  border-radius: 50%;
  box-shadow: 0 0 6px var(--cyan);
}

/* ─── KPI Cards ─────────────────────────── */
.kpi-row {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.kpi-card {
  position: relative;
  background: var(--panel-bg);
  border: 1px solid var(--border-glow);
  border-radius: 4px;
  padding: 14px 12px 10px;
  overflow: hidden;
  transition: border-color 0.3s;
}
.kpi-card:hover {
  border-color: rgba(6,182,212,0.5);
}
.kpi-card::before {
  content: '';
  position: absolute;
  top: 0; left: -100%; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--cyan), transparent);
  animation: borderScan 3s linear infinite;
}
@keyframes borderScan {
  0% { left: -100%; }
  100% { left: 100%; }
}
.kpi-label {
  font-size: 11px;
  color: rgba(255,255,255,0.4);
  letter-spacing: 1px;
  margin-bottom: 6px;
}
.kpi-value {
  font-size: 22px;
  font-weight: 800;
  font-family: "Outfit", monospace;
  color: #fff;
  text-shadow: 0 0 12px rgba(6,182,212,0.3);
  line-height: 1.3;
}
.kpi-trend {
  font-size: 11px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 3px;
}
.kpi-trend.up { color: var(--green); }
.kpi-trend.down { color: var(--cyan); }
.trend-arrow { font-size: 9px; }
.trend-sub { color: rgba(255,255,255,0.25); margin-left: 4px; }

/* KPI Corners */
.kpi-corner {
  position: absolute;
  width: 8px; height: 8px;
}
.kpi-corner::before, .kpi-corner::after {
  content: '';
  position: absolute;
  background: var(--cyan);
  opacity: 0.5;
}
.kpi-corner.tl { top: -1px; left: -1px; }
.kpi-corner.tl::before { top: 0; left: 0; width: 8px; height: 1px; }
.kpi-corner.tl::after { top: 0; left: 0; width: 1px; height: 8px; }
.kpi-corner.tr { top: -1px; right: -1px; }
.kpi-corner.tr::before { top: 0; right: 0; width: 8px; height: 1px; }
.kpi-corner.tr::after { top: 0; right: 0; width: 1px; height: 8px; }
.kpi-corner.bl { bottom: -1px; left: -1px; }
.kpi-corner.bl::before { bottom: 0; left: 0; width: 8px; height: 1px; }
.kpi-corner.bl::after { bottom: 0; left: 0; width: 1px; height: 8px; }
.kpi-corner.br { bottom: -1px; right: -1px; }
.kpi-corner.br::before { bottom: 0; right: 0; width: 8px; height: 1px; }
.kpi-corner.br::after { bottom: 0; right: 0; width: 1px; height: 8px; }

/* ─── Panels ────────────────────────────── */
.charts-grid {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 3fr 2fr;
  grid-template-rows: 1fr 1fr 1fr;
  gap: 12px;
  flex: 1;
  min-height: 0;
}
.panel {
  position: relative;
  background: var(--panel-bg);
  border: 1px solid var(--border-glow);
  border-radius: 4px;
  overflow: hidden;
  transition: border-color 0.3s;
  display: flex;
  flex-direction: column;
}
.panel:hover {
  border-color: rgba(6,182,212,0.45);
}
.panel-lg { }
.panel-sm { }
.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px 0;
}
.panel-dot {
  width: 6px; height: 6px;
  background: var(--cyan);
  border-radius: 50%;
  box-shadow: 0 0 6px var(--cyan);
  animation: pulse 2s ease-in-out infinite;
}
.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255,255,255,0.7);
  letter-spacing: 1px;
}
.panel-body {
  padding: 4px 10px 10px;
  flex: 1;
  min-height: 0;
}
.panel-body :deep(.echarts) {
  width: 100% !important;
  height: 100% !important;
}

/* Panel Corners */
.panel-corner {
  position: absolute;
  width: 12px; height: 12px;
}
.panel-corner::before, .panel-corner::after {
  content: '';
  position: absolute;
  background: var(--cyan);
  opacity: 0.4;
}
.panel-corner.tl { top: -1px; left: -1px; }
.panel-corner.tl::before { top: 0; left: 0; width: 12px; height: 1px; }
.panel-corner.tl::after { top: 0; left: 0; width: 1px; height: 12px; }
.panel-corner.tr { top: -1px; right: -1px; }
.panel-corner.tr::before { top: 0; right: 0; width: 12px; height: 1px; }
.panel-corner.tr::after { top: 0; right: 0; width: 1px; height: 12px; }
.panel-corner.bl { bottom: -1px; left: -1px; }
.panel-corner.bl::before { bottom: 0; left: 0; width: 12px; height: 1px; }
.panel-corner.bl::after { bottom: 0; left: 0; width: 1px; height: 12px; }
.panel-corner.br { bottom: -1px; right: -1px; }
.panel-corner.br::before { bottom: 0; right: 0; width: 12px; height: 1px; }
.panel-corner.br::after { bottom: 0; right: 0; width: 1px; height: 12px; }

/* Panel scan animation */
.panel::before {
  content: '';
  position: absolute;
  top: 0; left: -100%; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--cyan), transparent);
  animation: borderScan 4s linear infinite;
  opacity: 0.6;
}

/* ─── Gauges ────────────────────────────── */
.gauges {
  display: flex;
  align-items: center;
  justify-content: space-around;
  flex: 1;
  min-height: 0;
}
.gauge-item {
  flex: 1;
  height: 100%;
}
.gauge-item :deep(.echarts) {
  width: 100% !important;
  height: 100% !important;
}

/* ─── Responsive ────────────────────────── */
@media (max-width: 1200px) {
  .kpi-row {
    grid-template-columns: repeat(4, 1fr);
  }
  .charts-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 768px) {
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .header-deco { display: none; }
  .header-title { font-size: 20px; letter-spacing: 3px; }
}
</style>
