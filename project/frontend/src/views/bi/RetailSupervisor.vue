<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 店仓页签 -->
      <el-tab-pane label="店仓" name="store">
        <el-card shadow="never" class="search-card search-card--compact">
          <el-form :model="storeQuery" inline>
            <el-form-item label="编码">
              <el-input v-model="storeQuery.code" placeholder="请输入店仓编码" clearable @keyup.enter="storeSearch" />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="storeQuery.name" placeholder="请输入店仓名称" clearable @keyup.enter="storeSearch" />
            </el-form-item>
            <el-form-item label="零售主管">
              <el-input v-model="storeQuery.supervisorName" placeholder="请输入零售主管" clearable @keyup.enter="storeSearch" />
            </el-form-item>
            <el-form-item label="工号">
              <el-input v-model="storeQuery.erpJobNo" placeholder="请输入ERP工号" clearable @keyup.enter="storeSearch" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="storeSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="storeReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">店仓列表</span>
            </div>
          </template>

          <div class="table-responsive">
            <el-table v-loading="storeLoading" :data="storeData" border stripe empty-text="暂无数据">
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="storeCode" label="店仓编码" min-width="120" show-overflow-tooltip />
              <el-table-column prop="storeName" label="店仓名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="supervisorCode" label="零售主管编码" min-width="130" show-overflow-tooltip />
              <el-table-column prop="supervisorName" label="零售主管" min-width="120" show-overflow-tooltip />
              <el-table-column prop="erpJobNo" label="ERP工号" min-width="110" show-overflow-tooltip />
              <el-table-column prop="phone" label="手机号" min-width="130" show-overflow-tooltip />
              <el-table-column label="操作" width="100" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="storeQuery.pageNum"
              v-model:page-size="storeQuery.pageSize"
              :total="storeTotal"
              :page-sizes="PAGE_SIZES"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="storeSearch"
              @current-change="loadStoreData"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 客户页签 -->
      <el-tab-pane label="客户" name="customer">
        <el-card shadow="never" class="search-card search-card--compact">
          <el-form :model="customerQuery" inline>
            <el-form-item label="编码">
              <el-input v-model="customerQuery.code" placeholder="请输入客户编码" clearable @keyup.enter="customerSearch" />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="customerQuery.name" placeholder="请输入客户名称" clearable @keyup.enter="customerSearch" />
            </el-form-item>
            <el-form-item label="零售主管">
              <el-input v-model="customerQuery.supervisorName" placeholder="请输入零售主管" clearable @keyup.enter="customerSearch" />
            </el-form-item>
            <el-form-item label="工号">
              <el-input v-model="customerQuery.erpJobNo" placeholder="请输入ERP工号" clearable @keyup.enter="customerSearch" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="customerSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="customerReset">重置</el-button>
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
            <el-table v-loading="customerLoading" :data="customerData" border stripe empty-text="暂无数据">
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="customerCode" label="客户编码" min-width="120" show-overflow-tooltip />
              <el-table-column prop="customerName" label="客户名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="supervisorCode" label="零售主管编码" min-width="130" show-overflow-tooltip />
              <el-table-column prop="supervisorName" label="零售主管" min-width="120" show-overflow-tooltip />
              <el-table-column prop="erpJobNo" label="ERP工号" min-width="110" show-overflow-tooltip />
              <el-table-column prop="phone" label="手机号" min-width="130" show-overflow-tooltip />
              <el-table-column label="操作" width="100" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="customerQuery.pageNum"
              v-model:page-size="customerQuery.pageSize"
              :total="customerTotal"
              :page-sizes="PAGE_SIZES"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="customerSearch"
              @current-change="loadCustomerData"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
defineOptions({ name: 'RetailSupervisor' })
import { ref, onMounted } from 'vue'
import { Search, RefreshRight, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PAGE_SIZES } from '@/utils/appConfig'

// ===== Tab =====
const activeTab = ref('store')
function onTabChange(tab) {
  if (tab === 'store' && !storeData.value.length) loadStoreData()
  if (tab === 'customer' && !customerData.value.length) loadCustomerData()
}

// ===== 店仓 =====
const storeLoading = ref(false)
const storeData = ref([])
const storeTotal = ref(0)
const storeQuery = ref({ pageNum: 1, pageSize: 10, code: '', name: '', supervisorName: '', erpJobNo: '' })

async function loadStoreData() {
  storeLoading.value = true
  try {
    // TODO: 替换为真实 API
    // const res = await getRetailSupervisorStorePage(storeQuery.value)
    // storeData.value = res.data?.records || []
    // storeTotal.value = res.data?.total || 0
    storeData.value = []
    storeTotal.value = 0
  } finally {
    storeLoading.value = false
  }
}
function storeSearch() { storeQuery.value.pageNum = 1; loadStoreData() }
function storeReset() { storeQuery.value.code = ''; storeQuery.value.name = ''; storeQuery.value.supervisorName = ''; storeQuery.value.erpJobNo = ''; storeSearch() }

// ===== 客户 =====
const customerLoading = ref(false)
const customerData = ref([])
const customerTotal = ref(0)
const customerQuery = ref({ pageNum: 1, pageSize: 10, code: '', name: '', supervisorName: '', erpJobNo: '' })

async function loadCustomerData() {
  customerLoading.value = true
  try {
    // TODO: 替换为真实 API
    // const res = await getRetailSupervisorCustomerPage(customerQuery.value)
    // customerData.value = res.data?.records || []
    // customerTotal.value = res.data?.total || 0
    customerData.value = []
    customerTotal.value = 0
  } finally {
    customerLoading.value = false
  }
}
function customerSearch() { customerQuery.value.pageNum = 1; loadCustomerData() }
function customerReset() { customerQuery.value.code = ''; customerQuery.value.name = ''; customerQuery.value.supervisorName = ''; customerQuery.value.erpJobNo = ''; customerSearch() }

// ===== 操作 =====
function handleEdit(row) {
  // TODO: 打开编辑弹窗/跳转编辑页
  ElMessage.info('编辑功能开发中')
}

onMounted(() => loadStoreData())
</script>

<style scoped>
.search-card {
  margin-bottom: 12px;
}
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header-title {
  font-size: 16px;
  font-weight: 600;
}
.pagination-wrapper {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
