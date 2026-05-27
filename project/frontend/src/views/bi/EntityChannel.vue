<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="实体类型">
          <el-select v-model="query.entityType" placeholder="全部" clearable >
            <el-option label="店铺" value="shop" />
            <el-option label="仓库" value="stock" />
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
          <el-button v-if="hasPermission('bi:entity:add')" type="primary" size="small" :icon="Plus" @click="router.push('/bi/entity-channel/form')">新增配置</el-button>
        </div>
      </template>

      <div class="table-responsive">
        <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无数据">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="entityTypeTag(row.entityType)" size="small">{{ entityTypeLabel(row.entityType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="externalId" label="外部ID" min-width="120" />
          <el-table-column prop="entityName" label="实体名称" min-width="140" />
          <el-table-column prop="brandName" label="品牌" width="120">
            <template #default="{ row }">{{ row.brandName || '-' }}</template>
          </el-table-column>
          <el-table-column label="地区" min-width="160">
            <template #default="{ row }">
              {{ [row.regionLevel1Name, row.regionLevel2Name].filter(Boolean).join(' / ') || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="channelTypeName" label="渠道类型" width="110">
            <template #default="{ row }">{{ row.channelTypeName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="channelNatureName" label="渠道性质" width="110">
            <template #default="{ row }">{{ row.channelNatureName || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
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
  </div>
</template>

<script setup>
defineOptions({ name: 'EntityChannel' })
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEntityChannelPage, deleteEntityChannel } from '@/api/channel'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { usePageQuery } from '@/composables/usePageQuery'
import { PAGE_SIZES } from '@/utils/constants'

const { hasPermission } = usePermission()

const router = useRouter()
const { loading, tableData, total, query, loadData, handleSearch, resetQuery } = usePageQuery(getEntityChannelPage, { entityType: undefined, externalId: '', entityName: '', status: undefined })

const typeMap = { shop: ['店铺', 'success'], stock: ['仓库', 'primary'], customer: ['客户', 'warning'] }
function entityTypeLabel(t) { return typeMap[t]?.[0] || t }
function entityTypeTag(t) { return typeMap[t]?.[1] || 'info' }

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除该实体渠道配置？`, '提示', { type: 'warning' })
  await deleteEntityChannel(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>
