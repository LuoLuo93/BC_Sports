<template>
  <div class="cockpit">
    <!-- Topographic Background -->
    <div class="topo-bg"></div>

    <!-- Snow Fall -->
    <div class="snow-fall">
      <span v-for="n in 50" :key="n" class="snowflake" :style="snowStyle(n)"></span>
    </div>

    <!-- Header -->
    <header class="cockpit-header">
      <div class="header-deco left-deco">
        <svg width="160" height="24" viewBox="0 0 160 24" fill="none">
          <path d="M0 18 L20 18 L28 8 L44 16 L56 6 L72 14 L88 4 L104 12 L120 8 L136 16 L148 6 L160 10" stroke="#0ea5e9" stroke-width="1.2" fill="none" opacity="0.35" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div class="header-center">
        <h1 class="header-title">BC Sport 数据驾驶舱</h1>
        <div class="header-sub">
          <span class="status-dot"></span>
          <span class="sub-text">DATA LIVE</span>
          <span class="sub-sep">·</span>
          <span class="header-time">{{ currentTime }}</span>
        </div>
      </div>
      <div class="header-deco right-deco">
        <svg width="160" height="24" viewBox="0 0 160 24" fill="none">
          <path d="M0 10 L12 6 L28 14 L44 4 L60 12 L76 8 L92 16 L108 6 L124 14 L140 4 L152 18 L160 18" stroke="#0ea5e9" stroke-width="1.2" fill="none" opacity="0.35" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <button class="fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
        <svg v-if="!isFullscreen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M8 3H5a2 2 0 0 0-2 2v3"/><path d="M21 8V5a2 2 0 0 0-2-2h-3"/><path d="M3 16v3a2 2 0 0 0 2 2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-3"/></svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 14h6v6"/><path d="M20 10h-6V4"/><path d="M14 10l7-7"/><path d="M3 21l7-7"/></svg>
      </button>
    </header>

    <!-- HUD Bar -->
    <div class="hud-bar">
      <div class="hud-chip"><span>BC SPORT</span></div>
      <div class="hud-chip"><span>全国 {{ storeCount }} 家门店</span></div>
      <div class="hud-chip"><span>营业中</span></div>
      <div class="hud-chip signal"><span class="signal-dot"></span><span>数据实时更新</span></div>
    </div>

    <!-- KPI Row -->
    <div class="kpi-row">
      <div v-for="(kpi, idx) in kpis" :key="kpi.label" class="kpi-card" :style="{ animationDelay: 0.06 * idx + 's' }">
        <div class="kpi-label">{{ kpi.label }}</div>
        <div class="kpi-value">
          {{ kpi.prefix }}{{ animatedValues[kpi.label] }}{{ kpi.suffix }}
        </div>
        <div class="kpi-trend" :class="kpi.trendClass">
          <span class="trend-arrow">{{ kpi.trendClass === 'up' ? '↗' : kpi.trendClass === 'down' ? '↘' : '→' }}</span>
          {{ kpi.trend }}
          <span class="trend-sub">{{ kpi.trendSub }}</span>
        </div>
      </div>
    </div>

    <!-- Three-Column Layout -->
    <div class="immersive-layout">
      <!-- Left Column -->
      <div class="side-column left-col">
        <div class="panel" style="flex: 2; --anim-delay: 0.2s">
          <div class="panel-header">
            <span class="panel-indicator"></span>
            <span class="panel-title">销售趋势</span>
            <span class="panel-tag">12月</span>
          </div>
          <div class="panel-body">
            <v-chart :option="salesTrendOption" autoresize />
          </div>
        </div>
        <div class="panel" style="flex: 1; --anim-delay: 0.3s">
          <div class="panel-header">
            <span class="panel-indicator"></span>
            <span class="panel-title">渠道分布</span>
            <span class="panel-tag">MIX</span>
          </div>
          <div class="panel-body">
            <v-chart :option="channelOption" autoresize />
          </div>
        </div>
      </div>

      <!-- Center Column -->
      <div class="center-column">
        <div class="panel" style="flex: 1; --anim-delay: 0.25s">
          <div class="panel-header">
            <span class="panel-indicator"></span>
            <span class="panel-title">品类销售排行</span>
            <span class="panel-tag">TOP</span>
          </div>
          <div class="panel-body">
            <v-chart :option="categoryOption" autoresize />
          </div>
        </div>
        <div class="panel" style="flex: 1; --anim-delay: 0.35s">
          <div class="panel-header">
            <span class="panel-indicator"></span>
            <span class="panel-title">门店销售排行</span>
            <span class="panel-tag">TOP5</span>
          </div>
          <div class="panel-body">
            <v-chart :option="shopOption" autoresize />
          </div>
        </div>
      </div>

      <!-- Right Column -->
      <div class="side-column right-col">
        <div class="panel" style="flex: 2; --anim-delay: 0.5s">
          <div class="panel-header">
            <span class="panel-indicator"></span>
            <span class="panel-title">会员增长</span>
            <span class="panel-tag">趋势</span>
          </div>
          <div class="panel-body">
            <v-chart :option="memberGrowthOption" autoresize />
          </div>
        </div>
        <div class="panel" style="flex: 1.5; --anim-delay: 0.6s">
          <div class="panel-header">
            <span class="panel-indicator"></span>
            <span class="panel-title">订单来源</span>
            <span class="panel-tag">占比</span>
          </div>
          <div class="panel-body">
            <v-chart :option="orderSourceOption" autoresize />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'Dashboard' })
