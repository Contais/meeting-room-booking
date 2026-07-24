<template>
  <div class="page-view">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-fields">
        <div class="search-item">
          <label>搜索</label>
          <el-input v-model="filter.keyword" placeholder="请输入名称/位置" clearable @keyup.enter="applyFilter" />
        </div>
        <div class="search-item">
          <label>最少人数</label>
          <el-input-number v-model="filter.minCapacity" :min="1" :max="1000" controls-position="right" />
        </div>
      </div>
      <div class="search-actions">
        <el-button @click="resetFilter">重置</el-button>
        <el-button type="primary" @click="applyFilter">查询</el-button>
      </div>
    </div>

    <!-- 卡片列表 -->
    <div class="room-grid" v-loading="loading">
      <div v-for="room in filteredRooms" :key="room.id" class="room-card" @click="goDetail(room.id)">
        <div class="card-top">
          <div class="status-dot" :class="room.status === 1 ? 'active' : 'inactive'"></div>
          <el-tag :type="room.status === 1 ? 'success' : 'info'" size="small" effect="light">{{ room.status === 1 ? '可用' : '禁用' }}</el-tag>
        </div>
        <div class="card-icon"><el-icon :size="28"><OfficeBuilding /></el-icon></div>
        <h3 class="card-title">{{ room.name }}</h3>
        <p class="card-location"><el-icon><Location /></el-icon> {{ room.location || '暂无位置' }}</p>
        <div class="card-tags">
          <span class="tag"><el-icon><User /></el-icon>{{ room.capacity || '-' }}人</span>
          <span v-if="room.equipment" class="tag"><el-icon><Monitor /></el-icon>{{ room.equipment }}</span>
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
import { OfficeBuilding, User, Monitor, Location } from '@element-plus/icons-vue'
import { listActiveRooms } from '@/api/meeting'
import type { MeetingRoom } from '@/types/meeting'

const router = useRouter()
const rooms = ref<MeetingRoom[]>([])
const loading = ref(false)
const filter = reactive({ keyword: '', minCapacity: undefined as number | undefined })

const filteredRooms = computed(() => rooms.value.filter(room => {
  if (filter.keyword && !room.name.includes(filter.keyword) && !(room.location || '').includes(filter.keyword)) return false
  if (filter.minCapacity && (!room.capacity || room.capacity < filter.minCapacity)) return false
  return true
}))

function goDetail(id: number) { router.push(`/meeting/rooms/${id}`) }
function applyFilter() {}
function resetFilter() { filter.keyword = ''; filter.minCapacity = undefined }

onMounted(async () => {
  loading.value = true
  try { const res = await listActiveRooms(); rooms.value = res.data } catch { /* */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }

/* 搜索栏 */
.search-bar {
  background: #fff; border-radius: 12px; padding: 20px 24px;
  display: flex; align-items: flex-end; justify-content: space-between;
  border: 1px solid #f0f0f0;
}
.search-fields { display: flex; gap: 24px; flex: 1; }
.search-item { display: flex; flex-direction: column; gap: 6px; }
.search-item label { font-size: 13px; color: #606266; font-weight: 500; }
.search-item :deep(.el-input) { width: 220px; }
.search-actions { display: flex; gap: 8px; }

/* 卡片网格 */
.room-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.room-card {
  background: #fff; border: 1px solid #f0f0f0; border-radius: 16px;
  padding: 20px; cursor: pointer; transition: all 0.15s;
  display: flex; flex-direction: column;
}
.room-card:hover { border-color: var(--primary); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }

.card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; }
.status-dot.active { background: var(--success); box-shadow: 0 0 6px rgba(16,185,129,0.4); }
.status-dot.inactive { background: var(--info); }

.card-icon {
  width: 48px; height: 48px; border-radius: 14px;
  background: var(--primary-light); display: flex; align-items: center;
  justify-content: center; color: var(--primary); margin-bottom: 12px;
}

.card-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0 0 6px 0; }
.card-location { font-size: 12px; color: var(--text-muted); margin: 0 0 12px 0; display: flex; align-items: center; gap: 4px; }

.card-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: auto; }
.tag {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 11px; color: var(--text-secondary);
  background: #f5f7fa; padding: 3px 8px; border-radius: 6px;
}

.empty-state { grid-column: 1 / -1; text-align: center; padding: 60px 0; color: var(--text-muted); }
.empty-state p { margin-top: 12px; font-size: 13px; }
</style>
