<template>
  <div class="detail-page">
    <!-- 头部栏 -->
    <div class="detail-header">
      <el-button type="warning" size="small" @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回列表
      </el-button>
      <span class="detail-header-title">编辑销售明细</span>
      <el-button v-if="hasPermission('bi:dw-sales:edit')" type="primary" size="small" :loading="saving" @click="handleSave">保存</el-button>
    </div>

    <div class="detail-content">
      <el-alert type="warning" show-icon :closable="false"
        title="该表由ETL按日期范围重灌，人工修改在下次刷新该日期段时会被覆盖，仅作临时修正" />

      <!-- 单据信息(只读) -->
      <div class="info-section">
        <div class="section-title"><el-icon><Document /></el-icon> 单据信息</div>
        <div class="info-grid">
          <div class="info-card span-2">
            <span class="info-card-label">单据号</span>
            <span class="info-card-value mono-value">{{ form.billNo || '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">单据ID</span>
            <span class="info-card-value mono-value">{{ form.billId ?? '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">明细ID</span>
            <span class="info-card-value mono-value">{{ form.itemId ?? '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">单据日期</span>
            <span class="info-card-value">{{ formatBillDate(form.billDate) }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">提交时间</span>
            <span class="info-card-value">{{ formatTime(form.billTime) }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">销售类型</span>
            <span class="info-card-value">{{ form.salesType || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">促销名称</span>
            <span class="info-card-value">{{ form.promotionName || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">网单来源单号</span>
            <span class="info-card-value">{{ form.omsSourcecode || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 货品信息(只读) -->
      <div class="info-section">
        <div class="section-title"><el-icon><Goods /></el-icon> 货品信息</div>
        <div class="info-grid">
          <div class="info-card span-2">
            <span class="info-card-label">货号</span>
            <span class="info-card-value mono-value">{{ form.productCode || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">款号</span>
            <span class="info-card-value mono-value">{{ form.productStyleNo || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">货品名称</span>
            <span class="info-card-value">{{ form.productName || '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">颜色</span>
            <span class="info-card-value">{{ form.colorsalias || '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">尺码</span>
            <span class="info-card-value">{{ form.sizes || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">条码</span>
            <span class="info-card-value mono-value">{{ form.barcode || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">新旧货</span>
            <span class="info-card-value">{{ form.newOldNameAdjust || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 会员与主播(只读) -->
      <div class="info-section">
        <div class="section-title"><el-icon><User /></el-icon> 会员与主播</div>
        <div class="info-grid">
          <div class="info-card span-2">
            <span class="info-card-label">会员卡号</span>
            <span class="info-card-value">{{ form.vipCode || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">会员手机号</span>
            <span class="info-card-value">{{ form.vipMobile || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">主播ID</span>
            <span class="info-card-value">{{ form.anchorSummaryid || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">主播名称</span>
            <span class="info-card-value">{{ form.anchorSummaryname || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 店铺与营业员(可编辑) -->
      <div class="info-section">
        <div class="section-title">
          <el-icon><Shop /></el-icon> 店铺与营业员
          <el-tag size="small" type="warning" effect="plain" style="margin-left:8px">可编辑</el-tag>
        </div>
        <div class="info-grid">
          <div class="info-card editable">
            <span class="info-card-label">店铺CODE</span>
            <el-input v-model="form.storeCode" placeholder="请输入店铺CODE" size="small" />
          </div>
          <div class="info-card editable span-3">
            <span class="info-card-label">店铺名称</span>
            <el-input v-model="form.storeName" placeholder="请输入店铺名称" size="small" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">营业员CODE</span>
            <el-input v-model="form.billPosCode" placeholder="请输入营业员CODE" size="small" />
          </div>
          <div class="info-card editable span-3">
            <span class="info-card-label">营业员名称</span>
            <el-input v-model="form.billPosName" placeholder="请输入营业员名称" size="small" />
          </div>
        </div>
      </div>

      <!-- 数量与金额(可编辑) -->
      <div class="info-section">
        <div class="section-title">
          <el-icon><Coin /></el-icon> 数量与金额
          <el-tag size="small" type="warning" effect="plain" style="margin-left:8px">可编辑</el-tag>
        </div>
        <div class="info-grid">
          <div class="info-card editable">
            <span class="info-card-label">数量</span>
            <el-input-number v-model="form.qty" :controls="false" size="small" style="width:100%" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">零售价</span>
            <el-input-number v-model="form.retailPrice" :controls="false" size="small" style="width:100%" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">零售金额</span>
            <el-input-number v-model="form.retailAmount" :controls="false" size="small" style="width:100%" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">成交金额</span>
            <el-input-number v-model="form.transactionAmount" :controls="false" size="small" style="width:100%" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">业绩金额</span>
            <el-input-number v-model="form.revenue" :controls="false" size="small" style="width:100%" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">重算业绩</span>
            <el-input-number v-model="form.recalcRevenue" :controls="false" size="small" style="width:100%" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'DwSalesMainEdit' })
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Document, Goods, User, Shop, Coin } from '@element-plus/icons-vue'
import { updateDwSalesMain } from '@/api/bi'
import { formatTime } from '@/utils/format'
import { usePermission } from '@/composables/usePermission'

const router = useRouter()
const { hasPermission } = usePermission()

// 行唯一身份 = BILL_ID + ITEM_ID；可编辑 = 店铺/营业员/数量金额，其余只读展示
const form = reactive({
  billId: null, billNo: '', itemId: null, billDate: null, billTime: null,
  salesType: '', promotionName: '', omsSourcecode: '',
  productCode: '', productStyleNo: '', productName: '', colorsalias: '', sizes: '', barcode: '', newOldNameAdjust: '',
  vipCode: '', vipMobile: '', anchorSummaryid: '', anchorSummaryname: '',
  storeCode: '', storeName: '', billPosCode: '', billPosName: '',
  qty: null, retailPrice: null, retailAmount: null,
  transactionAmount: null, revenue: null, recalcRevenue: null
})

const saving = ref(false)

// 列表页 router.push 的 state 携带整行数据；history.state 在刷新后仍保留
const stateRow = window.history.state?.row
if (stateRow) {
  Object.assign(form, stateRow)
} else {
  ElMessage.warning('缺少行数据，请从列表页进入')
  router.replace('/bi/dw-sales')
}

// BILL_DATE 为 NUMBER(8) YYYYMMDD
function formatBillDate(v) {
  if (v === null || v === undefined || v === '') return '-'
  const s = String(v)
  return s.length === 8 ? `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}` : s
}

function goBack() {
  router.push('/bi/dw-sales')
}

async function handleSave() {
  saving.value = true
  try {
    await updateDwSalesMain(form)
    ElMessage.success('修改成功')
    router.push('/bi/dw-sales')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.detail-page {
  background: #f1f5f9;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 10;
}
.detail-header-title {
  font-size: 17px;
  font-weight: 700;
  color: #111827;
}
.detail-content {
  padding: 12px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.info-section {
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px 16px;
  border: 1px solid #eef2f7;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #1e40af;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e0e7ff;
}
.section-title .el-icon { font-size: 16px; color: #6366f1; }
.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.info-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
  transition: border-color 0.2s;
}
.info-card:hover { border-color: #c7d2fe; }
.info-card.span-2 { grid-column: span 2; }
.info-card.span-3 { grid-column: span 3; }
.info-card-label {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
  letter-spacing: 0.06em;
}
.info-card-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  word-break: break-all;
  line-height: 1.5;
}
.mono-value {
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  font-weight: 700;
  color: #0f172a;
}
.info-card.editable {
  background: #fffbeb;
  border-color: #fde68a;
}
.info-card.editable:hover { border-color: #f59e0b; }
</style>
