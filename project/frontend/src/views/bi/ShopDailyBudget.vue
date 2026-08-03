<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 数据列表 -->
      <el-tab-pane label="数据列表" name="data">
        <el-card shadow="never" class="search-card">
          <el-form :model="query" inline>
            <el-form-item label="店仓名称">
              <el-input v-model="query.storeName" placeholder="请输入店仓名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="店仓品牌">
              <el-input v-model="query.brandName" placeholder="请输入店仓品牌" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="预算日期">
              <el-date-picker v-model="budgetDateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:240px" />
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
              <span class="card-header-title">店铺日预算</span>
              <el-button v-if="hasPermission('bi:shop-daily-budget:import')" type="warning" size="small" :icon="Upload" @click="showImportDialog = true">批量导入</el-button>
            </div>
          </template>

          <div class="table-responsive">
            <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
              <el-table-column label="#" width="55" align="center" fixed>
                <template #default="{ $index }">{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="storeName" label="店仓名称" min-width="140" show-overflow-tooltip />
              <el-table-column prop="brandName" label="店仓品牌" min-width="120" show-overflow-tooltip />
              <el-table-column label="预算日期" width="120">
                <template #default="{ row }">{{ formatDate(row.budgetDtm) }}</template>
              </el-table-column>
              <el-table-column prop="monthlyName" label="预算月份" width="100" />
              <el-table-column label="一二级地区" min-width="200">
                <template #default="{ row }">
                  <template v-if="row.regionLevel1">
                    <span>{{ row.regionLevel1 }}</span>
                    <span v-if="row.regionLevel2" class="region-sep">/</span>
                    <span v-if="row.regionLevel2">{{ row.regionLevel2 }}</span>
                  </template>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="渠道类型/定义" min-width="140">
                <template #default="{ row }">
                  <template v-if="row.channelProperty">
                    <span>{{ row.channelProperty }}</span>
                    <span v-if="row.channelDef" class="region-sep">/</span>
                    <span v-if="row.channelDef">{{ row.channelDef }}</span>
                  </template>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="渠道性质/经营类型" min-width="140">
                <template #default="{ row }">
                  <template v-if="row.businessType">
                    <span>{{ row.businessType }}</span>
                    <span v-if="row.businessProperty" class="region-sep">/</span>
                    <span v-if="row.businessProperty">{{ row.businessProperty }}</span>
                  </template>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="salesType" label="销售类型" min-width="100" show-overflow-tooltip />
              <el-table-column label="预算金额" width="130" align="right">
                <template #default="{ row }">{{ formatAmount(row.budgetAmount) }}</template>
              </el-table-column>
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
      </el-tab-pane>

      <!-- 导入日志 -->
      <el-tab-pane label="导入日志" name="log">
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">导入日志</span>
              <el-button size="small" :icon="RefreshRight" @click="loadLogData">刷新</el-button>
            </div>
          </template>

          <div class="table-responsive">
            <el-table v-loading="logLoading" :data="logData" border stripe empty-text="暂无导入记录">
              <el-table-column label="#" width="60" align="center">
                <template #default="{ $index }">{{ (logQuery.pageNum - 1) * logQuery.pageSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="fileName" label="文件名" min-width="160" show-overflow-tooltip />
              <el-table-column label="文件大小" width="110" align="right">
                <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column prop="totalCount" label="总行数" width="90" align="right" />
              <el-table-column prop="successCount" label="成功" width="80" align="right">
                <template #default="{ row }"><span style="color:var(--el-color-success)">{{ row.successCount }}</span></template>
              </el-table-column>
              <el-table-column prop="failCount" label="失败" width="80" align="right">
                <template #default="{ row }"><span :style="{color: row.failCount > 0 ? 'var(--el-color-danger)' : ''}">{{ row.failCount }}</span></template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createBy" label="操作人" width="130" show-overflow-tooltip />
              <el-table-column label="导入时间" width="180">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="110" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.errorMsg" link type="primary" size="small" @click="viewErrors(row)">查看错误</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="logQuery.pageNum"
              v-model:page-size="logQuery.pageSize"
              :total="logTotal"
              :page-sizes="PAGE_SIZES"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadLogData"
              @current-change="loadLogData"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="showImportDialog" title="批量导入店铺日预算" width="520px" destroy-on-close @open="resetImportState">
      <div class="import-zone">
        <el-upload :limit="1" accept=".xlsx,.xls" :auto-upload="false" :before-upload="beforeUpload" drag :on-change="handleFileChange" :on-remove="handleFileRemove" :on-exceed="() => ElMessage.warning('只能上传一个文件')">
          <el-icon :size="40" style="color:var(--el-text-color-placeholder)"><Upload /></el-icon>
          <div style="margin-top:8px">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-hint">仅支持 .xlsx / .xls 格式，文件大小不超过 100MB，店铺+品牌+预算日期重复时自动更新</div>
          </template>
        </el-upload>
        <div style="margin-top:12px;text-align:center">
          <el-button link type="primary" :loading="templateLoading" @click="handleDownloadTemplate">下载导入模板</el-button>
        </div>
      </div>

      <div v-if="importResult" style="margin-top:16px">
        <el-alert
          :title="`导入完成：共 ${importResult.total} 条，成功 ${importResult.success} 条，失败 ${importResult.fail} 条`"
          :type="importResult.fail === 0 ? 'success' : (importResult.success === 0 ? 'error' : 'warning')"
          show-icon
          :closable="false"
          style="margin-bottom:8px"
        />
        <div v-if="importResult.errors?.length" style="max-height:240px;overflow-y:auto;border:1px solid var(--el-border-color-lighter);border-radius:6px;padding:8px 12px;background:var(--el-fill-color-lighter)">
          <div v-for="(err, idx) in importResult.errors" :key="idx" style="font-size:12px;color:var(--el-color-danger);line-height:2;border-bottom:1px dashed var(--el-border-color-extra-light)">
            {{ err }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showImportDialog = false">关闭</el-button>
        <el-button type="primary" :loading="importLoading" :disabled="importLoading" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 错误详情弹窗 -->
    <el-dialog v-model="errorDialogVisible" title="导入错误详情" width="600px">
      <div style="max-height:420px;overflow-y:auto;border:1px solid var(--el-border-color-lighter);border-radius:6px;padding:8px 12px;background:var(--el-fill-color-lighter)">
        <pre style="white-space:pre-wrap;font-size:12px;color:var(--el-color-danger);line-height:1.8;margin:0">{{ errorDialogContent }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'ShopDailyBudget' })