import { ref, onMounted, onUnmounted, computed } from 'vue'
import '@/utils/echarts'
import VChart from 'vue-echarts'

// ─── Fullscreen ────────────────────────
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

// ─── Clock ─────────────────────────────
const currentTime = ref('')
let clockTimer = null
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// ─── Snow Fall ─────────────────────────
function snowStyle(n) {
  const s = 2 + Math.random() * 4
  return {
    left: Math.random() * 100 + '%',
    width: s + 'px', height: s + 'px',
    animationDelay: Math.random() * 10 + 's',
    animationDuration: 8 + Math.random() * 12 + 's',
    opacity: 0.12 + Math.random() * 0.2
  }
}

// ─── Store Count ───────────────────────
const storeCount = ref(128)

// ─── KPIs ──────────────────────────────
const kpis = ref([
  { label: '今日销售额', target: 284600, prefix: '¥', suffix: '', trend: '+12.5%', trendClass: 'up', trendSub: '较昨日' },
  { label: '今日订单数', target: 3472, prefix: '', suffix: '单', trend: '+8.3%', trendClass: 'up', trendSub: '较昨日' },
  { label: '本月销售额', target: 5680000, prefix: '¥', suffix: '', trend: '+15.2%', trendClass: 'up', trendSub: '较上月' },
  { label: '客单价', target: 528, prefix: '¥', suffix: '', trend: '+6.8%', trendClass: 'up', trendSub: '较上月' },
  { label: '活跃会员', target: 15680, prefix: '', suffix: '人', trend: '+3.2%', trendClass: 'up', trendSub: '较上周' },
  { label: '退货率', target: 2.3, prefix: '', suffix: '%', trend: '-0.4%', trendClass: 'down', trendSub: '较上月' },
  { label: '连带率', target: 1.8, prefix: '', suffix: '', trend: '+0.2', trendClass: 'up', trendSub: '较上月' }
])

const animatedValues = ref({})
function playKpiAnimations() {
  kpis.value.forEach(kpi => {
    animateValue(kpi.label, kpi.target, kpi.suffix === '%' || kpi.label === '连带率' ? 1 : 0)
  })
}
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

// ─── Chart Colors ──────────────────────
const skiBlue = '#0ea5e9'
const skiIce = '#bae6fd'
const hikeGreen = '#10b981'
const trailOrange = '#f97316'
const auroraViolet = '#8b5cf6'

