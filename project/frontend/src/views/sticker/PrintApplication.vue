<template>
  <div class="page-container">
    <!-- ========== 列表视图 ========== -->
    <template v-if="!formVisible">
      <el-card shadow="never" class="search-card">
        <el-form inline>
          <el-form-item label="申请单号">
            <el-input v-model="query.orderNo" placeholder="申请单号" clearable style="min-width:170px;max-width:220px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="申请人">
            <el-input v-model="query.applicant" placeholder="申请人" clearable style="min-width:130px;max-width:170px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="min-width:110px;max-width:130px">
              <el-option label="全部" :value="null" />
              <el-option label="草稿" :value="0" />
              <el-option label="待审核" :value="1" />
              <el-option label="已审核" :value="2" />
              <el-option label="已驳回" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker v-model="query.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" style="min-width:135px;max-width:155px" />
            <span style="margin:0 4px;color:#999">-</span>
            <el-date-picker v-model="query.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" style="min-width:135px;max-width:155px" />
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
            <span class="card-header-title">贴纸打印申请</span>
            <div class="header-actions">
              <el-button v-if="hasPermission('sticker:print:add')" type="primary" size="small" @click="handleCreate">新建申请</el-button>
            </div>
          </div>
        </template>
        <div class="table-responsive">
          <el-table v-loading="loading" :data="tableData" border stripe>
            <el-table-column prop="orderNo" label="申请单号" width="200" />
            <el-table-column prop="applicant" label="申请人" width="120" />
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <span :class="['status-badge', 'status-' + row.status]">{{ statusLabel(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="460" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="success" plain size="small" @click="handleView(row)">查看</el-button>
                <el-button v-if="row.status === 0 && hasPermission('sticker:print:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button v-if="row.status === 0 && hasPermission('sticker:print:edit')" type="warning" plain size="small" @click="handleSubmit(row)">提交</el-button>
                <el-button v-if="row.status === 0 && hasPermission('sticker:print:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
                <el-button v-if="row.status === 1 && hasPermission('sticker:print:review')" type="success" plain size="small" @click="handleReview(row)">审核</el-button>
                <el-button v-if="row.status === 2 && hasPermission('sticker:print:execute')" type="primary" size="small" @click="handleAgentPrint(row)">打印</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="pagination-wrapper--sm">
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            :page-sizes="PAGE_SIZES"
            layout="total, sizes, prev, pager, next"
            @size-change="() => { query.pageNum = 1; loadData() }"
            @current-change="loadData"
          />
        </div>
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
            <span class="info-value">{{ form.orderNo || '保存后自动生成' }}</span>
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
            <span class="info-value">{{ formatTime(form.createTime) }}</span>
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
              <el-input v-model="searchMaterialNumber" placeholder="货号" size="small" clearable style="min-width:130px;max-width:170px" @keyup.enter="searchProductsAction" />
              <el-input v-model="searchStyleNumber" placeholder="款号" size="small" clearable style="min-width:130px;max-width:170px" @keyup.enter="searchProductsAction" />
              <el-input v-model="searchMaterialName" placeholder="商品名称" size="small" clearable style="min-width:150px;max-width:200px" @keyup.enter="searchProductsAction" />
              <el-select v-model="searchBrandId" placeholder="品牌" size="small" clearable filterable style="min-width:110px;max-width:140px">
                <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ID" />
              </el-select>
              <el-button type="primary" size="small" @click="searchProductsAction">搜索</el-button>
              <el-button type="success" size="small" :disabled="!selectedProducts.length" @click="confirmProductSelect">
                添加({{ selectedProducts.length }})
              </el-button>
            </div>
            <el-table
              v-loading="productLoading"
              :data="productList"
              border
              size="small"
              ref="productTableRef"
              height="100%"
              @selection-change="handleProductSelect"
              @row-click="onProductRowClick"
              :row-class-name="productRowClassName"
            >
              <el-table-column type="selection" width="35" fixed="left" />
              <el-table-column type="index" label="#" width="45" fixed="left" />
              <el-table-column prop="MATERIAL_NUMBER" label="货号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
              <el-table-column prop="STYLE_NUMBER" label="款号" width="170" show-overflow-tooltip class-name="col-key" />
              <el-table-column prop="MATERIAL_NAME" label="商品名称" width="200" show-overflow-tooltip class-name="col-key" />
              <el-table-column prop="BRAND_NAME" label="品牌" width="120" show-overflow-tooltip />
              <el-table-column prop="KIND_NAME" label="类别" width="100" show-overflow-tooltip />
              <el-table-column prop="COLOR" label="颜色" width="90" />
              <el-table-column prop="PRICE" label="价格" width="120">
                <template #default="{ row }">{{ row.PRICE != null ? Number(row.PRICE).toFixed(5) : '-' }}</template>
              </el-table-column>
              <el-table-column prop="EXECUTION_STANDARD" label="执行标准" width="160">
                <template #default="{ row }">{{ row.EXECUTION_STANDARD || '-' }}</template>
              </el-table-column>
              <el-table-column prop="SAFETY_CATEGORY" label="安全类别" width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ row.SAFETY_CATEGORY || '-' }}</template>
              </el-table-column>
              <el-table-column label="面料/辅料成分" width="240" show-overflow-tooltip>
                <template #default="{ row }">
                  <span v-if="row.FAB_CODE || row.FAB_ELEMENT || row.AC_CODE || row.ACC_ELEMENT">
                    <span v-if="row.FAB_CODE">面料1:{{ row.FAB_CODE }}</span>
                    <span v-if="row.FAB_CODE && (row.FAB_ELEMENT || row.AC_CODE || row.ACC_ELEMENT)"> / </span>
                    <span v-if="row.FAB_ELEMENT">面料2:{{ row.FAB_ELEMENT }}</span>
                    <span v-if="row.FAB_ELEMENT && (row.AC_CODE || row.ACC_ELEMENT)"> / </span>
                    <span v-if="row.AC_CODE">辅料1:{{ row.AC_CODE }}</span>
                    <span v-if="row.AC_CODE && row.ACC_ELEMENT"> / </span>
                    <span v-if="row.ACC_ELEMENT">辅料2:{{ row.ACC_ELEMENT }}</span>
                  </span>
                  <span v-else style="color:#d9d9d9">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="EAN13" label="EAN13" width="150">
                <template #default="{ row }">{{ row.EAN13 || '-' }}</template>
              </el-table-column>
              <el-table-column prop="SIZE_GROUP_NAME" label="矫正尺码组" width="150" show-overflow-tooltip>
                <template #default="{ row }">
                  <el-tag v-if="row.SIZE_GROUP_NAME" size="small" type="success" effect="plain">{{ row.SIZE_GROUP_NAME }}</el-tag>
                  <span v-else style="color:#d9d9d9">-</span>
                </template>
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
                <el-input v-model="detailKeyword" placeholder="搜索货号/款号/货品名称" size="small" clearable style="min-width:180px;max-width:240px" />
                <el-button size="small" :disabled="!selectedRows.length" @click="handleBatchSetQty">批量设置数量</el-button>
                <el-button type="danger" size="small" :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
              </div>
            </div>
            <el-table :data="pagedDetails" border size="small" class="detail-table" @selection-change="handleDetailSelect" ref="detailTableRef" height="100%">
              <el-table-column type="selection" width="35" fixed="left" />
              <el-table-column label="#" width="45" fixed="left">
                <template #default="{ $index }">{{ (detailPage - 1) * detailSize + $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="materialNumber" label="货号" width="170" show-overflow-tooltip fixed="left" class-name="col-key" />
              <el-table-column prop="styleNumber" label="款号" width="170" show-overflow-tooltip class-name="col-key" />
              <el-table-column prop="materialName" label="货品名称" width="200" show-overflow-tooltip class-name="col-key" />
              <el-table-column prop="brandName" label="品牌" width="120" />
              <el-table-column prop="kindName" label="类别" width="100" />
              <el-table-column prop="color" label="颜色" width="90" />
              <el-table-column prop="price" label="价格" width="120">
                <template #default="{ row }">{{ row.price ? Number(row.price).toFixed(5) : '-' }}</template>
              </el-table-column>
              <el-table-column prop="executionStandard" label="执行标准" width="160">
                <template #default="{ row }">{{ row.executionStandard || '-' }}</template>
              </el-table-column>
              <el-table-column prop="safetyCategory" label="安全类别" width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ row.safetyCategory || '-' }}</template>
              </el-table-column>
              <el-table-column prop="ean13" label="EAN13" width="150">
                <template #default="{ row }">{{ row.ean13 || '-' }}</template>
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
              <el-table-column prop="barcode" label="条码" width="170" show-overflow-tooltip>
                <template #default="{ row }">{{ row.barcode || '-' }}</template>
              </el-table-column>
              <el-table-column label="矫正尺码组" width="140" fixed="right">
                <template #default="{ row }">
                  <span v-if="row.localGroupName">{{ row.localGroupName }}</span>
                  <span v-else style="color:#c0c4cc">无</span>
                </template>
              </el-table-column>
              <el-table-column label="矫正尺码" width="120" fixed="right">
                <template #default="{ row }">
                  <el-tag v-if="row.localSizeName" type="success" size="small" effect="plain">{{ row.localSizeName }}</el-tag>
                  <span v-else style="color:#c0c4cc">无</span>
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
              :page-sizes="PAGE_SIZES"
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
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="showBatchQtyDialog = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" @click="confirmBatchQty">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审核 -->
    <el-dialog v-model="reviewVisible" title="审核" width="400px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.status">
            <el-radio :value="2">通过</el-radio>
            <el-radio :value="3">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="reviewVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" @click="confirmReview">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 打印（通过 Agent 下发） -->
    <el-dialog v-model="agentPrintVisible" title="打印 · 选择 Agent" width="720px" :close-on-click-modal="false">
      <div v-if="agentLoading" style="text-align:center;padding:20px">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <p style="margin-top:10px;color:#909399">正在加载 Agent 列表...</p>
      </div>
      <template v-else>
        <p style="margin-bottom:12px;color:#606266;font-size:13px">
          选择一个在线的 Agent 下发打印任务（共 <b>{{ agentOrderDetailCount }}</b> 条明细）
        </p>
        <el-table :data="agentList" border size="small" style="width:100%">
          <el-table-column label="选择" width="55" align="center">
            <template #default="{ row }">
              <el-radio v-model="selectedAgentId" :value="row.agentId" :disabled="row.status !== 1" @change="onAgentRadioChange(row)">&nbsp;</el-radio>
            </template>
          </el-table-column>
          <el-table-column prop="agentId" label="Agent ID" width="140" />
          <el-table-column prop="agentName" label="名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="ipAddress" label="IP 地址" width="140" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="agentPrintVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" @click="confirmAgentPrint"
            :loading="agentPrinting" :disabled="!selectedAgent">
            {{ agentPrinting ? '下发中...' : '下发打印任务' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 尺码赋值 -->
    <el-dialog v-model="showSizeAssignDialog" title="尺码赋值" width="720px" class="size-assign-dialog" destroy-on-close>
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
        <el-input v-model="sizeAssignSearch" placeholder="搜索尺码" size="small" clearable style="width:160px" />
        <el-checkbox v-model="sizeAssignAllChecked" @change="toggleSizeAssignAll">全选</el-checkbox>
        <span class="sa-toolbar-count">已选 <b>{{ sizeAssignCheckedCount }}</b> / {{ sizeAssignOptions.length }} 个尺码</span>
      </div>
      <div class="sa-list">
        <div v-for="(item, i) in filteredSizeAssignOptions" :key="i" class="sa-item" :class="{ 'sa-item--checked': item.checked, 'sa-item--existing': item.existing }" @click="item.checked = !item.checked">
          <el-checkbox v-model="item.checked" @click.stop />
          <span class="sa-item-size">{{ item.size }}</span>
          <el-tag v-if="item.existing" type="warning" size="small" effect="plain">已添加</el-tag>
          <el-input-number v-model="item.qty" :min="1" :max="999" size="small" controls-position="right" style="width:110px" :disabled="!item.checked" @click.stop />
        </div>
        <div v-if="!filteredSizeAssignOptions.length" class="sa-empty">无匹配尺码</div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="showSizeAssignDialog = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" @click="confirmSizeAssign">确认 ({{ sizeAssignCheckedCount }})</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Search, RefreshRight } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getPrintOrderPage, getPrintOrder, createPrintOrder, updatePrintOrder, submitPrintOrder, reviewPrintOrder, deletePrintOrder, searchProducts, getProductSizes, createAgentPrintTasks, getSizeGroupSizes } from '@/api/sticker'
