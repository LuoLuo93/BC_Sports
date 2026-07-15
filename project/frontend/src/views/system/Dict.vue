<template>
  <div class="page-container">
    <el-row :gutter="16" class="dict-row">
      <!-- 左侧：字典类型 -->
      <el-col :span="8">
        <el-card shadow="never" class="dict-type-card">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">字典类型</span>
              <el-button v-if="hasPermission('system:dict:add')" type="primary" size="small" :icon="Plus" @click="handleAddType">新增</el-button>
            </div>
          </template>
          <el-input v-model="typeSearch" placeholder="搜索字典类型" clearable class="mb-12" />
          <div class="type-list">
            <div
              v-for="item in filteredTypes"
              :key="item.id"
              class="type-item"
              :class="{ active: currentTypeId === item.id }"
              @click="selectType(item)"
            >
              <div class="type-name">{{ item.dictName }}</div>
              <div class="type-code">{{ item.dictType }}</div>
              <div class="type-actions">
                <el-button v-if="hasPermission('system:dict:edit')" type="primary" plain size="small" @click.stop="handleEditType(item)">编辑</el-button>
                <el-button v-if="hasPermission('system:dict:delete')" type="danger" plain size="small" @click.stop="handleDeleteType(item)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="filteredTypes.length === 0" description="暂无数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：字典数据 -->
      <el-col :span="16">
        <el-card shadow="never" class="dict-data-card">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">
                字典数据
                <el-tag v-if="currentDictType" size="small" class="ml-8">{{ currentDictType.dictName }}</el-tag>
              </span>
              <el-button v-if="hasPermission('system:dict:add')" type="primary" size="small" :icon="Plus" :disabled="!currentDictType" @click="handleAddData">新增</el-button>
            </div>
          </template>

          <div class="table-responsive">
            <el-table v-loading="dataLoading" :data="dataList" border stripe empty-text="暂无数据" height="100%">
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="dictLabel" label="字典标签" min-width="140" />
              <el-table-column prop="dictValue" label="字典值" min-width="120" />
              <el-table-column prop="sort" label="排序" width="80" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '正常' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="创建时间" width="170" align="center">
                <template #default="{ row }">
                  {{ formatTime(row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="hasPermission('system:dict:edit')" type="primary" plain size="small" @click="handleEditData(row)">编辑</el-button>
                  <el-button v-if="hasPermission('system:dict:delete')" type="danger" plain size="small" @click="handleDeleteData(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-wrapper--sm" v-if="currentDictType">
            <el-pagination v-model:current-page="dataQuery.pageNum" v-model:page-size="dataQuery.pageSize" :total="dataTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next" @size-change="handleDataSearch" @current-change="loadDataList" />
          </div>
          <el-empty v-if="!currentDictType" description="请先选择左侧字典类型" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="isEditType ? '编辑字典类型' : '新增字典类型'" width="480px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="请输入字典类型编码" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="typeDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="typeSubmitting" @click="handleSubmitType">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataDialogVisible" :title="isEditData ? '编辑字典数据' : '新增字典数据'" width="480px" destroy-on-close>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="100px">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="请输入字典标签" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="请输入字典值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="dataDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="dataSubmitting" @click="handleSubmitData">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'DictManagement' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatTime } from '@/utils/format'
import {
  getDictTypeList, getDictType, createDictType, updateDictType, deleteDictType,
  getDictDataPage, getDictData, createDictData, updateDictData, deleteDictData
} from '@/api/dict'
import { Plus } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { useDictStore } from '@/stores/dict'
import { PAGE_SIZES } from '@/utils/appConfig'

const { hasPermission } = usePermission()
const dictStore = useDictStore()

// --- 字典类型 ---
const typeList = ref([])
const typeSearch = ref('')
const currentTypeId = ref(null)
const currentDictType = ref(null)
const typeDialogVisible = ref(false)
const isEditType = ref(false)
const editTypeId = ref(null)
const typeFormRef = ref(null)
const typeSubmitting = ref(false)

const filteredTypes = computed(() => {
  if (!typeSearch.value) return typeList.value
  const kw = typeSearch.value.toLowerCase()
  return typeList.value.filter(t =>
    t.dictName.toLowerCase().includes(kw) || t.dictType.toLowerCase().includes(kw)
  )
})

const defaultTypeForm = () => ({ dictName: '', dictType: '', remark: '' })
const typeForm = reactive(defaultTypeForm())
const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型编码', trigger: 'blur' }]
}

async function loadTypes() {
  const res = await getDictTypeList()
  typeList.value = res.data || []
}

function selectType(item) {
  currentTypeId.value = item.id
  currentDictType.value = item
  dataQuery.dictType = item.dictType
  dataQuery.pageNum = 1
  loadDataList()
}

function handleAddType() {
  isEditType.value = false
  editTypeId.value = null
  Object.assign(typeForm, defaultTypeForm())
  typeDialogVisible.value = true
}

async function handleEditType(item) {
  isEditType.value = true
  editTypeId.value = item.id
  const res = await getDictType(item.id)
  Object.assign(typeForm, { dictName: res.data.dictName, dictType: res.data.dictType, remark: res.data.remark || '' })
  typeDialogVisible.value = true
}

async function handleDeleteType(item) {
  await ElMessageBox.confirm(`确定删除字典类型「${item.dictName}」？关联的字典数据也会被删除。`, '提示', { type: 'warning' })
  await deleteDictType(item.id)
  ElMessage.success('删除成功')
  if (currentTypeId.value === item.id) {
    currentTypeId.value = null
    currentDictType.value = null
    dataList.value = []
  }
  loadTypes()
}

async function handleSubmitType() {
  const valid = await typeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  typeSubmitting.value = true
  try {
    if (isEditType.value) {
      await updateDictType({ id: editTypeId.value, ...typeForm })
    } else {
      await createDictType({ ...typeForm })
    }
    ElMessage.success(isEditType.value ? '更新成功' : '创建成功')
    typeDialogVisible.value = false
    loadTypes()
  } finally {
    typeSubmitting.value = false
  }
}

// --- 字典数据 ---
const { loading: dataLoading, tableData: dataList, total: dataTotal, query: dataQuery, loadData: loadDataList, handleSearch: handleDataSearch } = usePageQuery(getDictDataPage, { dictType: '' })
const dataDialogVisible = ref(false)
const isEditData = ref(false)
const editDataId = ref(null)
const dataFormRef = ref(null)
const dataSubmitting = ref(false)

const defaultDataForm = () => ({ dictLabel: '', dictValue: '', sort: 0, status: 1, remark: '' })
const dataForm = reactive(defaultDataForm())
const dataRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }]
}

