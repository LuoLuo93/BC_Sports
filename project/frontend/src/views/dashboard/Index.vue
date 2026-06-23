<template>
  <div class="cockpit" ref="cockpitRef">
    <!-- 品牌底纹：等高线 SVG -->
    <div class="topo-bg" aria-hidden="true"></div>

    <!-- 顶部装饰光带 -->
    <div class="aurora-line" aria-hidden="true"></div>

    <!-- ============ Header ============ -->
    <header class="cockpit-header">
      <div class="header-left">
        <div class="brand-mark">
          <svg viewBox="0 0 32 32" width="30" height="30" fill="none">
            <path d="M4 26 L12 10 L18 20 L22 12 L28 26 Z" fill="url(#brandGrad)"/>
            <path d="M12 10 L15 16" stroke="#fff" stroke-width="1" opacity="0.6"/>
            <defs>
              <linearGradient id="brandGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#0ea5e9"/>
                <stop offset="100%" stop-color="#10b981"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div class="header-titles">
          <h1 class="header-title">BC Sport 数据驾驶舱</h1>
          <div class="header-sub">
            <span class="status-dot"></span>
            <span class="sub-text">DATA LIVE</span>
            <span class="sub-sep">·</span>
            <span class="header-time">{{ currentTime }}</span>
          </div>
        </div>
      </div>

      <div class="header-center-deco">
        <svg width="220" height="20" viewBox="0 0 220 20" fill="none">
          <path d="M0 14 L24 14 L34 6 L52 12 L68 4 L88 10 L110 2 L132 8 L152 4 L172 12 L192 6 L210 12 L220 10"
                stroke="url(#decoGrad)" stroke-width="1.1" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          <defs>
            <linearGradient id="decoGrad" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stop-color="#0ea5e9" stop-opacity="0"/>
              <stop offset="50%" stop-color="#0ea5e9" stop-opacity="0.55"/>
              <stop offset="100%" stop-color="#10b981" stop-opacity="0"/>
            </linearGradient>
          </defs>
        </svg>
      </div>

      <div class="header-right">
        <div class="hud-chips">
          <div class="hud-chip"><span class="chip-dot brand"></span>BC SPORT</div>
          <div class="hud-chip"><span class="chip-dot live"></span>数据实时更新</div>
        </div>
        <button class="fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
          <svg v-if="!isFullscreen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M8 3H5a2 2 0 0 0-2 2v3"/><path d="M21 8V5a2 2 0 0 0-2-2h-3"/>
            <path d="M3 16v3a2 2 0 0 0 2 2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-3"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M4 14h6v6"/><path d="M20 10h-6V4"/><path d="M14 10l7-7"/><path d="M3 21l7-7"/>
          </svg>
        </button>
      </div>
    </header>

    <!-- ============ 第一行：深色旗舰 KPI（4 卡） ============ -->
    <section class="kpi-row">
      <div
        v-for="(kpi, idx) in kpis"
        :key="kpi.key"
        class="kpi-card"
        :style="{ animationDelay: 0.07 * idx + 's' }"
      >
        <div class="kpi-glow"></div>
        <div class="kpi-main">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value">
            <span class="kpi-prefix">{{ kpi.prefix }}</span><span class="kpi-num">{{ formatKpi(kpi) }}</span><span class="kpi-suffix" v-if="kpi.suffix">{{ kpi.suffix }}</span>
          </div>
          <div class="kpi-trend" :class="trendClass(kpi.trend)">
            <span class="trend-arrow">{{ kpi.trend > 0 ? '▲' : kpi.trend < 0 ? '▼' : '—' }}</span>
            <span class="trend-val">{{ Math.abs(kpi.trend) }}%</span>
            <span class="trend-sub">{{ kpi.trendLabel }}</span>
          </div>
        </div>
        <div class="kpi-spark">
          <svg viewBox="0 0 100 32" preserveAspectRatio="none" class="spark-svg">
            <defs>
              <linearGradient :id="`sparkFill${idx}`" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" :stop-color="kpi.trend >= 0 ? '#0ea5e9' : '#f59e0b'" stop-opacity="0.45"/>
                <stop offset="100%" :stop-color="kpi.trend >= 0 ? '#0ea5e9' : '#f59e0b'" stop-opacity="0"/>
              </linearGradient>
            </defs>
            <path :d="sparkAreaPath(kpi.spark)" :fill="`url(#sparkFill${idx})`"/>
            <path
              :d="sparkLinePath(kpi.spark)"
              fill="none"
              :stroke="kpi.trend >= 0 ? '#38bdf8' : '#fbbf24'"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="spark-line"
            />
            <circle
              :cx="sparkLastPoint(kpi.spark).x"
              :cy="sparkLastPoint(kpi.spark).y"
              r="2.2"
              :fill="kpi.trend >= 0 ? '#38bdf8' : '#fbbf24'"
              class="spark-dot"
            />
          </svg>
        </div>
      </div>
    </section>

    <!-- ============ 第二行：销售趋势（浅色，2fr） + 目标达成（深色，1fr） ============ -->
    <section class="row-2">
      <div class="panel glass-panel trend-panel" style="--anim-delay: 0.3s">
        <div class="panel-header">
          <span class="panel-indicator"></span>
          <span class="panel-title">销售趋势</span>
          <span class="panel-tag">12 个月</span>
          <div class="panel-legend">
            <span class="leg leg-sky"><i></i>销售额(万)</span>
            <span class="leg leg-emerald"><i></i>订单量</span>
          </div>
        </div>
        <div class="panel-body">
          <v-chart :option="salesTrendOption" autoresize />
        </div>
      </div>

      <div class="panel dark-panel goal-panel" style="--anim-delay: 0.4s">
        <div class="dark-hi-line"></div>
        <div class="panel-header dark-header">
          <span class="panel-indicator light"></span>
          <span class="panel-title light">本月目标达成</span>
          <span class="panel-tag dark-tag">June</span>
        </div>
        <div class="goal-body">
          <div class="goal-ring-wrap">
            <svg viewBox="0 0 160 160" class="goal-ring">
              <defs>
                <linearGradient id="ringGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#0ea5e9"/>
                  <stop offset="100%" stop-color="#10b981"/>
                </linearGradient>
              </defs>
              <circle cx="80" cy="80" r="66" fill="none" stroke="rgba(148,163,184,0.18)" stroke-width="12"/>
              <circle
                cx="80" cy="80" r="66" fill="none"
                stroke="url(#ringGrad)" stroke-width="12"
                stroke-linecap="round"
                :stroke-dasharray="ringCircumference"
                :stroke-dashoffset="ringDashOffset"
                transform="rotate(-90 80 80)"
                class="ring-progress"
              />
            </svg>
            <div class="goal-center">
              <div class="goal-percent">{{ goal.percent }}<span class="goal-pct-sym">%</span></div>
              <div class="goal-label">达成率</div>
            </div>
          </div>
          <div class="goal-meta">
            <div class="goal-meta-item">
              <div class="gm-label">已完成</div>
              <div class="gm-value">¥{{ goal.current }}<span class="gm-unit">万</span></div>
            </div>
            <div class="goal-meta-item">
              <div class="gm-label">目标</div>
              <div class="gm-value light">¥{{ goal.target }}<span class="gm-unit">万</span></div>
            </div>
            <div class="goal-meta-item">
              <div class="gm-label">差额</div>
              <div class="gm-value accent">¥{{ goal.remaining }}<span class="gm-unit">万</span></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 第三行：渠道分布 / 品类排行 / 门店TOP5 / 会员增长 ============ -->
    <section class="row-3">
      <div class="panel glass-panel" style="--anim-delay: 0.45s">
        <div class="panel-header">
          <span class="panel-indicator"></span>
          <span class="panel-title">渠道分布</span>
          <span class="panel-tag">MIX</span>
        </div>
        <div class="panel-body">
          <v-chart :option="channelOption" autoresize />
        </div>
      </div>

      <div class="panel glass-panel" style="--anim-delay: 0.55s">
        <div class="panel-header">
          <span class="panel-indicator"></span>
          <span class="panel-title">品类销售排行</span>
          <span class="panel-tag">TOP</span>
        </div>
        <div class="panel-body">
          <v-chart :option="categoryOption" autoresize />
        </div>
      </div>

      <div class="panel glass-panel" style="--anim-delay: 0.65s">
        <div class="panel-header">
          <span class="panel-indicator"></span>
          <span class="panel-title">门店销售排行</span>
          <span class="panel-tag">TOP 5</span>
        </div>
        <div class="panel-body">
          <v-chart :option="shopOption" autoresize />
        </div>
      </div>

      <div class="panel glass-panel" style="--anim-delay: 0.75s">
        <div class="panel-header">
          <span class="panel-indicator"></span>
          <span class="panel-title">会员增长</span>
          <span class="panel-tag">12 月</span>
        </div>
        <div class="panel-body">
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
  // 全屏切换后 ECharts 需要重新计算尺寸
  setTimeout(() => window.dispatchEvent(new Event('resize')), 120)
}

