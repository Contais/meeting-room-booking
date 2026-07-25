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
      <!-- 横轴刻度头（固定） -->
      <div class="day-header">
        <div class="room-col-header">会议室</div>
        <div class="day-ticks-wrap" ref="dayHeaderRef">
          <div class="day-ticks" :style="{ width: hoursWidth + 'px', transform: `translateX(${-headerScrollX}px)` }">
            <div v-for="h in allHours" :key="h" class="tick" >
              <span class="tick-label" >{{ String(h).padStart(2, '0') }}:00</span>
              <span class="tick-mark"></span>
            </div>
          </div>
        </div>
      </div>
      <!-- 内容区域（可滚动） -->
      <div class="day-body-wrap" ref="dayBodyRef" @scroll="onDayScroll">
        <div class="day-body">
          <div v-for="(room, rIdx) in rooms" :key="room.id" class="day-row" :style="{ transform: `translateX(${-headerScrollX}px)` }">
            <div class="room-label">
              <div class="room-name">{{ room.name }}</div>
              <div class="room-meta">{{ room.capacity }}人</div>
            </div>
            <div class="day-grid" :style="{ width: hoursWidth + 'px' }">
              <div v-for="h in allHours" :key="h" class="grid-cell" @click="onDayCellClick(room, h)"></div>
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
      <div class="week-body-wrap" ref="weekBodyRef" @scroll="onWeekScroll">
        <div class="week-body">
          <!-- 纵轴时间刻度（固定在左侧） -->
          <div class="wk-times" :style="{ height: hoursWidth + 'px' }">
            <div v-for="h in allHours" :key="h" class="wk-time" :class="{ 'wk-time-now': isCurrentHour(h) }">
              <span class="wk-time-label" :class="{ 'wk-time-now-label': isCurrentHour(h) }">{{ String(h).padStart(2, '0') }}:00</span>
              <span class="wk-time-mark"></span>
            </div>
          </div>
          <!-- 网格区域 -->
          <div class="wk-grid-wrap">
            <div class="wk-grid" :style="{ height: hoursWidth + 'px' }">
              <div v-for="d in weekDays" :key="d.dateStr" class="wk-col">
                <div v-for="h in allHours" :key="h" class="wk-cell" @click="onWeekCellClick(d.dateStr, h)"></div>
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
        <el-form-item label="时段"><div style="display:flex;gap:8px;align-items:center"><el-time-select v-model="quickBookForm.startTime" :max-time="quickBookForm.endTime" placeholder="开始" start="00:00" step="00:30" end="23:30" style="width:140px" /><span>~</span><el-time-select v-model="quickBookForm.endTime" :min-time="quickBookForm.startTime" placeholder="结束" start="00:00" step="00:30" end="23:30" style="width:140px" /></div></el-form-item>
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
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
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

// ====== 时间配置 ======
const START_HOUR = 0 // 数据从 00:00 开始
const END_HOUR = 24 // 数据到 24:00 结束
const TOTAL_HOURS = END_HOUR - START_HOUR
const HOUR_WIDTH = 60 // 每小时宽度 px
const hoursWidth = TOTAL_HOURS * HOUR_WIDTH // 总宽度
const VIEW_START = 9 // 视口默认从 09:00 开始
const ROW_H = 64

const allHours = computed(() => { const h = []; for (let i = START_HOUR; i < END_HOUR; i++) h.push(i); return h })

// ====== 滚动容器引用 ======
const dayBodyRef = ref<HTMLElement>()
const weekBodyRef = ref<HTMLElement>()
const headerScrollX = ref(0)

// ====== 同步滚动 ======
function onDayScroll() {
  if (dayBodyRef.value) {
    headerScrollX.value = dayBodyRef.value.scrollLeft
  }
}

function onWeekScroll() {
  // 周视图时间刻度在左侧固定，不需要同步
}

// ====== 滚动到默认时间 ======
function scrollToDefaultHour() {
  const scrollLeft = VIEW_START * HOUR_WIDTH
  headerScrollX.value = scrollLeft
  nextTick(() => {
    if (dayBodyRef.value) {
      dayBodyRef.value.scrollLeft = scrollLeft
    }
    if (weekBodyRef.value) {
      weekBodyRef.value.scrollTop = scrollLeft
    }
  })
}

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
function timeToPct(t: string) {
  const hour = parseInt(t.substring(11, 13))
  const minute = parseInt(t.substring(14, 16))
  const totalMinutes = TOTAL_HOURS * 60
  const minutes = (hour - START_HOUR) * 60 + minute
  return (minutes / totalMinutes) * 100
}
function durPct(s: string, e: string) { return timeToPct(e) - timeToPct(s) }
function isCurrentHour(h: number) { return new Date().getHours() === h }