import { ref, reactive, onMounted, onActivated } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, Upload } from '@element-plus/icons-vue'
import { getShopDailyBudgetPage, importShopDailyBudget, getShopDailyBudgetTemplate, getShopDailyBudgetImportLogPage } from '@/api/bi'
import { formatTime } from '@/utils/format'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES, defaultPageSize } from '@/utils/appConfig'

const { hasPermission } = usePermission()

// ===== Tab =====
const activeTab = ref('data')
function onTabChange(tab) {
  if (tab === 'log') loadLogData()
}

// ===== 数据列表 =====
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: defaultPageSize.value, storeName: '', brandName: '', budgetDtmStart: '', budgetDtmEnd: '' })
const budgetDateRange = ref(null)

async function loadData() {
  // 同步日期范围
  if (budgetDateRange.value && budgetDateRange.value.length === 2) {
    query.budgetDtmStart = budgetDateRange.value[0]
    query.budgetDtmEnd = budgetDateRange.value[1]
  } else {
    query.budgetDtmStart = ''
    query.budgetDtmEnd = ''
  }
  loading.value = true
  try {
    const res = await getShopDailyBudgetPage(query)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}
function handleSearch() { query.pageNum = 1; loadData() }
function resetQuery() {
  query.storeName = ''; query.brandName = ''
  budgetDateRange.value = null
  handleSearch()
}

function formatDate(d) {
  if (!d) return '-'
  return String(d).substring(0, 10)
}
function formatAmount(n) {
  if (n === null || n === undefined) return '-'
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 6 })
}

// ===== 导入日志 =====
const logLoading = ref(false)
const logData = ref([])
const logTotal = ref(0)
const logQuery = reactive({ pageNum: 1, pageSize: defaultPageSize.value })

async function loadLogData() {
  logLoading.value = true
  try {
    const res = await getShopDailyBudgetImportLogPage({ pageNum: logQuery.pageNum, pageSize: logQuery.pageSize })
    logData.value = res.data?.records || []
    logTotal.value = res.data?.total || 0
  } finally {
    logLoading.value = false
  }
}

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}
function statusLabel(s) {
  return { SUCCESS: '全部成功', PARTIAL: '部分失败', FAILED: '失败' }[s] || s
}
function statusTagType(s) {
  return { SUCCESS: 'success', PARTIAL: 'warning', FAILED: 'danger' }[s] || 'info'
}

// 错误详情弹窗
const errorDialogVisible = ref(false)
const errorDialogContent = ref('')
function viewErrors(row) {
  errorDialogContent.value = row.errorMsg || ''
  errorDialogVisible.value = true
}

// ===== 导入 =====
const showImportDialog = ref(false)
const importLoading = ref(false)
const templateLoading = ref(false)
const importResult = ref(null)
const selectedFile = ref(null)

const MAX_FILE_SIZE = 100 * 1024 * 1024

function beforeUpload(file) {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  if (!isExcel) {
    ElMessage.error('仅支持 .xlsx / .xls 格式的 Excel 文件')
    return false
  }
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error(`文件大小不能超过 100MB（当前 ${(file.size / 1024 / 1024).toFixed(1)}MB）`)
    return false
  }
  return true
}

function handleFileChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  if (!raw.name.endsWith('.xlsx') && !raw.name.endsWith('.xls')) {
    ElMessage.error('仅支持 .xlsx / .xls 格式的 Excel 文件')
    return
  }
  if (raw.size > MAX_FILE_SIZE) {
    ElMessage.error(`文件大小不能超过 100MB（当前 ${(raw.size / 1024 / 1024).toFixed(1)}MB）`)
    return
  }
  selectedFile.value = raw
  importResult.value = null
}
function handleFileRemove() { selectedFile.value = null }
function resetImportState() { selectedFile.value = null; importResult.value = null; importLoading.value = false }

async function handleDownloadTemplate() {
  templateLoading.value = true
  try {
    const res = await getShopDailyBudgetTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '店铺日预算导入模板.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载模板失败')
  } finally {
    templateLoading.value = false
  }
}

async function submitImport() {
  if (!selectedFile.value) { ElMessage.warning('请先选择 Excel 文件'); return }
  if (selectedFile.value.size > MAX_FILE_SIZE) {
    ElMessage.error('文件过大，请重新选择')
    return
  }
  importLoading.value = true
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await importShopDailyBudget(formData)
    importResult.value = res.data
    if (res.data?.success > 0) {
      ElMessage.success('导入完成')
      loadData()
      if (activeTab.value === 'log') loadLogData()
    }
  } catch (e) {
    ElMessage.error('导入失败：' + (e.message || '服务器错误'))
  } finally {
    importLoading.value = false
  }
}

onMounted(() => loadData())
onActivated(() => loadData())
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
.region-sep {
  color: var(--el-text-color-placeholder);
  margin: 0 2px;
}
.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.pagination-wrapper {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