function handleAddData() {
  isEditData.value = false
  editDataId.value = null
  Object.assign(dataForm, defaultDataForm())
  dataDialogVisible.value = true
}

async function handleEditData(row) {
  isEditData.value = true
  editDataId.value = row.id
  const res = await getDictData(row.id)
  Object.assign(dataForm, {
    dictLabel: res.data.dictLabel,
    dictValue: res.data.dictValue,
    sort: res.data.sort,
    status: res.data.status,
    remark: res.data.remark || ''
  })
  dataDialogVisible.value = true
}

async function handleDeleteData(row) {
  await ElMessageBox.confirm(`确定删除字典数据「${row.dictLabel}」？`, '提示', { type: 'warning' })
  await deleteDictData(row.id)
  ElMessage.success('删除成功')
  // 清除字典缓存
  dictStore.clearCache()
  loadDataList()
}

async function handleSubmitData() {
  const valid = await dataFormRef.value?.validate().catch(() => false)
  if (!valid) return
  dataSubmitting.value = true
  try {
    const payload = { ...dataForm, dictType: currentDictType.value.dictType }
    if (isEditData.value) {
      await updateDictData({ id: editDataId.value, ...payload })
    } else {
      await createDictData(payload)
    }
    ElMessage.success(isEditData.value ? '更新成功' : '创建成功')
    dataDialogVisible.value = false
    // 清除字典缓存，确保其他页面能获取最新数据
    dictStore.clearCache()
    loadDataList()
  } finally {
    dataSubmitting.value = false
  }
}

onMounted(() => loadTypes())
</script>

<style scoped>
/* el-row 默认非 flex item，改 display:flex 撑满 page-container 剩余空间 */
.dict-row {
  flex: 1;
  min-height: 0;
  display: flex;
}
.dict-row :deep(.el-col) {
  display: flex;
}
.dict-type-card,
.dict-data-card {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.dict-type-card :deep(.el-card__body),
.dict-data-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* card body 自身不滚动，滚动交给内部 type-list / el-table */
}
/* 左侧类型列表：撑满剩余空间，内部滚动 */
.type-list { flex: 1; min-height: 0; overflow-y: auto; }
.type-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 12px; border-radius: 8px; cursor: pointer;
  transition: background 0.15s; border-bottom: 1px solid var(--bc-bg);
}
.type-item:hover { background: var(--bc-bg); }
.type-item.active { background: var(--bc-bg-primary-soft); border-left: 3px solid var(--bc-primary-light); }
.type-name { font-weight: 600; font-size: 0.875rem; flex-shrink: 0; }
.type-code { flex: 1; font-size: 0.75rem; color: var(--bc-text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.type-actions { flex-shrink: 0; }
/* 右侧搜索框固定 */
.dict-data-card :deep(.el-card__body > .table-responsive) {
  flex: 1;
  min-height: 0;
}
.dict-data-card :deep(.el-card__body > .pagination-wrapper--sm) {
  flex-shrink: 0;
}
</style>
