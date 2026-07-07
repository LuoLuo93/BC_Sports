<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="nxcrm-tag-tabs">
      <!-- 任务列表 -->
      <el-tab-pane label="任务列表" name="tasks">
        <el-card shadow="never" class="search-card">
          <el-form :model="taskQuery" inline>
            <el-form-item>
              <el-button type="primary" :icon="Plus" @click="showCreateDialog">创建任务</el-button>
              <el-button :icon="RefreshRight" @click="loadTasks">刷新</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">打标签任务</span>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="taskLoading" :data="taskData" border stripe @row-click="selectTask">
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="outShopId" label="店铺ID" min-width="100" show-overflow-tooltip />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalCount" label="总数" width="70" align="center" />
              <el-table-column prop="successCount" label="成功" width="70" align="center" />
              <el-table-column prop="failCount" label="失败" width="70" align="center" />
              <el-table-column label="创建时间" width="170" align="center">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.status === 0" type="primary" plain size="small" :loading="executingTaskId === row.id" @click.stop="handleExecute(row)">执行</el-button>
                  <el-button type="info" plain size="small" @click.stop="viewDetails(row)">明细</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="taskQuery.pageNum" v-model:page-size="taskQuery.pageSize" :total="taskTotal" :page-sizes="PAGE_SIZES_LG" layout="total, sizes, prev, pager, next" @size-change="loadTasks" @current-change="loadTasks" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 任务明细 -->
      <el-tab-pane label="任务明细" name="details" :disabled="!selectedTask" lazy>
        <el-card v-if="selectedTask" shadow="never" class="search-card">
          <el-form :inline="true">
            <el-form-item label="任务名称">
              <span>{{ selectedTask.taskName }}</span>
            </el-form-item>
            <el-form-item label="状态">
              <el-tag :type="statusTagType(selectedTask.status)" size="small">{{ statusLabel(selectedTask.status) }}</el-tag>
            </el-form-item>
            <el-form-item label="成功/失败">
              <span>{{ selectedTask.successCount }} / {{ selectedTask.failCount }}</span>
            </el-form-item>
            <el-form-item label="筛选状态">
              <el-select v-model="detailQuery.status" placeholder="全部" clearable style="min-width:100px;max-width:120px">
                <el-option label="待处理" :value="0" />
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="loadDetails">搜索</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card v-if="selectedTask" shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">任务明细</span>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="detailLoading" :data="detailData" border stripe>
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="memberId" label="会员ID" min-width="120" />
              <el-table-column prop="tagDataJson" label="标签数据" min-width="200" show-overflow-tooltip />
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'" size="small">
                    {{ row.status === 1 ? '成功' : row.status === 2 ? '失败' : '待处理' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="errorMsg" label="错误信息" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">{{ row.errorMsg || '-' }}</template>
              </el-table-column>
              <el-table-column label="更新时间" width="170" align="center">
                <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="detailQuery.pageNum" v-model:page-size="detailQuery.pageSize" :total="detailTotal" :page-sizes="PAGE_SIZES_LG" layout="total, sizes, prev, pager, next" @size-change="loadDetails" @current-change="loadDetails" />
          </div>
        </el-card>
      </el-tab-pane>

        <!-- 会员标签数据 -->
      <el-tab-pane label="会员标签数据" name="memberTags" lazy>
        <el-card shadow="never" class="search-card">
          <el-form :inline="true">
            <el-form-item label="批次号">
              <el-input v-model="memberTagQuery.batchNo" placeholder="请输入批次号" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="memberTagQuery.status" placeholder="全部" clearable style="min-width:100px;max-width:120px">
                <el-option label="待处理" :value="0" />
                <el-option label="shopId已填充" :value="1" />
                <el-option label="已打标签" :value="2" />
                <el-option label="失败" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleMemberTagSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetMemberTagQuery">重置</el-button>
              <el-button type="warning" @click="handleFillShopId" :loading="fillShopLoading">填充shopId</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">会员标签数据</span>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="memberTagLoading" :data="memberTagData" border stripe>
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="nasOuid" label="nasOuid" min-width="140" show-overflow-tooltip />
              <el-table-column prop="tagCode" label="标签编码" min-width="100" show-overflow-tooltip />
              <el-table-column prop="tagValueCode" label="标签值编码" min-width="100" show-overflow-tooltip />
              <el-table-column prop="tagValue" label="标签值" min-width="100" show-overflow-tooltip />
              <el-table-column prop="shopId" label="shopId" min-width="100" />
              <el-table-column prop="shopName" label="店铺名称" min-width="120" show-overflow-tooltip />
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="memberStatusType(row.status)" size="small">{{ memberStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="batchNo" label="批次号" min-width="140" show-overflow-tooltip />
            </el-table>
          </div>
          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="memberTagQuery.pageNum" v-model:page-size="memberTagQuery.pageSize" :total="memberTagTotal" :page-sizes="PAGE_SIZES_LG" layout="total, sizes, prev, pager, next" @size-change="loadMemberTags" @current-change="loadMemberTags" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建任务对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建打标签任务" width="500px" destroy-on-close>
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="任务名称">
          <el-input v-model="createForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="店铺ID">
          <el-input v-model="createForm.outShopId" placeholder="请输入外部店铺ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="createDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="createSubmitting" @click="handleCreate">创建</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'NxcrmCustomerTag' })
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { PAGE_SIZES_LG } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import { getNxcrmTagTasks, createNxcrmTagTask, executeNxcrmTagTask, getNxcrmTaskDetails, getNxcrmMemberTags, fillNxcrmShopId } from '@/api/nxcrm'

const activeTab = ref('tasks')

function statusLabel(s) { return ['待处理', '处理中', '已完成', '失败'][s] || '未知' }
function statusTagType(s) { return ['info', 'warning', 'success', 'danger'][s] || 'info' }

// ===== 任务列表 =====
const taskLoading = ref(false)
const taskData = ref([])
const taskTotal = ref(0)
const taskQuery = reactive({ pageNum: 1, pageSize: 20 })
const executingTaskId = ref(null)

async function loadTasks() {
  taskLoading.value = true
  try {
    const res = await getNxcrmTagTasks(taskQuery)
    taskData.value = res.data?.records || []
    taskTotal.value = res.data?.total || 0
  } finally { taskLoading.value = false }
}

async function handleExecute(row) {
  executingTaskId.value = row.id
  try {
    await executeNxcrmTagTask(row.id)
    ElMessage.success('任务已触发')
    startStatusPolling(row.id)
  } finally { executingTaskId.value = null }
}

let statusTimer = null
function startStatusPolling(taskId) {
  if (statusTimer) clearInterval(statusTimer)
  statusTimer = setInterval(async () => {
    try {
      const res = await getNxcrmTaskStatus(taskId)
      if (res.data?.status !== 1) {
        clearInterval(statusTimer)
        statusTimer = null
        loadTasks()
      }
    } catch { /* keep polling */ }
  }, 3000)
}

// ===== 任务明细 =====
const selectedTask = ref(null)
const detailLoading = ref(false)
const detailData = ref([])
const detailTotal = ref(0)
const detailQuery = reactive({ pageNum: 1, pageSize: 20, status: null })

function selectTask(row) { selectedTask.value = row }
function viewDetails(row) {
  selectedTask.value = row
  detailQuery.pageNum = 1
  detailQuery.status = null
  activeTab.value = 'details'
}

async function loadDetails() {
  if (!selectedTask.value) return
  detailLoading.value = true
  try {
    const res = await getNxcrmTaskDetails(selectedTask.value.id, detailQuery)
    detailData.value = res.data?.records || []
    detailTotal.value = res.data?.total || 0
  } finally { detailLoading.value = false }
}

// ===== 创建任务 =====
const createDialogVisible = ref(false)
const createSubmitting = ref(false)
const createForm = reactive({ taskName: '', outShopId: '' })

function showCreateDialog() {
  createForm.taskName = ''
  createForm.outShopId = ''
  createDialogVisible.value = true
}

async function handleCreate() {
  if (!createForm.taskName.trim()) { ElMessage.warning('请输入任务名称'); return }
  createSubmitting.value = true
  try {
    await createNxcrmTagTask(createForm)
    ElMessage.success('任务创建成功')
    createDialogVisible.value = false
    loadTasks()
  } catch { /* interceptor handles */ } finally { createSubmitting.value = false }
}

// ===== 会员标签数据 =====
const memberTagLoading = ref(false)
const memberTagData = ref([])
const memberTagTotal = ref(0)
const memberTagQuery = reactive({ batchNo: '', status: null, pageNum: 1, pageSize: 20 })
const fillShopLoading = ref(false)

function memberStatusLabel(s) { return ['待处理', 'shopId已填充', '已打标签', '失败'][s] || '未知' }
function memberStatusType(s) { return ['info', 'primary', 'success', 'danger'][s] || 'info' }

async function loadMemberTags() {
  memberTagLoading.value = true
  try {
    const res = await getNxcrmMemberTags(memberTagQuery)
    memberTagData.value = res.data?.records || []
    memberTagTotal.value = res.data?.total || 0
  } finally { memberTagLoading.value = false }
}

function handleMemberTagSearch() { memberTagQuery.pageNum = 1; loadMemberTags() }
function resetMemberTagQuery() { memberTagQuery.batchNo = ''; memberTagQuery.status = null; memberTagQuery.pageNum = 1; loadMemberTags() }

async function handleFillShopId() {
  if (!memberTagQuery.batchNo) { ElMessage.warning('请先输入批次号'); return }
  fillShopLoading.value = true
  try {
    await fillNxcrmShopId(memberTagQuery.batchNo)
    ElMessage.success('shopId填充完成')
    loadMemberTags()
  } catch { /* interceptor handles */ } finally { fillShopLoading.value = false }
}

watch(activeTab, (val) => {
  if (val === 'tasks') loadTasks()
  else if (val === 'details' && selectedTask.value) loadDetails()
  else if (val === 'memberTags') loadMemberTags()
})

loadTasks()
</script>

<style scoped>
.nxcrm-tag-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
</style>
