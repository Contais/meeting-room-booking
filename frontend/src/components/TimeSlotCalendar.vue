<template>
  <div class="time-slot-calendar">
    <div class="calendar-header">
      <el-button text @click="prevDay"><el-icon><ArrowLeft /></el-icon></el-button>
      <span class="calendar-date">{{ displayDate }}</span>
      <el-button text @click="nextDay"><el-icon><ArrowRight /></el-icon></el-button>
      <el-button size="small" @click="goToday">今天</el-button>
    </div>

    <div class="time-slots" v-loading="loading">
      <div v-for="slot in timeSlots" :key="slot.time" class="time-slot" :class="{
        available: slot.status === 'available',
        occupied: slot.status === 'occupied',
        past: slot.status === 'past',
        outside: slot.status === 'outside',
        selected: isSelected(slot.time),
      }" @click="handleSlotClick(slot)">
        <div class="slot-time">{{ slot.time }}</div>
        <div class="slot-info">
          <template v-if="slot.status === 'occupied' && slot.reservation">
            <div class="slot-subject">{{ slot.reservation.subject }}</div>
            <div class="slot-meta">{{ slot.reservation.contactPhone || '' }}</div>
          </template>
          <span v-else-if="slot.status === 'past'" class="slot-past">已过</span>
          <span v-else-if="slot.status === 'outside'" class="slot-outside">不可用</span>
          <span v-else-if="isSelected(slot.time)" class="slot-selected-text">已选择</span>
          <span v-else class="slot-available">可预约</span>
        </div>
        <el-icon v-if="slot.status === 'available' || isSelected(slot.time)" class="slot-arrow">
          <Check v-if="isSelected(slot.time)" /><ArrowRight v-else />
        </el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ArrowLeft, ArrowRight, Check } from '@element-plus/icons-vue'
import { listByRoomAndDate } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const props = defineProps<{
  roomId: number
  bookableStart?: string
  bookableEnd?: string
}>()

const emit = defineEmits<{
  (e: 'select', startTime: string, endTime: string): void
}>()

const selectedDate = ref(new Date())
const loading = ref(false)
const reservations = ref<Reservation[]>([])
const selectedTimes = ref<string[]>([])

interface TimeSlot {
  time: string
  status: 'available' | 'occupied' | 'past' | 'outside'
  reservation: Reservation | null
}

const timeSlots = ref<TimeSlot[]>([])

