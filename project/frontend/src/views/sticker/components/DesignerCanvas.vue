<template>
  <div class="designer-canvas-wrapper">
    <!-- 画布缩放控制 -->
    <div class="canvas-zoom-bar">
      <el-button-group>
        <el-button size="small" :icon="ZoomOut" @click="zoomOut" title="缩小" />
        <el-button size="small" class="zoom-level" @click="handleZoomInputClick">
            <input
              ref="zoomInputRef"
              class="zoom-input"
              :value="Math.round(zoomLevel * 100)"
              @change="handleZoomInputChange"
              @blur="handleZoomInputBlur"
              @keyup.enter="zoomInputRef?.blur()"
            />
            <span>%</span>
          </el-button>
        <el-button size="small" :icon="ZoomIn" @click="zoomIn" title="放大" />
        <el-button size="small" @click="zoomFit" title="适应画布">适应</el-button>
      </el-button-group>
      <div class="canvas-size-info">{{ labelWidth }}mm × {{ labelHeight }}mm ({{ dpi }} DPI)</div>
    </div>

    <!-- 画布容器 -->
    <div class="canvas-container" ref="containerRef">
      <div class="canvas-scroll-area">
        <canvas ref="canvasRef"></canvas>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import { Canvas, Line, Rect, Text, IText, Group } from 'fabric'

const props = defineProps({
  labelWidth: { type: Number, default: 60 },
  labelHeight: { type: Number, default: 40 },
  dpi: { type: Number, default: 203 },
  elements: { type: Array, default: () => [] },
  selectedId: { type: String, default: null }
})

const emit = defineEmits(['element-selected', 'element-modified', 'canvas-ready'])

const canvasRef = ref(null)
const containerRef = ref(null)
const zoomInputRef = ref(null)
const zoomLevel = ref(1)

let fabricCanvas = null
// 设计工具基准倍率：5px/mm，让画布在 1:1 时就有合理的视觉大小
const MM_TO_PX = 5

function getCanvasSize() {
  return {
    width: Math.round(props.labelWidth * MM_TO_PX),
    height: Math.round(props.labelHeight * MM_TO_PX)
  }
}

// ─── 初始化画布 ──────────────────────
function initCanvas() {
  const { width, height } = getCanvasSize()
  fabricCanvas = new Canvas(canvasRef.value, {
    width,
    height,
    backgroundColor: '#ffffff',
    selection: true,
    preserveObjectStacking: true,
    stopContextMenu: true,
    fireRightClick: true
  })

  drawGrid()

  fabricCanvas.on('selection:created', handleSelection)
  fabricCanvas.on('selection:updated', handleSelection)
  fabricCanvas.on('selection:cleared', () => emit('element-selected', null))
  fabricCanvas.on('object:modified', handleModified)
  fabricCanvas.on('object:scaling', handleModified)
  fabricCanvas.on('object:rotating', handleModified)

  nextTick(() => applyZoom())
  emit('canvas-ready', fabricCanvas)
}

// ─── 网格 ────────────────────────────
function drawGrid() {
  if (!fabricCanvas) return
  const { width, height } = getCanvasSize()
  const gridSize = MM_TO_PX * 5

  for (let x = gridSize; x < width; x += gridSize) {
    const line = new Line([x, 0, x, height], {
      stroke: '#e5e7eb', strokeWidth: 0.5,
      selectable: false, evented: false
    })
    line._isGrid = true
    fabricCanvas.add(line)
  }
  for (let y = gridSize; y < height; y += gridSize) {
    const line = new Line([0, y, width, y], {
      stroke: '#e5e7eb', strokeWidth: 0.5,
      selectable: false, evented: false
    })
    line._isGrid = true
    fabricCanvas.add(line)
  }
}

// ─── 事件处理 ────────────────────────
function handleSelection(e) {
  const obj = e.selected?.[0]
  if (obj) {
    emit('element-selected', obj._elementId || null, obj)
  }
}

function handleModified(e) {
  const obj = e.target
  if (!obj) return
  emit('element-modified', getElementData(obj))
}

