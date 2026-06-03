<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="bi-mgmt-tabs">
      <!-- 品牌管理 -->
      <el-tab-pane label="品牌管理" name="brand">
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">品牌列表</span>
              <el-button v-if="hasPermission('bi:brand:add')" type="primary" size="small" :icon="Plus" @click="handleBrandAdd">新增品牌</el-button>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="brandLoading" :data="brandList" border stripe empty-text="暂无数据">
              <el-table-column label="品牌名称" min-width="160">
                <template #default="{ row }">
                  <div class="name-cell"><el-icon class="icon-brand" :size="15"><PriceTag /></el-icon><span>{{ row.brandName }}</span></div>
                </template>
              </el-table-column>
              <el-table-column prop="brandCode" label="品牌编码" min-width="140" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center">
                <template #default="{ row }">
                  <el-button v-if="hasPermission('bi:brand:edit')" type="primary" plain size="small" @click="handleBrandEdit(row)">编辑</el-button>
                  <el-button v-if="hasPermission('bi:brand:delete')" type="danger" plain size="small" @click="handleBrandDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="brandQuery.pageNum" v-model:page-size="brandQuery.pageSize" :total="brandTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next" @size-change="() => { brandQuery.pageNum = 1; loadBrands() }" @current-change="loadBrands" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 地区管理 -->
      <el-tab-pane label="地区管理" name="region" lazy>
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">地区列表</span>
              <div>
                <el-button size="small" @click="toggleExpandAll">{{ isExpandAll ? '折叠全部' : '展开全部' }}</el-button>
                <el-button v-if="hasPermission('bi:region:add')" type="primary" size="small" :icon="Plus" @click="handleRegionAdd()">新增地区</el-button>
              </div>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-if="refreshTable" v-loading="regionLoading" :data="regionList" row-key="id" :tree-props="{ children: 'children' }" :default-expand-all="isExpandAll" border stripe :row-class-name="treeRowClass">
              <el-table-column label="地区名称" min-width="240">
                <template #default="{ row }">
                  <div class="name-cell">
                    <el-icon v-if="row.children?.length" class="icon-folder" :size="16"><Folder /></el-icon>
                    <el-icon v-else class="icon-doc" :size="14"><Document /></el-icon>
                    <span>{{ row.regionName }}</span>
                    <el-tag v-if="row.children?.length" size="small" type="primary" effect="plain" class="level-tag">一级地区</el-tag>
                    <el-tag v-else size="small" type="info" effect="plain" class="level-tag">二级地区</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="regionCode" label="地区编码" min-width="140" />
              <el-table-column prop="sort" label="排序" width="80" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280" align="right" header-align="right">
                <template #default="{ row }">
                  <el-button v-if="row.children?.length && hasPermission('bi:region:add')" type="warning" plain size="small" @click="handleRegionAdd(row)">新增子地区</el-button>
                  <el-button v-if="hasPermission('bi:region:edit')" type="primary" plain size="small" @click="handleRegionEdit(row)">编辑</el-button>
                  <el-button v-if="hasPermission('bi:region:delete')" type="danger" plain size="small" @click="handleRegionDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 渠道类型 -->
      <el-tab-pane label="渠道类型" name="channelType" lazy>
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">渠道类型</span>
              <div>
                <el-button size="small" @click="toggleExpandAll">{{ isExpandAll ? '折叠全部' : '展开全部' }}</el-button>
                <el-button v-if="hasPermission('bi:channelType:add')" type="primary" size="small" :icon="Plus" @click="handleCtAdd()">新增类型</el-button>
              </div>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-if="refreshTable" v-loading="ctLoading" :data="ctList" row-key="id" :tree-props="{ children: 'children' }" :row-class-name="treeRowClass" :default-expand-all="isExpandAll" border stripe>
              <el-table-column label="类型名称" min-width="240">
                <template #default="{ row }">
                  <div class="name-cell">
                    <el-icon v-if="row.children?.length" class="icon-folder" :size="16"><Folder /></el-icon>
                    <el-icon v-else class="icon-doc" :size="14"><Document /></el-icon>
                    <span>{{ row.typeName }}</span>
                    <el-tag v-if="row.children?.length" size="small" type="primary" effect="plain" class="level-tag">渠道类型</el-tag>
                    <el-tag v-else size="small" type="info" effect="plain" class="level-tag">渠道定义</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="sort" label="排序" width="80" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280" align="right" header-align="right">
                <template #default="{ row }">
                  <el-button v-if="row.children?.length && hasPermission('bi:channelType:add')" type="warning" plain size="small" @click="handleCtAdd(row)">新增子类型</el-button>
                  <el-button v-if="hasPermission('bi:channelType:edit')" type="primary" plain size="small" @click="handleCtEdit(row)">编辑</el-button>
                  <el-button v-if="hasPermission('bi:channelType:delete')" type="danger" plain size="small" @click="handleCtDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 渠道性质 -->
      <el-tab-pane label="渠道性质" name="channelNature" lazy>
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">渠道性质</span>
              <div>
                <el-button size="small" @click="toggleExpandAll">{{ isExpandAll ? '折叠全部' : '展开全部' }}</el-button>
                <el-button v-if="hasPermission('bi:channelNature:add')" type="primary" size="small" :icon="Plus" @click="handleCnAdd()">新增性质</el-button>
              </div>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-if="refreshTable" v-loading="cnLoading" :data="cnList" row-key="id" :tree-props="{ children: 'children' }" :row-class-name="treeRowClass" :default-expand-all="isExpandAll" border stripe>
              <el-table-column label="性质名称" min-width="240">
                <template #default="{ row }">
                  <div class="name-cell">
                    <el-icon v-if="row.children?.length" class="icon-folder" :size="16"><Folder /></el-icon>
                    <el-icon v-else class="icon-doc" :size="14"><Document /></el-icon>
                    <span>{{ row.natureName }}</span>
                    <el-tag v-if="row.children?.length" size="small" type="primary" effect="plain" class="level-tag">渠道性质</el-tag>
                    <el-tag v-else size="small" type="info" effect="plain" class="level-tag">销售类型</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="sort" label="排序" width="80" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280" align="right" header-align="right">
                <template #default="{ row }">
                  <el-button v-if="row.children?.length && hasPermission('bi:channelNature:add')" type="warning" plain size="small" @click="handleCnAdd(row)">新增子性质</el-button>
                  <el-button v-if="hasPermission('bi:channelNature:edit')" type="primary" plain size="small" @click="handleCnEdit(row)">编辑</el-button>
                  <el-button v-if="hasPermission('bi:channelNature:delete')" type="danger" plain size="small" @click="handleCnDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 品牌弹窗 -->
    <el-dialog v-model="brandDialogVisible" :title="brandIsEdit ? '编辑品牌' : '新增品牌'" width="480px" destroy-on-close>
      <el-form ref="brandFormRef" :model="brandForm" :rules="brandRules" label-width="100px">
        <el-form-item label="品牌名称" prop="brandName"><el-input v-model="brandForm.brandName" placeholder="请输入品牌名称" /></el-form-item>
        <el-form-item label="品牌编码" prop="brandCode"><el-input v-model="brandForm.brandCode" placeholder="请输入品牌编码" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="brandForm.status"><el-radio :value="1">正常</el-radio><el-radio :value="0">停用</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="brandForm.remark" type="textarea" :rows="3" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="brandDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="brandSubmitting" @click="handleBrandSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 地区弹窗 -->
    <el-dialog v-model="regionDialogVisible" :title="regionIsEdit ? '编辑地区' : '新增地区'" width="480px" destroy-on-close>
      <el-form ref="regionFormRef" :model="regionForm" :rules="regionRules" label-width="100px">
        <el-form-item label="上级地区">
          <el-tree-select v-model="regionForm.parentId" :data="regionTreeOpts" :props="{ label: 'regionName', value: 'id', children: 'children' }" placeholder="顶级地区" check-strictly clearable class="w-full" />
        </el-form-item>
        <el-form-item label="地区名称" prop="regionName"><el-input v-model="regionForm.regionName" placeholder="请输入地区名称" /></el-form-item>
        <el-form-item label="地区编码" prop="regionCode"><el-input v-model="regionForm.regionCode" placeholder="请输入地区编码" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="regionForm.sort" :min="0" :max="9999" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="regionForm.status"><el-radio :value="1">正常</el-radio><el-radio :value="0">停用</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="regionDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="regionSubmitting" @click="handleRegionSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 渠道类型弹窗 -->
    <el-dialog v-model="ctDialogVisible" :title="ctIsEdit ? '编辑渠道类型' : '新增渠道类型'" width="480px" destroy-on-close>
      <el-form ref="ctFormRef" :model="ctForm" :rules="ctRules" label-width="100px">
        <el-form-item label="上级类型">
          <el-tree-select v-model="ctForm.parentId" :data="ctTreeOpts" :props="{ label: 'typeName', value: 'id', children: 'children' }" placeholder="顶级类型" check-strictly clearable class="w-full" />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName"><el-input v-model="ctForm.typeName" placeholder="请输入类型名称" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="ctForm.sort" :min="0" :max="9999" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="ctForm.status"><el-radio :value="1">正常</el-radio><el-radio :value="0">停用</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="ctDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="ctSubmitting" @click="handleCtSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 渠道性质弹窗 -->
    <el-dialog v-model="cnDialogVisible" :title="cnIsEdit ? '编辑渠道性质' : '新增渠道性质'" width="480px" destroy-on-close>
      <el-form ref="cnFormRef" :model="cnForm" :rules="cnRules" label-width="100px">
        <el-form-item label="上级性质">
          <el-tree-select v-model="cnForm.parentId" :data="cnTreeOpts" :props="{ label: 'natureName', value: 'id', children: 'children' }" placeholder="顶级性质" check-strictly clearable class="w-full" />
        </el-form-item>
        <el-form-item label="性质名称" prop="natureName"><el-input v-model="cnForm.natureName" placeholder="请输入性质名称" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="cnForm.sort" :min="0" :max="9999" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="cnForm.status"><el-radio :value="1">正常</el-radio><el-radio :value="0">停用</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="cnDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="cnSubmitting" @click="handleCnSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'BiManagement' })
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBrandPage, getBrand, createBrand, updateBrand, deleteBrand } from '@/api/brand'
import { getRegionTree, getRegion, createRegion, updateRegion, deleteRegion } from '@/api/region'
import { getChannelTypeTree, getChannelType, createChannelType, updateChannelType, deleteChannelType, getChannelNatureTree, getChannelNature, createChannelNature, updateChannelNature, deleteChannelNature } from '@/api/channel'
import { Plus, Folder, Document, PriceTag } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/appConfig'