// ============ Mock 数据 ============
function jitter(base, pct) {
  return Math.round(base * (1 + (Math.random() - 0.5) * 2 * pct))
}

// 基础 sparkline 数据（7 个点）
const baseSpark = (base, variance = 0.18) => {
  const arr = []
  let v = base
  for (let i = 0; i < 7; i++) {
    v = Math.max(base * 0.6, v + (Math.random() - 0.45) * base * variance)
    arr.push(Math.round(v))
  }
  // 保证最后一点相对起点有方向感（多数向上）
  if (arr[6] < arr[0]) arr[6] = Math.round(arr[0] * (1 + Math.random() * 0.12))
  return arr
}

const kpis = reactive([
  {
    key: 'todaySales',
    label: '今日销售额',
    value: 284600,
    prefix: '¥',
    format: 'comma',
    trend: 12.4,
    trendLabel: '同比昨日',
    spark: [212, 228, 235, 251, 268, 276, 295],
  },
  {
    key: 'monthSales',
    label: '本月销售额',
    value: 5680000,
    prefix: '¥',
    format: 'wan',
    trend: 8.2,
    trendLabel: '环比上月',
    spark: [320, 410, 380, 465, 510, 548, 568],
  },
  {
    key: 'todayOrders',
    label: '今日订单',
    value: 1247,
    prefix: '',
    format: 'comma',
    trend: 6.7,
    trendLabel: '同比昨日',
    spark: [880, 920, 1010, 1080, 1120, 1180, 1247],
  },
  {
    key: 'avgPrice',
    label: '客单价',
    value: 228,
    prefix: '¥',
    format: 'comma',
    trend: -1.2,
    trendLabel: '环比昨日',
    spark: [236, 232, 240, 235, 230, 233, 228],
  },
])

