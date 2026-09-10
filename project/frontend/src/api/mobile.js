/**
 * 移动端「运动积分排名」数据源
 *
 * 数据来自后台导入的 BC_SPORTS_BCP_SPORT_POINTS 表（BC好玩家 → 运动积分导入），
 * 后端 GET /api/bcp/sport-points/rank 已在 Shiro 放行 anon（免登录外链可直接打开）。
 * 接口固定返回前 100 名（业务上限），关键字过滤与"第4名起分页"都在前端对这份数据做，
 * 与原演示版行为一致。
 */
import request from './request'

// 榜单最多展示前 100 名（与后端 RANK_TOP_LIMIT 一致）
const MAX_SHOW = 100

/* ============================ 数据获取 ============================ */

// 同一时刻并发刷新（汇总+列表）共享一次请求；不做时间缓存，下拉刷新始终取最新
let inFlight = null

async function fetchRankRows() {
  if (inFlight) return inFlight
  inFlight = request
    .get('/api/bcp/sport-points/rank')
    .then(res => res.data || [])
    .finally(() => { inFlight = null })
  return inFlight
}

function filterRows(rows, { keyword = '' } = {}) {
  if (!keyword) return rows
  return rows.filter(r => r.name.includes(keyword))
}

function todayLabel() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function weekNo() {
  const now = new Date()
  const start = new Date(now.getFullYear(), 0, 1)
  const days = Math.floor((now - start) / 86400000)
  return Math.ceil((days + start.getDay() + 1) / 7)
}

/* ============================ 对外接口 ============================ */

/**
 * 分页查询排名列表。领奖台已展示前三名,本接口从第 4 名起返回
 * @param {{ page: number, pageSize: number, keyword?: string }} params
 * @returns {Promise<{code: number, data: {list: Array, total: number}}>}
 *   list 元素: { id, name, points, rank }  rank 为全榜绝对名次
 */
export async function fetchRankList(params) {
  const { page = 1, pageSize = 10, keyword = '' } = params || {}
  try {
    const ranked = filterRows(await fetchRankRows(), { keyword }).slice(0, MAX_SHOW)
    const listArea = ranked.slice(3) // 前三名在领奖台展示,列表跳过
    const start = (page - 1) * pageSize
    return { code: 200, data: { list: listArea.slice(start, start + pageSize), total: listArea.length } }
  } catch {
    return { code: 500, data: { list: [], total: 0 } }
  }
}

/**
 * 榜单汇总(头部指标 + 领奖台前三名)
 * @returns {Promise<{code: number, data: {participants: number, totalPoints: number,
 *   avgPoints: number, top3: Array, date: string, weekNo: number}}>}
 */
export async function fetchRankSummary(params) {
  const { keyword = '' } = params || {}
  try {
    const ranked = filterRows(await fetchRankRows(), { keyword }).slice(0, MAX_SHOW)
    const totalPoints = ranked.reduce((s, r) => s + (r.points || 0), 0)
    return {
      code: 200,
      data: {
        participants: ranked.length,
        totalPoints,
        avgPoints: ranked.length ? Math.round(totalPoints / ranked.length) : 0,
        top3: ranked.slice(0, 3),
        date: todayLabel(),
        weekNo: weekNo()
      }
    }
  } catch {
    return { code: 500, data: { participants: 0, totalPoints: 0, avgPoints: 0, top3: [], date: '', weekNo: 0 } }
  }
}