const { hasPermission } = usePermission()

const activeTab = ref('brand')

// 树表折叠/展开
const isExpandAll = ref(false)
const refreshTable = ref(true)
function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => { refreshTable.value = true })
}

function treeRowClass({ row }) {
  return row.children?.length ? 'tree-parent-row' : 'tree-child-row'
}

// ==================== 品牌 ====================
const { loading: brandLoading, tableData: brandList, total: brandTotal, query: brandQuery, loadData: loadBrands } = usePageQuery(getBrandPage)
const brandSubmitting = ref(false)
const brandDialogVisible = ref(false)
const brandIsEdit = ref(false)
const brandEditId = ref(null)
const brandFormRef = ref(null)

const brandFormDef = () => ({ brandName: '', brandCode: '', status: 1, remark: '' })
const brandForm = reactive(brandFormDef())
const brandRules = { brandName: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }], brandCode: [{ required: true, message: '请输入品牌编码', trigger: 'blur' }] }

function handleBrandAdd() { brandIsEdit.value = false; brandEditId.value = null; Object.assign(brandForm, brandFormDef()); brandDialogVisible.value = true }
async function handleBrandEdit(row) { const res = await getBrand(row.id); brandIsEdit.value = true; brandEditId.value = row.id; Object.assign(brandForm, res.data); brandDialogVisible.value = true }
async function handleBrandDelete(row) { await ElMessageBox.confirm(`确定删除品牌「${row.brandName}」？`, '提示', { type: 'warning' }); await deleteBrand(row.id); ElMessage.success('删除成功'); loadBrands() }
async function handleBrandSubmit() {
  const valid = await brandFormRef.value?.validate().catch(() => false); if (!valid) return
  brandSubmitting.value = true
  try { if (brandIsEdit.value) { await updateBrand(brandEditId.value, { ...brandForm }) } else { await createBrand({ ...brandForm }) }; ElMessage.success(brandIsEdit.value ? '更新成功' : '创建成功'); brandDialogVisible.value = false; loadBrands() } finally { brandSubmitting.value = false }
}

