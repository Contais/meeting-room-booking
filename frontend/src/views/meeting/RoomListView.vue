<template>
  <div class="page-view">
    <div class="page-header"><h2>会议室列表</h2></div>
    <FilterBar :model="filter" @search="applyFilter" @reset="resetFilter">
      <el-form-item label="搜索"><el-input v-model="filter.keyword" placeholder="请输入名称/位置" clearable @keyup.enter="applyFilter" /></el-form-item>
      <el-form-item label="最少人数"><el-input-number v-model="filter.minCapacity" :min="1" :max="1000" controls-position="right" style="width:200px" /></el-form-item>
    </FilterBar>

    <div class="table-card page-card">
      <div class="table-toolbar">
        <div class="table-toolbar-left">
          <span style="font-size:13px;color:var(--text-secondary)">共 {{ filteredRooms.length }} 间</span>
        </div>
        <div class="table-toolbar-right" style="display:flex;gap:4px">
          <div class="action-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'" title="列表视图"><el-icon><List /></el-icon></div>
          <div class="action-btn" :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'" title="卡片视图"><el-icon><Grid /></el-icon></div>
        </div>
      </div>

      <!-- 卡片视图 -->
      <div v-if="viewMode === 'card'" class="room-grid">
        <div v-for="room in filteredRooms" :key="room.id" class="room-card" @click="goDetail(room.id)">
          <div class="card-top">
            <div class="status-dot" :class="room.status === 1 ? 'active' : 'inactive'"></div>
            <el-tag :type="room.status === 1 ? 'success' : 'info'" size="small">{{ room.status === 1 ? '可用' : '禁用' }}</el-tag>
          </div>
          <div class="card-icon"><el-icon :size="28"><OfficeBuilding /></el-icon></div>
          <h3 class="card-title">{{ room.name }}</h3>
          <p class="card-location"><el-icon><Location /></el-icon> {{ room.location || '暂无位置' }}</p>
          <div class="card-tags">
            <span class="tag"><el-icon><User /></el-icon>{{ room.capacity || '-' }}人</span>
            <span v-if="room.equipment" class="tag"><el-icon><Monitor /></el-icon>{{ room.equipment }}</span>
          </div>
        </div>
        <div v-if="filteredRooms.length === 0 && !loading" class="empty-state"><el-icon :size="48" color="#cbd5e1"><OfficeBuilding /></el-icon><p>暂无符合条件的会议室</p></div>
      </div>

      <!-- 列表视图 -->
      <el-table v-if="viewMode === 'list'" :data="filteredRooms" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="location" label="位置" min-width="100" />
        <el-table-column prop="capacity" label="容量" width="70" align="center" />
        <el-table-column prop="equipment" label="设备" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="70" align="center"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '可用' : '禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center"><template #default="{ row }"><el-button type="primary" link size="small" @click="goDetail(row.id)">详情</el-button></template></el-table-column>
      </el-table>
      <div v-if="viewMode === 'list'" class="table-footer"><el-pagination :total="filteredRooms.length" layout="total, prev, pager, next" :page-size="10" /></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, User, Monitor, Location, List, Grid } from '@element-plus/icons-vue'
import FilterBar from '@/components/FilterBar.vue'
import { listActiveRooms } from '@/api/meeting'
import type { MeetingRoom } from '@/types/meeting'

const router = useRouter(); const rooms = ref<MeetingRoom[]>([]); const loading = ref(false); const viewMode = ref<'card' | 'list'>('card')
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
.room-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; padding: 16px 20px; }
.room-card { background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius); padding: 20px; cursor: pointer; transition: all 0.15s; display: flex; flex-direction: column; }
.room-card:hover { border-color: var(--primary); }
.card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; }
.status-dot.active { background: var(--success); box-shadow: 0 0 6px rgba(16,185,129,0.4); }
.status-dot.inactive { background: var(--info); }
.card-icon { width: 44px; height: 44px; border-radius: 10px; background: var(--primary-light); display: flex; align-items: center; justify-content: center; color: var(--primary); margin-bottom: 12px; }
.card-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0 0 6px 0; }
.card-location { font-size: 12px; color: var(--text-muted); margin: 0 0 12px 0; display: flex; align-items: center; gap: 4px; }
.card-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: auto; }
.tag { display: inline-flex; align-items: center; gap: 3px; font-size: 11px; color: var(--text-secondary); background: var(--border-light); padding: 3px 8px; border-radius: 4px; }
.empty-state { grid-column: 1 / -1; text-align: center; padding: 60px 0; color: var(--text-muted); }
.empty-state p { margin-top: 12px; font-size: 13px; }
</style>
