<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="员工姓名">
          <el-input v-model="query.staffName" placeholder="请输入员工姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="员工编号">
          <el-input v-model="query.staffNo" placeholder="请输入员工编号" clearable @keyup.enter="handleSearch" />
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
          <span class="card-header-title">{{ pageTitle }}</span>
          <div>
            <el-button v-if="hasPermission('ihr:exclusion:delete')" type="danger" size="small" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除</el-button>
            <el-button v-if="hasPermission('ihr:exclusion:add')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增排除</el-button>
          </div>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe @selection-change="onSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="staffName" label="员工姓名" min-width="100" />
          <el-table-column prop="staffNo" label="员工编号" min-width="110" />
          <el-table-column prop="reason" label="排除原因" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '生效' : '失效' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170" align="center">
            <template #default="{ row }">{{ row.createTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('ihr:exclusion:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('ihr:exclusion:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSearch" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑排除' : '新增排除'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="员工姓名" prop="staffName"><el-input v-model="form.staffName" placeholder="请输入员工姓名" /></el-form-item>
        <el-form-item label="员工编号" prop="staffNo"><el-input v-model="form.staffNo" placeholder="请输入员工编号" /></el-form-item>
        <el-form-item label="排除原因"><el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入排除原因" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status"><el-radio :value="1">生效</el-radio><el-radio :value="0">失效</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'IhrExclusion' })
import { ref, reactive, computed, onMounted } from 'vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getIhrExclusionPage, getIhrExclusion, createIhrExclusion, updateIhrExclusion, deleteIhrExclusion, batchDeleteIhrExclusion } from '@/api/ihr'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES } from '@/utils/appConfig'

const { hasPermission } = usePermission()

const route = useRoute()
const exclusionType = route.meta?.exclusionType || 1
const pageTitle = computed(() => exclusionType === 1 ? '入职排除名单' : '离职排除名单')

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getIhrExclusionPage, { staffName: '', staffNo: '', exclusionType })
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const selectedIds = ref([])

const defaultForm = () => ({ staffName: '', staffNo: '', reason: '', exclusionType, status: 1 })
const form = reactive(defaultForm())
const rules = { staffName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }], staffNo: [{ required: true, message: '请输入员工编号', trigger: 'blur' }] }

function onSelectionChange(rows) { selectedIds.value = rows.map(r => r.id) }

function handleAdd() { isEdit.value = false; editId.value = null; Object.assign(form, defaultForm()); dialogVisible.value = true }
async function handleEdit(row) {
  const res = await getIhrExclusion(row.id)
  isEdit.value = true; editId.value = row.id
  Object.assign(form, res.data); dialogVisible.value = true
}
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除该排除记录？`, '提示', { type: 'warning' })
  await deleteIhrExclusion(row.id); ElMessage.success('删除成功'); loadData()
}
async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 条记录？`, '提示', { type: 'warning' })
  await batchDeleteIhrExclusion(selectedIds.value); ElMessage.success('删除成功'); loadData()
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) { await updateIhrExclusion(editId.value, { ...form }) } else { await createIhrExclusion({ ...form }) }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功'); dialogVisible.value = false; loadData()
  } finally { submitting.value = false }
}

onMounted(() => loadData())
</script>
