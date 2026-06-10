<template>
  <div class="sticker" :style="stickerStyle" ref="rootRef">
    <template v-for="el in elements" :key="el.id">
      <!-- 文本 -->
      <div v-if="el.type === 'text'" class="el-text" :style="textStyle(el)">
        {{ resolveText(el.content, data) }}
      </div>

      <!-- 条形码 -->
      <svg v-if="el.type === 'barcode'" class="el-barcode" :style="posStyle(el)"></svg>

      <!-- 二维码 -->
      <img v-if="el.type === 'qrcode'" class="el-qrcode" :style="posStyle(el)" :src="qrSources[el.id] || ''" />

      <!-- 线条 -->
      <div v-if="el.type === 'line'" class="el-line" :style="lineStyle(el)"></div>

      <!-- 矩形 -->
      <div v-if="el.type === 'rect'" class="el-rect" :style="rectStyle(el)"></div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, watch, nextTick, reactive, ref } from 'vue'
import JsBarcode from 'jsbarcode'
import QRCode from 'qrcode'

const props = defineProps({
  elements: { type: Array, default: () => [] },
  data: { type: Object, default: () => ({}) },
  width: { type: Number, default: 60 },
  height: { type: Number, default: 40 },
  dpi: { type: Number, default: 203 }
})

const rootRef = ref(null)
const qrSources = reactive({})

// 解析 {{field}} 占位符
function resolveText(content, data) {
  if (!content) return ''
  return content.replace(/\{\{(\w+)\}\}/g, (_, field) => {
    return data[field] != null ? String(data[field]) : ''
  })
}

// 获取条码数据
function getBarcodeData(el) {
  if (el.dataField && props.data[el.dataField]) {
    const val = String(props.data[el.dataField]).trim()
    if (val) return val
  }
  return '1234567890128'
}

// 样式
const stickerStyle = computed(() => ({
  width: `${props.width}mm`,
  height: `${props.height}mm`,
  position: 'relative',
  overflow: 'hidden',
  background: '#fff',
  flexShrink: '0'
}))

function posStyle(el) {
  return {
    position: 'absolute',
    left: `${el.x}mm`,
    top: `${el.y}mm`,
    width: `${el.width}mm`,
    height: el.type === 'qrcode' ? `${el.width}mm` : `${el.height}mm`
  }
}

function textStyle(el) {
  return {
    position: 'absolute',
    left: `${el.x}mm`,
    top: `${el.y}mm`,
    fontSize: `${el.fontSize}mm`,
    fontWeight: el.fontWeight || 'normal',
    textAlign: el.textAlign || 'left',
    fontFamily: 'Arial, sans-serif',
    lineHeight: 1.2,
    whiteSpace: 'nowrap'
  }
}

function lineStyle(el) {
  return {
    position: 'absolute',
    left: `${el.x}mm`,
    top: `${el.y}mm`,
    width: `${el.width}mm`,
    height: `${el.strokeWidth || 1}px`,
    background: '#000'
  }
}

function rectStyle(el) {
  return {
    position: 'absolute',
    left: `${el.x}mm`,
    top: `${el.y}mm`,
    width: `${el.width}mm`,
    height: `${el.height}mm`,
    border: `${el.strokeWidth || 1}px solid #000`,
    background: el.fill ? '#000' : 'transparent'
  }
}

async function renderBarcodes() {
  await nextTick()
  if (!rootRef.value) return
  // 渲染条形码：在当前组件根节点内查找 SVG
  const svgs = rootRef.value.querySelectorAll('.el-barcode') || []
  const barcodeElements = props.elements.filter(el => el.type === 'barcode')
  console.log('[StickerRenderer] barcodeElements:', barcodeElements.length, 'svgs:', svgs.length)
  for (let i = 0; i < barcodeElements.length && i < svgs.length; i++) {
    const svg = svgs[i]
    if (!svg) continue
    try {
      const value = getBarcodeData(barcodeElements[i])
      JsBarcode(svg, value, {
        format: barcodeElements[i].barcodeType === 'EAN13' ? 'EAN13' : barcodeElements[i].barcodeType === 'EAN8' ? 'EAN8' : 'CODE128',
        width: 1.5,
        height: 30,
        displayValue: barcodeElements[i].showText !== false,
        fontSize: 10,
        margin: 0,
        textMargin: 2
      })
    } catch (e) {
      console.error('[StickerRenderer] Barcode render error:', e.message, 'value:', getBarcodeData(barcodeElements[i]))
    }
  }
}

async function renderQRCodes() {
  await nextTick()
  for (const el of props.elements) {
    if (el.type !== 'qrcode') continue
    const value = el.dataField && props.data[el.dataField]
      ? String(props.data[el.dataField])
      : ''
    if (value) {
      try {
        qrSources[el.id] = await QRCode.toDataURL(value, {
          width: el.width * 5,
          margin: 0,
          errorCorrectionLevel: el.errorCorrectionLevel || 'M'
        })
      } catch (e) {
        console.warn('QR render error:', e)
      }
    }
  }
}

onMounted(() => {
  renderBarcodes()
  renderQRCodes()
})

watch(
  () => [props.elements, props.data],
  () => {
    renderBarcodes()
    renderQRCodes()
  },
  { deep: true }
)
</script>

<style scoped>
.sticker {
  box-sizing: border-box;
}
.el-text, .el-barcode, .el-qrcode, .el-line, .el-rect {
  box-sizing: border-box;
}
.el-qrcode {
  object-fit: contain;
}
</style>
