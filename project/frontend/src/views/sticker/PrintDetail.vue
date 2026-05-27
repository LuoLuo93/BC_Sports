<template>
  <div class="page-container">
    <el-card shadow="never">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <div>
          <el-button @click="$router.push('/sticker/print')">返回列表</el-button>
          <span style="margin-left:16px;font-size:16px;font-weight:bold">申请单: {{ order.orderNo }}</span>
          <el-tag :type="statusTagType(order.status)" style="margin-left:8px">{{ statusLabel(order.status) }}</el-tag>
        </div>
        <el-button v-if="order.status === 2" type="primary" @click="handlePrint">打印贴纸</el-button>
      </div>

      <el-descriptions :column="3" border size="small" style="margin-bottom:16px">
        <el-descriptions-item label="申请人">{{ order.applicant }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ order.remark }}</el-descriptions-item>
        <el-descriptions-item v-if="order.reviewer" label="审核人">{{ order.reviewer }}</el-descriptions-item>
        <el-descriptions-item v-if="order.reviewTime" label="审核时间">{{ order.reviewTime }}</el-descriptions-item>
        <el-descriptions-item v-if="order.reviewRemark" label="审核意见">{{ order.reviewRemark }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="order.details" border size="small">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="articleNo" label="货号" width="120" />
        <el-table-column prop="articleName" label="货品名称" show-overflow-tooltip />
        <el-table-column prop="brandName" label="品牌" width="100" />
        <el-table-column prop="sizeGroup" label="尺码组" width="80" />
        <el-table-column prop="sizeName" label="尺码" width="80" />
        <el-table-column prop="ean13" label="EAN13" width="140" />
        <el-table-column prop="price" label="价格" width="80" />
        <el-table-column prop="printQty" label="打印数量" width="80" />
      </el-table>
    </el-card>

    <div id="print-area" class="print-area">
      <div class="sticker-grid">
        <template v-for="(item, idx) in printItems" :key="idx">
          <StickerTemplate :item="item" :index="idx" />
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPrintOrder } from '@/api/sticker'
import StickerTemplate from './StickerTemplate.vue'

const route = useRoute()
const order = ref({ details: [] })

const STATUS_MAP = { 0: '草稿', 1: '待审核', 2: '已审核', 3: '已驳回' }
const STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
const statusLabel = (s) => STATUS_MAP[s] || '未知'
const statusTagType = (s) => STATUS_TAG[s] || 'info'

const printItems = computed(() => {
  const items = []
  for (const d of (order.value.details || [])) {
    for (let i = 0; i < (d.printQty || 1); i++) {
      items.push(d)
    }
  }
  return items
})

async function loadData() {
  const { data } = await getPrintOrder(route.params.orderId)
  order.value = data
}

function handlePrint() {
  window.print()
}

onMounted(loadData)
</script>

<style scoped>
.print-area {
  display: none;
}
@media print {
  body * { visibility: hidden; }
  .print-area, .print-area * { visibility: visible; }
  .print-area {
    display: block;
    position: absolute;
    left: 0;
    top: 0;
  }
  .sticker-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 0;
  }
}
</style>
