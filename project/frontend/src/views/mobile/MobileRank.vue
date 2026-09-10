<template>
  <div class="m-page">
    <!-- 顶部渐变区:标题 + 汇总指标 -->
    <header class="m-hero">
      <div class="m-hero-top">
        <div>
          <h1 class="m-hero-title">运动积分排名</h1>
          <p class="m-hero-sub">
            <span class="m-live-dot"></span>
            {{ summary.date }} · 第{{ summary.weekNo }}周
          </p>
        </div>
      </div>

      <div class="m-stats" v-if="summary.participants">
        <div class="m-stat">
          <b>{{ fmtNum(summary.participants) }}</b>
          <span>参与人数</span>
        </div>
        <i class="m-stat-divider"></i>
        <div class="m-stat">
          <b>{{ fmtNum(summary.totalPoints) }}</b>
          <span>累计积分</span>
        </div>
        <i class="m-stat-divider"></i>
        <div class="m-stat">
          <b>{{ fmtNum(summary.avgPoints) }}</b>
          <span>人均积分</span>
        </div>
      </div>
    </header>

    <!-- 前三名领奖台 -->
    <section class="m-podium" v-if="top3.length === 3">
      <div
        class="m-pod"
        v-for="p in podiumOrder"
        :key="p.item.id"
        :class="'m-pod-' + p.place"
      >
        <span class="m-crown" v-if="p.place === 1">👑</span>
        <div class="m-pod-avatar" :class="'is-' + p.place">
          {{ p.item.name.slice(-1) }}
          <i class="m-medal" :class="'is-' + p.place">{{ p.place }}</i>
        </div>
        <b class="m-pod-name">{{ p.item.name }}</b>
        <b class="m-pod-points" :class="'is-' + p.place">{{ fmtNum(p.item.points) }}</b>
      </div>
    </section>

    <!-- 吸顶工具条:搜索 -->
    <div class="m-toolbar">
      <van-search
        v-model="keyword"
        class="m-search"
        placeholder="搜索姓名"
        shape="round"
        @search="onSearchNow"
        @clear="onSearchNow"
      />
    </div>

    <!-- 第 4 名起的列表:下拉刷新 + 触底自动加载 -->
    <van-pull-refresh
      v-model="refreshing"
      class="m-refresh"
      success-text="刷新成功"
      @refresh="onRefresh"
    >
      <div class="m-list-card">
        <!-- 首屏骨架 -->
        <template v-if="firstLoading">
          <div class="m-row" v-for="n in 6" :key="n">
            <van-skeleton avatar avatar-shape="round" :row="1" :loading="true">
              <span></span>
            </van-skeleton>
          </div>
        </template>

        <van-list
          v-else
          v-model:loading="loading"
          :finished="finished"
          finished-text="— 没有更多了 —"
          @load="onLoad"
        >
          <div class="m-row" v-for="item in list" :key="item.id">
            <span class="m-rank-num">{{ item.rank }}</span>
            <span class="m-row-avatar" :style="{ background: avatarBg(item.name) }">
              {{ item.name.slice(-1) }}
            </span>
            <div class="m-row-info">
              <b>{{ item.name }}</b>
            </div>
            <div class="m-row-right">
              <b>{{ fmtNum(item.points) }}</b>
            </div>
          </div>

          <van-empty
            v-if="finished && !list.length"
            image="search"
            description="没有找到相关成员"
          />
        </van-list>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { fetchRankList, fetchRankSummary } from '@/api/mobile'

const PAGE_SIZE = 10

const keyword = ref('')
const list = ref([])
const top3 = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const firstLoading = ref(true)
const summary = ref({ participants: 0, totalPoints: 0, avgPoints: 0, top3: [], date: '', weekNo: '' })

// van-list 触底自动 onLoad 时自增;下拉刷新时归零
let page = 0
let searchTimer = null

// 领奖台展示顺序:亚军 / 冠军 / 季军
const podiumOrder = computed(() => {
  const [first, second, third] = top3.value
  return [
    { place: 2, item: second },
    { place: 1, item: first },
    { place: 3, item: third }
  ].filter(p => p.item)
})

function fmtNum(n) {
  return (n || 0).toLocaleString('zh-CN')
}

// 头像底色按姓名字符散列,同一人永远同色
function avatarBg(name) {
  const grads = [
    'linear-gradient(135deg,#3b82f6,#6366f1)',
    'linear-gradient(135deg,#10b981,#059669)',
    'linear-gradient(135deg,#f59e0b,#f97316)',
    'linear-gradient(135deg,#8b5cf6,#6366f1)',
    'linear-gradient(135deg,#ec4899,#f43f5e)',
    'linear-gradient(135deg,#06b6d4,#3b82f6)'
  ]
  let h = 0
  for (const ch of name) h = (h * 31 + ch.charCodeAt(0)) % 997
  return grads[h % grads.length]
}

async function loadPage(targetPage) {
  const res = await fetchRankList({
    page: targetPage,
    pageSize: PAGE_SIZE,
    keyword: keyword.value.trim()
  })
  if (res.code !== 200) return
  if (targetPage === 1) {
    list.value = res.data.list
  } else {
    list.value.push(...res.data.list)
  }
  finished.value = list.value.length >= res.data.total
}

// van-list 触底自动触发(组件要求 loading 已由 v-model 置 true)
async function onLoad() {
  page += 1
  try {
    await loadPage(page)
  } finally {
    loading.value = false
    refreshing.value = false
    firstLoading.value = false
  }
}

