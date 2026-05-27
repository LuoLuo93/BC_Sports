const syncMap = {
  0: ['未同步', 'info'],
  1: ['已同步', 'success'],
  2: ['同步失败', 'danger'],
  3: ['已跳过', 'warning']
}

export function syncStatusLabel(s) {
  return syncMap[s]?.[0] || s
}

export function syncStatusTag(s) {
  return syncMap[s]?.[1] || 'info'
}
