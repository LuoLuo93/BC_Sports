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
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <el-button v-if="row.status === 0 && hasPermission('sticker:print:edit')" link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 0 && hasPermission('sticker:print:edit')" link type="warning" @click="handleSubmit(row)">提交</el-button>
              <el-button v-if="row.status === 0 && hasPermission('sticker:print:delete')" link type="danger" @click="handleDelete(row)">删除</el-button>
              <el-button v-if="row.status === 1 && hasPermission('sticker:print:review')" link type="success" @click="handleReview(row)">审核</el-button>
              <el-button v-if="row.status === 2 && hasPermission('sticker:print:execute')" link type="primary" @click="handleBarTenderPrint(row)">打印</el-button>
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
          <el-button type="warning" size="small" @click="formVisible = false">返回列表</el-button>
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
              <el-input v-model="searchMaterialNumber" placeholder="货号" size="small" clearable style="width:130px" @keyup.enter="searchProductsAction" />
              <el-input v-model="searchStyleNumber" placeholder="款号" size="small" clearable style="width:130px" @keyup.enter="searchProductsAction" />
              <el-input v-model="searchMaterialName" placeholder="商品名称" size="small" clearable style="width:160px" @keyup.enter="searchProductsAction" />
              <el-select v-model="searchBrandId" placeholder="品牌" size="small" clearable filterable style="width:140px">
                <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ID" />
              </el-select>
              <el-button type="primary" size="small" @click="searchProductsAction">搜索</el-button>
              <el-button type="success" size="small" :disabled="!selectedProducts.length" @click="confirmProductSelect">
                添加({{ selectedProducts.length }})
              </el-button>
            </div>
            <el-table v-loading="productLoading" :data="productList" border size="small" @selection-change="handleProductSelect" height="100%">
              <el-table-column type="selection" width="35" />
              <el-table-column type="index" label="#" width="45" />
              <el-table-column prop="MATERIAL_NUMBER" label="货号" width="150" show-overflow-tooltip />
              <el-table-column prop="STYLE_NUMBER" label="款号" width="150" show-overflow-tooltip />
              <el-table-column prop="MATERIAL_NAME" label="商品名称" width="240" show-overflow-tooltip />
              <el-table-column prop="BRAND_NAME" label="品牌" width="100" show-overflow-tooltip />
              <el-table-column prop="COLOR" label="颜色" width="80" />
              <el-table-column prop="PRICE" label="价格" width="110">
                <template #default="{ row }">{{ row.PRICE != null ? Number(row.PRICE).toFixed(5) : '-' }}</template>
              </el-table-column>
              <el-table-column prop="EXECUTION_STANDARD" label="执行标准" width="100">
                <template #default="{ row }">{{ row.EXECUTION_STANDARD || '-' }}</template>
              </el-table-column>
              <el-table-column prop="EAN13" label="EAN13" width="130">
                <template #default="{ row }">{{ row.EAN13 || '-' }}</template>
              </el-table-column>
              <el-table-column prop="SIZES" label="尺码组列表" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ row.SIZES || '-' }}</template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 下方：已选明细（占更多空间） -->
          <div class="detail-panel">
            <div class="panel-bar">
              <span class="panel-bar-title">已选明细 <span class="detail-summary">共 {{ form.details.length }} 条，合计 {{ totalPrintQty }} 张</span></span>
              <div style="display:flex;gap:6px">
                <el-button size="small" :disabled="!selectedRows.length" @click="handleBatchSetQty">批量设置数量</el-button>
                <el-button type="danger" size="small" :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
              </div>
            </div>
            <el-table :data="pagedDetails" border size="small" @selection-change="handleDetailSelect" ref="detailTableRef" height="100%">
              <el-table-column type="selection" width="35" />
              <el-table-column label="#" width="45">
                <template #default="{ $index }">{{ (detailPage - 1) * detailSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="materialNumber" label="货号" width="150" show-overflow-tooltip />
              <el-table-column prop="articleNo" label="款号" width="150" show-overflow-tooltip />
              <el-table-column prop="articleName" label="商品名称" width="240" show-overflow-tooltip />
              <el-table-column prop="brandName" label="品牌" width="100" />
              <el-table-column prop="color" label="颜色" width="80" />
              <el-table-column prop="price" label="价格" width="110">
                <template #default="{ row }">{{ row.price ? Number(row.price).toFixed(5) : '-' }}</template>
              </el-table-column>
              <el-table-column prop="executionStandard" label="执行标准" width="100">
                <template #default="{ row }">{{ row.executionStandard || '-' }}</template>
              </el-table-column>
              <el-table-column prop="ean13" label="EAN13" width="130">
                <template #default="{ row }">{{ row.ean13 || '-' }}</template>
              </el-table-column>
              <el-table-column label="尺码" width="160">
                <template #default="{ row, $index }">
                  <div v-if="row.sizeName" style="display:flex;align-items:center;gap:4px">
                    <span>{{ row.sizeName }}</span>
                    <el-button link type="primary" size="small" @click="openSizeAssign(row, (detailPage - 1) * detailSize + $index)">编辑</el-button>
                  </div>
                  <el-button v-else link type="primary" size="small" @click="openSizeAssign(row, (detailPage - 1) * detailSize + $index)">调整数量</el-button>
                </template>
              </el-table-column>
              <el-table-column label="数量" width="110">
                <template #default="{ row }">
                  <span v-if="row.sizeName">{{ row.printQty }}</span>
                  <span v-else style="color:#d9d9d9">-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="50">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="form.details.splice((detailPage - 1) * detailSize + $index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-if="form.details.length > 0"
              v-model:current-page="detailPage"
              v-model:page-size="detailSize"
              :total="form.details.length"
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
    <el-dialog v-model="showSizeAssignDialog" title="尺码赋值" width="460px" class="size-assign-dialog">
      <div class="sa-header">
        <div class="sa-header-bar"></div>
        <div>
          <div class="sa-header-name">{{ sizeAssignProductName }}</div>
          <div class="sa-header-sub">选择需要打印的尺码及数量</div>
        </div>
      </div>
      <div class="sa-toolbar">
        <el-checkbox v-model="sizeAssignAllChecked" @change="toggleSizeAssignAll">全选</el-checkbox>
      </div>
      <div class="sa-list">
        <div v-for="(item, i) in sizeAssignOptions" :key="i" class="sa-item" :class="{ 'sa-item--checked': item.checked }">
          <el-checkbox v-model="item.checked" />
          <span class="sa-item-size">{{ item.size }}</span>
          <el-input-number v-model="item.qty" :min="1" :max="999" size="small" controls-position="right" style="width:130px" :disabled="!item.checked" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showSizeAssignDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmSizeAssign">确认赋值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
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