async function loadData() {
  const d = currentDate.value; const p: Record<string, string> = {}
  if (viewMode.value === 'day') { p.date = formatDate(d) }
  else if (viewMode.value === 'week') { p.startDate = formatDate(weekDays.value[0].date); p.endDate = formatDate(weekDays.value[6].date) }
  else { const ms = new Date(d.getFullYear(), d.getMonth(), 1); ms.setDate(ms.getDate() - ms.getDay()); const me = new Date(ms); me.setDate(me.getDate() + 41); p.startDate = formatDate(ms); p.endDate = formatDate(me) }
  try { const r = await getSchedule(p); rooms.value = r.data.rooms || []; reservations.value = r.data.reservations || []; if (viewMode.value === 'month') buildMonthDays() } catch { /* */ }
  scrollToDefaultHour()
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
    height: Math.max(durPct(r.startTime, r.endTime), 1) + '%'
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
.day-view { padding: 0; overflow: hidden; display: flex; flex-direction: column; }
.day-header { display: flex; border-bottom: 2px solid #e5e7eb; background: #fafbfc; flex-shrink: 0; }
.room-col-header { width: 100px; padding: 10px 12px; font-size: 12px; font-weight: 600; color: #6b7280; flex-shrink: 0; border-right: 1px solid #e5e7eb; position: sticky; left: 0; z-index: 3; background: #fafbfc; }
.day-ticks-wrap { flex: 1; overflow: hidden; }
.day-ticks { display: flex; }
.tick { width: 60px; display: flex; flex-direction: column; align-items: flex-start; padding-top: 8px; flex-shrink: 0; }
.tick-label { font-size: 11px; color: #6b7280; font-weight: 500; }
.tick-mark { width: 1px; height: 6px; background: #d1d5db; margin-top: 4px; }

.day-body-wrap { flex: 1; overflow: auto; }
.day-body { position: relative; }
.day-row { display: flex; height: 64px; border-bottom: 1px solid #f0f0f0; position: relative; }
.room-label { width: 100px; padding: 6px 12px; border-right: 1px solid #e5e7eb; display: flex; flex-direction: column; justify-content: center; flex-shrink: 0; background: #fff; z-index: 2; position: sticky; left: 0; }
.room-name { font-size: 12px; font-weight: 600; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.room-meta { font-size: 10px; color: #9ca3af; margin-top: 2px; }
.day-grid { display: flex; flex-shrink: 0; }
.grid-cell { width: 60px; border-right: 1px solid #f3f4f6; cursor: pointer; flex-shrink: 0; }
.grid-cell:hover { background: #f9fafb; }

.day-event { position: absolute; border-radius: 6px; padding: 3px 6px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 1; transition: box-shadow 0.15s; left: 100px; width: calc(100% - 100px); }
.day-event:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }


/* ========== 周视图 ========== */
.week-view { padding: 0; overflow: hidden; display: flex; flex-direction: column; }
.week-header { display: flex; border-bottom: 1px solid #e5e7eb; background: #fafbfc; flex-shrink: 0; }
.wk-corner { width: 40px; flex-shrink: 0; border-right: 1px solid #e5e7eb; position: sticky; left: 0; z-index: 3; background: #fafbfc; }
.wk-day { flex: 1; padding: 6px 4px; text-align: center; border-right: 1px solid #f3f4f6; }
.wk-day:last-child { border-right: none; }
.wk-day.today { background: #ecf5ff; }
.wk-day.today .wk-day-num { background: #409eff; color: #fff; border-radius: 50%; }
.wk-day-name { font-size: 11px; color: #9ca3af; }
.wk-day-num { font-size: 14px; font-weight: 600; color: #303133; margin-top: 2px; display: inline-block; width: 28px; height: 28px; line-height: 28px; }

.week-body-wrap { flex: 1; overflow: auto; }
.week-body { display: flex; position: relative; }
.wk-times { width: 40px; flex-shrink: 0; position: sticky; left: 0; z-index: 2; background: #fff; }
.wk-time { height: 60px; display: flex; align-items: flex-start; padding-top: 0; border-bottom: 1px solid #f0f0f0; position: relative; }
.wk-time-label { font-size: 11px; color: #9ca3af; position: absolute; top: -7px; left: 4px; background: #fff; padding: 0 2px; }
.wk-time-now-label { color: #ef4444; font-weight: 600; }
.wk-time-mark { position: absolute; top: 0; left: 0; width: 6px; height: 1px; background: #d1d5db; }
.wk-time-now .wk-time-mark { background: #ef4444; width: 10px; }

.wk-grid-wrap { flex: 1; }
.wk-grid { position: relative; display: flex; }
.wk-col { flex: 1; display: flex; flex-direction: column; }
.wk-cell { height: 60px; border-right: 1px solid #f3f4f6; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.wk-cell:last-child { border-right: none; }
.wk-cell:hover { background: #f9fafb; }

.week-event { position: absolute; left: 0; border-radius: 6px; padding: 3px 6px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 1; transition: box-shadow 0.15s; }
.week-event:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.wk-now-line { position: absolute; top: 0; bottom: 0; width: 2px; background: #ef4444; z-index: 10; pointer-events: none; }
.wk-now-line::before { content: ''; position: absolute; top: -4px; left: -4px; width: 10px; height: 10px; background: #ef4444; border-radius: 50%; }

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
