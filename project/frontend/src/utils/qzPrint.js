/**
 * QZ Tray 打印工具
 * 实现无弹窗连续打印 ZPL 标签
 * 通过 npm 安装 qz-tray@2.2.6，内置 RSVP/Sha256 依赖
 * 配合后端自签名证书实现静默打印
 *
 * 使用图片模式打印（Canvas 渲染 + ^GFA），不依赖打印机字体
 */
import qz from 'qz-tray'
import { generateImageZPL } from './zplGenerator'

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
 * 使用 QZ Tray 打印 ZPL（图片模式）
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

  // 分批打印（每批 10 张，图片模式数据量较大）
  const BATCH_SIZE = 10
  const batches = []
  for (let i = 0; i < items.length; i += BATCH_SIZE) {
    batches.push(items.slice(i, i + BATCH_SIZE))
  }

  const config = qz.configs.create(printerName)

  for (let i = 0; i < batches.length; i++) {
    // 生成图片模式 ZPL（异步，需要 Canvas 渲染）
    const zplParts = []
    for (const item of batches[i]) {
      const zpl = await generateImageZPL(tpl, item)
      zplParts.push(zpl)
    }
    const batchZpl = zplParts.join('')

    await qz.print(config, [batchZpl])

    if (onProgress) {
      const printed = Math.min((i + 1) * BATCH_SIZE, items.length)
      onProgress(printed, items.length)
    }
  }

  return items.length
}
