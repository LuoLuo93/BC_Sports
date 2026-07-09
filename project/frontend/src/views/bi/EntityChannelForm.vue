<template>
  <div class="page-container">
    <div class="form-view">
      <!-- 顶部操作栏 -->
      <div class="form-header">
        <el-button type="warning" size="small" @click="handleBack">返回列表</el-button>
        <span class="form-header-title">{{ isEdit ? `编辑 - ${editEntityName} (${editExternalId})` : '新建实体渠道配置' }}</span>
        <el-button type="primary" size="small" :loading="submitting" style="width:80px" @click="handleSubmit">保存</el-button>
      </div>

      <!-- 主体区域 -->
      <div class="form-body">
        <!-- 上方：实体搜索 -->
        <div class="search-panel">
          <div class="search-bar">
            <span class="panel-bar-title">实体搜索</span>
            <el-radio-group v-model="searchType" size="small" :disabled="isEdit" @change="onSearchTypeChange">
              <el-radio-button value="store">店仓</el-radio-button>
              <el-radio-button value="customer">客户</el-radio-button>
            </el-radio-group>
            <el-input v-model="searchKeyword" placeholder="输入编码或名称搜索" size="small" clearable :disabled="isEdit" style="min-width:200px;max-width:240px" @keyup.enter="searchEntities" />
            <el-button type="primary" size="small" :loading="entitySearching" :disabled="isEdit" style="width:80px" @click="searchEntities">搜索</el-button>
            <el-button type="success" size="small" :disabled="!selectedEntities.length" @click="confirmEntitySelect">
              添加({{ selectedEntities.length }})
            </el-button>
          </div>
          <el-table ref="entityTableRef" v-loading="entitySearching" :data="entityList" border size="small" @selection-change="handleEntitySelect" @row-click="handleRowClick" highlight-current-row height="100%">
            <el-table-column type="selection" width="35" fixed="left" />
            <el-table-column type="index" label="#" width="45" fixed="left" />
            <el-table-column prop="CODE" label="编码" width="180" show-overflow-tooltip fixed="left" class-name="col-key" />
            <el-table-column prop="NAME" label="名称" min-width="200" show-overflow-tooltip fixed="left" class-name="col-key" />
          </el-table>
        </div>

        <!-- 下方：已选实体明细 -->
        <div class="detail-panel">
          <div class="panel-bar">
            <span class="panel-bar-title">已选实体明细 <span class="detail-summary">共 {{ selectedDetail.length }} 条</span></span>
            <div style="display:flex;gap:6px;align-items:center">
              <el-input v-model="detailKeyword" placeholder="搜索编码/名称" size="small" clearable style="min-width:160px;max-width:200px" />
              <el-button type="danger" size="small" :disabled="!detailSelection.length" @click="handleBatchDelete">批量删除</el-button>
            </div>
          </div>
          <el-table :data="filteredDetails" border size="small" @selection-change="handleDetailSelect" height="100%">
            <el-table-column type="selection" width="35" fixed="left" />
            <el-table-column label="#" width="45" fixed="left">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="entityType" label="类型" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.entityType === 'store' ? 'success' : 'warning'" size="small">{{ row.entityType === 'store' ? '店仓' : '客户' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="externalId" label="编码" min-width="100" class-name="col-key" />
            <el-table-column prop="entityName" label="名称" min-width="120" show-overflow-tooltip />
            <el-table-column label="品牌" min-width="100">
              <template #default="{ row }">
                <el-select v-model="row.brandId" placeholder="选择" size="small" clearable filterable style="width:100%">
                  <el-option v-for="b in brandList" :key="b.id" :label="b.brandName" :value="b.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="一级地区" min-width="100">
              <template #default="{ row }">
                <el-tree-select v-model="row.regionLevel1Id" :data="regionTree" :props="{ label: 'regionName', value: 'id', children: 'children' }" placeholder="选择" size="small" check-strictly clearable style="width:100%" @change="(val) => { validateParentSelect(val, regionTree, row, 'regionLevel1Id'); row.regionLevel2Id = '' }" />
              </template>
            </el-table-column>
            <el-table-column label="二级地区" min-width="100">
              <template #default="{ row }">
                <el-tree-select v-model="row.regionLevel2Id" :data="getLevel2Regions(row.regionLevel1Id)" :props="{ label: 'regionName', value: 'id', children: 'children' }" placeholder="选择" size="small" check-strictly clearable style="width:100%" :disabled="!row.regionLevel1Id" @change="(val) => { if (val && hasChildren(getLevel2Regions(row.regionLevel1Id), val)) { row.regionLevel2Id = ''; ElMessage.warning('请选择子节点') } }" />
              </template>
            </el-table-column>
            <el-table-column label="渠道类型" min-width="100">
              <template #default="{ row }">
                <el-tree-select v-model="row.channelTypeId" :data="channelTypeTree" :props="{ label: 'typeName', value: 'id', children: 'children' }" placeholder="选择" size="small" check-strictly clearable style="width:100%" @change="(val) => { validateParentSelect(val, channelTypeTree, row, 'channelTypeId'); row.channelDefId = '' }" />
              </template>
            </el-table-column>
            <el-table-column label="渠道定义" min-width="100">
              <template #default="{ row }">
                <el-tree-select v-model="row.channelDefId" :data="getChannelDefs(row.channelTypeId)" :props="{ label: 'typeName', value: 'id', children: 'children' }" placeholder="选择" size="small" check-strictly clearable style="width:100%" :disabled="!row.channelTypeId" @change="(val) => { if (val && hasChildren(getChannelDefs(row.channelTypeId), val)) { row.channelDefId = ''; ElMessage.warning('请选择子节点') } }" />
              </template>
            </el-table-column>
            <el-table-column label="渠道性质" min-width="100">
              <template #default="{ row }">
                <el-tree-select v-model="row.channelNatureId" :data="channelNatureTree" :props="{ label: 'natureName', value: 'id', children: 'children' }" placeholder="选择" size="small" check-strictly clearable style="width:100%" @change="(val) => { validateParentSelect(val, channelNatureTree, row, 'channelNatureId'); row.businessTypeId = '' }" />
              </template>
            </el-table-column>
            <el-table-column label="经营类型" min-width="100">
              <template #default="{ row }">
                <el-tree-select v-model="row.businessTypeId" :data="getBusinessTypes(row.channelNatureId)" :props="{ label: 'natureName', value: 'id', children: 'children' }" placeholder="选择" size="small" check-strictly clearable style="width:100%" :disabled="!row.channelNatureId" @change="(val) => { if (val && hasChildren(getBusinessTypes(row.channelNatureId), val)) { row.businessTypeId = ''; ElMessage.warning('请选择子节点') } }" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="danger" plain size="small" @click="handleSingleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'EntityChannelForm' })
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEntityChannel, getEntityChannelListByEntity, batchSaveEntityChannel, createEntityChannel } from '@/api/channel'
import { getChannelTypeTree, getChannelNatureTree } from '@/api/channel'
import { getBrandList } from '@/api/brand'
import { getRegionTree } from '@/api/region'
import { getErpStorePage, getErpCustomerPage } from '@/api/erp'
import { PAGE_SIZES } from '@/utils/appConfig'

