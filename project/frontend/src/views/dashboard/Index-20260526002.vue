<template>
  <div class="cockpit">
    <!-- CRT Noise Texture -->
    <svg class="noise-svg" width="0" height="0">
      <filter id="noise">
        <feTurbulence type="fractalNoise" baseFrequency="0.65" numOctaves="3" stitchTiles="stitch" />
        <feColorMatrix type="saturate" values="0" />
      </filter>
    </svg>
    <div class="noise-overlay"></div>

    <!-- Grid Background -->
    <div class="grid-bg"></div>
    <div class="scan-line"></div>

    <!-- Particles -->
    <div class="particles">
      <span v-for="n in 40" :key="n" class="particle" :style="particleStyle(n)"></span>
    </div>

    <!-- Header -->
    <header class="cockpit-header">
      <div class="header-deco left-deco">
        <div class="deco-circuit">
          <svg width="140" height="20" viewBox="0 0 140 20">
            <path d="M0 10 L20 10 L25 5 L35 15 L45 5 L55 15 L65 5 L75 15 L85 5 L95 15 L105 5 L115 15 L125 5 L140 5" stroke="#00E5FF" stroke-width="0.8" fill="none" opacity="0.4" />
            <circle cx="140" cy="5" r="2" fill="#00E5FF" opacity="0.6">
              <animate attributeName="opacity" values="0.2;1;0.2" dur="1.5s" repeatCount="indefinite" />
            </circle>
          </svg>
        </div>
      </div>
      <div class="header-center">
        <div class="header-glitch-wrapper">
          <h1 class="header-title" data-text="BC Sport 数据驾驶舱">BC Sport 数据驾驶舱</h1>
        </div>
        <div class="header-sub">
          <span class="pulse-dot"></span>
          <span class="sub-label">SYS.STATUS: ONLINE</span>
          <span class="sub-sep">|</span>
          <span class="header-time">{{ currentTime }}</span>
          <span class="sub-sep">|</span>
          <span class="sub-coord">LAT: 31.2304°  LNG: 121.4737°</span>
        </div>
      </div>
      <div class="header-deco right-deco">
        <div class="deco-circuit">
          <svg width="140" height="20" viewBox="0 0 140 20">
            <path d="M0 5 L15 5 L25 15 L35 5 L45 15 L55 5 L65 15 L75 5 L85 15 L95 5 L105 15 L115 5 L120 10 L140 10" stroke="#00E5FF" stroke-width="0.8" fill="none" opacity="0.4" />
            <circle cx="0" cy="5" r="2" fill="#00E5FF" opacity="0.6">
              <animate attributeName="opacity" values="1;0.2;1" dur="1.8s" repeatCount="indefinite" />
            </circle>
          </svg>
        </div>
      </div>
      <button class="fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
        <svg v-if="!isFullscreen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M8 3H5a2 2 0 0 0-2 2v3"/><path d="M21 8V5a2 2 0 0 0-2-2h-3"/><path d="M3 16v3a2 2 0 0 0 2 2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-3"/></svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 14h6v6"/><path d="M20 10h-6V4"/><path d="M14 10l7-7"/><path d="M3 21l7-7"/></svg>
      </button>
    </header>

    <!-- Sub-header HUD Bar -->
    <div class="hud-bar">
      <div class="hud-item"><span class="hud-label">NODE</span><span class="hud-value">BC-DASH-01</span></div>
      <div class="hud-item"><span class="hud-label">UPTIME</span><span class="hud-value">00{{ ':' }}{{ String(Math.floor(Math.random() * 60)).padStart(2, '0') }}:{{ String(Math.floor(Math.random() * 60)).padStart(2, '0') }}</span></div>
      <div class="hud-item"><span class="hud-label">SYS.LOAD</span><span class="hud-value">23.7%</span></div>
      <div class="hud-item"><span class="hud-label">DATA.FLOW</span><span class="hud-value">1.42 Gbps</span></div>
      <div class="hud-item"><span class="hud-label">PROTOCOL</span><span class="hud-value">v2.4.1-REALTIME</span></div>
      <div class="hud-item hud-signal"><span class="signal-dot"></span><span class="hud-value">SIGNAL LOCKED</span></div>
    </div>

    <!-- KPI Row -->
    <div class="kpi-row">
      <div v-for="(kpi, idx) in kpis" :key="kpi.label" class="kpi-card" :style="{ animationDelay: 0.06 * idx + 's' }">
        <div class="kpi-corner tl"></div>
        <div class="kpi-corner tr"></div>
        <div class="kpi-corner bl"></div>
        <div class="kpi-corner br"></div>
        <div class="kpi-glow-bar"></div>
        <div class="kpi-label">{{ kpi.label }}</div>
        <div class="kpi-value">
          <span class="kpi-number" :data-target="kpi.target">{{ kpi.prefix }}{{ animatedValues[kpi.label] }}{{ kpi.suffix }}</span>
        </div>
        <div class="kpi-trend" :class="kpi.trendClass">
          <span class="trend-arrow">{{ kpi.trendClass === 'up' ? '▲' : kpi.trendClass === 'down' ? '▼' : '●' }}</span>
          {{ kpi.trend }}
          <span class="trend-sub">{{ kpi.trendSub }}</span>
        </div>
        <div class="kpi-metric-tag">{{ ['A','B','C','D','E','F','G'][idx] }}-{{ String(idx + 1).padStart(3, '0') }}</div>
      </div>
    </div>

    <!-- Charts Grid -->
    <div class="charts-grid">
      <!-- Row 1: Revenue Trend + Channel Pie -->
      <div class="panel panel-lg" style="--anim-delay: 0.25s">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-edge-top"></div>
        <div class="panel-edge-bottom"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">REVENUE_TREND // 营收趋势</span>
          <span class="panel-tag">REALTIME</span>
        </div>
        <div class="panel-body">
          <v-chart :option="revenueOption" autoresize />
        </div>
      </div>
      <div class="panel panel-sm" style="--anim-delay: 0.35s">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-edge-top"></div>
        <div class="panel-edge-bottom"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">CHANNEL // 渠道分布</span>
          <span class="panel-tag">LIVE</span>
        </div>
        <div class="panel-body">
          <v-chart :option="channelOption" autoresize />
        </div>
      </div>

      <!-- Row 2: Category Ranking + Shop TOP5 -->
      <div class="panel panel-sm" style="--anim-delay: 0.45s">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-edge-top"></div>
        <div class="panel-edge-bottom"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">CATEGORY // 品类排行</span>
          <span class="panel-tag">RANK</span>
        </div>
        <div class="panel-body">
          <v-chart :option="categoryOption" autoresize />
        </div>
      </div>
      <div class="panel panel-sm" style="--anim-delay: 0.55s">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-edge-top"></div>
        <div class="panel-edge-bottom"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">SHOP TOP5 // 门店排行</span>
          <span class="panel-tag">LEADER</span>
        </div>
        <div class="panel-body">
          <v-chart :option="shopOption" autoresize />
        </div>
      </div>

      <!-- Row 3: Customer Growth + Gauges -->
      <div class="panel panel-sm" style="--anim-delay: 0.65s">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-edge-top"></div>
        <div class="panel-edge-bottom"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">CUSTOMER // 客户增长</span>
          <span class="panel-tag">TREND</span>
        </div>
        <div class="panel-body">
          <v-chart :option="customerOption" autoresize />
        </div>
      </div>
      <div class="panel panel-lg" style="--anim-delay: 0.75s">
        <div class="panel-corner tl"></div>
        <div class="panel-corner tr"></div>
        <div class="panel-corner bl"></div>
        <div class="panel-corner br"></div>
        <div class="panel-edge-top"></div>
        <div class="panel-edge-bottom"></div>
        <div class="panel-header">
          <span class="panel-dot"></span>
          <span class="panel-title">OPERATION // 运营指标</span>
          <span class="panel-tag">GAUGE</span>
        </div>
        <div class="panel-body gauges">
          <div v-for="(g, i) in gauges" :key="g.title" class="gauge-item" :style="{'--g-delay': i * 0.15 + 's'}">
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
  const s = 1 + Math.random() * 2.5
  return {
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    width: s + 'px', height: s + 'px',
    animationDelay: Math.random() * 8 + 's',
    animationDuration: 5 + Math.random() * 6 + 's',
    opacity: 0.1 + Math.random() * 0.4
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
  { value: 94, max: 100, title: '库存周转', color: ['#00E5FF', '#2979FF'] },
  { value: 87, max: 100, title: '订单完成率', color: ['#00E676', '#00E5FF'] },
  { value: 15680, max: 20000, title: '会员总数', color: ['#D500F9', '#2979FF'] }
])

