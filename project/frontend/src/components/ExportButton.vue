<template>
  <el-button :type="type" size="small" :icon="icon" :loading="loading" :disabled="loading" @click="doExport">{{ label }}</el-button>
</template>

<script setup>
/**
 * 通用 Excel 导出按钮：点击调 fetch() 拿 blob（拦截器对 blob 直接返回 Blob 数据），浏览器保存为 filename。
 * 错误识别两条路：后端业务异常返回 200+JSON 时 blob.type 是 application/json，读出 message 弹准确提示；
 * HTTP 4xx/5xx 时拦截器只能弹"网络请求失败"（error.response.data 是 blob 拿不到 message），
 * 本组件再从 blob 读出真实原因补一条。请求超时已由各模块 api 放宽（如导出 10 分钟）。
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'

const props = defineProps({
  /** () => Promise<Blob>，页面闭包捕获当前查询条件等参数 */
  fetch: { type: Function, required: true },
  /** 下载保存的文件名（含扩展名） */
  filename: { type: String, required: true },
  label: { type: String, default: '导出Excel' },
  type: { type: String, default: 'success' },
  icon: { type: [Object, String], default: Download },
})

const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
const loading = ref(false)

async function readJsonMessage(blobData) {
  try {
    const json = JSON.parse(await blobData.text())
    return json.message || json.msg || '导出失败'
  } catch {
    return '导出失败'
  }
}

async function doExport() {
  loading.value = true
  try {
    const blobData = await props.fetch()
    if (blobData && blobData.type === 'application/json') {
      ElMessage.error(await readJsonMessage(blobData))
      return
    }
    const blob = new Blob([blobData], { type: XLSX_MIME })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = props.filename
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出完成')
  } catch (e) {
    const errBlob = e?.response?.data
    if (errBlob instanceof Blob) {
      ElMessage.error(await readJsonMessage(errBlob))
    } else if (!e?.response) {
      ElMessage.error('导出失败：' + (e.message || '网络异常'))
    }
    // 带 response 的非 blob 错误（401/403 等）拦截器已提示，不重复弹
  } finally {
    loading.value = false
  }
}
</script>
