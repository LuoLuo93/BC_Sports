<template>
  <div class="m-page">
    <!-- 顶部区:默认蓝色渐变;管理员设置头图后换背景图(叠暗色渐变保证白字可读) + 汇总指标 -->
    <header class="m-hero" :class="{ 'm-hero--img': summary.heroUrl }" :style="heroStyle">
      <div class="m-hero-top">
        <div>
          <h1 class="m-hero-title">徒步值排名</h1>
          <p class="m-hero-sub">
            <span class="m-live-dot"></span>
            {{ summary.date }} · 第{{ summary.weekNo }}周
          </p>
        </div>
      </div>

      <div class="m-stats" v-if="summary.participants">
        <div class="m-stat">
          <i class="m-stat-ico">🥾</i>
          <b>{{ fmtNum(summary.participants) }}</b>
          <span>参与人数</span>
        </div>
        <i class="m-stat-divider"></i>
        <div class="m-stat">
          <i class="m-stat-ico">👣</i>
          <b>{{ fmtNum(Math.round(summary.totalPoints || 0)) }}</b>
          <span>累计积分</span>
        </div>
        <i class="m-stat-divider"></i>
        <div class="m-stat">
          <i class="m-stat-ico">🧭</i>
          <b>{{ fmtNum(summary.avgPoints) }}</b>
          <span>人均积分</span>
        </div>
      </div>
    </header>

    <!-- 前三名领奖台:永远显示全榜前三(与搜索无关);全表人数不足3人时不渲染 -->
    <section class="m-podium" v-if="top3.length === 3">
      <div
        class="m-pod"
        v-for="p in podiumOrder"
        :key="p.item.id"
        :class="'m-pod-' + p.place"
      >
        <span class="m-crown" v-if="p.place === 1">👑</span>
        <div class="m-pod-avatar" :class="'is-' + p.place">
          <img
            v-if="p.item.avatarUrl && !imgFailed.has(p.item.id)"
            :src="avatarImg(p.item.avatarUrl)"
            alt=""
            @error="markImgFailed(p.item.id)"
          />
          <template v-else>{{ avatarOf(p.item.name).emoji }}</template>
          <i class="m-medal" :class="'is-' + p.place">{{ ['🥇', '🥈', '🥉'][p.place - 1] }}</i>
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
        placeholder="搜索队员"
        shape="round"
        @search="onSearchNow"
        @clear="onSearchNow"
      />
    </div>

    <!-- 第 4 名起的列表:下拉刷新 + 触底自动加载 -->
    <van-pull-refresh
      v-model="refreshing"
      class="m-refresh"
      success-text="补给完毕，继续出发"
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
          finished-text="— 已到达本段终点 🏕️ —"
          @load="onLoad"
        >
          <div class="m-row" v-for="item in list" :key="item.id">
            <span class="m-rank-num" :class="{ 'is-top': item.rank >= 4 && item.rank <= 10 }">{{ item.rank }}</span>
            <span class="m-row-avatar" :style="{ background: avatarOf(item.name).bg }">
              <img
                v-if="item.avatarUrl && !imgFailed.has(item.id)"
                :src="avatarImg(item.avatarUrl)"
                alt=""
                @error="markImgFailed(item.id)"
              />
              <template v-else>{{ avatarOf(item.name).emoji }}</template>
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
            description="没有找到这位队员"
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

// 头像:名字实际多为手机号,显示数字难看,按姓名散列出稳定的"随机"卡通头像
// (确定性散列:同一人每次刷新、每处出现都是同一头像,不是真随机)
const AVATAR_GRADS = [
  'linear-gradient(135deg,#3b82f6,#6366f1)',
  'linear-gradient(135deg,#10b981,#059669)',
  'linear-gradient(135deg,#f59e0b,#f97316)',
  'linear-gradient(135deg,#8b5cf6,#6366f1)',
  'linear-gradient(135deg,#ec4899,#f43f5e)',
  'linear-gradient(135deg,#06b6d4,#3b82f6)'
]
const AVATAR_EMOJIS = [
  '🐯', '🦁', '🐻', '🐨', '🦊', '🐸', '🐵', '🐰', '🦄', '🐙', '🦉', '🐳',
  '🐢', '🦋', '🐝', '🦖', '🐧', '🦌', '🐼', '🐮', '🦅', '🐴', '🦦', '🐷'
]

function avatarOf(name) {
  let h = 0
  for (const ch of String(name ?? '')) h = (h * 33 + ch.charCodeAt(0)) % 100003
  return {
    bg: AVATAR_GRADS[h % AVATAR_GRADS.length],
    emoji: AVATAR_EMOJIS[Math.floor(h / AVATAR_GRADS.length) % AVATAR_EMOJIS.length]
  }
}

// 自定义头像（管理员代传）：优先显示图片，无头像或加载失败(文件被清理)回退动物emoji。
// 换引用触发重渲染；后端文件名带时间戳，换头像天然破缓存
const imgFailed = ref(new Set())
function markImgFailed(id) { imgFailed.value = new Set(imgFailed.value).add(id) }

// 后端存应用内路径 /images/avatar/xxx，页面部署在 /bcsports 下需补 context-path（与后台 Logo 同一拼法）
function avatarImg(url) {
  if (url.startsWith('http') || url.startsWith('data:')) return url
  return url.startsWith('/bcsports') ? url : '/bcsports' + url
}

