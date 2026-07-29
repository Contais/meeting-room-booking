<template>
  <div class="page-view">
    <!-- 搜索栏 -->
    <SearchBar @search="applyFilter" @reset="resetFilter">
      <template #collapsed>
        <el-input v-model="filter.keyword" placeholder="搜索会议室名称 / 位置" clearable @input="onSearchInput" @keyup.enter="applyFilter" />
      </template>
      <template #expanded>
        <div class="search-item"><label>会议室名称</label><el-input v-model="filter.keyword" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="applyFilter" /></div>
        <div class="search-item"><label>位置</label><el-input v-model="filter.location" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="applyFilter" /></div>
        <div class="search-item"><label>最少人数</label><el-input-number v-model="filter.minCapacity" :min="1" :max="1000" controls-position="right" @change="applyFilter" /></div>
        <div class="search-item"><label>当天状态</label>
          <el-select v-model="filter.availability" placeholder="全部" clearable @change="applyFilter">
            <el-option label="空闲" value="available" />
            <el-option label="使用中" value="busy" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </div>
      </template>
    </SearchBar>

    <div class="status-legend">
      <span class="legend-tip">状态说明（点击筛选）：</span>
      <button type="button" class="legend-item" :class="{ active: filter.availability === 'available' }" @click="toggleLegendFilter('available')">
        <span class="status-badge active"><span class="status-dot"></span>空闲</span>
        <span class="legend-desc">当前无进行中的会议</span>
      </button>
      <button type="button" class="legend-item" :class="{ active: filter.availability === 'busy' }" @click="toggleLegendFilter('busy')">
        <span class="status-badge busy"><span class="status-dot"></span>使用中</span>
        <span class="legend-desc">当前有进行中的会议</span>
      </button>
      <button type="button" class="legend-item" :class="{ active: filter.availability === 'inactive' }" @click="toggleLegendFilter('inactive')">
        <span class="status-badge inactive"><span class="status-dot"></span>禁用</span>
        <span class="legend-desc">会议室已停用</span>
      </button>
      <button v-if="filter.availability" type="button" class="legend-clear" @click="clearLegendFilter">清除筛选</button>
    </div>

    <!-- 卡片列表 -->
    <div class="room-grid" v-loading="loading">
      <div v-for="room in filteredRooms" :key="room.id" class="room-card" @click="goDetail(room.id)">
        <div class="card-header">
          <div class="card-title-row">
            <div class="card-icon">
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <div class="card-title-wrap">
              <h3 class="card-title">{{ room.name }}</h3>
              <p class="card-capacity-mini">容纳 {{ room.capacity || '-' }} 人</p>
            </div>
          </div>
          <div class="card-status-wrap">
            <span class="status-badge" :class="roomStatusClass(room)">
              <span class="status-dot"></span>
              {{ roomStatusText(room) }}
            </span>
          </div>
        </div>
        <div class="card-body">
          <div class="card-info-item">
            <el-icon class="info-icon"><Location /></el-icon>
            <span>{{ room.location || '暂无位置信息' }}</span>
          </div>
          <div v-if="room.equipment" class="card-info-item">
            <el-icon class="info-icon"><Monitor /></el-icon>
            <span>{{ room.equipment }}</span>
          </div>
        </div>
        <div class="card-footer">
          <span class="view-detail">查看详情</span>
          <el-icon class="arrow-icon"><Right /></el-icon>
        </div>
      </div>
      <div v-if="filteredRooms.length === 0 && !loading" class="empty-state">
        <el-icon :size="48" color="#cbd5e1"><OfficeBuilding /></el-icon>
        <p>暂无符合条件的会议室</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, Monitor, Location, Right } from '@element-plus/icons-vue'
import { listActiveRooms } from '@/api/meeting'
import SearchBar from '@/components/SearchBar.vue'
import type { MeetingRoom } from '@/types/meeting'

const router = useRouter()
const rooms = ref<MeetingRoom[]>([])
const loading = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() { if (searchTimer) clearTimeout(searchTimer); searchTimer = setTimeout(applyFilter, 300) }
const filter = reactive({ keyword: '', location: '', minCapacity: undefined as number | undefined, availability: '' as string })

