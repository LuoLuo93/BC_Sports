<template>
  <div class="template-designer">
    <!-- 顶部工具栏 -->
    <div class="designer-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" size="small" @click="goBack">返回</el-button>
        <el-divider direction="vertical" />
        <span class="template-name">{{ templateName || '新建模板' }}</span>
      </div>
      <div class="header-center">
        <div class="size-setting">
          <span class="size-label">标签尺寸</span>
          <el-input-number v-model="labelWidth" size="small" :min="10" :max="200" :step="1" :precision="0" controls-position="right" style="width: 90px" />
          <span class="size-sep">×</span>
          <el-input-number v-model="labelHeight" size="small" :min="10" :max="200" :step="1" :precision="0" controls-position="right" style="width: 90px" />
          <span class="size-unit">mm</span>
        </div>
      </div>
      <div class="header-right">
        <el-button size="small" @click="previewZpl" :disabled="!hasElements">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:14px;height:14px;margin-right:4px"><path d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/></svg>
          ZPL 预览
        </el-button>
        <el-button type="primary" size="small" @click="saveTemplate">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:14px;height:14px;margin-right:4px"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
          保存
        </el-button>
      </div>
    </div>

    <!-- 三栏布局 -->
    <div class="designer-body">
      <!-- 左侧工具栏 -->
      <ElementToolbar
        :hasSelection="!!selectedElement"
        @add-element="handleAddElement"
        @delete="handleDelete"
        @duplicate="handleDuplicate"
        @align="handleAlign"
      />

      <!-- 中间画布 -->
      <DesignerCanvas
        ref="canvasRef"
        :labelWidth="labelWidth"
        :labelHeight="labelHeight"
        :dpi="dpi"
        @element-selected="handleElementSelected"
        @element-modified="handleElementModified"
        @canvas-ready="handleCanvasReady"
      />

      <!-- 右侧属性面板 -->
      <PropertyPanel
        :element="selectedElement"
        @update="handlePropertyUpdate"
      />
    </div>

    <!-- ZPL 预览弹窗 -->
    <ZplPreviewDialog
      v-model="showZplPreview"
      :zplCode="zplCode"
      :labelWidth="labelWidth"
      :labelHeight="labelHeight"
      :dpi="dpi"
    />

    <!-- 模板名称编辑弹窗 -->
    <el-dialog v-model="showSaveDialog" title="保存模板" width="420px" :close-on-click-modal="false">
      <el-form :model="saveForm" label-width="80px">
        <el-form-item label="模板名称">
          <el-input v-model="saveForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="DPI">
          <el-select v-model="saveForm.dpi">
            <el-option label="203 DPI" :value="203" />
            <el-option label="300 DPI" :value="300" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSaveDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmSave">确定保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DesignerCanvas from './components/DesignerCanvas.vue'
import ElementToolbar from './components/ElementToolbar.vue'
import PropertyPanel from './components/PropertyPanel.vue'
import ZplPreviewDialog from './components/ZplPreviewDialog.vue'
import { generateZPL } from '@/utils/zplGenerator'
import { getTemplate, createTemplate as apiCreate, updateTemplate as apiUpdate } from '@/api/sticker'

const router = useRouter()
const route = useRoute()

// ─── 模板基础属性（优先从路由参数取，避免默认值闪烁）─────────
const templateName = ref('')
const templateId = ref(route.query.id || null)
const labelWidth = ref(Number(route.query.w) || 60)
const labelHeight = ref(Number(route.query.h) || 40)
const dpi = ref(Number(route.query.dpi) || 203)

// ─── 画布与元素状态 ───────────────────
const canvasRef = ref(null)
const selectedElement = ref(null)
const fabricCanvas = ref(null)
const elementCount = ref(0)

const hasElements = computed(() => elementCount.value > 0)

// ─── ZPL 预览 ─────────────────────────
const showZplPreview = ref(false)
const zplCode = ref('')

function previewZpl() {
  const elements = canvasRef.value?.getElementsData() || []
  if (elements.length === 0) {
    ElMessage.warning('画布上没有元素')
    return
  }
  zplCode.value = generateZPL({
    labelWidth: labelWidth.value,
    labelHeight: labelHeight.value,
    dpi: dpi.value,
    elements
  })
  showZplPreview.value = true
}