// 下拉刷新 / 切换榜单 / 搜索:回到第一页并同步头部与领奖台
async function onRefresh() {
  page = 0
  finished.value = false
  loading.value = true
  loadSummary()
  await onLoad()
}

function onSearchNow() {
  clearTimeout(searchTimer)
  onRefresh()
}

async function loadSummary() {
  const res = await fetchRankSummary({
    keyword: keyword.value.trim()
  })
  if (res.code === 200) {
    summary.value = res.data
    top3.value = res.data.top3 || []
  }
}

// 关键词输入防抖后自动搜索
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(onRefresh, 400)
})

onMounted(() => {
  loadSummary()
  onRefresh()
})
</script>

<style scoped>
.m-page {
  min-height: 100vh;
  background: #f4f6fb;
  padding-bottom: calc(24px + env(safe-area-inset-bottom));
  -webkit-tap-highlight-color: transparent;
}

/* ==================== 顶部渐变区 ==================== */
.m-hero {
  padding: calc(20px + env(safe-area-inset-top)) 20px 52px;
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 55%, #6366f1 100%);
  border-radius: 0 0 28px 28px;
  color: #fff;
}

.m-hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.m-hero-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.m-hero-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 12px;
  opacity: 0.78;
}

.m-live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.8);
  animation: m-blink 2.4s ease-in-out infinite;
}

@keyframes m-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

.m-stats {
  display: flex;
  align-items: center;
  padding: 16px 4px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.m-stat {
  flex: 1;
  text-align: center;
  min-width: 0;
}

.m-stat b {
  display: block;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.m-stat span {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  opacity: 0.72;
}

.m-stat-divider {
  width: 1px;
  height: 30px;
  background: rgba(255, 255, 255, 0.22);
  flex-shrink: 0;
}

/* ==================== 领奖台 ==================== */
.m-podium {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 10px;
  margin: -40px 16px 0;
  padding: 22px 8px 18px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 1px 2px rgba(28, 25, 23, 0.04), 0 8px 24px rgba(28, 25, 23, 0.06);
  position: relative;
  z-index: 5;
}

.m-pod {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

/* 冠军列整体抬高 */
.m-pod-1 {
  order: 2;
}
.m-pod-2 {
  order: 1;
}
.m-pod-3 {
  order: 3;
}

.m-pod-avatar {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #94a3b8, #64748b);
  border: 3px solid #e2e8f0;
  box-shadow: 0 4px 12px rgba(28, 25, 23, 0.12);
}

.m-pod-1 .m-pod-avatar {
  width: 64px;
  height: 64px;
  font-size: 24px;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  border-color: #fde68a;
}

.m-pod-2 .m-pod-avatar {
  background: linear-gradient(135deg, #94a3b8, #b8c2cf);
  border-color: #e2e8f0;
}

.m-pod-3 .m-pod-avatar {
  background: linear-gradient(135deg, #b45309, #d97706);
  border-color: #fde8d0;
}

.m-crown {
  position: absolute;
  top: -26px;
  font-size: 22px;
  filter: drop-shadow(0 2px 4px rgba(245, 158, 11, 0.4));
  animation: m-crown-bob 2.6s ease-in-out infinite;
}

.m-pod-1 {
  position: relative;
}

@keyframes m-crown-bob {
  0%, 100% { transform: translateY(0) rotate(-6deg); }
  50% { transform: translateY(-3px) rotate(6deg); }
}

.m-medal {
  position: absolute;
  right: -4px;
  bottom: -2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
  color: #fff;
  border: 2px solid #fff;
}

.m-medal.is-1 { background: #f59e0b; }
.m-medal.is-2 { background: #94a3b8; }
.m-medal.is-3 { background: #b45309; }

.m-pod-name {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--bc-text);
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.m-pod-1 .m-pod-name {
  font-size: 15px;
}

.m-pod-points {
  margin-top: 6px;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 800;
  color: #1d4ed8;
  background: #eff6ff;
}

.m-pod-1 .m-pod-points {
  color: #b45309;
  background: #fffbeb;
}

/* ==================== 吸顶工具条 ==================== */
.m-toolbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: #f4f6fb;
  box-shadow: 0 6px 16px -12px rgba(29, 78, 216, 0.25);
}

.m-search {
  background: transparent;
  padding: 10px 12px 2px;
}

.m-search :deep(.van-search__content) {
  background: #fff;
  box-shadow: var(--bc-shadow-sm);
}

/* ==================== 排名列表 ==================== */
.m-refresh {
  min-height: 40vh;
}

.m-refresh :deep(.van-list) {
  padding: 12px 12px 0;
}

.m-list-card {
  padding: 4px 12px 0;
}

.m-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  padding: 12px 14px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 2px rgba(28, 25, 23, 0.04), 0 6px 18px rgba(28, 25, 23, 0.04);
}

.m-rank-num {
  flex-shrink: 0;
  width: 26px;
  text-align: center;
  font-size: 15px;
  font-weight: 800;
  font-style: italic;
  color: #a8a29e;
}

.m-row-avatar {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 800;
  color: #fff;
}

.m-row-info {
  flex: 1;
  min-width: 0;
}

.m-row-info b {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: var(--bc-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.m-row-right {
  flex-shrink: 0;
  text-align: right;
}

.m-row-right b {
  display: block;
  font-size: 16px;
  font-weight: 800;
  color: #1d4ed8;
  font-variant-numeric: tabular-nums;
}
</style>