// ─── Chart Colors ──────────────────────────────────────
const cyan = '#00E5FF'
const green = '#00E676'
const blue = '#2979FF'
const purple = '#D500F9'
const amber = '#FF6D00'
const rose = '#FF1744'

const darkTooltip = {
  backgroundColor: 'rgba(3,7,18,0.92)',
  borderColor: 'rgba(0,229,255,0.2)',
  borderWidth: 1,
  textStyle: { color: '#e2e8f0', fontSize: 12 }
}

const darkAxis = {
  axisLine: { lineStyle: { color: 'rgba(0,229,255,0.08)', type: 'dashed' } },
  axisTick: { show: false },
  axisLabel: { color: 'rgba(255,255,255,0.3)', fontSize: 10 },
  splitLine: { lineStyle: { color: 'rgba(0,229,255,0.04)', type: 'dashed' } }
}

// ─── Chart Data ────────────────────────────────────────
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
    textStyle: { color: 'rgba(255,255,255,0.45)', fontSize: 11, fontFamily: 'Outfit, monospace' },
    top: 0, right: 0,
    itemWidth: 12, itemHeight: 8
  },
  grid: { left: '3%', right: '3%', bottom: '8%', top: '14%', containLabel: true },
  xAxis: { type: 'category', data: months, boundaryGap: false, ...darkAxis },
  yAxis: { type: 'value', ...darkAxis },
  series: [
    {
      name: '订单量', type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
      lineStyle: { width: 2, color: cyan, shadowColor: cyan, shadowBlur: 12, shadowOffsetX: 0, shadowOffsetY: 0 },
      itemStyle: { color: cyan, borderColor: '#fff', borderWidth: 0.5 },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(0,229,255,0.3)' },
            { offset: 0.5, color: 'rgba(0,229,255,0.08)' },
            { offset: 1, color: 'rgba(0,229,255,0)' }
          ]
        }
      },
      data: revenueData.value.orders
    },
    {
      name: '营收(万)', type: 'line', smooth: true, symbol: 'diamond', symbolSize: 5,
      lineStyle: { width: 2, color: green, shadowColor: green, shadowBlur: 12 },
      itemStyle: { color: green, borderColor: '#fff', borderWidth: 0.5 },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(0,230,118,0.25)' },
            { offset: 0.5, color: 'rgba(0,230,118,0.06)' },
            { offset: 1, color: 'rgba(0,230,118,0)' }
          ]
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
    type: 'pie', radius: ['40%', '68%'], center: ['50%', '48%'],
    itemStyle: {
      borderColor: 'rgba(3,7,18,0.9)',
      borderWidth: 2,
      borderRadius: 4,
      shadowColor: 'rgba(0,229,255,0.15)',
      shadowBlur: 10
    },
    label: {
      color: 'rgba(255,255,255,0.5)',
      fontSize: 10,
      fontFamily: 'Rajdhani, monospace',
      formatter: '{b}\n{d}%'
    },
    labelLine: {
      lineStyle: { color: 'rgba(0,229,255,0.15)' }
    },
    emphasis: {
      label: { fontSize: 13, fontWeight: 'bold', color: '#fff' },
      itemStyle: { shadowBlur: 20, shadowColor: 'rgba(0,229,255,0.4)' }
    },
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
      color: {
        type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
        colorStops: [
          { offset: 0, color: 'rgba(0,229,255,0.08)' },
          { offset: 0.5, color: 'rgba(0,229,255,0.3)' },
          { offset: 1, color: cyan }
        ]
      }
    },
    backgroundStyle: { color: 'rgba(0,229,255,0.03)', borderRadius: [0, 3, 3, 0] },
    showBackground: true,
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
      color: {
        type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
        colorStops: [
          { offset: 0, color: 'rgba(0,230,118,0.08)' },
          { offset: 0.5, color: 'rgba(0,230,118,0.3)' },
          { offset: 1, color: green }
        ]
      },
      shadowColor: 'rgba(0,230,118,0.15)',
      shadowBlur: 8
    },
    backgroundStyle: { color: 'rgba(0,230,118,0.03)', borderRadius: [0, 3, 3, 0] },
    showBackground: true,
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
    type: 'bar', barWidth: 14,
    itemStyle: {
      borderRadius: [3, 3, 0, 0],
      color: {
        type: 'linear', x: 0, y: 1, x2: 0, y2: 0,
        colorStops: [
          { offset: 0, color: 'rgba(213,0,249,0.08)' },
          { offset: 0.5, color: 'rgba(213,0,249,0.3)' },
          { offset: 1, color: purple }
        ]
      },
      shadowColor: 'rgba(213,0,249,0.15)',
      shadowBlur: 8
    },
    backgroundStyle: { color: 'rgba(213,0,249,0.02)', borderRadius: [3, 3, 0, 0] },
    showBackground: true,
    data: customerData.value
  }]
}))

