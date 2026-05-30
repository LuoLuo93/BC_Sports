<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="erp-tabs">
      <!-- 入职 -->
      <el-tab-pane label="入职" name="onboarding">
        <el-card shadow="never" class="search-card">
          <el-form :model="onboardQuery" inline>
            <el-form-item label="员工姓名">
              <el-input v-model="onboardQuery.staffName" placeholder="请输入员工姓名" clearable @keyup.enter="handleOnboardSearch" />
            </el-form-item>
            <el-form-item label="员工编号">
              <el-input v-model="onboardQuery.staffNo" placeholder="请输入员工编号" clearable @keyup.enter="handleOnboardSearch" />
            </el-form-item>
            <el-form-item label="同步状态">
              <el-select v-model="onboardQuery.syncStatus" placeholder="全部" clearable >
                <el-option label="未同步" :value="0" />
                <el-option label="已同步" :value="1" />
                <el-option label="同步失败" :value="2" />
                <el-option label="已跳过" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleOnboardSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetOnboardQuery">重置</el-button>
              <el-button v-if="hasPermission('erp:employee:sync')" type="success" size="small" :icon="Refresh" :loading="ihrSyncLoading" @click="handleSyncIhr">从IHR同步</el-button>
              <el-button v-if="hasPermission('erp:employee:sync')" type="warning" size="small" :icon="Refresh" :loading="erpSyncLoading" @click="handleSyncErp">同步到ERP</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">入职人员</span>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="onboardLoading" :data="onboardData" border stripe empty-text="暂无数据">
              <el-table-column label="#" width="50" align="center">
                <template #default="{ $index }">{{ (onboardQuery.pageNum - 1) * onboardQuery.pageSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="staffName" label="员工姓名" min-width="100" />
              <el-table-column prop="staffNo" label="员工编号" min-width="110" />
              <el-table-column prop="mobileNo" label="手机号" width="130" />
              <el-table-column prop="departmentName" label="部门" min-width="140" />
              <el-table-column label="入职日期" width="110" align="center">
                <template #default="{ row }">{{ row.dateLabel || '-' }}</template>
              </el-table-column>
              <el-table-column label="员工状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.staffStatus === 'IN_SERVICE' ? 'success' : 'danger'" size="small">
                    {{ row.staffStatus === 'IN_SERVICE' ? '在职' : '离职' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="同步状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="row.syncStatus === 2 && row.errorMessage" :content="row.errorMessage" placement="top">
                  <el-tag :type="syncStatusTag(row.syncStatus)" size="small">{{ syncStatusLabel(row.syncStatus) }}</el-tag>
                </el-tooltip>
                <el-tag v-else :type="syncStatusTag(row.syncStatus)" size="small">{{ syncStatusLabel(row.syncStatus) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="syncTime" label="同步时间" width="170" align="center">
                <template #default="{ row }">{{ row.syncTime || '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.syncStatus !== 'success' && hasPermission('erp:employee:sync')" type="primary" plain size="small" @click="handleOnboardSyncOne(row)">同步ERP</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="onboardQuery.pageNum" v-model:page-size="onboardQuery.pageSize" :total="onboardTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next" @size-change="handleOnboardSearch" @current-change="loadOnboardData" />
          </div>
        </el-card>
      </el-tab-pane>

        <!-- 变动 -->
        <el-tab-pane label="变动" name="adjustment">
          <el-card shadow="never" class="search-card">
            <el-form :model="adjQuery" inline>
              <el-form-item label="员工姓名">
                <el-input v-model="adjQuery.staffName" placeholder="请输入员工姓名" clearable @keyup.enter="handleAdjSearch" />
              </el-form-item>
              <el-form-item label="员工编号">
                <el-input v-model="adjQuery.staffNo" placeholder="请输入员工编号" clearable @keyup.enter="handleAdjSearch" />
              </el-form-item>
              <el-form-item label="同步状态">
                <el-select v-model="adjQuery.syncStatus" placeholder="全部" clearable >
                  <el-option label="未同步" :value="0" />
                  <el-option label="已同步" :value="1" />
                  <el-option label="同步失败" :value="2" />
                  <el-option label="已跳过" :value="3" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleAdjSearch">搜索</el-button>
                <el-button :icon="RefreshRight" @click="resetAdjQuery">重置</el-button>
                <el-button v-if="hasPermission('erp:employee:sync')" type="success" size="small" :icon="Refresh" :loading="ihrSyncLoading" @click="handleSyncIhr">从IHR同步</el-button>
                <el-button v-if="hasPermission('erp:employee:sync')" type="warning" size="small" :icon="Refresh" :loading="erpSyncLoading" @click="handleSyncErp">同步到ERP</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <div class="card-header-row">
                <span class="card-header-title">变动人员</span>
              </div>
            </template>
            <div class="table-responsive">
              <el-table v-loading="adjLoading" :data="adjData" border stripe empty-text="暂无数据">
              <el-table-column label="#" width="50" align="center">
                <template #default="{ $index }">{{ (adjQuery.pageNum - 1) * adjQuery.pageSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="staffName" label="员工姓名" min-width="100" />
              <el-table-column prop="staffNo" label="员工编号" min-width="110" />
              <el-table-column prop="mobileNo" label="手机号" width="130" />
              <el-table-column prop="departmentName" label="部门" min-width="140" />
              <el-table-column label="变动日期" width="110" align="center">
                <template #default="{ row }">{{ row.dateLabel || '-' }}</template>
              </el-table-column>
              <el-table-column label="员工状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.staffStatus === 'IN_SERVICE' ? 'success' : 'danger'" size="small">
                    {{ row.staffStatus === 'IN_SERVICE' ? '在职' : '离职' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="同步状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="row.syncStatus === 2 && row.errorMessage" :content="row.errorMessage" placement="top">
                  <el-tag :type="syncStatusTag(row.syncStatus)" size="small">{{ syncStatusLabel(row.syncStatus) }}</el-tag>
                </el-tooltip>
                <el-tag v-else :type="syncStatusTag(row.syncStatus)" size="small">{{ syncStatusLabel(row.syncStatus) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="syncTime" label="同步时间" width="170" align="center">
                <template #default="{ row }">{{ row.syncTime || '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.syncStatus !== 'success' && hasPermission('erp:employee:sync')" type="primary" plain size="small" @click="handleAdjSyncOne(row)">同步ERP</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="adjQuery.pageNum" v-model:page-size="adjQuery.pageSize" :total="adjTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next" @size-change="handleAdjSearch" @current-change="loadAdjData" />
          </div>
        </el-card>
      </el-tab-pane>

        <!-- 离职 -->
        <el-tab-pane label="离职" name="leaving">
          <el-card shadow="never" class="search-card">
            <el-form :model="leaveQuery" inline>
              <el-form-item label="员工姓名">
                <el-input v-model="leaveQuery.staffName" placeholder="请输入员工姓名" clearable @keyup.enter="handleLeaveSearch" />
            </el-form-item>
            <el-form-item label="员工编号">
              <el-input v-model="leaveQuery.staffNo" placeholder="请输入员工编号" clearable @keyup.enter="handleLeaveSearch" />
            </el-form-item>
            <el-form-item label="同步状态">
              <el-select v-model="leaveQuery.syncStatus" placeholder="全部" clearable >
                <el-option label="未同步" :value="0" />
                <el-option label="已同步" :value="1" />
                <el-option label="同步失败" :value="2" />
                <el-option label="已跳过" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleLeaveSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetLeaveQuery">重置</el-button>
              <el-button v-if="hasPermission('erp:employee:sync')" type="success" size="small" :icon="Refresh" :loading="ihrSyncLoading" @click="handleSyncIhr">从IHR同步</el-button>
              <el-button v-if="hasPermission('erp:employee:sync')" type="warning" size="small" :icon="Refresh" :loading="erpSyncLoading" @click="handleSyncErp">同步到ERP</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">离职人员</span>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="leaveLoading" :data="leaveData" border stripe empty-text="暂无数据">
              <el-table-column label="#" width="50" align="center">
                <template #default="{ $index }">{{ (leaveQuery.pageNum - 1) * leaveQuery.pageSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="staffName" label="员工姓名" min-width="100" />
              <el-table-column prop="staffNo" label="员工编号" min-width="110" />
              <el-table-column prop="mobileNo" label="手机号" width="130" />
              <el-table-column prop="departmentName" label="部门" min-width="140" />
              <el-table-column label="离职日期" width="110" align="center">
                <template #default="{ row }">{{ row.dateLabel || '-' }}</template>
              </el-table-column>
              <el-table-column label="员工状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.staffStatus === 'IN_SERVICE' ? 'success' : 'danger'" size="small">
                    {{ row.staffStatus === 'IN_SERVICE' ? '在职' : '离职' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="同步状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="row.syncStatus === 2 && row.errorMessage" :content="row.errorMessage" placement="top">
                  <el-tag :type="syncStatusTag(row.syncStatus)" size="small">{{ syncStatusLabel(row.syncStatus) }}</el-tag>
                </el-tooltip>
                <el-tag v-else :type="syncStatusTag(row.syncStatus)" size="small">{{ syncStatusLabel(row.syncStatus) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="syncTime" label="同步时间" width="170" align="center">
                <template #default="{ row }">{{ row.syncTime || '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.syncStatus !== 'success' && hasPermission('erp:employee:sync')" type="primary" plain size="small" @click="handleLeaveSyncOne(row)">同步ERP</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="leaveQuery.pageNum" v-model:page-size="leaveQuery.pageSize" :total="leaveTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next" @size-change="handleLeaveSearch" @current-change="loadLeaveData" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
defineOptions({ name: 'ErpEmployee' })
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useSyncAction } from '@/composables/useSyncAction'
import { getErpEmployeeOnboardingPage, getErpEmployeeUpdatePage, getErpEmployeeLeavingPage, syncErpEmployeeIhr, syncErpEmployee, syncErpEmployeeByType, getErpSyncStatus } from '@/api/erp-employee'
import { getIhrSyncStatus } from '@/api/ihr'
import { syncStatusLabel, syncStatusTag } from '@/utils/syncStatus'
import { Search, RefreshRight, Refresh } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES } from '@/utils/constants'

const { hasPermission } = usePermission()
const activeTab = ref('onboarding')

// ===== 入职 =====
const onboardLoading = ref(false)
const onboardData = ref([])
const onboardTotal = ref(0)
const onboardQuery = reactive({ staffName: '', staffNo: '', syncStatus: undefined, pageNum: 1, pageSize: PAGE_SIZES[0] })

async function loadOnboardData() {
  onboardLoading.value = true
  try { const res = await getErpEmployeeOnboardingPage(onboardQuery); onboardData.value = res.data?.records || []; onboardTotal.value = res.data?.total || 0 } finally { onboardLoading.value = false }
}
function handleOnboardSearch() { onboardQuery.pageNum = 1; loadOnboardData() }
function resetOnboardQuery() { onboardQuery.staffName = ''; onboardQuery.staffNo = ''; onboardQuery.syncStatus = undefined; onboardQuery.pageNum = 1; loadOnboardData() }

// ===== 变动 =====
const adjLoading = ref(false)
const adjData = ref([])
const adjTotal = ref(0)
const adjQuery = reactive({ staffName: '', staffNo: '', syncStatus: undefined, pageNum: 1, pageSize: PAGE_SIZES[0] })

async function loadAdjData() {
  adjLoading.value = true
  try { const res = await getErpEmployeeUpdatePage(adjQuery); adjData.value = res.data?.records || []; adjTotal.value = res.data?.total || 0 } finally { adjLoading.value = false }
}
function handleAdjSearch() { adjQuery.pageNum = 1; loadAdjData() }
function resetAdjQuery() { adjQuery.staffName = ''; adjQuery.staffNo = ''; adjQuery.syncStatus = undefined; adjQuery.pageNum = 1; loadAdjData() }

// ===== 离职 =====
const leaveLoading = ref(false)
const leaveData = ref([])
const leaveTotal = ref(0)
const leaveQuery = reactive({ staffName: '', staffNo: '', syncStatus: undefined, pageNum: 1, pageSize: PAGE_SIZES[0] })

async function loadLeaveData() {
  leaveLoading.value = true
  try { const res = await getErpEmployeeLeavingPage(leaveQuery); leaveData.value = res.data?.records || []; leaveTotal.value = res.data?.total || 0 } finally { leaveLoading.value = false }
}
function handleLeaveSearch() { leaveQuery.pageNum = 1; loadLeaveData() }
function resetLeaveQuery() { leaveQuery.staffName = ''; leaveQuery.staffNo = ''; leaveQuery.syncStatus = undefined; leaveQuery.pageNum = 1; loadLeaveData() }

// ===== 共享同步状态（IHR同步和ERP同步各自是同一个后端任务） =====
function refreshCurrentTab() {
  if (activeTab.value === 'onboarding') loadOnboardData()
  else if (activeTab.value === 'adjustment') loadAdjData()
  else if (activeTab.value === 'leaving') loadLeaveData()
}

const { syncLoading: ihrSyncLoading, handleSync: handleSyncIhr, checkStatus: checkIhrStatus } = useSyncAction(syncErpEmployeeIhr, refreshCurrentTab, '确定从IHR系统同步人员数据？', getIhrSyncStatus)
const { syncLoading: erpSyncLoading, handleSync: handleSyncErp, checkStatus: checkErpStatus } = useSyncAction(syncErpEmployee, refreshCurrentTab, '确定将所有人员同步到ERP？', getErpSyncStatus)

async function handleOnboardSyncOne(row) {
  try { const res = await syncErpEmployeeByType('ONBOARDING', row.employeeId); ElMessage.success(res.data || '同步成功') } catch { /* interceptor showed error */ } finally { loadOnboardData() }
}
async function handleAdjSyncOne(row) {
  try { const res = await syncErpEmployeeByType('UPDATE', row.employeeId); ElMessage.success(res.data || '同步成功') } catch { /* interceptor showed error */ } finally { loadAdjData() }
}
async function handleLeaveSyncOne(row) {
  try { const res = await syncErpEmployeeByType('LEAVING', row.employeeId); ElMessage.success(res.data || '同步成功') } catch { /* interceptor showed error */ } finally { loadLeaveData() }
}

watch(activeTab, (val) => {
  if (val === 'onboarding') loadOnboardData()
  else if (val === 'adjustment') loadAdjData()
  else if (val === 'leaving') loadLeaveData()
})

onMounted(async () => {
  await Promise.all([checkIhrStatus(), checkErpStatus()])
  loadOnboardData()
})
</script>

<style scoped>
.erp-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
</style>
