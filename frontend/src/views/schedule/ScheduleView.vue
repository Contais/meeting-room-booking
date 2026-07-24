<template>
  <div class="page-view">
    <div class="page-header">
      <h2>日程视图</h2>
      <div class="schedule-controls">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="day">日</el-radio-button>
          <el-radio-button value="week">周</el-radio-button>
        </el-radio-group>
        <el-button-group size="small">
          <el-button @click="goPrev"><el-icon><ArrowLeft /></el-icon></el-button>
          <el-button @click="goToday">今天</el-button>
          <el-button @click="goNext"><el-icon><ArrowRight /></el-icon></el-button>
        </el-button-group>
        <span class="date-display">{{ dateDisplay }}</span>
      </div>
    </div>

    <div class="schedule-container page-card">
      <div class="schedule-grid" :style="{ gridTemplateColumns: '100px repeat(' + rooms.length + ', 1fr)' }">
        <div class="grid-header">时间</div>
        <div class="grid-header" v-for="room in rooms" :key="room.id">{{ room.name }}</div>
        <template v-for="hour in timeSlots" :key="hour">
          <div class="time-label">{{ String(hour).padStart(2, '0') }}:00</div>
          <div class="time-cell" v-for="room in rooms" :key="hour + '-' + room.id"></div>
        </template>
        <div v-for="r in reservations" :key="r.id"
          class="reservation-block"
          :class="statusClass(r.status)"
          :style="blockStyle(r)"
          @click="showDetail(r)">
          <div class="block-subject">{{ r.subject || '未命名' }}</div>
          <div class="block-time">{{ formatTime(r.startTime) }}-{{ formatTime(r.endTime) }}</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="预约详情" width="400px">
      <el-descriptions :column="1" border v-if="currentReservation">
        <el-descriptions-item label="会议室">{{ currentReservation.roomName }}</el-descriptions-item>
        <el-descriptions-item label="主题">{{ currentReservation.subject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间段">{{ formatTime(currentReservation.startTime) }} - {{ formatTime(currentReservation.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="参会人数">{{ currentReservation.attendeeCount || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(currentReservation.status)" size="small">{{ statusText(currentReservation.status) }}</el-tag></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getSchedule } from '@/api/reservation'

const viewMode = ref('day')
const currentDate = ref(new Date())
const rooms = ref<{ id: number; name: string; capacity: number }[]>([])
const reservations = ref<{ id: number; roomId: number; roomName: string; subject: string; startTime: string; endTime: string; status: number; attendeeCount: number }[]>([])
const detailVisible = ref(false)
const currentReservation = ref<any>(null)

const START_HOUR = 8
const END_HOUR = 20
const HOUR_HEIGHT = 60

const timeSlots = computed(() => {
  const slots = []
  for (let h = START_HOUR; h < END_HOUR; h++) slots.push(h)
  return slots
})

const dateDisplay = computed(() => {
  const d = currentDate.value
  const fmt = (date: Date) => date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0')
  if (viewMode.value === 'day') return fmt(d)
  const weekEnd = new Date(d)
  weekEnd.setDate(weekEnd.getDate() + 6)
  return fmt(d) + ' ~ ' + fmt(weekEnd)
})

async function loadData() {
  const d = currentDate.value
  const fmt = (date: Date) => date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0')
  let params: Record<string, string> = {}
  if (viewMode.value === 'day') {
    params.date = fmt(d)
  } else {
    const weekEnd = new Date(d)
    weekEnd.setDate(weekEnd.getDate() + 6)
    params.startDate = fmt(d)
    params.endDate = fmt(weekEnd)
  }
  try {
    const res = await getSchedule(params)
    rooms.value = res.data.rooms || []
    reservations.value = res.data.reservations || []
  } catch { /* */ }
}

function goPrev() {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'day') d.setDate(d.getDate() - 1)
  else d.setDate(d.getDate() - 7)
  currentDate.value = d
}

function goNext() {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'day') d.setDate(d.getDate() + 1)
  else d.setDate(d.getDate() + 7)
  currentDate.value = d
}

function goToday() { currentDate.value = new Date() }

function blockStyle(r: any) {
  const roomIndex = rooms.value.findIndex(room => room.id === r.roomId)
  if (roomIndex < 0) return { display: 'none' }
  const start = new Date(r.startTime)
  const end = new Date(r.endTime)
  const top = (start.getHours() - START_HOUR + start.getMinutes() / 60) * HOUR_HEIGHT
  const height = Math.max(((end.getTime() - start.getTime()) / 3600000) * HOUR_HEIGHT, 24)
  return {
    top: top + 'px',
    height: height + 'px',
    gridColumn: (roomIndex + 2),
  }
}

function statusClass(status: number) {
  return { 0: 'status-pending', 1: 'status-confirmed', 2: 'status-cancelled' }[status] || ''
}

function statusType(status: number): string {
  return { 0: 'warning', 1: 'success', 2: 'info' }[status] || 'info'
}

function statusText(status: number) {
  return { 0: '待确认', 1: '已确认', 2: '已取消' }[status] || '未知'
}

function formatTime(t: string) { return t ? t.replace('T', ' ').substring(11, 16) : '' }
function showDetail(r: any) { currentReservation.value = r; detailVisible.value = true }

watch([viewMode, currentDate], loadData)
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-header h2 { margin: 0; }
.schedule-controls { display: flex; align-items: center; gap: 12px; }
.date-display { font-size: 14px; color: var(--text-secondary); font-weight: 500; }

.schedule-container { overflow-x: auto; padding: 0; }
.schedule-grid { display: grid; position: relative; min-height: 720px; }

.grid-header {
  padding: 10px 8px; font-size: 12px; font-weight: 600; color: var(--text-secondary);
  background: #fafbfc; border-bottom: 1px solid var(--border-light); border-right: 1px solid var(--border-light);
  text-align: center; position: sticky; top: 0; z-index: 2;
}

.time-label {
  padding: 0 8px; font-size: 11px; color: var(--text-muted);
  border-right: 1px solid var(--border-light); border-bottom: 1px solid #f0f0f0;
  display: flex; align-items: flex-start; justify-content: flex-end; height: 60px;
}

.time-cell {
  border-right: 1px solid #f0f0f0; border-bottom: 1px solid #f0f0f0; height: 60px;
}

.reservation-block {
  position: absolute; left: 0; right: 0; margin: 1px 4px; border-radius: 6px;
  padding: 4px 8px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 1;
  transition: box-shadow 0.15s;
}
.reservation-block:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }

.status-pending { background: #fef3cd; border-left: 3px solid #f59e0b; }
.status-confirmed { background: #d1fae5; border-left: 3px solid #10b981; }
.status-cancelled { background: #f3f4f6; border-left: 3px solid #9ca3af; }

.block-subject { font-weight: 600; color: #374151; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.block-time { color: #6b7280; margin-top: 2px; }
</style>
