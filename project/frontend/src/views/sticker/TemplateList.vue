<template>
  <SearchPage
    title="打印模板管理"
    v-model:page-num="query.pageNum"
    v-model:page-size="query.pageSize"
    :total="total"
    @page-change="loadData"
  >
    <template #search>
      <el-form-item label="模板名称">
        <el-input v-model="query.templateName" placeholder="搜索模板名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
    </template>

    <template #actions>
      <el-button type="primary" size="small" @click="createTemplate">新建模板</el-button>
    </template>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" align="center" />
      <el-table-column prop="templateName" label="模板名称" min-width="140" />
      <el-table-column label="标签尺寸" width="130" align="center">
        <template #default="{ row }">{{ row.labelWidth }} × {{ row.labelHeight }} mm</template>
      </el-table-column>
      <el-table-column prop="dpi" label="DPI" width="80" align="center" />
      <el-table-column prop="createTime" label="创建日期" width="120" align="center" />
      <el-table-column label="默认" width="60" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="380" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" plain size="small" @click="editTemplate(row)">编辑</el-button>
          <el-button size="small" @click="openFieldMapping(row)">字段映射</el-button>
          <el-button size="small" @click="copyTemplate(row)">复制</el-button>
          <el-button size="small" @click="handleSetDefault(row)" :disabled="row.isDefault === 1">设为默认</el-button>
          <el-button type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </SearchPage>

  <!-- 字段映射弹窗 -->
  <FieldMappingDialog
    v-model="fieldMappingVisible"
    :template-id="currentTemplateId"
    :template-name="currentTemplateName"
  />

  <!-- 新建模板弹窗 -->
  <el-dialog v-model="showCreateDialog" title="新建标签模板" width="420px" :close-on-click-modal="false">
    <el-form :model="createForm" label-width="80px">
      <el-form-item label="模板名称" required>
        <el-input v-model="createForm.name" placeholder="请输入模板名称" />
      </el-form-item>
      <el-form-item label="标签宽度" required>
        <el-input-number v-model="createForm.width" :min="10" :max="200" :step="1" style="width: 100%" />
        <div class="form-hint">单位：毫米 (mm)</div>
      </el-form-item>
      <el-form-item label="标签高度" required>
        <el-input-number v-model="createForm.height" :min="10" :max="200" :step="1" style="width: 100%" />
        <div class="form-hint">单位：毫米 (mm)</div>
      </el-form-item>
      <el-form-item label="打印 DPI">
        <el-select v-model="createForm.dpi" style="width: 100%">
          <el-option label="203 DPI（ZT210）" :value="203" />
          <el-option label="300 DPI（ZT230）" :value="300" />
        </el-select>
      </el-form-item>
      <el-form-item label="常用尺寸">
        <div class="preset-sizes">
          <el-tag v-for="p in presets" :key="p.label" class="preset-tag" @click="applyPreset(p)">
            {{ p.label }}
          </el-tag>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showCreateDialog = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="confirmCreate">创建并设计</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchPage from '@/components/SearchPage.vue'
import FieldMappingDialog from './FieldMappingDialog.vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { getTemplatePage as apiGetPage, createTemplate as apiCreate, deleteTemplate as apiDelete, setDefaultTemplate } from '@/api/sticker'

const router = useRouter()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(apiGetPage, {
  templateName: ''
})

onMounted(() => loadData())

// 字段映射
const fieldMappingVisible = ref(false)
const currentTemplateId = ref('')
const currentTemplateName = ref('')

function openFieldMapping(row) {
  currentTemplateId.value = row.id
  currentTemplateName.value = row.templateName
  fieldMappingVisible.value = true
}

const submitting = ref(false)
const showCreateDialog = ref(false)
const createForm = reactive({ name: '', width: 60, height: 40, dpi: 203 })

const presets = [
  { label: '60×40', width: 60, height: 40 },
  { label: '40×30', width: 40, height: 30 },
  { label: '80×50', width: 80, height: 50 },
  { label: '100×60', width: 100, height: 60 },
  { label: '50×25', width: 50, height: 25 },
  { label: '45×25', width: 45, height: 25 },
]

function applyPreset(p) {
  createForm.width = p.width
  createForm.height = p.height
}

function createTemplate() {
  createForm.name = ''
  createForm.width = 60
  createForm.height = 40
  createForm.dpi = 203
  showCreateDialog.value = true
}

async function confirmCreate() {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  submitting.value = true
  try {
    const res = await apiCreate({
      templateName: createForm.name,
      labelWidth: createForm.width,
      labelHeight: createForm.height,
      dpi: createForm.dpi,
      templateData: '[]',
      status: 1,
      isDefault: 0
    })
    showCreateDialog.value = false
    ElMessage.success('模板创建成功')
    router.push({
      name: 'StickerTemplateDesigner',
      query: {
        id: res.data.id,
        w: createForm.width,
        h: createForm.height,
        dpi: createForm.dpi
      }
    })
  } catch (e) {
    ElMessage.error('创建失败：' + (e.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

function editTemplate(row) {
  router.push({
    name: 'StickerTemplateDesigner',
    query: { id: row.id, w: row.labelWidth, h: row.labelHeight, dpi: row.dpi }
  })
}

async function copyTemplate(row) {
  try {
    await apiCreate({
      templateName: row.templateName + ' (副本)',
      labelWidth: row.labelWidth,
      labelHeight: row.labelHeight,
      dpi: row.dpi,
      templateData: row.templateData || '[]',
      status: 1,
      isDefault: 0
    })
    ElMessage.success('模板已复制')
    loadData()
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

async function handleSetDefault(row) {
  try {
    await setDefaultTemplate(row.id)
    ElMessage.success('已设为默认模板')
    loadData()
  } catch (e) {
    ElMessage.error('设置失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${row.templateName}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await apiDelete(row.id)
    ElMessage.success('模板已删除')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.form-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.preset-sizes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preset-tag {
  cursor: pointer;
}

.preset-tag:hover {
  color: #0ea5e9;
  border-color: #0ea5e9;
}
</style>
