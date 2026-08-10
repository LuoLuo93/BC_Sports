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
        <el-form-item label="零售主管">
          <el-select v-model="query.supervisorId" placeholder="请选择零售主管" filterable clearable style="width:200px">
            <el-option v-for="s in supervisorList" :key="s.ID" :label="s.NAME" :value="s.ID" />
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
          <span class="card-header-title">店仓列表</span>
          <el-button v-if="hasPermission('bi:erpStore:edit')" type="warning" plain :icon="Switch"
                     @click="openInheritDialog">零售主管继承</el-button>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" fixed="left" />
          <el-table-column prop="STORE_CODE" label="店仓编码" min-width="150" fixed="left" />
          <el-table-column prop="STORE_NAME" label="店仓名称" min-width="200" fixed="left" />
          <el-table-column prop="STORE_BRAND" label="店仓主品牌" min-width="150">
            <template #default="{ row }">{{ row.STORE_BRAND || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_SUPERVISOR" label="店仓零售督导" min-width="150">
            <template #default="{ row }">{{ row.STORE_SUPERVISOR || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_IS_STOP" label="关店标志" min-width="100" align="center">
            <template #default="{ row }">{{ row.STORE_IS_STOP || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_HT_AREA" label="合同面积" min-width="100" align="right">
            <template #default="{ row }">{{ row.STORE_HT_AREA || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_PROP_TYPE" label="道具类型" min-width="120">
            <template #default="{ row }">{{ row.STORE_PROP_TYPE || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_GROUP_NAME" label="集团名称" min-width="150">
            <template #default="{ row }">{{ row.STORE_GROUP_NAME || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_CHANNEL_FORMAT" label="渠道业态" min-width="120">
            <template #default="{ row }">{{ row.STORE_CHANNEL_FORMAT || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_MALL_NAME" label="商场名称" min-width="150">
            <template #default="{ row }">{{ row.STORE_MALL_NAME || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_RENTBEGIN" label="开店日期" min-width="120" align="center">
            <template #default="{ row }">{{ row.STORE_RENTBEGIN || '-' }}</template>
          </el-table-column>
          <el-table-column prop="STORE_RENTEND" label="闭店日期" min-width="120" align="center">
            <template #default="{ row }">{{ row.STORE_RENTEND || '-' }}</template>
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

    <!-- 编辑品牌/督导及扩展属性弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑店仓信息" width="780px" destroy-on-close class="store-edit-dialog">
      <el-form :model="form" label-width="90px" class="store-edit-form">
        <div class="form-col">
          <el-form-item label="店仓编码">
            <el-input :model-value="form.storeCode" disabled />
          </el-form-item>
          <el-form-item label="店仓名称">
            <el-input :model-value="form.storeName" disabled />
          </el-form-item>
          <el-form-item label="主品牌">
            <el-select v-model="form.brandId" placeholder="选择品牌" filterable clearable style="width:100%">
              <el-option v-for="b in brandList" :key="b.ID" :label="b.NAME" :value="b.ID" />
            </el-select>
          </el-form-item>
          <el-form-item label="零售督导">
            <el-select v-model="form.supervisorId" placeholder="选择零售督导" filterable clearable style="width:100%">
              <el-option v-for="s in supervisorList" :key="s.ID" :label="s.NAME" :value="s.ID" />
            </el-select>
          </el-form-item>
          <el-form-item label="关店标志">
            <el-select v-model="form.isStop" placeholder="选择关店标志" clearable style="width:100%">
              <el-option label="否(N)" value="N" />
              <el-option label="是(Y)" value="Y" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-col">
          <el-form-item label="合同面积">
            <el-input v-model="form.htArea" placeholder="请输入合同面积" />
          </el-form-item>
          <el-form-item label="道具类型">
            <el-input v-model="form.propType" placeholder="请输入道具类型" />
          </el-form-item>
          <el-form-item label="集团名称">
            <el-input v-model="form.groupName" placeholder="请输入集团名称" />
          </el-form-item>
          <el-form-item label="渠道业态">
            <el-input v-model="form.channelFormat" placeholder="请输入渠道业态" />
          </el-form-item>
          <el-form-item label="商场名称">
            <el-input v-model="form.mallName" placeholder="请输入商场名称" />
          </el-form-item>
          <el-form-item label="开店日期">
            <el-input v-model="form.rentBegin" placeholder="请输入开店日期" />
          </el-form-item>
          <el-form-item label="闭店日期">
            <el-input v-model="form.rentEnd" placeholder="请输入闭店日期" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 零售主管继承弹窗 -->
    <el-dialog v-model="inheritVisible" title="零售主管继承" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon
                title="将「原零售主管」名下的所有店铺，批量变更为「目标零售主管」" />
      <el-form label-width="110px" style="margin-top:16px">
        <el-form-item label="原零售主管" required>
          <el-select v-model="inheritForm.fromId" filterable clearable placeholder="请选择原零售主管"
                     style="width:100%" @change="onInheritChange">
            <el-option v-for="s in supervisorList" :key="s.ID" :label="s.NAME" :value="s.ID" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标零售主管" required>
          <el-select v-model="inheritForm.toId" filterable clearable placeholder="请选择目标零售主管"
                     style="width:100%" @change="onInheritChange">
            <el-option v-for="s in supervisorList" :key="s.ID" :label="s.NAME" :value="s.ID" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert v-if="inheritPreview.text" :type="inheritPreview.valid ? 'warning' : 'error'"
                :title="inheritPreview.text" show-icon :closable="false" style="margin-top:8px" />
      <template #footer>
        <el-button @click="inheritVisible = false">取消</el-button>
        <el-button type="primary" :loading="inheriting" :disabled="!canConfirmInherit"
                   @click="confirmInherit">确认继承</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'ErpStore' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, Switch } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES } from '@/utils/appConfig'
