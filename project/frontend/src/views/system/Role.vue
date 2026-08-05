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
      <div class="perm-header">
        <span style="color:#78716c;font-size:0.875rem">
          角色：<strong>{{ permRoleName }}</strong>
        </span>
        <div class="perm-actions">
          <el-tooltip
            content="自动选中全部目录与菜单(含查询权限)，清空所有新增/编辑/删除等按钮权限，一键生成只读角色"
            placement="top"
          >
            <el-button size="small" plain @click="applyReadonly">仅查看权限</el-button>
          </el-tooltip>
          <el-tooltip
            content="默认关闭(独立模式)，可只勾菜单而不带按钮权限，便于配置只读角色；开启后勾父节点会自动带全部子节点"
            placement="top"
          >
            <span class="cascade-switch">
              父子联动
              <el-switch v-model="cascade" inline-prompt size="small" @change="onCascadeChange" />
            </span>
          </el-tooltip>
        </div>
      </div>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        node-key="id"
        :check-strictly="!cascade"
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
const refStore = useRefStore()

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
// 父子联动开关：true=级联勾选，false=可单独勾菜单不带子按钮(默认，便于配置只读角色)
const cascade = ref(false)

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
  refStore.loadRoleList(true)   // 刷新全局角色缓存，避免其它页面下拉读到旧数据
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
    refStore.loadRoleList(true)   // 刷新全局角色缓存，避免其它页面下拉读到旧数据
  } finally {
    submitting.value = false
  }
}

async function handlePermission(row) {
  permRoleId.value = row.id
  permRoleName.value = row.roleName
  checkedKeys.value = []
  cascade.value = false   // 默认独立模式，可直接单独勾选菜单不带按钮
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

/**
 * 切换父子联动时，需要按当前选中状态重新应用勾选：
 * 关闭联动(独立模式)时，把已选(含半选)的节点都显式勾上，避免丢失；
 * 重新开启联动(级联模式)时，从已选叶子重新计算，恢复自动推导父节点的行为。
 */
function onCascadeChange(val) {
  const tree = menuTreeRef.value
  if (!tree) return
  const currentKeys = [...tree.getCheckedKeys(), ...tree.getHalfCheckedKeys()]
  if (!val) {
    // 独立模式：显式勾选所有原来选中/半选的节点
    checkedKeys.value = currentKeys
    tree.setCheckedKeys(currentKeys)
  } else {
    // 级联模式：仅保留叶子，父节点由 el-tree 自动推导
    const leafOnly = currentKeys.filter(id => {
      const node = tree.getNode(id)
      return node && node.isLeaf
    })
    checkedKeys.value = leafOnly
    tree.setCheckedKeys(leafOnly)
  }
}

/**
 * 一键只读：勾选全部目录(menuType=0)与菜单(menuType=1，本身即 xxx:query 查询权限)，
 * 清空所有按钮(menuType=2，新增/编辑/删除等操作)。菜单节点自带查询权限，
 * 所以只保留 0/1 即可让角色"能看不能改"。切换到独立模式以避免级联把按钮又带上。
 */
function applyReadonly() {
  const keep = []
  function walk(nodes) {
    for (const n of nodes) {
      // menuType: 0=目录 1=菜单 2=按钮；保留目录和菜单，清掉按钮
      if (n.menuType !== 2) keep.push(n.id)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(menuTree.value)
  // 切到独立模式(check-strictly)，确保父勾选不会反向把子按钮带上
  cascade.value = false
  checkedKeys.value = keep
  nextTick(() => {
    const tree = menuTreeRef.value
    if (tree) tree.setCheckedKeys(keep)
  })
  ElMessage.success('已切换为只读权限：保留目录与菜单(查询)，已清空全部操作按钮')
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

<style scoped>
.perm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.perm-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cascade-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #78716c;
  font-size: 0.875rem;
  cursor: help;
}
</style>