// ─── Polling ───────────────────────────────────────────
let pollTimer = null

function jitter(base, pct) {
  return Math.round(base * (1 + (Math.random() - 0.5) * 2 * pct))
}

async function refreshData() {
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
    { value: jitter(94, 0.02), max: 100, title: '库存周转', color: ['#00E5FF', '#2979FF'] },
    { value: jitter(87, 0.03), max: 100, title: '订单完成率', color: ['#00E676', '#00E5FF'] },
    { value: jitter(15680, 0.03), max: 20000, title: '会员总数', color: ['#D500F9', '#2979FF'] }
  ]
}

onMounted(() => { pollTimer = setInterval(refreshData, 30000) })
onUnmounted(() => { clearInterval(pollTimer) })
</script>

<style scoped>
/* ─── Fonts ────────────────────────────── */
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;600;700;800;900&family=Rajdhani:wght@300;400;500;600;700&display=swap');

/* ─── CSS Variables ────────────────────── */
.cockpit {
  --bg: radial-gradient(circle at 50% -20%, #081121 0%, #030712 60%, #010308 100%);
  --panel-bg: rgba(4, 9, 20, 0.4);
  --border-glow: rgba(0, 229, 255, 0.12);
  --cyan: #00E5FF;
  --blue: #2979FF;
  --green: #00E676;
  --purple: #D500F9;
  --amber: #FF6D00;
  position: relative;
  height: calc(100vh - 56px - 38px - 32px);
  background: var(--bg);
  padding: 14px 20px 20px;
  overflow: hidden;
  color: #e2e8f0;
  margin: -20px -24px;
  display: flex;
  flex-direction: column;
  font-family: 'Rajdhani', 'Orbitron', system-ui, sans-serif;
}
.cockpit:fullscreen { height: 100vh; }

/* ─── CRT Noise Overlay ─────────────────── */
.noise-svg { display: none; }
.noise-overlay {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
  opacity: 0.025;
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-repeat: repeat;
  background-size: 256px 256px;
}

/* ─── Grid Background ──────────────────── */
.grid-bg {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(0,229,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,229,255,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
  z-index: 0;
}

/* ─── Scan Line ─────────────────────────── */
.scan-line {
  position: fixed;
  left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, rgba(0,229,255,0.08) 50%, transparent 100%);
  animation: scanDown 8s linear infinite;
  pointer-events: none;
  z-index: 1;
}
@keyframes scanDown {
  0% { top: -1px; }
  100% { top: 100%; }
}

/* ─── Particles ─────────────────────────── */
.particles { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.particle {
  position: absolute;
  background: var(--cyan);
  border-radius: 50%;
  opacity: 0;
  animation: particleFade 8s ease-in-out infinite;
  box-shadow: 0 0 4px var(--cyan);
}
@keyframes particleFade {
  0%, 100% { opacity: 0; transform: translateY(0); }
  50% { opacity: 0.6; transform: translateY(-30px); }
}

/* ─── Header ────────────────────────────── */
.cockpit-header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
  padding: 8px 0 10px;
  clip-path: polygon(20px 0, calc(100% - 20px) 0, 100% 100%, 0 100%);
  background: linear-gradient(180deg, rgba(0,229,255,0.04) 0%, transparent 100%);
  border-bottom: 1px solid rgba(0,229,255,0.1);
}

.fullscreen-btn {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0,229,255,0.06);
  border: 1px solid rgba(0,229,255,0.2);
  border-radius: 4px;
  color: rgba(0,229,255,0.6);
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  padding: 0;
}
.fullscreen-btn:hover {
  background: rgba(0,229,255,0.15);
  border-color: rgba(0,229,255,0.5);
  color: var(--cyan);
  box-shadow: 0 0 16px rgba(0,229,255,0.25);
  transform: translateY(-50%) scale(1.1);
}
.fullscreen-btn svg { width: 15px; height: 15px; }

