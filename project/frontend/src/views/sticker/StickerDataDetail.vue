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
      <el-button v-if="hasPermission('sticker:data:edit')" type="primary" size="small" :loading="saving" @click="handleSave">保存</el-button>
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
        <div class="info-section" style="flex:1;">
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
                <span v-if="ean13Check === 'error'" class="field-msg field-msg-error">需为 12 位纯数字</span>
                <span v-else-if="ean13Check === 'warning'" class="field-msg field-msg-warn">校验位不匹配，请核对</span>
              </span>
              <el-input
                v-model="row.EAN13"
                placeholder="请输入 EAN13（12 位数字）"
                size="small"
                maxlength="12"
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
          <div class="size-group-row">
            <div class="info-card editable" style="margin:0;flex:0 0 auto;min-width:160px;">
              <span class="info-card-label">贴纸尺码组</span>
              <el-button
                :type="selectedSizeGroupId ? 'primary' : 'default'"
                size="small"
                plain
                style="width:100%"
                @click="openSizeGroupDialog"
              >
                <el-icon style="margin-right:4px"><Search /></el-icon>
                {{ selectedSizeGroupId ? selectedGroupName : '点击选择尺码组' }}
              </el-button>
            </div>
            <div v-if="selectedGroupSizes.length" class="info-card" style="margin:0;flex:1;">
              <span class="info-card-label">组内尺码</span>
              <div class="size-tags-row">
                <el-tag v-for="s in selectedGroupSizes" :key="s.id || s.sizeName" size="small" effect="plain" class="size-tag">
                  <span class="size-tag-code">{{ s.sizeCode }}</span><span class="size-tag-sep" v-if="s.sizeCode && s.sizeName">:</span>{{ s.sizeName }}
                </el-tag>
              </div>
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

    <!-- 矫正尺码组选择模态框：左组列表 / 右组内尺码明细 -->
    <el-dialog
      v-model="sizeGroupDialogVisible"
      title="选择矫正尺码组"
      width="860px"
      top="6vh"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="sg-picker">
        <!-- 左侧：组列表 + 搜索 -->
        <div class="sg-picker-left">
          <el-input
            v-model="sgKeyword"
            placeholder="搜索组编码/组名称"
            size="small"
            clearable
            :prefix-icon="Search"
            style="margin-bottom:8px"
          />
          <div class="sg-list" v-loading="sgLoading">
            <div
              v-for="g in filteredSgOptions"
              :key="g.id"
              class="sg-item"
              :class="{ 'is-active': tempSizeGroupId === g.id }"
              @click="onPickGroup(g)"
            >
              <div class="sg-item-name">{{ g.groupName }}</div>
              <div class="sg-item-code">{{ g.groupCode }}</div>
            </div>
            <el-empty v-if="!sgLoading && !filteredSgOptions.length" description="无匹配尺码组" :image-size="60" />
          </div>
        </div>
        <!-- 右侧：组内尺码明细（尺码编码 / 尺码名称 两列） -->
        <div class="sg-picker-right">
          <div class="sg-detail-title">
            组内尺码明细
            <span v-if="tempGroupName" class="sg-detail-sub">— {{ tempGroupName }}</span>
            <span v-if="tempGroupSizes.length" class="sg-detail-count">共 {{ tempGroupSizes.length }} 条</span>
          </div>
          <div class="sg-detail-body" v-loading="tempSizesLoading">
            <template v-if="tempGroupSizes.length">
              <div class="sg-size-header">
                <span>尺码编码</span>
                <span>尺码名称</span>
              </div>
              <div class="sg-size-rows">
                <div v-for="s in tempGroupSizes" :key="s.id || s.sizeName" class="sg-size-row">
                  <span class="sg-size-code">{{ s.sizeCode || '-' }}</span>
                  <span class="sg-size-name">{{ s.sizeName || '-' }}</span>
                </div>
              </div>
            </template>
            <el-empty v-else-if="!tempSizesLoading" description="请先在左侧选择尺码组" :image-size="60" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button v-if="tempSizeGroupId" type="danger" plain @click="onClearSizeGroup">清除选择</el-button>
        <el-button @click="sizeGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!tempSizeGroupId" @click="onConfirmSizeGroup">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Document, Stamp, Files, Search } from '@element-plus/icons-vue'
