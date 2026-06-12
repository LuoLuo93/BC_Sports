/**
 * Canvas 标签渲染器
 * 将模板元素渲染为位图，通过 ZPL ^GFA 命令发送到打印机
 * 不依赖打印机字体，支持中文和任意语言
 */
import JsBarcode from 'jsbarcode'
import QRCode from 'qrcode'
import { resolveContent } from './zplGenerator'

// ─── 坐标换算 ─────────────────────────
function mmToDots(mm, dpi) {
  return Math.round(mm * dpi / 25.4)
}

// ─── 渲染标签到 Canvas ─────────────────
/**
 * 将模板元素渲染到 Canvas 上
 * @param {Object} template - 模板对象
 * @param {Object} dataItem - 数据项
 * @returns {HTMLCanvasElement}
 */
export async function renderLabelToCanvas(template, dataItem = {}) {
  const { labelWidth, labelHeight, dpi = 203, elements = [], offsetX = 0, offsetY = 0 } = template

  const widthPx = mmToDots(labelWidth, dpi)
  const heightPx = mmToDots(labelHeight, dpi)

  // 创建画布
  const canvas = document.createElement('canvas')
  canvas.width = widthPx
  canvas.height = heightPx
  const ctx = canvas.getContext('2d')

  // 白色背景
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, widthPx, heightPx)

  // 黑色前景
  ctx.fillStyle = '#000000'
  ctx.strokeStyle = '#000000'

  // 绘制每个元素
  for (const el of elements) {
    const x = mmToDots((el.x || 0) + offsetX, dpi)
    const y = mmToDots((el.y || 0) + offsetY, dpi)

    switch (el.type) {
      case 'text':
        await drawText(ctx, el, x, y, dataItem, dpi)
        break
      case 'barcode':
        await drawBarcode(ctx, el, x, y, dataItem, dpi)
        break
      case 'qrcode':
        await drawQrcode(ctx, el, x, y, dataItem, dpi)
        break
      case 'line':
        drawLine(ctx, el, x, y, dpi)
        break
      case 'rect':
        drawRect(ctx, el, x, y, dpi)
        break
    }
  }

  return canvas
}

// ─── 绘制文本 ─────────────────────────
async function drawText(ctx, el, x, y, dataItem, dpi) {
  const content = resolveContent(el, dataItem)
  if (!content) return

  const fontSize = mmToDots(el.fontSize || 3, dpi)
  const fontWeight = el.fontWeight === 'bold' ? 'bold' : 'normal'
  ctx.font = `${fontWeight} ${fontSize}px "SimSun", "宋体", "Microsoft YaHei", sans-serif`
  ctx.fillStyle = '#000000'
  ctx.textBaseline = 'top'

  // 对齐方式
  const width = mmToDots(el.width || 50, dpi)
  if (el.textAlign === 'center') {
    ctx.textAlign = 'center'
    ctx.fillText(content, x + width / 2, y)
  } else if (el.textAlign === 'right') {
    ctx.textAlign = 'right'
    ctx.fillText(content, x + width, y)
  } else {
    ctx.textAlign = 'left'
    ctx.fillText(content, x, y)
  }
}

// ─── 绘制条形码 ───────────────────────
async function drawBarcode(ctx, el, x, y, dataItem, dpi) {
  const data = el.dataField
    ? (dataItem[el.dataField] != null ? String(dataItem[el.dataField]) : '1234567890128')
    : '1234567890128'

  const height = mmToDots(el.height || 10, dpi)
  const moduleWidth = el.moduleWidth || 2

  // 创建临时 canvas 绘制条形码
  const barcodeCanvas = document.createElement('canvas')
  try {
    JsBarcode(barcodeCanvas, data, {
      format: el.barcodeType === 'EAN13' ? 'EAN13' : el.barcodeType === 'EAN8' ? 'EAN8' : 'CODE128',
      width: moduleWidth,
      height: height,
      displayValue: el.showText !== false,
      fontSize: mmToDots(2, dpi),
      margin: 0,
      background: 'transparent'
    })
    ctx.drawImage(barcodeCanvas, x, y)
  } catch (e) {
    console.warn('条形码渲染失败:', e)
  }
}

// ─── 绘制二维码 ───────────────────────
async function drawQrcode(ctx, el, x, y, dataItem, dpi) {
  const data = el.dataField
    ? (dataItem[el.dataField] != null ? String(dataItem[el.dataField]) : 'https://example.com')
    : 'https://example.com'

  const moduleSize = el.moduleSize || 4
  const qrCanvas = document.createElement('canvas')
  try {
    await QRCode.toCanvas(qrCanvas, data, {
      width: mmToDots(el.width || 20, dpi),
      margin: 0,
      errorCorrectionLevel: el.errorCorrectionLevel || 'M'
    })
    ctx.drawImage(qrCanvas, x, y)
  } catch (e) {
    console.warn('二维码渲染失败:', e)
  }
}

