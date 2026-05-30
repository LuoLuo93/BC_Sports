<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="nxcrm-member-tabs">
      <!-- 会员详情 -->
      <el-tab-pane label="会员详情" name="detail">
        <el-card shadow="never" class="search-card">
          <el-form :model="detailQuery" inline>
            <el-form-item label="nasOuid">
              <el-input v-model="detailQuery.nasOuid" placeholder="输入nasOuid搜索" clearable @keyup.enter="loadDetail" style="width:200px" />
            </el-form-item>
            <el-form-item label="标签名">
              <el-input v-model="detailQuery.tagName" placeholder="输入标签名搜索" clearable @keyup.enter="loadDetail" style="width:200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="loadDetail">搜索</el-button>
              <el-button type="warning" :loading="memberTagSyncLoading" @click="handleSyncMemberTags">同步会员标签</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">会员标签详情</span>
            </div>
          </template>
          <div class="table-responsive">
          <el-table v-loading="detailLoading" :data="detailData" border stripe>
            <el-table-column prop="nasOuid" label="nasOuid" width="200" />
            <el-table-column prop="tagName" label="TAG_NAME" min-width="200" show-overflow-tooltip />
            <el-table-column prop="tagValueName" label="TAG_VALUE_NAME" min-width="200" show-overflow-tooltip />
          </el-table>
          </div>
          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="detailQuery.pageNum" v-model:page-size="detailQuery.pageSize" :total="detailTotal" :page-sizes="[20, 50, 100]" layout="total, sizes, prev, pager, next" @current-change="loadDetail" @size-change="loadDetail" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 南讯标签列表 -->
      <el-tab-pane label="南讯标签列表" name="tagList">
        <el-card shadow="never" class="search-card">
          <el-form :model="tagQuery" inline>
            <el-form-item label="搜索">
              <el-input v-model="tagQuery.keyword" placeholder="输入标签名/标签编码/文件夹搜索" clearable @keyup.enter="loadTagList" style="width:300px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="loadTagList">搜索</el-button>
              <el-button type="warning" :loading="tagSyncLoading" @click="handleSyncTags">同步标签数据</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">南讯标签列表</span>
            </div>
          </template>
          <div class="table-responsive">
          <el-table v-loading="tagLoading" :data="tagData" border stripe row-key="tagCode">
            <el-table-column prop="tagCode" label="标签编码" width="140" />
            <el-table-column prop="tagName" label="标签名称" min-width="160" />
            <el-table-column prop="tagFolderName" label="所属文件夹" width="140" />
            <el-table-column prop="valueDataType" label="值类型" width="100" />
            <el-table-column prop="hasValue" label="有值" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.hasValue === 1 ? 'success' : 'info'" size="small">{{ row.hasValue === 1 ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="标签值" min-width="200">
              <template #default="{ row }">
                <span v-if="row.tagValueList && row.tagValueList.length">
                  <el-tag v-for="v in row.tagValueList" :key="v.id" size="small" style="margin:2px">{{ v.tagValueName }}</el-tag>
                </span>
                <span v-else style="color:#909399">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="syncTime" label="同步时间" width="170" />
          </el-table>
          </div>
          <div class="pagination-wrapper--sm">
            <el-pagination v-model:current-page="tagQuery.pageNum" v-model:page-size="tagQuery.pageSize" :total="tagTotal" :page-sizes="[20, 50, 100]" layout="total, sizes, prev, pager, next" @current-change="loadTagList" @size-change="loadTagList" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useSyncAction } from '@/composables/useSyncAction'
import {
  getNxcrmMemberDetail,
  getNxcrmTagList,
  syncNxcrmTags,
  syncNxcrmMemberTags,
  getNxcrmSyncStatus
} from '@/api/nxcrm'

const activeTab = ref('detail')

// ---- 会员详情 ----
const detailLoading = ref(false)
const detailData = ref([])
const detailTotal = ref(0)
const detailQuery = reactive({ nasOuid: '', tagName: '', pageNum: 1, pageSize: 20 })

async function loadDetail() {
  detailLoading.value = true
  try {
    const { data } = await getNxcrmMemberDetail(detailQuery)
    detailData.value = data.records || []
    detailTotal.value = data.total || 0
  } finally {
    detailLoading.value = false
  }
}

// ---- 南讯标签列表 ----
const tagLoading = ref(false)
const tagData = ref([])
const tagTotal = ref(0)
const tagQuery = reactive({ keyword: '', pageNum: 1, pageSize: 20 })

async function loadTagList() {
  tagLoading.value = true
  try {
    const { data } = await getNxcrmTagList(tagQuery)
    tagData.value = data.records || []
    tagTotal.value = data.total || 0
  } finally {
    tagLoading.value = false
  }
}

// ---- 同步按钮 ----
function wrapStatus(field) {
  return async () => {
    const res = await getNxcrmSyncStatus()
    return { data: { syncing: res.data?.[field] } }
  }
}

const { syncLoading: tagSyncLoading, handleSync: handleSyncTags, checkStatus: checkTagSync } = useSyncAction(
  syncNxcrmTags,
  loadTagList,
  '确定同步南讯标签数据？',
  wrapStatus('tagSyncing')
)

const { syncLoading: memberTagSyncLoading, handleSync: handleSyncMemberTags, checkStatus: checkMemberTagSync } = useSyncAction(
  syncNxcrmMemberTags,
  loadDetail,
  '确定同步会员标签？',
  wrapStatus('memberTagSyncing')
)

onMounted(() => {
  loadDetail()
  loadTagList()
  checkTagSync()
  checkMemberTagSync()
})
</script>

<style scoped>
.nxcrm-member-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
</style>
