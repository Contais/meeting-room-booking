<template>
  <div class="page-view">
    <!-- 顶部控制栏 -->
    <div class="control-bar">
      <div class="control-left">
        <el-button-group size="small">
          <el-button @click="goToday">今天</el-button>
          <el-button @click="goPrev"><el-icon><ArrowLeft /></el-icon></el-button>
          <el-button @click="goNext"><el-icon><ArrowRight /></el-icon></el-button>
        </el-button-group>
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="day">日</el-radio-button>
          <el-radio-button value="week">周</el-radio-button>
          <el-radio-button value="month">月</el-radio-button>
        </el-radio-group>
        <span class="date-display">{{ dateDisplay }}</span>
      </div>
      <div class="control-right">
        <el-button type="primary" size="small" @click="openQuickBook">
          <el-icon><Plus /></el-icon> 预约会议室
        </el-button>
      </div>
    </div>

    <!-- 日视图：横轴时间，纵轴会议室 -->
    <div v-if="viewMode === 'day'" class="day-view page-card">
      <div class="grid-header-row">
        <div class="room-col-header">会议室</div>
        <div class="time-col" v-for="h in dayHours" :key="h">{{ h }}:00</div>
      </div>
      <div class="grid-body">
        <div v-for="room in rooms" :key="room.id" class="room-row">
          <div class="room-label">
            <div class="room-name">{{ room.name }}</div>
            <div class="room-meta">{{ room.capacity }}人 | {{ room.equipment || '无设备' }}</div>
          </div>
          <div class="time-cell" v-for="h in dayHours" :key="room.id + '-' + h"
            @click="onCellClick(room, h)"></div>
          <div v-for="r in getRoomReservations(room.id)" :key="r.id"
            class="reservation-block" :class="'status-' + r.status"
            :style="dayBlockStyle(r)" @click="showDetail(r)">
            <el-tooltip :content="getTooltipContent(r)" placement="top" :show-after="300">
              <div class="block-inner">
                <div class="block-subject">{{ r.subject || '未命名' }}</div>
                <div class="block-time">{{ formatTime(r.startTime) }}-{{ formatTime(r.endTime) }}</div>
              </div>
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>

    <!-- 周视图：横轴日期，纵轴时间 -->
    <div v-if="viewMode === 'week'" class="week-view page-card">
      <div class="grid-header-row">
        <div class="time-col-header"></div>
        <div class="day-col" v-for="d in weekDays" :key="d.dateStr" :class="{ today: d.isToday }">
          <div class="day-name">{{ d.dayName }}</div>
          <div class="day-date">{{ d.dayNum }}</div>
        </div>
      </div>
      <div class="grid-body">
        <div v-for="h in dayHours" :key="h" class="time-row">
          <div class="time-label">{{ h }}:00</div>
          <div class="day-cell" v-for="d in weekDays" :key="d.dateStr + '-' + h"
            @click="onWeekCellClick(d.dateStr, h)"></div>
          <div v-for="r in getHourReservations(h)" :key="r.id"
            class="reservation-block week-block" :class="'status-' + r.status"
            :style="weekBlockStyle(r, h)" @click="showDetail(r)">
            <el-tooltip :content="getTooltipContent(r)" placement="top" :show-after="300">
              <div class="block-inner">
                <div class="block-subject">{{ r.subject || '未命名' }}</div>
                <div class="block-time">{{ formatTime(r.startTime) }}-{{ formatTime(r.endTime) }}</div>
              </div>
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>

    <!-- 月视图 -->
    <div v-if="viewMode === 'month'" class="month-view page-card">
      <div class="month-header">
        <div v-for="d in ['日', '一', '二', '三', '四', '五', '六']" :key="d" class="month-day-name">{{ d }}</div>
      </div>
      <div class="month-grid">
        <div v-for="(day, idx) in monthDays" :key="idx"
          class="month-cell" :class="{ 'other-month': !day.currentMonth, 'today': day.isToday }"
          @click="onMonthCellClick(day)">
          <div class="cell-date">{{ day.date }}</div>
          <div class="cell-events">
            <div v-for="r in day.reservations.slice(0, 3)" :key="r.id"
              class="cell-event" :class="'status-' + r.status"
              @click.stop="showDetail(r)">
              {{ formatTime(r.startTime) }} {{ r.subject || '未命名' }}
            </div>
            <div v-if="day.reservations.length > 3" class="cell-more">+{{ day.reservations.length - 3 }}更多</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 预约详情对话框 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="400px">
      <el-descriptions :column="1" border v-if="currentReservation">
        <el-descriptions-item label="会议室">{{ currentReservation.roomName }}</el-descriptions-item>
        <el-descriptions-item label="主题">{{ currentReservation.subject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间段">{{ formatTime(currentReservation.startTime) }} - {{ formatTime(currentReservation.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="参会人数">{{ currentReservation.attendeeCount || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(currentReservation.status)" size="small">{{ statusText(currentReservation.status) }}</el-tag></el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 快速预约对话框 -->
    <el-dialog v-model="quickBookVisible" title="预约会议室" width="480px" destroy-on-close>
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
        <el-button type="primary" :loading="quickBookSubmitting" @click="handleQuickBook">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ArrowLeft, ArrowRight, Plus } from '@element-plus/icons-vue'
import { getSchedule, createReservation } from '@/api/reservation'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'

const viewMode = ref<'day' | 'week' | 'month'>('day')
const currentDate = ref(new Date())
const rooms = ref<any[]>([])
const reservations = ref<any[]>([])

// 预约详情
const detailVisible = ref(false)
const currentReservation = ref<any>(null)

// 快速预约
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

// 时间相关
const START_HOUR = 8
const END_HOUR = 20
const dayHours = computed(() => {
  const hours = []
  for (let h = START_HOUR; h < END_HOUR; h++) hours.push(h)
  return hours
})

// 周日期（从周日开始）
const weekDays = computed(() => {
  const d = currentDate.value
  const startOfWeek = new Date(d)
  const day = startOfWeek.getDay()
  startOfWeek.setDate(startOfWeek.getDate() - day)  // 回到周日
  
  const today = new Date()
  const todayStr = formatDate(today)
  const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  
  return Array.from({ length: 7 }, (_, i) => {
    const date = new Date(startOfWeek)
    date.setDate(date.getDate() + i)
    const dateStr = formatDate(date)
    return {
      date: date,
      dateStr,
      dayName: dayNames[i],
      dayNum: date.getDate(),
      isToday: dateStr === todayStr
    }
  })
})

// 月日期
const monthDays = ref<any[]>([])

const dateDisplay = computed(() => {
  const d = currentDate.value
  const fmt = (date: Date) => `${date.getFullYear()}年${String(date.getMonth() + 1).padStart(2, '0')}月${String(date.getDate()).padStart(2, '0')}日`
  if (viewMode.value === 'day') return fmt(d)
  if (viewMode.value === 'week') {
    const end = new Date(d)
    end.setDate(end.getDate() + 6)
    return `${fmt(d)} ~ ${fmt(end)}`
  }
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})

function formatDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

async function loadData() {
  const d = currentDate.value
  let params: Record<string, string> = {}
  
  if (viewMode.value === 'day') {
    params.date = formatDate(d)
  } else if (viewMode.value === 'week') {
    const weekStart = new Date(d)
    const day = weekStart.getDay()
    weekStart.setDate(weekStart.getDate() - day)  // 回到周日
    const weekEnd = new Date(weekStart)
    weekEnd.setDate(weekEnd.getDate() + 6)
    params.startDate = formatDate(weekStart)
    params.endDate = formatDate(weekEnd)
  } else {
    const monthStart = new Date(d.getFullYear(), d.getMonth(), 1)
    const dayOfWeek = monthStart.getDay()
    monthStart.setDate(monthStart.getDate() - dayOfWeek)  // 回到周日
    const monthEnd = new Date(monthStart)
    monthEnd.setDate(monthEnd.getDate() + 41)
    params.startDate = formatDate(monthStart)
    params.endDate = formatDate(monthEnd)
  }
  
  try {
    const res = await getSchedule(params)
    rooms.value = res.data.rooms || []
    reservations.value = res.data.reservations || []
    if (viewMode.value === 'month') buildMonthDays()
  } catch { /* */ }
}

function getRoomReservations(roomId: number) {
  const today = formatDate(currentDate.value)
  return reservations.value.filter(r => {
    const rDate = r.startTime.split('T')[0]
    return r.roomId === roomId && rDate === today
  })
}

// 存储已合并的预约ID，避免重复显示
const mergedReservationIds = ref<Set<number>>(new Set())

function getHourReservations(hour: number) {
  mergedReservationIds.value.clear()
  
  return reservations.value.filter(r => {
    const start = new Date(r.startTime)
    const end = new Date(r.endTime)
    const startHour = start.getHours()
    const endHour = end.getHours() || 24
    
    // 只在预约开始的小时显示
    if (startHour !== hour) return false
    
    // 标记这个预约在哪些小时显示
    for (let h = startHour; h < endHour; h++) {
      if (h !== startHour) {
        mergedReservationIds.value.add(r.id)
      }
    }
    
    return true
  })
}

// 日视图预约块样式
function dayBlockStyle(r: any) {
  const start = new Date(r.startTime)
  const end = new Date(r.endTime)
  const startHour = start.getHours() + start.getMinutes() / 60
  const endHour = end.getHours() + end.getMinutes() / 60
  const duration = endHour - startHour
  
  // 计算相对于时间区域的偏移
  const totalHours = END_HOUR - START_HOUR
  const leftPercent = ((startHour - START_HOUR) / totalHours) * 100
  const widthPercent = (duration / totalHours) * 100
  
  return {
    left: `calc(${leftPercent}% + 2px)`,
    width: `calc(${widthPercent}% - 4px)`,
    top: '2px',
    height: 'calc(100% - 4px)',
    position: 'absolute' as const,
    zIndex: 1
  }
}

// 周视图预约块样式
function weekBlockStyle(r: any, _hour: number) {
  const dayIndex = weekDays.value.findIndex(d => d.dateStr === r.startTime.split('T')[0])
  if (dayIndex < 0) return { display: 'none' }
  
  const start = new Date(r.startTime)
  const end = new Date(r.endTime)
  const startMinutes = start.getMinutes()
  const duration = (end.getTime() - start.getTime()) / 60000
  
  // 计算跨小时数
  const startHour = start.getHours()
  const endHour = end.getHours() || 24
  const crossHours = endHour - startHour
  
  // 计算相对于时间行内的偏移
  const top = (startMinutes / 60) * 100
  const height = Math.max((duration / 60) * 100, 20)
  
  const colWidth = 100 / 7
  const leftPercent = dayIndex * colWidth
  
  return {
    left: `calc(${leftPercent}% + 2px)`,
    width: `calc(${colWidth}% - 4px)`,
    top: `${top}%`,
    height: `${height}%`,
    position: 'absolute' as const,
    zIndex: crossHours > 1 ? 2 : 1
  }
}

function buildMonthDays() {
  const d = currentDate.value
  const year = d.getFullYear()
  const month = d.getMonth()
  const today = new Date()
  const todayStr = formatDate(today)
  
  const monthStart = new Date(year, month, 1)
  const dayOfWeek = monthStart.getDay()  // 0=周日
  monthStart.setDate(monthStart.getDate() - dayOfWeek)  // 回到周日
  
  const days: any[] = []
  for (let i = 0; i < 42; i++) {
    const date = new Date(monthStart)
    date.setDate(date.getDate() + i)
    const dateStr = formatDate(date)
    days.push({
      date: date.getDate(),
      dateStr,
      currentMonth: date.getMonth() === month,
      isToday: dateStr === todayStr,
      reservations: reservations.value.filter(r => r.startTime.split('T')[0] === dateStr)
    })
  }
  monthDays.value = days
}

function goPrev() {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'day') d.setDate(d.getDate() - 1)
  else if (viewMode.value === 'week') d.setDate(d.getDate() - 7)
  else d.setMonth(d.getMonth() - 1)
  currentDate.value = d
}

