<template>
  <div class="page-view" v-loading="loading">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px">
        <el-button text @click="router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      </div>
    </div>

    <template v-if="room">
      <!-- 基础信息卡片 -->
      <div class="detail-card">
        <div class="card-header">
          <div class="room-icon"><el-icon :size="28"><OfficeBuilding /></el-icon></div>
          <div class="room-info">
            <h2>{{ room.name }}</h2>
            <p class="room-location"><el-icon><Location /></el-icon> {{ room.location || '暂无位置' }}</p>
          </div>
          <el-tag :type="room.status === 1 ? 'success' : 'info'" size="small" effect="light" round>
            {{ room.status === 1 ? '可用' : '禁用' }}
          </el-tag>
        </div>

        <div class="info-grid">
          <div class="info-item"><span class="info-label">容纳人数</span><span class="info-value">{{ room.capacity || '-' }} 人</span></div>
          <div class="info-item"><span class="info-label">设备设施</span><span class="info-value">{{ room.equipment || '暂无' }}</span></div>
          <div class="info-item"><span class="info-label">可预约时段</span><span class="info-value">{{ room.bookableStart || '08:00' }} ~ {{ room.bookableEnd || '20:00' }}</span></div>
          <div class="info-item"><span class="info-label">最大预约时长</span><span class="info-value">{{ room.maxDuration || 480 }} 分钟</span></div>
          <div class="info-item"><span class="info-label">提前预约</span><span class="info-value">最多 {{ room.advanceDays || 7 }} 天</span></div>
          <div class="info-item"><span class="info-label">审批模式</span><el-tag :type="room.needApproval === 1 ? 'warning' : 'success'" size="small" effect="light">{{ room.needApproval === 1 ? '需审批' : '免审批' }}</el-tag></div>
        </div>

        <div v-if="room.description" class="description">
          <h4>会议室描述</h4>
          <p>{{ room.description }}</p>
        </div>

        <div class="action-row">
          <el-button type="primary" :disabled="room.status !== 1" @click="showReserveDialog">
            <el-icon><Calendar /></el-icon> 立即预约
          </el-button>
        </div>
      </div>

      <!-- 预约日历 -->
      <div class="detail-card">
        <h4 class="section-title">预约日历</h4>
        <RoomScheduleView ref="calendarRef" :room-id="room.id" @book="handleTimeSelect" />
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

function showReserveDialog() {
  const today = new Date()
  const dateStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0')
  bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T09:00:00`
  bookingEndTime.value = `${dateStr}T10:00:00`
  reserveDialogVisible.value = true
}

function handleTimeSelect(startTime: string, endTime: string) {
  bookingDate.value = startTime.substring(0, 10)
  bookingStartTime.value = startTime
  bookingEndTime.value = endTime
  reserveDialogVisible.value = true
}

function onBookingSuccess() {
  calendarRef.value?.loadData()
}

onMounted(async () => {
  loading.value = true
  try { const res = await getRoomById(Number(route.params.id)); room.value = res.data } catch { /* */ } finally { loading.value = false }
})
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.page-header { margin-bottom: 0; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }

.detail-card { background: #fff; border-radius: 12px; border: 1px solid #f0f0f0; padding: 24px; }
.card-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.room-icon { width: 48px; height: 48px; border-radius: 12px; background: linear-gradient(135deg, #667eea, #764ba2); display: flex; align-items: center; justify-content: center; color: #fff; }
.room-info h2 { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.room-location { font-size: 13px; color: #909399; margin: 4px 0 0 0; display: flex; align-items: center; gap: 4px; }

.info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; padding: 16px 0; border-top: 1px solid #f5f5f5; border-bottom: 1px solid #f5f5f5; }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 12px; color: #909399; }
.info-value { font-size: 14px; color: #303133; font-weight: 500; }

.description { margin-top: 16px; }
.description h4 { font-size: 14px; font-weight: 600; color: #303133; margin: 0 0 8px 0; }
.description p { font-size: 13px; color: #606266; line-height: 1.7; margin: 0; }
.action-row { margin-top: 20px; display: flex; justify-content: flex-end; }
.section-title { font-size: 15px; font-weight: 600; color: #303133; margin: 0 0 16px 0; }

.dialog-form-item { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.dialog-form-item label { font-size: 13px; color: #606266; font-weight: 500; }

.dialog-rules-tip { background: #ecf5ff; border-radius: 8px; padding: 10px 14px; margin-bottom: 16px; font-size: 13px; color: #409eff; display: flex; align-items: center; gap: 6px; }

/* 时间段选择器 */
.time-selector { border: 1px solid #dcdfe6; border-radius: 8px; overflow: hidden; }
.time-selector-header { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: #fafbfc; border-bottom: 1px solid #ebeef5; font-size: 13px; color: #606266; }
.time-options { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; padding: 10px 12px; max-height: 140px; overflow-y: auto; }
.time-option { text-align: center; }
.time-option {
  padding: 6px 12px; border-radius: 6px; border: 1px solid #dcdfe6;
  font-size: 13px; color: #606266; cursor: pointer; transition: all 0.15s;
}
.time-option:hover { border-color: #409eff; color: #409eff; }
.time-option.active { background: #409eff; color: #fff; border-color: #409eff; }
.time-option.in-range { background: #ecf5ff; border-color: #b3d8ff; color: #409eff; }
.time-range-text { font-size: 14px; font-weight: 500; color: #303133; }
.time-range-text.placeholder { color: #c0c4cc; font-weight: 400; }

@media (max-width: 768px) { .info-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
