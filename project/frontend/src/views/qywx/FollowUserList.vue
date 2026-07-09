<template>
  <div class="page-container">
    <!-- 搜索区 -->
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="成员姓名">
          <el-input v-model="query.name" placeholder="请输入成员姓名" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.mobile" placeholder="请输入手机号" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="主部门">
          <el-input v-model="query.mainDepartment" placeholder="请输入部门名称" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="onReset">重置</el-button>
          <el-button type="success" size="small" :icon="Refresh" :loading="syncLoading" @click="handleSync">数据同步</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">客户联系成员</span>
        </div>
      </template>
      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="name" label="姓名" min-width="80" />
          <el-table-column prop="mobile" label="手机号" width="130">
            <template #default="{ row }">{{ row.mobile || '-' }}</template>
          </el-table-column>
          <el-table-column prop="mainDepartment" label="主部门" min-width="100">
            <template #default="{ row }">{{ row.mainDepartment || '-' }}</template>
          </el-table-column>
          <el-table-column label="录入时间" width="190" align="center">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" plain size="small" @click="openCustomerDialog(row)">客户</el-button>
              <el-button type="info" plain size="small" @click="openGroupStatDialog(row)">群统计</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          @size-change="() => { query.pageNum = 1; loadData() }"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 客户弹窗 -->
    <el-dialog v-model="customerDialogVisible" :title="`客户列表 - ${currentUser.name || currentUser.followUser}`" width="1200px" destroy-on-close>
      <el-form inline style="margin-bottom:12px">
        <el-form-item label="客户名称">
          <el-input v-model="customerQuery.name" placeholder="请输入客户名称" clearable @keyup.enter="handleCustomerSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleCustomerSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="resetCustomerQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="table-responsive">
        <el-table v-loading="customerLoading" :data="customerData" border stripe empty-text="暂无数据" max-height="400">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="name" label="客户名称" min-width="120" />
          <el-table-column prop="externalUserid" label="外部联系人ID" min-width="140" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.type === '1' ? 'primary' : 'warning'" size="small">{{ row.type === '1' ? '微信' : '企微' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150">
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column prop="addWay" label="添加方式" width="110" align="center">
            <template #default="{ row }">{{ formatAddWay(row.addWay) }}</template>
          </el-table-column>
          <el-table-column label="添加时间" width="180" align="center">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="customerQuery.pageNum"
          v-model:page-size="customerQuery.pageSize"
          :total="customerTotal"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          @size-change="() => { customerQuery.pageNum = 1; loadCustomerData() }"
          @current-change="loadCustomerData"
        />
      </div>
    </el-dialog>

    <!-- 群统计弹窗 -->
    <el-dialog v-model="groupStatDialogVisible" :title="`群聊统计 - ${currentUser.name || currentUser.followUser}`" width="900px" destroy-on-close>
      <div class="table-responsive">
        <el-table v-loading="groupStatLoading" :data="groupStatData" border stripe empty-text="暂无数据" max-height="400">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="starttime" label="日期" width="110" align="center" />
          <el-table-column prop="newChatCnt" label="新增群数" width="90" align="center" />
          <el-table-column prop="chatTotal" label="群总数" width="90" align="center" />
          <el-table-column prop="chatHasMsg" label="有消息群数" width="100" align="center" />
          <el-table-column prop="newMemberCnt" label="新增成员" width="90" align="center" />
          <el-table-column prop="memberTotal" label="成员总数" width="90" align="center" />
          <el-table-column prop="memberHasMsg" label="有消息成员" width="100" align="center" />
          <el-table-column prop="msgTotal" label="消息总数" width="90" align="center" />
        </el-table>
      </div>
      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="groupStatQuery.pageNum"
          v-model:page-size="groupStatQuery.pageSize"
          :total="groupStatTotal"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          @size-change="() => { groupStatQuery.pageNum = 1; loadGroupStatData() }"
          @current-change="loadGroupStatData"
        />
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
defineOptions({ name: 'FollowUserList' })
import { ref, reactive, onMounted } from 'vue'
import { Search, RefreshRight, Refresh } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { useSyncAction } from '@/composables/useSyncAction'
import { PAGE_SIZES } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import { getFollowUserPage, getFollowUserCustomers, getFollowUserGroupStats, syncFollowUsers, getFollowUserSyncStatus } from '@/api/qywx'

// ===== 添加方式映射 =====
const ADD_WAY_MAP = {
  '0': '其他',
  '1': '扫描二维码',
  '2': '搜索手机号',
  '3': '名片分享',
  '4': '群聊',
  '5': '通讯录',
  '6': '微信联系人',
  '8': '公众号',
  '9': '来自微信',
  '10': '其他',
  '11': '微信搜索',
  '12': '视频号',
  '13': '添加好友'
}
function formatAddWay(val) { return ADD_WAY_MAP[val] || val || '-' }

// ===== 主列表（统一 usePageQuery）=====
const { loading, tableData, total, query, loadData, handleSearch: onSearch } = usePageQuery(getFollowUserPage, {
  name: '', mobile: '', mainDepartment: ''
})

function onReset() {
  query.name = ''
  query.mobile = ''
  query.mainDepartment = ''
  onSearch()
}

// ===== 数据同步 =====
const { syncLoading, handleSync, checkStatus } = useSyncAction(syncFollowUsers, loadData, '确定同步客户联系成员数据？', getFollowUserSyncStatus)

// ===== 客户弹窗 =====
const customerDialogVisible = ref(false)
const customerLoading = ref(false)
const customerData = ref([])
const customerTotal = ref(0)
const currentUser = ref({})
const customerQuery = reactive({ userid: '', name: '', pageNum: 1, pageSize: 20 })

function openCustomerDialog(row) {
  currentUser.value = row
  customerQuery.userid = row.followUser
  customerQuery.name = ''
  customerQuery.pageNum = 1
  customerDialogVisible.value = true
  loadCustomerData()
}

async function loadCustomerData() {
  customerLoading.value = true
  try {
    const res = await getFollowUserCustomers(customerQuery)
    customerData.value = res.data?.records || []
    customerTotal.value = res.data?.total || 0
  } finally {
    customerLoading.value = false
  }
}
function handleCustomerSearch() { customerQuery.pageNum = 1; loadCustomerData() }
function resetCustomerQuery() { customerQuery.name = ''; customerQuery.pageNum = 1; loadCustomerData() }

// ===== 群统计弹窗 =====
const groupStatDialogVisible = ref(false)
const groupStatLoading = ref(false)
const groupStatData = ref([])
const groupStatTotal = ref(0)
const groupStatQuery = reactive({ owner: '', pageNum: 1, pageSize: 20 })

function openGroupStatDialog(row) {
  currentUser.value = row
  groupStatQuery.owner = row.followUser
  groupStatQuery.pageNum = 1
  groupStatDialogVisible.value = true
  loadGroupStatData()
}

async function loadGroupStatData() {
  groupStatLoading.value = true
  try {
    const res = await getFollowUserGroupStats(groupStatQuery)
    groupStatData.value = res.data?.records || []
    groupStatTotal.value = res.data?.total || 0
  } finally {
    groupStatLoading.value = false
  }
}
function handleGroupStatSearch() { groupStatQuery.pageNum = 1; loadGroupStatData() }

onMounted(() => { loadData(); checkStatus() })
</script>

<style scoped>
</style>