// ─── 元素数据提取 ────────────────────
function getElementData(obj) {
  const base = {
    id: obj._elementId,
    x: parseFloat((obj.left / MM_TO_PX).toFixed(1)),
    y: parseFloat((obj.top / MM_TO_PX).toFixed(1)),
    rotation: obj.angle || 0
  }

  if (obj.type === 'i-text' || obj.type === 'text') {
    return {
      ...base,
      type: 'text',
      width: parseFloat((obj.width * obj.scaleX / MM_TO_PX).toFixed(1)),
      height: parseFloat((obj.height * obj.scaleY / MM_TO_PX).toFixed(1)),
      content: obj.text,
      fontSize: parseFloat((obj.fontSize / MM_TO_PX).toFixed(1)),
      fontWeight: obj.fontWeight || 'normal',
      textAlign: obj.textAlign || 'left',
      dataField: obj._dataField || ''
    }
  }
  if (obj._elementType === 'barcode') {
    // _elWidth 已是 mm 值，直接乘 scaleX 即可，避免 Group 宽度因子元素 stroke 膨胀
    return {
      ...base,
      type: 'barcode',
      width: parseFloat(((obj._elWidth || 40) * obj.scaleX).toFixed(1)),
      height: parseFloat(((obj._elHeight || 10) * obj.scaleY).toFixed(1)),
      barcodeType: obj._barcodeType || 'EAN13',
      dataField: obj._dataField || '',
      showText: obj._showText !== false
    }
  }
  if (obj._elementType === 'qrcode') {
    return {
      ...base,
      type: 'qrcode',
      width: parseFloat(((obj._elWidth || 10) * Math.min(obj.scaleX, obj.scaleY)).toFixed(1)),
      height: parseFloat(((obj._elHeight || 10) * Math.min(obj.scaleX, obj.scaleY)).toFixed(1)),
      dataField: obj._dataField || '',
      moduleSize: obj._moduleSize || 4,
      errorCorrectionLevel: obj._ecLevel || 'M'
    }
  }
  if (obj.type === 'line') {
    return {
      ...base,
      type: 'line',
      width: parseFloat((obj.width * obj.scaleX / MM_TO_PX).toFixed(1)),
      height: 0,
      strokeWidth: obj.strokeWidth || 1
    }
  }
  if (obj.type === 'rect') {
    return {
      ...base,
      type: 'rect',
      width: parseFloat((obj.width * obj.scaleX / MM_TO_PX).toFixed(1)),
      height: parseFloat((obj.height * obj.scaleY / MM_TO_PX).toFixed(1)),
      strokeWidth: obj.strokeWidth || 1,
      fill: !!obj.fill && obj.fill !== 'transparent' && obj.fill !== ''
    }
  }
  return base
}

// ─── 暴露方法 ────────────────────────
function getCanvas() {
  return fabricCanvas
}

function getElementsData() {
  if (!fabricCanvas) return []
  return fabricCanvas.getObjects()
    .filter(o => !o._isGrid && o._elementId)
    .map(o => getElementData(o))
}

function loadElements(elements) {
  if (!fabricCanvas) return
  const grids = fabricCanvas.getObjects().filter(o => o._isGrid)
  fabricCanvas.clear()
  grids.forEach(g => fabricCanvas.add(g))

  for (const el of elements) {
    addElement(el)
  }
  fabricCanvas.renderAll()
}

