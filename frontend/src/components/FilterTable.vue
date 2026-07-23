<template>
  <div class="filter-table-wrapper">
    <!-- 数据过滤 -->
    <div class="filter-card page-card" v-if="filters.length > 0">
      <el-form :inline="true" :model="filterModel" class="filter-form">
        <template v-for="(filter) in filters" :key="filter.prop">
          <el-form-item :label="filter.label" :span="filter.span || 1">
            <!-- Input -->
            <el-input
              v-if="filter.type === 'input'"
              v-model="filterModel[filter.prop]"
              :placeholder="filter.placeholder || filter.label"
              clearable
              :style="{ width: filter.width || '180px' }"
              @keyup.enter="$emit('search')"
            />
            <!-- Select -->
            <el-select
              v-else-if="filter.type === 'select'"
              v-model="filterModel[filter.prop]"
              :placeholder="filter.placeholder || `请选择${filter.label}`"
              clearable
              filterable
              :style="{ width: filter.width || '140px' }"
            >
              <el-option
                v-for="opt in filter.options"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <!-- Number -->
            <el-input-number
              v-else-if="filter.type === 'number'"
              v-model="filterModel[filter.prop]"
              :min="filter.min"
              :max="filter.max"
              :placeholder="filter.placeholder"
              controls-position="right"
              :style="{ width: filter.width || '140px' }"
            />
          </el-form-item>
        </template>
        <el-form-item class="filter-actions">
          <el-button type="primary" @click="$emit('search')">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作入口 + 数据列表 -->
    <div class="table-card page-card">
      <div v-if="$slots.actions" class="table-actions">
        <slot name="actions" />
      </div>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  filters: Array<{
    prop: string
    label: string
    type: 'input' | 'select' | 'number'
    placeholder?: string
    width?: string
    options?: Array<{ label: string; value: any }>
    min?: number
    max?: number
    span?: number
  }>
  filterModel: Record<string, any>
}>()

const emit = defineEmits(['search', 'reset'])

function handleReset() {
  for (const key of Object.keys(props.filterModel)) {
    props.filterModel[key] = undefined
  }
  emit('reset')
  emit('search')
}
</script>

<style scoped>
.filter-form { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 0; }
.filter-form :deep(.el-form-item) { margin-bottom: 0; margin-right: 16px; }
.filter-actions { margin-left: auto; }
.table-actions { display: flex; justify-content: flex-end; margin-bottom: 16px; }
</style>