const goal = reactive({
  current: 568,
  target: 728,
  percent: 78,
  remaining: 160,
})

const salesData = ref([286, 312, 345, 398, 420, 468, 512, 488, 536, 502, 548, 568])
const orderData = ref([1820, 2100, 2350, 2680, 2810, 3120, 3480, 3260, 3580, 3320, 3680, 3850])
const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

const channelData = ref([
  { name: '线上商城', value: 38 },
  { name: '线下门店', value: 32 },
  { name: '经销商', value: 18 },
  { name: '大客户', value: 8 },
  { name: '企业团购', value: 4 },
])

const categoryData = ref([
  { name: '滑雪装备', value: 42 },
  { name: '户外服饰', value: 28 },
  { name: '徒步装备', value: 18 },
  { name: '运动鞋', value: 8 },
  { name: '配件', value: 4 },
])

const shopData = ref([
  { name: '杭州旗舰', value: 86 },
  { name: '成都太古里', value: 72 },
  { name: '北京三里屯', value: 65 },
  { name: '长春万达', value: 52 },
  { name: '哈尔滨中央大街', value: 46 },
])

const memberData = ref([820, 932, 901, 1034, 1290, 1330, 1520, 1450, 1680, 1590, 1820, 1950])

// ============ KPI 数字滚动动画 ============
const animatedValues = reactive({})

function easeOutCubic(t) {
  return 1 - Math.pow(1 - t, 3)
}

function formatKpi(kpi) {
  const v = animatedValues[kpi.key]
  if (v === undefined) return ''
  if (kpi.format === 'wan') {
    return (v / 10000).toFixed(1)
  }
  return Math.round(v).toLocaleString('en-US')
}

function animateValue(key, to, duration = 1600) {
  const from = animatedValues[key] ?? 0
  const start = performance.now()

  function step(now) {
    const elapsed = now - start
    const t = Math.min(1, elapsed / duration)
    const eased = easeOutCubic(t)
    animatedValues[key] = from + (to - from) * eased
    if (t < 1) {
      requestAnimationFrame(step)
    } else {
      animatedValues[key] = to
    }
  }
  requestAnimationFrame(step)
}

function playKpiAnimations() {
  kpis.forEach((kpi) => animateValue(kpi.key, kpi.value))
}

// ============ Sparkline 路径计算 ============
function sparkLinePath(data) {
  if (!data || data.length === 0) return ''
  const max = Math.max(...data)
  const min = Math.min(...data)
  const range = max - min || 1
  const stepX = 100 / (data.length - 1)
  return data
    .map((v, i) => {
      const x = i * stepX
      const y = 28 - ((v - min) / range) * 24
      return `${i === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`
    })
    .join(' ')
}

