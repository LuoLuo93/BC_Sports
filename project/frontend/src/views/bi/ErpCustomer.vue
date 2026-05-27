<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="客户编码">
          <el-input v-model="query.customerCode" placeholder="请输入客户编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="query.customerName" placeholder="请输入客户名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户类型">
          <el-select v-model="query.customerType" placeholder="全部" clearable >
            <el-option label="企业" value="enterprise" />
            <el-option label="个人" value="individual" />
            <el-option label="经销商" value="dealer" />
          </el-select>
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
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">客户列表</span>
          <el-button v-if="hasPermission('bi:erpCustomer:add')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增客户</el-button>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="customerCode" label="客户编码" min-width="120" />
          <el-table-column prop="customerName" label="客户名称" min-width="140" />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="customerTypeTag(row.customerType)" size="small">{{ customerTypeLabel(row.customerType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="所在地区" min-width="160">
            <template #default="{ row }">
              {{ [row.province, row.city, row.district].filter(Boolean).join(' / ') || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="contactPerson" label="联系人" width="100" />
          <el-table-column prop="contactPhone" label="联系电话" width="130" />
          <el-table-column label="信用额度" width="120" align="right">
            <template #default="{ row }">
              {{ row.creditLimit != null ? '¥' + Number(row.creditLimit).toLocaleString() : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="账期(天)" width="90" align="center">
            <template #default="{ row }">{{ row.creditPeriod ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('bi:erpCustomer:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('bi:erpCustomer:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑客户' : '新增客户'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="客户编码" prop="customerCode">
          <el-input v-model="form.customerCode" placeholder="请输入客户编码" />
        </el-form-item>
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="form.customerName" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="客户类型" prop="customerType">
          <el-radio-group v-model="form.customerType">
            <el-radio value="enterprise">企业</el-radio>
            <el-radio value="individual">个人</el-radio>
            <el-radio value="dealer">经销商</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="省份"><el-input v-model="form.province" placeholder="省份" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市"><el-input v-model="form.city" placeholder="城市" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县"><el-input v-model="form.district" placeholder="区县" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址"><el-input v-model="form.address" placeholder="请输入详细地址" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactPerson" placeholder="请输入联系人" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" placeholder="请输入联系电话" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="信用额度"><el-input-number v-model="form.creditLimit" :min="0" :precision="2" class="w-full" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账期(天)"><el-input-number v-model="form.creditPeriod" :min="0" :max="365" class="w-full" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="9999" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'ErpCustomer' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErpCustomerPage, getErpCustomer, createErpCustomer, updateErpCustomer, deleteErpCustomer } from '@/api/erp'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/constants'

const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getErpCustomerPage, { customerCode: '', customerName: '', customerType: undefined, status: undefined })

const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const defaultForm = () => ({
  customerCode: '', customerName: '', customerType: 'enterprise', province: '', city: '', district: '',
  address: '', contactPerson: '', contactPhone: '', email: '', creditLimit: 0, creditPeriod: 0,
  sort: 0, status: 1, remark: ''
})
const form = reactive(defaultForm())
const rules = {
  customerCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  customerType: [{ required: true, message: '请选择客户类型', trigger: 'change' }]
}

const typeMap = { enterprise: ['企业', 'primary'], individual: ['个人', 'success'], dealer: ['经销商', 'warning'] }
function customerTypeLabel(t) { return typeMap[t]?.[0] || t }
function customerTypeTag(t) { return typeMap[t]?.[1] || 'info' }

function handleAdd() { isEdit.value = false; editId.value = null; Object.assign(form, defaultForm()); dialogVisible.value = true }

async function handleEdit(row) {
  const res = await getErpCustomer(row.id)
  isEdit.value = true; editId.value = row.id
  Object.assign(form, res.data); dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除客户「${row.customerName}」？`, '提示', { type: 'warning' })
  await deleteErpCustomer(row.id); ElMessage.success('删除成功'); loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) { await updateErpCustomer(editId.value, { ...form }) } else { await createErpCustomer({ ...form }) }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功'); dialogVisible.value = false; loadData()
  } finally { submitting.value = false }
}

onMounted(() => loadData())
</script>