const router = useRouter()
const route = useRoute()
const submitting = ref(false)
const editEntityName = ref('')

const editId = computed(() => route.query.id)
const editExternalId = ref(route.query.externalId || '')
const editEntityType = ref(route.query.entityType || '')
const isEdit = computed(() => !!(editId.value || (editExternalId.value && editEntityType.value)))

// 搜索区域
const searchType = ref('store')
const searchKeyword = ref('')
const entitySearching = ref(false)
const entityList = ref([])
const selectedEntities = ref([])
const entityTableRef = ref(null)

// 已选明细
const selectedDetail = ref([])
const detailKeyword = ref('')
const detailSelection = ref([])

// 下拉数据
const brandList = ref([])
const channelTypeTree = ref([])
const channelNatureTree = ref([])
const regionTree = ref([])

const filteredDetails = computed(() => {
  if (!detailKeyword.value) return selectedDetail.value
  const kw = detailKeyword.value.toLowerCase()
  return selectedDetail.value.filter(r =>
    (r.externalId && r.externalId.toLowerCase().includes(kw)) ||
    (r.entityName && r.entityName.toLowerCase().includes(kw))
  )
})

function getLevel2Regions(level1Id) {
  if (!level1Id || !regionTree.value.length) return []
  const findChildren = (nodes) => {
    for (const n of nodes) {
      if (n.id === level1Id) return n.children || []
      if (n.children?.length) { const found = findChildren(n.children); if (found.length) return found }
    }
    return []
  }
  return findChildren(regionTree.value)
}

