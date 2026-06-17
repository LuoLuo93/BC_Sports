<template>
  <el-dialog v-model="visible" :title="`字段映射 - ${templateName}`" width="800px" destroy-on-close>
    <div style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center">
      <el-button type="primary" size="small" @click="handleAdd">新增映射</el-button>
      <el-button type="success" size="small" @click="handleCopyFrom">从其他模板复制</el-button>
    </div>

    <el-table :data="mappingList" border size="small" v-loading="loading">
      <el-table-column prop="dbField" label="数据库字段" width="200">
        <template #default="{ row }">
          <el-input v-if="row._editing" v-model="row.dbField" size="small" />
          <span v-else>{{ row.dbField }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="templateField" label="模板字段" width="200">
        <template #default="{ row }">
          <el-input v-if="row._editing" v-model="row.templateField" size="small" />
          <span v-else>{{ row.templateField }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="fieldFormat" label="格式化规则" width="180">
        <template #default="{ row }">
          <el-input v-if="row._editing" v-model="row.fieldFormat" size="small" placeholder="如 price:.2f" />
          <span v-else>{{ row.fieldFormat || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" align="center">
        <template #default="{ row }">
          <el-input-number v-if="row._editing" v-model="row.sortOrder" size="small" :min="0" controls-position="right" style="width:60px" />
          <span v-else>{{ row.sortOrder }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
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

    <!-- 从其他模板复制 -->
    <el-dialog v-model="copyVisible" title="从其他模板复制" width="400px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="选择模板">
          <el-select v-model="copySourceId" placeholder="选择模板" style="width:100%">
            <el-option v-for="t in templateOptions" :key="t.id" :label="t.templateName" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyVisible = false">取消</el-button>
        <el-button type="primary" :loading="copying" @click="confirmCopy">确定复制</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFieldMappingList, createFieldMapping, updateFieldMapping, deleteFieldMapping } from '@/api/sticker'
import { getTemplateList } from '@/api/sticker'

const props = defineProps({
  templateId: { type: String, default: '' },
  templateName: { type: String, default: '' }
})

const visible = defineModel({ type: Boolean, default: false })
const loading = ref(false)
const mappingList = ref([])

watch(visible, (val) => {
  if (val && props.templateId) {
    loadMappings()
  }
})

async function loadMappings() {
  loading.value = true
  try {
    const { data } = await getFieldMappingList(props.templateId)
    mappingList.value = (data || []).map(item => ({ ...item, _editing: false }))
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  mappingList.value.push({
    id: '',
    templateId: props.templateId,
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
    ElMessage.warning('数据库字段和模板字段不能为空')
    return
  }

  try {
    if (row._isNew) {
      await createFieldMapping({
        templateId: row.templateId,
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
  await ElMessageBox.confirm(`确定删除映射「${row.dbField} → ${row.templateField}」？`, '提示', { type: 'warning' })
  await deleteFieldMapping(row.id)
  ElMessage.success('删除成功')
  loadMappings()
}

// 从其他模板复制
const copyVisible = ref(false)
const copySourceId = ref('')
const templateOptions = ref([])
const copying = ref(false)

async function handleCopyFrom() {
  try {
    const { data } = await getTemplateList()
    templateOptions.value = (data || []).filter(t => t.id !== props.templateId)
    copySourceId.value = ''
    copyVisible.value = true
  } catch {
    templateOptions.value = []
  }
}

async function confirmCopy() {
  if (!copySourceId.value) {
    ElMessage.warning('请选择源模板')
    return
  }

  copying.value = true
  try {
    const { data } = await getFieldMappingList(copySourceId.value)
    if (!data?.length) {
      ElMessage.warning('源模板没有字段映射配置')
      return
    }

    await ElMessageBox.confirm(`将复制 ${data.length} 条映射到当前模板，已有映射将被覆盖？`, '提示', { type: 'warning' })

    // 删除现有映射
    for (const item of mappingList.value) {
      if (item.id) {
        await deleteFieldMapping(item.id)
      }
    }

    // 复制新映射
    for (const item of data) {
      await createFieldMapping({
        templateId: props.templateId,
        dbField: item.dbField,
        templateField: item.templateField,
        fieldFormat: item.fieldFormat,
        sortOrder: item.sortOrder
      })
    }

    ElMessage.success('复制成功')
    copyVisible.value = false
    loadMappings()
  } catch {
    // cancelled or error
  } finally {
    copying.value = false
  }
}
</script>
