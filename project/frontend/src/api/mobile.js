/**
 * 移动端「运动积分排名」数据源
 *
 * 数据来自后台导入的 BC_SPORTS_BCP_SPORT_POINTS 表（BC好玩家 → 运动积分导入），
 * 后端 GET /api/bcp/sport-points/rank 已在 Shiro 放行 anon（免登录外链可直接打开）。
 * 无关键字返回前 100 名 + 全表真实统计；带关键字后端全表按姓名模糊搜索（可搜到
 * 100 名以外的人，名次为全榜绝对名次）。领奖台切片与触底分页在前端完成。
 */
import request from './request'

/* ============================ 数据获取 ============================ */

// 同一关键字并发刷新（汇总+列表）共享一次请求；不做时间缓存，下拉刷新始终取最新
const inFlight = new Map()

async function fetchBoard(keyword = '') {
  const key = keyword || '__all__'
  if (inFlight.has(key)) return inFlight.get(key)
  const p = request
    .get('/api/bcp/sport-points/rank', { params: keyword ? { keyword } : {} })
    .then(res => res.data || { list: [], participants: 0, totalPoints: 0 })
    .finally(() => inFlight.delete(key))
  inFlight.set(key, p)
  return p
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
 * 分页查询排名列表。
 * 无关键字:领奖台(全榜前三)占用榜单前三名,列表从第 4 名起;
 * 搜索时:领奖台保持全榜前三不变,列表显示全部命中(从第 1 名起,含前三名)。
 * @param {{ page: number, pageSize: number, keyword?: string }} params
 * @returns {Promise<{code: number, data: {list: Array, total: number}}>}
 *   list 元素: { id, name, points, rank }  rank 为全榜绝对名次
 */
export async function fetchRankList(params) {
  const { page = 1, pageSize = 10, keyword = '' } = params || {}
  try {
    const board = await fetchBoard(keyword.trim())
    const rows = board.list || []
    const searching = Boolean(keyword.trim())
    const listArea = searching ? rows : rows.slice(3)
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
    const board = await fetchBoard(keyword.trim())
    const participants = board.participants || 0
    const totalPoints = board.totalPoints || 0
    return {
      code: 200,
      data: {
        participants,
        totalPoints,
        avgPoints: participants ? Math.round(totalPoints / participants) : 0,
        // 领奖台永远用全榜前三(board.top3),搜索时不变
        top3: board.top3 || board.list.slice(0, 3),
        date: todayLabel(),
        weekNo: weekNo()
      }
    }
  } catch {
    return { code: 500, data: { participants: 0, totalPoints: 0, avgPoints: 0, top3: [], date: '', weekNo: 0 } }
  }
}