function goNext() {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'day') d.setDate(d.getDate() + 1)
  else if (viewMode.value === 'week') d.setDate(d.getDate() + 7)
  else d.setMonth(d.getMonth() + 1)
  currentDate.value = d
}

function goToday() { currentDate.value = new Date() }

function onCellClick(room: any, hour: number) {
  quickBookForm.roomId = room.id
  quickBookForm.date = formatDate(currentDate.value)
  quickBookForm.startTime = String(hour).padStart(2, '0') + ':00'
  quickBookForm.endTime = String(hour + 1).padStart(2, '0') + ':00'
  quickBookForm.subject = ''
  quickBookForm.attendeeCount = 1
  quickBookForm.contactPhone = ''
  quickBookVisible.value = true
}

function onWeekCellClick(dateStr: string, hour: number) {
  quickBookForm.date = dateStr
  quickBookForm.startTime = String(hour).padStart(2, '0') + ':00'
  quickBookForm.endTime = String(hour + 1).padStart(2, '0') + ':00'
  quickBookVisible.value = true
}

function onMonthCellClick(day: any) {
  quickBookForm.date = day.dateStr
  quickBookVisible.value = true
}

function openQuickBook() {
  quickBookForm.roomId = undefined
  quickBookForm.date = formatDate(currentDate.value)
  quickBookForm.startTime = '09:00'
  quickBookForm.endTime = '10:00'
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
      startTime: `${quickBookForm.date}T${quickBookForm.startTime}:00`,
      endTime: `${quickBookForm.date}T${quickBookForm.endTime}:00`
    })
    ElMessage.success('预约成功')
    quickBookVisible.value = false
    loadData()
  } catch { /* */ } finally { quickBookSubmitting.value = false }
}

