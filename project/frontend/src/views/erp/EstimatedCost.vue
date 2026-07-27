<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="cost-tabs" @tab-change="onTabChange">
    <!-- 数据列表 -->
    <el-tab-pane label="数据列表" name="data">
        <el-card shadow="never" class="search-card">
          <el-form inline>
            <el-form-item label="货号">
              <el-input v-model="query.materialNumber" placeholder="请输入货号" clearable style="min-width:150px;max-width:200px" @keyup.enter="onSearch" />
            </el-form-item>
            <el-form-item label="款号">
              <el-input v-model="query.styleNumber" placeholder="请输入款号" clearable style="min-width:130px;max-width:170px" @keyup.enter="onSearch" />
            </el-form-item>
            <el-form-item label="货品名称">
              <el-input v-model="query.materialName" placeholder="请输入货品名称" clearable style="min-width:150px;max-width:200px" @keyup.enter="onSearch" />
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
              <span class="card-header-title">预估成本管理</span>
              <div class="header-actions">
                <el-button v-if="hasPermission('erp:estimatedCost:import')" type="warning" size="small" :icon="Upload" @click="showImportDialog = true">批量导入</el-button>
              </div>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="loading" :data="tableData" border size="small" height="100%" :empty-text="hasSearched ? '暂无数据' : '请输入查询条件后点击搜索'">
              <el-table-column label="#" width="50" fixed="left">
                <template #default="{ $index }">{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="MATERIAL_NUMBER" label="货号" width="180" show-overflow-tooltip fixed="left" />
              <el-table-column prop="MATERIAL_NAME" label="货品名称" min-width="200" show-overflow-tooltip />
              <el-table-column prop="STYLE_NUMBER" label="款号" width="160" show-overflow-tooltip />
              <el-table-column label="预估成本" width="140" align="right">
                <template #default="{ row }">
                  <span v-if="row.PRECOST != null && row.PRECOST !== ''">{{ formatCost(row.PRECOST) }}</span>
                  <span v-else style="color:#d9d9d9">-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="hasPermission('erp:estimatedCost:edit')" type="primary" plain size="small" @click="openEdit(row)">编辑</el-button>
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
      </el-tab-pane>

      <!-- 导入记录 -->
      <el-tab-pane label="导入记录" name="log" lazy>
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">导入记录</span>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="logLoading" :data="logData" border size="small" height="100%" empty-text="暂无导入记录">
              <el-table-column label="#" width="50">
                <template #default="{ $index }">{{ (logQuery.pageNum - 1) * logQuery.pageSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
              <el-table-column label="文件大小" width="100" align="center">
                <template #default="{ row }">{{ row.fileSize ? (row.fileSize / 1024).toFixed(1) + ' KB' : '-' }}</template>
              </el-table-column>
              <el-table-column prop="totalCount" label="总行数" width="90" align="center" />
              <el-table-column prop="successCount" label="成功" width="80" align="center">
                <template #default="{ row }"><span style="color:#67c23a">{{ row.successCount }}</span></template>
              </el-table-column>
              <el-table-column prop="failCount" label="失败" width="80" align="center">
                <template #default="{ row }"><span :style="{ color: row.failCount > 0 ? '#f56c6c' : '' }">{{ row.failCount }}</span></template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createBy" label="操作人" width="100" />
              <el-table-column label="导入时间" width="170" align="center">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.errorMsg" type="primary" link size="small" @click="showError(row)">查看错误</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-wrapper--sm">
            <el-pagination
              v-model:current-page="logQuery.pageNum"
              v-model:page-size="logQuery.pageSize"
              :total="logTotal"
              :page-sizes="PAGE_SIZES"
              layout="total, sizes, prev, pager, next"
              @size-change="() => { logQuery.pageNum = 1; loadLogData() }"
              @current-change="loadLogData"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑预估成本" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="货号">
          <span>{{ editRow.MATERIAL_NUMBER }}</span>
        </el-form-item>
        <el-form-item label="货品名称">
          <span>{{ editRow.MATERIAL_NAME || '-' }}</span>
        </el-form-item>
        <el-form-item label="款号">
          <span>{{ editRow.STYLE_NUMBER || '-' }}</span>
        </el-form-item>
        <el-form-item label="预估成本">
          <el-input v-model="editForm.precost" placeholder="请输入预估成本" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog v-model="showImportDialog" title="批量导入预估成本" width="520px" destroy-on-close @close="resetImportState">
      <div class="import-zone">
        <el-upload :limit="1" accept=".xlsx,.xls" :auto-upload="false" drag :on-change="handleFileChange" :on-remove="handleFileRemove" :on-exceed="() => ElMessage.warning('只能上传一个文件')">
          <el-icon :size="40" style="color:var(--el-text-color-placeholder)"><Upload /></el-icon>
          <div style="margin-top:8px">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-hint">仅支持 .xlsx / .xls 格式，按货号更新预估成本</div>
          </template>
        </el-upload>
        <div style="margin-top:12px;text-align:center">
          <el-button link type="primary" :loading="templateLoading" @click="handleDownloadTemplate">下载导入模板</el-button>
        </div>
      </div>

      <div v-if="importResult" style="margin-top:16px">
        <el-alert
          :title="`导入完成：共 ${importResult.total} 条，成功 ${importResult.success} 条，失败 ${importResult.fail} 条`"
          :type="importResult.fail > 0 ? 'warning' : 'success'"
          :closable="false"
          show-icon
        />
        <div v-if="importResult.errors?.length" style="margin-top:8px;max-height:120px;overflow-y:auto">
          <div v-for="(err, i) in importResult.errors" :key="i" style="font-size:12px;color:#f56c6c;padding:2px 0">{{ err }}</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showImportDialog = false">关闭</el-button>
        <el-button type="primary" :loading="importLoading" :disabled="importLoading" @click="handleImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 错误详情弹窗 -->
    <el-dialog v-model="errorDialogVisible" title="导入错误详情" width="640px">
      <pre class="error-pre">{{ errorDialogContent }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'ErpEstimatedCost' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, Upload } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES, defaultPageSize } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import { getEstimatedCostPage, updateEstimatedCost, importEstimatedCost, getEstimatedCostTemplate, getEstimatedCostImportLogPage } from '@/api/erp-estimated-cost'

