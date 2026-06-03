<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card search-card--compact">
      <el-form :model="query" inline>
        <el-form-item label="编码">
          <el-input v-model="query.code" placeholder="请输入客户编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="请输入客户名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">客户列表</span>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="CODE" label="客户编码" min-width="150" />
          <el-table-column prop="NAME" label="客户名称" min-width="200" />
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'ErpCustomer' })
import { onMounted } from 'vue'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/appConfig'
import { getErpCustomerPage } from '@/api/erp'

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(
  getErpCustomerPage,
  { code: '', name: '' }
)

onMounted(() => loadData())
</script>
