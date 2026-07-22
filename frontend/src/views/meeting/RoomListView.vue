<template>
  <div class="room-list">
    <div class="page-header">
      <h2>会议室列表</h2>
    </div>

    <div class="room-grid">
      <div v-for="room in rooms" :key="room.id" class="room-card" @click="goDetail(room.id)">
        <div class="room-card-header">
          <div class="room-status-dot" :class="room.status === 1 ? 'active' : 'inactive'"></div>
          <el-tag :type="room.status === 1 ? 'success' : 'info'" size="small" effect="dark" round>
            {{ room.status === 1 ? '可用' : '禁用' }}
          </el-tag>
        </div>
        <div class="room-card-icon">
          <el-icon :size="32"><OfficeBuilding /></el-icon>
        </div>
        <h3 class="room-name">{{ room.name }}</h3>
        <p class="room-location">{{ room.location || '暂无位置' }}</p>
        <div class="room-meta">
          <span><el-icon><User /></el-icon> {{ room.capacity || '-' }}人</span>
          <span><el-icon><Monitor /></el-icon> {{ room.equipment || '暂无设备' }}</span>
        </div>
      </div>

      <div v-if="rooms.length === 0 && !loading" class="empty-state">
        <el-icon :size="48" color="#d1d5db"><OfficeBuilding /></el-icon>
        <p>暂无会议室</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, User, Monitor } from '@element-plus/icons-vue'
import { listRooms } from '@/api/meeting'
import type { MeetingRoom } from '@/types/meeting'

const router = useRouter()
const rooms = ref<MeetingRoom[]>([])
const loading = ref(false)

function goDetail(id: number) {
  router.push(`/meeting/rooms/${id}`)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await listRooms()
    rooms.value = res.data
  } catch { /* */ } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.room-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.room-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
}

.room-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(102, 126, 234, 0.15);
}

.room-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.room-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.room-status-dot.active {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.4);
}

.room-status-dot.inactive {
  background: #9ca3af;
}

.room-card-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
  margin-bottom: 16px;
}

.room-name {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 6px 0;
}

.room-location {
  font-size: 13px;
  color: #9ca3af;
  margin: 0 0 16px 0;
}

.room-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #6b7280;
}

.room-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
  color: #9ca3af;
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}
</style>
