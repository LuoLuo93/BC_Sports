/**
 * ZPL II 标签指令生成器
 * 将模板元素列表 + 数据生成斑马打印机 ZPL 指令
 */

// ─── 坐标换算 ─────────────────────────
// mm → dots
function mmToDots(mm, dpi) {
  return Math.round(mm * dpi / 25.4)
}

// ─── ZPL 特殊字符转义 ──────────────────
function escapeZpl(str) {
  if (!str) return ''
  return str.replace(/\^/g, '').replace(/~/g, '')
}

// ─── 检测是否包含非 ASCII 字符（中文/日文/韩文等）──
function hasNonAscii(str) {
  return /[^\x00-\x7F]/.test(str)
}

// ─── 生成字体命令 ─────────────────────
// ASCII 用 ^A0（内置字体），非 ASCII 用 ^A@（TrueType 字体）
function fontCmd(height, width, content) {
  if (hasNonAscii(content)) {
    // ^A@N,h,w,fontName — 使用下载到打印机的 TrueType 字体
    return `^A@N,${height},0,SIMSUN`
  }
  return `^A0,${height},${width}`
}

// ─── 数据解析 ─────────────────────────
/**
 * 解析元素内容，替换 {{field}} 为实际数据
 */
export function resolveContent(element, dataItem) {
  if (!element.content) return ''
  let result = element.content
  result = result.replace(/\{\{(\w+)\}\}/g, (match, field) => {
    return dataItem[field] != null ? String(dataItem[field]) : ''
  })
  return result
}

// ─── 文本 ZPL ─────────────────────────
function generateTextZPL(x, y, el, content, dpi) {
  if (!content) return ''
  const safeContent = escapeZpl(content)
  const fontHeight = mmToDots(el.fontSize || 3, dpi)
  const fontWidth = Math.round(fontHeight * 0.6)
  const fCmd = fontCmd(fontHeight, fontWidth, safeContent)
  let zpl = `^FO${x},${y}`
  zpl += fCmd
  // 对齐方式（ZPL 不直接支持，通过 ^FB 域块实现）
  if (el.textAlign === 'center' || el.textAlign === 'right') {
    const blockWidth = mmToDots(el.width || 50, dpi)
    const align = el.textAlign === 'center' ? 'C' : 'R'
    zpl += `^FB${blockWidth},1,0,${align},0`
  }
  // 加粗通过重复偏移打印模拟
  if (el.fontWeight === 'bold') {
    zpl += `^FD${safeContent}^FS\n`
    zpl += `^FO${x + 1},${y}${fCmd}^FD${safeContent}^FS`
  } else {
    zpl += `^FD${safeContent}^FS`
  }
  return zpl
}

// ─── 条形码 ZPL ───────────────────────
function generateBarcodeZPL(x, y, el, content, dpi) {
  if (!content) return ''
  const safeContent = escapeZpl(content)
  const moduleWidth = el.moduleWidth || 2
  const barcodeHeight = mmToDots(el.height || 10, dpi)
  const showText = el.showText !== false ? 'Y' : 'N'

  let zpl = `^FO${x},${y}`
  zpl += `^BY${moduleWidth},2,${barcodeHeight}`

  if (el.barcodeType === 'EAN13') {
    // ^BENo,h,f,g : o=方向, h=高度, f=上方文字, g=下方文字
    zpl += `^BEN,N,${barcodeHeight},N,${showText}^FD${safeContent}^FS`
  } else if (el.barcodeType === 'EAN8') {
    // ^B8o,h,f,g : o=方向N, h=高度, f=显示文本Y/N, g=校验位
    zpl += `^B8N,${barcodeHeight},${showText},N^FD${safeContent}^FS`
  } else {
    // Code128: ^BCo,h,f,g,e,m
    zpl += `^BCN,${barcodeHeight},${showText},N,N^FD${safeContent}^FS`
  }
  return zpl
}

// ─── 二维码 ZPL ───────────────────────
function generateQrcodeZPL(x, y, el, content, dpi) {
  if (!content) return ''
  const safeContent = escapeZpl(content)
  const moduleSize = el.moduleSize || 4
  const ecLevel = { L: 'L', M: 'M', Q: 'Q', H: 'H' }[el.errorCorrectionLevel] || 'M'

  let zpl = `^FO${x},${y}`
  zpl += `^BQN,2,${moduleSize}`
  zpl += `^FD${ecLevel}A,${safeContent}^FS`
  return zpl
}