const { hasPermission } = usePermission()
const activeTab = ref('data')

// ===== 数据列表 =====
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const hasSearched = ref(false)
const query = reactive({ materialNumber: '', styleNumber: '', materialName: '', pageNum: 1, pageSize: defaultPageSize.value })

async function loadData() {
  loading.value = true
  try {
    const res = await getEstimatedCostPage(query)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}
function onSearch() {
  hasSearched.value = true
  query.pageNum = 1
  loadData()
}
function onReset() {
  query.materialNumber = ''
  query.styleNumber = ''
  query.materialName = ''
  query.pageNum = 1
  tableData.value = []
  total.value = 0
  hasSearched.value = false
}

function formatCost(val) {
  const n = Number(val)
  return isNaN(n) ? val : n.toFixed(2)
}

// ===== 编辑 =====
const editVisible = ref(false)
const editLoading = ref(false)
const editRow = ref({})
const editForm = reactive({ materialNumber: '', precost: '' })

function openEdit(row) {
  editRow.value = row
  editForm.materialNumber = row.MATERIAL_NUMBER
  editForm.precost = row.PRECOST != null ? String(row.PRECOST) : ''
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.materialNumber) { ElMessage.warning('货号不能为空'); return }
  // 预估成本非空时必须是数字
  if (editForm.precost && editForm.precost.trim() && !editForm.precost.trim().match(/^-?\d+(\.\d+)?$/)) {
    ElMessage.warning('预估成本必须是数字')
    return
  }
  editLoading.value = true
  try {
    await updateEstimatedCost({ materialNumber: editForm.materialNumber, precost: editForm.precost })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  } catch { /* interceptor */ } finally { editLoading.value = false }
}

// ===== 导入记录 =====
const logLoading = ref(false)
const logData = ref([])
const logTotal = ref(0)
const logQuery = reactive({ pageNum: 1, pageSize: defaultPageSize.value })

async function loadLogData() {
  logLoading.value = true
  try {
    const res = await getEstimatedCostImportLogPage(logQuery)
    logData.value = res.data?.records || []
    logTotal.value = res.data?.total || 0
  } finally { logLoading.value = false }
}

function statusLabel(s) {
  return { SUCCESS: '成功', PARTIAL: '部分成功', FAILED: '失败' }[s] || s || '-'
}
function statusTag(s) {
  return { SUCCESS: 'success', PARTIAL: 'warning', FAILED: 'danger' }[s] || 'info'
}

// 错误详情
const errorDialogVisible = ref(false)
const errorDialogContent = ref('')
function showError(row) {
  errorDialogContent.value = row.errorMsg || '（无）'
  errorDialogVisible.value = true
}

// ===== 导入 =====
const showImportDialog = ref(false)
const importLoading = ref(false)
const templateLoading = ref(false)
const importResult = ref(null)
const selectedFile = ref(null)
const MAX_FILE_SIZE = 100 * 1024 * 1024

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
    const res = await getEstimatedCostTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '预估成本导入模板.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载模板失败')
  } finally {
    templateLoading.value = false
  }
}

async function handleImport() {
  if (!selectedFile.value) { ElMessage.warning('请先选择文件'); return }
  importLoading.value = true
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await importEstimatedCost(formData)
    importResult.value = res.data
    // 导入成功后刷新日志页签（若当前在日志页）
    if (activeTab.value === 'log') loadLogData()
    // 同时刷新数据列表
    if (hasSearched.value) loadData()
  } catch { /* interceptor */ } finally {
    importLoading.value = false
  }
}

// ===== 页签切换 =====
function onTabChange(tab) {
  if (tab === 'log') loadLogData()
}

onMounted(() => {
  // 默认不查询，等用户输入条件点搜索
})
</script>

<style scoped>
.cost-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  text-align: center;
}
.error-pre {
  max-height: 400px;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
