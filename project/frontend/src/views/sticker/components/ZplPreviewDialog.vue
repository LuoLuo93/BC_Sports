<template>
  <el-dialog v-model="visible" title="ZPL 指令预览" width="680px" :close-on-click-modal="false">
    <div class="zpl-preview">
      <div class="zpl-actions">
        <el-button size="small" :icon="CopyDocument" @click="copyZpl">复制 ZPL</el-button>
        <el-button size="small" :icon="Download" @click="downloadZpl">下载 .zpl 文件</el-button>
      </div>
      <div class="zpl-code">
        <pre><code>{{ zplCode }}</code></pre>
      </div>
      <div class="zpl-info">
        <span>标签尺寸：{{ labelWidth }}mm × {{ labelHeight }}mm</span>
        <span>DPI：{{ dpi }}</span>
        <span>字节数：{{ zplCode.length }}</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { CopyDocument, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  zplCode: { type: String, default: '' },
  labelWidth: { type: Number, default: 60 },
  labelHeight: { type: Number, default: 40 },
  dpi: { type: Number, default: 203 }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)

watch(() => props.modelValue, v => { visible.value = v })
watch(visible, v => { emit('update:modelValue', v) })

async function copyZpl() {
  try {
    await navigator.clipboard.writeText(props.zplCode)
    ElMessage.success('ZPL 指令已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

function downloadZpl() {
  const blob = new Blob([props.zplCode], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `label_${props.labelWidth}x${props.labelHeight}_${Date.now()}.zpl`
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
</script>

<style scoped>
.zpl-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.zpl-actions {
  display: flex;
  gap: 8px;
}

.zpl-code {
  background: #1e293b;
  border-radius: 8px;
  padding: 16px;
  max-height: 400px;
  overflow: auto;
}

.zpl-code pre {
  margin: 0;
  color: #e2e8f0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.zpl-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
