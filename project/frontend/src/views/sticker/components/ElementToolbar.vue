<template>
  <div class="element-toolbar">
    <div class="toolbar-section">
      <div class="section-title">元素</div>
      <div class="tool-grid">
        <el-tooltip content="文本" placement="right" :show-after="500">
          <div class="tool-btn" @click="addText">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 7V4h16v3M9 20h6M12 4v16"/></svg>
          </div>
        </el-tooltip>
        <el-tooltip content="条形码" placement="right" :show-after="500">
          <div class="tool-btn" @click="addBarcode">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="4" width="2" height="16" fill="currentColor"/><rect x="6" y="4" width="1" height="16" fill="currentColor"/><rect x="9" y="4" width="3" height="16" fill="currentColor"/><rect x="14" y="4" width="1" height="16" fill="currentColor"/><rect x="17" y="4" width="2" height="16" fill="currentColor"/><rect x="21" y="4" width="1" height="16" fill="currentColor"/></svg>
          </div>
        </el-tooltip>
        <el-tooltip content="二维码" placement="right" :show-after="500">
          <div class="tool-btn" @click="addQrcode">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="3" height="3"/><rect x="18" y="18" width="3" height="3"/><rect x="18" y="14" width="3" height="3"/><rect x="14" y="18" width="3" height="3"/></svg>
          </div>
        </el-tooltip>
        <el-tooltip content="直线" placement="right" :show-after="500">
          <div class="tool-btn" @click="addLine">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="4" y1="20" x2="20" y2="4"/></svg>
          </div>
        </el-tooltip>
        <el-tooltip content="矩形" placement="right" :show-after="500">
          <div class="tool-btn" @click="addRect">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="1"/></svg>
          </div>
        </el-tooltip>
      </div>
    </div>

    <div class="toolbar-divider"></div>

    <div class="toolbar-section">
      <div class="section-title">操作</div>
      <div class="tool-grid">
        <el-tooltip content="删除 (Delete)" placement="right" :show-after="500">
          <div class="tool-btn" :class="{ disabled: !hasSelection }" @click="hasSelection && $emit('delete')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 6h18M8 6V4h8v2M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>
          </div>
        </el-tooltip>
        <el-tooltip content="复制 (Ctrl+D)" placement="right" :show-after="500">
          <div class="tool-btn" :class="{ disabled: !hasSelection }" @click="hasSelection && $emit('duplicate')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
          </div>
        </el-tooltip>
      </div>
    </div>

    <div class="toolbar-divider"></div>

    <div class="toolbar-section">
      <div class="section-title">对齐</div>
      <div class="tool-grid">
        <el-tooltip content="左对齐" placement="right" :show-after="500">
          <div class="tool-btn" :class="{ disabled: !hasSelection }" @click="hasSelection && align('left')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="4" y1="3" x2="4" y2="21"/><rect x="7" y="5" width="13" height="4" rx="1"/><rect x="7" y="12" width="9" height="4" rx="1"/></svg>
          </div>
        </el-tooltip>
        <el-tooltip content="水平居中" placement="right" :show-after="500">
          <div class="tool-btn" :class="{ disabled: !hasSelection }" @click="hasSelection && align('centerH')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="12" y1="3" x2="12" y2="21"/><rect x="5" y="5" width="14" height="4" rx="1"/><rect x="7" y="12" width="10" height="4" rx="1"/></svg>
          </div>
        </el-tooltip>
        <el-tooltip content="右对齐" placement="right" :show-after="500">
          <div class="tool-btn" :class="{ disabled: !hasSelection }" @click="hasSelection && align('right')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="20" y1="3" x2="20" y2="21"/><rect x="4" y="5" width="13" height="4" rx="1"/><rect x="8" y="12" width="9" height="4" rx="1"/></svg>
          </div>
        </el-tooltip>
        <el-tooltip content="上对齐" placement="right" :show-after="500">
          <div class="tool-btn" :class="{ disabled: !hasSelection }" @click="hasSelection && align('top')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="3" y1="4" x2="21" y2="4"/><rect x="5" y="7" width="4" height="13" rx="1"/><rect x="12" y="7" width="4" height="9" rx="1"/></svg>
          </div>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  hasSelection: { type: Boolean, default: false }
})

const emit = defineEmits(['add-element', 'delete', 'duplicate', 'align'])

function addText() {
  emit('add-element', {
    type: 'text',
    content: '文本',
    x: 5, y: 5,
    width: 20, height: 4,
    fontSize: 3, fontWeight: 'normal', textAlign: 'left',
    dataField: ''
  })
}

function addBarcode() {
  emit('add-element', {
    type: 'barcode',
    x: 5, y: 15,
    width: 35, height: 10,
    barcodeType: 'EAN13',
    dataField: 'ean13',
    showText: true
  })
}

function addQrcode() {
  emit('add-element', {
    type: 'qrcode',
    x: 42, y: 20,
    width: 10, height: 10,
    dataField: 'ean13',
    moduleSize: 4,
    errorCorrectionLevel: 'M'
  })
}

function addLine() {
  emit('add-element', {
    type: 'line',
    x: 2, y: 28,
    width: 50, height: 0,
    strokeWidth: 1
  })
}

function addRect() {
  emit('add-element', {
    type: 'rect',
    x: 2, y: 2,
    width: 56, height: 36,
    strokeWidth: 1,
    fill: false
  })
}

function align(direction) {
  emit('align', direction)
}
</script>

<style scoped>
.element-toolbar {
  width: 56px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  padding: 8px 6px;
  gap: 4px;
  overflow-y: auto;
}

.toolbar-section {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.section-title {
  font-size: 10px;
  color: #94a3b8;
  text-align: center;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.tool-grid {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.tool-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #475569;
  border: 1px solid transparent;
}

.tool-btn:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
  color: #0ea5e9;
}

.tool-btn:active {
  background: #e0f2fe;
}

.tool-btn.disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.tool-btn.disabled:hover {
  background: transparent;
  border-color: transparent;
  color: #475569;
}

.tool-btn svg {
  width: 20px;
  height: 20px;
}

.toolbar-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 6px 4px;
}
</style>