function getChannelDefs(channelTypeId) {
  if (!channelTypeId || !channelTypeTree.value.length) return []
  const findChildren = (nodes) => {
    for (const n of nodes) {
      if (n.id === channelTypeId) return n.children || []
      if (n.children?.length) { const found = findChildren(n.children); if (found.length) return found }
    }
    return []
  }
  return findChildren(channelTypeTree.value)
}

function getBusinessTypes(channelNatureId) {
  if (!channelNatureId || !channelNatureTree.value.length) return []
  const findChildren = (nodes) => {
    for (const n of nodes) {
      if (n.id === channelNatureId) return n.children || []
      if (n.children?.length) { const found = findChildren(n.children); if (found.length) return found }
    }
    return []
  }
  return findChildren(channelNatureTree.value)
}

// 判断节点是否有子节点
function hasChildren(treeData, nodeId) {
  const find = (nodes) => {
    for (const n of nodes) {
      if (n.id === nodeId) return !!(n.children && n.children.length > 0)
      if (n.children?.length) { const found = find(n.children); if (found !== undefined) return found }
    }
    return undefined
  }
  return find(treeData) || false
}

// 祖节点选择验证：只允许选有子节点的
function validateParentSelect(val, treeData, row, field) {
  if (val && !hasChildren(treeData, val)) {
    row[field] = ''
    ElMessage.warning('请选择父节点')
  }
}

// 孙节点选择验证：只允许选叶子节点
function validateChildSelect(val, treeData, row, field) {
  if (val && hasChildren(treeData, val)) {
    row[field] = ''
    ElMessage.warning('请选择子节点')
  }
}

function onSearchTypeChange() {
  searchKeyword.value = ''
  entityList.value = []
  selectedEntities.value = []
}

async function searchEntities() {
  entitySearching.value = true
  try {
    const params = { pageNum: 1, pageSize: PAGE_SIZES[0] }
    if (searchKeyword.value) {
      params.code = searchKeyword.value
      params.name = searchKeyword.value
    }
    const res = searchType.value === 'store'
      ? await getErpStorePage(params)
      : await getErpCustomerPage(params)
    entityList.value = res.data?.records || []
  } finally { entitySearching.value = false }
}

function handleEntitySelect(rows) {
  selectedEntities.value = rows
}

function handleRowClick(row) {
  entityTableRef.value?.toggleRowSelection(row)
}

function confirmEntitySelect() {
  let added = 0
  for (const entity of selectedEntities.value) {
    // 编辑模式下只能添加同一个实体
    if (isEdit.value && (entity.CODE !== editExternalId.value || searchType.value !== editEntityType.value)) {
      ElMessage.warning('编辑模式下只能添加同一个实体的渠道配置')
      continue
    }
    // 同一个实体可以添加多行，每次都是新行（渠道属性不同）
    selectedDetail.value.push({
      entityType: searchType.value,
      externalId: entity.CODE,
      entityName: entity.NAME,
      id: '',
      brandId: '',
      regionLevel1Id: '',
      regionLevel2Id: '',
      channelTypeId: '',
      channelDefId: '',
      channelNatureId: '',
      businessTypeId: '',
      sort: 0,
      status: 1
    })
    added++
  }
  if (added > 0) ElMessage.success(`已添加 ${added} 条`)
}

