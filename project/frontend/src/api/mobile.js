/**
 * 移动端「运动积分排名」数据源
 *
 * 页面是对外分享的免登录链接,当前接的是本地示例数据。领导定稿数据来源后
 * 切换真实接口只需:
 *   1. 把 USE_MOCK 改为 false,实现下面的 request 调用(出参字段对齐 MockRow);
 *   2. 后端必须把对应接口在 Shiro 的 filterChainDefinitionMap 里放行为 anon
 *      (页面本身免登录,若接口仍要会话,外链打开会是空数据);
 *   3. 免登录接口建议只读、不加敏感字段,必要时加简单防刷(限频)。
 */
// import request from './request'

const USE_MOCK = true

/* ============================ 示例数据 ============================ */

const NAMES = [
  '张伟', '李娜', '王强', '刘敏', '陈杰', '杨静', '赵磊', '黄丽', '周涛', '吴倩',
  '徐明', '孙丽', '马超', '朱婷', '胡军', '郭欣', '何平', '高远', '林悦', '罗凯',
  '郑爽', '梁波', '谢婷', '宋佳', '唐磊', '韩雪', '冯刚', '曹颖', '彭飞', '董洁',
  '袁帅', '蔡文', '潘阳', '蒋琳', '余波', '杜鹃', '叶青', '程龙', '苏婷', '魏东',
  '吕萌', '丁一', '任爽', '沈虹', '姚远', '卢楠', '傅磊', '钟灵', '姜涛', '崔娜',
  '谭凯', '陆芸', '汪洋', '石磊', '金铭', '邱婷', '郎峰', '秦岚', '侯亮', '雷婷',
  '白帆', '华英', '曾磊', '尹航', '岳琳', '甘霖', '盛开', '童瑶', '翟鹏', '温馨',
  '季风', '聂海', '邢云', '路遥', '葛亮', '齐飞', '伍月', '覃芳', '贺兰', '龚雪',
  '阮清', '庄严', '章涵', '单涛', '封霖', '桑青', '黎明', '易安', '常青', '乐琪',
  '顾城', '邵杰', '万红', '项楠', '原野', '米兰', '池雨', '乔松', '桂香', '蔚然',
  '佟丽', '岑琳', '束婷', '麦琪', '展鹏', '丛珊', '官晶', '涂雅', '祝萌', '简宁',
  '缪彤', '关彤', '游咏', '池澄', '竺星', '阚泽', '冷月', '席琳', '隋蕾', '邝野'
]

// 需求:榜单最多展示前 100 名(领奖台 3 人 + 列表 97 人),超出部分不返回
const MAX_SHOW = 100

// 榜单周期:周榜/月榜/总榜,各自独立积分与随机种子
export const PERIODS = ['周榜', '月榜', '总榜']

// 固定随机种子,保证同名次数据稳定,便于演示和核对
function mulberry32(seed) {
  return function () {
    seed |= 0
    seed = (seed + 0x6d2b79f5) | 0
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
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

// 各榜单积分区间:[最小, 最大]
const PERIOD_CONF = {
  周榜: { min: 120, max: 850 },
  月榜: { min: 500, max: 3000 },
  总榜: { min: 3000, max: 12000 }
}

function buildRankRows(period) {
  const conf = PERIOD_CONF[period] || PERIOD_CONF.总榜
  const rand = mulberry32(20260900 + PERIODS.indexOf(period) * 977)
  const rows = NAMES.map((name, i) => {
    const points = Math.round((conf.min + rand() * (conf.max - conf.min)) / 10) * 10
    return {
      id: `${period}-${i + 1}`,
      name,
      points
    }
  })
  // 积分相同时按姓名稳定排序,避免名次跳动;排序后截取前 100 名
  rows.sort((a, b) => b.points - a.points || a.name.localeCompare(b.name, 'zh'))
  return rows.slice(0, MAX_SHOW).map((r, i) => ({ ...r, rank: i + 1 }))
}

function mockRequest(data, delay = 500) {
  return new Promise(resolve => {
    setTimeout(() => resolve({ code: 200, data, message: 'ok' }), delay)
  })
}

function filterRows(rows, { keyword = '' } = {}) {
  if (!keyword) return rows
  return rows.filter(r => r.name.includes(keyword))
}

/* ============================ 对外接口 ============================ */

/**
 * 分页查询排名列表。领奖台已展示前三名,本接口从第 4 名起返回
 * @param {{ page: number, pageSize: number, keyword?: string, period?: string }} params
 * @returns {Promise<{code: number, data: {list: Array, total: number}}>}
 *   list 元素: { id, name, points, rank }  rank 为全榜绝对名次
 */
export function fetchRankList(params) {
  if (USE_MOCK) {
    const { page = 1, pageSize = 10, keyword = '', period = '周榜' } = params || {}
    const ranked = filterRows(buildRankRows(period), { keyword })
    const listArea = ranked.slice(3) // 前三名在领奖台展示,列表跳过
    const start = (page - 1) * pageSize
    return mockRequest({ list: listArea.slice(start, start + pageSize), total: listArea.length })
  }
  // return request({ url: '/api/mobile/rank/list', method: 'get', params })
}

/**
 * 榜单汇总(头部指标 + 领奖台前三名)
 * @returns {Promise<{code: number, data: {participants: number, totalPoints: number,
 *   avgPoints: number, top3: Array, date: string, weekNo: number}}>}
 */
export function fetchRankSummary(params) {
  if (USE_MOCK) {
    const { keyword = '', period = '周榜' } = params || {}
    const ranked = filterRows(buildRankRows(period), { keyword })
    const totalPoints = ranked.reduce((s, r) => s + r.points, 0)
    return mockRequest({
      participants: ranked.length,
      totalPoints,
      avgPoints: ranked.length ? Math.round(totalPoints / ranked.length) : 0,
      top3: ranked.slice(0, 3),
      date: todayLabel(),
      weekNo: weekNo()
    })
  }
  // return request({ url: '/api/mobile/rank/summary', method: 'get', params })
}
