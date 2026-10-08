<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
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
    </el-card>

    <!-- 提示条不放搜索卡内:全局样式 .search-card .el-card__body 是 flex 行布局,卡内会与表单并排抢宽度 -->
    <el-alert type="info" :closable="false" show-icon class="data-scope-tip"
      title="每日库存 = 最近月结期末 + 月结后出入库流水累加(数仓 T+1 跑批,可查到昨日);正推数据自 2026-08-01 起,历史补录(7月末)不在本页口径" />

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
          <el-table-column v-if="!query.groupByProduct" prop="attrInstanceId" label="批次实例ID" width="110" align="center" show-overflow-tooltip />
          <el-table-column label="库存数量" width="100" align="right" sortable sort-by="total">
            <template #default="{ row }">{{ formatQty(row.total) }}</template>
          </el-table-column>
          <el-table-column label="吊牌价" width="100" align="right">
            <template #default="{ row }">{{ formatAmount(row.pricelist) }}</template>
          </el-table-column>
          <el-table-column label="吊牌金额" width="120" align="right">
            <template #default="{ row }">{{ formatAmount(row.amtList) }}</template>
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
  </div>
</template>

<script setup>
defineOptions({ name: 'DwStockDaily' })
import { ref, onMounted, onActivated } from 'vue'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { getDwStockPage, getDwStockStores } from '@/api/bi'
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
.data-scope-tip {
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
</style>
