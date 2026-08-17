<template>
  <div class="page-view" v-loading="loading">
    <template v-if="room">
      <!-- 基础信息卡片 -->
      <div class="detail-card">
        <div class="detail-card-header">
          <div class="header-left">
            <el-button class="back-btn" @click="router.back()">
              <el-icon><ArrowLeft /></el-icon>
              <span>返回</span>
            </el-button>
            <span class="header-title">会议室详情</span>
          </div>
          <div class="header-actions">
            <el-button type="primary" plain :disabled="room.status !== 1" @click="showReserveDialog">
              <el-icon><Calendar /></el-icon>
              <span>立即预约</span>
            </el-button>
          </div>
        </div>

        <div class="detail-card-body">
          <div class="card-header">
            <div class="room-icon"><el-icon :size="28"><OfficeBuilding /></el-icon></div>
            <div class="room-info">
              <h2>{{ room.name }}</h2>
              <p class="room-location"><el-icon><Location /></el-icon> {{ room.location || '暂无位置' }}</p>
            </div>
            <el-tag :type="roomStatusType(room)" size="small" effect="light" round>
              {{ roomStatusText(room) }}
            </el-tag>
          </div>

          <div class="info-grid">
            <div class="info-item"><span class="info-label">容纳人数</span><span class="info-value">{{ room.capacity || '-' }} 人</span></div>
            <div class="info-item"><span class="info-label">设备设施</span><span class="info-value">{{ room.equipment || '暂无' }}</span></div>
            <div class="info-item"><span class="info-label">可预约时段</span><span class="info-value">{{ room.bookableStart || '08:00' }} ~ {{ room.bookableEnd || '20:00' }}</span></div>
            <div class="info-item"><span class="info-label">最小预约时长</span><span class="info-value">{{ room.minDuration > 0 ? room.minDuration + ' 分钟' : '不限制' }}</span></div>
            <div class="info-item"><span class="info-label">最大预约时长</span><span class="info-value">{{ room.maxDuration || 480 }} 分钟</span></div>
            <div class="info-item"><span class="info-label">提前预约</span><span class="info-value">最多 {{ room.advanceDays || 7 }} 天</span></div>
            <div class="info-item"><span class="info-label">审批模式</span><el-tag :type="room.needApproval === 1 ? 'warning' : 'success'" size="small" effect="light">{{ room.needApproval === 1 ? '需审批' : '免审批' }}</el-tag></div>
          </div>

          <div v-if="room.description" class="description">
            <h4>会议室描述</h4>
            <p>{{ room.description }}</p>
          </div>
        </div>
      </div>

      <!-- 预约日历 -->
      <div class="detail-card">
        <div class="detail-card-body">
          <h4 class="section-title">预约日历</h4>
          <RoomScheduleView ref="calendarRef" :room-id="room.id" />
        </div>
      </div>
    </template>

    <!-- 预约弹窗 -->
    <BookingDialog
      v-model="reserveDialogVisible"
      :room="room"
      :room-id="room?.id"
      :date="bookingDate"
      :start-time="bookingStartTime"
      :end-time="bookingEndTime"
      @success="onBookingSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, OfficeBuilding, Location, Calendar } from '@element-plus/icons-vue'
import { getRoomById } from '@/api/meeting'
import RoomScheduleView from '@/components/RoomScheduleView.vue'
import BookingDialog from '@/components/BookingDialog.vue'
import type { MeetingRoom } from '@/types/meeting'

const route = useRoute()
const router = useRouter()
const room = ref<MeetingRoom | null>(null)
const loading = ref(false)
const calendarRef = ref<InstanceType<typeof RoomScheduleView>>()

const reserveDialogVisible = ref(false)
const bookingDate = ref('')
const bookingStartTime = ref('')
const bookingEndTime = ref('')

function roomStatusType(r: MeetingRoom): 'success' | 'warning' | 'info' {
  if (r.status !== 1) return 'info'
  return r.currentAvailable ? 'success' : 'warning'
}
function roomStatusText(r: MeetingRoom): string {
  if (r.status !== 1) return '禁用'
  return r.currentAvailable ? '空闲' : '使用中'
}

function showReserveDialog() {
  const today = new Date()
  const dateStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0')
  bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T09:00:00`
  bookingEndTime.value = `${dateStr}T10:00:00`
  reserveDialogVisible.value = true
}

function onBookingSuccess() {
  calendarRef.value?.loadData()
}

onMounted(async () => {
  loading.value = true
  try { const res = await getRoomById(String(route.params.id)); room.value = res.data } catch { /* */ } finally { loading.value = false }
})
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.room-icon { width: 48px; height: 48px; border-radius: 12px; background: linear-gradient(135deg, #667eea, #764ba2); display: flex; align-items: center; justify-content: center; color: #fff; }
.room-info h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 0; }
.room-location { font-size: 13px; color: var(--text-secondary); margin: 4px 0 0 0; display: flex; align-items: center; gap: 4px; }

.info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; padding: 16px 0; border-top: 1px solid var(--border-light); border-bottom: 1px solid var(--border-light); }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 12px; color: var(--text-muted); }
.info-value { font-size: 14px; color: var(--text-primary); font-weight: 500; }

.description { margin-top: 16px; }
.description h4 { font-size: 14px; font-weight: 600; color: var(--text-primary); margin: 0 0 8px 0; }
.description p { font-size: 13px; color: var(--text-secondary); line-height: 1.7; margin: 0; }
.section-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0 0 16px 0; }

@media (max-width: 768px) { .info-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
