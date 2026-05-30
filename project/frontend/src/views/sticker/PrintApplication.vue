<template>
  <div class="page-container">
    <!-- ========== 列表视图 ========== -->
    <template v-if="!formVisible">
      <el-card shadow="never">
        <div style="display:flex;justify-content:space-between;margin-bottom:16px">
          <el-radio-group v-model="query.status" @change="loadData">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="0">草稿</el-radio-button>
            <el-radio-button :label="1">待审核</el-radio-button>
            <el-radio-button :label="2">已审核</el-radio-button>
            <el-radio-button :label="3">已驳回</el-radio-button>
          </el-radio-group>
          <el-button v-if="hasPermission('sticker:print:add')" type="primary" @click="handleCreate">新建申请</el-button>
        </div>

        <el-table v-loading="loading" :data="tableData" border stripe>
          <el-table-column prop="orderNo" label="申请单号" width="200" />
          <el-table-column prop="applicant" label="申请人" width="120" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <span :class="['status-badge', 'status-' + row.status]">{{ statusLabel(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="360" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="success" plain size="small" @click="handleView(row)">查看</el-button>
              <el-button v-if="row.status === 0 && hasPermission('sticker:print:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 0 && hasPermission('sticker:print:edit')" type="warning" plain size="small" @click="handleSubmit(row)">提交</el-button>
              <el-button v-if="row.status === 0 && hasPermission('sticker:print:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
              <el-button v-if="row.status === 1 && hasPermission('sticker:print:review')" type="success" plain size="small" @click="handleReview(row)">审核</el-button>
              <el-button v-if="row.status === 2 && hasPermission('sticker:print:execute')" type="primary" plain size="small" @click="handleBarTenderPrint(row)">打印</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="loadData"
          style="margin-top:16px;justify-content:flex-end"
        />
      </el-card>
    </template>

    <!-- ========== 新建/编辑视图 ========== -->
    <template v-else>
      <div class="form-view">
        <!-- 紧凑头部栏 -->
        <div class="form-header">
          <el-button type="warning" size="small" @click="handleBack">返回列表</el-button>
          <span class="form-header-title">{{ isEdit ? '编辑打印申请单' : '新建打印申请单' }}</span>
          <el-button type="primary" size="small" @click="handleSave">保存</el-button>
        </div>

        <!-- 自动填充信息行 -->
        <div class="form-info-row">
          <div class="info-item">
            <span class="info-label">申请单号</span>
            <span class="info-value">{{ form.orderNo }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">申请人</span>
            <span class="info-value">{{ form.applicant }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">申请部门</span>
            <span class="info-value">{{ form.deptName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">申请时间</span>
            <span class="info-value">{{ form.createTime }}</span>
          </div>
          <div class="info-item" style="flex:1.5">
            <span class="info-label">备注</span>
            <el-input v-model="form.remark" placeholder="请输入备注信息" size="small" />
          </div>
        </div>

        <!-- 主体区域：上下结构 -->
        <div class="form-body">
          <!-- 上方：货品搜索 -->
          <div class="search-panel">
            <div class="search-bar">
              <span class="panel-bar-title">货品搜索</span>
              <el-input v-model="searchMaterialNumber" placeholder="货号" size="small" clearable style="width:170px" @keyup.enter="searchProductsAction" />
              <el-input v-model="searchStyleNumber" placeholder="款号" size="small" clearable style="width:170px" @keyup.enter="searchProductsAction" />
              <el-input v-model="searchMaterialName" placeholder="商品名称" size="small" clearable style="width:200px" @keyup.enter="searchProductsAction" />
              <el-select v-model="searchBrandId" placeholder="品牌" size="small" clearable filterable style="width:140px">
                <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ID" />
              </el-select>
              <el-button type="primary" size="small" @click="searchProductsAction">搜索</el-button>
              <el-button type="success" size="small" :disabled="!selectedProducts.length" @click="confirmProductSelect">
                添加({{ selectedProducts.length }})
              </el-button>
            </div>
            <el-table v-loading="productLoading" :data="productList" border size="small" @selection-change="handleProductSelect" height="100%">
              <el-table-column type="selection" width="35" fixed="left" />
              <el-table-column type="index" label="#" width="45" fixed="left" />
              <el-table-column prop="MATERIAL_NUMBER" label="货号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
              <el-table-column prop="STYLE_NUMBER" label="款号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
              <el-table-column prop="MATERIAL_NAME" label="商品名称" width="200" show-overflow-tooltip fixed="left" class-name="col-key" />
              <el-table-column prop="BRAND_NAME" label="品牌" width="120" show-overflow-tooltip />
              <el-table-column prop="COLOR" label="颜色" width="90" />
              <el-table-column prop="PRICE" label="价格" width="120">
                <template #default="{ row }">{{ row.PRICE != null ? Number(row.PRICE).toFixed(5) : '-' }}</template>
              </el-table-column>
              <el-table-column prop="EXECUTION_STANDARD" label="执行标准" width="160">
                <template #default="{ row }">{{ row.EXECUTION_STANDARD || '-' }}</template>
              </el-table-column>
              <el-table-column prop="ORIGIN" label="产地" width="90">
                <template #default="{ row }">{{ row.ORIGIN || '-' }}</template>
              </el-table-column>
              <el-table-column prop="MANUFACTURER" label="制造商" width="140">
                <template #default="{ row }">{{ row.MANUFACTURER || '-' }}</template>
              </el-table-column>
              <el-table-column prop="MATERIAL_COMPOSITION" label="面料成分" width="160">
                <template #default="{ row }">{{ row.MATERIAL_COMPOSITION || '-' }}</template>
              </el-table-column>
              <el-table-column prop="EAN13" label="EAN13" width="150">
                <template #default="{ row }">{{ row.EAN13 || '-' }}</template>
              </el-table-column>
              <el-table-column prop="SIZES" label="尺码组列表" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">{{ row.SIZES || '-' }}</template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 下方：已选明细（占更多空间） -->
          <div class="detail-panel">
            <div class="panel-bar">
              <span class="panel-bar-title">已选明细 <span class="detail-summary">共 {{ filteredDetails.length }} 条，合计 <em class="total-qty">{{ totalPrintQty }}</em> 张</span></span>
              <div style="display:flex;gap:6px;align-items:center">
                <el-input v-model="detailKeyword" placeholder="搜索货号/款号/货品名称" size="small" clearable style="width:240px" />
                <el-button size="small" :disabled="!selectedRows.length" @click="handleBatchSetQty">批量设置数量</el-button>
                <el-button type="danger" size="small" :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
              </div>
            </div>
            <el-table :data="pagedDetails" border size="small" @selection-change="handleDetailSelect" ref="detailTableRef" height="100%">
              <el-table-column type="selection" width="35" fixed="left" />
              <el-table-column label="#" width="45" fixed="left">
                <template #default="{ $index }">{{ (detailPage - 1) * detailSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="materialNumber" label="货号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
              <el-table-column prop="styleNumber" label="款号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
              <el-table-column prop="materialName" label="货品名称" width="200" show-overflow-tooltip fixed="left" class-name="col-key" />
              <el-table-column prop="brandName" label="品牌" width="120" />
              <el-table-column prop="color" label="颜色" width="90" />
              <el-table-column prop="price" label="价格" width="120">
                <template #default="{ row }">{{ row.price ? Number(row.price).toFixed(5) : '-' }}</template>
              </el-table-column>
              <el-table-column prop="executionStandard" label="执行标准" width="160">
                <template #default="{ row }">{{ row.executionStandard || '-' }}</template>
              </el-table-column>
              <el-table-column prop="origin" label="产地" width="90">
                <template #default="{ row }">{{ row.origin || '-' }}</template>
              </el-table-column>
              <el-table-column prop="manufacturer" label="制造商" width="140">
                <template #default="{ row }">{{ row.manufacturer || '-' }}</template>
              </el-table-column>
              <el-table-column prop="materialComposition" label="面料成分" width="160">
                <template #default="{ row }">{{ row.materialComposition || '-' }}</template>
              </el-table-column>
              <el-table-column prop="ean13" label="EAN13" width="150">
                <template #default="{ row }">{{ row.ean13 || '-' }}</template>
              </el-table-column>
              <el-table-column label="尺码" width="80" align="center" fixed="right">
                <template #default="{ row }">
                  <el-tag v-if="row.sizeName" type="warning" size="small" effect="dark">{{ row.sizeName }}</el-tag>
                  <span v-else style="color:#d9d9d9">-</span>
                </template>
              </el-table-column>
              <el-table-column label="数量" width="70" align="center" fixed="right">
                <template #default="{ row }">
                  <span v-if="row.sizeName" class="detail-qty">{{ row.printQty }}</span>
                  <span v-else style="color:#d9d9d9">-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center" fixed="right">
                <template #default="{ row, $index }">
                  <el-button type="primary" plain size="small" @click="openSizeAssign(row, (detailPage - 1) * detailSize + $index)">{{ row.sizeName ? '编辑' : '调整数量' }}</el-button>
                  <el-button type="danger" plain size="small" @click="form.details.splice((detailPage - 1) * detailSize + $index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-if="filteredDetails.length > 0"
              v-model:current-page="detailPage"
              v-model:page-size="detailSize"
              :total="filteredDetails.length"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              size="small"
              style="margin-top:8px;justify-content:flex-end"
            />
          </div>
        </div>
      </div>
    </template>

    <!-- 批量设置数量 -->
    <el-dialog v-model="showBatchQtyDialog" title="批量设置打印数量" width="300px">
      <el-input-number v-model="batchQty" :min="1" :max="999" controls-position="right" style="width:100%" />
      <template #footer>
        <el-button @click="showBatchQtyDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchQty">确认</el-button>
      </template>
    </el-dialog>

    <!-- 审核 -->
    <el-dialog v-model="reviewVisible" title="审核" width="400px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.status">
            <el-radio :label="2">通过</el-radio>
            <el-radio :label="3">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReview">确认</el-button>
      </template>
    </el-dialog>

    <!-- 尺码赋值 -->
    <el-dialog v-model="showSizeAssignDialog" title="尺码赋值" width="520px" class="size-assign-dialog" destroy-on-close>
      <div class="sa-header">
        <div class="sa-header-bar"></div>
        <div class="sa-header-info">
          <div class="sa-header-name">{{ sizeAssignProductName }}</div>
          <div class="sa-header-meta">
            <span v-if="sizeAssignMeta.brand" class="sa-meta-item">品牌: {{ sizeAssignMeta.brand }}</span>
            <span v-if="sizeAssignMeta.color" class="sa-meta-item">颜色: {{ sizeAssignMeta.color }}</span>
            <span v-if="sizeAssignMeta.ean13" class="sa-meta-item">EAN13: {{ sizeAssignMeta.ean13 }}</span>
          </div>
        </div>
      </div>
      <div class="sa-toolbar">
        <el-checkbox v-model="sizeAssignAllChecked" @change="toggleSizeAssignAll">全选</el-checkbox>
        <span class="sa-toolbar-count">已选 <b>{{ sizeAssignCheckedCount }}</b> / {{ sizeAssignOptions.length }} 个尺码</span>
      </div>
      <div class="sa-list">
        <div v-for="(item, i) in sizeAssignOptions" :key="i" class="sa-item" :class="{ 'sa-item--checked': item.checked, 'sa-item--existing': item.existing }" @click="item.checked = !item.checked">
          <el-checkbox v-model="item.checked" @click.stop />
          <span class="sa-item-size">{{ item.size }}</span>
          <el-tag v-if="item.existing" type="warning" size="small" effect="plain">已添加</el-tag>
          <el-input-number v-model="item.qty" :min="1" :max="999" size="small" controls-position="right" style="width:120px" :disabled="!item.checked" @click.stop />
        </div>
        <div v-if="!sizeAssignOptions.length" class="sa-empty">无可用尺码</div>
      </div>
      <template #footer>
        <el-button @click="showSizeAssignDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmSizeAssign">确认 ({{ sizeAssignCheckedCount }})</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPrintOrderPage, getPrintOrder, createPrintOrder, updatePrintOrder, submitPrintOrder, reviewPrintOrder, deletePrintOrder, bartenderPrint, searchProducts, getProductBrands } from '@/api/sticker'
import { usePermission } from '@/composables/usePermission'
import { useAuthStore } from '@/stores/auth'

const { hasPermission } = usePermission()
const authStore = useAuthStore()
const viewAll = computed(() => hasPermission('sticker:print:all'))

const currentTimeStr = new Date().toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })

function generateOrderNo() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  const rand = String(Math.floor(Math.random() * 10000)).padStart(4, '0')
  return `PRT${y}${m}${d}${hh}${mm}${ss}${rand}`
}

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, status: null })

// ─── Form (replaces dialog) ────────────────────────────
const formVisible = ref(false)
const isEdit = ref(false)
const editOrderId = ref('')
const form = reactive({ orderNo: '', applicant: '', deptName: '', createTime: '', remark: '', details: [] })

// Detail table selection
const selectedRows = ref([])
const detailTableRef = ref()
const detailPage = ref(1)
const detailSize = ref(50)
const detailKeyword = ref('')

const filteredDetails = computed(() => {
  const kw = detailKeyword.value.trim().toLowerCase()
  if (!kw) return form.details
  return form.details.filter(d =>
    (d.materialNumber || '').toLowerCase().includes(kw) ||
    (d.styleNumber || '').toLowerCase().includes(kw) ||
    (d.materialName || '').toLowerCase().includes(kw)
  )
})

const totalPrintQty = computed(() => form.details.reduce((sum, d) => sum + (d.printQty || 0), 0))

const pagedDetails = computed(() => {
  const start = (detailPage.value - 1) * detailSize.value
  return filteredDetails.value.slice(start, start + detailSize.value)
})

watch(detailKeyword, () => { detailPage.value = 1 })

// Batch qty
const batchQty = ref(1)
const showBatchQtyDialog = ref(false)

// Product search
const searchMaterialNumber = ref('')
const searchStyleNumber = ref('')
const searchMaterialName = ref('')
const searchBrandId = ref('')
const brandList = ref([])
const productList = ref([])
const productLoading = ref(false)
const selectedProducts = ref([])

// Review
const reviewVisible = ref(false)
const reviewOrderId = ref('')
const reviewForm = reactive({ status: 2, reviewRemark: '' })

// Size assign
const showSizeAssignDialog = ref(false)
const sizeAssignRowIndex = ref(-1)
const sizeAssignProductName = ref('')
const sizeAssignMeta = reactive({ brand: '', color: '', ean13: '' })
const sizeAssignOptions = ref([])
const sizeAssignAllChecked = ref(false)
const sizeAssignCheckedCount = computed(() => sizeAssignOptions.value.filter(o => o.checked).length)

const STATUS_MAP = { 0: '草稿', 1: '待审核', 2: '已审核', 3: '已驳回' }
const STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
const statusLabel = (s) => STATUS_MAP[s] || '未知'
const statusTagType = (s) => STATUS_TAG[s] || 'info'

// ─── List ──────────────────────────────────────────────
async function loadData() {
  loading.value = true
  try {
    const { data } = await getPrintOrderPage({ page: query.page, size: query.size, status: query.status, viewAll: viewAll.value })
    tableData.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  editOrderId.value = ''
  form.orderNo = generateOrderNo()
  form.applicant = authStore.nickname || authStore.username || ''
  form.deptName = authStore.deptName || ''
  form.createTime = new Date().toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  form.remark = ''
  form.details = []
  selectedRows.value = []
  searchMaterialNumber.value = ''
  searchStyleNumber.value = ''
  searchMaterialName.value = ''
  searchBrandId.value = ''
  productList.value = []
  selectedProducts.value = []
  loadBrands()
  formVisible.value = true
}

async function handleEdit(row) {
  const { data } = await getPrintOrder(row.id)
  isEdit.value = true
  editOrderId.value = row.id
  form.orderNo = data.orderNo || ''
  form.applicant = data.applicant || ''
  form.deptName = ''
  form.createTime = data.createTime || ''
  form.remark = data.remark || ''
  form.details = data.details || []
  selectedRows.value = []
  searchMaterialNumber.value = ''
  searchStyleNumber.value = ''
  searchMaterialName.value = ''
  searchBrandId.value = ''
  productList.value = []
  selectedProducts.value = []
  loadBrands()
  formVisible.value = true
}

function handleView(row) {
  router.push(`/sticker/print/${row.id}`)
}

async function handleSubmit(row) {
  await ElMessageBox.confirm('确认提交审核？', '提示')
  await submitPrintOrder(row.id)
  ElMessage.success('提交成功')
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除此申请单？', '提示')
  await deletePrintOrder(row.id)
  ElMessage.success('删除成功')
  loadData()
}

function handleReview(row) {
  reviewOrderId.value = row.id
  reviewForm.status = 2
  reviewForm.reviewRemark = ''
  reviewVisible.value = true
}

async function confirmReview() {
  await reviewPrintOrder(reviewOrderId.value, {
    status: String(reviewForm.status),
    reviewRemark: reviewForm.reviewRemark
  })
  ElMessage.success('审核完成')
  reviewVisible.value = false
  loadData()
}

async function handleBarTenderPrint(row) {
  await ElMessageBox.confirm(`确认打印申请单 ${row.orderNo}？`, '打印确认')
  try {
    await bartenderPrint(row.id)
    ElMessage.success('打印指令已发送')
    loadData()
  } catch (e) {
    ElMessage.error('打印失败')
  }
}

function handleBack() {
  if (form.details.length > 0) {
    ElMessageBox.confirm('有未保存的数据，确定返回吗？', '提示', {
      confirmButtonText: '确定返回',
      cancelButtonText: '继续编辑',
      type: 'warning'
    }).then(() => {
      formVisible.value = false
    }).catch(() => {})
  } else {
    formVisible.value = false
  }
}

async function handleSave() {
  if (form.details.length === 0) {
    ElMessage.warning('请至少添加一条明细')
    return
  }
  const invalid = form.details.find(d => !d.sizeName || !d.printQty || d.printQty <= 0)
  if (invalid) {
    ElMessage.warning('存在未赋值或数量为0的明细，请检查')
    return
  }
  if (isEdit.value) {
    await updatePrintOrder(editOrderId.value, { remark: form.remark, details: form.details })
  } else {
    await createPrintOrder({ remark: form.remark, details: form.details })
  }
  ElMessage.success('保存成功')
  formVisible.value = false
  loadData()
}

// ─── Detail Selection ──────────────────────────────────
function handleDetailSelect(rows) {
  selectedRows.value = rows
}

function handleBatchSetQty() {
  if (!selectedRows.value.length) return
  batchQty.value = 1
  showBatchQtyDialog.value = true
}

function confirmBatchQty() {
  for (const row of selectedRows.value) {
    row.printQty = batchQty.value
  }
  showBatchQtyDialog.value = false
}

function handleBatchDelete() {
  if (!selectedRows.value.length) return
  const set = new Set(selectedRows.value)
  form.details = form.details.filter(d => !set.has(d))
  selectedRows.value = []
}

// ─── Product Search (inline) ───────────────────────────
async function loadBrands() {
  if (brandList.value.length) return
  try {
    const { data } = await getProductBrands()
    brandList.value = data || []
  } catch {}
}

async function searchProductsAction() {
  if (!searchMaterialNumber.value.trim() && !searchStyleNumber.value.trim() && !searchMaterialName.value.trim() && !searchBrandId.value) {
    ElMessage.warning('货号、款号和商品名称至少填写一项')
    return
  }
  productLoading.value = true
  try {
    const { data } = await searchProducts({
      materialNumber: searchMaterialNumber.value,
      styleNumber: searchStyleNumber.value,
      materialName: searchMaterialName.value,
      brandId: searchBrandId.value
    })
    productList.value = data || []
  } finally {
    productLoading.value = false
  }
}

function handleProductSelect(selection) {
  selectedProducts.value = selection
}

function confirmProductSelect() {
  const skipped = []
  for (const p of selectedProducts.value) {
    const mn = p.MATERIAL_NUMBER || ''
    const sn = p.STYLE_NUMBER || ''
    const exists = form.details.some(d => d.materialNumber === mn && d.styleNumber === sn)
    if (exists) {
      skipped.push(p.MATERIAL_NAME || mn)
      continue
    }
    form.details.push({
      materialNumber: mn,
      styleNumber: sn,
      materialName: p.MATERIAL_NAME || '',
      sizeGroup: p.SIZES || '',
      color: p.COLOR || '',
      ean13: p.EAN13 || '',
      brandName: p.BRAND_NAME || '',
      price: p.PRICE || 0,
      executionStandard: p.EXECUTION_STANDARD || '',
      origin: p.ORIGIN || '',
      manufacturer: p.MANUFACTURER || '',
      manufacturerAddress: p.MANUFACTURER_ADDRESS || '',
      contactPhone: p.CONTACT_PHONE || '',
      materialComposition: p.MATERIAL_COMPOSITION || '',
      sizeName: '',
      printQty: 0
    })
  }
  if (skipped.length) {
    ElMessage.warning(`${skipped.join('、')} 已在明细中，已跳过`)
  }
  selectedProducts.value = []
}

// ─── Size Assign ────────────────────────────────────────
function openSizeAssign(row, index) {
  sizeAssignRowIndex.value = index
  sizeAssignProductName.value = `${row.styleNumber || ''} ${row.materialName || ''}`
  sizeAssignMeta.brand = row.brandName || ''
  sizeAssignMeta.color = row.color || ''
  sizeAssignMeta.ean13 = row.ean13 || ''
  const sizes = parseSizes(row.sizeGroup)
  // 找出同货号已添加的尺码及数量
  const existingMap = {}
  form.details.forEach((d, i) => {
    if (d.materialNumber === row.materialNumber && d.styleNumber === row.styleNumber && d.sizeName) {
      existingMap[d.sizeName] = { qty: d.printQty, index: i }
    }
  })
  sizeAssignOptions.value = sizes.map(s => {
    const exist = existingMap[s]
    return {
      size: s,
      checked: !!exist,
      qty: exist ? exist.qty : 1,
      existing: !!exist
    }
  })
  sizeAssignAllChecked.value = sizeAssignOptions.value.every(o => o.checked)
  showSizeAssignDialog.value = true
}

function toggleSizeAssignAll(checked) {
  for (const item of sizeAssignOptions.value) {
    item.checked = checked
  }
}

function confirmSizeAssign() {
  const checked = sizeAssignOptions.value.filter(item => item.checked)
  if (!checked.length) {
    ElMessage.warning('请至少选择一个尺码')
    return
  }
  const idx = sizeAssignRowIndex.value
  const orig = form.details[idx]
  // 找到同货号所有行的位置，记录首个位置用于插入
  let firstIdx = -1
  const removeIndices = []
  form.details.forEach((d, i) => {
    if (d.materialNumber === orig.materialNumber && d.styleNumber === orig.styleNumber) {
      if (firstIdx === -1) firstIdx = i
      removeIndices.push(i)
    }
  })
  // 从后往前删除，避免索引偏移
  for (let i = removeIndices.length - 1; i >= 0; i--) {
    form.details.splice(removeIndices[i], 1)
  }
  // 在原首个位置插入勾选的尺码
  const insertAt = firstIdx >= 0 ? firstIdx : idx
  for (let i = 0; i < checked.length; i++) {
    form.details.splice(insertAt + i, 0, {
      materialNumber: orig.materialNumber,
      styleNumber: orig.styleNumber,
      materialName: orig.materialName,
      sizeGroup: orig.sizeGroup,
      color: orig.color,
      ean13: orig.ean13,
      brandName: orig.brandName,
      price: orig.price,
      executionStandard: orig.executionStandard,
      origin: orig.origin,
      manufacturer: orig.manufacturer,
      manufacturerAddress: orig.manufacturerAddress,
      contactPhone: orig.contactPhone,
      materialComposition: orig.materialComposition,
      sizeName: checked[i].size,
      printQty: checked[i].qty
    })
  }
  showSizeAssignDialog.value = false
}

// ─── Size Parser ───────────────────────────────────────
function parseSizes(sizesStr) {
  if (!sizesStr) return []
  return sizesStr.split(/[,，;；\s]+/).filter(Boolean)
}

// ─── Navigation guard ─────────────────────────────────
function hasUnsavedData() {
  return formVisible.value && form.details.length > 0
}

function beforeunloadHandler(e) {
  if (hasUnsavedData()) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onBeforeRouteLeave((to, from, next) => {
  if (hasUnsavedData()) {
    ElMessageBox.confirm('有未保存的数据，确定离开吗？', '提示', { type: 'warning' })
      .then(() => { formVisible.value = false; next() })
      .catch(() => next(false))
  } else {
    next()
  }
})

onMounted(() => {
  loadData()
  window.addEventListener('beforeunload', beforeunloadHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeunloadHandler)
})
</script>

<style scoped>
.form-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px - 38px - 32px - 40px);
  margin: -20px -24px;
  padding: 0;
  background: #f0f2f5;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.form-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.form-info-row {
  display: flex;
  gap: 0;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
  align-items: stretch;
}
.info-item {
  flex: 1;
  padding: 8px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  border-right: 1px solid #f3f4f6;
}
.info-item:last-child { border-right: none; }
.info-label {
  font-size: 11px;
  color: #6366f1;
  font-weight: 600;
  white-space: nowrap;
  letter-spacing: 0.04em;
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  padding: 2px 10px;
  border-radius: 10px;
  border: 1px solid #c7d2fe;
}
.info-value {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
}

.info-item :deep(.el-input) {
  flex: 1;
}

.form-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
  overflow: hidden;
}

