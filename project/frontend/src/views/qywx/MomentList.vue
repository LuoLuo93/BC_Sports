<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="发布者">
          <el-input v-model="query.creatorName" placeholder="请输入发布者姓名" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="onReset">重置</el-button>
          <el-button type="success" size="small" :icon="Refresh" :loading="syncLoading" @click="handleSync">数据同步</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">朋友圈管理</span>
        </div>
      </template>
      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="creatorName" label="发布者" width="120" />
          <el-table-column prop="createType" label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="TYPE_TAG_MAP[row.createType] || 'info'" size="small">{{ formatCreateType(row.createType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" width="180" align="center">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES_LG"
          layout="total, sizes, prev, pager, next"
          @size-change="() => { query.pageNum = 1; loadData() }"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'MomentList' })
import { onMounted } from 'vue'
import { Search, RefreshRight, Refresh } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { useSyncAction } from '@/composables/useSyncAction'
import { PAGE_SIZES_LG } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import { getMomentPage, syncMoments, getMomentSyncStatus } from '@/api/qywx'

const TYPE_MAP = { '0': '文字', '1': '图片', '2': '视频', '3': '图文链接', '4': '小程序' }
const TYPE_TAG_MAP = { '0': 'primary', '1': 'success', '2': 'warning', '3': 'info', '4': 'danger' }
function formatCreateType(val) { return TYPE_MAP[val] || val || '-' }

const { loading, tableData, total, query, loadData, handleSearch: onSearch } = usePageQuery(getMomentPage, {
  creatorName: ''
})

function onReset() {
  query.creatorName = ''
  onSearch()
}

// ===== 数据同步 =====
const { syncLoading, handleSync, checkStatus } = useSyncAction(syncMoments, loadData, '确定同步朋友圈数据？', getMomentSyncStatus)

onMounted(() => { loadData(); checkStatus() })
</script>

<style scoped>
</style>
