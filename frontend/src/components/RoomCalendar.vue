<template>
  <div class="room-calendar">
    <!-- 控制栏 -->
    <div class="calendar-controls">
      <el-button-group size="small">
        <el-button @click="goToday">今天</el-button>
        <el-button @click="goPrev"><el-icon><ArrowLeft /></el-icon></el-button>
        <el-button @click="goNext"><el-icon><ArrowRight /></el-icon></el-button>
      </el-button-group>
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button value="day">日</el-radio-button>
        <el-radio-button value="week">周</el-radio-button>
      </el-radio-group>
      <span class="date-display">{{ dateDisplay }}</span>
    </div>

    <!-- 日视图 -->
    <div v-if="viewMode === 'day'" class="day-view">
      <div class="time-header">
        <div class="time-col-header">时间</div>
        <div class="status-col-header">预约状态</div>
      </div>
      <div class="time-slots">
        <div v-for="slot in timeSlots" :key="slot.time" 
          class="time-slot" :class="slot.status"
          @click="handleSlotClick(slot)">
          <div class="slot-time">{{ slot.time }}</div>
          <div class="slot-content">
            <template v-if="slot.reservation">
              <div class="slot-subject">{{ slot.reservation.subject || '未命名' }}</div>
              <div class="slot-meta">{{ formatTime(slot.reservation.startTime) }}-{{ formatTime(slot.reservation.endTime) }}</div>
            </template>
            <span v-else-if="slot.status === 'available'" class="slot-available">可预约</span>
            <span v-else class="slot-past">已过</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 周视图 -->
    <div v-if="viewMode === 'week'" class="week-view">
      <div class="week-header">
        <div class="time-col-header"></div>
        <div v-for="d in weekDays" :key="d.dateStr" class="day-header" :class="{ today: d.isToday }">
          <div class="day-name">{{ d.dayName }}</div>
          <div class="day-date">{{ d.dayNum }}</div>
        </div>
      </div>
      <div class="week-body">
        <div v-for="h in dayHours" :key="h" class="time-row">
          <div class="time-label">{{ h }}:00</div>
          <div v-for="d in weekDays" :key="d.dateStr + '-' + h" 
            class="day-cell" :class="getCellClass(d.dateStr, h)"
            @click="onCellClick(d.dateStr, h)">
            <div v-if="getCellReservation(d.dateStr, h)" class="cell-event">
              {{ getCellReservation(d.dateStr, h)?.subject || '已预约' }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { listByRoomAndDate } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const props = defineProps<{
  roomId: string
  bookableStart?: string
  bookableEnd?: string
}>()

const emit = defineEmits<{
  (e: 'select', startTime: string, endTime: string): void
}>()

const viewMode = ref<'day' | 'week'>('day')
const currentDate = ref(new Date())
const reservations = ref<Reservation[]>([])

const START_HOUR = parseInt((props.bookableStart || '08:00').split(':')[0])
const END_HOUR = parseInt((props.bookableEnd || '20:00').split(':')[0])

const dayHours = computed(() => {
  const hours = []
  for (let h = START_HOUR; h < END_HOUR; h++) hours.push(h)
  return hours
})

const weekDays = computed(() => {
  const d = currentDate.value
  const startOfWeek = new Date(d)
  const day = startOfWeek.getDay()
  startOfWeek.setDate(startOfWeek.getDate() - day) // 从周日开始
  
  const today = new Date()
  const todayStr = formatDate(today)
  const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  
  return Array.from({ length: 7 }, (_, i) => {
    const date = new Date(startOfWeek)
    date.setDate(date.getDate() + i)
    const dateStr = formatDate(date)
    return {
      date,
      dateStr,
      dayName: dayNames[i],
      dayNum: date.getDate(),
      isToday: dateStr === todayStr
    }
  })
})

const dateDisplay = computed(() => {
  const d = currentDate.value
  const fmt = (date: Date) => `${date.getFullYear()}年${String(date.getMonth() + 1).padStart(2, '0')}月${String(date.getDate()).padStart(2, '0')}日`
  if (viewMode.value === 'day') return fmt(d)
  const end = new Date(d)
  end.setDate(end.getDate() + 6)
  return `${fmt(d)} ~ ${fmt(end)}`
})

// 日视图时间段
interface TimeSlot {
  time: string
  status: 'available' | 'occupied' | 'past'
  reservation: Reservation | null
}

const timeSlots = computed<TimeSlot[]>(() => {
  const slots: TimeSlot[] = []
  const dateStr = formatDate(currentDate.value)
  const now = new Date()
  
  for (let h = START_HOUR; h < END_HOUR; h++) {
    const time = `${String(h).padStart(2, '0')}:00`
    const slotStart = new Date(`${dateStr}T${time}:00`)
    const slotEnd = new Date(slotStart)
    slotEnd.setHours(slotEnd.getHours() + 1)
    
    const isPast = slotEnd <= now
    
    const reservation = reservations.value.find(r => {
      const rStart = new Date(r.startTime)
      const rEnd = new Date(r.endTime)
      return rStart < slotEnd && rEnd > slotStart
    })
    
    let status: TimeSlot['status'] = 'available'
    if (reservation) status = 'occupied'
    else if (isPast) status = 'past'
    
    slots.push({ time, status, reservation: reservation || null })
  }
  return slots
})

function formatDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').substring(11, 16) : ''
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

function goToday() {
  currentDate.value = new Date()
}

function handleSlotClick(slot: TimeSlot) {
  if (slot.status !== 'available') return
  const dateStr = formatDate(currentDate.value)
  const startTime = `${dateStr}T${slot.time}:00`
  const endHour = parseInt(slot.time.split(':')[0]) + 1
  const endTime = `${dateStr}T${String(endHour).padStart(2, '0')}:00`
  emit('select', startTime, endTime)
}

function onCellClick(dateStr: string, hour: number) {
  const startTime = `${dateStr}T${String(hour).padStart(2, '0')}:00`
  const endTime = `${dateStr}T${String(hour + 1).padStart(2, '0')}:00`
  emit('select', startTime, endTime)
}

function getCellClass(dateStr: string, hour: number) {
  const reservation = getCellReservation(dateStr, hour)
  if (reservation) return 'occupied'
  const now = new Date()
  const cellDate = new Date(`${dateStr}T${String(hour).padStart(2, '0')}:00`)
  if (cellDate < now) return 'past'
  return 'available'
}

function getCellReservation(dateStr: string, hour: number) {
  const slotStart = new Date(`${dateStr}T${String(hour).padStart(2, '0')}:00`)
  const slotEnd = new Date(slotStart)
  slotEnd.setHours(slotEnd.getHours() + 1)
  
  return reservations.value.find(r => {
    const rStart = new Date(r.startTime)
    const rEnd = new Date(r.endTime)
    return rStart < slotEnd && rEnd > slotStart
  }) || null
}

async function loadReservations() {
  try {
    const dateStr = viewMode.value === 'day' 
      ? formatDate(currentDate.value)
      : formatDate(weekDays.value[0].date)
    
    if (viewMode.value === 'week') {
      // 周视图需要加载整周数据
      const endDate = formatDate(weekDays.value[6].date)
      const startRes = await listByRoomAndDate(props.roomId, dateStr)
      const endRes = await listByRoomAndDate(props.roomId, endDate)
      const allReservations = [...startRes.data, ...endRes.data]
      // 去重
      const uniqueIds = new Set<string>()
      reservations.value = allReservations.filter(r => {
        if (uniqueIds.has(r.id)) return false
        uniqueIds.add(r.id)
        return true
      })
    } else {
      const res = await listByRoomAndDate(props.roomId, dateStr)
      reservations.value = res.data
    }
  } catch { /* */ }
}

defineExpose({ loadReservations })

watch([viewMode, currentDate], loadReservations)
watch(() => props.roomId, loadReservations)
onMounted(loadReservations)
</script>

<style scoped>
.room-calendar { background: #fff; border-radius: 12px; padding: 16px; }

.calendar-controls { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.date-display { font-size: 14px; color: #303133; font-weight: 500; margin-left: 8px; }

/* 日视图 */
.day-view { border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
.time-header { display: flex; background: #fafbfc; border-bottom: 1px solid #e5e7eb; }
.time-col-header { width: 80px; padding: 10px; font-size: 12px; font-weight: 600; color: #6b7280; border-right: 1px solid #e5e7eb; }
.status-col-header { flex: 1; padding: 10px; font-size: 12px; font-weight: 600; color: #6b7280; }

.time-slots { max-height: 400px; overflow-y: auto; }
.time-slot { display: flex; border-bottom: 1px solid #f3f4f6; cursor: pointer; transition: background 0.15s; }
.time-slot:hover { background: #f9fafb; }
.time-slot.available { background: #f0fdf4; }
.time-slot.available:hover { background: #dcfce7; }
.time-slot.occupied { background: #fef2f2; }
.time-slot.past { background: #f9fafb; opacity: 0.7; }

.slot-time { width: 80px; padding: 12px; font-size: 13px; font-weight: 600; color: #374151; border-right: 1px solid #e5e7eb; display: flex; align-items: center; }
.slot-content { flex: 1; padding: 12px; }
.slot-subject { font-size: 13px; font-weight: 500; color: #303133; }
.slot-meta { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.slot-available { font-size: 12px; color: #10b981; }
.slot-past { font-size: 12px; color: #9ca3af; }

/* 周视图 */
.week-view { border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
.week-header { display: flex; background: #fafbfc; border-bottom: 1px solid #e5e7eb; }
.day-header { flex: 1; padding: 8px; text-align: center; border-right: 1px solid #f3f4f6; }
.day-header:last-child { border-right: none; }
.day-header.today { background: #eef2ff; }
.day-header.today .day-date { background: var(--primary); color: #fff; border-radius: 50%; }
.day-name { font-size: 11px; color: #9ca3af; }
.day-date { font-size: 14px; font-weight: 600; color: #303133; margin-top: 4px; display: inline-block; width: 28px; height: 28px; line-height: 28px; }

.week-body { max-height: 400px; overflow-y: auto; }
.time-row { display: flex; border-bottom: 1px solid #f3f4f6; }
.time-row .time-label { width: 80px; padding: 8px; font-size: 11px; color: #9ca3af; border-right: 1px solid #e5e7eb; flex-shrink: 0; }
.day-cell { flex: 1; padding: 8px; min-height: 40px; border-right: 1px solid #f3f4f6; cursor: pointer; transition: background 0.15s; }
.day-cell:last-child { border-right: none; }
.day-cell.available:hover { background: #f0fdf4; }
.day-cell.occupied { background: #fef2f2; }
.day-cell.past { background: #f9fafb; opacity: 0.7; }
.cell-event { font-size: 11px; color: #dc2626; font-weight: 500; }
</style>