// ─── 直线 ZPL ─────────────────────────
function generateLineZPL(x, y, el, dpi) {
  const width = mmToDots(el.width || 50, dpi)
  const height = mmToDots(el.height || 0, dpi)
  const thickness = el.strokeWidth || 1

  let zpl = `^FO${x},${y}`
  // ^GB 至少需要 height=1 才能渲染，水平线用 thickness 当高度
  if (height <= 0) {
    zpl += `^GB${width},${thickness},${thickness}^FS`
  } else {
    zpl += `^GB${width},${height},${thickness}^FS`
  }
  return zpl
}

// ─── 矩形 ZPL ─────────────────────────
function generateRectZPL(x, y, el, dpi) {
  const width = mmToDots(el.width || 50, dpi)
  const height = mmToDots(el.height || 30, dpi)
  const borderWidth = el.strokeWidth || 1

  let zpl = `^FO${x},${y}`
  if (el.fill) {
    zpl += `^GB${width},${height},${Math.min(width, height)},B^FS`
  } else {
    zpl += `^GB${width},${height},${borderWidth}^FS`
  }
  return zpl
}

// ─── 主生成函数 ───────────────────────
/**
 * 根据模板和数据生成 ZPL II 指令
 * @param {Object} template - 模板对象 { labelWidth, labelHeight, dpi, elements }
 * @param {Object} dataItem - 数据项（可选，用于替换动态字段）
 * @returns {string} ZPL II 指令字符串
 */
export function generateZPL(template, dataItem = {}) {
  const { labelWidth, labelHeight, dpi = 203, elements = [], offsetX = 0, offsetY = 0 } = template

  const labelWidthDots = mmToDots(labelWidth, dpi)
  const labelHeightDots = mmToDots(labelHeight, dpi)

  let zpl = '^XA\n'
  zpl += `^CI28\n` // UTF-8 编码
  zpl += `^PW${labelWidthDots}\n`
  zpl += `^LL${labelHeightDots}\n`
  zpl += `^PR8,8,8\n` // 打印速度
  zpl += `^MD10\n`    // 浓度

  for (const el of elements) {
    const x = mmToDots((el.x || 0) + offsetX, dpi)
    const y = mmToDots((el.y || 0) + offsetY, dpi)

    switch (el.type) {
      case 'text': {
        const content = resolveContent(el, dataItem)
        zpl += generateTextZPL(x, y, el, content, dpi) + '\n'
        break
      }
      case 'barcode': {
        // 条码元素通过 dataField 绑定数据，不使用 content
        const barcodeData = el.dataField
          ? (dataItem[el.dataField] != null ? String(dataItem[el.dataField]) : '1234567890128')
          : '1234567890128'
        zpl += generateBarcodeZPL(x, y, el, barcodeData, dpi) + '\n'
        break
      }
      case 'qrcode': {
        // 二维码元素通过 dataField 绑定数据
        const qrData = el.dataField
          ? (dataItem[el.dataField] != null ? String(dataItem[el.dataField]) : 'https://example.com')
          : 'https://example.com'
        zpl += generateQrcodeZPL(x, y, el, qrData, dpi) + '\n'
        break
      }
      case 'line':
        zpl += generateLineZPL(x, y, el, dpi) + '\n'
        break
      case 'rect':
        zpl += generateRectZPL(x, y, el, dpi) + '\n'
        break
    }
  }

  zpl += '^XZ\n'
  return zpl
}

/**
 * 批量生成 ZPL（一个订单多条明细）
 */
export function generateBatchZPL(template, dataList) {
  return dataList.map(item => generateZPL(template, item)).join('\n')
}

// ─── 可用的数据字段（与 StickerPrintOrderDetail 实体对齐）────────
export const DATA_FIELDS = [
  { key: 'styleNumber', label: '款号' },
  { key: 'materialName', label: '品名' },
  { key: 'materialNumber', label: '货号' },
  { key: 'brandName', label: '品牌' },
  { key: 'color', label: '颜色' },
  { key: 'sizeName', label: '尺码' },
  { key: 'sizeCode', label: '尺码编码' },
  { key: 'sizeGroup', label: '尺码组' },
  { key: 'ean13', label: 'EAN13条码' },
  { key: 'price', label: '价格' },
  { key: 'printQty', label: '打印数量' },
  { key: 'origin', label: '产地' },
  { key: 'executionStandard', label: '执行标准' },
  { key: 'materialComposition', label: '材质成分' },
  { key: 'manufacturer', label: '生产厂家' },
  { key: 'manufacturerAddress', label: '厂家地址' },
  { key: 'contactPhone', label: '联系电话' },
]