function sparkAreaPath(data) {
  const line = sparkLinePath(data)
  if (!line) return ''
  return `${line} L 100 32 L 0 32 Z`
}

function sparkLastPoint(data) {
  if (!data || data.length === 0) return { x: 0, y: 0 }
  const max = Math.max(...data)
  const min = Math.min(...data)
  const range = max - min || 1
  const last = data[data.length - 1]
  return { x: 100, y: 28 - ((last - min) / range) * 24 }
}

function trendClass(trend) {
  if (trend > 0) return 'up'
  if (trend < 0) return 'down'
  return 'flat'
}

// ============ 目标达成环形进度 ============
const ringCircumference = 2 * Math.PI * 66 // ≈ 414.69
const ringDashOffset = computed(() => {
  const p = Math.max(0, Math.min(100, goal.percent))
  return ringCircumference * (1 - p / 100)
})

// ============ ECharts 配置 ============
const palette = {
  sky: '#0ea5e9',
  skyLight: '#38bdf8',
  emerald: '#10b981',
  emeraldLight: '#34d399',
  amber: '#f59e0b',
  violet: '#8b5cf6',
  violetLight: '#a78bfa',
  slate: '#64748b',
  slateLight: '#94a3b8',
}

const tooltipBase = {
  trigger: 'axis',
  backgroundColor: 'rgba(255,255,255,0.92)',
  borderColor: 'rgba(15,23,42,0.08)',
  borderWidth: 1,
  padding: [10, 14],
  textStyle: { color: '#0f172a', fontSize: 12, fontFamily: 'Inter' },
  extraCssText: 'backdrop-filter: blur(14px); border-radius: 10px; box-shadow: 0 8px 24px rgba(15,23,42,0.1);',
}

const salesTrendOption = computed(() => ({
  tooltip: {
    ...tooltipBase,
    axisPointer: { type: 'line', lineStyle: { color: 'rgba(14,165,233,0.3)', type: 'dashed' } },
  },
  legend: { show: false },
  grid: { left: 44, right: 48, top: 18, bottom: 28 },
  xAxis: {
    type: 'category',
    data: months,
    boundaryGap: false,
    axisLine: { lineStyle: { color: 'rgba(148,163,184,0.25)' } },
    axisTick: { show: false },
    axisLabel: { color: palette.slate, fontSize: 11, fontFamily: 'Inter' },
  },
  yAxis: [
    {
      type: 'value',
      name: '万',
      nameTextStyle: { color: palette.slateLight, fontSize: 10, padding: [0, 0, 0, -20] },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.12)', type: 'dashed' } },
      axisLabel: { color: palette.slate, fontSize: 11, fontFamily: 'Inter' },
    },
    {
      type: 'value',
      name: '单',
      nameTextStyle: { color: palette.slateLight, fontSize: 10, padding: [0, -16, 0, 0] },
      splitLine: { show: false },
      axisLabel: { color: palette.slate, fontSize: 11, fontFamily: 'Inter' },
    },
  ],
  series: [
    {
      name: '销售额(万)',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      showSymbol: false,
      data: salesData.value,
      lineStyle: { width: 2.6, color: palette.sky },
      itemStyle: { color: palette.sky, borderColor: '#fff', borderWidth: 2 },
      emphasis: { focus: 'series', scale: 1.4 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(14,165,233,0.28)' },
            { offset: 1, color: 'rgba(14,165,233,0)' },
          ],
        },
      },
    },
    {
      name: '订单量',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      showSymbol: false,
      data: orderData.value,
      lineStyle: { width: 2.2, color: palette.emerald },
      itemStyle: { color: palette.emerald, borderColor: '#fff', borderWidth: 2 },
      emphasis: { focus: 'series', scale: 1.4 },
    },
  ],
}))

const channelOption = computed(() => ({
  tooltip: { ...tooltipBase, trigger: 'item', formatter: '{b}: {c}%' },
  legend: {
    orient: 'vertical',
    right: 6,
    top: 'center',
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 10,
    textStyle: { color: palette.slate, fontSize: 11, fontFamily: 'Inter' },
  },
  series: [
    {
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['38%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 6,
        label: {
          show: true,
          position: 'center',
          formatter: '{b}\n{c}%',
          color: '#0f172a',
          fontSize: 13,
          fontWeight: 600,
          fontFamily: 'Inter',
        },
      },
      data: channelData.value.map((d, i) => ({
        ...d,
        itemStyle: {
          color: [palette.sky, palette.emerald, palette.amber, palette.violet, palette.slateLight][i],
        },
      })),
    },
  ],
}))