function handleDetailSelect(rows) {
  detailSelection.value = rows
}

function handleBatchDelete() {
  const selectedSet = new Set(detailSelection.value)
  selectedDetail.value = selectedDetail.value.filter(r => !selectedSet.has(r))
}

function handleSingleDelete(row) {
  const index = selectedDetail.value.indexOf(row)
  if (index > -1) selectedDetail.value.splice(index, 1)
}

function handleBack() {
  if (selectedDetail.value.length > 0) {
    ElMessageBox.confirm('有未保存的数据，确定返回吗？', '提示', {
      confirmButtonText: '确定返回',
      cancelButtonText: '继续编辑',
      type: 'warning'
    }).then(() => {
      router.push('/bi/entity-channel')
    }).catch(() => {})
  } else {
    router.push('/bi/entity-channel')
  }
}

async function handleSubmit() {
  if (!selectedDetail.value.length) {
    ElMessage.warning('请至少添加一个实体')
    return
  }
  // 校验必填项
  for (let i = 0; i < selectedDetail.value.length; i++) {
    const row = selectedDetail.value[i]
    const label = `${row.entityName || row.externalId}(${i + 1})`
    if (!row.brandId) {
      ElMessage.warning(`${label} 请选择品牌`)
      return
    }
    if (!row.regionLevel1Id) {
      ElMessage.warning(`${label} 请选择一级地区`)
      return
    }
    if (!row.regionLevel2Id) {
      ElMessage.warning(`${label} 请选择二级地区`)
      return
    }
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      // 编辑模式：全量替换（加载了旧数据，删除/修改/新增都在 batchSave 中处理）
      await batchSaveEntityChannel(editExternalId.value, editEntityType.value, selectedDetail.value)
      ElMessage.success('保存成功')
    } else {
      // 新增模式：逐条插入，不动历史数据；重复的跳过并留在明细中
      let successCount = 0
      const failedItems = []
      for (const item of selectedDetail.value) {
        try {
          await createEntityChannel(item)
          successCount++
        } catch {
          failedItems.push(item)
        }
      }
      if (failedItems.length === 0) {
        ElMessage.success(`${successCount} 条全部新增成功`)
      } else if (successCount === 0) {
        ElMessage.error('全部新增失败，数据可能已存在')
      } else {
        ElMessage.warning(`${successCount} 条新增成功，${failedItems.length} 条已存在被跳过`)
      }
      // 失败的留在明细中供用户调整，全部成功才跳转
      if (failedItems.length > 0) {
        selectedDetail.value = failedItems
        return
      }
    }
    router.push('/bi/entity-channel')
  } finally { submitting.value = false }
}

onMounted(async () => {
  const [brands, ctTree, cnTree, regions] = await Promise.all([
    getBrandList(), getChannelTypeTree(), getChannelNatureTree(), getRegionTree()
  ])
  brandList.value = brands.data || []
  channelTypeTree.value = ctTree.data || []
  channelNatureTree.value = cnTree.data || []
  regionTree.value = regions.data || []

  if (isEdit.value) {
    let extId = editExternalId.value
    let entType = editEntityType.value

    // 如果是按ID编辑，先获取单条记录拿到实体信息
    if (editId.value) {
      const singleRes = await getEntityChannel(editId.value)
      if (singleRes.data) {
        extId = singleRes.data.externalId
        entType = singleRes.data.entityType
        editEntityName.value = singleRes.data.entityName || ''
        editExternalId.value = extId
        editEntityType.value = entType
      }
    }

    // 加载该实体的所有配置
    if (extId && entType) {
      const res = await getEntityChannelListByEntity(extId, entType)
      if (res.data) {
        selectedDetail.value = res.data
        if (res.data.length > 0 && !editEntityName.value) {
          editEntityName.value = res.data[0].entityName || ''
        }
      }
      // 搜索区类型自动匹配
      searchType.value = entType
      // 自动搜索当前实体
      searchKeyword.value = extId
      await searchEntities()
      // 自动选中搜索结果中的当前实体
      nextTick(() => {
        const match = entityList.value.find(e => e.CODE === extId)
        if (match && entityTableRef.value) {
          entityTableRef.value.toggleRowSelection(match, true)
        }
      })
    }
  }
})

