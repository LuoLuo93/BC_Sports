<template>
  <!-- 不用 form-view（它强制 height:100% 导致底部留白），直接自然滚动 -->
  <div class="detail-page">
    <!-- 头部栏（固定顶部） -->
    <div class="detail-header">
      <el-button type="warning" size="small" @click="$router.push('/sticker/data')">
        <el-icon><ArrowLeft /></el-icon> 返回列表
      </el-button>
      <span class="detail-header-title">贴纸资料详情</span>
      <el-button type="primary" size="small" :loading="saving" @click="handleSave">保存</el-button>
    </div>

    <!-- 内容区：左右两栏 -->
    <div class="detail-body">
      <!-- 左侧：可编辑字段（主工作区） -->
      <div class="detail-main">
        <!-- 贴纸信息（单列三行） -->
        <div class="info-section">
          <div class="section-title">
            <el-icon><Stamp /></el-icon> 贴纸信息
            <el-tag size="small" type="warning" effect="plain" style="margin-left:8px">可编辑</el-tag>
          </div>
          <div class="editable-col">
            <div class="info-card editable">
              <span class="info-card-label">执行标准</span>
              <el-input v-model="row.EXECUTION_STANDARD" placeholder="请输入执行标准" size="small" />
            </div>
            <div class="info-card editable">
              <span class="info-card-label">EAN13</span>
              <el-input v-model="row.EAN13" placeholder="请输入 EAN13" size="small" @blur="row.EAN13 = (row.EAN13 || '').replace(/\s/g, '')" />
            </div>
            <div class="info-card editable">
              <span class="info-card-label">贴纸尺码组</span>
              <el-select v-model="selectedSizeGroupId" placeholder="请选择" size="small" filterable clearable style="width:100%">
                <el-option v-for="g in sizeGroupOptions" :key="g.id" :label="g.groupName" :value="g.id" />
              </el-select>
            </div>
          </div>
        </div>

        <!-- 材质信息（2列×2行） -->
        <div class="info-section fill-remaining">
          <div class="section-title">
            <el-icon><Files /></el-icon> 材质信息
            <el-tag size="small" type="warning" effect="plain" style="margin-left:8px">可编辑</el-tag>
          </div>
          <div class="info-grid-2">
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

      <!-- 右侧：基本信息（只读侧栏） -->
      <div class="detail-sidebar">
        <div class="info-section sidebar-card">
          <div class="section-title">
            <el-icon><Document /></el-icon> 基本信息
          </div>
          <div class="sidebar-list">
            <div class="sidebar-item">
              <span class="sidebar-label">货号</span>
              <span class="sidebar-value mono-value">{{ row.MATERIAL_NUMBER || '-' }}</span>
            </div>
            <div class="sidebar-item">
              <span class="sidebar-label">款号</span>
              <span class="sidebar-value mono-value">{{ row.STYLE_NUMBER || '-' }}</span>
            </div>
            <div class="sidebar-item">
              <span class="sidebar-label">货品名称</span>
              <span class="sidebar-value">{{ row.MATERIAL_NAME || '-' }}</span>
            </div>
            <div class="sidebar-item">
              <span class="sidebar-label">品牌</span>
              <span class="sidebar-value">{{ row.BRAND_NAME || '-' }}</span>
            </div>
            <div class="sidebar-item">
              <span class="sidebar-label">类别</span>
              <span class="sidebar-value">{{ row.KIND_NAME || '-' }}</span>
            </div>
            <div class="sidebar-item">
              <span class="sidebar-label">颜色</span>
              <span class="sidebar-value">
                <span v-if="row.COLOR" class="color-dot" :style="{ background: colorMap[row.COLOR] || '#909399' }"></span>
                {{ row.COLOR || '-' }}
              </span>
            </div>
            <div class="sidebar-item">
              <span class="sidebar-label">价格</span>
              <span class="sidebar-value price-value">{{ row.PRICE != null ? '¥' + Number(row.PRICE).toFixed(2) : '-' }}</span>
            </div>
            <div class="sidebar-item">
              <span class="sidebar-label">尺码组</span>
              <div class="sidebar-value">
                <template v-if="row.SIZES">
                  <el-tag v-for="s in parseSizes(row.SIZES)" :key="s" size="small" effect="plain" class="size-tag">{{ s }}</el-tag>
                </template>
                <span v-else>-</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Document, Stamp, Files } from '@element-plus/icons-vue'