const tooltip = {
  backgroundColor: 'rgba(255, 255, 255, 0.95)',
  borderColor: 'rgba(15, 23, 42, 0.06)',
  borderWidth: 1,
  textStyle: { color: '#0f172a', fontSize: 12, fontFamily: 'Inter, sans-serif' },
  extraCssText: 'box-shadow: 0 4px 16px rgba(15,23,42,0.08); border-radius: 8px;'
}
const axis = {
  axisLine: { lineStyle: { color: 'rgba(15, 23, 42, 0.06)' } },
  axisTick: { show: false },
  axisLabel: { color: '#64748b', fontSize: 10, fontFamily: 'Inter, sans-serif' },
  splitLine: { lineStyle: { color: 'rgba(15, 23, 42, 0.06)' } }
}

// ─── Sales Trend (12 months) ───────────
const months = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']
const salesData = ref([320, 280, 350, 420, 380, 460, 510, 490, 530, 580, 620, 680])
const orderData = ref([1800, 1600, 2100, 2500, 2200, 2800, 3100, 2900, 3200, 3500, 3800, 4200])

const salesTrendOption = computed(() => ({
  color: [skiBlue, hikeGreen],
  tooltip: { ...tooltip, trigger: 'axis' },
  legend: {
    data: ['销售额(万)', '订单量'],
    textStyle: { color: '#64748b', fontSize: 11, fontFamily: 'Inter, sans-serif' },
    top: 0, right: 0, itemWidth: 12, itemHeight: 8
  },
  grid: { left: '3%', right: '3%', bottom: '8%', top: '14%', containLabel: true },
  xAxis: { type: 'category', data: months, boundaryGap: false, ...axis },
  yAxis: [
    { type: 'value', name: '万元', ...axis, nameTextStyle: { color: '#94a3b8', fontSize: 10 } },
    { type: 'value', name: '单', ...axis, nameTextStyle: { color: '#94a3b8', fontSize: 10 } }
  ],
  series: [
    {
      name: '销售额(万)', type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
      lineStyle: { width: 2.5, color: skiBlue },
      itemStyle: { color: skiBlue },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(14,165,233,0.25)' }, { offset: 1, color: 'rgba(14,165,233,0)' }] } },
      data: salesData.value
    },
    {
      name: '订单量', type: 'line', smooth: true, symbol: 'diamond', symbolSize: 5, yAxisIndex: 1,
      lineStyle: { width: 2, color: hikeGreen },
      itemStyle: { color: hikeGreen },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(16,185,129,0.15)' }, { offset: 1, color: 'rgba(16,185,129,0)' }] } },
      data: orderData.value
    }
  ]
}))

// ─── Channel Pie ───────────────────────
const channelData = ref([
  { value: 735, name: '线上商城' },
  { value: 580, name: '线下门店' },
  { value: 284, name: '经销商' },
  { value: 148, name: '大客户' },
  { value: 96, name: '团购' }
])

const channelOption = computed(() => ({
  color: [skiBlue, hikeGreen, auroraViolet, trailOrange, skiIce],
  tooltip: { ...tooltip, trigger: 'item', formatter: '{b}: {c}万 ({d}%)' },
  series: [{
    type: 'pie', radius: ['40%', '68%'], center: ['50%', '48%'],
    itemStyle: { borderColor: 'rgba(255,255,255,0.9)', borderWidth: 2, borderRadius: 6 },
    label: { color: '#64748b', fontSize: 10, fontFamily: 'Inter, sans-serif', formatter: '{b}\n{d}%' },
    labelLine: { lineStyle: { color: 'rgba(15,23,42,0.1)' } },
    emphasis: { label: { fontSize: 13, fontWeight: 'bold', color: '#0f172a' }, itemStyle: { shadowBlur: 12, shadowColor: 'rgba(14,165,233,0.15)' } },
    data: channelData.value
  }]
}))