const categoryOption = computed(() => {
  const max = Math.max(...categoryData.value.map((d) => d.value))
  return {
    tooltip: { ...tooltipBase, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c}%' },
    grid: { left: 8, right: 36, top: 10, bottom: 8, containLabel: true },
    xAxis: { type: 'value', max, show: false },
    yAxis: {
      type: 'category',
      data: categoryData.value.map((d) => d.name),
      inverse: true,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: palette.slate, fontSize: 11.5, fontFamily: 'Inter', margin: 12 },
    },
    series: [
      {
        type: 'bar',
        barWidth: '54%',
        data: categoryData.value.map((d) => ({
          value: d.value,
          itemStyle: {
            borderRadius: [0, 7, 7, 0],
            color: {
              type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
              colorStops: [
                { offset: 0, color: 'rgba(14,165,233,0.55)' },
                { offset: 1, color: palette.sky },
              ],
            },
          },
        })),
        label: {
          show: true,
          position: 'right',
          formatter: '{c}%',
          color: palette.sky,
          fontSize: 11.5,
          fontWeight: 600,
          fontFamily: 'Inter',
        },
      },
    ],
  }
})

const shopOption = computed(() => {
  const max = Math.max(...shopData.value.map((d) => d.value))
  return {
    tooltip: { ...tooltipBase, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: ¥{c}万' },
    grid: { left: 8, right: 40, top: 10, bottom: 8, containLabel: true },
    xAxis: { type: 'value', max, show: false },
    yAxis: {
      type: 'category',
      data: shopData.value.map((d) => d.name),
      inverse: true,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: palette.slate, fontSize: 11.5, fontFamily: 'Inter', margin: 12 },
    },
    series: [
      {
        type: 'bar',
        barWidth: '54%',
        data: shopData.value.map((d) => ({
          value: d.value,
          itemStyle: {
            borderRadius: [0, 7, 7, 0],
            color: {
              type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
              colorStops: [
                { offset: 0, color: 'rgba(16,185,129,0.55)' },
                { offset: 1, color: palette.emerald },
              ],
            },
          },
        })),
        label: {
          show: true,
          position: 'right',
          formatter: '¥{c}万',
          color: palette.emerald,
          fontSize: 11.5,
          fontWeight: 600,
          fontFamily: 'Inter',
        },
        showBackground: true,
        backgroundStyle: { color: 'rgba(148,163,184,0.08)', borderRadius: [0, 7, 7, 0] },
      },
    ],
  }
})

const memberOption = computed(() => ({
  tooltip: {
    ...tooltipBase,
    trigger: 'axis',
    formatter: (params) => `${params[0].name}<br/>新增会员 <b>${params[0].value}</b> 人`,
  },
  grid: { left: 8, right: 12, top: 18, bottom: 28, containLabel: true },
  xAxis: {
    type: 'category',
    data: months,
    axisLine: { lineStyle: { color: 'rgba(148,163,184,0.25)' } },
    axisTick: { show: false },
    axisLabel: { color: palette.slate, fontSize: 10.5, fontFamily: 'Inter', interval: 1 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(148,163,184,0.12)', type: 'dashed' } },
    axisLabel: { color: palette.slate, fontSize: 11, fontFamily: 'Inter' },
  },
  series: [
    {
      type: 'bar',
      barWidth: '48%',
      data: memberData.value.map((v) => ({
        value: v,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: palette.violetLight },
              { offset: 1, color: 'rgba(139,92,246,0.35)' },
            ],
          },
        },
      })),
      emphasis: {
        itemStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: palette.violet },
              { offset: 1, color: 'rgba(139,92,246,0.5)' },
            ],
          },
        },
      },
    },
  ],
}))

// ============ 数据轮询（30s 微调） ============
let pollTimer = null
function refreshData() {
  // KPI 微调
  kpis[0].value = jitter(kpis[0].value, 0.04)
  kpis[1].value = jitter(kpis[1].value, 0.03)
  kpis[2].value = jitter(kpis[2].value, 0.05)
  kpis[3].value = jitter(kpis[3].value, 0.02)
  kpis.forEach((k) => {
    k.spark = baseSpark(k.value > 100000 ? k.value / 1000 : k.value * 10)
  })

  // 目标微调
  goal.current = jitter(goal.current, 0.02)
  goal.percent = Math.min(99, Math.round((goal.current / goal.target) * 100))
  goal.remaining = Math.max(0, goal.target - goal.current)

  // 趋势微调最后一个点
  salesData.value = salesData.value.map((v, i) =>
    i === salesData.value.length - 1 ? jitter(v, 0.04) : v
  )
  orderData.value = orderData.value.map((v, i) =>
    i === orderData.value.length - 1 ? jitter(v, 0.04) : v
  )
  memberData.value = memberData.value.map((v, i) =>
    i === memberData.value.length - 1 ? jitter(v, 0.04) : v
  )

  playKpiAnimations()
}

