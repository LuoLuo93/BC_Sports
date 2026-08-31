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

export function getStickerKinds() {
  return request.get('/api/sticker/data/kinds')
}

// 按货号查货品详情（贴纸资料详情页用）
export function getStickerDataDetail(materialNumber) {
  return request.get(`/api/sticker/data/${encodeURIComponent(materialNumber)}`)
}

// 保存货品材质字段（面料编码/面料成分/辅料编码/辅料成分），写回 ERP
export function updateStickerDataMaterial(data) {
  return request.put('/api/sticker/data/material', data)
}

// ─── 贴纸资料批量导入 ──────────────────────
// 按货号更新执行标准/EAN13/安全类别/材质；FormData 上传，大批量导入超时放宽到 10 分钟
export function importStickerData(formData) {
  return request.post('/api/sticker/data/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000
  })
}

// 下载导入模板（blob，拦截器已直接返回 blob 数据）
export function downloadStickerDataTemplate() {
  return request.get('/api/sticker/data/template', { responseType: 'blob' })
}

// 导入日志分页
export function getStickerDataImportLogPage(params) {
  return request.get('/api/sticker/data/import-log/page', { params })
}

// ─── Agent 打印 ──────────────────────
// force=true: 该单已有未完成任务时先批量取消再重新生成(强制重新下发)
export function createAgentPrintTasks(orderId, agentId, force = false) {
  return request.post(`/api/print/create-tasks/${orderId}?agentId=${encodeURIComponent(agentId)}${force ? '&force=true' : ''}`)
}

// 统计申请单未完成任务数(待打印/打印中/已暂停)——下发前轻量预检查,不拉任务实体(大单含CLOB会很重)
export function getOrderPrintPendingSummary(orderId) {
  return request.get(`/api/print/tasks/${orderId}/pending-summary`)
}

// 手动取消单个任务(仅待打印/打印中/已暂停)
export function cancelPrintTask(data) {
  return request.post('/api/print/cancel', data)
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

// ─── 打印字段映射（全局单套，不再按模板绑定） ──────────────────────
export function getFieldMappingPage(params) {
  return request.get('/api/sticker/field-mapping/page', { params })
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

// ─── 矫正尺码组维护 ──────────────────────
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
