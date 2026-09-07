<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 数据列表 -->
      <el-tab-pane label="数据列表" name="data">
        <el-card shadow="never" class="search-card">
          <el-form :model="query" inline>
            <el-form-item label="单据号">
              <el-input v-model="query.billNo" placeholder="请输入单据号(模糊)" clearable @keyup.enter="onSearch" />
            </el-form-item>
            <el-form-item label="提交时间">
              <el-date-picker v-model="billTimeRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:240px" />
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
              <span class="card-header-title">数仓销售明细（ODS_SALES_MAIN）</span>
              <el-button v-if="hasPermission('bi:dw-sales:import')" type="warning" plain size="small" :icon="Upload" @click="showImportDialog = true">批量导入</el-button>
            </div>
          </template>

          <div class="table-responsive">
            <el-table v-loading="loading" :data="tableData" border stripe :empty-text="hasSearched ? '暂无数据' : '请输入查询条件后点击搜索'">
              <el-table-column label="#" width="70" align="center" fixed>
                <template #default="{ $index }">{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="billNo" label="单据号" min-width="150" fixed show-overflow-tooltip />
              <el-table-column label="单据日期" width="110" fixed>
                <template #default="{ row }">{{ formatBillDate(row.billDate) }}</template>
              </el-table-column>
              <el-table-column label="提交时间" width="165" fixed>
                <template #default="{ row }">{{ formatTime(row.billTime) }}</template>
              </el-table-column>
              <el-table-column prop="salesType" label="销售类型" width="90" show-overflow-tooltip />
              <el-table-column prop="promotionName" label="促销名称" min-width="150" show-overflow-tooltip />
              <el-table-column prop="storeName" label="店铺名称" min-width="140" show-overflow-tooltip />
              <el-table-column prop="productCode" label="货号" min-width="120" show-overflow-tooltip />
              <el-table-column prop="productStyleNo" label="款号" min-width="110" show-overflow-tooltip />
              <el-table-column prop="productName" label="货品名称" min-width="150" show-overflow-tooltip />
              <el-table-column prop="colorsalias" label="颜色" width="90" show-overflow-tooltip />
              <el-table-column prop="sizes" label="尺码" width="80" show-overflow-tooltip />
              <el-table-column prop="barcode" label="条码" min-width="130" show-overflow-tooltip />
              <el-table-column prop="qty" label="数量" width="80" align="right" />
              <el-table-column label="零售价" width="100" align="right">
                <template #default="{ row }">{{ formatAmount(row.retailPrice) }}</template>
              </el-table-column>
              <el-table-column label="零售金额" width="110" align="right">
                <template #default="{ row }">{{ formatAmount(row.retailAmount) }}</template>
              </el-table-column>
              <el-table-column label="成交金额" width="110" align="right">
                <template #default="{ row }">{{ formatAmount(row.transactionAmount) }}</template>
              </el-table-column>
              <el-table-column label="业绩金额" width="110" align="right">
                <template #default="{ row }">{{ formatAmount(row.revenue) }}</template>
              </el-table-column>
              <el-table-column label="重算业绩" width="110" align="right">
                <template #default="{ row }">{{ formatAmount(row.recalcRevenue) }}</template>
              </el-table-column>
              <el-table-column prop="newOldNameAdjust" label="新旧货" width="100" show-overflow-tooltip />
              <el-table-column prop="anchorSummaryname" label="主播" min-width="100" show-overflow-tooltip />
              <el-table-column prop="billPosName" label="营业员" min-width="100" show-overflow-tooltip />
              <el-table-column prop="vipCode" label="会员卡号" min-width="120" show-overflow-tooltip />
              <el-table-column prop="vipMobile" label="会员手机号" min-width="120" show-overflow-tooltip />
              <el-table-column prop="omsSourcecode" label="网单来源单号" min-width="140" show-overflow-tooltip />
              <el-table-column v-if="hasPermission('bi:dw-sales:edit')" label="操作" width="90" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" plain size="small" :disabled="!row.billId || !row.itemId" @click="openEdit(row)">编辑</el-button>
                </template>
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
              @size-change="onSizeChange"
              @current-change="onPageChange"
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
    <el-dialog v-model="showImportDialog" title="批量导入销售明细" width="560px" destroy-on-close @open="resetImportState">
      <div class="import-zone">
        <el-upload :limit="1" accept=".xlsx,.xls" :auto-upload="false" :before-upload="beforeUpload" drag :on-change="handleFileChange" :on-remove="handleFileRemove" :on-exceed="() => ElMessage.warning('只能上传一个文件')">
          <el-icon :size="40" style="color:var(--el-text-color-placeholder)"><Upload /></el-icon>
          <div style="margin-top:8px">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-hint">
              仅支持 .xlsx / .xls，文件 ≤500MB、≤300万行（150W行约5~15分钟，请勿关闭页面；中途断网不影响后台处理，结果可在导入日志查看）<br/>
              表头须为英文列名（BILL_NO / BILL_TIME / ...），多个 sheet 时每个 sheet 需含表头行<br/>
              期初数据导入：仅插入不防重，同一文件重复导入会产生重复数据；BILL_ID / ITEM_ID 留空将自动生成（同一单据号共用 BILL_ID）
            </div>
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
defineOptions({ name: 'DwSalesMain' })
import { reactive, ref, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, Upload } from '@element-plus/icons-vue'
import { getDwSalesMainPage, importDwSalesMain, getDwSalesTemplate, getDwSalesImportLogPage } from '@/api/bi'
import { formatTime } from '@/utils/format'
import { PAGE_SIZES, defaultPageSize } from '@/utils/appConfig'
import { usePageQuery } from '@/composables/usePageQuery'
import { usePermission } from '@/composables/usePermission'