const displayDate = computed(() => {
  const d = selectedDate.value
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}年${m}月${day}日`
})

function toDateString(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function prevDay() {
  selectedTimes.value = []
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() - 1)
  selectedDate.value = d
}

function nextDay() {
  selectedTimes.value = []
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() + 1)
  selectedDate.value = d
}

function goToday() { selectedTimes.value = []; selectedDate.value = new Date() }

function isSelected(time: string) {
  return selectedTimes.value.includes(time)
}

function handleSlotClick(slot: TimeSlot) {
  if (slot.status !== 'available' && !isSelected(slot.time)) return

  if (selectedTimes.value.length === 0) {
    // 第一次点击：选中该时段
    selectedTimes.value = [slot.time]
  } else if (isSelected(slot.time)) {
    // 点击已选中的时段：如果是最两端的，取消选择
    const first = selectedTimes.value[0]
    const last = selectedTimes.value[selectedTimes.value.length - 1]
    if (slot.time === first) {
      selectedTimes.value = selectedTimes.value.slice(1)
    } else if (slot.time === last) {
      selectedTimes.value = selectedTimes.value.slice(0, -1)
    } else {
      // 点击中间的：重置选区
      selectedTimes.value = [slot.time]
    }
  } else {
    // 点击未选中的时段：检查是否连续
    const allAvailable = getAvailableTimes()
    const clickedIdx = allAvailable.indexOf(slot.time)
    const firstIdx = allAvailable.indexOf(selectedTimes.value[0])
    const lastIdx = allAvailable.indexOf(selectedTimes.value[selectedTimes.value.length - 1])

    if (clickedIdx >= firstIdx && clickedIdx <= lastIdx + 1) {
      // 往后扩展
      const range = allAvailable.slice(firstIdx, clickedIdx + 1)
      // 检查范围内没有被占用的
      const hasOccupied = range.some(t => {
        const s = timeSlots.value.find(ts => ts.time === t)
        return s && s.status === 'occupied'
      })
      if (!hasOccupied) selectedTimes.value = range
    } else if (clickedIdx <= lastIdx && clickedIdx >= firstIdx - 1) {
      // 往前扩展
      const range = allAvailable.slice(clickedIdx, lastIdx + 1)
      const hasOccupied = range.some(t => {
        const s = timeSlots.value.find(ts => ts.time === t)
        return s && s.status === 'occupied'
      })
      if (!hasOccupied) selectedTimes.value = range
    } else {
      // 不连续，重新选
      selectedTimes.value = [slot.time]
    }
  }

  // 发送选中范围
  if (selectedTimes.value.length > 0) {
    const dateStr = toDateString(selectedDate.value)
    const startTime = `${dateStr}T${selectedTimes.value[0]}:00`
    const lastTime = selectedTimes.value[selectedTimes.value.length - 1]
    const endHour = parseInt(lastTime.split(':')[0]) + 1
    const endTime = `${dateStr}T${String(endHour).padStart(2, '0')}:00:00`
    emit('select', startTime, endTime)
  }
}

function getAvailableTimes(): string[] {
  return timeSlots.value
    .filter(s => s.status === 'available' || isSelected(s.time))
    .map(s => s.time)
}

function buildTimeSlots() {
  const slots: TimeSlot[] = []
  const dateStr = toDateString(selectedDate.value)
  const startHour = parseInt((props.bookableStart || '08:00').split(':')[0])
  const endHour = parseInt((props.bookableEnd || '20:00').split(':')[0])
  const now = new Date()

  for (let h = startHour; h < endHour; h++) {
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
  timeSlots.value = slots
}

async function loadReservations() {
  loading.value = true
  selectedTimes.value = []
  try {
    const res = await listByRoomAndDate(props.roomId, toDateString(selectedDate.value))
    reservations.value = res.data
    buildTimeSlots()
  } catch { /* */ } finally { loading.value = false }
}

watch(selectedDate, loadReservations)
watch(() => [props.bookableStart, props.bookableEnd], buildTimeSlots)
onMounted(loadReservations)
</script>

<style scoped>
.time-slot-calendar { background: #fff; border-radius: 12px; padding: 20px; }
.calendar-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.calendar-date { font-size: 15px; font-weight: 600; color: #303133; min-width: 140px; text-align: center; }
.time-slots { display: flex; flex-direction: column; gap: 4px; max-height: 400px; overflow-y: auto; }

.time-slot {
  display: flex; align-items: center; gap: 12px; padding: 10px 14px;
  border-radius: 8px; transition: all 0.15s; cursor: default;
}
.time-slot.available { background: #f0fdf4; border-left: 3px solid #10b981; cursor: pointer; }
.time-slot.available:hover { background: #dcfce7; }
.time-slot.selected { background: #dbeafe; border-left: 3px solid #3b82f6; cursor: pointer; }
.time-slot.selected:hover { background: #bfdbfe; }
.time-slot.occupied { background: #fef2f2; border-left: 3px solid #ef4444; }
.time-slot.past { background: #f9fafb; border-left: 3px solid #d1d5db; }
.time-slot.outside { background: #f9fafb; border-left: 3px solid #d1d5db; }

.slot-time { font-size: 13px; font-weight: 600; color: #374151; min-width: 50px; }
.slot-info { flex: 1; }
.slot-subject { font-size: 13px; color: #303133; font-weight: 500; }
.slot-meta { font-size: 11px; color: #909399; margin-top: 2px; }
.slot-available { font-size: 12px; color: #10b981; }
.slot-past { font-size: 12px; color: #9ca3af; }
.slot-outside { font-size: 12px; color: #c0c4cc; }
.slot-selected-text { font-size: 12px; color: #3b82f6; font-weight: 500; }
.slot-arrow { color: #10b981; font-size: 14px; }
.time-slot.selected .slot-arrow { color: #3b82f6; }
</style>
