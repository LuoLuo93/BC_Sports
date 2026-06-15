import request from '@/api/request'

export function getPrintOrderPage(params) {
  return request.get('/api/sticker/print/page', { params })
}

export function getPrintOrder(orderId) {
  return request.get(`/api/sticker/print/${orderId}`)
}

export function createPrintOrder(data) {
  return request.post('/api/sticker/print', data)
}

export function updatePrintOrder(orderId, data) {
  return request.put(`/api/sticker/print/${orderId}`, data)
}

export function submitPrintOrder(orderId) {
  return request.post(`/api/sticker/print/${orderId}/submit`)
}

export function reviewPrintOrder(orderId, data) {
  return request.post(`/api/sticker/print/${orderId}/review`, data)
}

export function deletePrintOrder(orderId) {
  return request.delete(`/api/sticker/print/${orderId}`)
}

export function bartenderPrint(orderId) {
  return request.post(`/api/sticker/print/${orderId}/bartender-print`)
}

export function zplPrint(zpl, printerIp, printerPort) {
  return request.post('/api/sticker/print/zpl-print', { zpl, printerIp, printerPort })
}

export function getZplPrinters() {
  return request.get('/api/sticker/print/zpl-printers')
}

export function searchProducts(params) {
  return request.get('/api/sticker/print/product/search', { params })
}

export function getProductBrands() {
  return request.get('/api/sticker/print/product/brands')
}

export function getStickerDataPage(params) {
  return request.get('/api/sticker/data/page', { params })
}

export function getStickerBrands() {
  return request.get('/api/sticker/data/brands')
}

// ─── 标签模板管理 ──────────────────────
export function getTemplateList(params) {
  return request.get('/api/sticker/template/list', { params })
}

export function getTemplatePage(params) {
  return request.get('/api/sticker/template/page', { params })
}

export function getTemplate(id) {
  return request.get(`/api/sticker/template/${id}`)
}

export function createTemplate(data) {
  return request.post('/api/sticker/template', data)
}

export function updateTemplate(id, data) {
  return request.put(`/api/sticker/template/${id}`, data)
}

export function deleteTemplate(id) {
  return request.delete(`/api/sticker/template/${id}`)
}

export function setDefaultTemplate(id) {
  return request.post(`/api/sticker/template/${id}/set-default`)
}

export function getDefaultTemplate() {
  return request.get('/api/sticker/template/default')
}

// ─── Agent 打印 ──────────────────────
export function createAgentPrintTasks(orderId, agentId) {
  return request.post(`/api/print/create-tasks/${orderId}?agentId=${encodeURIComponent(agentId)}`)
}
