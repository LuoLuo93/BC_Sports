<template>
  <SearchPage title="角色列表" v-model:page-num="query.pageNum" v-model:page-size="query.pageSize"
    :total="total" @page-change="loadData">
    <template #search>
      <el-form-item label="角色名称">
        <el-input v-model="query.roleName" placeholder="请输入角色名称" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="角色编码">
        <el-input v-model="query.roleCode" placeholder="请输入角色编码" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable >
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
      </el-form-item>
    </template>
    <template #actions>
      <el-button v-if="hasPermission('role:add')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增角色</el-button>
    </template>
    <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="roleName" label="角色名称" min-width="140" />
          <el-table-column prop="roleCode" label="角色编码" min-width="140">
            <template #default="{ row }">
              <el-tag size="small">{{ row.roleCode }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('role:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('role:assignPermission')" type="warning" plain size="small" @click="handlePermission(row)">分配权限</el-button>
              <el-button v-if="hasPermission('role:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
  </SearchPage>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 权限分配弹窗 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="480px" destroy-on-close>
      <div style="margin-bottom:12px;color:#78716c;font-size:0.875rem">
        角色：<strong>{{ permRoleName }}</strong>
      </div>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedKeys"
        :default-expand-all="true"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="permDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="permSubmitting" @click="handlePermSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
</template>

<script setup>
defineOptions({ name: 'RoleManagement' })
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchPage from '@/components/SearchPage.vue'
import { getRolePage, getRole, createRole, updateRole, deleteRole, getRolePermissions, updateRolePermissions } from '@/api/role'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { useRefStore } from '@/stores/reference'

const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getRolePage, { roleName: '', roleCode: '', status: undefined })

const permSubmitting = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const defaultForm = () => ({
  roleName: '', roleCode: '', description: '', sort: 0, status: 1
})
const form = reactive(defaultForm())
const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

// 权限分配
const permDialogVisible = ref(false)
const permRoleId = ref(null)
const permRoleName = ref('')
const menuTree = ref([])
const checkedKeys = ref([])
const menuTreeRef = ref(null)

function handleAdd() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row) {
  const res = await getRole(row.id)
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, res.data)
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRole(editId.value, { ...form })
    } else {
      await createRole({ ...form })
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handlePermission(row) {
  permRoleId.value = row.id
  permRoleName.value = row.roleName
  checkedKeys.value = []
  permDialogVisible.value = true

  const refStore = useRefStore()
  const [treeData, permRes] = await Promise.all([
    refStore.loadFullMenuTree(),
    getRolePermissions(row.id)
  ])
  menuTree.value = treeData

  // el-tree 的 checkedKeys 只设置叶子节点，父节点会自动计算
  const leafKeys = getAllLeafIds(menuTree.value, new Set(permRes.data || []))
  await nextTick()
  checkedKeys.value = leafKeys
}

function getAllLeafIds(tree, permSet) {
  const leaves = []
  function walk(nodes) {
    for (const node of nodes) {
      if (permSet.has(node.id)) {
        if (node.children?.length) {
          walk(node.children)
        } else {
          leaves.push(node.id)
        }
      }
    }
  }
  walk(tree)
  return leaves
}

async function handlePermSubmit() {
  permSubmitting.value = true
  try {
    const checked = menuTreeRef.value.getCheckedKeys()
    const halfChecked = menuTreeRef.value.getHalfCheckedKeys()
    const allKeys = [...checked, ...halfChecked]
    await updateRolePermissions(permRoleId.value, allKeys)
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
  } finally {
    permSubmitting.value = false
  }
}

onMounted(() => loadData())
</script>
