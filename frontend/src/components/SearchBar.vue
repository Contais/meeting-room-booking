<template>
  <div class="search-bar">
    <div class="search-fields">
      <!-- 折叠态：单行关键字搜索 -->
      <div v-if="!expanded" class="search-collapsed">
        <el-icon class="search-icon"><Search /></el-icon>
        <slot name="collapsed" />
      </div>
      <!-- 展开态：多字段网格 -->
      <div v-else class="search-expanded">
        <slot name="expanded" />
      </div>
    </div>
    <div class="search-actions">
      <el-tooltip content="清空所有筛选条件">
        <el-button @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          <span>重置</span>
        </el-button>
      </el-tooltip>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        <span>查询</span>
      </el-button>
      <el-button link type="primary" class="expand-toggle" @click="toggleExpand">
        <span>{{ expanded ? '收起' : '展开' }}</span>
        <el-icon class="toggle-icon" :class="{ 'is-expanded': expanded }"><ArrowDown /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search, ArrowDown, RefreshLeft } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  defaultExpanded?: boolean
}>(), { defaultExpanded: false })

const emit = defineEmits<{
  (e: 'search'): void
  (e: 'reset'): void
  (e: 'expand-change', expanded: boolean): void
}>()

const expanded = ref(props.defaultExpanded)
function toggleExpand() {
  expanded.value = !expanded.value
  emit('expand-change', expanded.value)
}
function handleSearch() { emit('search') }
function handleReset() { emit('reset') }
</script>

<style scoped>
.search-bar {
  background: #fff;
  border-radius: 12px;
  padding: 18px 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease;
}
.search-fields { flex: 1; min-width: 0; }
.search-collapsed {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.search-icon { color: #909399; font-size: 16px; flex-shrink: 0; }
.search-collapsed > :deep(.el-input),
.search-collapsed > :deep(.el-select),
.search-collapsed > :deep(.el-date-editor) { flex: 1; width: auto; }

.search-expanded {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px 20px;
  align-items: end;
}
@media (max-width: 1100px) {
  .search-expanded { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .search-expanded { grid-template-columns: 1fr; }
}

.search-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}
.expand-toggle { padding: 0 8px; }
.toggle-icon { transition: transform 0.2s ease; }
.toggle-icon.is-expanded { transform: rotate(180deg); }
</style>