// ─── 保存模板 ─────────────────────────
const showSaveDialog = ref(false)
const saving = ref(false)
const saveForm = reactive({ name: '', dpi: 203 })

function saveTemplate() {
  // 编辑已有模板（有 id 就说明是编辑，名称等 API 返回后已有值）
  if (templateId.value) {
    doSave(templateName.value || '未命名模板', dpi.value)
    return
  }
  // 新建模板：弹框填名称
  saveForm.name = templateName.value
  saveForm.dpi = dpi.value
  showSaveDialog.value = true
}

async function doSave(name, dpiVal) {
  const elements = canvasRef.value?.getElementsData() || []
  const payload = {
    templateName: name,
    labelWidth: labelWidth.value,
    labelHeight: labelHeight.value,
    dpi: dpiVal,
    templateData: JSON.stringify(elements),
    status: 1
  }

  saving.value = true
  try {
    if (templateId.value) {
      await apiUpdate(templateId.value, payload)
    } else {
      const res = await apiCreate(payload)
      templateId.value = res.data.id
    }
    templateName.value = name
    dpi.value = dpiVal
    showSaveDialog.value = false
    ElMessage.success('模板已保存')
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

async function confirmSave() {
  if (!saveForm.name.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  doSave(saveForm.name, saveForm.dpi)
}

// ─── 加载模板 ─────────────────────────
function loadTemplate(data) {
  templateId.value = data.id
  templateName.value = data.templateName
  labelWidth.value = Number(data.labelWidth)
  labelHeight.value = Number(data.labelHeight)
  dpi.value = data.dpi || 203
  // templateData 是 JSON 字符串，解析为元素数组
  let elements = []
  if (data.templateData) {
    try {
      elements = typeof data.templateData === 'string' ? JSON.parse(data.templateData) : data.templateData
    } catch { elements = [] }
  }
  if (elements.length > 0 && canvasRef.value) {
    canvasRef.value.loadElements(elements)
  }
  elementCount.value = elements.length
}

// ─── 事件处理 ─────────────────────────
function handleCanvasReady(canvas) {
  fabricCanvas.value = canvas
  // 如果有路由参数 id，从后端加载模板
  const id = route.query.id
  if (id) {
    getTemplate(id).then(res => {
      if (res.data) loadTemplate(res.data)
    }).catch(() => {
      ElMessage.error('加载模板失败')
    })
  }
}

function handleAddElement(el) {
  if (canvasRef.value) {
    canvasRef.value.addElement(el)
    elementCount.value = canvasRef.value.getElementsData().length
  }
}

function handleElementSelected(id, fabricObj) {
  if (!id || !fabricObj) {
    selectedElement.value = null
    elementCount.value = canvasRef.value?.getElementsData().length || 0
    return
  }
  selectedElement.value = canvasRef.value.getElementsData().find(e => e.id === id) || null
}

function handleElementModified(data) {
  // 更新选中元素
  if (selectedElement.value && data.id === selectedElement.value.id) {
    selectedElement.value = { ...data }
  }
}

function handlePropertyUpdate(updatedData) {
  if (!fabricCanvas.value || !selectedElement.value) return

  const canvas = fabricCanvas.value
  const obj = canvas.getActiveObject()
  if (!obj || obj._elementId !== selectedElement.value.id) return

  // 与 DesignerCanvas 保持一致
  const MM_TO_PX = 5

  // 条码和二维码需要重建元素以反映视觉变化（宽高、dataField、showText 等）
  if (obj._elementType === 'barcode' || obj._elementType === 'qrcode') {
    const elData = { ...updatedData, id: selectedElement.value.id, type: selectedElement.value.type }
    canvas.remove(obj)
    canvasRef.value.addElement(elData)
    // addElement 内部会 setActiveObject 并 emit element-selected
    elementCount.value = canvasRef.value.getElementsData().length
    return
  }

  // 更新 fabric 对象属性
  obj.set({
    left: (updatedData.x || 0) * MM_TO_PX,
    top: (updatedData.y || 0) * MM_TO_PX,
    angle: updatedData.rotation || 0
  })

  if (obj.type === 'i-text' || obj.type === 'text') {
    obj.set({
      text: updatedData.content || '',
      fontSize: (updatedData.fontSize || 3) * MM_TO_PX,
      fontWeight: updatedData.fontWeight || 'normal',
      textAlign: updatedData.textAlign || 'left'
    })
  }

  if (obj.type === 'line') {
    const newWidth = (updatedData.width || 50) * MM_TO_PX
    obj.set({
      points: [0, 0, newWidth, 0],
      width: newWidth,
      strokeWidth: updatedData.strokeWidth || 1
    })
  }

  if (obj.type === 'rect') {
    obj.set({
      width: (updatedData.width || 50) * MM_TO_PX,
      height: (updatedData.height || 30) * MM_TO_PX,
      strokeWidth: updatedData.strokeWidth || 1,
      fill: updatedData.fill ? '#1e293b' : 'transparent'
    })
  }

  // 从面板更新尺寸时重置缩放，避免拖拽缩放后面板改尺寸导致双重缩放
  if (obj.type !== 'i-text' && obj.type !== 'text') {
    obj.set({ scaleX: 1, scaleY: 1 })
  }

  obj.setCoords()
  canvas.renderAll()

  // 更新选中元素引用
  selectedElement.value = { ...updatedData, id: selectedElement.value.id, type: selectedElement.value.type }
}

function handleAlign(direction) {
  if (!fabricCanvas.value) return
  const active = fabricCanvas.value.getActiveObject()
  if (!active) return

  const MM_TO_PX = 5
  const canvasWidth = labelWidth.value * MM_TO_PX
  const canvasHeight = labelHeight.value * MM_TO_PX

  // 条码/二维码使用 _elWidth 避免 stroke 膨胀
  const objWidth = (active._elWidth || active.width / MM_TO_PX) * active.scaleX * MM_TO_PX
  const objHeight = (active._elHeight || active.height / MM_TO_PX) * active.scaleY * MM_TO_PX

  switch (direction) {
    case 'left':
      active.set('left', 0)
      break
    case 'right':
      active.set('left', canvasWidth - objWidth)
      break
    case 'centerH':
      active.set('left', (canvasWidth - objWidth) / 2)
      break
    case 'top':
      active.set('top', 0)
      break
  }
  active.setCoords()
  fabricCanvas.value.renderAll()
  // 更新属性面板的坐标值
  if (selectedElement.value && canvasRef.value) {
    const updated = canvasRef.value.getElementsData().find(e => e.id === selectedElement.value.id)
    if (updated) selectedElement.value = { ...updated }
  }
}

function handleDelete() {
  if (canvasRef.value) {
    canvasRef.value.deleteSelected()
    elementCount.value = canvasRef.value.getElementsData().length
  }
}

function handleDuplicate() {
  if (canvasRef.value) {
    canvasRef.value.duplicateSelected()
    // clone 是异步的，延迟更新计数
    setTimeout(() => {
      elementCount.value = canvasRef.value?.getElementsData().length || 0
    }, 100)
  }
}

function goBack() {
  router.push({ name: 'StickerTemplateList' })
}

// ─── 导出数据供外部使用 ───────────────
function getTemplateData() {
  return {
    id: templateId.value,
    templateName: templateName.value,
    labelWidth: labelWidth.value,
    labelHeight: labelHeight.value,
    dpi: dpi.value,
    elements: canvasRef.value?.getElementsData() || []
  }
}

defineExpose({ getTemplateData, loadTemplate })
</script>

<style scoped>
.template-designer {
  height: calc(100vh - 56px - 38px - 32px);
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  margin: -20px -24px;
}

/* ─── 顶部工具栏 ────────────────────── */
.designer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  min-height: 48px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.template-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.header-center {
  display: flex;
  align-items: center;
}

.size-setting {
  display: flex;
  align-items: center;
  gap: 6px;
}

.size-label {
  font-size: 12px;
  color: #64748b;
}

.size-sep {
  color: #94a3b8;
  font-weight: 600;
}

.size-unit {
  font-size: 12px;
  color: #64748b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ─── 三栏布局 ──────────────────────── */
.designer-body {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}
</style>