// 监听路由变化，处理新建/编辑切换
watch(() => route.fullPath, async () => {
  const id = route.query.id
  const extId = route.query.externalId
  const entType = route.query.entityType

  // 重置状态
  selectedDetail.value = []
  selectedEntities.value = []
  entityList.value = []
  searchKeyword.value = ''
  editEntityName.value = ''
  editExternalId.value = route.query.externalId || ''
  editEntityType.value = route.query.entityType || ''

  let finalExtId = extId
  let finalEntType = entType

  if (id) {
    // 按ID加载单条记录，获取实体信息
    const singleRes = await getEntityChannel(id)
    if (singleRes.data) {
      finalExtId = singleRes.data.externalId
      finalEntType = singleRes.data.entityType
      editEntityName.value = singleRes.data.entityName || ''
      editExternalId.value = finalExtId
      editEntityType.value = finalEntType
    }
  }

  if (finalExtId && finalEntType) {
    // 加载该实体的所有配置
    const res = await getEntityChannelListByEntity(finalExtId, finalEntType)
    if (res.data) {
      selectedDetail.value = res.data
      if (res.data.length > 0 && !editEntityName.value) {
        editEntityName.value = res.data[0].entityName || ''
      }
    }
    // 搜索区类型自动匹配
    searchType.value = finalEntType
    // 自动搜索当前实体
    searchKeyword.value = finalExtId
    await searchEntities()
    // 自动选中搜索结果中的当前实体
    nextTick(() => {
      const match = entityList.value.find(e => e.CODE === finalExtId)
      if (match && entityTableRef.value) {
        entityTableRef.value.toggleRowSelection(match, true)
      }
    })
  } else {
    searchType.value = 'store'
  }
})
</script>

<style scoped>
.form-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  margin: -16px;
  background: #f0f2f5;
  overflow: hidden;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.form-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.form-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 12px 16px;
  overflow: hidden;
}

.search-panel {
  height: 38%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid #f3f4f6;
  flex-shrink: 0;
}

.panel-bar-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  white-space: nowrap;
}

.detail-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  margin-top: 12px;
}

.panel-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f3f4f6;
  flex-shrink: 0;
}

.detail-summary {
  font-weight: 400;
  color: #6b7280;
  margin-left: 8px;
  font-size: 12px;
}

:deep(.col-key) {
  font-weight: 600;
}

/* 明细表格内联编辑组件样式 */
.detail-panel :deep(.el-table) {
  font-size: 12px;
}
.detail-panel :deep(.el-table .cell) {
  padding: 0 4px;
  overflow: visible;
}
.detail-panel :deep(.el-table td) {
  padding: 2px 0;
}
.detail-panel :deep(.el-select),
.detail-panel :deep(.el-tree-select) {
  width: 100% !important;
  min-width: 0;
}
.detail-panel :deep(.el-select .el-input),
.detail-panel :deep(.el-tree-select .el-input) {
  width: 100% !important;
}
.detail-panel :deep(.el-select .el-input__wrapper),
.detail-panel :deep(.el-tree-select .el-input__wrapper) {
  padding: 0 6px;
  box-shadow: none !important;
  border: 1px solid transparent;
  border-radius: 4px;
}
.detail-panel :deep(.el-select .el-input__wrapper:hover),
.detail-panel :deep(.el-tree-select .el-input__wrapper:hover) {
  border-color: var(--el-border-color);
}
.detail-panel :deep(.el-select .el-input__wrapper.is-focus),
.detail-panel :deep(.el-tree-select .el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-8) !important;
}
.detail-panel :deep(.el-select .el-input__inner),
.detail-panel :deep(.el-tree-select .el-input__inner) {
  height: 24px;
  line-height: 24px;
  font-size: 12px;
}
</style>