import { getCommonBrands } from '@/api/common'
import { usePageQuery } from '@/composables/usePageQuery'
import { usePermission } from '@/composables/usePermission'
import { useAuthStore } from '@/stores/auth'
import { PAGE_SIZES } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'

defineOptions({ name: 'StickerPrintList' })

const { hasPermission } = usePermission()
const authStore = useAuthStore()
const viewAll = computed(() => hasPermission('sticker:print:all'))

const router = useRouter()

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(
  (params) => getPrintOrderPage({ ...params, viewAll: viewAll.value }),
  { status: null, orderNo: '', applicant: '', startDate: '', endDate: '' }
)

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
const productTableRef = ref()

// Review
const reviewVisible = ref(false)
const reviewOrderId = ref('')
const reviewForm = reactive({ status: 2, reviewRemark: '' })

// Agent Print
const agentPrintVisible = ref(false)
const agentLoading = ref(false)
const agentPrinting = ref(false)
const agentList = ref([])
const selectedAgent = ref(null)
const selectedAgentId = ref('')
const agentOrder = ref(null)
const agentOrderDetailCount = computed(() => agentOrder.value?.details?.length || 0)

// Size assign
const showSizeAssignDialog = ref(false)
const sizeAssignRowIndex = ref(-1)
const sizeAssignProductName = ref('')
const sizeAssignMeta = reactive({ brand: '', color: '', ean13: '' })
const sizeAssignOptions = ref([])
const sizeAssignAllChecked = ref(false)
const sizeAssignSearch = ref('')
const sizeAssignCheckedCount = computed(() => sizeAssignOptions.value.filter(o => o.checked).length)
const filteredSizeAssignOptions = computed(() => {
  const kw = sizeAssignSearch.value.trim().toLowerCase()
  if (!kw) return sizeAssignOptions.value
  return sizeAssignOptions.value.filter(o => (o.size || '').toLowerCase().includes(kw))
})