import { updateStickerDataMaterial, getSizeGroupList, getSizeGroupSizes, getStickerDataDetail } from '@/api/sticker'
import { usePermission } from '@/composables/usePermission'

defineOptions({ name: 'StickerDataDetail' })

const { hasPermission } = usePermission()

const router = useRouter()
const route = useRoute()
const row = ref({})
const saving = ref(false)
const sizeGroupOptions = ref([])
const selectedSizeGroupId = ref('')
const selectedGroupSizes = ref([]) // 当前选中组的尺码明细
const originalRow = ref({})         // 脏检查基线（深拷贝的初始数据）

// 当前选中组的名称（详情页按钮上回显用）
const selectedGroupName = computed(() => {
  const g = sizeGroupOptions.value.find(x => x.id === selectedSizeGroupId.value)
  return g ? g.groupName : ''
})

// ── 矫正尺码组选择模态框 ──
const sizeGroupDialogVisible = ref(false)
const sgKeyword = ref('')           // 模态框左侧搜索关键字
const sgLoading = ref(false)        // 组列表加载中
const tempSizeGroupId = ref('')     // 模态框内临时选中的组ID（确认前不回填）
const tempGroupName = ref('')       // 模态框内临时选中组名
const tempGroupSizes = ref([])      // 模态框内临时选中组的尺码明细
const tempSizesLoading = ref(false) // 明细加载中
const tempSizesCache = {}           // groupId → 尺码明细 缓存，避免重复请求

/** 打开模态框：重新拉取当前品牌+类别下的启用组，预选当前组 */
async function openSizeGroupDialog() {
  sizeGroupDialogVisible.value = true
  sgKeyword.value = ''
  tempSizeGroupId.value = selectedSizeGroupId.value
  tempGroupName.value = selectedGroupName.value
  tempGroupSizes.value = selectedGroupSizes.value.slice()
  sgLoading.value = true
  try {
    const brandId = row.value.BRAND_ID
    const kindId = row.value.KIND_ID
    const { data } = await getSizeGroupList({ brandId: brandId || undefined, kindId: kindId || undefined })
    sizeGroupOptions.value = data || []
  } catch {
    sizeGroupOptions.value = []
  } finally {
    sgLoading.value = false
  }
}

/** 模态框内左侧点中某个组：右侧带出尺码明细 */
async function onPickGroup(g) {
  tempSizeGroupId.value = g.id
  tempGroupName.value = g.groupName
  if (tempSizesCache[g.id]) {
    tempGroupSizes.value = tempSizesCache[g.id]
    return
  }
  tempSizesLoading.value = true
  try {
    const { data } = await getSizeGroupSizes(g.id)
    tempSizesCache[g.id] = data || []
    tempGroupSizes.value = tempSizesCache[g.id]
  } catch {
    tempGroupSizes.value = []
  } finally {
    tempSizesLoading.value = false
  }
}

/** 确认：把模态框内的临时选择回填到详情页 */
function onConfirmSizeGroup() {
  selectedSizeGroupId.value = tempSizeGroupId.value
  selectedGroupSizes.value = tempGroupSizes.value.slice()
  sizeGroupDialogVisible.value = false
}

/** 清除选择：解绑矫正尺码组（保存时写空） */
function onClearSizeGroup() {
  tempSizeGroupId.value = ''
  tempGroupName.value = ''
  tempGroupSizes.value = []
  sizeGroupDialogVisible.value = false
  selectedSizeGroupId.value = ''
  selectedGroupSizes.value = []
}

/** 模态框左侧列表按关键字过滤（组编码/组名称） */
const filteredSgOptions = computed(() => {
  const kw = sgKeyword.value.trim().toLowerCase()
  if (!kw) return sizeGroupOptions.value
  return sizeGroupOptions.value.filter(g =>
    (g.groupName || '').toLowerCase().includes(kw) ||
    (g.groupCode || '').toLowerCase().includes(kw)
  )
})
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

