<template>
  <div class="page-container">
    <el-tabs v-model="activeTab">
      <!-- 数据列表 -->
      <el-tab-pane label="数据列表" name="data">
        <el-card shadow="never" class="search-card">
          <el-form :model="query" inline>
            <el-form-item label="运动员">
              <el-input v-model="query.sporter" placeholder="请输入运动员名称" clearable @keyup.enter="handleSearch" />
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
              <span class="card-header-title">运动积分</span>
              <el-button v-if="hasPermission('bcp:sport-points:import')" type="warning" size="small" :icon="Upload" @click="showImportDialog = true">批量导入</el-button>
            </div>
          </template>

          <div class="table-responsive">
            <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
              <el-table-column label="#" width="60" align="center">
                <template #default="{ $index }">{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="sporter" label="运动员" min-width="160" show-overflow-tooltip />
              <el-table-column prop="points" label="积分" min-width="120" align="right" />
              <el-table-column prop="updateBy" label="修改人" min-width="120" show-overflow-tooltip />
              <el-table-column label="修改时间" min-width="180">
                <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
              </el-table-column>
              <el-table-column v-if="hasPermission('bcp:sport-points:edit')" label="操作" width="90" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" plain size="small" @click="openEdit(row)">编辑</el-button>
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
              @size-change="handleSearch"
              @current-change="loadData"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 导入日志 -->
      <el-tab-pane label="导入日志" name="log" lazy>
        <ImportLogPanel ref="logPanel" :fetcher="getSportPointsImportLogPage" />
      </el-tab-pane>

      <!-- 排名预览：手机尺寸内嵌 /points 移动端页面，改完数据直接看排名变化 -->
      <el-tab-pane label="排名预览" name="preview" lazy>
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">移动端排名预览</span>
              <div>
                <el-button size="small" :icon="RefreshRight" @click="refreshPreview">刷新</el-button>
                <el-button size="small" :icon="View" tag="a" href="/bcsports/points" target="_blank">新窗口打开</el-button>
              </div>
            </div>
          </template>
          <div class="preview-wrap">
            <div class="phone-frame">
              <div class="phone-notch"></div>
              <iframe
                :key="previewKey"
                :src="`/bcsports/points?_pv=${previewKey}`"
                class="phone-screen"
                title="运动积分排名移动端预览"
              />
            </div>
            <p class="preview-hint">展示内容与手机外链完全一致（前 100 名）；导入或编辑成功后自动刷新，无需进手机查看。</p>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑运动积分" width="440px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="运动员" prop="sporter">
          <el-input v-model="editForm.sporter" maxlength="100" placeholder="运动员姓名" />
        </el-form-item>
        <el-form-item label="积分" prop="points">
          <el-input v-model="editForm.points" placeholder="整数，可为负" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="showImportDialog" title="批量导入运动积分" width="520px" destroy-on-close @open="resetImportState">
      <div class="import-zone">
        <el-upload :limit="1" accept=".xlsx,.xls" :auto-upload="false" :before-upload="beforeUpload" drag :on-change="handleFileChange" :on-remove="handleFileRemove" :on-exceed="() => ElMessage.warning('只能上传一个文件')">
          <el-icon :size="40" style="color:var(--el-text-color-placeholder)"><Upload /></el-icon>
          <div style="margin-top:8px">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-hint">仅支持 .xlsx / .xls 格式，文件大小不超过 100MB；列为「运动员 + 积分」，同一运动员重复出现时只保留最后一行积分</div>
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
  </div>
</template>

<script setup>
defineOptions({ name: 'BcpSportPointsImport' })
import { ref, reactive, onMounted, onActivated } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, Upload, View } from '@element-plus/icons-vue'
import { getSportPointsPage, importSportPoints, getSportPointsTemplate, getSportPointsImportLogPage, updateSportPoints } from '@/api/bcp'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES, defaultPageSize } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import ImportLogPanel from '@/components/ImportLogPanel.vue'

const { hasPermission } = usePermission()

// ===== Tab =====
const activeTab = ref('data')

// 导入日志面板（F70：收口到 ImportLogPanel，tab lazy 首次切入时自动加载）
const logPanel = ref(null)

// ===== 数据列表 =====
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: defaultPageSize.value, sporter: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await getSportPointsPage({ pageNum: query.pageNum, pageSize: query.pageSize, sporter: query.sporter })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}
function handleSearch() { query.pageNum = 1; loadData() }
function resetQuery() { query.sporter = ''; handleSearch() }

// ===== 排名预览 =====
// 每次变化重挂 iframe（key + 时间戳双保险），导入/编辑成功后自动刷新预览
const previewKey = ref(Date.now())
function refreshPreview() { previewKey.value = Date.now() }

// ===== 编辑 =====
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editForm = reactive({ id: null, sporter: '', points: '' })

const editRules = {
  sporter: [{ required: true, message: '运动员不能为空', trigger: 'blur' }],
  points: [
    { required: true, message: '积分不能为空', trigger: 'blur' },
    { pattern: /^[+-]?\d+$/, message: '积分必须是整数', trigger: 'blur' }
  ]
}

function openEdit(row) {
  editForm.id = row.id
  editForm.sporter = row.sporter
  editForm.points = String(row.points)
  editDialogVisible.value = true
}

async function submitEdit() {
  await editFormRef.value?.validate()
  editLoading.value = true
  try {
    await updateSportPoints(editForm.id, { sporter: editForm.sporter.trim(), points: Number(editForm.points) })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadData()
    refreshPreview()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    editLoading.value = false
  }
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
    const res = await getSportPointsTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '运动积分导入模板.xlsx'
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
    const res = await importSportPoints(formData)
    importResult.value = res.data
    if (res.data?.success > 0) {
      ElMessage.success('导入完成')
      loadData()
      refreshPreview()
      if (activeTab.value === 'log') logPanel.value?.loadLog()
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

/* ===== 排名预览：手机外框 ===== */
.preview-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 4px;
}
.phone-frame {
  position: relative;
  width: 392px;
  max-width: 100%;
  height: 720px;
  border-radius: 36px;
  border: 10px solid #1f2937;
  box-shadow: 0 12px 32px rgba(28, 25, 23, 0.18);
  overflow: hidden;
  background: #fff;
}
.phone-notch {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 120px;
  height: 22px;
  background: #1f2937;
  border-radius: 0 0 14px 14px;
  z-index: 2;
}
.phone-screen {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}
.preview-hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
