<template>
  <div class="page-view">
    <div class="page-header">
      <h2>日程视图</h2>
      <div class="schedule-controls">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="day">日</el-radio-button>
          <el-radio-button value="week">周</el-radio-button>
          <el-radio-button value="month">月</el-radio-button>
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

    <!-- 月视图 -->
    <div v-if="viewMode === 'month'" class="month-view page-card">
      <div class="month-header">
        <div class="month-day-name" v-for="d in ['一', '二', '三', '四', '五', '六', '日']" :key="d">{{ d }}</div>
      </div>
      <div class="month-grid">
        <div v-for="(day, idx) in monthDays" :key="idx"
          class="month-cell" :class="{ 'other-month': !day.currentMonth, 'today': day.isToday }"
          @click="onMonthCellClick(day)">
          <div class="cell-date">{{ day.date }}</div>
          <div class="cell-events">
            <div v-for="r in day.reservations" :key="r.id"
              class="cell-event" :class="'status-' + r.status"
              @click.stop="showDetail(r)">
              {{ formatTime(r.startTime) }} {{ r.subject || '未命名' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快速预约对话框 -->
    <el-dialog v-model="quickBookVisible" title="快速预约" width="480px" destroy-on-close>
      <el-form ref="quickBookFormRef" :model="quickBookForm" :rules="quickBookRules" label-width="80px">
        <el-form-item label="会议室">
          <el-select v-model="quickBookForm.roomId" placeholder="请选择会议室" style="width:100%">
            <el-option v-for="room in rooms" :key="room.id" :label="room.name" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="quickBookForm.date" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="时段">
          <div style="display:flex;gap:8px;align-items:center">
            <el-time-select v-model="quickBookForm.startTime" :max-time="quickBookForm.endTime" placeholder="开始时间" start="08:00" step="00:30" end="20:00" style="width:140px" />
            <span>~</span>
            <el-time-select v-model="quickBookForm.endTime" :min-time="quickBookForm.startTime" placeholder="结束时间" start="08:00" step="00:30" end="20:00" style="width:140px" />
          </div>
        </el-form-item>
        <el-form-item label="主题" prop="subject">
          <el-input v-model="quickBookForm.subject" placeholder="请输入会议主题" />
        </el-form-item>
        <el-form-item label="人数">
          <el-input-number v-model="quickBookForm.attendeeCount" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="quickBookForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickBookVisible = false">取消</el-button>
        <el-button type="primary" :loading="quickBookSubmitting" @click="handleQuickBook">确定</el-button>
      </template>
    </el-dialog>

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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getSchedule, createReservation } from '@/api/reservation'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'

const viewMode = ref('day')
const currentDate = ref(new Date())
const rooms = ref<{ id: number; name: string; capacity: number }[]>([])
const reservations = ref<{ id: number; roomId: number; roomName: string; subject: string; startTime: string; endTime: string; status: number; attendeeCount: number }[]>([])
const detailVisible = ref(false)
const currentReservation = ref<any>(null)

// 月视图相关
const monthDays = ref<any[]>([])

// 快速预约相关
const quickBookVisible = ref(false)
const quickBookSubmitting = ref(false)
const quickBookFormRef = ref<FormInstance>()
const quickBookForm = reactive({
  roomId: undefined as number | undefined,
  date: '',
  startTime: '',
  endTime: '',
  subject: '',
  attendeeCount: 1,
  contactPhone: ''
})
const quickBookRules: FormRules = {
  roomId: [{ required: true, message: '请选择会议室', trigger: 'change' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  subject: [{ required: true, message: '请输入会议主题', trigger: 'blur' }]
}

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
  } else if (viewMode.value === 'week') {
    const weekEnd = new Date(d)
    weekEnd.setDate(weekEnd.getDate() + 6)
    params.startDate = fmt(d)
    params.endDate = fmt(weekEnd)
  } else {
    // 月视图
    const monthStart = new Date(d.getFullYear(), d.getMonth(), 1)
    const monthEnd = new Date(d.getFullYear(), d.getMonth() + 1, 0)
    // 补充前后月份的日期
    const startDay = monthStart.getDay() || 7
    monthStart.setDate(monthStart.getDate() - startDay + 1)
    const endDay = monthEnd.getDay() || 7
    monthEnd.setDate(monthEnd.getDate() + (7 - endDay))
    params.startDate = fmt(monthStart)
    params.endDate = fmt(monthEnd)
  }
  try {
    const res = await getSchedule(params)
    rooms.value = res.data.rooms || []
    reservations.value = res.data.reservations || []
    if (viewMode.value === 'month') {
      buildMonthDays()
    }
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

function buildMonthDays() {
  const d = currentDate.value
  const year = d.getFullYear()
  const month = d.getMonth()
  const today = new Date()
  const todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0')
  
  const monthStart = new Date(year, month, 1)
  const startDay = monthStart.getDay() || 7
  
  const days: any[] = []
  const startDate = new Date(monthStart)
  startDate.setDate(startDate.getDate() - startDay + 1)
  
  for (let i = 0; i < 42; i++) {
    const date = new Date(startDate)
    date.setDate(date.getDate() + i)
    const dateStr = date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0')
    days.push({
      date: date.getDate(),
      dateStr,
      currentMonth: date.getMonth() === month,
      isToday: dateStr === todayStr,
      reservations: reservations.value.filter(r => {
        const rDate = r.startTime.split('T')[0]
        return rDate === dateStr
      })
    })
  }
  monthDays.value = days
}

function onMonthCellClick(day: any) {
  quickBookForm.date = day.dateStr
  quickBookForm.roomId = undefined
  quickBookForm.startTime = ''
  quickBookForm.endTime = ''
  quickBookForm.subject = ''
  quickBookForm.attendeeCount = 1
  quickBookForm.contactPhone = ''
  quickBookVisible.value = true
}

async function handleQuickBook() {
  const valid = await quickBookFormRef.value?.validate().catch(() => false)
  if (!valid) return
  quickBookSubmitting.value = true
  try {
    await createReservation({
      roomId: quickBookForm.roomId!,
      subject: quickBookForm.subject,
      attendeeCount: quickBookForm.attendeeCount,
      contactPhone: quickBookForm.contactPhone,
      startTime: quickBookForm.date + 'T' + quickBookForm.startTime + ':00',
      endTime: quickBookForm.date + 'T' + quickBookForm.endTime + ':00'
    })
    ElMessage.success('预约成功')
    quickBookVisible.value = false
    loadData()
  } catch { /* */ } finally {
    quickBookSubmitting.value = false
  }
}

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

/* 月视图样式 */
.month-view { padding: 16px; }
.month-header { display: grid; grid-template-columns: repeat(7, 1fr); border-bottom: 1px solid var(--border-light); }
.month-day-name { padding: 8px; text-align: center; font-size: 12px; font-weight: 600; color: var(--text-secondary); }
.month-grid { display: grid; grid-template-columns: repeat(7, 1fr); }
.month-cell {
  min-height: 100px; border: 1px solid #f0f0f0; padding: 4px;
  cursor: pointer; transition: background 0.15s;
}
.month-cell:hover { background: #f9fafb; }
.month-cell.other-month { background: #fafbfc; }
.month-cell.other-month .cell-date { color: #c0c4cc; }
.month-cell.today { background: #ecf5ff; }
.month-cell.today .cell-date { color: var(--primary); font-weight: 600; }
.cell-date { font-size: 12px; padding: 4px; }
.cell-events { display: flex; flex-direction: column; gap: 2px; }
.cell-event {
  font-size: 11px; padding: 2px 4px; border-radius: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.cell-event.status-0 { background: #fef3cd; color: #92400e; }
.cell-event.status-1 { background: #d1fae5; color: #065f46; }
.cell-event.status-2 { background: #f3f4f6; color: #6b7280; }
</style>