.header-center { text-align: center; }

/* ─── Glitch Title ──────────────────────── */
.header-glitch-wrapper { position: relative; display: inline-block; }
.header-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 5px;
  background: linear-gradient(90deg, var(--cyan), #60a5fa, var(--cyan));
  background-size: 200% 100%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: titleShimmer 5s ease-in-out infinite;
  margin: 0;
  position: relative;
}
.header-title::before,
.header-title::after {
  content: attr(data-text);
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  opacity: 0;
}
.header-title::before {
  clip-path: inset(0 0 60% 0);
  color: #FF1744;
  -webkit-text-fill-color: #FF1744;
  animation: glitchTop 4s ease-in-out infinite;
}
.header-title::after {
  clip-path: inset(60% 0 0 0);
  color: #2979FF;
  -webkit-text-fill-color: #2979FF;
  animation: glitchBottom 4s ease-in-out infinite;
}
@keyframes glitchTop {
  0%, 90%, 100% { opacity: 0; transform: translateX(0); }
  91.5% { opacity: 0.7; transform: translateX(-2px); }
  92.5% { opacity: 0; transform: translateX(2px); }
}
@keyframes glitchBottom {
  0%, 90%, 100% { opacity: 0; transform: translateX(0); }
  91.5% { opacity: 0.7; transform: translateX(2px); }
  92.5% { opacity: 0; transform: translateX(-2px); }
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
  font-size: 11px;
  color: rgba(255,255,255,0.3);
  margin-top: 4px;
  font-family: 'Rajdhani', monospace;
  letter-spacing: 1.5px;
}
.sub-label { color: rgba(0,229,255,0.5); }
.sub-sep { color: rgba(255,255,255,0.1); }
.sub-coord { color: rgba(255,255,255,0.2); font-size: 10px; }