// ─── Category Ranking ──────────────────
const categoryOption = computed(() => ({
  color: [skiBlue],
  tooltip: { ...tooltip, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c}万' },
  grid: { left: '3%', right: '14%', bottom: '6%', top: '6%', containLabel: true },
  xAxis: { type: 'value', ...axis },
  yAxis: { type: 'category', data: ['配件', '户外服饰', '徒步装备', '滑雪装备', '运动鞋'], ...axis, inverse: true },
  series: [{
    type: 'bar', barWidth: 16,
    itemStyle: {
      borderRadius: [0, 6, 6, 0],
      color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: 'rgba(14,165,233,0.06)' }, { offset: 1, color: skiBlue }] }
    },
    backgroundStyle: { color: 'rgba(14,165,233,0.03)', borderRadius: [0, 6, 6, 0] },
    showBackground: true,
    label: { show: true, position: 'right', formatter: '{c}万', fontSize: 11, color: '#64748b', fontFamily: 'Inter, sans-serif' },
    data: [86, 156, 210, 320, 480]
  }]
}))

// ─── Shop TOP5 ─────────────────────────
const shopOption = computed(() => ({
  color: [hikeGreen],
  tooltip: { ...tooltip, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c}万' },
  grid: { left: '3%', right: '14%', bottom: '6%', top: '6%', containLabel: true },
  xAxis: { type: 'value', ...axis },
  yAxis: { type: 'category', data: ['杭州旗舰', '成都太古', '北京三里', '长春万达', '哈尔滨中央'], ...axis, inverse: true },
  series: [{
    type: 'bar', barWidth: 16,
    itemStyle: {
      borderRadius: [0, 6, 6, 0],
      color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: 'rgba(16,185,129,0.06)' }, { offset: 1, color: hikeGreen }] }
    },
    backgroundStyle: { color: 'rgba(16,185,129,0.03)', borderRadius: [0, 6, 6, 0] },
    showBackground: true,
    label: { show: true, position: 'right', formatter: '{c}万', fontSize: 11, color: '#64748b', fontFamily: 'Inter, sans-serif' },
    data: [156, 198, 245, 312, 389]
  }]
}))

// ─── Member Growth ─────────────────────
const memberData = ref([820, 960, 1050, 1280, 1150, 1420, 1360, 1580, 1690, 1520, 1780, 1950])

const memberGrowthOption = computed(() => ({
  color: [auroraViolet],
  tooltip: { ...tooltip, trigger: 'axis', formatter: '{b}: {c}人' },
  grid: { left: '3%', right: '3%', bottom: '8%', top: '8%', containLabel: true },
  xAxis: { type: 'category', data: months, ...axis },
  yAxis: { type: 'value', ...axis },
  series: [{
    name: '新增会员', type: 'bar', barWidth: 14,
    itemStyle: {
      borderRadius: [6, 6, 0, 0],
      color: { type: 'linear', x: 0, y: 1, x2: 0, y2: 0, colorStops: [{ offset: 0, color: 'rgba(139,92,246,0.06)' }, { offset: 1, color: auroraViolet }] }
    },
    backgroundStyle: { color: 'rgba(139,92,246,0.03)', borderRadius: [6, 6, 0, 0] },
    showBackground: true,
    data: memberData.value
  }]
}))

// ─── Order Source ──────────────────────
const orderSourceOption = computed(() => ({
  color: [skiBlue, hikeGreen, trailOrange, auroraViolet],
  tooltip: { ...tooltip, trigger: 'item', formatter: '{b}: {c}单 ({d}%)' },
  series: [{
    type: 'pie', radius: ['45%', '72%'], center: ['50%', '48%'],
    roseType: 'radius',
    itemStyle: { borderColor: 'rgba(255,255,255,0.9)', borderWidth: 2, borderRadius: 6 },
    label: { color: '#64748b', fontSize: 10, fontFamily: 'Inter, sans-serif', formatter: '{b}\n{d}%' },
    labelLine: { lineStyle: { color: 'rgba(15,23,42,0.1)' } },
    emphasis: { label: { fontSize: 12, fontWeight: 'bold', color: '#0f172a' } },
    data: [
      { value: 1580, name: '小程序' },
      { value: 1120, name: '门店POS' },
      { value: 480, name: '经销商' },
      { value: 292, name: '企业团购' }
    ]
  }]
}))