// ============ 生命周期 ============
onMounted(() => {
  updateTime()
  clockTimer = setInterval(updateTime, 1000)
  pollTimer = setInterval(refreshData, 30000)
  document.addEventListener('fullscreenchange', onFullscreenChange)

  // 首次入场动画
  nextTick(() => {
    playKpiAnimations()
    // 修复 ECharts 初始尺寸
    setTimeout(() => window.dispatchEvent(new Event('resize')), 120)
  })
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (pollTimer) clearInterval(pollTimer)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700&display=swap');

/* ============ 设计 Token ============ */
.cockpit {
  /* 配色 */
  --sky: #0ea5e9;
  --sky-light: #38bdf8;
  --sky-soft: rgba(14, 165, 233, 0.12);
  --emerald: #10b981;
  --emerald-light: #34d399;
  --amber: #f59e0b;
  --violet: #8b5cf6;
  --violet-light: #a78bfa;

  /* 文字 */
  --text-1: #0f172a;
  --text-2: #475569;
  --text-3: #64748b;
  --text-4: #94a3b8;
  --text-inv: #f8fafc;
  --text-inv-2: #cbd5e1;
  --text-inv-3: #94a3b8;

  /* 面板 */
  --glass-bg: rgba(255, 255, 255, 0.72);
  --glass-border: rgba(255, 255, 255, 0.9);
  --glass-blur: 20px;
  --dark-bg: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  --dark-border: rgba(148, 163, 184, 0.15);

  /* 阴影 */
  --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.04);
  --shadow-md: 0 4px 12px rgba(15, 23, 42, 0.05);
  --shadow-lg: 0 8px 24px rgba(15, 23, 42, 0.06);
  --shadow-xl: 0 12px 40px rgba(15, 23, 42, 0.1);
  --shadow-dark: 0 12px 32px rgba(15, 23, 42, 0.25);

  /* 几何 */
  --radius-lg: 16px;
  --radius-md: 12px;
  --radius-sm: 8px;

  position: relative;
  width: 100%;
  height: calc(100vh - 56px - 38px - 24px);
  min-height: 680px;
  padding: 18px 22px 16px;
  margin: -20px -24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
  background: radial-gradient(ellipse at top, #f8fafc 0%, #e2e8f0 100%);
  box-sizing: border-box;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  color: var(--text-1);
}

.cockpit:fullscreen {
  height: 100vh;
  padding: 22px 28px 20px;
  margin: 0;
  border-radius: 0;
}

/* ============ 品牌底纹：等高线 ============ */
.topo-bg {
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='600' height='600' viewBox='0 0 600 600'><g fill='none' stroke='%230ea5e9' stroke-width='1' opacity='0.5'><path d='M50 500 Q150 420 250 460 T450 440 T580 480'/><path d='M80 420 Q180 350 280 390 T480 360 T590 400'/><path d='M120 340 Q220 280 320 310 T500 290 T600 320'/><path d='M160 260 Q260 210 360 240 T520 220 T610 250'/><path d='M200 180 Q300 140 400 170 T540 150 T620 180'/><path d='M250 110 Q340 80 430 105 T560 90 T640 115'/></g></svg>");
  background-size: 600px 600px;
  opacity: 0.06;
  pointer-events: none;
  z-index: 0;
}

.aurora-line {
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(14, 165, 233, 0.5), rgba(16, 185, 129, 0.5), transparent);
  opacity: 0.5;
  z-index: 1;
  pointer-events: none;
}

/* ============ Header ============ */
.cockpit-header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.12), rgba(16, 185, 129, 0.12));
  border: 1px solid rgba(14, 165, 233, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-sm);
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.header-title {
  margin: 0;
  font-family: 'Inter', sans-serif;
  font-size: 19px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: -0.01em;
  line-height: 1.2;
}

.header-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'Inter', sans-serif;
  font-size: 11.5px;
  color: var(--text-3);
  font-weight: 500;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--emerald);
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.5);
  animation: statusPulse 2s ease-in-out infinite;
}

