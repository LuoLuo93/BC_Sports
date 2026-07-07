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
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="MATERIAL_NUMBER" label="货号" width="150" show-overflow-tooltip />
          <el-table-column prop="STYLE_NUMBER" label="款号" width="150" show-overflow-tooltip />
          <el-table-column prop="MATERIAL_NAME" label="货品名称" min-width="240" show-overflow-tooltip />
          <el-table-column prop="BRAND_NAME" label="品牌" width="100" />
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
          <el-table-column prop="SIZES" label="尺码组" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>
      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES_LG"
          layout="total, sizes, prev, pager, next"
          @size-change="() => { query.pageNum = 1; loadData() }"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { getStickerDataPage, getStickerBrands } from '@/api/sticker'

defineOptions({ name: 'StickerData' })

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
}

function onReset() {
  query.materialNumber = ''
  query.styleNumber = ''
  query.materialName = ''
  query.brandId = ''
  query.pageNum = 1
  tableData.value = []
  total.value = 0
}

async function loadBrands() {
  try {
    const { data } = await getStickerBrands()
    brandList.value = data || []
  } catch {}
}

onMounted(loadBrands)
</script>

<style scoped>
.card-header-sub { font-size: 13px; color: #909399; font-weight: 400; margin-left: 12px; }
</style>