.search-panel {
  height: 38%;
  display: flex;
  flex-direction: column;
  padding: 8px;
  background: #fff;
  border-bottom: 4px solid #f0f2f5;
  overflow: hidden;
}
.search-table {
  flex: 1;
}

.detail-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 8px;
  background: #fff;
  overflow: hidden;
}
.detail-table {
  flex: 1;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-shrink: 0;
}

.panel-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-shrink: 0;
}
.panel-bar-title {
  font-weight: 600;
  font-size: 14px;
  color: #111827;
}
.detail-summary {
  font-weight: 400;
  font-size: 12px;
  color: #9ca3af;
}
.detail-qty {
  display: inline-block;
  min-width: 28px;
  padding: 2px 8px;
  background: #ecfdf5;
  color: #059669;
  border-radius: 4px;
  font-weight: 700;
  font-size: 14px;
  text-align: center;
}
.total-qty {
  font-style: normal;
  font-weight: 700;
  font-size: 15px;
  color: #dc2626;
  background: #fef2f2;
  padding: 1px 8px;
  border-radius: 4px;
}

/* ─── Size Assign Dialog ────────────────────────────── */
.size-assign-dialog :deep(.el-dialog__body) {
  padding: 20px;
}
.sa-header {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px;
  background: #f0f5ff;
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid #dbeafe;
}
.sa-header-bar {
  width: 4px;
  height: 44px;
  background: linear-gradient(180deg, #3b82f6, #6366f1);
  border-radius: 2px;
  flex-shrink: 0;
}
.sa-header-info {
  flex: 1;
  min-width: 0;
}
.sa-header-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sa-header-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  flex-wrap: wrap;
}
.sa-meta-item {
  font-size: 12px;
  color: #6b7280;
}
.sa-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid #f3f4f6;
}
.sa-toolbar-count {
  font-size: 12px;
  color: #9ca3af;
}
.sa-toolbar-count b {
  color: #3b82f6;
}
.sa-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 340px;
  overflow-y: auto;
}
.sa-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid #f3f4f6;
  border-radius: 6px;
  transition: all 0.15s;
  cursor: pointer;
}
.sa-item:hover {
  background: #f8fafc;
}
.sa-item--checked {
  background: #eff6ff;
  border-color: #bfdbfe;
}
.sa-item--existing {
  background: #fffbeb;
  border-color: #fde68a;
}
.sa-item--existing.sa-item--checked {
  background: #fef3c7;
  border-color: #fcd34d;
}
.sa-item-size {
  width: 60px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}
.sa-empty {
  text-align: center;
  padding: 24px;
  color: #c0c4cc;
  font-size: 13px;
}
</style>
