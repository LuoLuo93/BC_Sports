import { ref } from 'vue'

export const PAGE_SIZES = [10, 20, 50]
export const PAGE_SIZES_LG = [20, 50, 100]

export const SYNC_STATUS = {
  NOT_SYNCED: 0,
  SYNCED: 1,
  FAILED: 2,
  SKIPPED: 3
}

export const defaultPageSize = ref(20)
export const dateFormat = ref('yyyy-MM-dd HH:mm:ss')
export const timezone = ref('GMT+8')
export const systemName = ref('BC体育数据管理系统')

export function applyPublicConfig(data) {
  if (data['sys.pageSize']) {
    const size = parseInt(data['sys.pageSize'], 10)
    if (size > 0) defaultPageSize.value = size
  }
  if (data['sys.dateFormat']) dateFormat.value = data['sys.dateFormat']
  if (data['sys.timezone']) timezone.value = data['sys.timezone']
  if (data['sys.name']) systemName.value = data['sys.name']
}
