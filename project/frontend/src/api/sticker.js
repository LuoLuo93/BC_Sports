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

export function searchProducts(params) {
  return request.get('/api/sticker/print/product/search', { params })
}

export function getProductBrands() {
  return request.get('/api/sticker/print/product/brands')
}

export function getProductSizes(productId) {
  return request.get('/api/sticker/print/product/sizes', { params: { productId } })
}

export function getStickerDataPage(params) {
  return request.get('/api/sticker/data/page', { params })
}

export function getStickerBrands() {
  return request.get('/api/sticker/data/brands')
}

// ─── Agent 打印 ──────────────────────
export function createAgentPrintTasks(orderId, agentId) {
  return request.post(`/api/print/create-tasks/${orderId}?agentId=${encodeURIComponent(agentId)}`)
}

// 补打单个任务
export function reprintTask(data) {
  return request.post('/api/print/reprint', data)
}

// ─── Agent 管理 ──────────────────────
export function getAgentPage(params) {
  return request.get('/api/agent/page', { params })
}

export function getAgentList() {
  return request.get('/api/agent/list')
}

export function getAgentTasksPage(agentId, params) {
  return request.get(`/api/agent/${agentId}/tasks/page`, { params })
}

// ─── 打印字段映射 ──────────────────────
export function getFieldMappingPage(params) {
  return request.get('/api/sticker/field-mapping/page', { params })
}

export function getFieldMappingList(templateName) {
  return request.get('/api/sticker/field-mapping/list', { params: { templateName } })
}

export function createFieldMapping(data) {
  return request.post('/api/sticker/field-mapping', data)
}

export function updateFieldMapping(id, data) {
  return request.put(`/api/sticker/field-mapping/${id}`, data)
}

export function deleteFieldMapping(id) {
  return request.delete(`/api/sticker/field-mapping/${id}`)
}

export function deleteFieldMappingByTemplate(templateName) {
  return request.delete(`/api/sticker/field-mapping/by-template/${templateName}`)
}

// ─── 品牌模板关系 ──────────────────────
export function getBrandTemplateMatchPage(params) {
  return request.get('/api/sticker/brand-template/page', { params })
}

export function getBrandTemplateMatchList() {
  return request.get('/api/sticker/brand-template/list')
}

export function getBrandTemplateMatch(id) {
  return request.get(`/api/sticker/brand-template/${id}`)
}

export function createBrandTemplateMatch(data) {
  return request.post('/api/sticker/brand-template', data)
}

export function updateBrandTemplateMatch(id, data) {
  return request.put(`/api/sticker/brand-template/${id}`, data)
}

export function deleteBrandTemplateMatch(id) {
  return request.delete(`/api/sticker/brand-template/${id}`)
}

export function getBrandTemplateKinds() {
  return request.get('/api/sticker/brand-template/kinds')
}

export function getBrandTemplateNames() {
  return request.get('/api/sticker/brand-template/templates')
}

export function getAvailableFields() {
  return request.get('/api/sticker/field-mapping/available-fields')
}

// ─── 本地尺码组维护 ──────────────────────
export function getSizeGroupPage(params) {
  return request.get('/api/sticker/size-group/page', { params })
}

// 按品牌+类别查启用组列表(供明细行下拉)
export function getSizeGroupList(params) {
  return request.get('/api/sticker/size-group/list', { params })
}

// 查某组下尺码明细
export function getSizeGroupSizes(groupId) {
  return request.get(`/api/sticker/size-group/${groupId}/sizes`)
}

export function getSizeGroup(id) {
  return request.get(`/api/sticker/size-group/${id}`)
}

export function createSizeGroup(data) {
  return request.post('/api/sticker/size-group', data)
}

export function updateSizeGroup(id, data) {
  return request.put(`/api/sticker/size-group/${id}`, data)
}

export function deleteSizeGroup(id) {
  return request.delete(`/api/sticker/size-group/${id}`)
}
