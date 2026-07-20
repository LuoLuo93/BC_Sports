<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="品牌">
          <el-select v-model="query.brandId" placeholder="全部" clearable filterable style="min-width:140px;max-width:180px">
            <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ID" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="query.kindId" placeholder="全部" clearable filterable style="min-width:140px;max-width:180px">
            <el-option v-for="k in kindList" :key="k.ID" :label="k.ATTRIBNAME" :value="k.ID" />
          </el-select>
        </el-form-item>
        <el-form-item label="组编码">
          <el-input v-model="query.groupCode" placeholder="组编码" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="组名称">
          <el-input v-model="query.groupName" placeholder="组名称" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:100px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">本地尺码组维护</span>
          <div class="header-actions">
            <el-button v-if="hasPermission('sticker:size-group:add')" type="primary" size="small" :icon="Plus" @click="handleAdd">新增</el-button>
          </div>
        </div>
      </template>
      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe>
          <el-table-column prop="groupCode" label="组编码" width="140" />
          <el-table-column prop="groupName" label="组名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="brandName" label="品牌" width="140" />
          <el-table-column prop="kindName" label="类别" width="140" />
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('sticker:size-group:edit')" type="primary" plain size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('sticker:size-group:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          @size-change="() => { query.pageNum = 1; loadData() }"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑尺码组' : '新增尺码组'" width="780px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <!-- 基本信息区 -->
        <div class="dialog-section">
          <div class="section-header">
            <el-icon class="section-icon"><InfoFilled /></el-icon>
            <span class="section-title">基本信息</span>
          </div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="组编码" prop="groupCode">
                <el-input v-model="form.groupCode" placeholder="如 AD-OUTERWEAR" maxlength="50" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="组名称" prop="groupName">
                <el-input v-model="form.groupName" placeholder="如 阿迪外衣尺码组" maxlength="100" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="品牌" prop="brandId">
                <el-select v-model="form.brandId" placeholder="选择品牌" filterable style="width:100%" @change="onBrandChange">
                  <el-option v-for="b in brandList" :key="b.ID" :label="b.ATTRIBNAME" :value="b.ID" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="类别" prop="kindId">
                <el-select v-model="form.kindId" placeholder="选择类别" filterable style="width:100%" @change="onKindChange">
                  <el-option v-for="k in kindList" :key="k.ID" :label="k.ATTRIBNAME" :value="k.ID" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="排序">
                <el-input-number v-model="form.sort" :min="0" :max="999999" controls-position="right" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态">
                <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit resize="none" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 尺码明细区 -->
        <div class="dialog-section">
          <div class="section-header section-header--between">
            <div class="section-header-left">
              <el-icon class="section-icon"><Histogram /></el-icon>
              <span class="section-title">尺码明细</span>
              <span class="section-desc">名称为空的行保存时自动忽略</span>
            </div>
            <el-button type="primary" plain size="small" :icon="Plus" @click="addSizeRow">添加尺码</el-button>
          </div>
          <el-table v-if="form.sizes.length" :data="form.sizes" border size="small" class="size-table">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column label="尺码编码" width="160">
              <template #default="{ row }">
                <el-input v-model="row.sizeCode" placeholder="如 S / 38" size="small" maxlength="50" />
              </template>
            </el-table-column>
            <el-table-column label="尺码名称" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.sizeName" placeholder="必填" size="small" maxlength="50" />
              </template>
            </el-table-column>
            <el-table-column label="排序" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.sort" :min="0" :max="999999" size="small" controls-position="right" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button type="danger" plain size="small" @click="removeSizeRow($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="size-empty">
            <el-icon class="size-empty-icon"><Histogram /></el-icon>
            <div class="size-empty-text">暂无尺码，点击右上角"添加尺码"开始配置</div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, Plus, InfoFilled, Histogram } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getProductBrands, getSizeGroupPage, getSizeGroup, createSizeGroup, updateSizeGroup, deleteSizeGroup } from '@/api/sticker'
import { usePageQuery } from '@/composables/usePageQuery'
import { usePermission } from '@/composables/usePermission'
import { PAGE_SIZES } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'

const { hasPermission } = usePermission()

const brandList = ref([])
const kindList = ref([])

