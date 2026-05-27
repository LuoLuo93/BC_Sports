<template>
  <div class="page-container">
    <el-tabs v-model="activeTab">
      <!-- 会员详情 -->
      <el-tab-pane label="会员详情" name="detail">
        <el-card shadow="never">
          <div style="display:flex;gap:8px;margin-bottom:16px;align-items:center">
            <el-input v-model="detailQuery.nasOuid" placeholder="输入nasOuid搜索" clearable @keyup.enter="loadDetail" style="width:240px" />
            <el-input v-model="detailQuery.tagName" placeholder="输入标签名搜索" clearable @keyup.enter="loadDetail" style="width:240px" />
            <el-button type="primary" @click="loadDetail">搜索</el-button>
            <div style="flex:1" />
            <el-button type="warning" :loading="memberTagSyncLoading" @click="handleSyncMemberTags">同步会员标签</el-button>
          </div>
          <el-table v-loading="detailLoading" :data="detailData" border stripe>
            <el-table-column prop="nasOuid" label="nasOuid" width="200" />
            <el-table-column prop="tagName" label="TAG_NAME" min-width="200" show-overflow-tooltip />
            <el-table-column prop="tagValueName" label="TAG_VALUE_NAME" min-width="200" show-overflow-tooltip />
          </el-table>
          <el-pagination
            v-model:current-page="detailQuery.pageNum"
            v-model:page-size="detailQuery.pageSize"
            :total="detailTotal"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadDetail"
            @size-change="loadDetail"
            style="margin-top:16px;justify-content:flex-end"
          />
        </el-card>
      </el-tab-pane>

      <!-- 南讯标签列表 -->
      <el-tab-pane label="南讯标签列表" name="tagList">
        <el-card shadow="never">
          <div style="display:flex;gap:8px;margin-bottom:16px;align-items:center">
            <el-input v-model="tagQuery.keyword" placeholder="输入标签名/标签编码/文件夹搜索" clearable @keyup.enter="loadTagList" style="width:300px" />
            <el-button type="primary" @click="loadTagList">搜索</el-button>
            <div style="flex:1" />
            <el-button type="warning" :loading="tagSyncLoading" @click="handleSyncTags">同步标签数据</el-button>
          </div>
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
          <el-pagination
            v-model:current-page="tagQuery.pageNum"
            v-model:page-size="tagQuery.pageSize"
            :total="tagTotal"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadTagList"
            @size-change="loadTagList"
            style="margin-top:16px;justify-content:flex-end"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
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
