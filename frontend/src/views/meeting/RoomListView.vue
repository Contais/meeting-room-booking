<template>
  <div class="room-list">
    <div class="page-header"><h2>会议室列表</h2></div>

    <FilterBar :model="filter" @search="applyFilter" @reset="resetFilter">
      <el-form-item label="搜索">
        <el-input v-model="filter.keyword" placeholder="名称/位置" clearable style="width: 220px" @keyup.enter="applyFilter" />
      </el-form-item>
      <el-form-item label="最少人数">
        <el-input-number v-model="filter.minCapacity" :min="1" :max="1000" controls-position="right" style="width: 130px" />
      </el-form-item>
    </FilterBar>

    <div class="room-grid">
      <div v-for="room in filteredRooms" :key="room.id" class="room-card" @click="goDetail(room.id)">
        <div class="room-card-header">
          <div class="room-status-dot" :class="room.status === 1 ? 'active' : 'inactive'"></div>
          <el-tag :type="room.status === 1 ? 'success' : 'info'" size="small" effect="dark" round>{{ room.status === 1 ? '可用' : '禁用' }}</el-tag>
        </div>
        <div class="room-card-icon"><el-icon :size="32"><OfficeBuilding /></el-icon></div>
        <h3 class="room-name">{{ room.name }}</h3>
        <p class="room-location"><el-icon><Location /></el-icon> {{ room.location || '暂无位置' }}</p>
        <div class="room-meta">
          <span class="meta-item"><el-icon><User /></el-icon> {{ room.capacity || '-' }}人</span>
          <span class="meta-item" v-if="room.equipment"><el-icon><Monitor /></el-icon> {{ room.equipment }}</span>
        </div>
        <div class="room-actions"><el-button type="primary" size="small" round class="btn-gradient" @click.stop="goDetail(room.id)">查看详情</el-button></div>
      </div>
      <div v-if="filteredRooms.length === 0 && !loading" class="empty-state"><el-icon :size="48" color="#d1d5db"><OfficeBuilding /></el-icon><p>暂无符合条件的会议室</p></div>
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
.room-list { display: flex; flex-direction: column; gap: 20px; }
.room-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
.room-card { background: #fff; border-radius: 14px; padding: 24px; cursor: pointer; transition: all 0.25s; box-shadow: 0 2px 12px rgba(0,0,0,0.06); display: flex; flex-direction: column; }
.room-card:hover { transform: translateY(-4px); box-shadow: 0 8px 30px rgba(102,126,234,0.15); }
.room-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.room-status-dot { width: 8px; height: 8px; border-radius: 50%; }
.room-status-dot.active { background: #10b981; box-shadow: 0 0 8px rgba(16,185,129,0.4); }
.room-status-dot.inactive { background: #9ca3af; }
.room-card-icon { width: 56px; height: 56px; border-radius: 14px; background: linear-gradient(135deg, rgba(102,126,234,0.1), rgba(118,75,162,0.1)); display: flex; align-items: center; justify-content: center; color: #667eea; margin-bottom: 16px; }
.room-name { font-size: 18px; font-weight: 600; color: #1a1a2e; margin: 0 0 8px 0; }
.room-location { font-size: 13px; color: #9ca3af; margin: 0 0 16px 0; display: flex; align-items: center; gap: 4px; }
.room-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.meta-item { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #6b7280; background: #f9fafb; padding: 4px 10px; border-radius: 6px; }
.room-actions { margin-top: auto; padding-top: 12px; border-top: 1px solid #f3f4f6; }
.empty-state { grid-column: 1 / -1; text-align: center; padding: 60px 0; color: #9ca3af; }
.empty-state p { margin-top: 12px; font-size: 14px; }
</style>
