<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="群名称">
          <el-input v-model="query.name" placeholder="请输入群名称" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="群主">
          <el-input v-model="query.ownerName" placeholder="请输入群主姓名" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="onReset">重置</el-button>
          <el-button type="success" size="small" :icon="Refresh" :loading="syncLoading" @click="handleSync">数据同步</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">群聊管理</span>
        </div>
      </template>
      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="name" label="群名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="chatId" label="群ID" min-width="180" show-overflow-tooltip />
          <el-table-column prop="ownerName" label="群主" width="120" />
          <el-table-column prop="groupType" label="群类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.groupType === '外部群' ? 'warning' : 'success'" size="small">{{ row.groupType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="memberList" label="成员数" width="90" align="center" />
          <el-table-column label="创建时间" width="180" align="center">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" plain size="small" @click="openMemberDialog(row)">成员</el-button>
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

    <!-- 群成员弹窗 -->
    <el-dialog v-model="memberDialogVisible" :title="`群成员 - ${currentGroup.name || ''}`" width="800px" destroy-on-close>
      <div class="table-responsive">
        <el-table v-loading="memberLoading" :data="memberData" border stripe empty-text="暂无数据" max-height="450">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="name" label="成员名称" min-width="120" />
          <el-table-column prop="userid" label="成员ID" min-width="150" show-overflow-tooltip />
          <el-table-column prop="type" label="身份" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.type === '1' ? 'danger' : 'info'" size="small">{{ row.type === '1' ? '群主' : '群成员' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" width="180" align="center">
            <template #default="{ row }">{{ formatTime(row.joinTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'GroupChatList' })
import { ref, onMounted } from 'vue'
import { Search, RefreshRight, Refresh } from '@element-plus/icons-vue'
import { usePageQuery } from '@/composables/usePageQuery'
import { useSyncAction } from '@/composables/useSyncAction'
import { PAGE_SIZES } from '@/utils/appConfig'
import { formatTime } from '@/utils/format'
import { getGroupChatPage, getGroupChatMembers, syncGroupChats, getGroupChatSyncStatus } from '@/api/qywx'

const { loading, tableData, total, query, loadData, handleSearch: onSearch } = usePageQuery(getGroupChatPage, {
  name: '', ownerName: ''
})

function onReset() {
  query.name = ''
  query.ownerName = ''
  onSearch()
}

// ===== 数据同步 =====
const { syncLoading, handleSync, checkStatus } = useSyncAction(syncGroupChats, loadData, '确定同步群聊数据？', getGroupChatSyncStatus)

// ===== 群成员弹窗 =====
const memberDialogVisible = ref(false)
const memberLoading = ref(false)
const memberData = ref([])
const currentGroup = ref({})

async function openMemberDialog(row) {
  currentGroup.value = row
  memberDialogVisible.value = true
  memberLoading.value = true
  try {
    const res = await getGroupChatMembers(row.chatId)
    memberData.value = res.data || []
  } finally {
    memberLoading.value = false
  }
}

onMounted(() => { loadData(); checkStatus() })
</script>

<style scoped>
</style>
