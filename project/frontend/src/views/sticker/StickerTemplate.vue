<template>
  <div class="sticker-item" :style="{ width: stickerWidth + 'mm', height: stickerHeight + 'mm' }">
    <div class="sticker-brand">{{ item.brandName }}</div>
    <div class="sticker-name">{{ item.articleName }}</div>
    <div class="sticker-info">
      <span>货号: {{ item.articleNo }}</span>
      <span>尺码: {{ item.sizeName }}</span>
    </div>
    <svg :id="'barcode-' + item.ean13 + '-' + index"></svg>
    <div class="sticker-price">&yen;{{ item.price }}</div>
  </div>
</template>

<script setup>
import { onMounted, nextTick } from 'vue'
import JsBarcode from 'jsbarcode'

const props = defineProps({
  item: { type: Object, required: true },
  index: { type: Number, default: 0 },
  stickerWidth: { type: Number, default: 40 },
  stickerHeight: { type: Number, default: 25 }
})

onMounted(async () => {
  await nextTick()
  if (props.item.ean13) {
    JsBarcode(`#barcode-${props.item.ean13}-${props.index}`, props.item.ean13, {
      format: 'EAN13',
      width: 1.5,
      height: 30,
      displayValue: true,
      fontSize: 10,
      margin: 2
    })
  }
})
</script>

<style scoped>
.sticker-item {
  border: 1px solid #ccc;
  padding: 2mm;
  box-sizing: border-box;
  font-size: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  page-break-inside: avoid;
}
.sticker-brand {
  font-weight: bold;
  font-size: 11px;
  margin-bottom: 1mm;
}
.sticker-name {
  font-size: 9px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  margin-bottom: 1mm;
}
.sticker-info {
  display: flex;
  justify-content: space-between;
  width: 100%;
  font-size: 8px;
  color: #666;
  margin-bottom: 1mm;
}
.sticker-price {
  font-weight: bold;
  color: #e4393c;
  margin-top: 1mm;
}
</style>
