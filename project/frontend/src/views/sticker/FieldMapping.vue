<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增映射</el-button>
        </el-form-item>
        <el-form-item>
          <span style="font-size:12px;color:#909399">字段映射为全局配置，所有打印模板共用同一份</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">字段映射配置</span>
        </div>
      </template>
      <div class="table-responsive">
        <el-table :data="mappingList" border stripe v-loading="loading" empty-text="暂无映射，点击新增映射">
          <el-table-column type="index" label="#" width="60" align="center" />
          <el-table-column prop="dbField" label="数据字段" width="280">
            <template #default="{ row }">
              <el-select
                v-if="row._editing"
                v-model="row.dbField"
                filterable
                clearable
                placeholder="留空则用默认值"
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
              <span v-else>{{ row.dbField ? getFieldLabel(row.dbField) : '（固定值）' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="→" width="60" align="center">
            <template #default>
              <el-icon><Right /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="templateField" label="模板字段" width="240">
            <template #default="{ row }">
              <el-input v-if="row._editing" v-model="row.templateField" size="small" placeholder="如 货号" />
              <span v-else>{{ row.templateField }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="defaultValue" label="默认值" min-width="160">
            <template #default="{ row }">
              <el-input v-if="row._editing" v-model="row.defaultValue" size="small" placeholder="固定值，留空则从货品取" />
              <span v-else>{{ row.defaultValue || '-' }}</span>
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
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          @size-change="() => { pageNum = 1; loadMappings() }"
          @current-change="loadMappings"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Right } from '@element-plus/icons-vue'
import { getFieldMappingPage, createFieldMapping, updateFieldMapping, deleteFieldMapping, getAvailableFields } from '@/api/sticker'
import { PAGE_SIZES } from '@/utils/appConfig'

defineOptions({ name: 'FieldMapping' })

const loading = ref(false)
const mappingList = ref([])
const availableFields = ref([])

// 分页状态
const pageNum = ref(1)
const pageSize = ref(PAGE_SIZES[0])
const total = ref(0)

onMounted(async () => {
  await loadAvailableFields()
  loadMappings()
})

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

async function loadMappings() {
  loading.value = true
  try {
    const { data } = await getFieldMappingPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value
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
    dbField: '',
    templateField: '',
    defaultValue: '',
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
  if (!row.templateField?.trim()) {
    ElMessage.warning('模板字段不能为空')
    return
  }
  if (!row.dbField?.trim() && !row.defaultValue?.trim()) {
    ElMessage.warning('数据字段和默认值至少填一个')
    return
  }

  try {
    if (row._isNew) {
      await createFieldMapping({
        dbField: row.dbField?.trim() || null,
        templateField: row.templateField.trim(),
        defaultValue: row.defaultValue?.trim() || null,
        fieldFormat: row.fieldFormat?.trim() || null,
        sortOrder: row.sortOrder || 0
      })
      ElMessage.success('新增成功')
    } else {
      await updateFieldMapping(row.id, {
        dbField: row.dbField?.trim() || null,
        templateField: row.templateField.trim(),
        defaultValue: row.defaultValue?.trim() || null,
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
</script>
