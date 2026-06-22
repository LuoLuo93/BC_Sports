<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="群名称">
          <el-input v-model="query.name" placeholder="请输入群名称" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="群主">
          <el-input v-model="query.ownerName" placeholder="请输入群主姓名" clearable @keyup.enter="onSearch" />
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
          <span class="card-header-title">群聊管理</span>
        </div>
      </template>
      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="name" label="群名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="chatId" label="群ID" min-width="180" show-overflow-tooltip />
          <el-table-column prop="ownerName" label="群主" width="120" />
          <el-table-column prop="memberList" label="成员数" width="90" align="center" />
          <el-table-column label="创建时间" width="180" align="center">
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
defineOptions({ name: 'GroupChatList' })
import { onMounted } from 'vue'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES_LG } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import { getGroupChatPage } from '@/api/qywx'

const { loading, tableData, total, query, loadData, handleSearch: onSearch } = usePageQuery(getGroupChatPage, {
  name: '', ownerName: ''
})

function onReset() {
  query.name = ''
  query.ownerName = ''
  onSearch()
}

onMounted(() => { loadData() })
</script>

<style scoped>
</style>