// ─── 绘制直线 ─────────────────────────
function drawLine(ctx, el, x, y, dpi) {
  const width = mmToDots(el.width || 50, dpi)
  const height = mmToDots(el.height || 0, dpi)
  const thickness = el.strokeWidth || 1

  ctx.lineWidth = thickness
  ctx.strokeStyle = '#000000'
  ctx.beginPath()
  if (height <= 0) {
    // 水平线
    ctx.moveTo(x, y)
    ctx.lineTo(x + width, y)
  } else {
    // 垂直线
    ctx.moveTo(x, y)
    ctx.lineTo(x, y + height)
  }
  ctx.stroke()
}

// ─── 绘制矩形 ─────────────────────────
function drawRect(ctx, el, x, y, dpi) {
  const width = mmToDots(el.width || 50, dpi)
  const height = mmToDots(el.height || 30, dpi)
  const borderWidth = el.strokeWidth || 1

  ctx.lineWidth = borderWidth
  ctx.strokeStyle = '#000000'
  if (el.fill) {
    ctx.fillStyle = '#000000'
    ctx.fillRect(x, y, width, height)
  } else {
    ctx.strokeRect(x, y, width, height)
  }
}

// ─── Canvas 转 ZPL ^GFA 位图 ──────────
/**
 * 将 Canvas 转换为 ZPL ^GFA 命令
 * 使用 RLE 压缩减少数据量
 * @param {HTMLCanvasElement} canvas
 * @returns {string} ZPL ^GFA 命令
 */
export function canvasToZplGFA(canvas) {
  const width = canvas.width
  const height = canvas.height
  const ctx = canvas.getContext('2d')
  const imageData = ctx.getImageData(0, 0, width, height)
  const pixels = imageData.data

  // 转换为黑白位图
  // Zebra ^GFA: 1 = 白色(不打印), 0 = 黑色(打印)
  const bytesPerRow = Math.ceil(width / 8)
  const totalBytes = bytesPerRow * height
  const bitmap = new Uint8Array(totalBytes) // 初始化为 0（全黑）

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const pixelIndex = (y * width + x) * 4
      const r = pixels[pixelIndex]
      const g = pixels[pixelIndex + 1]
      const b = pixels[pixelIndex + 2]
      // 灰度 >= 128 为白色（设置 bit=1），< 128 为黑色（bit=0）
      const isWhite = (r + g + b) / 3 >= 128
      if (isWhite) {
        const byteIndex = y * bytesPerRow + Math.floor(x / 8)
        const bitIndex = 7 - (x % 8)
        bitmap[byteIndex] |= (1 << bitIndex)
      }
    }
  }

  // RLE 压缩编码
  const compressed = rleCompress(bitmap)

  // 生成 ^GFA 命令
  return `^GFA,${totalBytes},${bytesPerRow},${compressed}`
}

// ─── RLE 压缩 ─────────────────────────
/**
 * Zebra RLE 压缩
 * 格式：
 * - 单字节: h = 十六进制值
 * - 重复: <count><h> — count 用 Zebra 编码
 */
function rleCompress(bitmap) {
  let result = ''
  let i = 0

  while (i < bitmap.length) {
    // 查找连续相同字节
    let count = 1
    while (i + count < bitmap.length && bitmap[i + count] === bitmap[i] && count < 255) {
      count++
    }

    const hex = bitmap[i].toString(16).padStart(2, '0').toUpperCase()

    if (count === 1) {
      result += hex
    } else if (count === 2) {
      result += hex + hex
    } else {
      // Zebra RLE: count 用特殊字符编码
      result += zebraRleCount(count) + hex
    }

    i += count
  }

  return result
}

// ─── Zebra RLE 重复次数编码 ────────────
function zebraRleCount(count) {
  if (count < 20) {
    // 1-19: ASCII 32+count (space=1, !=2, "=3, ...)
    return String.fromCharCode(32 + count)
  } else if (count < 280) {
    // 20-279: 两个字符
    const high = Math.floor(count / 20) + 32
    const low = (count % 20) + 32
    return String.fromCharCode(high) + String.fromCharCode(low)
  } else {
    // 280+: 三个字符
    const c1 = Math.floor(count / 280) + 32
    const remainder = count % 280
    const c2 = Math.floor(remainder / 20) + 32
    const c3 = (remainder % 20) + 32
    return String.fromCharCode(c1) + String.fromCharCode(c2) + String.fromCharCode(c3)
  }
}