// 顶部头图：管理员代传后整块换背景图。渐变蒙版只压上下两端（保标题/统计条可读），
// 中段近乎透出让人物/风景露出来；配合 .m-hero--img 拉高成横幅、标题压顶统计沉底
const heroStyle = computed(() => {
  if (!summary.value.heroUrl) return {}
  return {
    backgroundImage: `linear-gradient(180deg, rgba(15,23,42,0.52) 0%, rgba(15,23,42,0.06) 42%, rgba(15,23,42,0.08) 62%, rgba(15,23,42,0.44) 100%), url(${avatarImg(summary.value.heroUrl)})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center'
  }
})

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
/* 手机画布:手机上(视口<=480px)无感知;桌面打开时居中成手机宽度,两侧暗色留白 */
.m-page {
  max-width: 480px;
  margin: 0 auto;
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.25);
  min-height: 100vh;
  /* 户外主题:极淡的等高线地形纹(地图感),白色卡片压在上面;线条色比底色只深一档,远看不吵近看有细节 */
  background-color: #f4f6fb;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='280' height='280' viewBox='0 0 280 280'%3E%3Cg fill='none' stroke='%23dde6f4' stroke-width='1.3'%3E%3Cpath d='M20 200 Q80 120 150 160 T270 120'/%3E%3Cpath d='M10 160 Q70 90 140 130 T260 90'/%3E%3Cpath d='M0 120 Q60 50 130 90 T250 50'/%3E%3Cpath d='M30 230 Q90 150 160 190 T280 150'/%3E%3Cpath d='M40 90 Q100 20 170 60 T290 20'/%3E%3C/g%3E%3C/svg%3E");
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

/* ===== 头图模式：拉高成横幅 =====
   默认渐变模式标题+统计条挤在 ~225px 里没问题；换成照片后被文字/蒙版/领奖台遮得死死的。
   头图模式改为：最小高 340px 的横幅，标题压顶(文字阴影替代重蒙版)，统计条 margin-top:auto 沉底，
   中间整段让给图片；渐变蒙版也只压两端(见 heroStyle)。不设头图时样式与原版完全一致 */
.m-hero--img {
  min-height: 340px;
  display: flex;
  flex-direction: column;
}
.m-hero--img .m-hero-top {
  margin-bottom: 0;
}
.m-hero--img .m-stats {
  margin-top: auto;
}
.m-hero--img .m-hero-title {
  text-shadow: 0 2px 10px rgba(15, 23, 42, 0.65);
}
.m-hero--img .m-hero-sub {
  text-shadow: 0 1px 6px rgba(15, 23, 42, 0.65);
}
.m-hero--img .m-live-dot {
  box-shadow: none;
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
  /* 头图模式下统计条压在照片上，0.18 白底+磨砂保证亮色照片上数字也清晰 */
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.24);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.m-stat {
  flex: 1;
  text-align: center;
  min-width: 0;
}

/* 户外主题小图标(🥾👣🧭),压在数字上方 */
.m-stat-ico {
  display: block;
  font-style: normal;
  font-size: 15px;
  line-height: 1;
  margin-bottom: 4px;
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
  font-size: 27px;
  line-height: 1;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #94a3b8, #64748b);
  border: 3px solid #e2e8f0;
  box-shadow: 0 4px 12px rgba(28, 25, 23, 0.12);
  /* 注意不能加 overflow:hidden：奖牌要挂在圆框外沿 */
}

.m-pod-1 .m-pod-avatar {
  width: 64px;
  height: 64px;
  font-size: 33px;
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

/* 名次改金银铜奖牌emoji挂在头像右下外沿(原数字小圆徽在照片模式下会被裁且太小看不清) */
.m-medal {
  position: absolute;
  right: -5px;
  bottom: -5px;
  font-size: 20px;
  font-style: normal;
  line-height: 1;
  filter: drop-shadow(0 2px 3px rgba(28, 25, 23, 0.35));
}

.m-pod-1 .m-medal {
  font-size: 24px;
  right: -6px;
  bottom: -6px;
}

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

/* 亚军/季军积分胶囊配色与奖牌呼应（冠军金色在下方覆盖） */
.m-pod-2 .m-pod-points {
  color: #475569;
  background: #f1f5f9;
}

.m-pod-3 .m-pod-points {
  color: #9a3412;
  background: #fff3e8;
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
  /* 中性灰投影：头图模式下顶部已是照片，蓝调投影会显得突兀 */
  box-shadow: 0 6px 16px -12px rgba(28, 25, 23, 0.22);
}

.m-search {
  background: transparent;
  padding: 10px 12px 2px;
}

.m-search :deep(.van-search__content) {
  background: #fff;
  box-shadow: var(--bc-shadow-sm);
}

/* iOS Safari 在输入框字号<16px时聚焦会自动放大页面,提到16px根治 */
.m-search :deep(.van-field__control) {
  font-size: 16px;
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

/* 领奖台(1-3)之下、前 10 名：淡蓝圆徽标延续领奖台的荣誉层级；11 名起保持灰色斜体数字 */
.m-rank-num.is-top {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  border-radius: 50%;
  background: #eef4ff;
  color: #1d4ed8;
  font-style: normal;
  font-size: 13px;
}

.m-row-avatar {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  line-height: 1;
  font-weight: 800;
  color: #fff;
  overflow: hidden;
  /* 白描边+浅投影与领奖台头像同一语言；照片/emoji 通用 */
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(28, 25, 23, 0.14);
}

/* 自定义头像图片：铺满圆框；渐变底在图片加载完成前露出来当占位 */
.m-row-avatar img,
.m-pod-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
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

<style>
/* 桌面居中画布的两侧底色(仅 /points 独立路由使用,不在后台布局内) */
body:has(.m-page) {
  background: #111827;
}
</style>

