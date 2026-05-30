import { dateFormat } from './appConfig'

const TOKEN_MAP = {
  'yyyy': (d) => d.getFullYear(),
  'MM': (d) => String(d.getMonth() + 1).padStart(2, '0'),
  'dd': (d) => String(d.getDate()).padStart(2, '0'),
  'HH': (d) => String(d.getHours()).padStart(2, '0'),
  'mm': (d) => String(d.getMinutes()).padStart(2, '0'),
  'ss': (d) => String(d.getSeconds()).padStart(2, '0')
}

function applyFormat(date, fmt) {
  let result = fmt
  for (const [token, fn] of Object.entries(TOKEN_MAP)) {
    result = result.replace(token, fn(date))
  }
  return result
}

function toDate(t) {
  if (Array.isArray(t)) {
    const [y, m, d, h, min, s] = t
    return new Date(y, (m || 1) - 1, d || 1, h || 0, min || 0, s || 0)
  }
  const str = String(t).replace('T', ' ')
  const date = new Date(str)
  return isNaN(date.getTime()) ? null : date
}

export function formatTime(t) {
  if (!t) return '-'
  const date = toDate(t)
  if (!date) return String(t).replace('T', ' ').substring(0, 19)
  return applyFormat(date, dateFormat.value)
}