// ─── Polling ───────────────────────────
function jitter(base, pct) {
  return Math.round(base * (1 + (Math.random() - 0.5) * 2 * pct))
}
let pollTimer = null
async function refreshData() {
  kpis.value = [
    { label: '今日销售额', target: jitter(284600, 0.05), prefix: '¥', suffix: '', trend: '+' + (10 + Math.random() * 5).toFixed(1) + '%', trendClass: 'up', trendSub: '较昨日' },
    { label: '今日订单数', target: jitter(3472, 0.08), prefix: '', suffix: '单', trend: '+' + (5 + Math.random() * 6).toFixed(1) + '%', trendClass: 'up', trendSub: '较昨日' },
    { label: '本月销售额', target: jitter(5680000, 0.02), prefix: '¥', suffix: '', trend: '+' + (12 + Math.random() * 5).toFixed(1) + '%', trendClass: 'up', trendSub: '较上月' },
    { label: '客单价', target: jitter(528, 0.04), prefix: '¥', suffix: '', trend: '+' + (4 + Math.random() * 4).toFixed(1) + '%', trendClass: 'up', trendSub: '较上月' },
    { label: '活跃会员', target: jitter(15680, 0.02), prefix: '', suffix: '人', trend: '+' + (1 + Math.random() * 3).toFixed(1) + '%', trendClass: 'up', trendSub: '较上周' },
    { label: '退货率', target: +(1.8 + Math.random() * 1).toFixed(1), prefix: '', suffix: '%', trend: '-' + (Math.random() * 0.5).toFixed(1) + '%', trendClass: 'down', trendSub: '较上月' },
    { label: '连带率', target: +(1.5 + Math.random() * 0.6).toFixed(1), prefix: '', suffix: '', trend: '+' + (Math.random() * 0.3).toFixed(1), trendClass: 'up', trendSub: '较上月' }
  ]
  playKpiAnimations()
  salesData.value = salesData.value.map((v, i) => i === salesData.value.length - 1 ? jitter(v, 0.05) : v)
  orderData.value = orderData.value.map((v, i) => i === orderData.value.length - 1 ? jitter(v, 0.06) : v)
  memberData.value = memberData.value.map((v, i) => i === memberData.value.length - 1 ? jitter(v, 0.06) : v)
}

