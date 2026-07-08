<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="货号">
          <el-input v-model="query.materialNumber" placeholder="请输入货号" clearable style="min-width:130px;max-width:170px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="款号">
          <el-input v-model="query.styleNumber" placeholder="请输入款号" clearable style="min-width:130px;max-width:170px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="货品名称">
          <el-input v-model="query.materialName" placeholder="请输入货品名称" clearable style="min-width:150px;max-width:200px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-select v-model="query.brandId" placeholder="全部" clearable filterable style="min-width:110px;max-width:140px">
            <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ID" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">贴纸资料维护</span>
          <span class="card-header-sub">数据来源：伯俊 ERP</span>
        </div>
      </template>
      <div class="table-responsive">
          <el-table v-loading="loading" :data="tableData" border size="small" height="100%">
          <el-table-column label="#" width="45" fixed="left">
            <template #default="{ $index }">{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="MATERIAL_NUMBER" label="货号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
          <el-table-column prop="STYLE_NUMBER" label="款号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
          <el-table-column prop="MATERIAL_NAME" label="货品名称" width="200" show-overflow-tooltip fixed="left" class-name="col-key" />
          <el-table-column prop="BRAND_NAME" label="品牌" width="120" />
          <el-table-column prop="KIND_NAME" label="类别" width="100" show-overflow-tooltip />
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
          <el-table-column prop="SIZES" label="尺码组" width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="70" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES_LG"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES_LG } from '@/utils/appConfig'
import { getStickerDataPage, getStickerBrands } from '@/api/sticker'

defineOptions({ name: 'StickerData' })

const router = useRouter()

const { loading, tableData, total, query, loadData, handleSearch } = usePageQuery(getStickerDataPage, {
  materialNumber: '', styleNumber: '', materialName: '', brandId: ''
})

const brandList = ref([])

function onSearch() {
  if (!query.materialNumber?.trim() && !query.styleNumber?.trim() && !query.materialName?.trim() && !query.brandId) {
    ElMessage.warning('请至少输入一个搜索条件')
    return
  }
  handleSearch()
  sessionStorage.setItem('stickerDataQuery', JSON.stringify({ materialNumber: query.materialNumber, styleNumber: query.styleNumber, materialName: query.materialName, brandId: query.brandId }))
}

function onReset() {
  query.materialNumber = ''
  query.styleNumber = ''
  query.materialName = ''
  query.brandId = ''
  query.pageNum = 1
  tableData.value = []
  total.value = 0
  sessionStorage.removeItem('stickerDataQuery')
}

function handleEdit(row) {
  router.push({ name: 'StickerDataDetail', params: { materialNumber: row.MATERIAL_NUMBER }, state: { row: JSON.parse(JSON.stringify(row)) } })
}

async function loadBrands() {
  try {
    const { data } = await getStickerBrands()
    brandList.value = data || []
  } catch {}
}

function restoreAndLoad() {
  const saved = sessionStorage.getItem('stickerDataQuery')
  if (saved) {
    const q = JSON.parse(saved)
    query.materialNumber = q.materialNumber || ''
    query.styleNumber = q.styleNumber || ''
    query.materialName = q.materialName || ''
    query.brandId = q.brandId || ''
    loadData()
  }
}

onMounted(() => {
  loadBrands()
  restoreAndLoad()
})

onActivated(() => {
  restoreAndLoad()
})
</script>

<style scoped>
.card-header-sub { font-size: 13px; color: #909399; font-weight: 400; margin-left: 12px; }
</style>
