<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="选择模板">
          <el-select v-model="selectedTemplateName" placeholder="请选择打印模板" filterable style="width:300px" @change="onTemplateChange">
            <el-option
              v-for="name in templateNames"
              :key="name"
              :label="name"
              :value="name"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Plus" @click="handleAdd" :disabled="!selectedTemplateName">新增映射</el-button>
          <el-button type="success" :icon="CopyDocument" @click="handleCopyFrom" :disabled="!selectedTemplateName">从其他模板复制</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">字段映射配置</span>
          <span v-if="selectedTemplateName" style="font-size:13px;color:#909399;margin-left:12px">当前模板：{{ selectedTemplateName }}</span>
        </div>
      </template>
      <div class="table-responsive">
        <el-table :data="mappingList" border stripe v-loading="loading" empty-text="请选择模板后配置字段映射">
          <el-table-column type="index" label="#" width="60" align="center" />
          <el-table-column prop="dbField" label="数据字段" width="280">
            <template #default="{ row }">
              <el-select
                v-if="row._editing"
                v-model="row.dbField"
                filterable
                placeholder="选择数据字段"
                size="small"
                style="width:100%"
              >
                <el-option
                  v-for="field in availableFields"
                  :key="field.value"
                  :label="field.label"
                  :value="field.value"
                >
                  <span>{{ field.label }}</span>
                  <span style="float:right;color:#8492a6;font-size:12px">{{ field.value }}</span>
                </el-option>
              </el-select>
              <span v-else>{{ getFieldLabel(row.dbField) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="→" width="60" align="center">
            <template #default>
              <el-icon><Right /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="templateField" label="模板字段" width="280">
            <template #default="{ row }">
              <el-input v-if="row._editing" v-model="row.templateField" size="small" placeholder="如 货号" />
              <span v-else>{{ row.templateField }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="fieldFormat" label="格式化规则" min-width="200">
            <template #default="{ row }">
              <el-select v-if="row._editing" v-model="row.fieldFormat" size="small" clearable placeholder="不格式化" style="width:100%">
                <el-option label="不格式化" :value="null" />
                <el-option label="保留整数（.0f）" value=".0f" />
                <el-option label="保留1位小数（.1f）" value=".1f" />
                <el-option label="保留2位小数（.2f）" value=".2f" />
                <el-option label="保留3位小数（.3f）" value=".3f" />
              </el-select>
              <span v-else>{{ fieldFormatLabel(row.fieldFormat) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="120" align="center">
            <template #default="{ row }">
              <el-input-number v-if="row._editing" v-model="row.sortOrder" size="small" :min="0" controls-position="right" style="width:80px" />
              <span v-else>{{ row.sortOrder }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row, $index }">
              <template v-if="row._editing">
                <el-button type="success" plain size="small" @click="handleSave(row)">保存</el-button>
                <el-button size="small" @click="handleCancel($index)">取消</el-button>
              </template>
              <template v-else>
                <el-button type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES_LG"
          layout="total, sizes, prev, pager, next"
          @size-change="() => { pageNum = 1; loadMappings() }"
          @current-change="loadMappings"
        />
      </div>
    </el-card>

    <!-- 从其他模板复制 -->
    <el-dialog v-model="copyVisible" title="从其他模板复制" width="400px">
      <el-form label-width="80px">
        <el-form-item label="源模板">
          <el-select v-model="copySourceName" placeholder="选择源模板" filterable style="width:100%">
            <el-option
              v-for="name in copyTemplateOptions"
              :key="name"
              :label="name"
              :value="name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyVisible = false">取消</el-button>
        <el-button type="primary" :loading="copying" @click="confirmCopy">确定复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CopyDocument, Right } from '@element-plus/icons-vue'
import { getBrandTemplateNames, getFieldMappingPage, createFieldMapping, updateFieldMapping, deleteFieldMapping, deleteFieldMappingByTemplate, getAvailableFields } from '@/api/sticker'
import { PAGE_SIZES_LG } from '@/utils/appConfig'

defineOptions({ name: 'FieldMapping' })

const templateNames = ref([])
const selectedTemplateName = ref('')
const loading = ref(false)
const mappingList = ref([])
const availableFields = ref([])

// 分页状态
const pageNum = ref(1)
const pageSize = ref(PAGE_SIZES_LG[0])
const total = ref(0)

onMounted(async () => {
  await loadTemplateNames()
  await loadAvailableFields()
})

async function loadTemplateNames() {
  try {
    const { data } = await getBrandTemplateNames()
    templateNames.value = data || []
  } catch {
    templateNames.value = []
  }
}

async function loadAvailableFields() {
  try {
    const { data } = await getAvailableFields()
    availableFields.value = data || []
  } catch {
    availableFields.value = []
  }
}

function getFieldLabel(value) {
  const field = availableFields.value.find(f => f.value === value)
  return field ? `${field.label} (${value})` : value
}

const fieldFormatMap = {
  '.0f': '保留整数',
  '.1f': '保留1位小数',
  '.2f': '保留2位小数',
  '.3f': '保留3位小数'
}

function fieldFormatLabel(value) {
  return fieldFormatMap[value] || '不格式化'
}

function onTemplateChange() {
  pageNum.value = 1
  loadMappings()
}

async function loadMappings() {
  if (!selectedTemplateName.value) {
    mappingList.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const { data } = await getFieldMappingPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      templateId: selectedTemplateName.value
    })
    mappingList.value = (data?.records || []).map(item => ({ ...item, _editing: false }))
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  mappingList.value.push({
    id: '',
    templateId: selectedTemplateName.value,
    dbField: '',
    templateField: '',
    fieldFormat: '',
    sortOrder: mappingList.value.length,
    _editing: true,
    _isNew: true
  })
}

function handleEdit(row) {
  row._editing = true
  row._backup = { ...row }
}

function handleCancel(index) {
  const row = mappingList.value[index]
  if (row._isNew) {
    mappingList.value.splice(index, 1)
  } else {
    Object.assign(row, row._backup)
    row._editing = false
  }
}

async function handleSave(row) {
  if (!row.dbField?.trim() || !row.templateField?.trim()) {
    ElMessage.warning('数据字段和模板字段不能为空')
    return
  }

  try {
    if (row._isNew) {
      await createFieldMapping({
        templateId: selectedTemplateName.value,
        dbField: row.dbField.trim(),
        templateField: row.templateField.trim(),
        fieldFormat: row.fieldFormat?.trim() || null,
        sortOrder: row.sortOrder || 0
      })
      ElMessage.success('新增成功')
    } else {
      await updateFieldMapping(row.id, {
        dbField: row.dbField.trim(),
        templateField: row.templateField.trim(),
        fieldFormat: row.fieldFormat?.trim() || null,
        sortOrder: row.sortOrder || 0
      })
      ElMessage.success('修改成功')
    }
    row._editing = false
    row._isNew = false
    loadMappings()
  } catch {
    // interceptor shows error
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除映射「${getFieldLabel(row.dbField)} → ${row.templateField}」？`, '提示', { type: 'warning' })
  await deleteFieldMapping(row.id)
  ElMessage.success('删除成功')
  loadMappings()
}

// 从其他模板复制
const copyVisible = ref(false)
const copySourceName = ref('')
const copyTemplateOptions = ref([])
const copying = ref(false)

function handleCopyFrom() {
  copyTemplateOptions.value = templateNames.value.filter(name => name !== selectedTemplateName.value)
  copySourceName.value = ''
  copyVisible.value = true
}

async function confirmCopy() {
  if (!copySourceName.value) {
    ElMessage.warning('请选择源模板')
    return
  }

  copying.value = true
  try {
    const { data } = await getFieldMappingPage({
      pageNum: 1,
      pageSize: 9999,
      templateId: copySourceName.value
    })
    const sourceList = data?.records || []
    if (!sourceList.length) {
      ElMessage.warning('源模板没有字段映射配置')
      return
    }

    await ElMessageBox.confirm(`将复制 ${sourceList.length} 条映射到当前模板，已有映射将被覆盖？`, '提示', { type: 'warning' })

    // 删除现有映射
    await deleteFieldMappingByTemplate(selectedTemplateName.value)

    // 复制新映射
    for (const item of sourceList) {
      await createFieldMapping({
        templateId: selectedTemplateName.value,
        dbField: item.dbField,
        templateField: item.templateField,
        fieldFormat: item.fieldFormat,
        sortOrder: item.sortOrder
      })
    }

    ElMessage.success('复制成功')
    copyVisible.value = false
    pageNum.value = 1
    loadMappings()
  } catch {
    // cancelled or error
  } finally {
    copying.value = false
  }
}
</script>
