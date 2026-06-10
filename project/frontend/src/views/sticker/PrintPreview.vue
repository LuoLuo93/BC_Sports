<template>
  <div class="preview-page">
    <!-- 顶部工具栏 -->
    <div class="preview-bar">
      <el-button size="small" @click="$router.back()">← 返回</el-button>
      <el-select v-model="selectedTemplateId" size="small" placeholder="选择模板" style="width: 200px">
        <el-option v-for="t in templateList" :key="t.id" :label="t.templateName" :value="t.id">
          <span>{{ t.templateName }}</span>
          <span style="float:right;color:#94a3b8;font-size:11px">{{ t.labelWidth }}×{{ t.labelHeight }}mm</span>
        </el-option>
      </el-select>
      <el-button type="primary" size="small" :disabled="!selectedTemplateId || printing" @click="doPrintAll">
        {{ printing ? `打印中 (${printIndex + 1}/${printItems.length})` : '循环打印' }}
      </el-button>
      <span class="preview-info">共 {{ printItems.length }} 张标签</span>
    </div>

    <!-- 贴纸预览区域（分页渲染，避免大量 DOM） -->
    <div class="preview-body" id="print-area">
      <div class="sticker-grid">
        <div v-for="(item, idx) in pageItems" :key="pageOffset + idx" class="sticker-wrapper" :class="{ 'sticker-current': printing && (pageOffset + idx) === printIndex }">
          <StickerRenderer
            v-if="printElements.length > 0"
            :elements="printElements"
            :data="item"
            :width="selectedTemplate?.labelWidth || 60"
            :height="selectedTemplate?.labelHeight || 40"
            :dpi="selectedTemplate?.dpi || 203"
          />
        </div>
      </div>
      <!-- 分页控件 -->
      <div v-if="totalPages > 1" class="pagination">
        <el-button size="small" :disabled="currentPage <= 1" @click="currentPage--">上一页</el-button>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <el-button size="small" :disabled="currentPage >= totalPages" @click="currentPage++">下一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPrintOrder, getTemplateList } from '@/api/sticker'
import { ElMessage } from 'element-plus'
import StickerRenderer from './components/StickerRenderer.vue'

const PAGE_SIZE = 20

const route = useRoute()
const router = useRouter()
const order = ref({ details: [] })
const templateList = ref([])
const selectedTemplateId = ref(null)
const printing = ref(false)
const printIndex = ref(0)
const currentPage = ref(1)

const selectedTemplate = computed(() => templateList.value.find(t => t.id === selectedTemplateId.value) || null)

const printItems = computed(() => {
  const items = []
  for (const d of (order.value.details || [])) {
    for (let i = 0; i < (d.printQty || 1); i++) {
      items.push(d)
    }
  }
  return items
})

const printElements = computed(() => {
  if (!selectedTemplate.value) return []
  const tpl = selectedTemplate.value
  try {
    return typeof tpl.templateData === 'string' ? JSON.parse(tpl.templateData) : tpl.templateData || []
  } catch { return [] }
})

const totalPages = computed(() => Math.ceil(printItems.value.length / PAGE_SIZE))

const pageOffset = computed(() => (currentPage.value - 1) * PAGE_SIZE)

const pageItems = computed(() => {
  const start = pageOffset.value
  return printItems.value.slice(start, start + PAGE_SIZE)
})

async function loadData() {
  const { data } = await getPrintOrder(route.params.orderId)
  order.value = data
}

async function loadTemplates() {
  try {
    const res = await getTemplateList()
    templateList.value = res.data || []
    const defaultTpl = templateList.value.find(t => t.isDefault === 1)
    if (defaultTpl) selectedTemplateId.value = defaultTpl.id
    else if (templateList.value.length > 0) selectedTemplateId.value = templateList.value[0].id
  } catch { /* ignore */ }
}

