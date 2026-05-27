<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="部门名称">
          <el-input v-model="query.deptName" placeholder="请输入部门名称" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable >
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">部门列表</span>
          <el-button v-if="hasPermission('dept:add')" type="primary" size="small" :icon="Plus" @click="handleAdd()">新增部门</el-button>
        </div>
      </template>

      <div class="table-responsive">
        <el-table
          v-loading="loading"
          :data="tableData"
          row-key="id"
          :tree-props="{ children: 'children' }"
          default-expand-all
          border
          stripe
          empty-text="暂无数据"
        >
          <el-table-column prop="deptName" label="部门名称" min-width="200" />
          <el-table-column prop="sort" label="排序" width="80" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="leader" label="负责人" width="120" />
          <el-table-column prop="phone" label="联系电话" width="140" />
          <el-table-column prop="email" label="邮箱" width="180" />
          <el-table-column label="创建时间" width="170" align="center">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('dept:add')" type="warning" plain size="small" @click="handleAdd(row)">新增子部门</el-button>
              <el-button v-if="hasPermission('dept:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('dept:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="deptTreeOptions"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="顶级部门"
            check-strictly
            clearable
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'DeptManagement' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptList, getDept, createDept, updateDept, deleteDept } from '@/api/dept'
import { formatTime } from '@/utils/format'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const tableData = ref([])

const query = reactive({ deptName: '', status: undefined })

const defaultForm = () => ({
  parentId: '0',
  deptName: '',
  sort: 0,
  leader: '',
  phone: '',
  email: '',
  status: 1
})

const form = reactive(defaultForm())

const rules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

const deptTreeOptions = computed(() => {
  const root = { id: '0', deptName: '顶级部门', children: tableData.value }
  return [root]
})

async function loadData() {
  loading.value = true
  try {
    const res = await getDeptList(query)
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.deptName = ''
  query.status = undefined
  loadData()
}

function handleAdd(parent) {
  isEdit.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  if (parent) form.parentId = parent.id
  dialogVisible.value = true
}

async function handleEdit(row) {
  const res = await getDept(row.id)
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, res.data)
  dialogVisible.value = true
}

async function handleDelete(row) {
  if (row.children?.length) {
    ElMessage.warning('该部门下有子部门，无法删除')
    return
  }
  await ElMessageBox.confirm(`确定删除部门「${row.deptName}」？`, '提示', { type: 'warning' })
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDept(editId.value, { ...form })
    } else {
      await createDept({ ...form })
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => loadData())
</script>