const STATUS_MAP = { 0: '草稿', 1: '待审核', 2: '已审核', 3: '已驳回' }
const STATUS_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
const statusLabel = (s) => STATUS_MAP[s] || '未知'
const statusTagType = (s) => STATUS_TAG[s] || 'info'

// ─── List (usePageQuery handles loadData, handleSearch, resetQuery) ────

function handleCreate() {
  isEdit.value = false
  editOrderId.value = ''
  form.orderNo = ''
  form.applicant = authStore.nickname || authStore.username || ''
  form.deptName = authStore.deptName || ''
  form.createTime = formatTime(new Date())
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
  form.details = (data.details || []).map(d => ({
    ...d,
    brandId: d.brandId ? String(d.brandId) : '',
    kindId: d.kindId ? String(d.kindId) : '',
    localGroupId: d.localGroupId ? String(d.localGroupId) : '',
    localSizeId: d.localSizeId ? String(d.localSizeId) : ''
  }))
  selectedRows.value = []
  searchMaterialNumber.value = ''
  searchStyleNumber.value = ''
  searchMaterialName.value = ''
  searchBrandId.value = ''
  productList.value = []
  selectedProducts.value = []
  loadBrands()
  formVisible.value = true
  // 编辑模式：预加载已有矫正组的尺码缓存(确保 el-select 显示 label 而非 id)，
  // 随后按 sizeName↔sizeCode 规则对已存明细重新匹配矫正尺码，覆盖历史数据(所见即所得)
  await preloadLocalCaches()
  for (const d of form.details) {
    if (d.sizeName && d.localGroupId) {
      autoMatchLocalSize(d)
    }
  }
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

// ─── Agent 打印 ──────────────────────
async function handleAgentPrint(row) {
  agentOrder.value = null
  selectedAgent.value = null
  selectedAgentId.value = ''
  agentPrinting.value = false

  // 加载订单详情
  try {
    const { data } = await getPrintOrder(row.id)
    agentOrder.value = data
  } catch {
    ElMessage.error('获取订单详情失败')
    return
  }

  // 加载 Agent 列表
  agentLoading.value = true
  try {
    const { data } = await request.get('/api/agent/list')
    agentList.value = data || []
  } catch {
    agentList.value = []
    ElMessage.error('获取 Agent 列表失败')
  } finally {
    agentLoading.value = false
  }

  agentPrintVisible.value = true
}

function onAgentRadioChange(row) {
  selectedAgent.value = row
}

async function confirmAgentPrint() {
  if (!selectedAgent.value) {
    ElMessage.warning('请选择一个 Agent')
    return
  }
  if (selectedAgent.value.status !== 1) {
    ElMessage.warning('只能选择在线的 Agent')
    return
  }

  agentPrinting.value = true
  try {
    const res = await createAgentPrintTasks(agentOrder.value.id, selectedAgent.value.agentId)
    if (res.code === 200) {
      const taskCount = typeof res.data === 'string' ? res.data.split(',').length : agentOrderDetailCount.value
      ElMessage.success(`已下发 ${taskCount} 个打印任务到 ${selectedAgent.value.agentName || selectedAgent.value.agentId}`)
      agentPrintVisible.value = false
    } else {
      ElMessage.error(res.message || '下发失败')
    }
  } catch (e) {
    ElMessage.error('下发失败: ' + (e.message || '未知错误'))
  } finally {
    agentPrinting.value = false
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
    const { data } = await getCommonBrands()
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

/** 点击行任意位置切换选中（不用非得点最前面的 checkbox） */
function onProductRowClick(row) {
  productTableRef.value?.toggleRowSelection(row)
}

/** 选中的行加高亮 class */
function productRowClassName({ row }) {
  return selectedProducts.value.some(p => p === row) ? 'row-selected' : ''
}

function confirmProductSelect() {
  const skipped = []
  const groupIdsToPreload = new Set()
  for (const p of selectedProducts.value) {
    const mn = p.MATERIAL_NUMBER || ''
    const sn = p.STYLE_NUMBER || ''
    const exists = form.details.some(d => d.materialNumber === mn && d.styleNumber === sn)
    if (exists) {
      skipped.push(p.MATERIAL_NAME || mn)
      continue
    }
    // 矫正尺码组：从货品搜索结果自动带入(来自 M_PRODUCT.BOX_QTY_NEW)，用户不可改
    const gid = p.SIZE_GROUP_ID ? String(p.SIZE_GROUP_ID) : ''
    form.details.push({
      productId: p.PRODUCT_ID || '',
      materialNumber: mn,
      styleNumber: sn,
      materialName: p.MATERIAL_NAME || '',
      sizeGroup: p.SIZES || '',
      color: p.COLOR || '',
      ean13: p.EAN13 || '',
      brandName: p.BRAND_NAME || '',
      brandId: p.BRAND_ID ? String(p.BRAND_ID) : '',
      kindId: p.KIND_ID ? String(p.KIND_ID) : '',
      kindName: p.KIND_NAME || '',
      price: p.PRICE || 0,
      executionStandard: p.EXECUTION_STANDARD || '',
      origin: p.ORIGIN || '',
      manufacturer: p.MANUFACTURER || '',
      manufacturerAddress: p.MANUFACTURER_ADDRESS || '',
      contactPhone: p.CONTACT_PHONE || '',
      fabCode: p.FAB_CODE || '',
      fabElement: p.FAB_ELEMENT || '',
      acCode: p.AC_CODE || '',
      accElement: p.ACC_ELEMENT || '',
      safetyCategory: p.SAFETY_CATEGORY || '',
      sizeCode: '',
      sizeName: '',
      barcode: '',
      printQty: 0,
      localGroupId: gid,
      localGroupName: p.SIZE_GROUP_NAME || '',
      localSizeId: '',
      localSizeName: ''
    })
    if (gid) groupIdsToPreload.add(gid)
  }
  // 预加载所带入矫正组的尺码缓存，矫正尺码下拉才能直接用
  for (const gid of groupIdsToPreload) {
    ensureLocalSizeCache(gid)
  }
  if (skipped.length) {
    ElMessage.warning(`${skipped.join('、')} 已在明细中，已跳过`)
  }
  selectedProducts.value = []
}

// ─── Size Assign ────────────────────────────────────────
async function openSizeAssign(row, index) {
  sizeAssignRowIndex.value = index
  sizeAssignProductName.value = `${row.styleNumber || ''} ${row.materialName || ''}`
  sizeAssignMeta.brand = row.brandName || ''
  sizeAssignMeta.color = row.color || ''
  sizeAssignMeta.ean13 = row.ean13 || ''
  sizeAssignSearch.value = ''
  showSizeAssignDialog.value = true

  // 优先按 productId 查后端拿「尺码+条码」一一对应；没有 productId 时回退解析 sizeGroup
  let sizeRows = []
  if (row.productId) {
    try {
      const { data } = await getProductSizes(row.productId)
      sizeRows = (data || []).filter(r => r.SIZES)
    } catch { sizeRows = [] }
  }
  // 找出同货号已添加的尺码及数量
  const existingMap = {}
  form.details.forEach((d, i) => {
    if (d.materialNumber === row.materialNumber && d.styleNumber === row.styleNumber && d.sizeName) {
      existingMap[d.sizeName] = { qty: d.printQty, index: i }
    }
  })
  if (sizeRows.length) {
    sizeAssignOptions.value = sizeRows.map(r => {
      const exist = existingMap[r.SIZES]
      return {
        size: r.SIZES,
        sizeCode: r.SIZESCODE || '',
        barcode: r.BARCODE || '',
        checked: !!exist,
        qty: exist ? exist.qty : 1,
        existing: !!exist
      }
    })
  } else {
    const sizes = parseSizes(row.sizeGroup)
    sizeAssignOptions.value = sizes.map(s => {
      const exist = existingMap[s]
      return {
        size: s,
        sizeCode: '',
        barcode: '',
        checked: !!exist,
        qty: exist ? exist.qty : 1,
        existing: !!exist
      }
    })
  }
  sizeAssignAllChecked.value = sizeAssignOptions.value.every(o => o.checked)
}

function toggleSizeAssignAll(checked) {
  for (const item of sizeAssignOptions.value) {
    item.checked = checked
  }
}

async function confirmSizeAssign() {
  const checked = sizeAssignOptions.value.filter(item => item.checked)
  if (!checked.length) {
    ElMessage.warning('请至少选择一个尺码')
    return
  }
  const idx = sizeAssignRowIndex.value
  const orig = form.details[idx]
  // 确保该货品的矫正尺码组明细已加载进缓存，供下方按 sizeName↔sizeCode 自动匹配
  if (orig.localGroupId) {
    await ensureLocalSizeCache(orig.localGroupId)
  }
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
    const newRow = {
      productId: orig.productId,
      materialNumber: orig.materialNumber,
      styleNumber: orig.styleNumber,
      materialName: orig.materialName,
      sizeGroup: orig.sizeGroup,
      color: orig.color,
      ean13: orig.ean13,
      brandName: orig.brandName,
      brandId: orig.brandId,
      kindId: orig.kindId,
      kindName: orig.kindName,
      price: orig.price,
      executionStandard: orig.executionStandard,
      origin: orig.origin,
      manufacturer: orig.manufacturer,
      manufacturerAddress: orig.manufacturerAddress,
      contactPhone: orig.contactPhone,
      fabCode: orig.fabCode || '',
      fabElement: orig.fabElement || '',
      acCode: orig.acCode || '',
      accElement: orig.accElement || '',
      safetyCategory: orig.safetyCategory || '',
      sizeCode: checked[i].sizeCode || '',
      sizeName: checked[i].size,
      barcode: checked[i].barcode || '',
      printQty: checked[i].qty,
      localGroupId: orig.localGroupId || '',
      localGroupName: orig.localGroupName || '',
      localSizeId: '',
      localSizeName: ''
    }
    // 按显示尺码(sizeName)↔ 矫正尺码 sizeCode 自动匹配矫正尺码(每行独立)
    autoMatchLocalSize(newRow)
    form.details.splice(insertAt + i, 0, newRow)
  }
  showSizeAssignDialog.value = false
}

// ─── Local Size Group (本地尺码组) ───────────────────────
// 缓存: groupId -> 尺码列表（矫正尺码组只读带入，自动匹配 sizeName↔sizeCode 时读取）
const localSizeCache = reactive({})
const sizeLoadingKeys = new Set()

/** 按 groupId 预加载该组尺码列表到缓存(自动匹配矫正尺码用) */
async function ensureLocalSizeCache(groupId) {
  if (!groupId) return
  const key = groupId
  if (localSizeCache[key] || sizeLoadingKeys.has(key)) return
  sizeLoadingKeys.add(key)
  try {
    const { data } = await getSizeGroupSizes(key)
    localSizeCache[key] = data || []
  } catch {
    localSizeCache[key] = []
  } finally {
    sizeLoadingKeys.delete(key)
  }
}

// 编辑模式：遍历已有明细，预加载矫正组的尺码列表缓存（自动匹配矫正尺码用）
async function preloadLocalCaches() {
  const details = form.details
  if (!details?.length) return
  const sizeKeys = new Set()
  for (const d of details) {
    if (d.localGroupId) {
      sizeKeys.add(d.localGroupId)
    }
  }
  const promises = []
  for (const key of sizeKeys) {
    if (!localSizeCache[key] && !sizeLoadingKeys.has(key)) {
      sizeLoadingKeys.add(key)
      promises.push(
        getSizeGroupSizes(key)
          .then(({ data }) => { localSizeCache[key] = data || [] })
          .catch(() => { localSizeCache[key] = [] })
          .finally(() => sizeLoadingKeys.delete(key))
      )
    }
  }
  if (promises.length) await Promise.all(promises)
}

/**
 * 自动匹配矫正尺码：拿明细行显示的 ERP 尺码值(sizeName)，
 * 去 localGroupId 对应的矫正尺码组明细里按 sizeCode 查同名项。
 * 匹配上 → localSizeId = 该矫正尺码id, localSizeName = 该矫正尺码 sizeName(打印用)；
 * 匹配不上 → 两者置空(不显示)。
 * @param row 明细行(就地修改 localSizeId/localSizeName)
 */
function autoMatchLocalSize(row) {
  if (!row.localGroupId || !row.sizeName) {
    row.localSizeId = ''
    row.localSizeName = ''
    return
  }
  // sizeName 比较：统一去空白后精确比较，避免 "40 " vs "40" 差异
  const target = String(row.sizeName).trim()
  const match = (localSizeCache[row.localGroupId] || []).find(
    s => String(s.sizeCode || '').trim() === target
  )
  if (match) {
    row.localSizeId = match.id || ''
    row.localSizeName = match.sizeName || ''
  } else {
    row.localSizeId = ''
    row.localSizeName = ''
  }
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

// keep-alive 缓存后，从详情等页面返回时保留查询条件/分页，刷新一下数据
// （离开页面若有未保存数据会被 onBeforeRouteLeave 拦截，能回到此页说明表单已收起）
onActivated(() => {
  loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeunloadHandler)
})
</script>

<style scoped>
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
  gap: 10px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
  align-items: stretch;
}
.info-item {
  flex: 1;
  min-width: 0;
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

.info-item :deep(.el-input) {
  width: 100%;
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

/* 货品搜索表格：整行可点切换选中 + 选中行高亮 */
.search-panel :deep(.el-table__row) {
  cursor: pointer;
}
.search-panel :deep(.el-table__row.row-selected td) {
  background-color: #ecfdf5 !important;
}
.search-panel :deep(.el-table__body) tr.row-selected:hover > td {
  background-color: #d1fae5 !important;
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
/* 表格内矫正尺码下拉框：覆盖全局 200px 强制宽度，跟随列宽 */
.detail-table :deep(.el-select) {
  width: 100% !important;
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
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  max-height: 360px;
  overflow-y: auto;
}
.sa-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #f3f4f6;
  border-radius: 6px;
  transition: all 0.15s;
  cursor: pointer;
  flex-wrap: wrap;
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
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  flex: 1;
  min-width: 0;
}
.sa-item-barcode {
  font-size: 12px;
  color: #6b7280;
  font-family: 'Consolas', monospace;
  letter-spacing: 0.02em;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sa-empty {
  text-align: center;
  padding: 24px;
  color: #c0c4cc;
  font-size: 13px;
}
</style>