@keyframes statusPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.5); }
  50% { box-shadow: 0 0 0 5px rgba(16, 185, 129, 0); }
}

.sub-text {
  letter-spacing: 0.08em;
  font-weight: 600;
  color: var(--emerald);
}

.sub-sep {
  color: var(--text-4);
}

.header-time {
  font-variant-numeric: tabular-nums;
  color: var(--text-2);
}

.header-center-deco {
  flex: 1;
  display: flex;
  justify-content: center;
  opacity: 0.7;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hud-chips {
  display: flex;
  gap: 8px;
}

.hud-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-2);
  letter-spacing: 0.04em;
  box-shadow: var(--shadow-sm);
}

.chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.chip-dot.brand { background: var(--sky); }
.chip-dot.live {
  background: var(--emerald);
  animation: statusPulse 2s ease-in-out infinite;
}

.fullscreen-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--glass-border);
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  color: var(--text-2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: var(--shadow-sm);
}

.fullscreen-btn:hover {
  color: var(--sky);
  border-color: rgba(14, 165, 233, 0.3);
  transform: translateY(-1px);
}

.fullscreen-btn:active {
  transform: scale(0.96);
}

.fullscreen-btn svg {
  width: 17px;
  height: 17px;
}

/* ============ 第一行：深色旗舰 KPI ============ */
.kpi-row {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  flex-shrink: 0;
}

.kpi-card {
  position: relative;
  background: var(--dark-bg);
  border-radius: var(--radius-lg);
  padding: 18px 20px 14px;
  overflow: hidden;
  box-shadow: var(--shadow-dark);
  border: 1px solid var(--dark-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  animation: fadeUp 0.6s cubic-bezier(0.22, 1, 0.36, 1) backwards;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.kpi-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.32);
}

.kpi-glow {
  position: absolute;
  top: -50%;
  right: -20%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.22), transparent 65%);
  pointer-events: none;
  animation: glowBreathe 5s ease-in-out infinite;
}

@keyframes glowBreathe {
  0%, 100% { opacity: 0.7; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.08); }
}

.kpi-main {
  position: relative;
  z-index: 1;
  flex: 1;
  min-width: 0;
}

.kpi-label {
  font-family: 'Inter', sans-serif;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--text-inv-3);
  letter-spacing: 0.04em;
  margin-bottom: 8px;
}

.kpi-value {
  font-family: 'Outfit', 'Inter', sans-serif;
  font-size: 30px;
  font-weight: 700;
  color: var(--text-inv);
  line-height: 1.1;
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.kpi-prefix {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-inv-2);
  margin-right: 1px;
}

.kpi-suffix {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-inv-2);
  margin-left: 2px;
}

.kpi-trend {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 999px;
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  font-weight: 600;
}

.kpi-trend.up {
  color: var(--emerald-light);
  background: rgba(16, 185, 129, 0.14);
}

.kpi-trend.down {
  color: #fca5a5;
  background: rgba(244, 63, 94, 0.14);
}

.kpi-trend.flat {
  color: var(--text-inv-3);
  background: rgba(148, 163, 184, 0.14);
}

.trend-arrow {
  font-size: 9px;
}

.trend-sub {
  font-weight: 500;
  opacity: 0.8;
  margin-left: 2px;
}

/* Sparkline */
.kpi-spark {
  position: relative;
  z-index: 1;
  width: 92px;
  height: 38px;
  flex-shrink: 0;
}

.spark-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.spark-line {
  stroke-dasharray: 300;
  stroke-dashoffset: 300;
  animation: sparkDraw 1.4s cubic-bezier(0.65, 0, 0.35, 1) 0.5s forwards;
}

@keyframes sparkDraw {
  to { stroke-dashoffset: 0; }
}

.spark-dot {
  opacity: 0;
  animation: sparkDotIn 0.4s ease 1.7s forwards;
  transform-origin: center;
  transform-box: fill-box;
}

@keyframes sparkDotIn {
  from { opacity: 0; transform: scale(0); }
  to { opacity: 1; transform: scale(1); }
}

/* ============ 通用面板 ============ */
.panel {
  animation: fadeUp 0.7s cubic-bezier(0.22, 1, 0.36, 1) backwards;
  animation-delay: var(--anim-delay, 0s);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.panel:hover {
  transform: translateY(-2px);
}

.glass-panel {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.dark-panel {
  background: var(--dark-bg);
  border: 1px solid var(--dark-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-dark);
  position: relative;
  overflow: hidden;
}

.dark-hi-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--sky), var(--emerald), transparent);
  opacity: 0.7;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px 0;
  flex-shrink: 0;
}

