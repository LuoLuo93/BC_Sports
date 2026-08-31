<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="资料列表" name="data">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="货号">
          <el-input v-model="query.materialNumber" placeholder="请输入货号" clearable style="min-width:130px;max-width:170px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="货品名称">
          <el-input v-model="query.materialName" placeholder="请输入货品名称" clearable style="min-width:150px;max-width:200px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-select v-model="query.brandId" placeholder="全部" clearable filterable style="min-width:110px;max-width:140px">
            <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ID" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="query.kindId" placeholder="全部" clearable filterable style="min-width:110px;max-width:140px">
            <el-option v-for="k in kindList" :key="k.ID" :label="k.ATTRIBNAME" :value="k.ID" />
          </el-select>
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
          <span class="card-header-title">贴纸资料维护</span>
          <div class="header-actions">
            <el-button v-if="hasPermission('sticker:data:import')" type="warning" size="small" :icon="Upload" @click="showImportDialog = true">批量导入</el-button>
          </div>
        </div>
      </template>
      <div class="table-responsive">
          <el-table v-loading="loading" :data="tableData" border size="small" height="100%" :empty-text="hasSearched ? '暂无数据' : '请输入查询条件后点击搜索'">
          <el-table-column label="#" width="45" fixed="left">
            <template #default="{ $index }">{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="MATERIAL_NUMBER" label="货号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
          <el-table-column prop="STYLE_NUMBER" label="款号" width="170" show-overflow-tooltip class-name="col-key" />
          <el-table-column prop="MATERIAL_NAME" label="货品名称" width="200" show-overflow-tooltip class-name="col-key" />
          <el-table-column prop="BRAND_NAME" label="品牌" width="120" />
          <el-table-column prop="KIND_NAME" label="类别" width="100" show-overflow-tooltip />
          <el-table-column prop="COLOR" label="颜色" width="90" />
          <el-table-column prop="PRICE" label="价格" width="120">
            <template #default="{ row }">{{ row.PRICE != null ? Number(row.PRICE).toFixed(5) : '-' }}</template>
          </el-table-column>
          <el-table-column prop="EXECUTION_STANDARD" label="执行标准" width="160">
            <template #default="{ row }">{{ row.EXECUTION_STANDARD || '-' }}</template>
          </el-table-column>
          <el-table-column prop="SAFETY_CATEGORY" label="安全类别" width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.SAFETY_CATEGORY || '-' }}</template>
          </el-table-column>
          <el-table-column label="面料/辅料成分" width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.FAB_CODE || row.FAB_ELEMENT || row.AC_CODE || row.ACC_ELEMENT">
                <span v-if="row.FAB_CODE">面料1:{{ row.FAB_CODE }}</span>
                <span v-if="row.FAB_CODE && (row.FAB_ELEMENT || row.AC_CODE || row.ACC_ELEMENT)"> / </span>
                <span v-if="row.FAB_ELEMENT">面料2:{{ row.FAB_ELEMENT }}</span>
                <span v-if="row.FAB_ELEMENT && (row.AC_CODE || row.ACC_ELEMENT)"> / </span>
                <span v-if="row.AC_CODE">辅料1:{{ row.AC_CODE }}</span>
                <span v-if="row.AC_CODE && row.ACC_ELEMENT"> / </span>
                <span v-if="row.ACC_ELEMENT">辅料2:{{ row.ACC_ELEMENT }}</span>
              </span>
              <span v-else style="color:#d9d9d9">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="EAN13" label="EAN13" width="150">
            <template #default="{ row }">{{ row.EAN13 || '-' }}</template>
          </el-table-column>
          <el-table-column prop="SIZE_GROUP_NAME" label="矫正尺码组" width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag v-if="row.SIZE_GROUP_NAME" size="small" type="success" effect="plain">{{ row.SIZE_GROUP_NAME }}</el-tag>
              <span v-else style="color:#d9d9d9">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="SIZES" label="尺码组" width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="70" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('sticker:data:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
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
            <el-table v-loading="logLoading" :data="logData" border stripe size="small" empty-text="暂无导入记录">
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
    <el-dialog v-model="showImportDialog" title="批量导入贴纸资料" width="520px" destroy-on-close @open="resetImportState">
      <div class="import-zone">
        <el-upload :limit="1" accept=".xlsx,.xls" :auto-upload="false" :before-upload="beforeUpload" drag :on-change="handleFileChange" :on-remove="handleFileRemove" :on-exceed="() => ElMessage.warning('只能上传一个文件')">
          <el-icon :size="40" style="color:var(--el-text-color-placeholder)"><Upload /></el-icon>
          <div style="margin-top:8px">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-hint">仅支持 .xlsx / .xls 格式，按货号更新执行标准 / EAN13 / 安全类别 / 材质；<b>单元格留空 = 不更新该字段</b>（保留系统原值，不会清空）</div>
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
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="showImportDialog = false">关闭</el-button>
          <el-button class="btn-confirm" type="primary" :loading="importLoading" :disabled="importLoading" @click="submitImport">开始导入</el-button>
        </div>
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
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, Upload } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES } from '@/utils/appConfig'
import { getStickerDataPage, importStickerData, downloadStickerDataTemplate, getStickerDataImportLogPage } from '@/api/sticker'
import { getCommonBrands, getCommonKinds } from '@/api/common'
import { formatTime } from '@/utils/format'

