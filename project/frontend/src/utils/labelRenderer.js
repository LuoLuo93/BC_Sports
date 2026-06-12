/**
 * HTML 标签渲染器
 * 用 HTML/CSS 渲染标签，然后用 html2canvas 转成图片
 * 支持中文、任意字体、复杂排版
 * 与 BarTender 原理一致（渲染成图片发送到打印机）
 */
import html2canvas from 'html2canvas'
import JsBarcode from 'jsbarcode'
import QRCode from 'qrcode'
import { resolveContent } from './zplGenerator'

// ─── 坐标换算 ─────────────────────────
function mmToPx(mm, dpi) {
  return Math.round(mm * dpi / 25.4)
}

// ─── 渲染标签为 Canvas ─────────────────
/**
 * 将模板元素渲染到隐藏的 HTML 元素，然后转成 Canvas
 * @param {Object} template - 模板对象
 * @param {Object} dataItem - 数据项
 * @returns {Promise<HTMLCanvasElement>}
 */
export async function renderLabelToCanvas(template, dataItem = {}) {
  const { labelWidth, labelHeight, dpi = 203, elements = [], offsetX = 0, offsetY = 0 } = template

  const widthPx = mmToPx(labelWidth, dpi)
  const heightPx = mmToPx(labelHeight, dpi)

  // 创建隐藏的容器
  const container = document.createElement('div')
  container.style.cssText = `
    position: fixed;
    left: -9999px;
    top: -9999px;
    width: ${widthPx}px;
    height: ${heightPx}px;
    background: white;
    overflow: hidden;
    font-family: "SimSun", "宋体", "Microsoft YaHei", sans-serif;
  `
  document.body.appendChild(container)

  try {
    // 渲染每个元素
    for (const el of elements) {
      const x = mmToPx((el.x || 0) + offsetX, dpi)
      const y = mmToPx((el.y || 0) + offsetY, dpi)

      switch (el.type) {
        case 'text':
          renderText(container, el, x, y, dataItem, dpi)
          break
        case 'barcode':
          await renderBarcode(container, el, x, y, dataItem, dpi)
          break
        case 'qrcode':
          await renderQrcode(container, el, x, y, dataItem, dpi)
          break
        case 'line':
          renderLine(container, el, x, y, dpi)
          break
        case 'rect':
          renderRect(container, el, x, y, dpi)
          break
      }
    }

    // 用 html2canvas 转成 Canvas
    const canvas = await html2canvas(container, {
      width: widthPx,
      height: heightPx,
      scale: 1,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false
    })

    return canvas
  } finally {
    // 清理临时元素
    document.body.removeChild(container)
  }
}

// ─── 渲染文本 ─────────────────────────
function renderText(container, el, x, y, dataItem, dpi) {
  const content = resolveContent(el, dataItem)
  if (!content) return

  const fontSize = mmToPx(el.fontSize || 3, dpi)
  const fontWeight = el.fontWeight === 'bold' ? 'bold' : 'normal'
  const width = mmToPx(el.width || 50, dpi)

  const div = document.createElement('div')
  div.style.cssText = `
    position: absolute;
    left: ${x}px;
    top: ${y}px;
    width: ${width}px;
    font-size: ${fontSize}px;
    font-weight: ${fontWeight};
    color: black;
    white-space: nowrap;
    overflow: hidden;
  `

  // 对齐方式
  if (el.textAlign === 'center') {
    div.style.textAlign = 'center'
  } else if (el.textAlign === 'right') {
    div.style.textAlign = 'right'
  } else {
    div.style.textAlign = 'left'
  }

  div.textContent = content
  container.appendChild(div)
}

// ─── 渲染条形码 ───────────────────────
async function renderBarcode(container, el, x, y, dataItem, dpi) {
  const data = el.dataField
    ? (dataItem[el.dataField] != null ? String(dataItem[el.dataField]) : '1234567890128')
    : '1234567890128'

  const height = mmToPx(el.height || 10, dpi)
  const moduleWidth = el.moduleWidth || 2

  // 创建临时 canvas 绘制条形码
  const barcodeCanvas = document.createElement('canvas')
  try {
    JsBarcode(barcodeCanvas, data, {
      format: el.barcodeType === 'EAN13' ? 'EAN13' : el.barcodeType === 'EAN8' ? 'EAN8' : 'CODE128',
      width: moduleWidth,
      height: height,
      displayValue: el.showText !== false,
      fontSize: mmToPx(2, dpi),
      margin: 0,
      background: 'transparent'
    })

    const img = document.createElement('img')
    img.src = barcodeCanvas.toDataURL()
    img.style.cssText = `
      position: absolute;
      left: ${x}px;
      top: ${y}px;
    `
    container.appendChild(img)
  } catch (e) {
    console.warn('条形码渲染失败:', e)
  }
}

// ─── 渲染二维码 ───────────────────────
async function renderQrcode(container, el, x, y, dataItem, dpi) {
  const data = el.dataField
    ? (dataItem[el.dataField] != null ? String(dataItem[el.dataField]) : 'https://example.com')
    : 'https://example.com'

  const qrCanvas = document.createElement('canvas')
  try {
    await QRCode.toCanvas(qrCanvas, data, {
      width: mmToPx(el.width || 20, dpi),
      margin: 0,
      errorCorrectionLevel: el.errorCorrectionLevel || 'M'
    })

    const img = document.createElement('img')
    img.src = qrCanvas.toDataURL()
    img.style.cssText = `
      position: absolute;
      left: ${x}px;
      top: ${y}px;
    `
    container.appendChild(img)
  } catch (e) {
    console.warn('二维码渲染失败:', e)
  }
}

// ─── 渲染直线 ─────────────────────────
function renderLine(container, el, x, y, dpi) {
  const width = mmToPx(el.width || 50, dpi)
  const height = mmToPx(el.height || 0, dpi)
  const thickness = el.strokeWidth || 1

  const div = document.createElement('div')
  if (height <= 0) {
    // 水平线
    div.style.cssText = `
      position: absolute;
      left: ${x}px;
      top: ${y}px;
      width: ${width}px;
      height: ${thickness}px;
      background: black;
    `
  } else {
    // 垂直线
    div.style.cssText = `
      position: absolute;
      left: ${x}px;
      top: ${y}px;
      width: ${thickness}px;
      height: ${height}px;
      background: black;
    `
  }
  container.appendChild(div)
}

// ─── 渲染矩形 ─────────────────────────
function renderRect(container, el, x, y, dpi) {
  const width = mmToPx(el.width || 50, dpi)
  const height = mmToPx(el.height || 30, dpi)
  const borderWidth = el.strokeWidth || 1

  const div = document.createElement('div')
  div.style.cssText = `
    position: absolute;
    left: ${x}px;
    top: ${y}px;
    width: ${width}px;
    height: ${height}px;
  `

  if (el.fill) {
    div.style.background = 'black'
  } else {
    div.style.border = `${borderWidth}px solid black`
  }

  container.appendChild(div)
}
