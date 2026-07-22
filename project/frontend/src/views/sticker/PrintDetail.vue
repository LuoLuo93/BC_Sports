<template>
  <div class="page-container">
    <div class="form-view">
      <!-- 紧凑头部栏 -->
      <div class="form-header">
        <el-button type="warning" size="small" @click="$router.push('/sticker/print')">返回列表</el-button>
        <span class="form-header-title">查看打印申请单</span>
        <span v-if="order.status === 2" style="color:#f97316;font-size:12px;font-weight:600">已审核</span>
        <span v-else></span>
      </div>

      <!-- 信息行 -->
      <div class="form-info-row">
        <div class="info-item">
          <span class="info-label">申请单号</span>
          <span class="info-value">{{ order.orderNo || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">申请人</span>
          <span class="info-value">{{ order.applicant || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">申请时间</span>
          <span class="info-value">{{ formatTime(order.createTime) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">状态</span>
          <span :class="['status-badge', 'status-' + order.status]">{{ statusLabel(order.status) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">备注</span>
          <span class="info-value">{{ order.remark || '-' }}</span>
        </div>
        <div v-if="order.reviewer" class="info-item">
          <span class="info-label">审核人</span>
          <span class="info-value">{{ order.reviewer }}</span>
        </div>
        <div v-if="order.reviewTime" class="info-item">
          <span class="info-label">审核时间</span>
          <span class="info-value">{{ order.reviewTime }}</span>
        </div>
        <div v-if="order.reviewRemark" class="info-item" style="flex:1.5">
          <span class="info-label">审核意见</span>
          <span class="info-value">{{ order.reviewRemark }}</span>
        </div>
      </div>

      <!-- 明细区域 -->
      <div class="detail-panel">
        <div class="panel-bar">
          <span class="panel-bar-title">申请明细 <span class="detail-summary">共 {{ order.details.length }} 条，合计 <em class="total-qty">{{ totalPrintQty }}</em> 张</span></span>
        </div>
        <el-table :data="pagedDetails" border size="small" height="100%">
          <el-table-column label="#" width="45" fixed="left">
            <template #default="{ $index }">{{ (detailPage - 1) * detailSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="materialNumber" label="货号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
          <el-table-column prop="styleNumber" label="款号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
          <el-table-column prop="materialName" label="货品名称" width="200" show-overflow-tooltip fixed="left" class-name="col-key" />
          <el-table-column prop="brandName" label="品牌" width="120" />
          <el-table-column prop="kindName" label="类别" width="100" />
          <el-table-column prop="color" label="颜色" width="90" />
          <el-table-column prop="price" label="价格" width="120">
            <template #default="{ row }">{{ row.price ? Number(row.price).toFixed(5) : '-' }}</template>
          </el-table-column>
          <el-table-column prop="executionStandard" label="执行标准" width="160">
            <template #default="{ row }">{{ row.executionStandard || '-' }}</template>
          </el-table-column>
          <el-table-column label="面料/辅料成分" width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.fabCode || row.fabElement || row.acCode || row.accElement">
                <span v-if="row.fabCode">面料1:{{ row.fabCode }}</span>
                <span v-if="row.fabCode && (row.fabElement || row.acCode || row.accElement)"> / </span>
                <span v-if="row.fabElement">面料2:{{ row.fabElement }}</span>
                <span v-if="row.fabElement && (row.acCode || row.accElement)"> / </span>
                <span v-if="row.acCode">辅料1:{{ row.acCode }}</span>
                <span v-if="row.acCode && row.accElement"> / </span>
                <span v-if="row.accElement">辅料2:{{ row.accElement }}</span>
              </span>
              <span v-else style="color:#d9d9d9">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="ean13" label="EAN13" width="150">
            <template #default="{ row }">{{ row.ean13 || '-' }}</template>
          </el-table-column>
          <el-table-column prop="barcode" label="条码" width="170" show-overflow-tooltip>
            <template #default="{ row }">{{ row.barcode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="localGroupName" label="矫正尺码组" width="140" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag v-if="row.localGroupName" type="success" size="small" effect="dark">{{ row.localGroupName }}</el-tag>
              <span v-else style="color:#d9d9d9">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="localSizeName" label="矫正尺码" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.localSizeName" type="primary" size="small" effect="dark">{{ row.localSizeName }}</el-tag>
              <span v-else style="color:#d9d9d9">-</span>
            </template>
          </el-table-column>
          <el-table-column label="尺码" width="80" align="center" fixed="right">
            <template #default="{ row }">
              <el-tag v-if="row.sizeName" type="warning" size="small" effect="dark">{{ row.sizeName }}</el-tag>
              <span v-else style="color:#d9d9d9">-</span>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="70" align="center" fixed="right">
            <template #default="{ row }">
              <span v-if="row.printQty" class="detail-qty">{{ row.printQty }}</span>
              <span v-else style="color:#d9d9d9">-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="order.details.length > 0"
          v-model:current-page="detailPage"
          v-model:page-size="detailSize"
          :total="order.details.length"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          size="small"
          style="margin-top:8px;justify-content:flex-end"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPrintOrder } from '@/api/sticker'
import { PAGE_SIZES } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'

const route = useRoute()
const order = ref({ details: [] })

const detailPage = ref(1)
const detailSize = ref(50)

const pagedDetails = computed(() => {
  const start = (detailPage.value - 1) * detailSize.value
  return (order.value.details || []).slice(start, start + detailSize.value)
})

const STATUS_MAP = { 0: '草稿', 1: '待审核', 2: '已审核', 3: '已驳回' }
const STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
const statusLabel = (s) => STATUS_MAP[s] || '未知'
const statusTagType = (s) => STATUS_TAG[s] || 'info'

const totalPrintQty = computed(() => (order.value.details || []).reduce((sum, d) => sum + (d.printQty || 0), 0))

async function loadData() {
  const { data } = await getPrintOrder(route.params.orderId)
  order.value = data
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #f0f2f5;
  flex-shrink: 0;
}
.form-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  text-align: center;
  flex: 1;
}
.form-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.header-print-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-info-row {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
  align-items: stretch;
  flex-wrap: wrap;
}
.info-item {
  flex: 1;
  min-width: 120px;
  padding: 8px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  justify-content: center;
}
.info-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  line-height: 1.4;
}
.info-value {
  font-size: 14px;
  color: #111827;
  font-weight: 600;
  line-height: 1.4;
  word-break: break-all;
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

</style>