function addElement(el) {
  if (!fabricCanvas) return null
  const id = el.id || `el_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
  let fabricObj = null

  switch (el.type) {
    case 'text': fabricObj = createTextElement(el, id); break
    case 'barcode': fabricObj = createBarcodeElement(el, id); break
    case 'qrcode': fabricObj = createQrcodeElement(el, id); break
    case 'line': fabricObj = createLineElement(el, id); break
    case 'rect': fabricObj = createRectElement(el, id); break
  }

  if (fabricObj) {
    fabricCanvas.add(fabricObj)
    fabricCanvas.setActiveObject(fabricObj)
    fabricCanvas.renderAll()
    // setActiveObject 不会自动触发 selection:created，手动通知父组件
    emit('element-selected', fabricObj._elementId || null, fabricObj)
  }
  return fabricObj
}

// ─── 创建各类元素 ────────────────────
function createTextElement(el, id) {
  const obj = new IText(el.content || '文本', {
    left: (el.x || 0) * MM_TO_PX,
    top: (el.y || 0) * MM_TO_PX,
    fontSize: (el.fontSize || 3) * MM_TO_PX,
    fontWeight: el.fontWeight || 'normal',
    textAlign: el.textAlign || 'left',
    fontFamily: 'Arial, sans-serif',
    fill: '#000000',
    angle: el.rotation || 0,
    originX: 'left',
    originY: 'top'
  })
  obj._elementId = id
  obj._elementType = 'text'
  obj._dataField = el.dataField || ''
  return obj
}

function createBarcodeElement(el, id) {
  const w = (el.width || 40) * MM_TO_PX
  const h = (el.height || 10) * MM_TO_PX
  const showText = el.showText !== false
  const textHeight = showText ? 14 : 0
  const textPadding = showText ? 4 : 0

  const children = [
    new Rect({
      width: w, height: h,
      fill: '#f8fafc',
      stroke: '#cbd5e1',
      strokeWidth: 1,
      rx: 2, ry: 2
    })
  ]

  // 模拟条码线条
  const barCount = 40
  const barWidth = w / barCount
  for (let i = 0; i < barCount; i++) {
    if (i % 2 === 0 || Math.random() > 0.4) {
      children.push(new Rect({
        left: i * barWidth,
        top: 2,
        width: Math.max(barWidth * (0.5 + Math.random() * 0.5), 0.5),
        height: h - 4 - textHeight - textPadding,
        fill: '#1e293b',
        opacity: 0.8
      }))
    }
  }

  // 条码文本（仅在 showText 为 true 时显示）
  if (showText) {
    children.push(new Text(el.dataField ? `{{${el.dataField}}}` : '1234567890128', {
      fontSize: 8,
      fill: '#64748b',
      top: h - textHeight,
      left: w / 2,
      originX: 'center',
      fontFamily: 'monospace'
    }))
  }

  const group = new Group(children, {
    left: (el.x || 0) * MM_TO_PX,
    top: (el.y || 0) * MM_TO_PX,
    angle: el.rotation || 0,
    originX: 'left',
    originY: 'top'
  })

  group._elementId = id
  group._elementType = 'barcode'
  group._barcodeType = el.barcodeType || 'EAN13'
  group._dataField = el.dataField || ''
  group._showText = showText
  // 存储原始逻辑尺寸，避免 Group 宽度因子元素 stroke 膨胀
  group._elWidth = el.width || 40
  group._elHeight = el.height || 10
  return group
}

function createQrcodeElement(el, id) {
  const size = (el.width || 10) * MM_TO_PX
  const cells = []
  const gridCount = 8
  const cellSize = size / gridCount

  for (let row = 0; row < gridCount; row++) {
    for (let col = 0; col < gridCount; col++) {
      const isCorner = (row < 2 && col < 2) || (row < 2 && col >= gridCount - 2) || (row >= gridCount - 2 && col < 2)
      if (isCorner || Math.random() > 0.45) {
        cells.push(new Rect({
          left: col * cellSize,
          top: row * cellSize,
          width: cellSize,
          height: cellSize,
          fill: '#1e293b'
        }))
      }
    }
  }

  const group = new Group(cells, {
    left: (el.x || 0) * MM_TO_PX,
    top: (el.y || 0) * MM_TO_PX,
    angle: el.rotation || 0,
    originX: 'left',
    originY: 'top'
  })

  group._elementId = id
  group._elementType = 'qrcode'
  group._dataField = el.dataField || ''
  group._moduleSize = el.moduleSize || 4
  group._ecLevel = el.errorCorrectionLevel || 'M'
  group._elWidth = el.width || 10
  group._elHeight = el.height || 10
  return group
}

function createLineElement(el, id) {
  const w = (el.width || 50) * MM_TO_PX
  const obj = new Line([0, 0, w, 0], {
    left: (el.x || 0) * MM_TO_PX,
    top: (el.y || 0) * MM_TO_PX,
    stroke: '#1e293b',
    strokeWidth: el.strokeWidth || 1,
    angle: el.rotation || 0,
    originX: 'left',
    originY: 'top'
  })
  obj._elementId = id
  obj._elementType = 'line'
  return obj
}

function createRectElement(el, id) {
  const obj = new Rect({
    left: (el.x || 0) * MM_TO_PX,
    top: (el.y || 0) * MM_TO_PX,
    width: (el.width || 50) * MM_TO_PX,
    height: (el.height || 30) * MM_TO_PX,
    fill: el.fill ? '#1e293b' : 'transparent',
    stroke: '#1e293b',
    strokeWidth: el.strokeWidth || 1,
    angle: el.rotation || 0,
    originX: 'left',
    originY: 'top'
  })
  obj._elementId = id
  obj._elementType = 'rect'
  return obj
}

// ─── 删除 / 复制 ────────────────────
function deleteSelected() {
  if (!fabricCanvas) return
  const active = fabricCanvas.getActiveObjects()
  active.forEach(obj => {
    if (!obj._isGrid) fabricCanvas.remove(obj)
  })
  fabricCanvas.discardActiveObject()
  fabricCanvas.renderAll()
  // 通知父组件选中已清空，触发 elementCount 和 selectedElement 更新
  emit('element-selected', null)
}

function duplicateSelected() {
  if (!fabricCanvas) return
  const active = fabricCanvas.getActiveObject()
  if (!active || active._isGrid) return
  active.clone().then(cloned => {
    cloned.set({ left: cloned.left + 10, top: cloned.top + 10 })
    cloned._elementId = `el_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
    cloned._elementType = active._elementType
    cloned._dataField = active._dataField
    cloned._barcodeType = active._barcodeType
    cloned._showText = active._showText
    cloned._moduleSize = active._moduleSize
    cloned._ecLevel = active._ecLevel
    cloned._elWidth = active._elWidth
    cloned._elHeight = active._elHeight
    fabricCanvas.add(cloned)
    fabricCanvas.setActiveObject(cloned)
    fabricCanvas.renderAll()
    emit('element-selected', cloned._elementId || null, cloned)
  })
}

