<template>
  <div class="table-toolbar-actions">
    <el-tooltip content="刷新">
      <el-button circle aria-label="刷新" @click="emit('refresh')">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </el-tooltip>

    <el-tooltip :content="sortTooltip">
      <el-button circle aria-label="排序" @click="toggleSort">
        <el-icon><component :is="sortIcon" /></el-icon>
      </el-button>
    </el-tooltip>

    <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'">
      <el-button circle aria-label="全屏" @click="toggleFullscreen">
        <el-icon><FullScreen /></el-icon>
      </el-button>
    </el-tooltip>

    <el-popover v-if="columns.length > 0" placement="bottom-end" trigger="click" :width="220">
      <template #reference>
        <el-button circle aria-label="列设置">
          <el-icon><Setting /></el-icon>
        </el-button>
      </template>
      <div class="column-settings">
        <div class="column-settings-title">显示列</div>
        <el-checkbox-group v-model="visibleColumnsModel" @change="handleColumnVisibilityChange">
          <el-checkbox v-for="item in columns" :key="item.key" :value="item.key">{{ item.label }}</el-checkbox>
        </el-checkbox-group>
      </div>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, SortUp, SortDown, FullScreen, Setting } from '@element-plus/icons-vue'
import type { SortOrder, TableColumnOption } from '@/types/table'

const props = withDefaults(defineProps<{
  fullscreenTarget?: HTMLElement | null
  sortOrder?: SortOrder
  columns?: TableColumnOption[]
  visibleColumns?: string[]
}>(), {
  fullscreenTarget: null,
  sortOrder: 'asc',
  columns: () => [],
  visibleColumns: () => []
})

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'update:sortOrder', value: SortOrder): void
  (e: 'update:visibleColumns', value: string[]): void
}>()

const isFullscreen = ref(false)
const sortTooltip = computed(() => props.sortOrder === 'asc' ? '升序' : '降序')
const sortIcon = computed(() => props.sortOrder === 'asc' ? SortUp : SortDown)
const visibleColumnsModel = computed({
  get: () => props.visibleColumns,
  set: (value) => emit('update:visibleColumns', value)
})

function toggleSort() {
  emit('update:sortOrder', props.sortOrder === 'asc' ? 'desc' : 'asc')
}

function handleColumnVisibilityChange(value: string[]) {
  if (value.length === 0 && props.columns.length > 0) {
    visibleColumnsModel.value = [props.columns[0].key]
    ElMessage.warning('至少保留一列')
  }
}

function onFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === props.fullscreenTarget
}

async function toggleFullscreen() {
  try {
    if (isFullscreen.value) {
      await document.exitFullscreen()
    } else if (props.fullscreenTarget) {
      await props.fullscreenTarget.requestFullscreen()
    }
  } catch {
    ElMessage.error('全屏切换失败，请检查浏览器权限')
  }
}

onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})
</script>

<style scoped>
.table-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.column-settings-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.column-settings .el-checkbox {
  display: flex;
  margin-right: 0;
}

.column-settings .el-checkbox + .el-checkbox {
  margin-top: 4px;
}
</style>
