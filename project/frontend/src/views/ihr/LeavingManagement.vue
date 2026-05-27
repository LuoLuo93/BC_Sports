<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="员工姓名">
          <el-input v-model="query.staffName" placeholder="请输入员工姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="员工编号">
          <el-input v-model="query.staffNo" placeholder="请输入员工编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="同步状态">
          <el-select v-model="query.syncStatus" placeholder="全部" clearable >
            <el-option label="未同步" value="pending" />
            <el-option label="同步中" value="syncing" />
            <el-option label="已同步" value="success" />
            <el-option label="同步失败" value="failed" />
          </el-select>
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
          <span class="card-header-title">离职员工列表</span>
          <el-button v-if="hasPermission('ihr:leaving:sync')" type="success" size="small" :icon="Refresh" :loading="syncLoading" @click="handleSyncIhr">从IHR同步</el-button>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="staffName" label="员工姓名" min-width="100" />
          <el-table-column prop="staffNo" label="员工编号" min-width="110" />
          <el-table-column prop="mobileNo" label="手机号" width="130" />
          <el-table-column prop="departmentName" label="部门" min-width="140" />
          <el-table-column label="同步状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="syncStatusTag(row.syncStatus)" size="small">{{ syncStatusLabel(row.syncStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="syncTime" label="同步时间" width="170" align="center">
            <template #default="{ row }">{{ row.syncTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('ihr:leaving:sync')" type="primary" plain size="small" @click="handleSyncOne(row)">同步企微</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSearch" @current-change="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'IhrLeaving' })
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getIhrLeavingPage, syncIhrLeaving, syncQywxLeaving } from '@/api/ihr'
import { syncStatusLabel, syncStatusTag } from '@/utils/syncStatus'
import { Search, RefreshRight, Refresh } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/constants'

const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getIhrLeavingPage, { staffName: '', staffNo: '', syncStatus: undefined })
const syncLoading = ref(false)

async function handleSyncIhr() {
  await ElMessageBox.confirm('确定从IHR系统同步离职数据？', '提示', { type: 'warning' })
  syncLoading.value = true
  try { await syncIhrLeaving(); ElMessage.success('同步任务已触发'); loadData() } finally { syncLoading.value = false }
}
async function handleSyncOne(row) {
  await syncQywxLeaving(row.employeeId); ElMessage.success('同步任务已触发'); loadData()
}

onMounted(() => loadData())
</script>
