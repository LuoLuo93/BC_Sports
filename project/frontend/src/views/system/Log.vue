<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="[new Date(0,0,0,0,0,0), new Date(0,0,0,23,59,59)]"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">操作日志</span>
          <div class="header-actions">
            <el-button v-if="hasPermission('system:log:remove')" type="danger" size="small" @click="handleClean">清理日志</el-button>
          </div>
        </div>
      </template>

      <div class="table-responsive">
      <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无日志">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="module" label="模块" width="120" align="center" />
        <el-table-column prop="operation" label="操作" width="140" />
        <el-table-column prop="username" label="操作人" width="100" align="center" />
        <el-table-column prop="ip" label="IP" width="130" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="方法" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.operationTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain size="small" @click="handleDetail(row)">详情</el-button>
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
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="600px">
      <el-descriptions :column="1" border v-if="currentLog">
        <el-descriptions-item label="模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.operation }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="方法">{{ currentLog.method }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatTime(currentLog.operationTime) }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" v-if="currentLog.params">
          <pre class="log-params">{{ currentLog.params }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentLog.errorMsg">
          <span class="log-error">{{ currentLog.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'LogManagement' })
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getLogPage, cleanLogs } from '@/api/log'
import { usePermission } from '@/composables/usePermission'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const { hasPermission } = usePermission()
const tableData = ref([])
const total = ref(0)
const dateRange = ref(null)
const detailVisible = ref(false)
const currentLog = ref(null)

const modules = ['用户管理', '角色管理', '菜单管理', '部门管理', '字典管理', '实体渠道', '系统认证']

const query = ref({
  pageNum: 1,
  pageSize: 20,
  module: '',
  username: '',
  status: null,
  startTime: null,
  endTime: null
})

function buildParams() {
  const params = { ...query.value }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  } else {
    params.startTime = null
    params.endTime = null
  }
  return params
}

async function loadData() {
  loading.value = true
  try {
    const params = buildParams()
    const res = await getLogPage(params)
    if (res.code === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载日志失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.value.pageNum = 1
  loadData()
}

function handleReset() {
  query.value = { pageNum: 1, pageSize: 20, module: '', username: '', status: null, startTime: null, endTime: null }
  dateRange.value = null
  loadData()
}

function handleDetail(row) {
  currentLog.value = row
  detailVisible.value = true
}

async function handleClean() {
  try {
    const { value } = await ElMessageBox.prompt('请输入保留最近多少天的日志', '清理日志', {
      confirmButtonText: '确定清理',
      cancelButtonText: '取消',
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入正整数',
      inputValue: '30'
    })
    const res = await cleanLogs(parseInt(value))
    if (res.code === 200) {
      ElMessage.success(res.message || '清理完成')
      loadData()
    } else {
      ElMessage.error(res.message || '清理失败')
    }
  } catch {
    // cancelled
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.log-params {
  max-height: 200px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  margin: 0;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
}
.log-error {
  color: #f56c6c;
  font-size: 13px;
}
</style>