// ==================== 地区 ====================
const regionLoading = ref(false)
const regionSubmitting = ref(false)
const regionDialogVisible = ref(false)
const regionIsEdit = ref(false)
const regionEditId = ref(null)
const regionFormRef = ref(null)
const regionList = ref([])

const regionFormDef = () => ({ parentId: '0', regionName: '', regionCode: '', sort: 0, status: 1 })
const regionForm = reactive(regionFormDef())
const regionRules = { regionName: [{ required: true, message: '请输入地区名称', trigger: 'blur' }] }
const regionTreeOpts = computed(() => [{ id: '0', regionName: '顶级地区', children: regionList.value }])

async function loadRegions() { regionLoading.value = true; try { const res = await getRegionTree(); regionList.value = res.data || [] } finally { regionLoading.value = false } }
function handleRegionAdd(parent) { regionIsEdit.value = false; regionEditId.value = null; Object.assign(regionForm, regionFormDef()); if (parent) regionForm.parentId = parent.id; regionDialogVisible.value = true }
async function handleRegionEdit(row) { const res = await getRegion(row.id); regionIsEdit.value = true; regionEditId.value = row.id; Object.assign(regionForm, res.data); regionDialogVisible.value = true }
async function handleRegionDelete(row) { if (row.children?.length) { ElMessage.warning('该地区下有子地区，无法删除'); return }; await ElMessageBox.confirm(`确定删除地区「${row.regionName}」？`, '提示', { type: 'warning' }); await deleteRegion(row.id); ElMessage.success('删除成功'); loadRegions() }
async function handleRegionSubmit() {
  const valid = await regionFormRef.value?.validate().catch(() => false); if (!valid) return
  regionSubmitting.value = true
  try { if (regionIsEdit.value) { await updateRegion(regionEditId.value, { ...regionForm }) } else { await createRegion({ ...regionForm }) }; ElMessage.success(regionIsEdit.value ? '更新成功' : '创建成功'); regionDialogVisible.value = false; loadRegions() } finally { regionSubmitting.value = false }
}