.pulse-dot {
  width: 5px; height: 5px;
  background: var(--green);
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
  box-shadow: 0 0 6px var(--green);
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.6); }
}
.header-time { font-family: 'Orbitron', monospace; letter-spacing: 1px; }

.header-deco {
  flex: 1;
  display: flex;
  align-items: center;
}
.left-deco { justify-content: flex-end; padding-right: 20px; }
.right-deco { justify-content: flex-start; padding-left: 20px; }
.deco-circuit { display: flex; align-items: center; }

/* ─── HUD Bar ──────────────────────────── */
.hud-bar {
  position: relative;
  z-index: 2;
  display: flex;
  gap: 16px;
  padding: 5px 16px;
  margin-bottom: 8px;
  background: rgba(0,229,255,0.03);
  border: 1px solid rgba(0,229,255,0.06);
  border-left: none;
  border-right: none;
  clip-path: polygon(8px 0, calc(100% - 8px) 0, 100% 100%, 0 100%);
}
.hud-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  font-family: 'Rajdhani', monospace;
  letter-spacing: 0.5px;
}
.hud-label { color: rgba(255,255,255,0.25); }
.hud-value { color: rgba(0,229,255,0.6); font-weight: 500; }
.hud-signal { margin-left: auto; }
.signal-dot {
  width: 4px; height: 4px;
  background: var(--green);
  border-radius: 50%;
  animation: pulse 1s ease-in-out infinite;
  box-shadow: 0 0 4px var(--green);
}

