<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="任务状态">
          <el-select v-model="query.buildable" style="width:130px" @change="onSearch">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
            <el-option label="全部" :value="null" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名">
          <el-input v-model="query.keyword" placeholder="请输入任务名(模糊)" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <div class="stat-row">
            <span class="card-header-title">Jenkins 任务状态（BC_SPORTS_JENKINS_JOB_STATE）</span>
            <el-tag size="small" type="success" effect="plain">启用 {{ info.enabled ?? '-' }}</el-tag>
            <el-tag size="small" type="info" effect="plain">停用 {{ info.disabled ?? '-' }}</el-tag>
            <el-tag size="small" effect="plain">最近同步: {{ info.lastSyncTime || '-' }}</el-tag>
          </div>
          <div>
            <el-button size="small" :loading="syncing" :icon="Refresh" @click="onSync">立即同步</el-button>
            <el-button v-if="info.baseUrl" size="small" type="primary" plain :icon="Position" @click="openConsole">打开Jenkins控制台</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="jobName" label="任务名" min-width="220" fixed show-overflow-tooltip>
          <template #default="{ row }">
            <el-link v-if="row.jobUrl" type="primary" :href="row.jobUrl" target="_blank">{{ row.jobName }}</el-link>
            <span v-else>{{ row.jobName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.buildable === 1 ? 'success' : 'info'">{{ row.buildable === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近构建" width="90" align="center">
          <template #default="{ row }">
            <span class="mono">{{ row.lastBuildNumber ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="resultTag(row.lastResult)">{{ resultText(row.lastResult) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="165">
          <template #default="{ row }">{{ formatTime(row.lastBuildTime) }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">{{ formatDuration(row.lastDurationMs) }}</template>
        </el-table-column>
        <el-table-column label="健康分" width="90" align="center">
          <template #default="{ row }">
            <span :style="healthStyle(row.healthScore)">{{ row.healthScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近成功" width="90" align="center">
          <template #default="{ row }">
            <span class="mono">{{ row.lastSuccessBuild ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="成功时间" width="165">
          <template #default="{ row }">{{ formatTime(row.lastSuccessTime) }}</template>
        </el-table-column>
        <el-table-column prop="nextBuildNumber" label="下一号" width="80" align="center" />
        <el-table-column prop="updateTime" label="同步时间" width="165" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="onSizeChange"
          @current-change="onPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'JenkinsJobs' })
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, Refresh, Position } from '@element-plus/icons-vue'
import { getJenkinsJobPage, getJenkinsInfo, syncJenkinsNow } from '@/api/monitor'
import { formatTime } from '@/utils/format'
import { PAGE_SIZES } from '@/utils/appConfig'
import { usePageQuery } from '@/composables/usePageQuery'

// 默认只看启用任务(停用任务入库不展示,下拉可切换)
const { loading, tableData, total, query, loadData, handleSearch } = usePageQuery(getJenkinsJobPage, {
  buildable: 1,
  keyword: ''
})

const info = ref({})
const syncing = ref(false)

async function loadInfo() {
  const res = await getJenkinsInfo()
  if (res.code === 200) info.value = res.data || {}
}
loadInfo()
loadData()

function onSearch() {
  handleSearch()
}

function onReset() {
  query.buildable = 1
  query.keyword = ''
  query.pageNum = 1
  handleSearch()
}

function onSizeChange() {
  loadData()
}

function onPageChange() {
  loadData()
}

async function onSync() {
  syncing.value = true
  try {
    const res = await syncJenkinsNow()
    if (res.code === 200) {
      ElMessage.success(res.data || '同步完成')
      await Promise.all([loadData(), loadInfo()])
    } else {
      ElMessage.error(res.msg || '同步失败')
    }
  } finally {
    syncing.value = false
  }
}

function openConsole() {
  window.open(info.value.baseUrl, '_blank', 'noopener')
}

function resultTag(result) {
  if (result === 'SUCCESS') return 'success'
  if (result === 'UNSTABLE') return 'warning'
  if (result === 'FAILURE') return 'danger'
  return 'info'
}

function resultText(result) {
  return result || '-'
}

function formatDuration(ms) {
  if (ms === null || ms === undefined) return '-'
  if (ms < 1000) return ms + 'ms'
  const s = Math.floor(ms / 1000)
  if (s < 60) return s + 's'
  const m = Math.floor(s / 60)
  if (m < 60) return m + 'm' + String(s % 60).padStart(2, '0') + 's'
  const h = Math.floor(m / 60)
  return h + 'h' + String(m % 60).padStart(2, '0') + 'm'
}

function healthStyle(score) {
  if (score === null || score === undefined) return {}
  if (score >= 80) return { color: '#16a34a', fontWeight: 600 }
  if (score >= 40) return { color: '#d97706', fontWeight: 600 }
  return { color: '#dc2626', fontWeight: 600 }
}
</script>

<style scoped>
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.stat-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.card-header-title {
  font-weight: 700;
  margin-right: 4px;
}
.mono {
  font-family: 'Cascadia Code', 'Fira Code', Consolas, monospace;
  font-weight: 600;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
