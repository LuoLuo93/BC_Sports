<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="店铺代码">
          <el-input v-model="query.shopCode" placeholder="请输入店铺代码" clearable style="min-width:150px;max-width:200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="店铺名称">
          <el-input v-model="query.shopName" placeholder="请输入店铺名称" clearable style="min-width:150px;max-width:200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="联营老板">
          <el-input v-model="query.shopBoss" placeholder="请输入联营老板" clearable style="min-width:150px;max-width:200px" @keyup.enter="handleSearch" />
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
          <span class="card-header-title">揽众客户押金资料</span>
          <el-button v-if="hasPermission('erp:lzCustomer:add')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="SHOPCODE" label="店铺代码" width="140" show-overflow-tooltip />
          <el-table-column prop="SHOPNAME" label="店铺/客户名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="SHOPBOSS" label="门店所属联营老板" min-width="160" show-overflow-tooltip />
          <el-table-column prop="FUNDINGLIMIT" label="资金额度" width="120" />
          <el-table-column prop="FUNDINGRATIO" label="资金倍率" width="120" />
          <el-table-column label="操作" width="160" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('erp:lzCustomer:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('erp:lzCustomer:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑揽众资料' : '新增揽众资料'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="店铺代码" prop="shopCode">
          <el-input v-model="form.shopCode" placeholder="请输入店铺代码" />
        </el-form-item>
        <el-form-item label="店铺/客户名称" prop="shopName">
          <el-input v-model="form.shopName" placeholder="请输入店铺/客户名称" />
        </el-form-item>
        <el-form-item label="门店所属联营老板" prop="shopBoss">
          <el-input v-model="form.shopBoss" placeholder="请输入门店所属联营老板" />
        </el-form-item>
        <el-form-item label="资金额度" prop="fundingLimit">
          <el-input v-model="form.fundingLimit" placeholder="请输入资金额度" />
        </el-form-item>
        <el-form-item label="资金倍率" prop="fundingRatio">
          <el-input v-model="form.fundingRatio" placeholder="请输入资金倍率" />
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
defineOptions({ name: 'LzCustomer' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import { getLzCustomerPage, getLzCustomer, createLzCustomer, updateLzCustomer, deleteLzCustomer } from '@/api/erp'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/appConfig'

const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getLzCustomerPage, {
  shopCode: '', shopName: '', shopBoss: ''
})

const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const defaultForm = () => ({
  shopCode: '', shopName: '', shopBoss: '', fundingLimit: '', fundingRatio: ''
})
const form = reactive(defaultForm())
const rules = {
  shopCode: [{ required: true, message: '请输入店铺代码', trigger: 'blur' }],
  shopName: [{ required: true, message: '请输入店铺/客户名称', trigger: 'blur' }]
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row) {
  const res = await getLzCustomer(row.ID)
  isEdit.value = true
  editId.value = row.ID
  const d = res.data || {}
  form.shopCode = d.SHOPCODE || ''
  form.shopName = d.SHOPNAME || ''
  form.shopBoss = d.SHOPBOSS || ''
  form.fundingLimit = d.FUNDINGLIMIT || ''
  form.fundingRatio = d.FUNDINGRATIO || ''
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.SHOPNAME || row.SHOPCODE}」？`, '提示', { type: 'warning' })
  await deleteLzCustomer(row.ID)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateLzCustomer(editId.value, { ...form })
    } else {
      await createLzCustomer({ ...form })
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