import { updateStickerDataMaterial, getSizeGroupList } from '@/api/sticker'

defineOptions({ name: 'StickerDataDetail' })

const router = useRouter()
const row = ref({})
const saving = ref(false)

// 打印矫正尺码组（按品牌+类别筛选）
const sizeGroupOptions = ref([])
const selectedSizeGroupId = ref('')

async function loadSizeGroups() {
  const brandId = row.value.BRAND_ID
  const kindId = row.value.KIND_ID
  if (!brandId && !kindId) return
  try {
    const { data } = await getSizeGroupList({ brandId: brandId || undefined, kindId: kindId || undefined })
    sizeGroupOptions.value = data || []
  } catch {}
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

async function handleSave() {
  if (!row.value.MATERIAL_NUMBER) {
    ElMessage.warning('缺少货号，无法保存')
    return
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
      accElement: row.value.ACC_ELEMENT || ''
    })
    ElMessage.success('保存成功')
  } catch (e) {
    // request 拦截器已统一提示
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  const state = window.history.state
  if (state?.row) {
    row.value = state.row
    loadSizeGroups()
  } else {
    ElMessage.warning('数据加载失败，请从列表页进入')
    router.push('/sticker/data')
  }
})
</script>

<style scoped>
/* 页面整体：自然滚动，不用 form-view（它 height:100% 导致底部留白） */
.detail-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f1f5f9;
  overflow-y: auto;
}

/* 头部栏 */
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
  letter-spacing: -0.02em;
}

/* 内容区：左右两栏 */
.detail-body {
  display: flex;
  gap: 12px;
  padding: 12px 16px 20px;
  flex: 1;
  min-height: 0;
}

/* 左侧主区域：可编辑字段 */
.detail-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

/* 右侧侧栏：只读基本信息 */
.detail-sidebar {
  flex: 1;
  min-width: 220px;
  max-width: 300px;
}

/* 侧栏卡片跟主区等高 */
.sidebar-card {
  height: 100%;
}

/* 信息区块 */
.info-section {
  background: #fff;
  border-radius: 10px;
  padding: 14px 18px 16px;
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
.section-title .el-icon {
  font-size: 16px;
  color: #6366f1;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.info-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
/* 单列纵向（贴纸信息：执行标准/EAN13/尺码组垂直堆叠） */
.editable-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
/* 填满剩余高度（材质信息卡片拉伸到和侧栏等高） */
.fill-remaining {
  flex: 1;
}
.info-card {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
  transition: border-color 0.2s;
}
.info-card:hover {
  border-color: #c7d2fe;
}
.info-card.span-2 {
  grid-column: span 2;
}

/* 侧栏列表（紧凑 key-value） */
.sidebar-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.sidebar-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
}
.sidebar-item:last-child {
  border-bottom: none;
}
.sidebar-label {
  flex-shrink: 0;
  width: 60px;
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.sidebar-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  word-break: break-all;
  line-height: 1.5;
}

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
  letter-spacing: 0.02em;
}
.price-value {
  color: #dc2626;
  font-weight: 700;
  font-size: 16px;
}

/* 颜色圆点 */
.color-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
  border: 1px solid rgba(0,0,0,0.1);
}

/* 尺码标签 */
.size-tag {
  margin: 1px 3px 1px 0;
}

/* 可编辑卡片 */
.info-card.editable {
  background: #fffbeb;
  border-color: #fde68a;
}
.info-card.editable:hover {
  border-color: #f59e0b;
}
.info-card.editable :deep(.el-input__inner) {
  font-size: 14px;
}
</style>
