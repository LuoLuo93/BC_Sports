<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <!-- wrapper 占满 body 宽度:绕开全局 .search-card .el-card__body 的 flex 行布局,让表单/提示各自独占一行 -->
      <div class="search-wrap">
        <el-form :model="query" inline>
        <el-form-item label="业务日期" required>
          <el-date-picker v-model="query.billdate" type="date" placeholder="选择日期" value-format="YYYYMMDD" :clearable="false" style="width:160px" />
        </el-form-item>
        <el-form-item label="店仓">
          <el-select v-model="query.storeId" placeholder="全部店仓" clearable filterable style="width:240px">
            <el-option v-for="s in storeOptions" :key="s.storeId" :label="`${s.storeCode} ${s.storeName}`" :value="s.storeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="货号">
          <el-input v-model="query.productCode" placeholder="如 CT135297-2" clearable style="width:170px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="品名">
          <el-input v-model="query.productName" placeholder="如 凉感长裤" clearable style="width:170px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="onReset">重置</el-button>
        </el-form-item>
        </el-form>
        <!-- 口径提示:红色小字,搜索行下方单独一行,左对齐可换行 -->
        <div class="calc-tip">每日库存 = 最近月结期末 + 月结后出入库流水累加(数仓 T+1 跑批,可查到昨日);正推数据自 2026-08-01 起,历史补录(7月末)不在本页口径</div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">数仓库存明细（DWD_STOCK_DAILY）</span>
          <el-radio-group v-model="query.groupByProduct" size="small" @change="onSearch">
            <el-radio-button :value="false">批次明细</el-radio-button>
            <el-radio-button :value="true">商品汇总</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="该日期暂无数据">
          <el-table-column label="#" width="70" align="center" fixed>
            <template #default="{ $index }">{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="业务日期" width="105" fixed>
            <template #default="{ row }">{{ formatBillDate(row.billdate) }}</template>
          </el-table-column>
          <el-table-column prop="storeCode" label="店仓编码" width="100" fixed show-overflow-tooltip />
          <el-table-column prop="storeName" label="店仓名称" min-width="140" fixed show-overflow-tooltip />
          <el-table-column prop="productCode" label="货号" min-width="125" show-overflow-tooltip />
          <el-table-column prop="productName" label="品名" min-width="150" show-overflow-tooltip />
          <el-table-column prop="cardName" label="品牌" min-width="120" show-overflow-tooltip />
          <el-table-column prop="seasonName" label="季节" width="80" show-overflow-tooltip />
          <el-table-column prop="sexName" label="性别" width="80" show-overflow-tooltip />
          <el-table-column prop="yearNo" label="年份" width="70" align="center" />
          <el-table-column v-if="!query.groupByProduct" prop="colorName" label="颜色" width="90" show-overflow-tooltip>
            <template #default="{ row }">{{ row.colorName || '-' }}</template>
          </el-table-column>
          <el-table-column v-if="!query.groupByProduct" prop="sizeName" label="尺码" width="70" align="center">
            <template #default="{ row }">{{ row.sizeName || '-' }}</template>
          </el-table-column>
          <el-table-column label="库存数量" width="120" align="right" sortable sort-by="total">
            <template #default="{ row }">{{ formatQty(row.total) }}</template>
          </el-table-column>
          <el-table-column v-if="query.groupByProduct" label="吊牌价" width="100" align="right">
            <template #default="{ row }">{{ formatAmount(row.pricelist) }}</template>
          </el-table-column>
          <el-table-column v-if="query.groupByProduct" label="吊牌金额" width="120" align="right">
            <template #default="{ row }">{{ formatAmount(row.amtList) }}</template>
          </el-table-column>
          <el-table-column v-if="!query.groupByProduct" label="操作" width="80" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" plain size="small" @click="openFlowDetail(row)">明细</el-button>
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
          @size-change="onSizeChange"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 库存构成明细:月结期末 + 构成流水 = 当前库存(与跑批正推口径一致,可对账) -->
    <el-dialog v-model="flowDialogVisible" width="880px" destroy-on-close>
      <template #header>
        <!-- 查询日期=库存快照日(红色加粗强调),不是流水日期 -->
        <span class="flow-dlg-title">
          <span class="flow-dlg-date">{{ formatBillDate(query.billdate) }}</span>
          库存构成 - {{ flowDialogTitle }}
        </span>
      </template>
      <div v-loading="flowLoading" class="flow-body">
        <el-descriptions v-if="flowDetail" :column="2" border size="small" class="flow-summary">
          <el-descriptions-item label="月结期间">{{ flowDetail.yearmonth }}</el-descriptions-item>
          <el-descriptions-item label="月结期末数量">{{ formatQty(flowDetail.monthQty) }}</el-descriptions-item>
          <el-descriptions-item label="流水合计">{{ formatQty(flowDetail.flowSum) }}</el-descriptions-item>
          <el-descriptions-item label="月结+流水=库存">{{ formatQty(flowDetail.stockQty) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="flowDetail && flowDetail.flows.length" class="flow-filter">
          <el-input v-model="flowFilter.docno" placeholder="单据号" clearable size="small" style="width:200px" />
          <el-date-picker v-model="flowFilter.date" type="date" placeholder="业务日期" value-format="YYYYMMDD" clearable size="small" style="width:150px" />
          <span class="flow-filter-count">匹配 {{ flowFiltered.length }} / {{ flowDetail.flows.length }} 笔</span>
        </div>
        <el-table v-if="flowDetail" :data="flowPagedData" border stripe size="small" max-height="420">
          <el-table-column label="业务日期" width="105">
            <template #default="{ row }">{{ formatBillDate(row.changedate) }}</template>
          </el-table-column>
          <el-table-column prop="docno" label="单据号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="doctype" label="单据类型" min-width="110" show-overflow-tooltip />
          <el-table-column label="业务类型" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <span :title="row.billtype">{{ billtypeText(row.billtype) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="变动数量" width="100" align="right">
            <template #default="{ row }">
              <span :class="Number(row.qtychange) >= 0 ? 'qty-in' : 'qty-out'">{{ Number(row.qtychange) > 0 ? '+' : '' }}{{ formatQty(row.qtychange) }}</span>
            </template>
          </el-table-column>
          <template #empty>月结后无流水,当前库存 = 月结期末数量</template>
        </el-table>
        <div v-if="flowDetail && flowFiltered.length > flowPage.pageSize" class="flow-pagination">
          <el-pagination
            v-model:current-page="flowPage.pageNum"
            :page-size="flowPage.pageSize"
            :total="flowFiltered.length"
            layout="total, prev, pager, next"
            small
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'DwStockDaily' })
import { ref, reactive, computed, watch, onMounted, onActivated } from 'vue'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { getDwStockPage, getDwStockStores, getDwStockFlowDetail } from '@/api/bi'
import { PAGE_SIZES } from '@/utils/appConfig'
import { usePageQuery } from '@/composables/usePageQuery'

// 默认业务日期=昨天(库存 T+1 跑批,今天的数据还没算)
function yesterdayYmd() {
  const d = new Date(Date.now() - 86400000)
  return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
}

const { loading, tableData, total, query, loadData, handleSearch } = usePageQuery(getDwStockPage, {
  billdate: yesterdayYmd(),
  storeId: null,
  productCode: '',
  productName: '',
  groupByProduct: false
})

const storeOptions = ref([])

function onSearch() {
  handleSearch()
}

function onReset() {
  query.billdate = yesterdayYmd()
  query.storeId = null
  query.productCode = ''
  query.productName = ''
  query.groupByProduct = false
  handleSearch()
}

function onSizeChange() {
  handleSearch()
}

function onPageChange() {
  loadData()
}

async function loadStores() {
  try {
    const res = await getDwStockStores()
    storeOptions.value = res.data || []
  } catch {
    // 拦截器已统一 toast,下拉选项加载失败不阻塞主列表
  }
}

// ===== 库存构成明细弹窗 =====
// 伯俊业务类型代码 → 中文(用户提供的对应关系;未收录代码原样显示)
const BILLTYPE_MAP = {
  M_SALEOUT: '销售出库',
  M_RET_SALEOUT: '销售退货出库',
  M_TRANSFEROUT: '调拨出库',
  M_RET_PUROUT: '采购退货出库',
  M_SALEIN: '销售入库',
  M_RET_SALEIN: '销售退货入库',
  M_TRANSFERIN: '调拨入库',
  M_PURCHASEIN: '采购入库',
  M_OTHER_INOUT: '其他出入库',
  M_RETAIL: '零售',
  M_INVENTORY: '盘点损益',
  M_AGTPURIN: '代销采购入库',
  M_AGTRET_PUROUT: '代销采购退货出库',
  O2O_SOOUT: '云仓发货单',
  CRO_SALEOUT: '跨级销售出库',
  CRO_RETSALEIN: '跨级销售退货入库',
  CRO_SALEIN: '跨级销售入库',
  CRO_RETSALEOUT: '跨级销售退货出库',
  M_AGTSALEIN: '代销销售入库',
  M_AGTSALEOUT: '代销销售出库',
  M_AGTRET_SALEOUT: '代销销售退货出库',
  M_AGTRET_SALEIN: '代销销售退货入库',
  M_JITOUT: 'JIT出库',
  M_OUT_BATCHES: '分批出库',
  M_IN_BATCHES: '分批入库'
}
function billtypeText(code) {
  if (!code) return '-'
  return BILLTYPE_MAP[code] || code
}

// 流水本地分页(高频组合月结后可达上千笔,全量渲染会卡)+ 弹窗内过滤(单据号/业务日期)
const flowPage = reactive({ pageNum: 1, pageSize: 20 })
const flowFilter = reactive({ docno: '', date: null })

const flowFiltered = computed(() => {
  const list = flowDetail.value?.flows || []
  const kw = flowFilter.docno.trim().toLowerCase()
  return list.filter(f => {
    if (kw && !(f.docno || '').toLowerCase().includes(kw)) return false
    if (flowFilter.date && String(f.changedate) !== flowFilter.date) return false
    return true
  })
})

const flowPagedData = computed(() => {
  const list = flowFiltered.value
  const start = (flowPage.pageNum - 1) * flowPage.pageSize
  return list.slice(start, start + flowPage.pageSize)
})

// 过滤条件变化回到第 1 页
watch([() => flowFilter.docno, () => flowFilter.date], () => { flowPage.pageNum = 1 })

const flowDialogVisible = ref(false)
const flowLoading = ref(false)
const flowDetail = ref(null)
const flowDialogTitle = ref('')

async function openFlowDetail(row) {
  flowDialogVisible.value = true
  flowLoading.value = true
  flowDetail.value = null
  flowPage.pageNum = 1
  flowFilter.docno = ''
  flowFilter.date = null
  flowDialogTitle.value = `${row.storeName} ${row.productCode}${row.colorName ? ' ' + row.colorName : ''}${row.sizeName ? ' ' + row.sizeName : ''}`
  try {
    const res = await getDwStockFlowDetail({
      billdate: query.billdate,
      storeId: row.storeId,
      productId: row.productId,
      attrInstanceId: row.attrInstanceId == null ? '' : row.attrInstanceId
    })
    flowDetail.value = res.data
  } catch {
    // 拦截器已统一 toast
  } finally {
    flowLoading.value = false
  }
}

// BILLDATE 为 NUMBER(8) YYYYMMDD
function formatBillDate(v) {
  if (v === null || v === undefined || v === '') return '-'
  const s = String(v)
  return s.length === 8 ? `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}` : s
}

function formatQty(n) {
  if (n === null || n === undefined) return '-'
  return Number(n).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatAmount(n) {
  if (n === null || n === undefined) return '-'
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  loadStores()
  loadData()
})

onActivated(() => {
  loadData()
})
</script>

<style scoped>
.search-card {
  margin-bottom: 12px;
}
.search-wrap {
  width: 100%;
}
/* 口径提示:红色小字,搜索行下方单独一行左对齐,窄屏可自然换行 */
.calc-tip {
  font-size: 12px;
  color: var(--el-color-danger);
  text-align: left;
  line-height: 1.6;
  margin-top: 4px;
}
/* 构成流水数量:+绿 -红 */
.qty-in {
  color: var(--el-color-success);
  font-weight: 600;
}
.qty-out {
  color: var(--el-color-danger);
  font-weight: 600;
}
.flow-summary {
  margin-bottom: 10px;
}
/* 弹窗标题:查询日期(库存快照日)红色加粗 */
.flow-dlg-title {
  font-size: 16px;
  font-weight: 600;
}
.flow-dlg-date {
  color: var(--el-color-danger);
  font-weight: 700;
  margin-right: 4px;
}
/* 弹窗流水过滤行 */
.flow-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.flow-filter-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
/* 表头单元格不换行:sortable 列的"库存数量"+排序箭头挤在同一行 */
:deep(.el-table .el-table__header th .cell) {
  white-space: nowrap;
}
.flow-pagination {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
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
</style>
