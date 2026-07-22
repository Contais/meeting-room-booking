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

    <div v-if="room" class="detail-layout">
      <!-- 左侧信息 -->
      <div class="detail-main">
        <div class="detail-card page-card">
          <div class="detail-header">
            <div class="detail-icon">
              <el-icon :size="32"><OfficeBuilding /></el-icon>
            </div>
            <div>
              <h3>{{ room.name }}</h3>
              <p class="location-text"><el-icon><Location /></el-icon> {{ room.location || '暂无位置信息' }}</p>
            </div>
            <div class="status-badge">
              <el-tag :type="room.status === 1 ? 'success' : 'info'" effect="dark" round size="large">
                {{ room.status === 1 ? '可用' : '禁用' }}
              </el-tag>
            </div>
          </div>

          <el-divider />

          <div class="info-grid">
            <div class="info-item">
              <div class="info-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
                <el-icon :size="18"><User /></el-icon>
              </div>
              <div>
                <span class="info-label">容纳人数</span>
                <span class="info-value">{{ room.capacity || '-' }} 人</span>
              </div>
            </div>
            <div class="info-item">
              <div class="info-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
                <el-icon :size="18"><Monitor /></el-icon>
              </div>
              <div>
                <span class="info-label">设备设施</span>
                <span class="info-value">{{ room.equipment || '暂无' }}</span>
              </div>
            </div>
          </div>

          <div v-if="room.description" class="description-section">
            <h4>会议室描述</h4>
            <p>{{ room.description }}</p>
          </div>
        </div>
      </div>

      <!-- 右侧操作 -->
      <div class="detail-sidebar">
        <div class="action-card page-card">
          <h4>预约此会议室</h4>
          <p class="action-desc">选择时间段，快速预约</p>
          <el-button type="primary" class="btn-gradient" size="large" block :disabled="room.status !== 1" @click="handleReserve">
            <el-icon><Calendar /></el-icon>
            立即预约
          </el-button>
          <p v-if="room.status !== 1" class="tip-text">该会议室当前不可预约</p>
        </div>

        <div class="info-card page-card">
          <h4>会议室信息</h4>
          <div class="info-list">
            <div class="info-row">
              <span class="info-key">位置</span>
              <span class="info-val">{{ room.location || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-key">容量</span>
              <span class="info-val">{{ room.capacity }} 人</span>
            </div>
            <div class="info-row">
              <span class="info-key">状态</span>
              <span class="info-val">{{ room.status === 1 ? '可用' : '禁用' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, OfficeBuilding, User, Monitor, Location, Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
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

function handleReserve() {
  ElMessage.info('预约功能即将上线')
}
</script>

<style scoped>
.room-detail { display: flex; flex-direction: column; gap: 20px; }

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
  align-items: start;
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

.detail-header h3 { font-size: 22px; font-weight: 600; color: #1a1a2e; margin: 0; }
.location-text { font-size: 14px; color: #9ca3af; margin: 4px 0 0 0; display: flex; align-items: center; gap: 4px; }
.status-badge { margin-left: auto; }

.info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
.info-item { display: flex; align-items: center; gap: 12px; }
.info-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.info-label { font-size: 12px; color: #9ca3af; display: block; }
.info-value { font-size: 15px; color: #1a1a2e; font-weight: 500; }

.description-section { margin-top: 20px; }
.description-section h4 { font-size: 14px; font-weight: 600; color: #374151; margin: 0 0 8px 0; }
.description-section p { font-size: 14px; color: #4b5563; line-height: 1.7; margin: 0; }

.action-card, .info-card { margin-bottom: 16px; }
.action-card h4, .info-card h4 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0 0 8px 0; }
.action-desc { font-size: 13px; color: #9ca3af; margin: 0 0 16px 0; }
.tip-text { font-size: 12px; color: #ef4444; margin: 8px 0 0 0; text-align: center; }

.info-list { margin-top: 12px; }
.info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f3f4f6; }
.info-row:last-child { border-bottom: none; }
.info-key { font-size: 13px; color: #9ca3af; }
.info-val { font-size: 13px; color: #374151; font-weight: 500; }

@media (max-width: 768px) {
  .detail-layout { grid-template-columns: 1fr; }
}
</style>
