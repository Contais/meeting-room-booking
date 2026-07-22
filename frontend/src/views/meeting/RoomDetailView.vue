<template>
  <div class="room-detail" v-loading="loading">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2>{{ room?.name || '会议室详情' }}</h2>
      </div>
    </div>

    <div v-if="room" class="detail-content">
      <div class="detail-card page-card">
        <div class="detail-header">
          <div class="detail-icon">
            <el-icon :size="32"><OfficeBuilding /></el-icon>
          </div>
          <div>
            <h3>{{ room.name }}</h3>
            <p>{{ room.location || '暂无位置信息' }}</p>
          </div>
        </div>

        <el-divider />

        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">容纳人数</span>
            <span class="info-value">{{ room.capacity || '-' }} 人</span>
          </div>
          <div class="info-item">
            <span class="info-label">状态</span>
            <el-tag :type="room.status === 1 ? 'success' : 'info'" effect="dark" round>
              {{ room.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
          <div class="info-item">
            <span class="info-label">设备设施</span>
            <span class="info-value">{{ room.equipment || '暂无' }}</span>
          </div>
        </div>

        <el-divider v-if="room.description" />

        <div v-if="room.description" class="description">
          <span class="info-label">描述</span>
          <p>{{ room.description }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, OfficeBuilding } from '@element-plus/icons-vue'
import { getRoomById } from '@/api/meeting'
import type { MeetingRoom } from '@/types/meeting'

const route = useRoute()
const router = useRouter()
const room = ref<MeetingRoom | null>(null)
const loading = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const res = await getRoomById(id)
    room.value = res.data
  } catch { /* */ } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.room-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-card {
  max-width: 700px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.detail-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 4px 0;
}

.detail-header p {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 12px;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 15px;
  color: #1a1a2e;
  font-weight: 500;
}

.description p {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.7;
  margin-top: 8px;
}
</style>