// ==================== 渠道类型 ====================
const ctLoading = ref(false)
const ctSubmitting = ref(false)
const ctDialogVisible = ref(false)
const ctIsEdit = ref(false)
const ctEditId = ref(null)
const ctFormRef = ref(null)
const ctList = ref([])

const ctFormDef = () => ({ parentId: '0', typeName: '', sort: 0, status: 1 })
const ctForm = reactive(ctFormDef())
const ctRules = { typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }] }
const ctTreeOpts = computed(() => [{ id: '0', typeName: '顶级类型', children: ctList.value }])

async function loadCt() { ctLoading.value = true; try { const res = await getChannelTypeTree(); ctList.value = res.data || [] } finally { ctLoading.value = false } }
function handleCtAdd(parent) { ctIsEdit.value = false; ctEditId.value = null; Object.assign(ctForm, ctFormDef()); if (parent) ctForm.parentId = parent.id; ctDialogVisible.value = true }
async function handleCtEdit(row) { const res = await getChannelType(row.id); ctIsEdit.value = true; ctEditId.value = row.id; Object.assign(ctForm, res.data); ctDialogVisible.value = true }
async function handleCtDelete(row) { if (row.children?.length) { ElMessage.warning('该类型下有子类型，无法删除'); return }; await ElMessageBox.confirm(`确定删除渠道类型「${row.typeName}」？`, '提示', { type: 'warning' }); await deleteChannelType(row.id); ElMessage.success('删除成功'); loadCt() }
async function handleCtSubmit() {
  const valid = await ctFormRef.value?.validate().catch(() => false); if (!valid) return
  ctSubmitting.value = true
  try { if (ctIsEdit.value) { await updateChannelType(ctEditId.value, { ...ctForm }) } else { await createChannelType({ ...ctForm }) }; ElMessage.success(ctIsEdit.value ? '更新成功' : '创建成功'); ctDialogVisible.value = false; loadCt() } finally { ctSubmitting.value = false }
}

