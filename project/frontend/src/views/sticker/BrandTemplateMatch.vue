<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="品牌">
          <el-select v-model="query.brandName" placeholder="全部" clearable filterable style="min-width:140px;max-width:180px">
            <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ATTRIBNAME" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="query.kindName" placeholder="全部" clearable filterable style="min-width:140px;max-width:180px">
            <el-option v-for="k in kindList" :key="k.ID" :label="k.ATTRIBNAME" :value="k.ATTRIBNAME" />
          </el-select>
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
          <span class="card-header-title">品牌模板配置</span>
          <div class="header-actions">
            <el-button v-if="hasPermission('sticker:brand-template:import')" type="warning" size="small" :icon="Upload" @click="showImportDialog = true">批量导入</el-button>
            <el-button v-if="hasPermission('sticker:brand-template:add')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增</el-button>
          </div>
        </div>
      </template>
      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe>
          <el-table-column prop="brandName" label="品牌" width="150" />
          <el-table-column prop="kindName" label="类别" width="150" />
          <el-table-column prop="templateName" label="打印模板" min-width="200" show-overflow-tooltip />
          <el-table-column prop="printerName" label="默认打印机" width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ getPrinterLabel(row.printerName) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" width="200" show-overflow-tooltip />
          <el-table-column prop="isActive" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">
                {{ row.isActive === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('sticker:brand-template:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('sticker:brand-template:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑' : '新增'" width="520px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="品牌" prop="brandId">
          <el-select v-model="form.brandId" placeholder="选择品牌" filterable style="width:100%" @change="onBrandChange">
            <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ID" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别" prop="kindId">
          <el-select v-model="form.kindId" placeholder="选择类别" filterable style="width:100%" @change="onKindChange">
            <el-option v-for="k in kindList" :key="k.ID" :label="k.ATTRIBNAME" :value="k.ID" />
          </el-select>
        </el-form-item>
        <el-form-item label="打印模板" prop="templateName">
          <el-select v-model="form.templateName" placeholder="选择打印模板" filterable style="width:100%">
            <el-option v-for="t in templateOptions" :key="t.dictValue" :label="t.dictLabel" :value="t.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认打印机">
          <el-select v-model="form.printerName" placeholder="选择打印机" filterable clearable style="width:100%">
            <el-option v-for="p in printerOptions" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="showImportDialog" title="批量导入品牌模板关系" width="520px" destroy-on-close @open="resetImportState">
      <div class="import-zone">
        <el-upload :limit="1" accept=".xlsx,.xls" :auto-upload="false" :before-upload="beforeUpload" drag :on-change="handleFileChange" :on-remove="handleFileRemove" :on-exceed="() => ElMessage.warning('只能上传一个文件')">
          <el-icon :size="40" style="color:var(--el-text-color-placeholder)"><Upload /></el-icon>
          <div style="margin-top:8px">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-hint">仅支持 .xlsx / .xls 格式，品牌+类别重复时自动更新</div>
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
        <el-button type="primary" :loading="importLoading" :disabled="importLoading" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, Plus, Upload } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getProductBrands } from '@/api/sticker'
import { usePageQuery } from '@/composables/usePageQuery'
import { usePermission } from '@/composables/usePermission'
import { useDictStore } from '@/stores/dict'
import { PAGE_SIZES } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'

const { hasPermission } = usePermission()
const dictStore = useDictStore()

const brandList = ref([])
const kindList = ref([])
const printerOptions = ref([])
const templateOptions = ref([])

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(
  (params) => request.get('/api/sticker/brand-template/page', { params }),
  { brandName: '', kindName: '' }
)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref('')
const saving = ref(false)
const formRef = ref()

const form = reactive({
  brandId: '', brandName: '',
  kindId: '', kindName: '',
  templateId: '', templateName: '',
  printerName: '', remark: '', isActive: 1
})

const rules = {
  brandId: [{ required: true, message: '请选择品牌', trigger: 'change' }],
  kindId: [{ required: true, message: '请选择类别', trigger: 'change' }],
  templateName: [{ required: true, message: '请选择打印模板', trigger: 'change' }]
}

async function loadBrands() {
  if (brandList.value.length) return
  try {
    const { data } = await getProductBrands()
    brandList.value = (data || []).map(b => ({ ...b, ID: String(b.ID) }))
  } catch {}
}

async function loadKinds() {
  if (kindList.value.length) return
  try {
    const { data } = await request.get('/api/sticker/brand-template/kinds')
    kindList.value = (data || []).map(k => ({ ...k, ID: String(k.ID) }))
  } catch {}
}

async function loadPrinterOptions() {
  try {
    const data = await dictStore.loadDict('printer_name')
    printerOptions.value = (data || []).map(d => ({ value: d.dictValue, label: d.dictLabel }))
  } catch (e) {
    console.error('加载打印机字典失败:', e)
    printerOptions.value = []
  }
}

async function loadTemplateOptions() {
  try {
    templateOptions.value = await dictStore.loadDict('sticker_template')
  } catch (e) {
    console.error('加载打印模板字典失败:', e)
    templateOptions.value = []
  }
}

function getPrinterLabel(value) {
  if (!value) return '-'
  const option = printerOptions.value.find(p => p.value === value)
  return option ? option.label : value
}

function onBrandChange(val) {
  const b = brandList.value.find(item => item.ID === val)
  form.brandName = b ? b.ATTRIBNAME : ''
}

function onKindChange(val) {
  const k = kindList.value.find(item => item.ID === val)
  form.kindName = k ? k.ATTRIBNAME : ''
}

function handleAdd() {
  isEdit.value = false
  editId.value = ''
  Object.assign(form, { brandId: '', brandName: '', kindId: '', kindName: '', templateId: '', templateName: '', printerName: '', remark: '', isActive: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  dialogVisible.value = true
  nextTick(() => {
    // 确保ID类型与选择器期望的类型一致（String）
    const brandId = row.brandId ? String(row.brandId) : ''
    const kindId = row.kindId ? String(row.kindId) : ''

    Object.assign(form, {
      brandId: brandId, brandName: row.brandName || '',
      kindId: kindId, kindName: row.kindName || '',
      templateId: row.templateId, templateName: row.templateName,
      printerName: row.printerName, remark: row.remark, isActive: row.isActive
    })

    // 手动触发同步函数，确保名称字段与ID字段一致
    if (brandId) onBrandChange(brandId)
    if (kindId) onKindChange(kindId)
  })
}

async function handleSave() {
  try {
    await formRef.value.validate()
  } catch { return }
  saving.value = true
  try {
    if (isEdit.value) {
      await request.put(`/api/sticker/brand-template/${editId.value}`, form)
    } else {
      await request.post('/api/sticker/brand-template', form)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除「${row.brandName} - ${row.kindName}」？`, '提示')
  await request.delete(`/api/sticker/brand-template/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

// ========== 批量导入 ==========
const showImportDialog = ref(false)
const importLoading = ref(false)
const templateLoading = ref(false)
const selectedFile = ref(null)
const importResult = ref(null)
const MAX_FILE_SIZE = 50 * 1024 * 1024

function resetImportState() {
  selectedFile.value = null
  importResult.value = null
}

function beforeUpload(file) {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  if (!isExcel) { ElMessage.error('仅支持 .xlsx / .xls 格式的 Excel 文件'); return false }
  if (file.size > MAX_FILE_SIZE) { ElMessage.error('文件大小不能超过 50MB'); return false }
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
    const res = await request.post('/api/sticker/brand-template/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000
    })
    importResult.value = res.data
    if (res.data?.success > 0) {
      ElMessage.success('导入完成')
      loadData()
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
    const res = await request.get('/api/sticker/brand-template/template', { responseType: 'blob' })
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '品牌模板关系导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('模板下载失败')
  } finally {
    templateLoading.value = false
  }
}

onMounted(() => {
  loadData()
  loadBrands()
  loadKinds()
  loadPrinterOptions()
  loadTemplateOptions()
})
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
</style>
