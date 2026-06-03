<template>
  <component :is="nested ? 'template' : 'div'" v-bind="!nested && { class: 'page-container' }">
    <el-card v-if="$slots.search" shadow="never" class="search-card">
      <el-form inline>
        <slot name="search" />
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">{{ title }}</span>
          <div v-if="$slots.actions" class="header-actions">
            <slot name="actions" />
          </div>
        </div>
      </template>
      <div class="table-responsive">
        <slot />
      </div>
      <div class="pagination-wrapper--sm">
        <el-pagination
          v-model:current-page="currentPageNum"
          v-model:page-size="currentPageSize"
          :total="total"
          :page-sizes="pageSizes"
          layout="total, sizes, prev, pager, next"
          @size-change="onSizeChange"
          @current-change="$emit('page-change')"
        />
      </div>
    </el-card>
  </component>
</template>

<script setup>
import { computed } from 'vue'
import { PAGE_SIZES } from '@/utils/appConfig'

const props = defineProps({
  title: { type: String, default: '' },
  nested: { type: Boolean, default: false },
  pageNum: { type: Number, default: 1 },
  pageSize: { type: Number, default: PAGE_SIZES[0] },
  total: { type: Number, default: 0 },
  pageSizes: { type: Array, default: () => PAGE_SIZES }
})

const emit = defineEmits(['update:pageNum', 'update:pageSize', 'page-change'])

const currentPageNum = computed({
  get: () => props.pageNum,
  set: (val) => emit('update:pageNum', val)
})

const currentPageSize = computed({
  get: () => props.pageSize,
  set: (val) => emit('update:pageSize', val)
})

function onSizeChange() {
  emit('update:pageNum', 1)
  emit('page-change')
}
</script>
