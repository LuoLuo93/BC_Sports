<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="店铺代码">
          <el-input v-model="query.shopCode" placeholder="请输入店铺代码" clearable style="min-width:150px;max-width:200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="店铺名称">
          <el-input v-model="query.shopName" placeholder="请输入店铺名称" clearable style="min-width:150px;max-width:200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="联营老板">
          <el-input v-model="query.shopBoss" placeholder="请输入联营老板" clearable style="min-width:150px;max-width:200px" @keyup.enter="handleSearch" />
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
          <span class="card-header-title">揽众客户押金资料</span>
          <div class="header-actions">
            <el-button v-if="hasPermission('erp:lzCustomer:add')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增</el-button>
            <el-button v-if="hasPermission('erp:lzCustomer:import')" type="warning" size="small" :icon="Upload" @click="showImportDialog = true">批量导入</el-button>
          </div>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="SHOPCODE" label="店铺代码" width="140" show-overflow-tooltip />
          <el-table-column prop="SHOPNAME" label="店铺/客户名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="SHOPBOSS" label="门店所属联营老板" min-width="160" show-overflow-tooltip />
          <el-table-column prop="FUNDINGLIMIT" label="资金额度" width="120" />
          <el-table-column prop="FUNDINGRATIO" label="资金倍率" width="120" />
          <el-table-column label="操作" width="160" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('erp:lzCustomer:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('erp:lzCustomer:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑揽众资料' : '新增揽众资料'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="店铺代码" prop="shopCode">
          <el-input v-model="form.shopCode" placeholder="请输入店铺代码" />
        </el-form-item>
        <el-form-item label="店铺/客户名称" prop="shopName">
          <el-input v-model="form.shopName" placeholder="请输入店铺/客户名称" />
        </el-form-item>
        <el-form-item label="门店所属联营老板" prop="shopBoss">
          <el-input v-model="form.shopBoss" placeholder="请输入门店所属联营老板" />
        </el-form-item>
        <el-form-item label="资金额度" prop="fundingLimit">
          <el-input v-model="form.fundingLimit" placeholder="请输入资金额度" />
        </el-form-item>
        <el-form-item label="资金倍率" prop="fundingRatio">
          <el-input v-model="form.fundingRatio" placeholder="请输入资金倍率" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="showImportDialog" title="批量导入揽众客户资料" width="520px" destroy-on-close @open="resetImportState">
      <div class="import-zone">
        <el-upload :limit="1" accept=".xlsx,.xls" :auto-upload="false" :before-upload="beforeUpload" drag :on-change="handleFileChange" :on-remove="handleFileRemove" :on-exceed="() => ElMessage.warning('只能上传一个文件')">
          <el-icon :size="40" style="color:var(--el-text-color-placeholder)"><Upload /></el-icon>
          <div style="margin-top:8px">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-hint">仅支持 .xlsx / .xls 格式，店铺代码重复时自动更新</div>
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
defineOptions({ name: 'LzCustomer' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight, Upload } from '@element-plus/icons-vue'
import { getLzCustomerPage, getLzCustomer, createLzCustomer, updateLzCustomer, deleteLzCustomer, importLzCustomer, getLzCustomerTemplate } from '@/api/erp'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/appConfig'

const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getLzCustomerPage, {
  shopCode: '', shopName: '', shopBoss: ''
})

const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const defaultForm = () => ({
  shopCode: '', shopName: '', shopBoss: '', fundingLimit: '', fundingRatio: ''
})
const form = reactive(defaultForm())
const rules = {
  shopCode: [{ required: true, message: '请输入店铺代码', trigger: 'blur' }],
  shopName: [{ required: true, message: '请输入店铺/客户名称', trigger: 'blur' }]
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row) {
  const res = await getLzCustomer(row.ID)
  isEdit.value = true
  editId.value = row.ID
  const d = res.data || {}
  form.shopCode = d.SHOPCODE || ''
  form.shopName = d.SHOPNAME || ''
  form.shopBoss = d.SHOPBOSS || ''
  form.fundingLimit = d.FUNDINGLIMIT || ''
  form.fundingRatio = d.FUNDINGRATIO || ''
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.SHOPNAME || row.SHOPCODE}」？`, '提示', { type: 'warning' })
  await deleteLzCustomer(row.ID)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateLzCustomer(editId.value, { ...form })
    } else {
      await createLzCustomer({ ...form })
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
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
    const res = await importLzCustomer(formData)
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
    const res = await getLzCustomerTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '揽众客户资料导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('模板下载失败')
  } finally {
    templateLoading.value = false
  }
}

onMounted(() => loadData())
</script>