const pagedDetails = computed(() => {
  const start = (detailPage.value - 1) * detailSize.value
  return form.details.slice(start, start + detailSize.value)
})

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
const sizeAssignOptions = ref([])
const sizeAssignAllChecked = ref(false)

const STATUS_MAP = { 0: '草稿', 1: '待审核', 2: '已审核', 3: '已驳回' }
const STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
const statusLabel = (s) => STATUS_MAP[s] || '未知'
const statusTagType = (s) => STATUS_TAG[s] || 'info'

const totalPrintQty = computed(() => form.details.reduce((sum, d) => sum + (d.printQty || 0), 0))

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
  for (const p of selectedProducts.value) {
    form.details.push({
      materialNumber: p.MATERIAL_NUMBER || '',
      articleNo: p.STYLE_NUMBER || '',
      articleName: p.MATERIAL_NAME || '',
      sizeGroup: p.SIZES || '',
      color: p.COLOR || '',
      ean13: p.EAN13 || '',
      brandName: p.BRAND_NAME || '',
      price: p.PRICE || 0,
      executionStandard: p.EXECUTION_STANDARD || '',
      sizeName: '',
      printQty: 0
    })
  }
  selectedProducts.value = []
}

// ─── Size Assign ────────────────────────────────────────
function openSizeAssign(row, index) {
  sizeAssignRowIndex.value = index
  sizeAssignProductName.value = `${row.articleNo} ${row.articleName}`
  const sizes = parseSizes(row.sizeGroup)
  sizeAssignOptions.value = sizes.map(s => ({
    size: s,
    checked: false,
    qty: 1
  }))
  sizeAssignAllChecked.value = false
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
  const insertBefore = idx
  // Remove original row
  form.details.splice(idx, 1)
  // Insert one row per selected size
  for (const c of checked) {
    form.details.splice(insertBefore, 0, {
      articleNo: orig.articleNo,
      articleName: orig.articleName,
      sizeGroup: orig.sizeGroup,
      color: orig.color,
      ean13: orig.ean13,
      brandName: orig.brandName,
      price: orig.price,
      executionStandard: orig.executionStandard,
      sizeName: c.size,
      printQty: c.qty
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
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
  align-items: stretch;
}
.info-item {
  flex: 1;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-right: 1px solid #f3f4f6;
}
.info-item:last-child { border-right: none; }
.info-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
  white-space: nowrap;
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

/* ─── Size Assign Dialog ────────────────────────────── */
.size-assign-dialog :deep(.el-dialog__body) {
  padding: 20px;
}
.sa-header {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 16px;
}
.sa-header-bar {
  width: 4px;
  height: 36px;
  background: linear-gradient(180deg, #3b82f6, #6366f1);
  border-radius: 2px;
  flex-shrink: 0;
}
.sa-header-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}
.sa-header-sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}
.sa-toolbar {
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid #f3f4f6;
}
.sa-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 300px;
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
}
.sa-item:hover {
  background: #f8fafc;
}
.sa-item--checked {
  background: #eff6ff;
  border-color: #bfdbfe;
}
.sa-item-size {
  width: 60px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}
</style>