import {
  getErpStorePage, getErpStoreBrands, getErpStoreSupervisors, updateErpStoreAttrib,
  previewSupervisorInherit, executeSupervisorInherit
} from '@/api/erp'

const { hasPermission } = usePermission()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(
  getErpStorePage,
  { code: '', name: '', supervisorId: '' }
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
const form = reactive({
  storeId: '', storeCode: '', storeName: '', brandId: '', supervisorId: '',
  isStop: '', htArea: '', propType: '', groupName: '', channelFormat: '', mallName: '',
  rentBegin: '', rentEnd: ''
})

function handleEdit(row) {
  form.storeId = row.STORE_ID || ''
  form.storeCode = row.STORE_CODE || ''
  form.storeName = row.STORE_NAME || ''
  form.brandId = row.BRAND_ID || ''
  form.supervisorId = row.SUPERVISOR_ID || ''
  form.isStop = row.STORE_IS_STOP || ''
  form.htArea = row.STORE_HT_AREA != null ? String(row.STORE_HT_AREA) : ''
  form.propType = row.STORE_PROP_TYPE || ''
  form.groupName = row.STORE_GROUP_NAME || ''
  form.channelFormat = row.STORE_CHANNEL_FORMAT || ''
  form.mallName = row.STORE_MALL_NAME || ''
  form.rentBegin = row.STORE_RENTBEGIN || ''
  form.rentEnd = row.STORE_RENTEND || ''
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
      supervisorId: form.supervisorId || '',
      isStop: form.isStop || '',
      htArea: form.htArea || '',
      propType: form.propType || '',
      groupName: form.groupName || '',
      channelFormat: form.channelFormat || '',
      mallName: form.mallName || '',
      rentBegin: form.rentBegin || '',
      rentEnd: form.rentEnd || ''
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

// ===== 零售主管继承 =====
const inheritVisible = ref(false)
const inheriting = ref(false)
const inheritForm = reactive({ fromId: '', toId: '' })
const inheritPreview = reactive({ text: '', valid: false, count: 0 })

// 名称查表辅助
function supervisorName(id) {
  const s = supervisorList.value.find(x => String(x.ID) === String(id))
  return s ? s.NAME : ''
}

const canConfirmInherit = computed(() => inheritPreview.valid && inheritPreview.count > 0)

// 打开继承弹窗
function openInheritDialog() {
  inheritForm.fromId = ''
  inheritForm.toId = ''
  inheritPreview.text = ''
  inheritPreview.valid = false
  inheritPreview.count = 0
  if (!supervisorList.value.length) {
    loadOptions()
  }
  inheritVisible.value = true
}

// 任一改变时校验并预览
async function onInheritChange() {
  const { fromId, toId } = inheritForm
  if (!fromId || !toId) {
    inheritPreview.text = ''
    inheritPreview.valid = false
    inheritPreview.count = 0
    return
  }
  if (fromId === toId) {
    inheritPreview.text = '原零售主管与目标零售主管不能相同'
    inheritPreview.valid = false
    inheritPreview.count = 0
    return
  }
  try {
    const res = await previewSupervisorInherit(fromId)
    const count = res.data?.count ?? 0
    inheritPreview.count = count
    inheritPreview.valid = count > 0
    inheritPreview.text = count > 0
      ? `将把【${supervisorName(fromId)}】名下的 ${count} 家店铺全部改为【${supervisorName(toId)}】`
      : `【${supervisorName(fromId)}】名下暂无店铺，无可继承的数据`
  } catch {
    inheritPreview.text = '预览失败，请重试'
    inheritPreview.valid = false
    inheritPreview.count = 0
  }
}

// 二次确认并执行继承
async function confirmInherit() {
  if (!canConfirmInherit.value) return
  const { fromId, toId } = inheritForm
  try {
    await ElMessageBox.confirm(
      `确认将【${supervisorName(fromId)}】名下的 ${inheritPreview.count} 家店铺的零售主管全部改为【${supervisorName(toId)}】？此操作不可撤销。`,
      '二次确认',
      { type: 'warning', confirmButtonText: '确认继承', cancelButtonText: '取消' }
    )
  } catch {
    return // 用户取消
  }
  inheriting.value = true
  try {
    const res = await executeSupervisorInherit(fromId, toId)
    ElMessage.success(res.data || '继承成功')
    inheritVisible.value = false
    loadData()
  } finally {
    inheriting.value = false
  }
}

onMounted(() => {
  loadData()
  loadOptions()
})
</script>

<style scoped>
.store-edit-form {
  display: flex;
  gap: 24px;
}
.store-edit-form .form-col {
  flex: 1;
  min-width: 0;
}
</style>
