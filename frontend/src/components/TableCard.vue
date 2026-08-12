<template>
  <div class="table-card">
    <div v-if="$slots['toolbar-left'] || $slots['toolbar-right']" class="table-toolbar">
      <div class="toolbar-left"><slot name="toolbar-left" /></div>
      <div class="toolbar-right"><slot name="toolbar-right" /></div>
    </div>
    <slot />
    <div v-if="showPagination" class="pagination-wrap">
      <span class="total-text">共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="pageModel"
        v-model:page-size="sizeModel"
        :total="total"
        :page-sizes="pageSizes"
        background
        layout="prev, pager, next, sizes, jumper"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'

const props = withDefaults(defineProps<{
  total: number
  page?: number
  size?: number
  pageSizes?: number[]
  showPagination?: boolean
}>(), {
  page: 1,
  size: 10,
  pageSizes: () => [10, 20, 50],
  showPagination: true,
})

const emit = defineEmits<{
  (e: 'update:page', value: number): void
  (e: 'update:size', value: number): void
  (e: 'size-change', value: number): void
  (e: 'current-change', value: number): void
}>()

const pageModel = computed({
  get: () => props.page,
  set: (val: number) => emit('update:page', val),
})
const sizeModel = computed({
  get: () => props.size,
  set: (val: number) => emit('update:size', val),
})

// v-model:current-page / v-model:page-size 已处理双向绑定，
// 此处通过 watch 向父组件透传 current-change / size-change 事件
watch(pageModel, (val) => emit('current-change', val))
watch(sizeModel, (val) => emit('size-change', val))
</script>
