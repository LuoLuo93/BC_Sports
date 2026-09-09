<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header-row">
        <span class="card-header-title">{{ title }}</span>
        <el-button size="small" :icon="RefreshRight" @click="loadLogData">刷新</el-button>
      </div>
    </template>

    <div class="table-responsive">
      <el-table v-loading="logLoading" :data="logData" border stripe empty-text="暂无导入记录">
        <el-table-column label="#" width="60" align="center">
          <template #default="{ $index }">{{ (logQuery.pageNum - 1) * logQuery.pageSize + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="120" show-overflow-tooltip />
        <el-table-column label="文件大小" width="110" align="right">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总行数" width="100" align="right" />
        <el-table-column prop="successCount" label="成功" width="100" align="right">
          <template #default="{ row }"><span style="color:var(--el-color-success)">{{ row.successCount }}</span></template>
        </el-table-column>
        <el-table-column prop="failCount" label="失败" width="100" align="right">
          <template #default="{ row }"><span :style="{color: row.failCount > 0 ? 'var(--el-color-danger)' : ''}">{{ row.failCount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createBy" label="操作人" width="130" show-overflow-tooltip />
        <el-table-column label="导入时间" width="190">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.errorMsg" link type="primary" size="small" @click="viewErrors(row)">查看错误</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="logQuery.pageNum"
        v-model:page-size="logQuery.pageSize"
        :total="logTotal"
        :page-sizes="PAGE_SIZES"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadLogData"
        @current-change="loadLogData"
      />
    </div>
  </el-card>

  <!-- 错误详情弹窗 -->
  <el-dialog v-model="errorDialogVisible" title="导入错误详情" width="600px">
    <div style="max-height:420px;overflow-y:auto;border:1px solid var(--el-border-color-lighter);border-radius:6px;padding:8px 12px;background:var(--el-fill-color-lighter)">
      <pre style="white-space:pre-wrap;font-size:12px;color:var(--el-color-danger);line-height:1.8;margin:0">{{ errorDialogContent }}</pre>
    </div>
  </el-dialog>
</template>

<script setup>
/**
 * 导入日志面板（F70 / R01 阶段3）：表格 + 分页 + 错误详情弹窗，读统一日志接口
 * （各模块 /import-log/page，返回字段含 importType，其余与旧表一致）。
 * 用在导入页的"导入日志" tab 内；tab-pane 需加 lazy 避免进页面即请求。
 */
import { onMounted } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { formatTime } from '@/utils/format'
import { PAGE_SIZES } from '@/utils/appConfig'
import { useImportLog } from '@/composables/useImportLog'

const props = defineProps({
  /** 各模块的日志分页接口函数 (params) => Promise */
  fetcher: { type: Function, required: true },
  title: { type: String, default: '导入日志' },
})

const {
  logLoading, logData, logTotal, logQuery,
  loadLogData, viewErrors,
  errorDialogVisible, errorDialogContent,
  formatSize, statusLabel, statusTagType,
} = useImportLog(props.fetcher)

/** 供外层（导入成功后等场景）刷新 */
defineExpose({ loadLog: loadLogData })

onMounted(loadLogData)
</script>

<style scoped>
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header-title {
  font-size: 16px;
  font-weight: 600;
}
.pagination-wrapper {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