const router = useRouter()
const { hasPermission } = usePermission()

// ===== Tab =====
const activeTab = ref('data')
function onTabChange(tab) {
  if (tab === 'log') loadLogData()
}

// ===== 数据列表 =====
const { loading, tableData, total, query, loadData, handleSearch } = usePageQuery(getDwSalesMainPage, { billNo: '' })
const billTimeRange = ref(null)

// 默认不查询：首次进入页面不发请求，点搜索才查
const hasSearched = ref(false)

// 发请求前同步日期范围到 query（loadData 序列化的是 query 对象）
function syncDateRange() {
  if (billTimeRange.value && billTimeRange.value.length === 2) {
    query.billTimeStart = billTimeRange.value[0]
    query.billTimeEnd = billTimeRange.value[1]
  } else {
    query.billTimeStart = ''
    query.billTimeEnd = ''
  }
}

function onSearch() {
  hasSearched.value = true
  syncDateRange()
  handleSearch()
}

function onReset() {
  query.billNo = ''
  billTimeRange.value = null
  hasSearched.value = false
  query.pageNum = 1
  tableData.value = []
  total.value = 0
}

function onSizeChange() {
  if (hasSearched.value) {
    syncDateRange()
    handleSearch()
  }
}

function onPageChange() {
  if (hasSearched.value) {
    syncDateRange()
    loadData()
  }
}

// BILL_DATE 为 NUMBER(8) YYYYMMDD
function formatBillDate(v) {
  if (v === null || v === undefined || v === '') return '-'
  const s = String(v)
  return s.length === 8 ? `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}` : s
}

function formatAmount(n) {
  if (n === null || n === undefined) return '-'
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 6 })
}

// ===== 编辑：跳转独立编辑页(state携带整行数据，刷新后history.state仍保留) =====
function openEdit(row) {
  router.push({ path: '/bi/dw-sales/edit', state: { row } })
}

// 从编辑页返回时刷新列表(仅已搜索过时，保持默认不查询语义)
onActivated(() => {
  if (hasSearched.value) {
    syncDateRange()
    loadData()
  }
})

// ===== 导入日志 =====
const logLoading = ref(false)
const logData = ref([])
const logTotal = ref(0)
const logQuery = reactive({ pageNum: 1, pageSize: defaultPageSize.value })

async function loadLogData() {
  logLoading.value = true
  try {
    const res = await getDwSalesImportLogPage({ pageNum: logQuery.pageNum, pageSize: logQuery.pageSize })
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

// ===== 批量导入 =====
const showImportDialog = ref(false)
const importLoading = ref(false)
const templateLoading = ref(false)
const importResult = ref(null)
const selectedFile = ref(null)

const MAX_IMPORT_SIZE = 500 * 1024 * 1024

function beforeUpload(file) {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  if (!isExcel) {
    ElMessage.error('仅支持 .xlsx / .xls 格式的 Excel 文件')
    return false
  }
  if (file.size > MAX_IMPORT_SIZE) {
    ElMessage.error(`文件大小不能超过 500MB（当前 ${(file.size / 1024 / 1024).toFixed(1)}MB）`)
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
  if (raw.size > MAX_IMPORT_SIZE) {
    ElMessage.error(`文件大小不能超过 500MB（当前 ${(raw.size / 1024 / 1024).toFixed(1)}MB）`)
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
    const res = await getDwSalesTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '数仓销售导入模板.xlsx'
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
  importLoading.value = true
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await importDwSalesMain(formData)
    importResult.value = res.data
    if (res.data?.success > 0) {
      ElMessage.success('导入完成')
      if (hasSearched.value) loadData()
      if (activeTab.value === 'log') loadLogData()
    }
  } catch (e) {
    ElMessage.error('导入失败：' + (e.message || '服务器错误'))
  } finally {
    importLoading.value = false
  }
}
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
.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  line-height: 1.8;
}
</style>