/* ─── KPI Cards ─────────────────────────── */
.kpi-row {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}
.kpi-card {
  position: relative;
  background: var(--panel-bg);
  backdrop-filter: blur(16px) saturate(150%);
  -webkit-backdrop-filter: blur(16px) saturate(150%);
  border: 1px solid var(--border-glow);
  border-radius: 4px;
  padding: 12px 10px 8px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  animation: bootUp 0.6s ease-out both;
  box-shadow: inset 0 0 20px rgba(0,229,255,0.03);
}
.kpi-card:hover {
  border-color: rgba(0,229,255,0.4);
  transform: scale(1.02) translateY(-3px);
  box-shadow: 0 8px 30px rgba(0,229,255,0.1), inset 0 0 30px rgba(0,229,255,0.05);
}
@keyframes bootUp {
  from { transform: translateY(15px) scale(0.97); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}

.kpi-glow-bar {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 1px;
  background: linear-gradient(90deg, transparent, var(--cyan), transparent);
  opacity: 0.5;
  animation: borderScan 3s linear infinite;
}
@keyframes borderScan {
  0% { left: -100%; }
  100% { left: 100%; }
}

.kpi-label {
  font-size: 10px;
  color: rgba(255,255,255,0.35);
  letter-spacing: 1.5px;
  margin-bottom: 4px;
  font-family: 'Rajdhani', sans-serif;
  text-transform: uppercase;
}
.kpi-value {
  font-size: 20px;
  font-weight: 700;
  font-family: 'Orbitron', 'Rajdhani', monospace;
  color: #fff;
  text-shadow: 0 0 10px rgba(0,229,255,0.2);
  line-height: 1.3;
}
.kpi-trend {
  font-size: 10px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 3px;
  font-family: 'Rajdhani', monospace;
}
.kpi-trend.up { color: var(--green); }
.kpi-trend.down { color: var(--cyan); }
.trend-arrow { font-size: 8px; }
.trend-sub { color: rgba(255,255,255,0.2); margin-left: 3px; }

.kpi-metric-tag {
  position: absolute;
  top: 6px;
  right: 6px;
  font-size: 8px;
  color: rgba(0,229,255,0.15);
  font-family: 'Orbitron', monospace;
  letter-spacing: 1px;
}

/* KPI Corners */
.kpi-corner {
  position: absolute;
  width: 8px; height: 8px;
}
.kpi-corner::before, .kpi-corner::after {
  content: '';
  position: absolute;
  background: var(--cyan);
  opacity: 0.3;
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
  gap: 10px;
  flex: 1;
  min-height: 0;
}
.panel {
  position: relative;
  background: var(--panel-bg);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid var(--border-glow);
  border-radius: 4px;
  overflow: hidden;
  transition: border-color 0.4s ease;
  display: flex;
  flex-direction: column;
  clip-path: polygon(8px 0, calc(100% - 8px) 0, 100% 8px, 100% calc(100% - 8px), calc(100% - 8px) 100%, 8px 100%, 0 calc(100% - 8px), 0 8px);
  box-shadow: inset 0 0 20px rgba(0,229,255,0.03);
  animation: panelBoot 0.8s ease-out both;
  animation-delay: var(--anim-delay, 0s);
}
@keyframes panelBoot {
  from { transform: translateY(20px) scale(0.97); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}
.panel:hover {
  border-color: rgba(0,229,255,0.3);
  box-shadow: inset 0 0 30px rgba(0,229,255,0.05);
}

/* Panel edge light runners */
.panel-edge-top, .panel-edge-bottom {
  position: absolute;
  left: 8px; right: 8px;
  height: 1px;
  overflow: hidden;
}
.panel-edge-top { top: -1px; }
.panel-edge-bottom { bottom: -1px; }
.panel-edge-top::before, .panel-edge-bottom::before {
  content: '';
  position: absolute;
  width: 60%;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--cyan), transparent);
  animation: lightRun 3s ease-in-out infinite;
}
.panel-edge-bottom::before {
  animation-delay: 1.5s;
}
@keyframes lightRun {
  0% { left: -60%; }
  100% { left: 100%; }
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px 0;
}
.panel-dot {
  width: 5px; height: 5px;
  background: var(--cyan);
  border-radius: 50%;
  box-shadow: 0 0 6px var(--cyan);
  animation: pulse 2s ease-in-out infinite;
}
.panel-title {
  font-family: 'Rajdhani', 'Orbitron', sans-serif;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255,255,255,0.6);
  letter-spacing: 1.5px;
  text-transform: uppercase;
}
.panel-tag {
  margin-left: auto;
  font-size: 8px;
  font-family: 'Orbitron', monospace;
  color: rgba(0,229,255,0.3);
  border: 1px solid rgba(0,229,255,0.1);
  padding: 0 6px;
  border-radius: 2px;
  letter-spacing: 1px;
}
.panel-body {
  padding: 4px 8px 8px;
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
  width: 14px; height: 14px;
}
.panel-corner::before, .panel-corner::after {
  content: '';
  position: absolute;
  background: var(--cyan);
  opacity: 0.25;
}
.panel-corner.tl { top: -1px; left: -1px; }
.panel-corner.tl::before { top: 0; left: 0; width: 14px; height: 1px; }
.panel-corner.tl::after { top: 0; left: 0; width: 1px; height: 14px; }
.panel-corner.tr { top: -1px; right: -1px; }
.panel-corner.tr::before { top: 0; right: 0; width: 14px; height: 1px; }
.panel-corner.tr::after { top: 0; right: 0; width: 1px; height: 14px; }
.panel-corner.bl { bottom: -1px; left: -1px; }
.panel-corner.bl::before { bottom: 0; left: 0; width: 14px; height: 1px; }
.panel-corner.bl::after { bottom: 0; left: 0; width: 1px; height: 14px; }
.panel-corner.br { bottom: -1px; right: -1px; }
.panel-corner.br::before { bottom: 0; right: 0; width: 14px; height: 1px; }
.panel-corner.br::after { bottom: 0; right: 0; width: 1px; height: 14px; }

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
  animation: gaugeBoot 0.8s ease-out both;
  animation-delay: var(--g-delay, 0s);
}
@keyframes gaugeBoot {
  from { transform: translateY(10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
.gauge-item :deep(.echarts) {
  width: 100% !important;
  height: 100% !important;
}

/* ─── Responsive ────────────────────────── */
@media (max-width: 1200px) {
  .kpi-row { grid-template-columns: repeat(4, 1fr); }
  .charts-grid { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .header-deco { display: none; }
  .header-title { font-size: 18px; letter-spacing: 3px; }
  .hud-bar { flex-wrap: wrap; gap: 8px; }
}
</style>