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
          <el-table-column prop="createType" label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.createType === '1' ? 'primary' : 'info'" size="small">{{ row.createType === '1' ? '文字' : row.createType === '2' ? '图片' : row.createType === '3' ? '视频' : row.createType || '-' }}</el-tag>
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
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES_LG } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import { getMomentPage } from '@/api/qywx'

const { loading, tableData, total, query, loadData, handleSearch: onSearch } = usePageQuery(getMomentPage, {
  creatorName: ''
})

function onReset() {
  query.creatorName = ''
  onSearch()
}

onMounted(() => { loadData() })
</script>

<style scoped>
</style>
