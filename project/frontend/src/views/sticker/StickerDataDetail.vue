<template>
  <div class="page-container">
    <div class="form-view form-view--scroll">
      <!-- 头部栏 -->
      <div class="form-header">
        <el-button type="warning" size="small" @click="$router.push('/sticker/data')">
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
        <span class="form-header-title">贴纸资料详情</span>
        <el-button type="primary" size="small" :loading="saving" @click="handleSave">保存</el-button>
      </div>

      <!-- 基本信息卡片 -->
      <div class="info-section">
        <div class="section-title">
          <el-icon><Document /></el-icon> 基本信息
        </div>
        <div class="info-grid">
          <div class="info-card">
            <span class="info-card-label">货号</span>
            <span class="info-card-value key-value">{{ row.MATERIAL_NUMBER || '-' }}</span>
          </div>
          <div class="info-card">
            <span class="info-card-label">款号</span>
            <span class="info-card-value key-value">{{ row.STYLE_NUMBER || '-' }}</span>
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
          <div class="info-card span-4">
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

      <!-- 贴纸信息（可编辑） -->
      <div class="info-section">
        <div class="section-title">
          <el-icon><Stamp /></el-icon> 贴纸信息
          <el-tag size="small" type="warning" effect="plain" style="margin-left:8px">可编辑</el-tag>
        </div>
        <div class="info-grid">
          <div class="info-card editable">
            <span class="info-card-label">执行标准</span>
            <el-input v-model="row.EXECUTION_STANDARD" placeholder="请输入执行标准" size="small" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">EAN13</span>
            <el-input v-model="row.EAN13" placeholder="请输入 EAN13" size="small" />
          </div>
          <div class="info-card editable">
            <span class="info-card-label">打印矫正尺码组</span>
            <el-select v-model="selectedSizeGroupId" placeholder="请选择" size="small" filterable clearable style="width:100%">
              <el-option v-for="g in sizeGroupOptions" :key="g.id" :label="g.groupName" :value="g.id" />
            </el-select>
          </div>
        </div>
      </div>

      <!-- 材质信息（可编辑） -->
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
    // 加载尺码组选项（按货品的品牌+类别筛选）
    loadSizeGroups()
  } else {
    ElMessage.warning('数据加载失败，请从列表页进入')
    router.push('/sticker/data')
  }
})
</script>

<style scoped>
.form-header {
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
.form-header-title {
  font-size: 17px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.02em;
}

/* 信息区块 */
.info-section {
  margin: 12px 16px 0;
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.info-section:last-child {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #1e40af;
  margin-bottom: 14px;
  padding-bottom: 10px;
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
  gap: 12px;
}
.info-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
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
.info-card.span-4 {
  grid-column: span 4;
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
.key-value {
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  font-weight: 700;
  color: #0f172a;
  font-size: 15px;
  letter-spacing: 0.02em;
}
.mono-value {
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  letter-spacing: 0.04em;
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
  margin: 2px 4px 2px 0;
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
  color: #1e293b;
}
</style>
