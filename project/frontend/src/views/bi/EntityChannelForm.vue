<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">{{ isEdit ? '编辑实体渠道配置' : '新增实体渠道配置' }}</span>
          <el-button @click="router.back()">返回列表</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="max-w-720">
        <el-divider content-position="left">实体关联</el-divider>

        <el-form-item label="实体类型" prop="entityType">
          <el-radio-group v-model="form.entityType" :disabled="isEdit" @change="onEntityTypeChange">
            <el-radio value="shop">店铺</el-radio>
            <el-radio value="stock">仓库</el-radio>
            <el-radio value="customer">客户</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.entityType" label="选择实体" prop="externalId">
          <el-select v-model="form.externalId" placeholder="搜索并选择实体" filterable remote :remote-method="searchEntities" :loading="entitySearching" class="w-full" @change="onEntitySelect">
            <el-option v-for="e in entityOptions" :key="e.id" :label="entityOptionLabel(e)" :value="e.id" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.entityName" label="实体名称">
          <el-input :model-value="form.entityName" disabled />
        </el-form-item>

        <el-divider content-position="left">基础属性</el-divider>

        <el-form-item label="所属品牌">
          <el-select v-model="form.brandId" placeholder="请选择品牌" clearable class="w-full">
            <el-option v-for="b in brandList" :key="b.id" :label="b.brandName" :value="b.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider content-position="left">渠道配置</el-divider>

        <el-form-item label="渠道类型">
          <el-tree-select v-model="form.channelTypeId" :data="channelTypeTree" :props="{ label: 'typeName', value: 'id', children: 'children' }" placeholder="请选择渠道类型" check-strictly clearable class="w-full" />
        </el-form-item>

        <el-form-item label="渠道性质">
          <el-tree-select v-model="form.channelNatureId" :data="channelNatureTree" :props="{ label: 'natureName', value: 'id', children: 'children' }" placeholder="请选择渠道性质" check-strictly clearable class="w-full" />
        </el-form-item>

        <el-divider content-position="left">所属地区</el-divider>

        <el-form-item label="一级地区">
          <el-tree-select v-model="form.regionLevel1Id" :data="regionTree" :props="{ label: 'regionName', value: 'id', children: 'children' }" placeholder="请选择一级地区" check-strictly clearable class="w-full" @change="form.regionLevel2Id = ''" />
        </el-form-item>

        <el-form-item label="二级地区">
          <el-tree-select v-model="form.regionLevel2Id" :data="level2Regions" :props="{ label: 'regionName', value: 'id', children: 'children' }" placeholder="请先选择一级地区" check-strictly clearable class="w-full" :disabled="!form.regionLevel1Id" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'EntityChannelForm' })
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getEntityChannel, createEntityChannel, updateEntityChannel } from '@/api/channel'
import { getChannelTypeTree, getChannelNatureTree } from '@/api/channel'
import { getBrandList } from '@/api/brand'
import { getRegionTree } from '@/api/region'
import { getErpShopPage, getErpShop, getErpWarehousePage, getErpWarehouse, getErpCustomerPage, getErpCustomer } from '@/api/erp'
import { PAGE_SIZES_LG } from '@/utils/constants'

const router = useRouter()
const route = useRoute()
const isEdit = !!route.query.id
const formRef = ref(null)
const submitting = ref(false)

const defaultForm = () => ({
  entityType: '', externalId: '', entityName: '', brandId: '',
  channelTypeId: '', channelNatureId: '', regionLevel1Id: '', regionLevel2Id: '',
  sort: 0, status: 1
})
const form = reactive(defaultForm())
const rules = {
  entityType: [{ required: true, message: '请选择实体类型', trigger: 'change' }],
  externalId: [{ required: true, message: '请选择实体', trigger: 'change' }]
}

// 下拉数据
const brandList = ref([])
const channelTypeTree = ref([])
const channelNatureTree = ref([])
const regionTree = ref([])
const entityOptions = ref([])
const entitySearching = ref(false)

const level2Regions = computed(() => {
  if (!form.regionLevel1Id || !regionTree.value.length) return []
  const findChildren = (nodes) => {
    for (const n of nodes) {
      if (n.id === form.regionLevel1Id) return n.children || []
      if (n.children?.length) { const found = findChildren(n.children); if (found.length) return found }
    }
    return []
  }
  return findChildren(regionTree.value)
})

function entityOptionLabel(e) {
  const code = e.shopCode || e.warehouseCode || e.customerCode || ''
  const name = e.shopName || e.warehouseName || e.customerName || ''
  return code ? `${code} - ${name}` : name
}

async function searchEntities(keyword) {
  if (!keyword || !form.entityType) return
  entitySearching.value = true
  try {
    const params = { pageNum: 1, pageSize: PAGE_SIZES_LG[0] }
    let res
    if (form.entityType === 'shop') {
      params.shopName = keyword
      res = await getErpShopPage(params)
      entityOptions.value = (res.data?.records || []).map(r => ({ ...r, id: r.id }))
    } else if (form.entityType === 'stock') {
      params.warehouseName = keyword
      res = await getErpWarehousePage(params)
      entityOptions.value = (res.data?.records || []).map(r => ({ ...r, id: r.id }))
    } else {
      params.customerName = keyword
      res = await getErpCustomerPage(params)
      entityOptions.value = (res.data?.records || []).map(r => ({ ...r, id: r.id }))
    }
  } finally { entitySearching.value = false }
}

function onEntityTypeChange() {
  form.externalId = ''
  form.entityName = ''
  entityOptions.value = []
}

function onEntitySelect(id) {
  const entity = entityOptions.value.find(e => e.id === id)
  if (entity) {
    form.entityName = entity.shopName || entity.warehouseName || entity.customerName || ''
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit) {
      await updateEntityChannel(route.query.id, { ...form })
    } else {
      await createEntityChannel({ ...form })
    }
    ElMessage.success(isEdit ? '更新成功' : '创建成功')
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

  if (isEdit) {
    const res = await getEntityChannel(route.query.id)
    Object.assign(form, res.data)
    // 编辑模式下填充 entityOptions 让下拉显示名称而非原始 ID
    if (form.entityType === 'shop' && form.externalId) {
      const e = await getErpShop(form.externalId)
      if (e.data) entityOptions.value = [e.data]
    } else if (form.entityType === 'stock' && form.externalId) {
      const e = await getErpWarehouse(form.externalId)
      if (e.data) entityOptions.value = [e.data]
    } else if (form.entityType === 'customer' && form.externalId) {
      const e = await getErpCustomer(form.externalId)
      if (e.data) entityOptions.value = [e.data]
    }
  }
})
</script>