defineOptions({ name: 'StickerData' })

const router = useRouter()
const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch } = usePageQuery(getStickerDataPage, {
  materialNumber: '', materialName: '', brandId: '', kindId: ''
})

const brandList = ref([])
const kindList = ref([])
// 是否已执行过搜索（首次进入未搜索前绝不加载数据，避免大数据量首屏卡顿）
const hasSearched = ref(false)

function onSearch() {
  hasSearched.value = true
  handleSearch()
  persistQuery()
}

/** 持久化搜索条件+分页，返回列表时恢复 */
function persistQuery() {
  sessionStorage.setItem('stickerDataQuery', JSON.stringify({
    materialNumber: query.materialNumber,
    materialName: query.materialName,
    brandId: query.brandId,
    kindId: query.kindId,
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }))
}

/** 改每页条数：回到第1页加载并持久化 */
function onSizeChange() {
  query.pageNum = 1
  loadData()
  persistQuery()
}

/** 翻页：加载并持久化分页状态 */
function onPageChange() {
  loadData()
  persistQuery()
}

function onReset() {
  query.materialNumber = ''
  query.materialName = ''
  query.brandId = ''
  query.kindId = ''
  query.pageNum = 1
  tableData.value = []
  total.value = 0
  hasSearched.value = false
  sessionStorage.removeItem('stickerDataQuery')
}

function handleEdit(row) {
  router.push({ name: 'StickerDataDetail', params: { materialNumber: row.MATERIAL_NUMBER }, state: { row: JSON.parse(JSON.stringify(row)) } })
}

async function loadBrands() {
  try {
    const { data } = await getCommonBrands()
    brandList.value = data || []
  } catch {}
}

async function loadKinds() {
  try {
    const { data } = await getCommonKinds()
    kindList.value = data || []
  } catch {}
}

function restoreAndLoad() {
  const saved = sessionStorage.getItem('stickerDataQuery')
  if (!saved) {
    // 首次进入无缓存：数据量较大，默认不查询，等用户输入条件点搜索
    hasSearched.value = false
    return
  }
  try {
    const q = JSON.parse(saved)
    query.materialNumber = q.materialNumber || ''
    query.materialName = q.materialName || ''
    query.brandId = q.brandId || ''
    query.kindId = q.kindId || ''
    // 恢复分页(翻页/每页条数)，避免返回后 pageSize 被重置为默认值
    query.pageNum = q.pageNum || 1
    query.pageSize = q.pageSize || query.pageSize
    hasSearched.value = true
    loadData()
  } catch {
    sessionStorage.removeItem('stickerDataQuery')
    hasSearched.value = false
  }
}

// ─── 批量导入 ───────────────────────────────────────────────
const showImportDialog = ref(false)
const importLoading = ref(false)
const templateLoading = ref(false)
const selectedFile = ref(null)
const importResult = ref(null)
const MAX_FILE_SIZE = 100 * 1024 * 1024

function resetImportState() {
  selectedFile.value = null
  importResult.value = null
}

function beforeUpload(file) {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  if (!isExcel) { ElMessage.error('仅支持 .xlsx / .xls 格式的 Excel 文件'); return false }
  if (file.size > MAX_FILE_SIZE) { ElMessage.error('文件大小不能超过 100MB'); return false }
  return true
}

function handleFileChange(file) {
  if (beforeUpload(file.raw)) {
    selectedFile.value = file.raw
    importResult.value = null
  }
}

function handleFileRemove() {
  selectedFile.value = null
}

async function submitImport() {
  if (!selectedFile.value) { ElMessage.warning('请先选择 Excel 文件'); return }
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await importStickerData(formData)
    importResult.value = res.data
    if (res.data?.success > 0) {
      ElMessage.success('导入完成')
      // 已搜索过才刷新列表；未搜索过保持"输入条件后查询"状态
      if (hasSearched.value) loadData()
    }
  } catch (e) {
    ElMessage.error('导入失败：' + (e.message || '服务器错误'))
  } finally {
    importLoading.value = false
  }
}

async function handleDownloadTemplate() {
  templateLoading.value = true
  try {
    const blobData = await downloadStickerDataTemplate()
    const blob = new Blob([blobData], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '贴纸资料导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('模板下载失败')
  } finally {
    templateLoading.value = false
  }
}

// ─── Tab + 导入日志 ─────────────────────────────────────────
const activeTab = ref('data')
function onTabChange(tab) {
  if (tab === 'log') loadLogData()
}

const logLoading = ref(false)
const logData = ref([])
const logTotal = ref(0)
const logQuery = reactive({ pageNum: 1, pageSize: 10 })
const errorDialogVisible = ref(false)
const errorDialogContent = ref('')

async function loadLogData() {
  logLoading.value = true
  try {
    const res = await getStickerDataImportLogPage({ pageNum: logQuery.pageNum, pageSize: logQuery.pageSize })
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

function viewErrors(row) {
  errorDialogContent.value = row.errorMsg || ''
  errorDialogVisible.value = true
}

onMounted(() => {
  loadBrands()
  loadKinds()
  restoreAndLoad()
})

onActivated(() => {
  restoreAndLoad()
})
</script>

<style scoped>
.pagination-wrapper {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

/* 导入弹窗 */
.import-zone {
  text-align: center;
}
.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