const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(
  (params) => getSizeGroupPage(params),
  { brandId: '', kindId: '', groupCode: '', groupName: '', status: undefined }
)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref('')
const saving = ref(false)
const formRef = ref()

const emptyForm = () => ({
  groupCode: '', groupName: '',
  brandId: '', brandName: '',
  kindId: '', kindName: '',
  status: 1, sort: 0, remark: '',
  sizes: []
})

const form = reactive(emptyForm())

const rules = {
  groupCode: [{ required: true, message: '请输入组编码', trigger: 'blur' }],
  groupName: [{ required: true, message: '请输入组名称', trigger: 'blur' }],
  brandId: [{ required: true, message: '请选择品牌', trigger: 'change' }],
  kindId: [{ required: true, message: '请选择类别', trigger: 'change' }]
}

async function loadBrands() {
  if (brandList.value.length) return
  try {
    const { data } = await getProductBrands()
    brandList.value = (data || []).map(b => ({ ...b, ID: String(b.ID) }))
  } catch {}
}

async function loadKinds() {
  if (kindList.value.length) return
  try {
    const { data } = await request.get('/api/sticker/brand-template/kinds')
    kindList.value = (data || []).map(k => ({ ...k, ID: String(k.ID) }))
  } catch {}
}

function onBrandChange(val) {
  const b = brandList.value.find(item => item.ID === val)
  form.brandName = b ? b.ATTRIBNAME : ''
}

function onKindChange(val) {
  const k = kindList.value.find(item => item.ID === val)
  form.kindName = k ? k.ATTRIBNAME : ''
}

function addSizeRow() {
  form.sizes.push({ sizeCode: '', sizeName: '', sort: form.sizes.length })
}

function removeSizeRow(index) {
  form.sizes.splice(index, 1)
}

function handleAdd() {
  isEdit.value = false
  editId.value = ''
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

async function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  dialogVisible.value = true
  try {
    const { data } = await getSizeGroup(row.id)
    await nextTick()
    Object.assign(form, {
      groupCode: data.groupCode || '',
      groupName: data.groupName || '',
      brandId: data.brandId ? String(data.brandId) : '',
      brandName: data.brandName || '',
      kindId: data.kindId ? String(data.kindId) : '',
      kindName: data.kindName || '',
      status: data.status ?? 1,
      sort: data.sort ?? 0,
      remark: data.remark || '',
      sizes: (data.sizes || []).map(s => ({
        id: s.id,
        sizeCode: s.sizeCode || '',
        sizeName: s.sizeName || '',
        sort: s.sort ?? 0
      }))
    })
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value.validate()
  } catch { return }
  // 校验至少有一条非空尺码名称
  const validSizes = (form.sizes || []).filter(s => s.sizeName && s.sizeName.trim())
  if (!validSizes.length) {
    ElMessage.warning('请至少添加一个尺码')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      sizes: validSizes.map((s, i) => ({
        id: s.id || null,
        sizeCode: s.sizeCode || null,
        sizeName: s.sizeName.trim(),
        sort: s.sort ?? i
      }))
    }
    if (isEdit.value) {
      await updateSizeGroup(editId.value, payload)
    } else {
      await createSizeGroup(payload)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除尺码组「${row.groupName}」及其下所有尺码？`, '提示', { type: 'warning' })
  await deleteSizeGroup(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => {
  loadData()
  loadBrands()
  loadKinds()
})
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}

/* 弹窗内分区 */
.dialog-section {
  padding: 4px 2px 16px;
}
.dialog-section + .dialog-section {
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px dashed #ebeef5;
}

/* 分区标题行 */
.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.section-header--between {
  justify-content: space-between;
}
.section-header-left {
  display: flex;
  align-items: center;
}
.section-icon {
  font-size: 18px;
  color: var(--el-color-primary);
  margin-right: 6px;
}
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  letter-spacing: 0.01em;
}
.section-desc {
  margin-left: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 尺码明细表格 */
.size-table {
  border-radius: 8px;
  overflow: hidden;
}

/* 尺码空状态 */
.size-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  background: #fafbfc;
}
.size-empty-icon {
  font-size: 34px;
  color: #c0c4cc;
  margin-bottom: 8px;
}
.size-empty-text {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
