<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="实体类型">
          <el-select v-model="query.entityType" placeholder="全部" clearable >
            <el-option label="店仓" value="store" />
            <el-option label="客户" value="customer" />
          </el-select>
        </el-form-item>
        <el-form-item label="外部ID">
          <el-input v-model="query.externalId" placeholder="请输入外部ID" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="实体名称">
          <el-input v-model="query.entityName" placeholder="请输入实体名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable >
            <el-option label="正常" :value="1" />
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
          <span class="card-header-title">实体渠道配置</span>
          <div style="display:flex;gap:8px">
            <el-button v-if="hasPermission('bi:entity:add')" type="warning" size="small" :icon="Upload" @click="showImportDialog = true">批量导入</el-button>
            <el-button v-if="hasPermission('bi:entity:add')" type="primary" size="small" :icon="Plus" @click="router.push('/bi/entity-channel/form')">新增配置</el-button>
          </div>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据" :default-sort="{ prop: 'entityType', order: 'ascending' }" @sort-change="handleSortChange">
          <el-table-column label="#" width="50" align="center">
            <template #default="{ $index }">{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="类型" width="80" align="center" prop="entityType" sortable="custom">
            <template #default="{ row }">
              <el-tag :type="entityTypeTag(row.entityType)" size="small">{{ entityTypeLabel(row.entityType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="externalId" label="编码" min-width="100" sortable="custom">
            <template #default="{ row }">
              <el-tag v-if="row.externalId" size="small" type="info">{{ row.externalId }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="entityName" label="名称" min-width="180" show-overflow-tooltip sortable="custom">
            <template #default="{ row }">
              <div class="name-cell">
                <el-icon v-if="row.entityType === 'store'" class="icon-store" :size="15"><OfficeBuilding /></el-icon>
                <el-icon v-else class="icon-customer" :size="15"><User /></el-icon>
                <span>{{ row.entityName || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="brandName" label="品牌" min-width="90">
            <template #default="{ row }">
              <div v-if="row.brandName" class="name-cell"><el-icon class="icon-brand" :size="14"><PriceTag /></el-icon><span>{{ row.brandName }}</span></div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="一二级地区" min-width="160">
            <template #default="{ row }">
              <template v-if="row.regionLevel1Name">
                <span>{{ row.regionLevel1Name }}</span>
                <span v-if="row.regionLevel2Name" class="region-sep">/</span>
                <span v-if="row.regionLevel2Name">{{ row.regionLevel2Name }}</span>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="渠道类型/定义" min-width="160">
            <template #default="{ row }">
              <template v-if="row.channelTypeName">
                <span>{{ row.channelTypeName }}</span>
                <span v-if="row.channelDefName" class="region-sep">/</span>
                <span v-if="row.channelDefName">{{ row.channelDefName }}</span>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="渠道性质/经营类型" min-width="140">
            <template #default="{ row }">
              <template v-if="row.channelNatureName">
                <span>{{ row.channelNatureName }}</span>
                <span v-if="row.businessTypeName" class="region-sep">/</span>
                <span v-if="row.businessTypeName">{{ row.businessTypeName }}</span>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('bi:entity:edit')" type="primary" plain size="small" @click="router.push(`/bi/entity-channel/form?id=${row.id}`)">编辑</el-button>
              <el-button v-if="hasPermission('bi:entity:delete')" type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="showImportDialog" title="批量导入实体渠道配置" width="520px" destroy-on-close @open="resetImportState">
      <div class="import-zone">
        <el-upload ref="uploadRef" :limit="1" accept=".xlsx,.xls" :auto-upload="false" drag :on-change="handleFileChange" :on-remove="handleFileRemove">
          <el-icon :size="40" style="color:var(--el-text-color-placeholder)"><Upload /></el-icon>
          <div style="margin-top:8px">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="upload-hint">仅支持 .xlsx / .xls 格式</div>
          </template>
        </el-upload>
        <div style="margin-top:12px;text-align:center">
          <el-button link type="primary" :loading="templateLoading" @click="handleDownloadTemplate">下载导入模板</el-button>
        </div>
      </div>

      <!-- 导入结果 -->
      <div v-if="importResult" style="margin-top:16px">
        <el-alert
          :title="`导入完成：共 ${importResult.total} 条，成功 ${importResult.success} 条，失败 ${importResult.fail} 条`"
          :type="importResult.fail === 0 ? 'success' : (importResult.success === 0 ? 'error' : 'warning')"
          show-icon
          :closable="false"
          style="margin-bottom:8px"
        />
        <div v-if="importResult.errors?.length" style="max-height:240px;overflow-y:auto;border:1px solid var(--el-border-color-lighter);border-radius:6px;padding:8px 12px;background:var(--el-fill-color-lighter)">
          <div v-for="(err, idx) in importResult.errors" :key="idx" style="font-size:12px;color:var(--el-color-danger);line-height:2;border-bottom:1px dashed var(--el-border-color-extra-light)">
            {{ err }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showImportDialog = false">关闭</el-button>
        <el-button type="primary" :loading="importLoading" :disabled="importLoading" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'EntityChannel' })
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEntityChannelPage, deleteEntityChannel, getEntityChannelTemplate, importEntityChannel } from '@/api/channel'
import { Plus, Search, RefreshRight, OfficeBuilding, User, PriceTag, Upload } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/appConfig'

const { hasPermission } = usePermission()

const router = useRouter()
const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getEntityChannelPage, { entityType: undefined, externalId: '', entityName: '', status: undefined })

const typeMap = { store: ['店仓', 'success'], customer: ['客户', 'warning'] }
function entityTypeLabel(t) { return typeMap[t]?.[0] || t }
function entityTypeTag(t) { return typeMap[t]?.[1] || 'info' }

// 字段映射：前端 prop → 后端数据库列名
const sortPropMap = { entityType: 'entity_type', externalId: 'external_id', entityName: 'entity_name' }
function handleSortChange({ prop, order }) {
  if (prop && order) {
    query.orderBy = sortPropMap[prop] || prop
    query.orderDirection = order === 'ascending' ? 'asc' : 'desc'
  } else {
    query.orderBy = undefined
    query.orderDirection = undefined
  }
  handleSearch()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除该实体渠道配置？`, '提示', { type: 'warning' })
  await deleteEntityChannel(row.id)
  ElMessage.success('删除成功')
  loadData()
}

// === 批量导入 ===
const showImportDialog = ref(false)
const uploadRef = ref(null)
const importLoading = ref(false)
const templateLoading = ref(false)
const importResult = ref(null)
const selectedFile = ref(null)

async function handleDownloadTemplate() {
  templateLoading.value = true
  try {
    const res = await getEntityChannelTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '实体渠道配置导入模板.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载模板失败')
  } finally {
    templateLoading.value = false
  }
}

function handleFileChange(uploadFile) {
  selectedFile.value = uploadFile.raw || null
  importResult.value = null
}

function handleFileRemove() {
  selectedFile.value = null
}

function resetImportState() {
  selectedFile.value = null
  importResult.value = null
  importLoading.value = false
}

async function submitImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }

  importLoading.value = true
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await importEntityChannel(formData)
    importResult.value = res.data
    if (res.data?.success > 0) {
      loadData()
    }
  } catch (e) {
    ElMessage.error('导入失败：' + (e.message || '服务器错误'))
  } finally {
    importLoading.value = false
  }
}

onMounted(() => loadData())
onActivated(() => loadData())
</script>

<style scoped>
.name-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.icon-store { color: var(--el-color-primary); }
.icon-customer { color: var(--el-color-warning); }
.icon-brand { color: var(--el-color-danger); }
.region-sep {
  color: var(--el-text-color-placeholder);
  margin: 0 2px;
}

.import-zone {
  padding: 8px 0;
}
.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}
.import-error-row {
  font-size: 12px;
  color: var(--el-color-danger);
  line-height: 1.8;
  padding: 0 4px;
}
</style>