.dark-header {
  padding-top: 16px;
}

.panel-indicator {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--sky), var(--emerald));
}

.panel-indicator.light {
  background: linear-gradient(180deg, var(--sky-light), var(--emerald-light));
}

.panel-title {
  font-family: 'Inter', sans-serif;
  font-size: 14.5px;
  font-weight: 600;
  color: var(--text-1);
  letter-spacing: 0.01em;
}

.panel-title.light {
  color: var(--text-inv);
}

.panel-tag {
  font-family: 'Inter', sans-serif;
  font-size: 10px;
  font-weight: 600;
  color: var(--text-3);
  background: rgba(148, 163, 184, 0.12);
  padding: 2px 8px;
  border-radius: 6px;
  letter-spacing: 0.04em;
}

.dark-tag {
  color: var(--text-inv-3);
  background: rgba(148, 163, 184, 0.15);
}

.panel-legend {
  margin-left: auto;
  display: flex;
  gap: 12px;
}

.leg {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: var(--text-3);
}

.leg i {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.leg-sky i { background: var(--sky); }
.leg-emerald i { background: var(--emerald); }

.panel-body {
  flex: 1;
  min-height: 0;
  padding: 6px 8px 8px;
}

.panel-body :deep(.echarts) {
  width: 100% !important;
  height: 100% !important;
}

/* ============ 第二行 ============ */
.row-2 {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 14px;
  flex: 1;
  min-height: 0;
}

.trend-panel .panel-body {
  padding: 8px 12px 12px;
}

/* 目标达成卡 */
.goal-panel {
  display: flex;
  flex-direction: column;
}

.goal-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 18px 18px;
  gap: 16px;
}

.goal-ring-wrap {
  position: relative;
  width: 150px;
  height: 150px;
}

.goal-ring {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 4px 16px rgba(14, 165, 233, 0.25));
}

.ring-progress {
  transition: stroke-dashoffset 1.8s cubic-bezier(0.22, 1, 0.36, 1);
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
  font-family: 'Outfit', 'Inter', sans-serif;
  font-size: 38px;
  font-weight: 700;
  color: var(--text-inv);
  line-height: 1;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.goal-pct-sym {
  font-size: 20px;
  color: var(--text-inv-2);
  margin-left: 1px;
}

.goal-label {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: var(--text-inv-3);
  margin-top: 4px;
  letter-spacing: 0.06em;
}

.goal-meta {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.goal-meta-item {
  text-align: center;
}

.gm-label {
  font-family: 'Inter', sans-serif;
  font-size: 10.5px;
  color: var(--text-inv-3);
  letter-spacing: 0.04em;
  margin-bottom: 4px;
}

.gm-value {
  font-family: 'Outfit', 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-inv);
  font-variant-numeric: tabular-nums;
}

.gm-value.light { color: var(--text-inv-2); }
.gm-value.accent { color: var(--sky-light); }

.gm-unit {
  font-size: 10px;
  font-weight: 500;
  color: var(--text-inv-3);
  margin-left: 1px;
}

/* ============ 第三行 ============ */
.row-3 {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1fr 1.1fr 1.1fr 1.2fr;
  gap: 14px;
  flex: 1;
  min-height: 0;
}

/* ============ 入场动画 ============ */
@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ============ 响应式 ============ */
@media (max-width: 1400px) {
  .row-3 {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: 1fr 1fr;
  }
  .kpi-value { font-size: 27px; }
}

@media (max-width: 1200px) {
  .cockpit {
    height: auto;
    min-height: 0;
    overflow-y: auto;
  }
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .row-2 {
    grid-template-columns: 1fr;
    flex: none;
  }
  .trend-panel { min-height: 320px; }
  .goal-panel { min-height: 280px; }
  .row-3 {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: auto auto;
    flex: none;
  }
  .row-3 > .panel { min-height: 280px; }
  .header-center-deco { display: none; }
}

@media (max-width: 768px) {
  .cockpit { padding: 14px 12px; }
  .kpi-row { grid-template-columns: 1fr; }
  .row-3 { grid-template-columns: 1fr; }
  .header-title { font-size: 16px; }
  .hud-chips { display: none; }
  .kpi-value { font-size: 24px; }
  .header-left { gap: 8px; }
  .brand-mark { width: 36px; height: 36px; }
}
</style>