const filteredRooms = computed(() => rooms.value.filter(room => {
  if (filter.keyword && !room.name.includes(filter.keyword)) return false
  if (filter.location && !(room.location || '').includes(filter.location)) return false
  if (filter.minCapacity && (!room.capacity || room.capacity < filter.minCapacity)) return false
  if (filter.availability) {
    const cls = roomStatusClass(room)
    if (cls !== filter.availability) return false
  }
  return true
}))

function goDetail(id: number) { router.push({ path: `/meeting/rooms/${id}`, query: { from: '/meeting/rooms', fromTitle: '会议室列表', dt: '会议室详情' } }) }

function roomStatusClass(room: MeetingRoom): string {
  if (room.status !== 1) return 'inactive'
  return room.currentAvailable ? 'active' : 'busy'
}
function roomStatusText(room: MeetingRoom): string {
  if (room.status !== 1) return '禁用'
  return room.currentAvailable ? '空闲' : '使用中'
}
function applyFilter() {
  // 客户端过滤通过 computed(filteredRooms) 自动响应；查询按钮提供显式触发入口
  // 此处强制触发一次响应式更新，确保用户期望的"点击查询"反馈
  rooms.value = [...rooms.value]
}
function resetFilter() { filter.keyword = ''; filter.location = ''; filter.minCapacity = undefined; filter.availability = '' }

function toggleLegendFilter(status: string) {
  filter.availability = filter.availability === status ? '' : status
}
function clearLegendFilter() { filter.availability = '' }

onMounted(async () => {
  loading.value = true
  try { const res = await listActiveRooms(); rooms.value = res.data } catch { /* */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.status-legend { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: var(--bg-card); border-radius: 8px; border: 1px solid var(--border-light); font-size: 12px; color: var(--text-muted); flex-wrap: wrap; }
.legend-tip { color: var(--text-muted); font-size: 12px; margin-right: 4px; }
.legend-item {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 10px; border-radius: 16px;
  background: transparent; border: 1px solid var(--border-light);
  cursor: pointer; transition: all 0.2s; font-size: 12px;
  color: var(--text-secondary);
}
.legend-item:hover { border-color: var(--primary); background: var(--primary-light, #f5f7ff); }
.legend-item.active { border-color: var(--primary); background: var(--primary-light, #ecf5ff); color: var(--primary); }
.legend-desc { color: var(--text-muted); font-size: 11px; }
.legend-clear {
  margin-left: auto; padding: 4px 10px; border-radius: 16px;
  background: transparent; border: 1px solid #f56c6c; color: #f56c6c;
  cursor: pointer; font-size: 12px; transition: all 0.2s;
}
.legend-clear:hover { background: #f56c6c; color: #fff; }

/* 卡片网格 */
.room-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.room-card {
  background: var(--bg-card);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-light);
}
.room-card:hover {
  border-color: var(--primary);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.08);
  transform: translateY(-2px);
}

/* 卡片头部 */
.card-header {
  padding: 18px 20px 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--primary-light, #eef0ff);
  color: var(--primary, #667eea);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.card-title-wrap {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 2px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-capacity-mini {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
}

.card-status-wrap {
  flex-shrink: 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}
.status-badge.active {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}
.status-badge.busy {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}
.status-badge.inactive {
  background: rgba(144, 147, 153, 0.1);
  color: #909399;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* 卡片内容 */
.card-body {
  padding: 0 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-icon {
  color: var(--text-muted);
  font-size: 14px;
  flex-shrink: 0;
}

/* 卡片底部 */
.card-footer {
  padding: 12px 20px 16px;
  border-top: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.25s ease;
}

.view-detail {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  transition: color 0.25s ease;
}
.room-card:hover .view-detail {
  color: var(--primary);
}

.arrow-icon {
  color: var(--text-muted);
  font-size: 14px;
  transition: all 0.25s ease;
}
.room-card:hover .arrow-icon {
  color: var(--primary);
  transform: translateX(3px);
}

.empty-state { grid-column: 1 / -1; text-align: center; padding: 80px 0; color: var(--text-muted); }
.empty-state p { margin-top: 12px; font-size: 14px; }
</style>
