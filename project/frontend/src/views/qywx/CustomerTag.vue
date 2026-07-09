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
            <el-upload ref="uploadRef" :limit="1" accept=".xlsx,.xls" :auto-upload="false" drag :http-request="handleUpload">
              <el-icon :size="48"><Upload /></el-icon>
              <div>拖拽或点击上传 Excel 文件</div>
              <div class="upload-hint">第一列: externalUserid (客户ID)，后续列: 标签名称</div>
            </el-upload>
            <div style="margin-top:12px; text-align:center;">
              <el-button link type="primary" @click="handleDownloadTemplate">下载模板</el-button>
            </div>
            <div style="margin-top:12px; text-align:center;">
              <el-button type="primary" :loading="batchTagLoading" @click="handleUploadSubmit">开始打标</el-button>
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

      <!-- 打标签日志 -->
      <el-tab-pane label="打标签日志" name="records" lazy>
        <el-card shadow="never" class="search-card">
          <el-form :model="recordQuery" inline>
            <el-form-item label="客户ID">
              <el-input v-model="recordQuery.externalUserid" placeholder="请输入客户ID" clearable @keyup.enter="handleRecordSearch" />
            </el-form-item>
            <el-form-item label="标签名称">
              <el-input v-model="recordQuery.tagName" placeholder="请输入标签名称" clearable @keyup.enter="handleRecordSearch" />
            </el-form-item>
            <el-form-item label="批次号">
              <el-input v-model="recordQuery.batchNo" placeholder="请输入批次号" clearable @keyup.enter="handleRecordSearch" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleRecordSearch">搜索</el-button>
              <el-button :icon="RefreshRight" @click="resetRecordQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">打标签日志</span>
            </div>
          </template>
          <div class="table-responsive">
            <el-table v-loading="recordLoading" :data="recordData" border stripe empty-text="暂无数据">
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="externalUserid" label="客户ID" min-width="140" show-overflow-tooltip />
              <el-table-column prop="userid" label="跟进员工" min-width="120" show-overflow-tooltip />
              <el-table-column prop="tagName" label="标签名称" min-width="120" />
              <el-table-column label="来源" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.source === 'IMPORT' ? 'primary' : 'info'" size="small">{{ row.source === 'IMPORT' ? 'Excel导入' : row.source }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="batchNo" label="批次号" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ row.batchNo || '-' }}</template>
              </el-table-column>
              <el-table-column label="打标时间" width="170" align="center">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="recordQuery.pageNum" v-model:page-size="recordQuery.pageSize" :total="recordTotal" :page-sizes="PAGE_SIZES" layout="total, sizes, prev, pager, next" @size-change="handleRecordSearch" @current-change="loadRecordData" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

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
import { PAGE_SIZES } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import { getCorpTags, syncQywxTags, getQywxTagSyncStatus, getTagTemplate, uploadTagData, getTagRecords, addCorpTagGroup, editCorpTagGroup, deleteCorpTagGroup } from '@/api/qywx'

const { hasPermission } = usePermission()
const activeTab = ref('upload')

// ===== 打标签 =====
const uploadRef = ref(null)
const batchTagLoading = ref(false)
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
        loadRecordData()
        activeTab.value = 'records'
        ElMessage.success('打标任务已完成')
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

async function handleUpload({ file }) {
  batchTagLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    await uploadTagData(formData)
    ElMessage.success('打标任务已触发')
    startBatchTagPolling()
  } catch {
    batchTagLoading.value = false
  }
}

function handleUploadSubmit() { uploadRef.value?.submit() }

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

// ===== 打标签日志 =====
const recordLoading = ref(false)
const recordData = ref([])
const recordTotal = ref(0)
const recordQuery = reactive({ externalUserid: '', tagName: '', batchNo: '', pageNum: 1, pageSize: 20 })

async function loadRecordData() {
  recordLoading.value = true
  try { const res = await getTagRecords(recordQuery); recordData.value = res.data?.records || []; recordTotal.value = res.data?.total || 0 } finally { recordLoading.value = false }
}
function handleRecordSearch() { recordQuery.pageNum = 1; loadRecordData() }
function resetRecordQuery() { recordQuery.externalUserid = ''; recordQuery.tagName = ''; recordQuery.batchNo = ''; recordQuery.pageNum = 1; loadRecordData() }

watch(activeTab, (val) => {
  if (val === 'tags') loadTagData()
  else if (val === 'records') loadRecordData()
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
</style>