// ─── 缩放 ────────────────────────────
function zoomIn() {
  zoomLevel.value = Math.min(zoomLevel.value + 0.25, 4)
  applyZoom()
}

function zoomOut() {
  zoomLevel.value = Math.max(zoomLevel.value - 0.25, 0.25)
  applyZoom()
}

function zoomFit() {
  if (!containerRef.value || !fabricCanvas) return
  const container = containerRef.value
  const { width, height } = getCanvasSize()
  const padding = 60
  const scaleX = (container.clientWidth - padding) / width
  const scaleY = (container.clientHeight - padding) / height
  zoomLevel.value = Math.max(0.25, Math.min(scaleX, scaleY, 4))
  applyZoom()
}

function handleZoomInputClick() {
  const input = zoomInputRef.value
  if (input) {
    input.focus()
    input.select()
  }
}

function handleZoomInputChange(e) {
  const val = parseInt(e.target.value, 10)
  if (!isNaN(val) && val >= 25 && val <= 400) {
    zoomLevel.value = val / 100
    applyZoom()
  }
  e.target.value = Math.round(zoomLevel.value * 100)
}

function handleZoomInputBlur(e) {
  e.target.value = Math.round(zoomLevel.value * 100)
}

function applyZoom() {
  if (!fabricCanvas) return
  fabricCanvas.setZoom(zoomLevel.value)
  const { width, height } = getCanvasSize()
  fabricCanvas.setDimensions({
    width: Math.round(width * zoomLevel.value),
    height: Math.round(height * zoomLevel.value)
  })
}

// ─── 标签尺寸变化 ────────────────────
watch(() => [props.labelWidth, props.labelHeight], () => {
  if (fabricCanvas) {
    const elements = getElementsData()
    fabricCanvas.clear()
    const { width, height } = getCanvasSize()
    fabricCanvas.setDimensions({ width, height })
    fabricCanvas.setZoom(1)
    drawGrid()
    for (const el of elements) {
      addElement(el)
    }
    fabricCanvas.discardActiveObject()
    fabricCanvas.renderAll()
    nextTick(() => zoomFit())
  }
})

// ─── 键盘快捷键 ──────────────────────
function handleKeyDown(e) {
  if (!fabricCanvas) return
  // 焦点在输入框/文本域内时不拦截按键，避免删除键误删画布元素
  const active = document.activeElement
  if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.isContentEditable)) return
  // el-input-number / el-select 等的包裹容器也可能捕获焦点
  if (active && active.closest && active.closest('.el-input, .el-select, .el-textarea')) return
  if (fabricCanvas.getActiveObject()?.isEditing) return

  if (e.key === 'Delete' || e.key === 'Backspace') {
    e.preventDefault()
    deleteSelected()
  }
  if (e.ctrlKey && e.key === 'd') {
    e.preventDefault()
    duplicateSelected()
  }
}

onMounted(() => {
  nextTick(() => initCanvas())
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
  if (fabricCanvas) {
    fabricCanvas.dispose()
    fabricCanvas = null
  }
})

defineExpose({
  getCanvas, getElementsData, loadElements, addElement,
  deleteSelected, duplicateSelected, zoomFit
})
</script>

<style scoped>
.designer-canvas-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.canvas-zoom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.zoom-level {
  min-width: 64px;
  cursor: pointer;
  font-size: 12px;
  padding: 0 6px;
}

.zoom-input {
  width: 32px;
  border: none;
  outline: none;
  background: transparent;
  text-align: center;
  font-size: 12px;
  color: inherit;
  font-family: inherit;
  -moz-appearance: textfield;
}

.zoom-input::-webkit-outer-spin-button,
.zoom-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.canvas-size-info {
  font-size: 12px;
  color: #64748b;
}

.canvas-container {
  flex: 1;
  overflow: auto;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.canvas-scroll-area {
  padding: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.canvas-scroll-area :deep(.canvas-container) {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}
</style>
