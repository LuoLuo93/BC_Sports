<template>
  <div class="page-container">
    <el-card shadow="never">
      <div style="display:flex;justify-content:space-between;margin-bottom:16px">
        <div style="display:flex;gap:12px;align-items:center">
          <el-select v-model="query.brandName" placeholder="品牌" size="default" clearable filterable style="width:180px" @change="loadData">
            <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ATTRIBNAME" />
          </el-select>
          <el-select v-model="query.kindName" placeholder="类别" size="default" clearable filterable style="width:180px" @change="loadData">
            <el-option v-for="k in kindList" :key="k.ID" :label="k.ATTRIBNAME" :value="k.ATTRIBNAME" />
          </el-select>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <el-button v-if="hasPermission('sticker:brand-template:add')" type="primary" @click="handleAdd">新增</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="brandName" label="品牌" width="150" />
        <el-table-column prop="kindName" label="类别" width="150" />
        <el-table-column prop="templateName" label="打印模板" min-width="200" show-overflow-tooltip />
        <el-table-column prop="printerName" label="默认打印机" width="200" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" width="200" show-overflow-tooltip />
        <el-table-column prop="isActive" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">
              {{ row.isActive === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('sticker:brand-template:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('sticker:brand-template:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
        style="margin-top:16px;justify-content:flex-end"
      />
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
          <el-input v-model="form.templateName" placeholder="请输入 .btw 模板文件名" />
        </el-form-item>
        <el-form-item label="默认打印机">
          <el-input v-model="form.printerName" placeholder="打印机名称" />
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
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { getTemplateList, getProductBrands } from '@/api/sticker'
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, brandName: '', kindName: '' })

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
  templateName: [{ required: true, message: '请输入模板文件名', trigger: 'blur' }]
}

const brandList = ref([])
const kindList = ref([])
const templateList = ref([])

async function loadData() {
  loading.value = true
  try {
    const { data } = await request.get('/api/sticker/brand-template/page', {
      params: { page: query.page, size: query.size, brandName: query.brandName, kindName: query.kindName }
    })
    tableData.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleReset() {
  query.brandName = ''
  query.kindName = ''
  query.page = 1
  loadData()
}

async function loadBrands() {
  if (brandList.value.length) return
  try {
    const { data } = await getProductBrands()
    brandList.value = data || []
  } catch {}
}

async function loadKinds() {
  if (kindList.value.length) return
  try {
    const { data } = await request.get('/api/sticker/brand-template/kinds')
    kindList.value = data || []
  } catch {}
}

async function loadTemplates() {
  if (templateList.value.length) return
  try {
    const { data } = await getTemplateList()
    templateList.value = data || []
  } catch {}
}

function onBrandChange(val) {
  const b = brandList.value.find(item => item.ID === val)
  form.brandName = b ? b.ATTRIBNAME : ''
}

function onKindChange(val) {
  const k = kindList.value.find(item => item.ID === val)
  form.kindName = k ? k.ATTRIBNAME : ''
}

function onTemplateChange(val) {
  const t = templateList.value.find(item => item.id === val)
  form.templateName = t ? t.templateName : ''
}

function handleAdd() {
  isEdit.value = false
  editId.value = ''
  Object.assign(form, { brandId: '', brandName: '', kindId: '', kindName: '', templateId: '', templateName: '', printerName: '', remark: '', isActive: 1 })
  loadBrands()
  loadKinds()
  loadTemplates()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    brandId: row.brandId, brandName: row.brandName,
    kindId: row.kindId, kindName: row.kindName,
    templateId: row.templateId, templateName: row.templateName,
    printerName: row.printerName, remark: row.remark, isActive: row.isActive
  })
  loadBrands()
  loadKinds()
  loadTemplates()
  dialogVisible.value = true
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

onMounted(() => {
  loadData()
  loadBrands()
  loadKinds()
})
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
</style>
