<template>
  <div class="page-view">
    <!-- 顶部控制栏 -->
    <div class="control-bar">
      <div class="control-left">
        <el-button size="small" @click="goToday">今天</el-button>
        <el-button-group size="small">
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

    <!-- ==================== 日视图 ==================== -->
    <div v-if="viewMode === 'day'" class="day-view page-card">
      <div class="day-header">
        <div class="room-col-header">会议室</div>
        <div class="day-ticks">
          <div v-for="h in dayHours" :key="h" class="tick" :class="{ 'tick-now': isCurrentHour(h) }">
            <span class="tick-label" :class="{ 'tick-now-label': isCurrentHour(h) }">{{ h }}:00</span>
            <span class="tick-mark"></span>
          </div>
        </div>
      </div>
      <div class="day-body">
        <div v-for="(room, rIdx) in rooms" :key="room.id" class="day-row">
          <div class="room-label">
            <div class="room-name">{{ room.name }}</div>
            <div class="room-meta">{{ room.capacity }}人</div>
          </div>
          <div class="day-grid">
            <div v-for="h in dayHours" :key="h" class="grid-cell" @click="onDayCellClick(room, h)"></div>
          </div>
          <div v-for="r in getRoomReservations(room.id)" :key="r.id"
            class="day-event" :class="'s' + r.status"
            :style="dayEventStyle(r, rIdx)" @click="showDetail(r)">
            <div class="evt-inner">
              <div class="evt-title">{{ r.subject || '未命名' }}</div>
              <div class="evt-time">{{ formatTime(r.startTime) }}-{{ formatTime(r.endTime) }}</div>
              <div class="evt-user">{{ r.username || '' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 周视图 ==================== -->
    <div v-if="viewMode === 'week'" class="week-view page-card">
      <div class="week-header">
        <div class="wk-corner"></div>
        <div v-for="d in weekDays" :key="d.dateStr" class="wk-day" :class="{ today: d.isToday }">
          <div class="wk-day-name">{{ d.dayName }}</div>
          <div class="wk-day-num">{{ d.dayNum }}</div>
        </div>
      </div>
      <div class="week-body">
        <div class="wk-times">
          <div v-for="h in dayHours" :key="h" class="wk-time">{{ h }}:00</div>
        </div>
        <div class="wk-grid">
          <div v-for="d in weekDays" :key="d.dateStr" class="wk-col">
            <div v-for="h in dayHours" :key="h" class="wk-cell" @click="onWeekCellClick(d.dateStr, h)"></div>
          </div>
          <div v-for="r in weekReservations" :key="r.id"
            class="week-event" :class="'s' + r.status"
            :style="weekEventStyle(r)" @click="showDetail(r)">
            <div class="evt-inner">
              <div class="evt-title">{{ r.subject || '未命名' }}</div>
              <div class="evt-time">{{ formatTime(r.startTime) }}-{{ formatTime(r.endTime) }}</div>
              <div class="evt-user">{{ r.username || '' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 月视图 ==================== -->
    <div v-if="viewMode === 'month'" class="month-view page-card">
      <div class="month-header">
        <div v-for="d in ['日', '一', '二', '三', '四', '五', '六']" :key="d" class="mh-cell">{{ d }}</div>
      </div>
      <div class="month-grid">
        <div v-for="(day, idx) in monthDays" :key="idx"
          class="month-cell" :class="{ 'other-month': !day.currentMonth, today: day.isToday }"
          @click="onMonthCellClick(day)">
          <div class="mc-date">{{ day.date }}</div>
          <div class="mc-events">
            <div v-for="r in getDayReservations(day)" :key="r.id"
              class="mc-event" :class="'s' + r.status"
              :title="formatTime(r.startTime) + ' ' + (r.subject || '未命名')"
              @click.stop="showDetail(r)">
              {{ formatTime(r.startTime) }} {{ r.subject || '未命名' }}
            </div>
            <div v-if="day.reservations.length > 3 && !expandedDays.has(day.dateStr)"
              class="mc-more" @click.stop="toggleDayExpand(day.dateStr)">
              +{{ day.reservations.length - 3 }}更多
            </div>
            <div v-if="expandedDays.has(day.dateStr) && day.reservations.length > 3"
              class="mc-more" @click.stop="toggleDayExpand(day.dateStr)">收起</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 预约详情 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="400px">
      <el-descriptions :column="1" border v-if="currentReservation">
        <el-descriptions-item label="会议室">{{ currentReservation.roomName }}</el-descriptions-item>
        <el-descriptions-item label="主题">{{ currentReservation.subject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间段">{{ formatTime(currentReservation.startTime) }} - {{ formatTime(currentReservation.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="预约人">{{ currentReservation.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="参会人数">{{ currentReservation.attendeeCount || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(currentReservation.status)" size="small">{{ statusText(currentReservation.status) }}</el-tag></el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 快速预约 -->
    <el-dialog v-model="quickBookVisible" title="预约会议室" width="480px" destroy-on-close>
      <el-form ref="quickBookFormRef" :model="quickBookForm" :rules="quickBookRules" label-width="80px">
        <el-form-item label="会议室"><el-select v-model="quickBookForm.roomId" placeholder="请选择会议室" style="width:100%"><el-option v-for="room in rooms" :key="room.id" :label="room.name" :value="room.id" /></el-select></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="quickBookForm.date" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="时段"><div style="display:flex;gap:8px;align-items:center"><el-time-select v-model="quickBookForm.startTime" :max-time="quickBookForm.endTime" placeholder="开始" start="08:00" step="00:30" end="20:00" style="width:140px" /><span>~</span><el-time-select v-model="quickBookForm.endTime" :min-time="quickBookForm.startTime" placeholder="结束" start="08:00" step="00:30" end="20:00" style="width:140px" /></div></el-form-item>
        <el-form-item label="主题" prop="subject"><el-input v-model="quickBookForm.subject" placeholder="请输入会议主题" /></el-form-item>
        <el-form-item label="人数"><el-input-number v-model="quickBookForm.attendeeCount" :min="1" :max="100" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="quickBookForm.contactPhone" placeholder="请输入联系电话" /></el-form-item>
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
const detailVisible = ref(false)
const currentReservation = ref<any>(null)
const quickBookVisible = ref(false)
const quickBookSubmitting = ref(false)
const quickBookFormRef = ref<FormInstance>()
const quickBookForm = reactive({ roomId: undefined as number | undefined, date: '', startTime: '', endTime: '', subject: '', attendeeCount: 1, contactPhone: '' })
const quickBookRules: FormRules = {
  roomId: [{ required: true, message: '请选择会议室', trigger: 'change' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  subject: [{ required: true, message: '请输入会议主题', trigger: 'blur' }]
}

const START_HOUR = 8
const END_HOUR = 20
const TOTAL_HOURS = END_HOUR - START_HOUR
const ROW_H = 64

const dayHours = computed(() => { const h = []; for (let i = START_HOUR; i < END_HOUR; i++) h.push(i); return h })

const weekDays = computed(() => {
  const d = currentDate.value
  const s = new Date(d); s.setDate(s.getDate() - s.getDay())
  const today = formatDate(new Date())
  const names = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return Array.from({ length: 7 }, (_, i) => {
    const dt = new Date(s); dt.setDate(dt.getDate() + i)
    const ds = formatDate(dt)
    return { date: dt, dateStr: ds, dayName: names[i], dayNum: dt.getDate(), isToday: ds === today }
  })
})

const weekReservations = computed(() => reservations.value.filter(r => weekDays.value.some(d => d.dateStr === r.startTime.split('T')[0])))

const monthDays = ref<any[]>([])
const expandedDays = ref<Set<string>>(new Set())

const dateDisplay = computed(() => {
  const d = currentDate.value
  const f = (dt: Date) => `${dt.getFullYear()}年${String(dt.getMonth() + 1).padStart(2, '0')}月${String(dt.getDate()).padStart(2, '0')}日`
  if (viewMode.value === 'day') return f(d)
  if (viewMode.value === 'week') { const e = new Date(d); e.setDate(e.getDate() + 6); return `${f(d)} ~ ${f(e)}` }
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})

function formatDate(dt: Date) { return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}` }
function formatTime(t: string) { return t ? t.substring(11, 16) : '' }
function timeToPct(t: string) { return ((parseInt(t.substring(11, 13)) - START_HOUR) + parseInt(t.substring(14, 16)) / 60) / TOTAL_HOURS * 100 }
function durPct(s: string, e: string) { return timeToPct(e) - timeToPct(s) }

async function loadData() {
  const d = currentDate.value; const p: Record<string, string> = {}
  if (viewMode.value === 'day') { p.date = formatDate(d) }
  else if (viewMode.value === 'week') { p.startDate = formatDate(weekDays.value[0].date); p.endDate = formatDate(weekDays.value[6].date) }
  else { const ms = new Date(d.getFullYear(), d.getMonth(), 1); ms.setDate(ms.getDate() - ms.getDay()); const me = new Date(ms); me.setDate(me.getDate() + 41); p.startDate = formatDate(ms); p.endDate = formatDate(me) }
  try { const r = await getSchedule(p); rooms.value = r.data.rooms || []; reservations.value = r.data.reservations || []; if (viewMode.value === 'month') buildMonthDays() } catch { /* */ }
}

// ====== 当前时间 ======
function isCurrentHour(h: number) {
  const now = new Date()
  return now.getHours() === h
}

// ====== 日视图 ======
function getRoomReservations(roomId: number) {
  const today = formatDate(currentDate.value)
  return reservations.value.filter(r => r.roomId === roomId && r.startTime.split('T')[0] === today)
}
function dayEventStyle(r: any, rIdx: number) {
  return { left: timeToPct(r.startTime) + '%', width: durPct(r.startTime, r.endTime) + '%', top: rIdx * ROW_H + 4 + 'px', height: ROW_H - 8 + 'px' }
}
function onDayCellClick(room: any, h: number) {
  quickBookForm.roomId = room.id; quickBookForm.date = formatDate(currentDate.value)
  quickBookForm.startTime = String(h).padStart(2, '0') + ':00'; quickBookForm.endTime = String(h + 1).padStart(2, '0') + ':00'
  quickBookForm.subject = ''; quickBookForm.attendeeCount = 1; quickBookForm.contactPhone = ''; quickBookVisible.value = true
}

// ====== 周视图 ======
function weekEventStyle(r: any) {
  const di = weekDays.value.findIndex(d => d.dateStr === r.startTime.split('T')[0])
  if (di < 0) return { display: 'none' }
  return {
    left: (di / 7 * 100) + '%',
    width: (100 / 7) + '%',
    top: timeToPct(r.startTime) + '%',
    height: Math.max(durPct(r.startTime, r.endTime), 2) + '%'
  }
}
function onWeekCellClick(dateStr: string, h: number) {
  quickBookForm.roomId = undefined; quickBookForm.date = dateStr
  quickBookForm.startTime = String(h).padStart(2, '0') + ':00'; quickBookForm.endTime = String(h + 1).padStart(2, '0') + ':00'
  quickBookVisible.value = true
}

// ====== 月视图 ======
function getDayReservations(day: any) { return expandedDays.value.has(day.dateStr) ? day.reservations : day.reservations.slice(0, 3) }
function toggleDayExpand(ds: string) { expandedDays.value.has(ds) ? expandedDays.value.delete(ds) : expandedDays.value.add(ds) }
function buildMonthDays() {
  const d = currentDate.value, m = d.getMonth(), today = formatDate(new Date())
  const ms = new Date(d.getFullYear(), m, 1); ms.setDate(ms.getDate() - ms.getDay())
  monthDays.value = Array.from({ length: 42 }, (_, i) => { const dt = new Date(ms); dt.setDate(dt.getDate() + i); const ds = formatDate(dt); return { date: dt.getDate(), dateStr: ds, currentMonth: dt.getMonth() === m, isToday: ds === today, reservations: reservations.value.filter(r => r.startTime.split('T')[0] === ds) } })
}
function onMonthCellClick(day: any) { quickBookForm.roomId = undefined; quickBookForm.date = day.dateStr; quickBookVisible.value = true }

// ====== 导航 ======
function goPrev() { const d = new Date(currentDate.value); if (viewMode.value === 'day') d.setDate(d.getDate() - 1); else if (viewMode.value === 'week') d.setDate(d.getDate() - 7); else d.setMonth(d.getMonth() - 1); currentDate.value = d }
function goNext() { const d = new Date(currentDate.value); if (viewMode.value === 'day') d.setDate(d.getDate() + 1); else if (viewMode.value === 'week') d.setDate(d.getDate() + 7); else d.setMonth(d.getMonth() + 1); currentDate.value = d }
function goToday() { currentDate.value = new Date() }
function openQuickBook() { quickBookForm.roomId = undefined; quickBookForm.date = formatDate(currentDate.value); quickBookForm.startTime = '09:00'; quickBookForm.endTime = '10:00'; quickBookForm.subject = ''; quickBookForm.attendeeCount = 1; quickBookForm.contactPhone = ''; quickBookVisible.value = true }
async function handleQuickBook() {
  const v = await quickBookFormRef.value?.validate().catch(() => false); if (!v) return
  quickBookSubmitting.value = true
  try { await createReservation({ roomId: quickBookForm.roomId!, subject: quickBookForm.subject, attendeeCount: quickBookForm.attendeeCount, contactPhone: quickBookForm.contactPhone, startTime: `${quickBookForm.date}T${quickBookForm.startTime}:00`, endTime: `${quickBookForm.date}T${quickBookForm.endTime}:00` }); ElMessage.success('预约成功'); quickBookVisible.value = false; loadData() } catch { /* */ } finally { quickBookSubmitting.value = false }
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

/* ========== 日视图 ========== */
.day-view { padding: 0; overflow: hidden; }
.day-header { display: flex; border-bottom: 2px solid #e5e7eb; background: #fafbfc; }
.room-col-header { width: 100px; padding: 10px 12px; font-size: 12px; font-weight: 600; color: #6b7280; flex-shrink: 0; border-right: 1px solid #e5e7eb; }
.day-ticks { flex: 1; display: flex; }
.tick { flex: 1; display: flex; flex-direction: column; align-items: flex-start; padding-top: 8px; }
.tick-label { font-size: 11px; color: #6b7280; font-weight: 500; }
.tick-now-label { color: #ef4444; font-weight: 600; }
.tick-mark { width: 1px; height: 6px; background: #9ca3af; margin-top: 4px; }
.tick-now .tick-mark { background: #ef4444; height: 10px; }

.day-body { position: relative; }
.day-row { display: flex; height: 64px; border-bottom: 1px solid #f0f0f0; position: relative; }
.room-label { width: 100px; padding: 6px 12px; border-right: 1px solid #e5e7eb; display: flex; flex-direction: column; justify-content: center; flex-shrink: 0; background: #fff; z-index: 2; }
.room-name { font-size: 12px; font-weight: 600; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.room-meta { font-size: 10px; color: #9ca3af; margin-top: 2px; }
.day-grid { flex: 1; display: flex; }
.grid-cell { flex: 1; border-right: 1px solid #f3f4f6; cursor: pointer; }
.grid-cell:last-child { border-right: none; }
.grid-cell:hover { background: #f9fafb; }

.day-event { position: absolute; border-radius: 6px; padding: 3px 6px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 1; transition: box-shadow 0.15s; left: 100px; width: calc(100% - 100px); }
.day-event:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }

/* ========== 周视图 ========== */
.week-view { padding: 0; overflow: hidden; }
.week-header { display: flex; border-bottom: 1px solid #e5e7eb; background: #fafbfc; }
.wk-corner { width: 40px; flex-shrink: 0; border-right: 1px solid #e5e7eb; }
.wk-day { flex: 1; padding: 6px 4px; text-align: center; border-right: 1px solid #f3f4f6; }
.wk-day:last-child { border-right: none; }
.wk-day.today { background: #ecf5ff; }
.wk-day.today .wk-day-num { background: #409eff; color: #fff; border-radius: 50%; }
.wk-day-name { font-size: 11px; color: #9ca3af; }
.wk-day-num { font-size: 14px; font-weight: 600; color: #303133; margin-top: 2px; display: inline-block; width: 28px; height: 28px; line-height: 28px; }

.week-body { display: flex; position: relative; }
.wk-times { width: 40px; flex-shrink: 0; }
.wk-time { height: 64px; padding: 2px 4px; font-size: 11px; color: #9ca3af; display: flex; align-items: flex-start; }
.wk-grid { flex: 1; position: relative; display: flex; }
.wk-col { flex: 1; display: flex; flex-direction: column; }
.wk-cell { height: 64px; border-right: 1px solid #f3f4f6; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.wk-cell:last-child { border-right: none; }
.wk-cell:hover { background: #f9fafb; }

.week-event { position: absolute; left: 0; border-radius: 6px; padding: 3px 6px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 1; transition: box-shadow 0.15s; }
.week-event:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }

/* ========== 通用预约块 ========== */
.s0 { background: #fef3cd; border-left: 3px solid #f59e0b; }
.s1 { background: #d1fae5; border-left: 3px solid #10b981; }
.s2 { background: #f3f4f6; border-left: 3px solid #9ca3af; }
.evt-inner { height: 100%; display: flex; flex-direction: column; justify-content: center; overflow: hidden; }
.evt-title { font-weight: 500; color: #374151; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 12px; }
.evt-time { font-size: 10px; color: #6b7280; margin-top: 1px; white-space: nowrap; }
.evt-user { font-size: 10px; color: #9ca3af; margin-top: 1px; white-space: nowrap; }

/* ========== 月视图 ========== */
.month-view { padding: 16px; }
.month-header { display: grid; grid-template-columns: repeat(7, 1fr); border-bottom: 1px solid #e5e7eb; }
.mh-cell { padding: 8px; text-align: center; font-size: 12px; font-weight: 600; color: #6b7280; }
.month-grid { display: grid; grid-template-columns: repeat(7, 1fr); }
.month-cell { min-height: 100px; border: 1px solid #f0f0f0; padding: 4px; cursor: pointer; transition: background 0.15s; overflow: hidden; }
.month-cell:hover { background: #f9fafb; }
.month-cell.other-month { background: #fafbfc; }
.month-cell.other-month .mc-date { color: #c0c4cc; }
.month-cell.today { background: #ecf5ff; }
.month-cell.today .mc-date { color: #409eff; font-weight: 600; }
.mc-date { font-size: 12px; padding: 4px; }
.mc-events { display: flex; flex-direction: column; gap: 2px; }
.mc-event { font-size: 11px; padding: 2px 4px; border-radius: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; height: 18px; line-height: 14px; }
.mc-event.s0 { background: #fef3cd; color: #92400e; }
.mc-event.s1 { background: #d1fae5; color: #065f46; }
.mc-event.s2 { background: #f3f4f6; color: #6b7280; }
.mc-more { font-size: 11px; color: #9ca3af; padding: 2px 4px; cursor: pointer; }
.mc-more:hover { color: #409eff; }
</style>
