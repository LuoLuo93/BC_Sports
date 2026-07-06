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
              <el-select v-model="onboardQuery.syncStatus" placeholder="全部" clearable>
                <el-option label="未同步" :value="0" />
                <el-option label="已同步" :value="1" />
                <el-option label="同步失败" :value="2" />
                <el-option label="已跳过" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleOnboardSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetOnboardQuery">重置</el-button>
              <el-button v-if="hasPermission('hk:personnel:sync')" type="warning" size="small" :icon="Refresh" :loading="onboardingSyncLoading" :disabled="isAnySyncing" @click="handleSyncOnboarding">入职同步</el-button>
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
              <el-table-column prop="syncTime" label="同步时间" width="180" align="center">
                <template #default="{ row }">{{ formatTime(row.syncTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.syncStatus !== 1 && hasPermission('hk:personnel:sync')" type="primary" plain size="small" @click="handleOnboardSyncOne(row)">同步ERP</el-button>
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
      <el-tab-pane label="变动" name="update" lazy>
        <el-card shadow="never" class="search-card">
          <el-form :model="updateQuery" inline>
            <el-form-item label="员工姓名">
              <el-input v-model="updateQuery.staffName" placeholder="请输入员工姓名" clearable @keyup.enter="handleUpdateSearch" />
            </el-form-item>
            <el-form-item label="员工编号">
              <el-input v-model="updateQuery.staffNo" placeholder="请输入员工编号" clearable @keyup.enter="handleUpdateSearch" />
            </el-form-item>
            <el-form-item label="同步状态">
              <el-select v-model="updateQuery.syncStatus" placeholder="全部" clearable>
                <el-option label="未同步" :value="0" />
                <el-option label="已同步" :value="1" />
                <el-option label="同步失败" :value="2" />
                <el-option label="已跳过" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleUpdateSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetUpdateQuery">重置</el-button>
              <el-button v-if="hasPermission('hk:personnel:sync')" type="warning" size="small" :icon="Refresh" :loading="updateSyncLoading" :disabled="isAnySyncing" @click="handleSyncUpdate">变更离职同步</el-button>
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
            <el-table v-loading="updateLoading" :data="updateData" border stripe empty-text="暂无数据">
              <el-table-column label="#" width="50" align="center">
                <template #default="{ $index }">{{ (updateQuery.pageNum - 1) * updateQuery.pageSize + $index + 1 }}</template>
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
              <el-table-column prop="syncTime" label="同步时间" width="180" align="center">
                <template #default="{ row }">{{ formatTime(row.syncTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.syncStatus !== 1 && hasPermission('hk:personnel:sync')" type="primary" plain size="small" @click="handleUpdateSyncOne(row)">同步ERP</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="updateQuery.pageNum" v-model:page-size="updateQuery.pageSize" :total="updateTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next" @size-change="handleUpdateSearch" @current-change="loadUpdateData" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 离职 -->
      <el-tab-pane label="离职" name="leaving" lazy>
        <el-card shadow="never" class="search-card">
          <el-form :model="leaveQuery" inline>
            <el-form-item label="员工姓名">
              <el-input v-model="leaveQuery.staffName" placeholder="请输入员工姓名" clearable @keyup.enter="handleLeaveSearch" />
            </el-form-item>
            <el-form-item label="员工编号">
              <el-input v-model="leaveQuery.staffNo" placeholder="请输入员工编号" clearable @keyup.enter="handleLeaveSearch" />
            </el-form-item>
            <el-form-item label="同步状态">
              <el-select v-model="leaveQuery.syncStatus" placeholder="全部" clearable>
                <el-option label="未同步" :value="0" />
                <el-option label="已同步" :value="1" />
                <el-option label="同步失败" :value="2" />
                <el-option label="已跳过" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleLeaveSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetLeaveQuery">重置</el-button>
              <el-button v-if="hasPermission('hk:personnel:sync')" type="warning" size="small" :icon="Refresh" :loading="updateSyncLoading" :disabled="isAnySyncing" @click="handleSyncUpdate">变更离职同步</el-button>
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
              <el-table-column prop="syncTime" label="同步时间" width="180" align="center">
                <template #default="{ row }">{{ formatTime(row.syncTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.syncStatus !== 1 && hasPermission('hk:personnel:sync')" type="primary" plain size="small" @click="handleLeaveSyncOne(row)">同步ERP</el-button>
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
defineOptions({ name: 'HkPersonnelSync' })
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getHkOnboardingPage, getHkUpdatePage, getHkLeavingPage, syncHkOnboarding, syncHkUpdate, syncHkByType, getHkSyncStatus } from '@/api/hk-personnel'
import { syncStatusLabel, syncStatusTag } from '@/utils/syncStatus'
import { Search, RefreshRight, Refresh } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES, defaultPageSize } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'

const { hasPermission } = usePermission()
const activeTab = ref('onboarding')

// ===== 入职 =====
const onboardLoading = ref(false)
const onboardData = ref([])
const onboardTotal = ref(0)
const onboardQuery = reactive({ staffName: '', staffNo: '', syncStatus: undefined, pageNum: 1, pageSize: defaultPageSize.value })