// 生成单张小票的打印 HTML（纯数据驱动，不依赖 DOM）
function buildStickerHTML(item) {
  const labelW = selectedTemplate.value?.labelWidth || 60
  const labelH = selectedTemplate.value?.labelHeight || 40
  const elements = printElements.value

  let inner = ''
  for (const el of elements) {
    const resolve = (content) => {
      if (!content) return ''
      return content.replace(/\{\{(\w+)\}\}/g, (_, field) => {
        return item[field] != null ? String(item[field]) : ''
      })
    }

    if (el.type === 'text') {
      inner += `<div style="position:absolute;left:${el.x}mm;top:${el.y}mm;font-size:${el.fontSize}mm;font-weight:${el.fontWeight || 'normal'};text-align:${el.textAlign || 'left'};font-family:Arial,sans-serif;line-height:1.2;white-space:nowrap;color:#000">${resolve(el.content)}</div>`
    } else if (el.type === 'line') {
      inner += `<div style="position:absolute;left:${el.x}mm;top:${el.y}mm;width:${el.width}mm;height:${el.strokeWidth || 1}px;background:#000"></div>`
    } else if (el.type === 'rect') {
      inner += `<div style="position:absolute;left:${el.x}mm;top:${el.y}mm;width:${el.width}mm;height:${el.height}mm;border:${el.strokeWidth || 1}px solid #000;background:${el.fill ? '#000' : 'transparent'}"></div>`
    } else if (el.type === 'barcode') {
      const val = el.dataField && item[el.dataField] ? item[el.dataField] : '1234567890128'
      inner += `<div style="position:absolute;left:${el.x}mm;top:${el.y}mm;width:${el.width}mm;height:${el.height}mm;overflow:hidden"><svg class="barcode" data-value="${val}" data-type="${el.barcodeType || 'EAN13'}" data-show="${el.showText !== false}"></svg></div>`
    } else if (el.type === 'qrcode') {
      const val = el.dataField && item[el.dataField] ? item[el.dataField] : ''
      inner += `<div style="position:absolute;left:${el.x}mm;top:${el.y}mm;width:${el.width}mm;height:${el.width}mm" data-qrcode="${val}"></div>`
    }
  }

  return `<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>打印</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
@page{size:${labelW}mm ${labelH}mm;margin:0}
body{width:${labelW}mm;height:${labelH}mm;overflow:hidden;position:relative;background:#fff}
.s{width:${labelW}mm;height:${labelH}mm;position:relative}
.barcode{display:block;width:100%;height:100%}
</style>
</head><body>
<div class="s">${inner}</div>
<script src="https://cdn.jsdelivr.net/npm/jsbarcode@3.11.6/dist/JsBarcode.all.min.js"><\/script>
<script src="https://cdn.jsdelivr.net/npm/qrcode@1.5.4/build/qrcode.min.js"><\/script>
<script>
document.querySelectorAll('.barcode').forEach(function(el){
  try{JsBarcode(el,el.dataset.value,{format:el.dataset.type,width:1.5,height:30,displayValue:el.dataset.show==='true',fontSize:10,margin:0,textMargin:2})}catch(e){console.warn(e)}
});
document.querySelectorAll('[data-qrcode]').forEach(function(el){
  var v=el.getAttribute('data-qrcode');
  if(v&&typeof QRCode!=='undefined'){QRCode.toDataURL(v,{width:200,margin:0}).then(function(url){el.innerHTML='<img src="'+url+'" style="width:100%;height:100%;object-fit:contain">'})}
});
window._ready=true;
<\/script></body></html>`
}

// 循环打印
function doPrintAll() {
  if (printing.value) return
  if (!printItems.value.length) return
  printing.value = true
  printIndex.value = 0
  printNext()
}

function printNext() {
  if (printIndex.value >= printItems.value.length) {
    printing.value = false
    printIndex.value = 0
    return
  }

  // 自动翻页到当前打印位置
  currentPage.value = Math.floor(printIndex.value / PAGE_SIZE) + 1

  const item = printItems.value[printIndex.value]
  const html = buildStickerHTML(item)
  const win = window.open('', '_blank', 'width=400,height=300')

  if (!win) {
    ElMessage.warning('请允许弹出窗口后再试')
    printing.value = false
    return
  }

  win.document.write(html)
  win.document.close()

  const checkReady = setInterval(() => {
    if (win._ready) {
      clearInterval(checkReady)
      win.print()

      const waitClose = setInterval(() => {
        if (win.closed || !win.document) {
          clearInterval(waitClose)
          printIndex.value++
          printNext()
        }
      }, 300)

      setTimeout(() => {
        clearInterval(waitClose)
        if (!win.closed) win.close()
        printIndex.value++
        printNext()
      }, 5000)
    }
  }, 100)
}

onMounted(() => {
  loadData()
  loadTemplates()
})
</script>

<style scoped>
.preview-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.preview-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.preview-info {
  margin-left: auto;
  font-size: 13px;
  color: #94a3b8;
}

.preview-body {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

.sticker-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-start;
}

.sticker-wrapper {
  border: 1px dashed #d1d5db;
  border-radius: 2px;
  position: relative;
  transition: border-color 0.2s;
}

.sticker-current {
  border-color: #3b82f6;
  border-width: 2px;
  box-shadow: 0 0 0 2px rgba(59,130,246,0.2);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.page-info {
  font-size: 13px;
  color: #64748b;
}
</style>
