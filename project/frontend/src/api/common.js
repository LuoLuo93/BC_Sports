import request from '@/api/request'

/**
 * 公共基础数据接口（仅需登录，无业务权限要求）。
 * 品牌/类别/字典/渠道类型/性质/地区等只读基础数据，供各模块下拉框共用，
 * 避免借用其它模块带权限的接口导致 403。
 */

/** ERP 品牌（M_DIM DIM1），返回 [{ID, ATTRIBNAME}] */
export function getCommonBrands() {
  return request.get('/api/common/brands')
}

/** ERP 类别（M_DIM DIM4），返回 [{ID, ATTRIBNAME}] */
export function getCommonKinds() {
  return request.get('/api/common/kinds')
}

/** 字典数据（按 dictType 查询启用项） */
export function getCommonDict(dictType) {
  return request.get('/api/common/dict', { params: { dictType } })
}

/** 本系统品牌列表（下拉用） */
export function getCommonBrandList() {
  return request.get('/api/common/brand/list')
}

/** 渠道类型树（下拉用） */
export function getCommonChannelTypeTree(params) {
  return request.get('/api/common/channel-type/tree', { params })
}

/** 渠道性质树（下拉用） */
export function getCommonChannelNatureTree(params) {
  return request.get('/api/common/channel-nature/tree', { params })
}

/** 地区树（下拉用） */
export function getCommonRegionTree(params) {
  return request.get('/api/common/region/tree', { params })
}
