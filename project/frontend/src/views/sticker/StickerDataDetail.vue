<template>
  <div class="detail-page">
    <!-- 头部栏 -->
    <div class="detail-header">
      <el-button type="warning" size="small" @click="$router.push('/sticker/data')">
        <el-icon><ArrowLeft /></el-icon> 返回列表
      </el-button>
      <span class="detail-header-title">
        贴纸资料详情
        <el-tag v-if="dirty" size="small" type="warning" effect="dark" style="margin-left:8px">未保存</el-tag>
      </span>
      <el-button type="primary" size="small" :loading="saving" @click="handleSave">保存</el-button>
    </div>

    <div class="detail-content">
      <!-- 顶部：基本信息（只读） -->
      <div class="info-section">
        <div class="section-title"><el-icon><Document /></el-icon> 基本信息</div>
        <div class="info-grid">
          <div class="info-card">
            <span class="info-card-label">货号</span>
            <span class="info-card-value mono-value">{{ row.MATERIAL_NUMBER || '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">款号</span>
            <span class="info-card-value mono-value">{{ row.STYLE_NUMBER || '-' }}</span>
          </div>
          <div class="info-card span-2">
            <span class="info-card-label">货品名称</span>
            <span class="info-card-value">{{ row.MATERIAL_NAME || '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">品牌</span>
            <span class="info-card-value">{{ row.BRAND_NAME || '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">类别</span>
            <span class="info-card-value">{{ row.KIND_NAME || '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">颜色</span>
            <span class="info-card-value">
              <span v-if="row.COLOR" class="color-dot" :style="{ background: colorMap[row.COLOR] || '#909399' }"></span>
              {{ row.COLOR || '-' }}
            </span>
          </div>
          <div class="info-card">
            <span class="info-card-label">价格</span>
            <span class="info-card-value price-value">{{ row.PRICE != null ? '¥' + Number(row.PRICE).toFixed(2) : '-' }}</span>
          </div>
          <div class="info-card span-3">
            <span class="info-card-label">尺码组</span>
            <span class="info-card-value">
              <template v-if="row.SIZES">
                <el-tag v-for="s in parseSizes(row.SIZES)" :key="s" size="small" effect="plain" class="size-tag">{{ s }}</el-tag>
              </template>
              <span v-else>-</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 中间：贴纸信息 + 矫正贴纸尺码（左右并排） -->
      <div class="middle-row">
        <div class="info-section" style="flex:2;">
          <div class="section-title">
            <el-icon><Stamp /></el-icon> 贴纸信息
            <el-tag size="small" type="warning" effect="plain" style="margin-left:8px">可编辑</el-tag>
          </div>
          <div class="info-grid">
            <div class="info-card editable span-2">
              <span class="info-card-label">执行标准</span>
              <el-input v-model="row.EXECUTION_STANDARD" placeholder="请输入执行标准" size="small" />
            </div>
            <div class="info-card editable span-2" :class="ean13InputClass">
              <span class="info-card-label">
                EAN13
                <span v-if="ean13Check === 'error'" class="field-msg field-msg-error">需为 13 位纯数字</span>
                <span v-else-if="ean13Check === 'warning'" class="field-msg field-msg-warn">校验位不匹配，请核对</span>
              </span>
              <el-input
                v-model="row.EAN13"
                placeholder="请输入 EAN13（13 位数字）"
                size="small"
                maxlength="13"
                @input="onEan13Input"
                @blur="onEan13Blur"
              />
            </div>
          </div>
        </div>
        <div class="info-section" style="flex:1;">
          <div class="section-title">
            <el-icon><Files /></el-icon> 矫正贴纸尺码
            <el-tag size="small" type="warning" effect="plain" style="margin-left:8px">可编辑</el-tag>
          </div>
          <div class="info-card editable" style="margin:0;">
            <span class="info-card-label">贴纸尺码组</span>
            <el-select v-model="selectedSizeGroupId" placeholder="请选择尺码组" size="small" filterable clearable style="width:100%" @change="onSizeGroupChange">
              <el-option v-for="g in sizeGroupOptions" :key="g.id" :label="g.groupName" :value="g.id" />
            </el-select>
          </div>
          <div v-if="selectedGroupSizes.length" class="size-detail-list">
            <span class="info-card-label">组内尺码</span>
            <div class="size-tags-row">
              <el-tag v-for="s in selectedGroupSizes" :key="s.id || s.sizeName" size="small" effect="plain" class="size-tag">
                {{ s.sizeName }}<span v-if="s.sizeCode" class="size-code-suffix"> / {{ s.sizeCode }}</span>
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部：材质信息 -->
      <div class="info-section">
        <div class="section-title">
          <el-icon><Files /></el-icon> 材质信息
          <el-tag size="small" type="warning" effect="plain" style="margin-left:8px">可编辑</el-tag>
        </div>
        <div class="info-grid">
          <div class="info-card editable">
            <span class="info-card-label">面料成分1</span>
            <el-input v-model="row.FAB_CODE" placeholder="请输入面料成分1" size="small" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">面料成分2</span>
            <el-input v-model="row.FAB_ELEMENT" placeholder="请输入面料成分2" size="small" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">辅料成分1</span>
            <el-input v-model="row.AC_CODE" placeholder="请输入辅料成分1" size="small" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">辅料成分2</span>
            <el-input v-model="row.ACC_ELEMENT" placeholder="请输入辅料成分2" size="small" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Document, Stamp, Files } from '@element-plus/icons-vue'
import { updateStickerDataMaterial, getSizeGroupList, getSizeGroupSizes, getStickerDataDetail } from '@/api/sticker'

defineOptions({ name: 'StickerDataDetail' })

const router = useRouter()
const route = useRoute()
const row = ref({})
const saving = ref(false)
const sizeGroupOptions = ref([])
const selectedSizeGroupId = ref('')
const selectedGroupSizes = ref([]) // 当前选中组的尺码明细
const originalRow = ref({})         // 脏检查基线（深拷贝的初始数据）
const originalSizeGroupId = ref('') // 脏检查基线（初始尺码组ID）
const dirty = ref(false)            // 是否有未保存改动

async function loadSizeGroups() {
  const brandId = row.value.BRAND_ID
  const kindId = row.value.KIND_ID
  if (!brandId && !kindId) return
  try {
    const { data } = await getSizeGroupList({ brandId: brandId || undefined, kindId: kindId || undefined })
    sizeGroupOptions.value = data || []
    // 回显已保存的矫正组(来自 M_PRODUCT.BOX_QTY_NEW → SIZE_GROUP_ID)
    const savedId = row.value.SIZE_GROUP_ID
    if (savedId && sizeGroupOptions.value.some(g => g.id === savedId)) {
      selectedSizeGroupId.value = savedId
      loadGroupSizes(savedId)
    }
  } catch {}
}

/** 拉取某尺码组下的尺码明细展示 */
async function loadGroupSizes(groupId) {
  if (!groupId) { selectedGroupSizes.value = []; return }
  try {
    const { data } = await getSizeGroupSizes(groupId)
    selectedGroupSizes.value = data || []
  } catch {
    selectedGroupSizes.value = []
  }
}

/** 下拉切换：重新加载组内尺码 */
function onSizeGroupChange(val) {
  loadGroupSizes(val)
}

const colorMap = {
  '黑色': '#000', '白色': '#f5f5f5', '红色': '#ef4444', '蓝色': '#3b82f6',
  '绿色': '#22c55e', '黄色': '#eab308', '灰色': '#9ca3af', '粉色': '#ec4899',
  '紫色': '#a855f7', '橙色': '#f97316', '棕色': '#92400e', '米色': '#fef3c7',
}

function parseSizes(s) {
  if (!s) return []
  return s.split(/[,，;；\s]+/).filter(Boolean)
}

// ─── EAN13 校验 ─────────────────────────────────
// 两级反馈：error=格式错(红,阻止保存)；warning=校验位不符(黄,仅提醒不阻止)
const ean13Check = ref('ok') // 'ok' | 'error' | 'warning'

/** EAN-13 标准校验位算法(GS1)：偶数位×1 + 奇数位×3，合计取模 10 取补 */
function ean13ChecksumMatches(code) {
  if (!/^\d{13}$/.test(code)) return false
  let sum = 0
  for (let i = 0; i < 12; i++) {
    sum += Number(code[i]) * (i % 2 === 0 ? 1 : 3)
  }
  const checkBit = (10 - (sum % 10)) % 10
  return checkBit === Number(code[12])
}

/** 实时校验：空=ok；非13位数字=error；13位但校验位不符=warning */
function validateEan13(val) {
  const v = (val || '').trim()
  if (!v) return 'ok'
  if (!/^\d{13}$/.test(v)) return 'error'
  return ean13ChecksumMatches(v) ? 'ok' : 'warning'
}

/** 输入时：剥离空格、仅保留数字，同步校验状态 */
function onEan13Input(val) {
  row.value.EAN13 = (val || '').replace(/\D/g, '').slice(0, 13)
  ean13Check.value = validateEan13(row.value.EAN13)
}

/** 失焦：再校验一次(兜底)，错误状态弹个提示 */
function onEan13Blur() {
  const v = (row.value.EAN13 || '').trim()
  if (v && !/^\d{13}$/.test(v)) {
    ElMessage.warning('EAN13 需为 13 位纯数字')
  }
}

/** 输入框动态 class：错误红框、警告黄框 */
const ean13InputClass = computed(() => ({
  'field-error': ean13Check.value === 'error',
  'field-warning': ean13Check.value === 'warning'
}))

async function handleSave() {
  if (!row.value.MATERIAL_NUMBER) {
    ElMessage.warning('缺少货号，无法保存')
    return
  }
  // EAN13 格式硬校验：非空时必须 13 位纯数字才允许保存
  ean13Check.value = validateEan13(row.value.EAN13)
  if (ean13Check.value === 'error') {
    ElMessage.warning('EAN13 格式错误：需为 13 位纯数字')
    return
  }
  if (ean13Check.value === 'warning') {
    try {
      await ElMessageBox.confirm(
        'EAN13 校验位不匹配，确认仍要保存吗？',
        '校验位提醒',
        { confirmButtonText: '仍保存', cancelButtonText: '返回修改', type: 'warning' }
      )
    } catch {
      return // 用户选择返回修改
    }
  }
  saving.value = true
  try {
    await updateStickerDataMaterial({
      materialNumber: row.value.MATERIAL_NUMBER,
      executionStandard: row.value.EXECUTION_STANDARD || '',
      ean13: row.value.EAN13 || '',
      fabCode: row.value.FAB_CODE || '',
      fabElement: row.value.FAB_ELEMENT || '',
      acCode: row.value.AC_CODE || '',
      accElement: row.value.ACC_ELEMENT || '',
      sizeGroupId: selectedSizeGroupId.value || ''
    })
    ElMessage.success('保存成功')
    // 保存成功：重置脏检查基线，标记为干净
    syncOriginal()
  } catch (e) {
    // request 拦截器已统一提示
  } finally {
    saving.value = false
  }
}

/** 把当前数据快照为脏检查基线，并标记为干净 */
function syncOriginal() {
  originalRow.value = JSON.parse(JSON.stringify(row.value))
  originalSizeGroupId.value = selectedSizeGroupId.value
  dirty.value = false
}

/** 计算当前是否有未保存改动（监听 row 各可编辑字段 + 尺码组ID） */
function computeDirty() {
  const o = originalRow.value
  const r = row.value
  const fields = ['EXECUTION_STANDARD', 'EAN13', 'FAB_CODE', 'FAB_ELEMENT', 'AC_CODE', 'ACC_ELEMENT']
  for (const f of fields) {
    if ((o[f] || '') !== (r[f] || '')) return true
  }
  if ((originalSizeGroupId.value || '') !== (selectedSizeGroupId.value || '')) return true
  return false
}

// 监听可编辑字段变化，自动同步 dirty 状态（供保存按钮/离开判断用）
watch(
  () => [
    row.value.EXECUTION_STANDARD, row.value.EAN13,
    row.value.FAB_CODE, row.value.FAB_ELEMENT, row.value.AC_CODE, row.value.ACC_ELEMENT,
    selectedSizeGroupId.value
  ],
  () => { dirty.value = computeDirty() }
)

onMounted(async () => {
  const materialNumber = route.params.materialNumber
  // history.state 带了整行数据：先用它秒开渲染(加速)，再调接口覆盖成最新值
  const stateRow = window.history.state?.row
  if (stateRow) {
    row.value = stateRow
    ean13Check.value = validateEan13(row.value.EAN13)
    loadSizeGroups()
  }
  // 始终按货号拉最新数据（刷新/分享链接也能正常打开）
  if (!materialNumber) {
    ElMessage.warning('缺少货号，请从列表页进入')
    router.push('/sticker/data')
    return
  }
  try {
    const { data } = await getStickerDataDetail(materialNumber)
    if (data) {
      row.value = data
      ean13Check.value = validateEan13(row.value.EAN13)
      await loadSizeGroups() // 数据到位后加载尺码组(含回显)
      syncOriginal()         // 初始化脏检查基线
    } else {
      ElMessage.warning('货号数据不存在')
      router.push('/sticker/data')
    }
  } catch {
    // 接口失败时若 history.state 兜底了数据，仍可编辑；否则回列表
    if (!stateRow) {
      router.push('/sticker/data')
    }
  }
})

// 离开拦截：有未保存改动时弹确认
onBeforeRouteLeave(async (_to, _from) => {
  if (!computeDirty()) return true
  try {
    await ElMessageBox.confirm('有未保存的修改，确定离开吗？', '离开提醒', {
      confirmButtonText: '离开', cancelButtonText: '继续编辑', type: 'warning'
    })
    return true
  } catch {
    return false
  }
})
</script>

<style scoped>
.detail-page {
  background: #f1f5f9;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}
.detail-content {
  padding: 12px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
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

/* 中间行：贴纸信息 + 矫正尺码 并排 */
.middle-row {
  display: flex;
  gap: 12px;
}

/* 信息区块 */
.info-section {
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px 18px;
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

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.info-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 14px;
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
  text-transform: uppercase;
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
  font-size: 15px;
}
.price-value { color: #dc2626; font-weight: 700; font-size: 16px; }
.color-dot {
  display: inline-block;
  width: 12px; height: 12px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
  border: 1px solid rgba(0,0,0,0.1);
}
.size-tag { margin: 1px 3px 1px 0; }

/* 矫正组尺码明细展示 */
.size-detail-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.size-tags-row { display: flex; flex-wrap: wrap; gap: 4px; }
.size-code-suffix { color: #94a3b8; font-size: 11px; }

/* 可编辑卡片 */
.info-card.editable {
  background: #fffbeb;
  border-color: #fde68a;
}
.info-card.editable:hover { border-color: #f59e0b; }
.info-card.editable :deep(.el-input__inner) { font-size: 14px; }

/* EAN13 校验状态 */
.info-card.field-error {
  background: #fef2f2;
  border-color: #fca5a5;
}
.info-card.field-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #ef4444 inset !important;
}
.info-card.field-warning {
  background: #fffbeb;
  border-color: #f59e0b;
}
.info-card.field-warning :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #f59e0b inset !important;
}
/* 标签行内提示文字 */
.info-card-label { display: flex; align-items: center; gap: 8px; }
.field-msg { font-size: 11px; font-weight: 500; text-transform: none; letter-spacing: 0; }
.field-msg-error { color: #ef4444; }
.field-msg-warn { color: #d97706; }
</style>
