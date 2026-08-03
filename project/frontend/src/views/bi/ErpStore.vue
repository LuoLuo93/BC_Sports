<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card search-card--compact">
      <el-form :model="query" inline>
        <el-form-item label="编码">
          <el-input v-model="query.code" placeholder="请输入店仓编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="请输入店仓名称" clearable @keyup.enter="handleSearch" />
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
          <span class="card-header-title">店仓列表</span>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="STORE_CODE" label="店仓编码" min-width="150" />
          <el-table-column prop="STORE_NAME" label="店仓名称" min-width="200" />
          <el-table-column prop="STORE_BRAND" label="店仓主品牌" min-width="150">
            <template #default="{ row }">{{ row.STORE_BRAND || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_SUPERVISOR" label="店仓零售督导" min-width="150">
            <template #default="{ row }">{{ row.STORE_SUPERVISOR || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('bi:erpStore:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
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

    <!-- 编辑品牌/督导归属弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑店仓品牌/督导归属" width="460px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="店仓编码">
          <el-input :model-value="form.storeCode" disabled />
        </el-form-item>
        <el-form-item label="店仓名称">
          <el-input :model-value="form.storeName" disabled />
        </el-form-item>
        <el-form-item label="店仓主品牌">
          <el-select v-model="form.brandId" placeholder="选择品牌" filterable clearable style="width:100%">
            <el-option v-for="b in brandList" :key="b.ID" :label="b.NAME" :value="b.ID" />
          </el-select>
        </el-form-item>
        <el-form-item label="零售督导">
          <el-select v-model="form.supervisorId" placeholder="选择零售督导" filterable clearable style="width:100%">
            <el-option v-for="s in supervisorList" :key="s.ID" :label="s.NAME" :value="s.ID" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'ErpStore' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES } from '@/utils/appConfig'
import { getErpStorePage, getErpStoreBrands, getErpStoreSupervisors, updateErpStoreAttrib } from '@/api/erp'

const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(
  getErpStorePage,
  { code: '', name: '' }
)

// 品牌/督导下拉
const brandList = ref([])
const supervisorList = ref([])

async function loadOptions() {
  try {
    const [b, s] = await Promise.all([getErpStoreBrands(), getErpStoreSupervisors()])
    brandList.value = b.data || []
    supervisorList.value = s.data || []
  } catch {
    brandList.value = []
    supervisorList.value = []
  }
}

// 编辑弹窗
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ storeId: '', storeCode: '', storeName: '', brandId: '', supervisorId: '' })

function handleEdit(row) {
  form.storeId = row.STORE_ID || ''
  form.storeCode = row.STORE_CODE || ''
  form.storeName = row.STORE_NAME || ''
  form.brandId = row.BRAND_ID || ''
  form.supervisorId = row.SUPERVISOR_ID || ''
  if (!brandList.value.length && !supervisorList.value.length) {
    loadOptions()
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.storeId) {
    ElMessage.warning('缺少店仓ID')
    return
  }
  saving.value = true
  try {
    await updateErpStoreAttrib({
      storeId: form.storeId,
      brandId: form.brandId || '',
      supervisorId: form.supervisorId || ''
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(() => loadData())
</script>
