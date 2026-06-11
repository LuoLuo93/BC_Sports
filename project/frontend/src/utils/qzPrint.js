/**
 * QZ Tray 打印工具
 * 实现无弹窗连续打印 ZPL 标签
 */
import { generateZPL } from './zplGenerator'

let qz = null

/**
 * 动态加载 QZ Tray 库
 */
async function loadQZ() {
  if (qz) return qz

  try {
    // 尝试从 CDN 加载
    const module = await import('https://cdn.jsdelivr.net/npm/qz-tray@2.2.4/qz-tray.js')
    qz = module.default || window.qz
    return qz
  } catch (e) {
    throw new Error('请先安装 QZ Tray 客户端: https://qz.io/download/')
  }
}

/**
 * 连接 QZ Tray
 */
export async function connectQZ() {
  const qzInstance = await loadQZ()
  if (!qzInstance.websocket.isActive()) {
    await qzInstance.websocket.connect()
  }
  return qzInstance
}

/**
 * 断开连接
 */
export async function disconnectQZ() {
  if (qz && qz.websocket.isActive()) {
    await qz.websocket.disconnect()
  }
}

/**
 * 获取打印机列表
 */
export async function getPrinters() {
  await connectQZ()
  return await qz.printers.list()
}

/**
 * 使用 QZ Tray 打印 ZPL
 * @param {string} printerName - 打印机名称
 * @param {string[]} zplArray - ZPL 指令数组
 */
export async function printZPL(printerName, zplArray) {
  await connectQZ()
  const config = qz.configs.create(printerName)
  await qz.print(config, zplArray)
}

/**
 * 批量打印订单明细
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

  // 批量生成 ZPL
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

  // 分批打印（每批 50 张，避免数据过大）
  const BATCH_SIZE = 50
  const batches = []
  for (let i = 0; i < items.length; i += BATCH_SIZE) {
    batches.push(items.slice(i, i + BATCH_SIZE))
  }

  const config = qz.configs.create(printerName)

  for (let i = 0; i < batches.length; i++) {
    const batchZpl = batches[i].map(item => generateZPL(tpl, item))
    await qz.print(config, batchZpl)

    if (onProgress) {
      const printed = Math.min((i + 1) * BATCH_SIZE, items.length)
      onProgress(printed, items.length)
    }
  }

  return items.length
}
