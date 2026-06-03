<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card search-card--compact">
      <el-form :model="query" inline>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="query.nickname" placeholder="请输入昵称" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="请输入手机号" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">用户列表</span>
          <el-button v-if="hasPermission('user:add')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增用户</el-button>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column label="部门" width="120">
            <template #default="{ row }">
              <span v-if="row.deptName" class="dept-tag">{{ row.deptName }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="角色" min-width="160">
            <template #default="{ row }">
              <div v-if="row.roleNames?.length" class="role-tags">
                <span v-for="name in row.roleNames.split(',')" :key="name" class="role-tag">{{ name }}</span>
              </div>
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
          <el-table-column label="操作" width="260" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('user:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('user:resetPassword')" type="warning" plain size="small" @click="handleResetPwd(row)">重置密码</el-button>
              <el-button v-if="hasPermission('user:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="480px" destroy-on-close>
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="100px">
        <el-form-item label="用户">
          <span>{{ resetPwdTarget?.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="resetPwdVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="resetPwdSubmitting" @click="handleResetPwdSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="720px" class="user-dialog" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="user-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="!isEdit" label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item v-else label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="!isEdit">
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="部门">
              <el-tree-select
                v-model="form.deptId"
                :data="deptOptions"
                :props="{ label: 'deptName', value: 'id', children: 'children' }"
                placeholder="请选择部门"
                check-strictly
                clearable
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :max="9999" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="角色">
          <el-checkbox-group v-model="form.roleIds" class="role-checkbox-group">
            <el-checkbox v-for="role in roleList" :key="role.id" :value="role.id">{{ role.roleName }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
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
defineOptions({ name: 'UserManagement' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, getUser, createUser, updateUser, deleteUser, resetPassword, getUserRoles } from '@/api/user'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/appConfig'
import { useRefStore } from '@/stores/reference'

const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getUserPage, { username: '', nickname: '', phone: '', status: undefined })

const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const roleList = ref([])
const deptOptions = ref([])

const defaultForm = () => ({
  username: '', password: '', confirmPassword: '', nickname: '', email: '', phone: '',
  deptId: '', roleIds: [], sort: 0, status: 1, remark: ''
})
const form = reactive(defaultForm())

const rules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: isEdit.value ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: isEdit.value ? [] : [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (rule, value, callback) => value !== form.password ? callback(new Error('两次密码不一致')) : callback(), trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}))

// 重置密码
const resetPwdVisible = ref(false)
const resetPwdSubmitting = ref(false)
const resetPwdFormRef = ref(null)
const resetPwdTarget = ref(null)
const resetPwdForm = reactive({ newPassword: '' })
const resetPwdRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

const refStore = useRefStore()

async function loadOptions() {
  const [roles, depts] = await Promise.all([
    refStore.loadRoleList(),
    refStore.loadDeptTree()
  ])
  roleList.value = roles
  deptOptions.value = depts
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row) {
  const [userRes, roleRes] = await Promise.all([getUser(row.id), getUserRoles(row.id)])
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    ...userRes.data,
    roleIds: roleRes.data || []
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleResetPwd(row) {
  resetPwdTarget.value = row
  resetPwdForm.newPassword = ''
  resetPwdVisible.value = true
}

async function handleResetPwdSubmit() {
  const valid = await resetPwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  resetPwdSubmitting.value = true
  try {
    await resetPassword(resetPwdTarget.value.id, { newPassword: resetPwdForm.newPassword })
    ElMessage.success('密码已重置')
    resetPwdVisible.value = false
  } finally {
    resetPwdSubmitting.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateUser(editId.value, { ...form })
    } else {
      const { confirmPassword, ...data } = form
      await createUser(data)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
  loadOptions()
})
</script>

<style scoped>
/* 搜索栏紧凑 */
.search-card--compact :deep(.el-card__body) {
  padding: 10px 16px;
}
.search-card--compact :deep(.el-form-item) {
  margin-bottom: 0;
}
.search-card--compact :deep(.el-form-item__label) {
  font-size: 13px;
}
.search-card--compact :deep(.el-input__wrapper) {
  padding: 4px 10px;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.role-tag {
  display: inline-block;
  padding: 2px 10px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #047857;
}
.dept-tag {
  display: inline-block;
  padding: 2px 10px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #1d4ed8;
}

/* 角色选择 */
.role-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.role-checkbox-group .el-checkbox {
  margin-right: 0;
  padding: 6px 16px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.role-checkbox-group .el-checkbox:hover {
  background: #d1fae5;
  border-color: #6ee7b7;
}

.role-checkbox-group .el-checkbox.is-checked {
  background: #10b981;
  border-color: #10b981;
}

.role-checkbox-group .el-checkbox.is-checked .el-checkbox__label {
  color: #fff;
}

.role-checkbox-group .el-checkbox__label {
  font-weight: 500;
  color: #047857;
}

.role-checkbox-group .el-checkbox__inner {
  display: none;
}

/* label 样式 */
.user-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
}

/* 行间距 */
.user-form :deep(.el-row) {
  margin-bottom: 8px;
}
.user-form :deep(.el-form-item) {
  margin-bottom: 18px;
}
</style>