async function loadOnboardData() {
  onboardLoading.value = true
  try { const res = await getHkOnboardingPage(onboardQuery); onboardData.value = res.data?.records || []; onboardTotal.value = res.data?.total || 0 } finally { onboardLoading.value = false }
}
function handleOnboardSearch() { onboardQuery.pageNum = 1; loadOnboardData() }
function resetOnboardQuery() { onboardQuery.staffName = ''; onboardQuery.staffNo = ''; onboardQuery.syncStatus = undefined; onboardQuery.pageNum = 1; loadOnboardData() }

// ===== 变动 =====
const updateLoading = ref(false)
const updateData = ref([])
const updateTotal = ref(0)
const updateQuery = reactive({ staffName: '', staffNo: '', syncStatus: undefined, pageNum: 1, pageSize: defaultPageSize.value })

async function loadUpdateData() {
  updateLoading.value = true
  try { const res = await getHkUpdatePage(updateQuery); updateData.value = res.data?.records || []; updateTotal.value = res.data?.total || 0 } finally { updateLoading.value = false }
}
function handleUpdateSearch() { updateQuery.pageNum = 1; loadUpdateData() }
function resetUpdateQuery() { updateQuery.staffName = ''; updateQuery.staffNo = ''; updateQuery.syncStatus = undefined; updateQuery.pageNum = 1; loadUpdateData() }

// ===== 离职 =====
const leaveLoading = ref(false)
const leaveData = ref([])
const leaveTotal = ref(0)
const leaveQuery = reactive({ staffName: '', staffNo: '', syncStatus: undefined, pageNum: 1, pageSize: defaultPageSize.value })

async function loadLeaveData() {
  leaveLoading.value = true
  try { const res = await getHkLeavingPage(leaveQuery); leaveData.value = res.data?.records || []; leaveTotal.value = res.data?.total || 0 } finally { leaveLoading.value = false }
}
function handleLeaveSearch() { leaveQuery.pageNum = 1; loadLeaveData() }
function resetLeaveQuery() { leaveQuery.staffName = ''; leaveQuery.staffNo = ''; leaveQuery.syncStatus = undefined; leaveQuery.pageNum = 1; loadLeaveData() }

// ===== 同步动作（手动触发 + 轮询状态 + 完成后刷新） =====
function refreshCurrentTab() {
  if (activeTab.value === 'onboarding') loadOnboardData()
  else if (activeTab.value === 'update') loadUpdateData()
  else if (activeTab.value === 'leaving') loadLeaveData()
}

const onboardingSyncLoading = ref(false)
const updateSyncLoading = ref(false)
const backgroundSyncing = ref(false)  // 定时任务触发的 syncAll 进行中状态
const isAnySyncing = computed(() => onboardingSyncLoading.value || updateSyncLoading.value || backgroundSyncing.value)
let pollTimer = null

async function pollStatus() {
  try {
    const res = await getHkSyncStatus()
    const d = res.data || {}
    const running = d.lifecycleSyncing || d.onboardingSyncing || d.updateSyncing
    if (backgroundSyncing.value && !running) {
      refreshCurrentTab()
      stopPolling()
    }
    backgroundSyncing.value = !!running
  } catch { /* 静默 */ }
}
function startPolling() {
  stopPolling()
  pollTimer = setInterval(pollStatus, 3000)
}
function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

// ===== 单条同步（操作列按钮） =====
async function handleOnboardSyncOne(row) {
  try { const res = await syncHkByType('HK_ONBOARDING', row.employeeId); ElMessage.success(res.data || '同步成功') } catch { /* 拦截器已弹错 */ } finally { loadOnboardData() }
}
async function handleUpdateSyncOne(row) {
  try { const res = await syncHkByType('HK_UPDATE', row.employeeId); ElMessage.success(res.data || '同步成功') } catch { /* 拦截器已弹错 */ } finally { loadUpdateData() }
}
async function handleLeaveSyncOne(row) {
  try { const res = await syncHkByType('HK_LEAVING', row.employeeId); ElMessage.success(res.data || '同步成功') } catch { /* 拦截器已弹错 */ } finally { loadLeaveData() }
}

async function handleSyncOnboarding() {
  onboardingSyncLoading.value = true
  try {
    await syncHkOnboarding()
    ElMessage.success('入职同步已触发')
    pollStatus._on = true
    startPolling()
  } catch { /* 拦截器已弹错 */ } finally { onboardingSyncLoading.value = false }
}

async function handleSyncUpdate() {
  updateSyncLoading.value = true
  try {
    await syncHkUpdate()
    ElMessage.success('变更离职同步已触发')
    pollStatus._up = true
    startPolling()
  } catch { /* 拦截器已弹错 */ } finally { updateSyncLoading.value = false }
}

watch(activeTab, (val) => {
  if (val === 'onboarding') loadOnboardData()
  else if (val === 'update') loadUpdateData()
  else if (val === 'leaving') loadLeaveData()
})

onMounted(() => {
  loadOnboardData()
})
</script>

<style scoped>
.erp-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
</style>