// ==================== 渠道性质 ====================
const cnLoading = ref(false)
const cnSubmitting = ref(false)
const cnDialogVisible = ref(false)
const cnIsEdit = ref(false)
const cnEditId = ref(null)
const cnFormRef = ref(null)
const cnList = ref([])

const cnFormDef = () => ({ parentId: '0', natureName: '', sort: 0, status: 1 })
const cnForm = reactive(cnFormDef())
const cnRules = { natureName: [{ required: true, message: '请输入性质名称', trigger: 'blur' }] }
const cnTreeOpts = computed(() => [{ id: '0', natureName: '顶级性质', children: cnList.value }])

async function loadCn() { cnLoading.value = true; try { const res = await getChannelNatureTree(); cnList.value = res.data || [] } finally { cnLoading.value = false } }
function handleCnAdd(parent) { cnIsEdit.value = false; cnEditId.value = null; Object.assign(cnForm, cnFormDef()); if (parent) cnForm.parentId = parent.id; cnDialogVisible.value = true }
async function handleCnEdit(row) { const res = await getChannelNature(row.id); cnIsEdit.value = true; cnEditId.value = row.id; Object.assign(cnForm, res.data); cnDialogVisible.value = true }
async function handleCnDelete(row) { if (row.children?.length) { ElMessage.warning('该性质下有子性质，无法删除'); return }; await ElMessageBox.confirm(`确定删除渠道性质「${row.natureName}」？`, '提示', { type: 'warning' }); await deleteChannelNature(row.id); ElMessage.success('删除成功'); loadCn() }
async function handleCnSubmit() {
  const valid = await cnFormRef.value?.validate().catch(() => false); if (!valid) return
  cnSubmitting.value = true
  try { if (cnIsEdit.value) { await updateChannelNature(cnEditId.value, { ...cnForm }) } else { await createChannelNature({ ...cnForm }) }; ElMessage.success(cnIsEdit.value ? '更新成功' : '创建成功'); cnDialogVisible.value = false; loadCn() } finally { cnSubmitting.value = false }
}

const loadedTabs = new Set(['brand'])
watch(activeTab, (val) => {
  if (loadedTabs.has(val)) return
  loadedTabs.add(val)
  if (val === 'region') loadRegions()
  else if (val === 'channelType') loadCt()
  else if (val === 'channelNature') loadCn()
})
onMounted(() => { loadBrands() })
</script>

<style scoped>
.bi-mgmt-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
/* 父子行样式 */
.el-table :deep(.tree-parent-row) td {
  font-weight: 600;
  background-color: #f8fafc !important;
}
.el-table :deep(.tree-child-row) td {
  font-weight: 400;
}
/* 只有第一列内推，其他列统一对齐 */
.el-table :deep(.tree-child-row) td:first-child .cell {
  padding-left: 32px !important;
}
.el-table :deep(.tree-child-row) td:not(:first-child) .cell {
  padding-left: 10px !important;
}
/* hover 态 */
.el-table :deep(.tree-parent-row:hover) td {
  background-color: #f1f5f9 !important;
}
.el-table :deep(.tree-child-row:hover) td {
  background-color: #f8fafc !important;
}
/* 名称列图标 */
.name-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.icon-folder {
  color: var(--el-color-primary);
  flex-shrink: 0;
}
.icon-doc {
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}
.level-tag {
  margin-left: 6px;
  font-size: 11px;
  transform: scale(0.9);
  transform-origin: left center;
}
.icon-brand {
  color: var(--el-color-primary);
  flex-shrink: 0;
}
/* 展开箭头间距 */
.el-table :deep(.el-table__expand-icon) {
  margin-right: 4px;
}
</style>
