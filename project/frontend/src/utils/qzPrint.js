/**
 * QZ Tray 打印工具
 * 使用图片模式打印（HTML 渲染 + html2canvas + QZ Tray 图片打印）
 * 与 BarTender 原理一致，不依赖打印机字体
 */
import qz from 'qz-tray'
import { renderLabelToCanvas } from './labelRenderer'

// 使用浏览器原生 Promise
qz.api.setPromiseType(resolver => new Promise(resolver))

// 配置证书（从后端获取）
qz.security.setCertificatePromise((resolve, reject) => {
  fetch('/bcsports/api/qz/certificate')
    .then(r => r.text())
    .then(resolve)
    .catch(reject)
})

// 配置签名（从后端获取）
qz.security.setSignaturePromise(hash => (resolve, reject) => {
  fetch('/bcsports/api/qz/sign', {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain' },
    body: hash
  })
    .then(r => r.text())
    .then(resolve)
    .catch(reject)
})

/**
 * 连接 QZ Tray（全局只连一次）
 */
let connected = false

export async function connectQZ() {
  if (connected && qz.websocket.isActive()) {
    return qz
  }
  await qz.websocket.connect()
  connected = true
  return qz
}

/**
 * 断开连接
 */
export async function disconnectQZ() {
  if (connected && qz.websocket.isActive()) {
    await qz.websocket.disconnect()
    connected = false
  }
}

/**
 * 获取打印机列表
 */
export async function getPrinters() {
  await connectQZ()

  try {
    const list = await qz.printers.find()
    console.log('打印机列表:', list)
    return Array.isArray(list) ? list : []
  } catch (e) {
    console.error('获取打印机列表失败:', e)
    throw e
  }
}

/**
 * 批量打印订单明细（图片模式）
 * @param {string} printerName - 打印机名称
 * @param {Object} template - 模板对象
 * @param {Array} details - 明细数组（每项含 printQty）
 * @param {Function} onProgress - 进度回调 (current, total)
 */
export async function printOrderDetails(printerName, template, details, onProgress) {
  await connectQZ()

  // 展开明细为单个标签列表
  const items = []
  for (const d of details) {
    for (let i = 0; i < (d.printQty || 1); i++) {
      items.push(d)
    }
  }

  if (items.length === 0) {
    throw new Error('没有可打印的标签')
  }

  // 准备模板参数
  const tpl = {
    labelWidth: template.labelWidth || 60,
    labelHeight: template.labelHeight || 40,
    dpi: template.dpi || 203,
    elements: typeof template.templateData === 'string'
      ? JSON.parse(template.templateData)
      : (template.templateData || []),
    offsetX: template.offsetX || 0,
    offsetY: template.offsetY || 0
  }

  const config = qz.configs.create(printerName)

  // 先渲染所有标签为图片
  const printData = []
  for (let i = 0; i < items.length; i++) {
    const canvas = await renderLabelToCanvas(tpl, items[i])
    const dataUrl = canvas.toDataURL('image/png')
    printData.push({
      type: 'image',
      format: 'image',
      data: dataUrl
    })

    if (onProgress) {
      onProgress(i + 1, items.length)
    }
  }

  // 一次性发送所有图片到打印机（只触发一次安全确认）
  await qz.print(config, printData)

  return items.length
}