// ─── Lifecycle ─────────────────────────
onMounted(() => {
  updateTime()
  clockTimer = setInterval(updateTime, 1000)
  playKpiAnimations()
  pollTimer = setInterval(refreshData, 30000)
})
onUnmounted(() => {
  clearInterval(clockTimer)
  clearInterval(pollTimer)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&family=Inter:wght@300;400;500;600&display=swap');

.cockpit {
  --ski-blue: #0ea5e9;
  --ski-ice: #bae6fd;
  --hike-green: #10b981;
  --trail-orange: #f97316;
  --aurora: #8b5cf6;
  --panel-bg: rgba(255, 255, 255, 0.7);
  --text-primary: #0f172a;
  --text-secondary: #64748b;
  position: relative;
  height: calc(100vh - 56px - 38px - 32px);
  background: radial-gradient(ellipse at 50% 0%, #f8fafc 0%, #e2e8f0 70%);
  padding: 16px 24px 24px;
  overflow: hidden;
  color: var(--text-primary);
  margin: -20px -24px;
  display: flex;
  flex-direction: column;
  font-family: 'Inter', system-ui, sans-serif;
}
.cockpit:fullscreen { height: 100vh; }

/* ─── Topographic Background ────────── */
.topo-bg {
  position: fixed; inset: 0; opacity: 0.04; pointer-events: none; z-index: 0;
  background-image: url("data:image/svg+xml,%3Csvg width='400' height='400' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M50 200 Q100 160 150 200 Q200 240 250 200 Q300 160 350 200' fill='none' stroke='rgba(15,23,42,1)' stroke-width='0.5'/%3E%3Cpath d='M30 240 Q90 200 150 240 Q210 280 270 240 Q330 200 370 240' fill='none' stroke='rgba(15,23,42,1)' stroke-width='0.5'/%3E%3Cpath d='M50 280 Q100 250 150 280 Q200 310 250 280 Q300 250 350 280' fill='none' stroke='rgba(15,23,42,1)' stroke-width='0.4'/%3E%3Cpath d='M70 160 Q120 130 170 160 Q220 190 270 160 Q320 130 370 160' fill='none' stroke='rgba(15,23,42,1)' stroke-width='0.4'/%3E%3Cpath d='M50 120 Q100 100 150 120 Q200 140 250 120 Q300 100 350 120' fill='none' stroke='rgba(15,23,42,1)' stroke-width='0.3'/%3E%3C/svg%3E");
  background-repeat: repeat; background-size: 400px 400px;
}

/* ─── Snow Fall ─────────────────────── */
.snow-fall { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.snowflake {
  position: absolute; top: -10px; background: #cbd5e1;
  border-radius: 50%; opacity: 0; animation: snowfall linear infinite;
}
@keyframes snowfall {
  0% { transform: translateY(-10px) translateX(0); opacity: 0; }
  10% { opacity: 0.5; }
  90% { opacity: 0.2; }
  100% { transform: translateY(100vh) translateX(30px); opacity: 0; }
}

/* ─── Header ────────────────────────── */
.cockpit-header {
  position: relative; z-index: 2; display: flex; align-items: center;
  justify-content: center; padding: 12px 0 16px; margin-bottom: 6px;
}
.header-center { text-align: center; }
.header-title {
  font-family: 'Outfit', sans-serif; font-size: 22px; font-weight: 700;
  letter-spacing: 3px; color: var(--text-primary); margin: 0;
}
.header-sub {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  font-size: 11px; color: var(--text-secondary); margin-top: 4px;
  font-family: 'Inter', sans-serif; letter-spacing: 0.5px;
}
.sub-text { color: rgba(14,165,233,0.6); }
.sub-sep { color: rgba(15,23,42,0.1); }
.header-time { font-family: 'Outfit', monospace; letter-spacing: 1px; color: var(--text-secondary); }
.status-dot {
  width: 5px; height: 5px; background: var(--hike-green);
  border-radius: 50%; box-shadow: 0 0 6px rgba(16,185,129,0.4);
  animation: statusPulse 2s ease-in-out infinite;
}
@keyframes statusPulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
.header-deco { flex: 1; display: flex; align-items: center; }
.left-deco { justify-content: flex-end; padding-right: 24px; }
.right-deco { justify-content: flex-start; padding-left: 24px; }
.fullscreen-btn {
  position: absolute; right: 16px; top: 50%; transform: translateY(-50%);
  background: rgba(15,23,42,0.04); border: 1px solid rgba(15,23,42,0.08);
  border-radius: 8px; color: var(--text-secondary);
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.25s ease; padding: 0;
}
.fullscreen-btn:hover { background: rgba(14,165,233,0.08); border-color: rgba(14,165,233,0.2); color: var(--ski-blue); }
.fullscreen-btn svg { width: 16px; height: 16px; }

/* ─── HUD Bar ──────────────────────── */
.hud-bar {
  position: relative; z-index: 2; display: flex; gap: 8px;
  margin-bottom: 10px; padding: 0 4px;
}
.hud-chip {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; font-family: 'Inter', sans-serif;
  color: var(--text-secondary); background: rgba(255,255,255,0.6);
  border: 1px solid rgba(15,23,42,0.06); border-radius: 20px;
  padding: 3px 12px; backdrop-filter: blur(8px);
}
.hud-chip.signal { margin-left: auto; }
.signal-dot {
  width: 5px; height: 5px; background: var(--hike-green);
  border-radius: 50%; animation: statusPulse 1.5s ease-in-out infinite;
}

/* ─── KPI Cards ─────────────────────── */
.kpi-row {
  position: relative; z-index: 2;
  display: grid; grid-template-columns: repeat(7, 1fr);
  gap: 10px; margin-bottom: 12px;
}
.kpi-card {
  background: var(--panel-bg); backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.8); border-radius: 16px;
  padding: 14px 12px 10px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  animation: fadeUp 0.5s ease-out both;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.05);
}
.kpi-card:hover { transform: translateY(-2px); box-shadow: 0 14px 44px rgba(15, 23, 42, 0.08); }
@keyframes fadeUp { from { transform: translateY(12px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.kpi-label {
  font-size: 10px; color: var(--text-secondary);
  letter-spacing: 0.5px; margin-bottom: 4px; font-family: 'Inter', sans-serif;
}
.kpi-value {
  font-size: 20px; font-weight: 700; font-family: 'Outfit', sans-serif;
  color: var(--text-primary); line-height: 1.3;
}
.kpi-trend {
  font-size: 11px; margin-top: 4px;
  display: flex; align-items: center; gap: 3px;
  font-family: 'Inter', sans-serif;
}
.kpi-trend.up { color: var(--hike-green); }
.kpi-trend.down { color: var(--trail-orange); }
.trend-arrow { font-size: 12px; font-weight: 600; }
.trend-sub { color: rgba(15,23,42,0.2); margin-left: 3px; font-size: 10px; }

/* ─── Three-Column Layout ───────────── */
.immersive-layout {
  position: relative; z-index: 2;
  display: grid; grid-template-columns: 1fr 1.5fr 1fr;
  gap: 10px; flex: 1; min-height: 0;
}
.side-column { display: flex; flex-direction: column; gap: 10px; min-height: 0; }
.left-col { grid-column: 1; }
.right-col { grid-column: 3; }
.center-column { grid-column: 2; display: flex; flex-direction: column; gap: 10px; min-height: 0; }

/* ─── Panel ─────────────────────────── */
.panel {
  position: relative; background: var(--panel-bg);
  backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.8); border-radius: 16px;
  overflow: hidden; transition: box-shadow 0.3s ease;
  display: flex; flex-direction: column;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.05);
  animation: fadeUp 0.6s ease-out both;
  animation-delay: var(--anim-delay, 0s);
}
.panel:hover { box-shadow: 0 14px 44px rgba(15, 23, 42, 0.08); }
.panel-header {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px 0;
}
.panel-indicator {
  width: 3px; height: 14px; background: var(--ski-blue);
  border-radius: 2px; opacity: 0.6;
}
.panel-title {
  font-family: 'Outfit', sans-serif; font-size: 12px; font-weight: 600;
  color: var(--text-primary); letter-spacing: 0.5px;
}
.panel-tag {
  margin-left: auto; font-size: 9px; font-family: 'Inter', sans-serif;
  color: rgba(14,165,233,0.55); background: rgba(14,165,233,0.06);
  padding: 2px 8px; border-radius: 10px; letter-spacing: 0.5px; font-weight: 500;
}
.panel-body { padding: 4px 10px 10px; flex: 1; min-height: 0; }
.panel-body :deep(.echarts) { width: 100% !important; height: 100% !important; }

/* ─── Responsive ────────────────────── */
@media (max-width: 1400px) {
  .immersive-layout { grid-template-columns: 1fr 1.5fr 1fr; }
}
@media (max-width: 1200px) {
  .kpi-row { grid-template-columns: repeat(4, 1fr); }
  .immersive-layout { display: flex; flex-direction: column; }
  .side-column { flex-direction: row !important; flex-wrap: wrap; }
  .side-column .panel { min-height: 200px; flex: 1 1 45% !important; }
  .center-column { flex-direction: row; flex-wrap: wrap; }
  .center-column .panel { flex: 1 1 45% !important; min-height: 200px; }
}
@media (max-width: 768px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .header-deco { display: none; }
  .header-title { font-size: 18px; letter-spacing: 2px; }
  .hud-bar { flex-wrap: wrap; gap: 6px; }
  .side-column { flex-direction: column !important; }
  .side-column .panel { min-height: 180px; flex: none !important; }
  .center-column { flex-direction: column; }
  .center-column .panel { flex: none !important; min-height: 180px; }
}
</style>
