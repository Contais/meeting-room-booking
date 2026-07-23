<template>
  <div class="page-view">
    <div class="page-header"><h2>会议室列表</h2></div>

    <FilterBar :model="filter" @search="applyFilter" @reset="resetFilter">
      <el-form-item label="搜索">
        <el-input v-model="filter.keyword" placeholder="名称 / 位置" clearable style="width: 220px" @keyup.enter="applyFilter" />
      </el-form-item>
      <el-form-item label="最少人数">
        <el-input-number v-model="filter.minCapacity" :min="1" :max="1000" controls-position="right" style="width: 130px" />
      </el-form-item>
    </FilterBar>

    <div class="room-grid">
      <div v-for="room in filteredRooms" :key="room.id" class="room-card" @click="goDetail(room.id)">
        <div class="card-top">
          <div class="status-dot" :class="room.status === 1 ? 'active' : 'inactive'"></div>
          <el-tag :type="room.status === 1 ? 'success' : 'info'" size="small" effect="dark" round>{{ room.status === 1 ? '可用' : '禁用' }}</el-tag>
        </div>
        <div class="card-icon"><el-icon :size="28"><OfficeBuilding /></el-icon></div>
        <h3 class="card-title">{{ room.name }}</h3>
        <p class="card-location"><el-icon><Location /></el-icon> {{ room.location || '暂无位置' }}</p>
        <div class="card-tags">
          <span class="tag"><el-icon><User /></el-icon>{{ room.capacity || '-' }}人</span>
          <span v-if="room.equipment" class="tag"><el-icon><Monitor /></el-icon>{{ room.equipment }}</span>
        </div>
        <div class="card-footer">
          <el-button type="primary" size="small" round class="btn-gradient" @click.stop="goDetail(room.id)">查看详情</el-button>
        </div>
      </div>
      <div v-if="filteredRooms.length === 0 && !loading" class="empty-state"><el-icon :size="48" color="#cbd5e1"><OfficeBuilding /></el-icon><p>暂无符合条件的会议室</p></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, User, Monitor, Location } from '@element-plus/icons-vue'
import FilterBar from '@/components/FilterBar.vue'
import { listActiveRooms } from '@/api/meeting'
import type { MeetingRoom } from '@/types/meeting'

const router = useRouter(); const rooms = ref<MeetingRoom[]>([]); const loading = ref(false)
const filter = reactive({ keyword: '', minCapacity: undefined as number | undefined })
const filteredRooms = computed(() => rooms.value.filter(room => {
  if (filter.keyword && !room.name.includes(filter.keyword) && !(room.location || '').includes(filter.keyword)) return false
  if (filter.minCapacity && (!room.capacity || room.capacity < filter.minCapacity)) return false
  return true
}))
function goDetail(id: number) { router.push(`/meeting/rooms/${id}`) }
function applyFilter() {}
function resetFilter() { filter.keyword = ''; filter.minCapacity = undefined }
onMounted(async () => { loading.value = true; try { const res = await listActiveRooms(); rooms.value = res.data } catch { /* */ } finally { loading.value = false } })
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.room-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.room-card { background: #fff; border-radius: var(--radius-md); padding: 20px; cursor: pointer; transition: all 0.2s; box-shadow: var(--shadow-sm); border: 1px solid var(--border-light); display: flex; flex-direction: column; }
.room-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); border-color: var(--border); }
.card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; }
.status-dot.active { background: #10b981; box-shadow: 0 0 6px rgba(16,185,129,0.4); }
.status-dot.inactive { background: #94a3b8; }
.card-icon { width: 48px; height: 48px; border-radius: 12px; background: linear-gradient(135deg, rgba(102,126,234,0.08), rgba(118,75,162,0.08)); display: flex; align-items: center; justify-content: center; color: var(--primary); margin-bottom: 12px; }
.card-title { font-size: 16px; font-weight: 600; color: var(--text-primary); margin: 0 0 6px 0; }
.card-location { font-size: 12px; color: var(--text-muted); margin: 0 0 12px 0; display: flex; align-items: center; gap: 4px; }
.card-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 14px; }
.tag { display: inline-flex; align-items: center; gap: 3px; font-size: 11px; color: var(--text-secondary); background: var(--border-light); padding: 3px 8px; border-radius: 4px; }
.card-footer { margin-top: auto; padding-top: 12px; border-top: 1px solid var(--border-light); }
.empty-state { grid-column: 1 / -1; text-align: center; padding: 60px 0; color: var(--text-muted); }
.empty-state p { margin-top: 12px; font-size: 13px; }
</style>
