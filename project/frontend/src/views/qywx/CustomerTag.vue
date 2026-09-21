<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="qywx-tag-tabs">
      <!-- 打标签 -->
      <el-tab-pane label="打标签" name="upload">
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">批量打标签</span>
            </div>
          </template>
          <div class="upload-drop-zone">
            <el-upload ref="uploadRef" :limit="1" accept=".xlsx,.xls" :auto-upload="false" drag :on-change="handleFileChange" :on-remove="() => (selectedFile = null)">
              <el-icon :size="48"><Upload /></el-icon>
              <div>拖拽或点击上传 Excel 文件</div>
              <div class="upload-hint">第一列: externalUserid (客户ID)，后续列: 标签名称；标签名前加 - 表示移除该标签</div>
            </el-upload>
            <div style="margin-top:12px; text-align:center;">
              <el-button link type="primary" @click="handleDownloadTemplate">下载模板</el-button>
            </div>
            <div style="margin-top:12px; text-align:center;">
              <el-button type="primary" :loading="batchTagLoading || previewLoading" @click="handleUploadSubmit">开始打标</el-button>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 标签集 -->
      <el-tab-pane label="标签集" name="tags" lazy>
        <el-card shadow="never" class="search-card">
          <el-form :model="tagQuery" inline>
            <el-form-item label="标签名称">
              <el-input v-model="tagQuery.tagName" placeholder="请输入标签名称" clearable @keyup.enter="handleTagSearch" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleTagSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetTagQuery">重置</el-button>
              <el-button v-if="hasPermission('qywx:tag:sync')" type="success" size="small" :icon="Refresh" :loading="syncLoading" @click="handleSyncTag">同步标签库</el-button>
              <el-button type="primary" size="small" :icon="Plus" @click="showAddTagDialog">添加标签组</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">企业标签库</span>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="tagLoading" :data="tagTreeData" border stripe>
              <el-table-column prop="tagName" label="标签组名称" width="160" />
              <el-table-column label="标签" min-width="400">
                <template #default="{ row }">
                  <span v-if="row.children && row.children.length">
                    <el-tag v-for="child in row.children" :key="child.tagId" size="small" style="margin:2px">{{ child.tagName }}</el-tag>
                  </span>
                  <span v-else style="color:#909399">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
              <el-table-column label="创建时间" width="170" align="center">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" plain size="small" @click="showEditTagDialog(row)">编辑</el-button>
                  <el-button type="danger" plain size="small" @click="handleDeleteTagGroup(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 打标签日志：默认只显示批次汇总，选中批次后展开明细 -->
      <el-tab-pane label="打标签日志" name="records" lazy>
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">打标批次汇总</span>
              <el-button link type="primary" @click="loadBatchData">刷新</el-button>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="batchLoading" :data="batchData" border stripe empty-text="暂无打标批次，请先在「打标签」页上传Excel">
              <el-table-column prop="batchNo" label="批次号" min-width="150" show-overflow-tooltip>
                <template #default="{ row }">
                  <el-button link type="primary" @click="viewBatchDetail(row)">{{ row.batchNo }}</el-button>
                </template>
              </el-table-column>
              <el-table-column prop="fileName" label="文件名" min-width="110" show-overflow-tooltip>
                <template #default="{ row }">{{ row.fileName || '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'DONE' ? 'success' : row.status === 'RUNNING' ? 'warning' : 'danger'" size="small">
                    {{ row.status === 'DONE' ? '完成' : row.status === 'RUNNING' ? '进行中' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalRows" label="总行数" width="70" align="center" />
              <el-table-column prop="successCnt" label="成功" width="65" align="center">
                <template #default="{ row }">
                  <span :style="row.successCnt > 0 && row.status !== 'RUNNING' ? 'color:#67C23A' : ''">{{ row.successCnt }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="failCnt" label="失败" width="65" align="center">
                <template #default="{ row }">
                  <span :style="row.failCnt > 0 ? 'color:#F56C6C' : ''">{{ row.failCnt }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="unmatchedTagRows" label="未匹配标签" width="90" align="center">
                <template #default="{ row }">
                  <span :style="row.unmatchedTagRows > 0 ? 'color:#E6A23C' : ''">{{ row.unmatchedTagRows }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="unmatchedCustomerRows" label="未匹配客户" width="90" align="center">
                <template #default="{ row }">
                  <span :style="row.unmatchedCustomerRows > 0 ? 'color:#E6A23C' : ''">{{ row.unmatchedCustomerRows }}</span>
                </template>
              </el-table-column>
              <el-table-column label="时间" width="150" align="center">
                <template #default="{ row }">
                  <div class="batch-time">{{ formatTime(row.startTime) || '-' }}</div>
                  <div class="batch-time" v-if="row.endTime">→ {{ formatTime(row.endTime).slice(5) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="失败原因" min-width="110" show-overflow-tooltip>
                <template #default="{ row }">{{ row.errmsg || '-' }}</template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="batchQuery.pageNum" v-model:page-size="batchQuery.pageSize" :total="batchTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next" @size-change="() => { batchQuery.pageNum = 1; loadBatchData() }" @current-change="loadBatchData" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加/编辑标签组 -->
    <el-dialog v-model="addTagDialogVisible" :title="editingGroupId ? '编辑标签组' : '添加标签组'" width="500px" destroy-on-close>
      <el-form :model="addTagForm" label-width="90px">
        <el-form-item label="标签组名称">
          <el-input v-model="addTagForm.groupName" placeholder="请输入标签组名称" />
        </el-form-item>
        <el-form-item label="标签列表">
          <div style="width:100%">
            <div v-for="(tag, idx) in addTagForm.tags" :key="idx" style="display:flex;gap:8px;margin-bottom:8px">
              <el-input v-model="tag.tagName" placeholder="请输入标签名称" />
              <el-button :icon="Delete" circle size="small" @click="removeTagRow(idx)" />
            </div>
            <el-button size="small" @click="addTagForm.tags.push({ tagId: null, tagName: '' })">+ 添加标签</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="addTagDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="addTagSubmitting" @click="handleAddTagGroup">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 上传预检确认 -->
    <el-dialog v-model="previewDialogVisible" title="打标预检确认" width="560px" destroy-on-close>
      <div v-if="previewResult">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="有效行数">{{ previewResult.totalRows }}（新增 {{ previewResult.addRows }} / 移除 {{ previewResult.removeRows }}）</el-descriptions-item>
          <el-descriptions-item label="可匹配行数">{{ previewResult.matchedRows }}</el-descriptions-item>
          <el-descriptions-item label="预计执行操作数" :span="2">{{ previewResult.plannedApplications }}（去重后按 员工-客户-标签 计）</el-descriptions-item>
        </el-descriptions>

        <div v-if="previewResult.unmatchedTags?.length" style="margin-top:12px">
          <div style="margin-bottom:4px;color:#E6A23C">未匹配标签（标签库中不存在，{{ previewResult.unmatchedTags.length }} 个）：{{ previewResult.unmatchedTags.slice(0, 20).join('、') }}{{ previewResult.unmatchedTags.length > 20 ? ' …' : '' }}</div>
        </div>
        <div v-if="previewResult.unmatchedRemoveTags?.length" style="margin-top:4px;color:#E6A23C">
          移除动作未匹配标签（{{ previewResult.unmatchedRemoveTags.length }} 个）：{{ previewResult.unmatchedRemoveTags.slice(0, 20).join('、') }}{{ previewResult.unmatchedRemoveTags.length > 20 ? ' …' : '' }}
        </div>
        <div v-if="previewResult.unmatchedCustomers?.length" style="margin-top:8px;color:#E6A23C">
          未匹配客户（不存在或无跟进员工，{{ previewResult.unmatchedCustomers.length }} 个）：
          <div class="unmatched-list">{{ previewResult.unmatchedCustomers.slice(0, 20).join('、') }}{{ previewResult.unmatchedCustomers.length > 20 ? ' …' : '' }}</div>
        </div>

        <el-checkbox v-if="previewResult.unmatchedTags?.length" v-model="autoCreateTags" style="margin-top:12px">
          未匹配的新增标签自动创建到「Excel导入」标签组
        </el-checkbox>

        <el-alert v-if="previewResult.unmatchedTags?.length || previewResult.unmatchedCustomers?.length"
          type="info" :closable="false" style="margin-top:12px"
          title="未匹配的行不会在企微生效，会以「未匹配」状态记入打标签日志" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="previewDialogVisible = false">取消</el-button>
          <el-button class="btn-confirm" type="primary" :loading="batchTagLoading" @click="confirmUpload">开始打标</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批次明细弹窗 -->
    <el-dialog v-model="recordDialogVisible" :title="selectedBatch ? `批次明细：${selectedBatch.batchNo}（${selectedBatch.fileName || '-'}）` : '批次明细'" width="1100px" destroy-on-close>
      <el-form :model="recordQuery" inline style="margin-bottom:4px">
        <el-form-item label="客户ID">
          <el-input v-model="recordQuery.externalUserid" placeholder="请输入客户ID" clearable @keyup.enter="handleRecordSearch" />
        </el-form-item>
        <el-form-item label="标签名称">
          <el-input v-model="recordQuery.tagName" placeholder="请输入标签名称" clearable @keyup.enter="handleRecordSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="recordQuery.status" placeholder="全部" clearable style="width:130px" @change="handleRecordSearch">
            <el-option label="成功" :value="1" />
            <el-option label="接口失败" :value="0" />
            <el-option label="标签未匹配" :value="2" />
            <el-option label="客户未匹配" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleRecordSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="resetRecordQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="recordLoading" :data="recordData" border stripe empty-text="该批次暂无记录" :max-height="420">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="externalUserid" label="客户ID" min-width="150" show-overflow-tooltip />
        <el-table-column prop="userid" label="跟进员工" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.userid || '-' }}</template>
        </el-table-column>
        <el-table-column prop="tagName" label="标签名称" min-width="110" />
        <el-table-column label="动作" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.tagAction === 'REMOVE' ? 'danger' : 'info'" size="small" effect="plain">{{ row.tagAction === 'REMOVE' ? '移除' : '打标' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errmsg || '-' }}</template>
        </el-table-column>
        <el-table-column label="来源" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.source === 'IMPORT' ? 'primary' : 'info'" size="small">{{ row.source === 'IMPORT' ? 'Excel导入' : row.source }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="打标时间" width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper--sm">
        <el-pagination v-model:current-page="recordQuery.pageNum" v-model:page-size="recordQuery.pageSize" :total="recordTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next" @size-change="handleRecordSearch" @current-change="loadRecordData" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'CustomerTag' })
import { ref, reactive, watch, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { Search, RefreshRight, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { useSyncAction } from '@/composables/useSyncAction'
import { PAGE_SIZES, defaultPageSize } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import { getCorpTags, syncQywxTags, getQywxTagSyncStatus, getTagTemplate, uploadTagData, previewTagUpload, getTagBatches, getTagRecords, addCorpTagGroup, editCorpTagGroup, deleteCorpTagGroup } from '@/api/qywx'

const { hasPermission } = usePermission()
const activeTab = ref('upload')

// ===== 打标签 =====
const uploadRef = ref(null)
const selectedFile = ref(null)
const batchTagLoading = ref(false)
const previewLoading = ref(false)
const previewDialogVisible = ref(false)
const previewResult = ref(null)
const autoCreateTags = ref(true)
let batchTagTimer = null

function stopBatchTagPolling() {
  if (batchTagTimer) { clearInterval(batchTagTimer); batchTagTimer = null }
}

function startBatchTagPolling() {
  if (batchTagTimer) return
  batchTagTimer = setInterval(async () => {
    try {
      const res = await getQywxTagSyncStatus()
      if (!res.data?.batchTagging) {
        stopBatchTagPolling()
        batchTagLoading.value = false
        await loadBatchData()
        // 自动弹出最新批次的明细
        if (batchData.value.length && !selectedBatch.value) {
          viewBatchDetail(batchData.value[0])
        }
        activeTab.value = 'records'
        ElMessage.success('打标任务已完成，结果见批次汇总')
      }
    } catch {
      // keep polling on transient errors
    }
  }, 3000)
}

async function checkBatchTagStatus() {
  try {
    const res = await getQywxTagSyncStatus()
    if (res.data?.batchTagging) {
      batchTagLoading.value = true
      startBatchTagPolling()
    }
  } catch {
    // ignore
  }
}

function handleFileChange(uploadFile) {
  selectedFile.value = uploadFile?.raw || null
}

async function handleUploadSubmit() {
  if (!selectedFile.value) { ElMessage.warning('请先选择Excel文件'); return }
  previewLoading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    const res = await previewTagUpload(fd)
    previewResult.value = res.data
    autoCreateTags.value = true
    previewDialogVisible.value = true
  } catch {
    // interceptor showed error
  } finally {
    previewLoading.value = false
  }
}

async function confirmUpload() {
  if (!selectedFile.value) { previewDialogVisible.value = false; return }
  batchTagLoading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('autoCreateTags', autoCreateTags.value)
    await uploadTagData(fd)
    ElMessage.success('打标任务已触发')
    previewDialogVisible.value = false
    startBatchTagPolling()
  } catch {
    batchTagLoading.value = false
  }
}

async function handleDownloadTemplate() {
  const res = await getTagTemplate()
  const url = window.URL.createObjectURL(new Blob([res]))
  const link = document.createElement('a'); link.href = url; link.download = '批量打标模板.xlsx'; link.click()
  window.URL.revokeObjectURL(url)
}

// ===== 标签集 =====
const tagLoading = ref(false)
const tagRawData = ref([])
const tagQuery = reactive({ tagName: '', pageNum: 1, pageSize: 500 })

const tagTreeData = computed(() => buildTagTree(tagRawData.value))

function buildTagTree(records) {
  const groups = []
  const childMap = {}
  records.forEach(tag => {
    if (!tag.groupId) {
      groups.push({ ...tag, children: [] })
    } else {
      if (!childMap[tag.groupId]) childMap[tag.groupId] = []
      childMap[tag.groupId].push(tag)
    }
  })
  groups.forEach(g => {
    g.children = childMap[g.tagId] || []
  })
  return groups
}

async function loadTagData() {
  tagLoading.value = true
  try { const res = await getCorpTags(tagQuery); tagRawData.value = res.data?.records || [] } finally { tagLoading.value = false }
}
function handleTagSearch() { tagQuery.pageNum = 1; loadTagData() }
function resetTagQuery() { tagQuery.tagName = ''; tagQuery.pageNum = 1; loadTagData() }

const { syncLoading, handleSync: handleSyncTag, checkStatus: checkTagSyncStatus } = useSyncAction(syncQywxTags, loadTagData, '确定同步企业标签库？', getQywxTagSyncStatus)

// ===== 添加/编辑标签组 =====
const addTagDialogVisible = ref(false)
const addTagSubmitting = ref(false)
const editingGroupId = ref(null)
const deletedTagIds = ref([])
const addTagForm = reactive({ groupName: '', tags: [{ tagId: null, tagName: '' }] })

function showAddTagDialog() {
  editingGroupId.value = null
  deletedTagIds.value = []
  addTagForm.groupName = ''
  addTagForm.tags = [{ tagId: null, tagName: '' }]
  addTagDialogVisible.value = true
}

function showEditTagDialog(row) {
  editingGroupId.value = row.tagId
  deletedTagIds.value = []
  addTagForm.groupName = row.tagName
  addTagForm.tags = (row.children || []).map(c => ({ tagId: c.tagId, tagName: c.tagName }))
  addTagDialogVisible.value = true
}

function removeTagRow(idx) {
  const removed = addTagForm.tags.splice(idx, 1)[0]
  if (removed.tagId && editingGroupId.value) {
    deletedTagIds.value.push(removed.tagId)
  }
}

async function handleAddTagGroup() {
  if (!addTagForm.groupName.trim()) { ElMessage.warning('请输入标签组名称'); return }
  const tags = addTagForm.tags.filter(t => t.tagName.trim())
  if (tags.length === 0) { ElMessage.warning('请至少添加一个标签'); return }
  addTagSubmitting.value = true
  try {
    if (editingGroupId.value) {
      await editCorpTagGroup({
        groupId: editingGroupId.value,
        groupName: addTagForm.groupName.trim(),
        tags: tags.map(t => ({ tagId: t.tagId || null, tagName: t.tagName.trim() })),
        deletedTagIds: deletedTagIds.value
      })
      ElMessage.success('标签组编辑成功')
    } else {
      await addCorpTagGroup({ groupName: addTagForm.groupName.trim(), tags: tags.map(t => t.tagName.trim()) })
      ElMessage.success('标签组创建成功')
    }
    addTagDialogVisible.value = false
    loadTagData()
  } catch {
    // interceptor showed error
  } finally {
    addTagSubmitting.value = false
  }
}

async function handleDeleteTagGroup(row) {
  try {
    await ElMessageBox.confirm(`确定删除标签组「${row.tagName}」及其所有标签？此操作不可恢复。`, '提示', { type: 'warning' })
  } catch { return }
  try {
    const tagIds = (row.children || []).map(c => c.tagId)
    await deleteCorpTagGroup({ groupId: row.tagId, tagIds })
    ElMessage.success('标签组删除成功')
    loadTagData()
  } catch {
    // interceptor showed error
  }
}

// ===== 打标批次汇总 =====
const batchLoading = ref(false)
const batchData = ref([])
const batchTotal = ref(0)
const batchQuery = reactive({ pageNum: 1, pageSize: 10 })
const selectedBatch = ref(null)
const recordDialogVisible = ref(false)

async function loadBatchData() {
  batchLoading.value = true
  try { const res = await getTagBatches(batchQuery); batchData.value = res.data?.records || []; batchTotal.value = res.data?.total || 0 } finally { batchLoading.value = false }
}

function viewBatchDetail(row) {
  selectedBatch.value = row
  recordQuery.externalUserid = ''
  recordQuery.tagName = ''
  recordQuery.status = undefined
  recordQuery.batchNo = row.batchNo
  recordQuery.pageNum = 1
  recordDialogVisible.value = true
  loadRecordData()
}

// ===== 打标签日志 =====
const recordLoading = ref(false)
const recordData = ref([])
const recordTotal = ref(0)
const recordQuery = reactive({ externalUserid: '', tagName: '', batchNo: '', status: undefined, pageNum: 1, pageSize: defaultPageSize.value })

const STATUS_MAP = {
  1: { label: '成功', type: 'success' },
  0: { label: '接口失败', type: 'danger' },
  2: { label: '标签未匹配', type: 'warning' },
  3: { label: '客户未匹配', type: 'info' }
}
function statusLabel(s) { return STATUS_MAP[s]?.label || '成功' }
function statusTagType(s) { return STATUS_MAP[s]?.type || 'success' }

async function loadRecordData() {
  recordLoading.value = true
  try { const res = await getTagRecords(recordQuery); recordData.value = res.data?.records || []; recordTotal.value = res.data?.total || 0 } finally { recordLoading.value = false }
}
function handleRecordSearch() { recordQuery.pageNum = 1; loadRecordData() }
function resetRecordQuery() { recordQuery.externalUserid = ''; recordQuery.tagName = ''; recordQuery.status = undefined; recordQuery.pageNum = 1; loadRecordData() }

watch(activeTab, (val) => {
  if (val === 'tags') loadTagData()
  else if (val === 'records') loadBatchData()
})

onMounted(() => {
  checkTagSyncStatus()
  checkBatchTagStatus()
})

onUnmounted(() => {
  stopBatchTagPolling()
})
</script>

<style scoped>
.qywx-tag-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.upload-drop-zone {
  padding: 20px;
  max-width: 500px;
}
.upload-hint {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
.unmatched-list {
  word-break: break-all;
  max-height: 80px;
  overflow-y: auto;
  color: #909399;
  font-size: 12px;
}
.batch-time {
  font-size: 12px;
  line-height: 18px;
  color: #606266;
}
</style>
