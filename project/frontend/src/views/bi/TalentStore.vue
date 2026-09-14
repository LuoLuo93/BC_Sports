<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="达人名称">
          <el-input v-model="query.talentName" placeholder="请输入达人名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="店仓编码">
          <el-input v-model="query.storeCode" placeholder="请输入伯俊ERP店仓编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="店仓名称">
          <el-input v-model="query.storeName" placeholder="请输入店仓名称" clearable @keyup.enter="handleSearch" />
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
          <span class="card-header-title">达人店铺列表</span>
          <el-button v-if="hasPermission('bi:talent-store:add')" type="primary" size="small" :icon="Plus" @click="openAdd">新增达人店铺</el-button>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column label="#" width="60" align="center">
            <template #default="{ $index }">{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="talentName" label="达人名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="storeCode" label="店仓编码" min-width="130" show-overflow-tooltip />
          <el-table-column prop="storeName" label="店仓名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="updateBy" label="修改人" min-width="110" show-overflow-tooltip />
          <el-table-column label="修改时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column v-if="canEdit || canDelete" label="操作" width="130" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="canEdit" type="primary" plain size="small" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="canDelete" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑达人店铺' : '新增达人店铺'" width="480px" destroy-on-close @open="onDialogOpen">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="达人名称" prop="talentName">
          <el-input v-model="form.talentName" maxlength="100" placeholder="请输入达人名称" />
        </el-form-item>
        <el-form-item label="店仓" prop="storeCode">
          <el-select
            v-model="form.storeCode"
            placeholder="输入编码或名称搜索店仓"
            filterable
            remote
            clearable
            :remote-method="searchStores"
            :loading="storeSearching"
            style="width:100%"
            @change="onStoreChange"
          >
            <el-option v-for="s in storeOptions" :key="s.CODE" :label="`${s.CODE} ${s.NAME}`" :value="s.CODE" />
          </el-select>
          <div v-if="form.storeName" class="store-name-hint">店仓名称：{{ form.storeName }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'TalentStore' })
import { ref, reactive, computed, onMounted, onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, Plus } from '@element-plus/icons-vue'
import { getTalentStorePage, addTalentStore, updateTalentStore, deleteTalentStore } from '@/api/bi'
import { getErpStoreSimplePage } from '@/api/erp'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES, defaultPageSize } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'

const { hasPermission } = usePermission()
const canEdit = computed(() => hasPermission('bi:talent-store:edit'))
const canDelete = computed(() => hasPermission('bi:talent-store:delete'))

// ===== 列表 =====
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: defaultPageSize.value, talentName: '', storeCode: '', storeName: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await getTalentStorePage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      talentName: query.talentName,
      storeCode: query.storeCode,
      storeName: query.storeName
    })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}
function handleSearch() { query.pageNum = 1; loadData() }
function resetQuery() { query.talentName = ''; query.storeCode = ''; query.storeName = ''; handleSearch() }

// ===== 新增/编辑弹窗 =====
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, talentName: '', storeCode: '', storeName: '' })

const formRules = {
  talentName: [{ required: true, message: '达人名称不能为空', trigger: 'blur' }],
  storeCode: [{ required: true, message: '请选择店仓', trigger: 'change' }]
}

function openAdd() {
  isEdit.value = false
  form.id = null
  form.talentName = ''
  form.storeCode = ''
  form.storeName = ''
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.id = row.id
  form.talentName = row.talentName
  form.storeCode = row.storeCode
  form.storeName = row.storeName
  dialogVisible.value = true
}

// 弹窗打开后预载店仓候选；编辑态把当前店仓塞进候选，保证下拉能回显 label
async function onDialogOpen() {
  if (isEdit.value) {
    storeOptions.value = [{ CODE: form.storeCode, NAME: form.storeName }]
  } else {
    await searchStores('')
  }
}

async function submitForm() {
  await formRef.value?.validate()
  if (!form.storeName) {
    ElMessage.warning('请从下拉中重新选择店仓')
    return
  }
  submitLoading.value = true
  try {
    const payload = { talentName: form.talentName.trim(), storeCode: form.storeCode, storeName: form.storeName }
    if (isEdit.value) {
      await updateTalentStore(form.id, payload)
    } else {
      await addTalentStore(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除达人「${row.talentName}」与店仓「${row.storeName}」的绑定吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteTalentStore(row.id)
    ElMessage.success('删除成功')
    // 当前页删空后回退一页，避免停在空页
    if (tableData.value.length === 1 && query.pageNum > 1) query.pageNum--
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

// ===== 店仓远程搜索（伯俊ERP C_STORE，编码/名称任一命中） =====
const storeOptions = ref([])
const storeSearching = ref(false)

async function searchStores(keyword) {
  storeSearching.value = true
  try {
    const res = await getErpStoreSimplePage({ pageNum: 1, pageSize: 50, code: keyword || undefined, name: keyword || undefined })
    storeOptions.value = res.data?.records || []
  } finally {
    storeSearching.value = false
  }
}

function onStoreChange(code) {
  const hit = storeOptions.value.find(s => s.CODE === code)
  form.storeName = hit ? hit.NAME : ''
}

onMounted(() => loadData())
onActivated(() => loadData())
</script>

<style scoped>
.search-card {
  margin-bottom: 12px;
}
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header-title {
  font-size: 16px;
  font-weight: 600;
}
.pagination-wrapper {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
.store-name-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  line-height: 1.4;
}
</style>
