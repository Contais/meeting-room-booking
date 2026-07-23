<template>
  <div class="time-slot-calendar">
    <div class="calendar-header">
      <el-button text @click="prevDay"><el-icon><ArrowLeft /></el-icon></el-button>
      <span class="calendar-date">{{ displayDate }}</span>
      <el-button text @click="nextDay"><el-icon><ArrowRight /></el-icon></el-button>
      <el-button size="small" @click="goToday">今天</el-button>
    </div>

    <div class="time-slots" v-loading="loading">
      <div v-for="slot in timeSlots" :key="slot.time" class="time-slot" :class="slot.status">
        <div class="slot-time">{{ slot.time }}</div>
        <div class="slot-info">
          <template v-if="slot.reservation">
            <div class="slot-subject">{{ slot.reservation.subject }}</div>
            <div class="slot-meta">{{ slot.reservation.contactPhone || '' }}</div>
          </template>
          <span v-else class="slot-available">可预约</span>
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

const props = defineProps<{ roomId: number }>()

const selectedDate = ref(new Date())
const loading = ref(false)
const reservations = ref<Reservation[]>([])

interface TimeSlot {
  time: string
  status: 'available' | 'occupied'
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
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() - 1)
  selectedDate.value = d
}

function nextDay() {
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() + 1)
  selectedDate.value = d
}

function goToday() {
  selectedDate.value = new Date()
}

function buildTimeSlots() {
  const slots: TimeSlot[] = []
  const dateStr = toDateString(selectedDate.value)
  for (let h = 8; h < 20; h++) {
    const time = `${String(h).padStart(2, '0')}:00`
    const slotStart = new Date(`${dateStr}T${time}:00`)
    const slotEnd = new Date(slotStart)
    slotEnd.setHours(slotEnd.getHours() + 1)

    const reservation = reservations.value.find(r => {
      const rStart = new Date(r.startTime)
      const rEnd = new Date(r.endTime)
      return rStart < slotEnd && rEnd > slotStart
    })

    slots.push({
      time,
      status: reservation ? 'occupied' : 'available',
      reservation: reservation || null,
    })
  }
  timeSlots.value = slots
}

async function loadReservations() {
  loading.value = true
  try {
    const res = await listByRoomAndDate(props.roomId, toDateString(selectedDate.value))
    reservations.value = res.data
    buildTimeSlots()
  } catch { /* */ } finally { loading.value = false }
}

watch(selectedDate, loadReservations)
onMounted(loadReservations)
</script>

<style scoped>
.time-slot-calendar { background: #fff; border-radius: 12px; padding: 20px; }
.calendar-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.calendar-date { font-size: 15px; font-weight: 600; color: #1a1a2e; min-width: 140px; text-align: center; }
.time-slots { display: flex; flex-direction: column; gap: 4px; max-height: 400px; overflow-y: auto; }
.time-slot { display: flex; align-items: center; gap: 12px; padding: 10px 14px; border-radius: 8px; transition: background 0.2s; }
.time-slot.available { background: #f0fdf4; border-left: 3px solid #10b981; }
.time-slot.occupied { background: #fef2f2; border-left: 3px solid #ef4444; }
.slot-time { font-size: 13px; font-weight: 600; color: #374151; min-width: 50px; }
.slot-info { flex: 1; }
.slot-subject { font-size: 13px; color: #1a1a2e; font-weight: 500; }
.slot-meta { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.slot-available { font-size: 12px; color: #10b981; }
</style>
