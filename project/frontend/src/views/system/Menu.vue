<template>
  <div class="page-container">
    <!-- 工具栏 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">菜单管理</span>
          <div>
            <el-button size="small" @click="toggleExpandAll">{{ isExpandAll ? '折叠全部' : '展开全部' }}</el-button>
            <el-button v-if="hasPermission('menu:add')" type="primary" size="small" @click="handleAdd()">新增顶级菜单</el-button>
          </div>
        </div>
      </template>

      <div class="table-responsive">
        <el-table empty-text="暂无数据"
          v-if="refreshTable"
          v-loading="loading"
          :data="tableData"
          row-key="id"
          :tree-props="{ children: 'children' }"
          :default-expand-all="isExpandAll"
          border
          stripe
        >
          <el-table-column prop="menuName" label="菜单名称" min-width="200">
            <template #default="{ row }">
              <span class="inline-flex-center">
                <el-icon v-if="row.icon" :style="iconColorStyle(row)"><component :is="getIcon(row.icon)" /></el-icon>
                {{ row.menuName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.menuType === 0" type="" size="small">目录</el-tag>
              <el-tag v-else-if="row.menuType === 1" type="success" size="small">菜单</el-tag>
              <el-tag v-else type="warning" size="small">按钮</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路由路径" min-width="150">
            <template #default="{ row }">
              <el-tag v-if="row.path" size="small" type="info">{{ row.path }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="permission" label="权限标识" min-width="150">
            <template #default="{ row }">
              <el-tag v-if="row.permission" size="small">{{ row.permission }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="可见" width="70" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.visible === 1" type="success" size="small">是</el-tag>
              <el-tag v-else type="info" size="small">否</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" align="center" fixed="right">
            <template #default="{ row }">
              <template v-if="row.menuType !== 2">
                <el-button v-if="hasPermission('menu:add')" type="warning" plain size="small" @click="handleAdd(row)">新增子菜单</el-button>
              </template>
              <el-button v-if="hasPermission('menu:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('menu:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="menuTreeOptions"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            placeholder="顶级菜单"
            check-strictly
            clearable
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 2" label="图标">
          <el-input v-model="form.icon" placeholder="如 bi-speedometer2">
            <template #prefix>
              <el-icon v-if="form.icon"><component :is="getIcon(form.icon)" /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="form.menuType !== 2" label="路由路径">
          <el-input v-model="form.path" placeholder="如 /system/user" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 2" label="权限标识">
          <el-input v-model="form.permission" placeholder="如 system:user:add" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 2" label="图标颜色">
          <el-select v-model="form.iconColor" placeholder="默认" clearable class="w-full">
            <el-option label="蓝色 (primary)" value="primary" />
            <el-option label="绿色 (success)" value="success" />
            <el-option label="橙色 (warning)" value="warning" />
            <el-option label="红色 (danger)" value="danger" />
            <el-option label="紫色" value="purple" />
            <el-option label="青色" value="cyan" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.menuType !== 2" label="是否可见">
          <el-radio-group v-model="form.visible">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
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
defineOptions({ name: 'MenuManagement' })
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMenuIcon, getIconColorStyle } from '@/utils/iconMap'
import { getMenuTree, getMenu, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const tableData = ref([])
const isExpandAll = ref(true)
const refreshTable = ref(true)

const defaultForm = () => ({
  parentId: '0', menuName: '', icon: '', menuType: 0,
  path: '', permission: '', sort: 0, status: 1, visible: 1,
  iconColor: ''
})
const form = reactive(defaultForm())
const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
}

const menuTreeOptions = computed(() => {
  const root = { id: '0', menuName: '顶级菜单', children: tableData.value }
  return [root]
})

function getIcon(icon) {
  return getMenuIcon(icon)
}

function iconColorStyle(row) {
  return getIconColorStyle(row.iconColor)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMenuTree()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  await nextTick()
  refreshTable.value = true
}

function handleAdd(parent) {
  isEdit.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  if (parent) form.parentId = parent.id
  dialogVisible.value = true
}

async function handleEdit(row) {
  const res = await getMenu(row.id)
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, res.data)
  dialogVisible.value = true
}

async function handleDelete(row) {
  if (row.children?.length) {
    ElMessage.warning('该菜单下有子菜单，无法删除')
    return
  }
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMenu(editId.value, { ...form })
    } else {
      await createMenu({ ...form })
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