function formatTime(t: string) { return t ? t.replace('T', ' ').substring(11, 16) : '' }

function getTooltipContent(r: any) {
  const roomName = rooms.value.find(room => room.id === r.roomId)?.name || ''
  return `${r.subject || '未命名'}\n${formatTime(r.startTime)}-${formatTime(r.endTime)}\n${roomName}`
}
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info' }
function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function showDetail(r: any) { currentReservation.value = r; detailVisible.value = true }

watch([viewMode, currentDate], loadData)
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }

.control-bar { display: flex; justify-content: space-between; align-items: center; background: #fff; border-radius: 12px; padding: 12px 20px; border: 1px solid #f0f0f0; }
.control-left { display: flex; align-items: center; gap: 12px; }
.date-display { font-size: 14px; color: #303133; font-weight: 500; margin-left: 8px; }

/* 日视图 */
.day-view { padding: 0; overflow: hidden; }
.grid-header-row { display: flex; background: #fafbfc; border-bottom: 1px solid #e5e7eb; }
.room-col-header { width: 100px; padding: 12px; font-size: 12px; font-weight: 600; color: #6b7280; border-right: 1px solid #e5e7eb; }
.time-col { flex: 1; padding: 12px 4px; font-size: 11px; font-weight: 500; color: #6b7280; text-align: center; border-right: 1px solid #f3f4f6; }
.time-col:last-child { border-right: none; }

.grid-body { position: relative; min-height: 500px; }
.room-row { display: flex; border-bottom: 1px solid #f3f4f6; height: 80px; position: relative; }
.room-label { width: 100px; padding: 12px; border-right: 1px solid #e5e7eb; display: flex; flex-direction: column; justify-content: center; }
.room-name { font-size: 13px; font-weight: 600; color: #303133; }
.room-meta { font-size: 11px; color: #9ca3af; margin-top: 4px; }
.time-cell { flex: 1; border-right: 1px solid #f3f4f6; cursor: pointer; transition: background 0.15s; min-width: 0; }
.time-cell:hover { background: #f9fafb; }
.time-cell:last-child { border-right: none; }

/* 预约色块 */
.reservation-block { margin: 2px 4px; border-radius: 6px; padding: 4px 8px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 1; transition: box-shadow 0.15s; }
.reservation-block:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.status-0 { background: #fef3cd; border-left: 3px solid #f59e0b; }
.status-1 { background: #d1fae5; border-left: 3px solid #10b981; }
.status-2 { background: #f3f4f6; border-left: 3px solid #9ca3af; }
.block-inner { height: 100%; display: flex; flex-direction: column; justify-content: center; overflow: hidden; }
.block-subject { font-weight: 500; color: #374151; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 12px; }
.block-time { font-size: 10px; color: #6b7280; margin-top: 2px; white-space: nowrap; }

/* 周视图 */
.week-view { padding: 0; overflow: hidden; }
.time-col-header { width: 60px; flex-shrink: 0; }
.day-col { flex: 1; padding: 8px 4px; text-align: center; border-right: 1px solid #f3f4f6; }
.day-col:last-child { border-right: none; }
.day-col.today { background: #ecf5ff; }
.day-col.today .day-date { background: #409eff; color: #fff; border-radius: 50%; }
.day-name { font-size: 11px; color: #9ca3af; }
.day-date { font-size: 14px; font-weight: 600; color: #303133; margin-top: 4px; display: inline-block; width: 28px; height: 28px; line-height: 28px; }
.time-row { position: relative; border-bottom: 1px solid #f3f4f6; height: 80px; }
.time-label { width: 60px; padding: 8px; font-size: 11px; color: #9ca3af; border-right: 1px solid #e5e7eb; flex-shrink: 0; }
.day-cell { position: absolute; top: 0; bottom: 0; border-right: 1px solid #f3f4f6; cursor: pointer; }
.day-cell:hover { background: #f9fafb; }
.day-cell:hover { background: #f9fafb; }
.week-block { height: calc(100% - 4px); }

/* 月视图 */
.month-view { padding: 16px; }
.month-header { display: grid; grid-template-columns: repeat(7, 1fr); border-bottom: 1px solid #e5e7eb; }
.month-day-name { padding: 8px; text-align: center; font-size: 12px; font-weight: 600; color: #6b7280; }
.month-grid { display: grid; grid-template-columns: repeat(7, 1fr); }
.month-cell { min-height: 100px; border: 1px solid #f0f0f0; padding: 4px; cursor: pointer; transition: background 0.15s; }
.month-cell:hover { background: #f9fafb; }
.month-cell.other-month { background: #fafbfc; }
.month-cell.other-month .cell-date { color: #c0c4cc; }
.month-cell.today { background: #ecf5ff; }
.month-cell.today .cell-date { color: #409eff; font-weight: 600; }
.cell-date { font-size: 12px; padding: 4px; }
.cell-events { display: flex; flex-direction: column; gap: 2px; }
.cell-event { font-size: 11px; padding: 2px 4px; border-radius: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cell-event.status-0 { background: #fef3cd; color: #92400e; }
.cell-event.status-1 { background: #d1fae5; color: #065f46; }
.cell-event.status-2 { background: #f3f4f6; color: #6b7280; }
.cell-more { font-size: 11px; color: #9ca3af; padding: 2px 4px; }
</style>