/** EAN-13 校验位算法(GS1)：前11位加权求和，第12位为校验位 */
function ean13ChecksumMatches(code) {
  if (!/^\d{12}$/.test(code)) return false
  let sum = 0
  for (let i = 0; i < 11; i++) {
    sum += Number(code[i]) * (i % 2 === 0 ? 1 : 3)
  }
  const checkBit = (10 - (sum % 10)) % 10
  return checkBit === Number(code[11])
}

/** 实时校验：空=ok；非12位数字=error；12位但校验位不符=warning */
function validateEan13(val) {
  const v = (val || '').trim()
  if (!v) return 'ok'
  if (!/^\d{12}$/.test(v)) return 'error'
  return ean13ChecksumMatches(v) ? 'ok' : 'warning'
}

/** 输入时：剥离空格、仅保留数字，同步校验状态 */
function onEan13Input(val) {
  row.value.EAN13 = (val || '').replace(/\D/g, '').slice(0, 12)
  ean13Check.value = validateEan13(row.value.EAN13)
}

/** 失焦：再校验一次(兜底)，错误状态弹个提示 */
function onEan13Blur() {
  const v = (row.value.EAN13 || '').trim()
  if (v && !/^\d{12}$/.test(v)) {
    ElMessage.warning('EAN13 需为 12 位纯数字')
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
  // EAN13 格式硬校验：非空时必须 12 位纯数字才允许保存
  ean13Check.value = validateEan13(row.value.EAN13)
  if (ean13Check.value === 'error') {
    ElMessage.warning('EAN13 格式错误：需为 12 位纯数字')
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
.size-tag-code { font-family: 'Cascadia Code', monospace; color: #0f766e; font-weight: 600; }
.size-tag-sep { margin: 0 1px; color: #cbd5e1; }

/* 矫正组：下拉框 + 组内尺码 左右并排 */
.size-group-row {
  display: flex;
  gap: 10px;
  align-items: stretch;
}
.size-tags-row { display: flex; flex-wrap: wrap; gap: 4px; align-content: flex-start; }

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

/* 矫正尺码组选择模态框：左右分栏 */
.sg-picker {
  display: flex;
  gap: 14px;
  height: 460px;
}
.sg-picker-left {
  flex: 0 0 300px;
  display: flex;
  flex-direction: column;
}
.sg-picker-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  padding: 12px 14px;
}
/* 左侧组列表（可滚动） */
.sg-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
}
.sg-item {
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: all 0.15s;
  border-left: 3px solid transparent;
}
.sg-item:last-child { border-bottom: none; }
.sg-item:hover { background: #eff6ff; }
.sg-item.is-active {
  background: linear-gradient(90deg, #ecfdf5 0%, #f0fdf4 100%);
  border-left-color: #10b981;
}
.sg-item-name { font-size: 13px; font-weight: 600; color: #1e293b; }
.sg-item-code { font-size: 11px; color: #94a3b8; margin-top: 2px; font-family: 'Cascadia Code', monospace; }
/* 右侧明细标题 */
.sg-detail-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e40af;
  padding-bottom: 10px;
  border-bottom: 2px solid #e0e7ff;
  margin-bottom: 12px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.sg-detail-sub { font-size: 12px; font-weight: 500; color: #64748b; }
.sg-detail-count {
  margin-left: auto;
  font-size: 11px;
  font-weight: 600;
  color: #6366f1;
  background: #eef2ff;
  padding: 1px 8px;
  border-radius: 10px;
}
.sg-detail-body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
/* 两列明细表头 */
.sg-size-header {
  display: flex;
  gap: 10px;
  padding: 7px 12px;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  background: #e2e8f0;
  border-radius: 6px 6px 0 0;
}
.sg-size-header span { flex: 1; }
/* 明细行 */
.sg-size-rows {
  border: 1px solid #e2e8f0;
  border-top: none;
  border-radius: 0 0 6px 6px;
  overflow: hidden;
  background: #fff;
}
.sg-size-row {
  display: flex;
  gap: 10px;
  padding: 9px 12px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.12s;
}
.sg-size-row:last-child { border-bottom: none; }
.sg-size-row:hover { background: #f8fafc; }
.sg-size-row span { flex: 1; font-size: 13px; line-height: 1.4; }
.sg-size-code {
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-weight: 600;
  color: #0f766e;
}
.sg-size-name { color: #1e293b; font-weight: 500; }

</style>
