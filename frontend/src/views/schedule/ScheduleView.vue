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
    <div
      v-if="viewMode === 'day'"
      class="day-view page-card"
      :class="{ dragging: dayDrag.active }"
      :style="{ '--hour-width': `${hourWidth}px`, '--day-edge-gap': `${dayEdgeGap}px` }"
    >
      <!-- 横轴刻度头（固定） -->
      <div class="day-header">
        <div class="room-col-header">会议室</div>
        <div class="day-ticks-wrap">
          <div class="day-ticks" :style="{ width: timelineWidth + 'px', transform: `translateX(${-headerScrollX}px)` }">
            <div v-for="h in allHours" :key="h" class="tick" >
              <span class="tick-label" >{{ String(h).padStart(2, '0') }}:00</span>
              <span class="tick-mark"></span>
            </div>
            <div class="tick tick-end">
              <span class="tick-label">24:00</span>
              <span class="tick-mark"></span>
            </div>
          </div>
        </div>
      </div>
      <!-- 内容区域（可滚动） -->
      <div
        ref="dayBodyRef"
        class="day-body-wrap"
        @scroll="onDayScroll"
        @pointerdown="onDayPointerDown"
        @pointermove="onDayPointerMove"
        @pointerup="onDayPointerUp"
        @pointercancel="onDayPointerUp"
      >
        <div class="day-body">
          <div
            class="day-work-hours-band"
            :style="{ left: `${ROOM_COLUMN_WIDTH + dayWorkHoursLeft}px`, width: `${dayWorkHoursWidth}px` }"
          ></div>
          <div
            class="day-work-hours-boundary"
            :style="{ left: `${ROOM_COLUMN_WIDTH + dayWorkHoursLeft}px` }"
          ></div>
          <div
            class="day-work-hours-boundary"
            :style="{ left: `${ROOM_COLUMN_WIDTH + dayWorkHoursLeft + dayWorkHoursWidth}px` }"
          ></div>
          <div
            v-if="showDayNowLine"
            class="day-now-column"
            :style="{ left: `${ROOM_COLUMN_WIDTH + dayNowLineLeft}px` }"
          >
            <span class="day-now-label">{{ currentTimeLabel }}</span>
          </div>
          <div v-for="room in rooms" :key="room.id" class="day-row">
            <div class="room-label">
              <div class="room-name">{{ room.name }}</div>
              <div class="room-meta">{{ room.capacity }}人</div>
            </div>
            <div class="day-grid-wrap" :style="{ width: dayTrackWidth + 'px' }">
              <div class="day-grid" :style="{ width: gridWidth + 'px' }">
                <div v-for="h in allHours" :key="h" class="grid-cell" @click="onDayCellClick(room, h)"></div>
              </div>
              <el-tooltip v-for="r in getRoomReservations(room.id)" :key="r.id"
                :content="`${r.subject || '未命名'}\n${formatTime(r.startTime)}-${formatTime(r.endTime)}\n${r.userName || ''}\n${r.roomName || ''}`"
                placement="top" raw-content>
                <div class="day-event" :class="'s' + r.status"
                  :style="dayEventStyle(r)"
                  @click="onDayEventClick(r)">
                  <div class="evt-inner">
                    <div class="evt-title">{{ r.subject || '未命名' }}</div>
                    <div class="evt-time">{{ formatTime(r.startTime) }}-{{ formatTime(r.endTime) }}</div>
                    <div class="evt-user">{{ r.userName || '' }}</div>
                  </div>
                </div>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 周视图 ==================== -->
    <div
      v-if="viewMode === 'week'"
      class="week-view page-card"
      :class="{ dragging: weekDrag.active }"
      :style="{ '--week-edge-gap': `${weekEdgeGap}px` }"
    >
      <div class="week-header">
        <div class="wk-corner"></div>
        <div v-for="d in weekDays" :key="d.dateStr" class="wk-day" :class="{ today: d.isToday }">
          <div class="wk-day-name">{{ d.dayName }}</div>
          <div class="wk-day-num">{{ d.dayNum }}</div>
        </div>
      </div>
      <div
        ref="weekBodyRef"
        class="week-body-wrap"
        @scroll="onWeekScroll"
        @pointerdown="onWeekPointerDown"
        @pointermove="onWeekPointerMove"
        @pointerup="onWeekPointerUp"
        @pointercancel="onWeekPointerUp"
      >
        <div class="week-body">
          <!-- 纵轴时间刻度（固定在左侧） -->
          <div class="wk-times" :style="{ height: weekTrackHeight + 'px' }">
            <div v-for="h in allHours" :key="h" class="wk-time" :class="{ 'wk-time-now': isCurrentHour(h) }">
              <span class="wk-time-label" :class="{ 'wk-time-now-label': isCurrentHour(h) }">{{ String(h).padStart(2, '0') }}:00</span>
              <span class="wk-time-mark"></span>
            </div>
          </div>
          <!-- 网格区域 -->
          <div class="wk-grid-wrap">
            <div class="wk-grid" :style="{ height: weekTrackHeight + 'px' }">
              <div class="wk-work-hours-band" :style="{ top: `${weekWorkHoursTop}px`, height: `${weekWorkHoursHeight}px` }"></div>
              <div class="wk-work-hours-boundary" :style="{ top: `${weekWorkHoursTop}px` }"></div>
              <div class="wk-work-hours-boundary" :style="{ top: `${weekWorkHoursTop + weekWorkHoursHeight}px` }"></div>
              <div v-for="d in weekDays" :key="d.dateStr" class="wk-col">
                <div v-for="h in allHours" :key="h" class="wk-cell" @click="onWeekCellClick(d.dateStr, h)"></div>
              </div>
              <div v-if="showWeekNowLine" class="wk-now-line" :style="{ top: `${weekNowLineTop}px`, left: weekNowLineLeft, width: weekNowLineWidth }">
                <span class="wk-now-label">{{ currentTimeLabel }}</span>
              </div>
              <el-tooltip v-for="r in weekReservations" :key="r.id"
                :content="`${r.roomName || ''} | ${r.subject || '未命名'}\n${formatTime(r.startTime)}-${formatTime(r.endTime)}\n${r.userName || ''}`"
                placement="top" raw-content>
                <div class="week-event" :class="'s' + r.status"
                  :style="weekEventStyle(r)"
                  @click="onWeekEventClick(r)">
                  <div class="evt-inner">
                    <div class="evt-title">{{ r.subject || '未命名' }}</div>
                    <div class="evt-time">{{ formatTime(r.startTime) }}-{{ formatTime(r.endTime) }}</div>
                    <div class="evt-room" v-if="(weekEventLayoutMap.get(r.id)?.totalColumns ?? 1) > 1">{{ r.roomName || '' }}</div>
                    <div class="evt-user">{{ r.userName || '' }}</div>
                  </div>
                </div>
              </el-tooltip>
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
        <el-descriptions-item label="预约人">{{ currentReservation.userName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="参会人数">{{ currentReservation.attendeeCount || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(currentReservation.status)" size="small">{{ statusText(currentReservation.status) }}</el-tag></el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 预约弹窗 -->
    <BookingDialog
      v-model="bookingVisible"
      :rooms="rooms"
      :room-id="bookingRoomId"
      :date="bookingDate"
      :start-time="bookingStartTime"
      :end-time="bookingEndTime"
      @success="onBookingSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ArrowLeft, ArrowRight, Plus } from '@element-plus/icons-vue'
import { getSchedule } from '@/api/reservation'
import BookingDialog from '@/components/BookingDialog.vue'

const viewMode = ref<'day' | 'week' | 'month'>('day')
const currentDate = ref(new Date())
const rooms = ref<any[]>([])
const reservations = ref<any[]>([])
const detailVisible = ref(false)
const currentReservation = ref<any>(null)

// 预约弹窗相关
const bookingVisible = ref(false)
const bookingRoomId = ref<number | undefined>(undefined)
const bookingDate = ref('')
const bookingStartTime = ref('')
const bookingEndTime = ref('')

// ====== 时间配置 ======
const START_HOUR = 0 // 数据从 00:00 开始
const END_HOUR = 24 // 数据到 24:00 结束
const TOTAL_HOURS = END_HOUR - START_HOUR
const ROOM_COLUMN_WIDTH = 120
const VISIBLE_HOURS = 9
const DEFAULT_HOUR_WIDTH = 60
const WEEK_HOUR_HEIGHT = 60
const DAY_EDGE_GAP_RATIO = 0.35
const WEEK_EDGE_GAP_RATIO = 0.35
const VIEW_START = 9 // 视口默认从 09:00 开始
const WORK_START_HOUR = 9
const WORK_END_HOUR = 18
const hourWidth = ref(DEFAULT_HOUR_WIDTH)
const gridWidth = computed(() => TOTAL_HOURS * hourWidth.value)
const dayEdgeGap = computed(() => hourWidth.value * DAY_EDGE_GAP_RATIO)
const dayTrackWidth = computed(() => gridWidth.value + dayEdgeGap.value * 2)
const timelineWidth = computed(() => dayTrackWidth.value + hourWidth.value)
const weekHoursHeight = TOTAL_HOURS * WEEK_HOUR_HEIGHT
const weekEdgeGap = WEEK_HOUR_HEIGHT * WEEK_EDGE_GAP_RATIO
const weekTrackHeight = weekHoursHeight + weekEdgeGap * 2
const nowTimestamp = ref(Date.now())

const allHours = computed(() => { const h = []; for (let i = START_HOUR; i < END_HOUR; i++) h.push(i); return h })

// ====== 滚动容器引用 ======
const dayBodyRef = ref<HTMLElement>()
const weekBodyRef = ref<HTMLElement>()
const headerScrollX = ref(0)
let dayBodyResizeObserver: ResizeObserver | null = null
let dayInertiaFrame = 0
let weekInertiaFrame = 0
let nowTimer: number | undefined

const dayDrag = reactive({
  active: false,
  pointerId: -1,
  startX: 0,
  startScrollLeft: 0,
  lastX: 0,
  lastTime: 0,
  velocity: 0,
  moved: false
})
const weekDrag = reactive({
  active: false,
  pointerId: -1,
  startY: 0,
  startScrollTop: 0,
  lastY: 0,
  lastTime: 0,
  velocity: 0,
  moved: false
})
const ignoreDayClick = ref(false)
const ignoreWeekClick = ref(false)

const todayString = computed(() => formatDate(new Date(nowTimestamp.value)))
const currentMinutes = computed(() => {
  const now = new Date(nowTimestamp.value)
  return now.getHours() * 60 + now.getMinutes()
})
const currentTimeLabel = computed(() => {
  const now = new Date(nowTimestamp.value)
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
})
const showDayNowLine = computed(() => formatDate(currentDate.value) === todayString.value)
const dayNowLineLeft = computed(() => dayEdgeGap.value + (currentMinutes.value / (TOTAL_HOURS * 60)) * gridWidth.value)
const showWeekNowLine = computed(() => weekDays.value.some(d => d.dateStr === todayString.value))
const weekNowLineTop = computed(() => weekEdgeGap + (currentMinutes.value / (TOTAL_HOURS * 60)) * weekHoursHeight)
const weekNowLineTodayIndex = computed(() => weekDays.value.findIndex(d => d.dateStr === todayString.value))
const weekNowLineLeft = computed(() => (weekNowLineTodayIndex.value / 7) * 100 + '%')
const weekNowLineWidth = computed(() => (100 / 7) + '%')
const dayWorkHoursLeft = computed(() => dayEdgeGap.value + (WORK_START_HOUR - START_HOUR) * hourWidth.value)
const dayWorkHoursWidth = computed(() => (WORK_END_HOUR - WORK_START_HOUR) * hourWidth.value)
const weekWorkHoursTop = computed(() => weekEdgeGap + (WORK_START_HOUR - START_HOUR) * WEEK_HOUR_HEIGHT)
const weekWorkHoursHeight = computed(() => (WORK_END_HOUR - WORK_START_HOUR) * WEEK_HOUR_HEIGHT)

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
function setDayScrollLeft(scrollLeft: number) {
  if (!dayBodyRef.value) return
  dayBodyRef.value.scrollLeft = scrollLeft
  headerScrollX.value = scrollLeft
}

function setWeekScrollTop(scrollTop: number) {
  if (!weekBodyRef.value) return
  weekBodyRef.value.scrollTop = scrollTop
}

function cancelDayInertia() {
  if (dayInertiaFrame) {
    cancelAnimationFrame(dayInertiaFrame)
    dayInertiaFrame = 0
  }
}

function cancelWeekInertia() {
  if (weekInertiaFrame) {
    cancelAnimationFrame(weekInertiaFrame)
    weekInertiaFrame = 0
  }
}

function startDayInertia(initialVelocity: number) {
  if (!dayBodyRef.value || Math.abs(initialVelocity) < 0.02) return
  cancelDayInertia()
  let velocity = initialVelocity
  let lastTime = performance.now()
  const step = (now: number) => {
    if (!dayBodyRef.value) return
    const dt = now - lastTime
    lastTime = now
    const nextScrollLeft = dayBodyRef.value.scrollLeft + velocity * dt
    setDayScrollLeft(nextScrollLeft)
    velocity *= Math.pow(0.95, dt / 16)
    const atEdge = dayBodyRef.value.scrollLeft <= 0 || dayBodyRef.value.scrollLeft >= dayBodyRef.value.scrollWidth - dayBodyRef.value.clientWidth
    if (Math.abs(velocity) < 0.02 || atEdge) {
      dayInertiaFrame = 0
      return
    }
    dayInertiaFrame = requestAnimationFrame(step)
  }
  dayInertiaFrame = requestAnimationFrame(step)
}

function startWeekInertia(initialVelocity: number) {
  if (!weekBodyRef.value || Math.abs(initialVelocity) < 0.02) return
  cancelWeekInertia()
  let velocity = initialVelocity
  let lastTime = performance.now()
  const step = (now: number) => {
    if (!weekBodyRef.value) return
    const dt = now - lastTime
    lastTime = now
    weekBodyRef.value.scrollTop += velocity * dt
    velocity *= Math.pow(0.95, dt / 16)
    const atEdge = weekBodyRef.value.scrollTop <= 0 || weekBodyRef.value.scrollTop >= weekBodyRef.value.scrollHeight - weekBodyRef.value.clientHeight
    if (Math.abs(velocity) < 0.02 || atEdge) {
      weekInertiaFrame = 0
      return
    }
    weekInertiaFrame = requestAnimationFrame(step)
  }
  weekInertiaFrame = requestAnimationFrame(step)
}

function updateDayHourWidth() {
  if (!dayBodyRef.value) return
  const viewportWidth = dayBodyRef.value.clientWidth - ROOM_COLUMN_WIDTH
  if (viewportWidth <= 0) return
  hourWidth.value = Number((viewportWidth / (VISIBLE_HOURS + DAY_EDGE_GAP_RATIO * 2)).toFixed(2))
}

function scrollToDefaultHour() {
  updateDayHourWidth()
  const scrollLeft = VIEW_START * hourWidth.value
  const scrollTop = weekEdgeGap + VIEW_START * WEEK_HOUR_HEIGHT
  headerScrollX.value = scrollLeft
  // 多次尝试确保 DOM 渲染完成
  const tryScroll = (attempt: number) => {
    if (attempt > 10) return
    nextTick(() => {
      if (dayBodyRef.value) {
        setDayScrollLeft(scrollLeft)
      }
      if (weekBodyRef.value) {
        setWeekScrollTop(scrollTop)
      }
      // 验证是否滚动成功
      const dayScrollPending = !!dayBodyRef.value && dayBodyRef.value.scrollLeft !== scrollLeft
      const weekScrollPending = !!weekBodyRef.value && weekBodyRef.value.scrollTop !== scrollTop
      if (dayScrollPending || weekScrollPending) {
        setTimeout(() => tryScroll(attempt + 1), 100)
      }
    })
  }
  setTimeout(() => tryScroll(0), 100)
}

function observeDayBodyResize() {
  dayBodyResizeObserver?.disconnect()
  if (!dayBodyRef.value || typeof ResizeObserver === 'undefined') return

  dayBodyResizeObserver = new ResizeObserver(() => {
    if (!dayBodyRef.value) return
    const currentHourOffset = dayBodyRef.value.scrollLeft / hourWidth.value
    updateDayHourWidth()
    nextTick(() => {
      setDayScrollLeft(currentHourOffset * hourWidth.value)
    })
  })

  dayBodyResizeObserver.observe(dayBodyRef.value)
}

function onDayPointerDown(event: PointerEvent) {
  if (event.button !== 0 || !dayBodyRef.value) return
  cancelDayInertia()
  dayDrag.active = true
  dayDrag.pointerId = event.pointerId
  dayDrag.startX = event.clientX
  dayDrag.startScrollLeft = dayBodyRef.value.scrollLeft
  dayDrag.lastX = event.clientX
  dayDrag.lastTime = performance.now()
  dayDrag.velocity = 0
  dayDrag.moved = false
  dayBodyRef.value.setPointerCapture(event.pointerId)
}

function onDayPointerMove(event: PointerEvent) {
  if (!dayDrag.active || dayDrag.pointerId !== event.pointerId || !dayBodyRef.value) return
  const deltaX = event.clientX - dayDrag.startX
  if (!dayDrag.moved && Math.abs(deltaX) > 4) {
    dayDrag.moved = true
  }
  if (!dayDrag.moved) return
  const now = performance.now()
  const dt = Math.max(now - dayDrag.lastTime, 1)
  dayDrag.velocity = -((event.clientX - dayDrag.lastX) / dt)
  dayDrag.lastX = event.clientX
  dayDrag.lastTime = now
  setDayScrollLeft(dayDrag.startScrollLeft - deltaX)
  event.preventDefault()
}

function onDayPointerUp(event: PointerEvent) {
  if (!dayDrag.active || dayDrag.pointerId !== event.pointerId || !dayBodyRef.value) return
  const didMove = dayDrag.moved
  if (dayBodyRef.value.hasPointerCapture(event.pointerId)) {
    dayBodyRef.value.releasePointerCapture(event.pointerId)
  }
  dayDrag.active = false
  dayDrag.pointerId = -1
  dayDrag.moved = false
  if (didMove) {
    startDayInertia(dayDrag.velocity * 18)
    ignoreDayClick.value = true
    setTimeout(() => { ignoreDayClick.value = false }, 0)
  }
}

function onWeekPointerDown(event: PointerEvent) {
  if (event.button !== 0 || !weekBodyRef.value) return
  cancelWeekInertia()
  weekDrag.active = true
  weekDrag.pointerId = event.pointerId
  weekDrag.startY = event.clientY
  weekDrag.startScrollTop = weekBodyRef.value.scrollTop
  weekDrag.lastY = event.clientY
  weekDrag.lastTime = performance.now()
  weekDrag.velocity = 0
  weekDrag.moved = false
  weekBodyRef.value.setPointerCapture(event.pointerId)
}

function onWeekPointerMove(event: PointerEvent) {
  if (!weekDrag.active || weekDrag.pointerId !== event.pointerId || !weekBodyRef.value) return
  const deltaY = event.clientY - weekDrag.startY
  if (!weekDrag.moved && Math.abs(deltaY) > 4) {
    weekDrag.moved = true
  }
  if (!weekDrag.moved) return
  const now = performance.now()
  const dt = Math.max(now - weekDrag.lastTime, 1)
  weekDrag.velocity = -((event.clientY - weekDrag.lastY) / dt)
  weekDrag.lastY = event.clientY
  weekDrag.lastTime = now
  weekBodyRef.value.scrollTop = weekDrag.startScrollTop - deltaY
  event.preventDefault()
}

function onWeekPointerUp(event: PointerEvent) {
  if (!weekDrag.active || weekDrag.pointerId !== event.pointerId || !weekBodyRef.value) return
  const didMove = weekDrag.moved
  if (weekBodyRef.value.hasPointerCapture(event.pointerId)) {
    weekBodyRef.value.releasePointerCapture(event.pointerId)
  }
  weekDrag.active = false
  weekDrag.pointerId = -1
  weekDrag.moved = false
  if (didMove) {
    startWeekInertia(weekDrag.velocity * 18)
    ignoreWeekClick.value = true
    setTimeout(() => { ignoreWeekClick.value = false }, 0)
  }
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

// 周视图重叠布局：按天分组，将同时段重叠事件分配到不同列（列宽固定，事件宽度在列内等分缩窄）
const weekEventLayoutMap = computed(() => {
  const layout = new Map<number, { columnIndex: number; totalColumns: number }>()
  const eventsByDay = new Map<string, any[]>()
  for (const r of weekReservations.value) {
    const dayStr = r.startTime.split('T')[0]
    if (!eventsByDay.has(dayStr)) eventsByDay.set(dayStr, [])
    eventsByDay.get(dayStr)!.push(r)
  }
  for (const [, dayEvents] of eventsByDay) {
    if (dayEvents.length === 0) continue
    // 按开始时间升序、时长降序排序，便于贪心列分配
    dayEvents.sort((a, b) => {
      const aStart = new Date(a.startTime).getTime()
      const bStart = new Date(b.startTime).getTime()
      if (aStart !== bStart) return aStart - bStart
      const aDur = new Date(a.endTime).getTime() - aStart
      const bDur = new Date(b.endTime).getTime() - bStart
      return bDur - aDur
    })
    // 贪心分配列：找第一个不冲突的列，没有则新建一列
    const eventColumns: number[] = []
    const columnEnds: number[] = []
    for (const event of dayEvents) {
      const eStart = new Date(event.startTime).getTime()
      let placed = false
      for (let ci = 0; ci < columnEnds.length; ci++) {
        if (eStart >= columnEnds[ci]) {
          columnEnds[ci] = new Date(event.endTime).getTime()
          eventColumns.push(ci)
          placed = true
          break
        }
      }
      if (!placed) {
        eventColumns.push(columnEnds.length)
        columnEnds.push(new Date(event.endTime).getTime())
      }
    }
    // 并查集找连通分量：直接或间接重叠的事件共享同一组最大列数
    const n = dayEvents.length
    const parent = Array.from({ length: n }, (_, i) => i)
    function find(i: number): number {
      while (parent[i] !== i) { parent[i] = parent[parent[i]]; i = parent[i] }
      return i
    }
    function union(i: number, j: number) { parent[find(i)] = find(j) }
    for (let i = 0; i < n; i++) {
      for (let j = i + 1; j < n; j++) {
        const iEnd = new Date(dayEvents[i].endTime).getTime()
        const jStart = new Date(dayEvents[j].startTime).getTime()
        if (jStart < iEnd) { union(i, j) } else { break }
      }
    }
    const groupMaxCol = new Map<number, number>()
    for (let i = 0; i < n; i++) {
      const root = find(i)
      groupMaxCol.set(root, Math.max(groupMaxCol.get(root) ?? 0, eventColumns[i] + 1))
    }
    for (let i = 0; i < n; i++) {
      layout.set(dayEvents[i].id, { columnIndex: eventColumns[i], totalColumns: groupMaxCol.get(find(i))! })
    }
  }
  return layout
})

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
function isCurrentHour(h: number) { return new Date().getHours() === h }




async function loadData() {
  const d = currentDate.value; const p: Record<string, string> = {}
  if (viewMode.value === 'day') { p.date = formatDate(d) }
  else if (viewMode.value === 'week') { p.startDate = formatDate(weekDays.value[0].date); p.endDate = formatDate(weekDays.value[6].date) }
  else { const ms = new Date(d.getFullYear(), d.getMonth(), 1); ms.setDate(ms.getDate() - ms.getDay()); const me = new Date(ms); me.setDate(me.getDate() + 41); p.startDate = formatDate(ms); p.endDate = formatDate(me) }
  try { const r = await getSchedule(p); rooms.value = r.data.rooms || []; reservations.value = r.data.reservations || []; if (viewMode.value === 'month') buildMonthDays() } catch { /* */ }
  await nextTick()
  scrollToDefaultHour()
}

// ====== 日视图 ======
function getRoomReservations(roomId: number) {
  const today = formatDate(currentDate.value)
  return reservations.value.filter(r => r.roomId === roomId && r.startTime.split('T')[0] === today)
}
function dayEventStyle(r: any) {
  // 使用像素值计算位置，与 hoursWidth 对齐
  const start = new Date(r.startTime)
  const end = new Date(r.endTime)
  const startMinutes = (start.getHours() - START_HOUR) * 60 + start.getMinutes()
  const durationMinutes = (end.getTime() - start.getTime()) / 60000
  const leftPx = dayEdgeGap.value + (startMinutes / (TOTAL_HOURS * 60)) * gridWidth.value
  const widthPx = (durationMinutes / (TOTAL_HOURS * 60)) * gridWidth.value
  return { left: leftPx + 'px', width: widthPx + 'px' }
}
function onDayCellClick(room: any, h: number) {
  if (ignoreDayClick.value) return
  const dateStr = formatDate(currentDate.value)
  bookingRoomId.value = room.id
  bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T${String(h).padStart(2, '0')}:00:00`
  bookingEndTime.value = `${dateStr}T${String(h + 1).padStart(2, '0')}:00:00`
  bookingVisible.value = true
}
function onDayEventClick(r: any) {
  if (ignoreDayClick.value) return
  showDetail(r)
}

// ====== 周视图 ======
function weekEventStyle(r: any) {
  const di = weekDays.value.findIndex(d => d.dateStr === r.startTime.split('T')[0])
  if (di < 0) return { display: 'none' }
  const start = new Date(r.startTime)
  const end = new Date(r.endTime)
  const startMinutes = (start.getHours() - START_HOUR) * 60 + start.getMinutes()
  const durationMinutes = (end.getTime() - start.getTime()) / 60000
  const topPx = weekEdgeGap + (startMinutes / (TOTAL_HOURS * 60)) * weekHoursHeight
  const heightPx = Math.max((durationMinutes / (TOTAL_HOURS * 60)) * weekHoursHeight, 16)
  // 列宽固定为 100/7，重叠事件在列内等分缩窄宽度
  const dayWidth = 100 / 7
  const info = weekEventLayoutMap.value.get(r.id)
  const col = info?.columnIndex ?? 0
  const total = info?.totalColumns ?? 1
  const gapPct = total > 1 ? 0.3 : 0
  const eventWidth = (dayWidth - (total - 1) * gapPct) / total
  const eventLeft = di * dayWidth + col * (eventWidth + gapPct)
  return {
    left: eventLeft + '%',
    width: eventWidth + '%',
    top: topPx + 'px',
    height: heightPx + 'px'
  }
}
function onWeekCellClick(dateStr: string, h: number) {
  if (ignoreWeekClick.value) return
  bookingRoomId.value = undefined
  bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T${String(h).padStart(2, '0')}:00:00`
  bookingEndTime.value = `${dateStr}T${String(h + 1).padStart(2, '0')}:00:00`
  bookingVisible.value = true
}
function onWeekEventClick(r: any) {
  if (ignoreWeekClick.value) return
  showDetail(r)
}

// ====== 月视图 ======
function getDayReservations(day: any) { return expandedDays.value.has(day.dateStr) ? day.reservations : day.reservations.slice(0, 3) }
function toggleDayExpand(ds: string) { expandedDays.value.has(ds) ? expandedDays.value.delete(ds) : expandedDays.value.add(ds) }
function buildMonthDays() {
  const d = currentDate.value, m = d.getMonth(), today = formatDate(new Date())
  const ms = new Date(d.getFullYear(), m, 1); ms.setDate(ms.getDate() - ms.getDay())
  monthDays.value = Array.from({ length: 42 }, (_, i) => { const dt = new Date(ms); dt.setDate(dt.getDate() + i); const ds = formatDate(dt); return { date: dt.getDate(), dateStr: ds, currentMonth: dt.getMonth() === m, isToday: ds === today, reservations: reservations.value.filter(r => r.startTime.split('T')[0] === ds) } })
}
function onMonthCellClick(day: any) {
  bookingRoomId.value = undefined
  bookingDate.value = day.dateStr
  bookingStartTime.value = ''
  bookingEndTime.value = ''
  bookingVisible.value = true
}

// ====== 导航 ======
function goPrev() { const d = new Date(currentDate.value); if (viewMode.value === 'day') d.setDate(d.getDate() - 1); else if (viewMode.value === 'week') d.setDate(d.getDate() - 7); else d.setMonth(d.getMonth() - 1); currentDate.value = d }
function goNext() { const d = new Date(currentDate.value); if (viewMode.value === 'day') d.setDate(d.getDate() + 1); else if (viewMode.value === 'week') d.setDate(d.getDate() + 7); else d.setMonth(d.getMonth() + 1); currentDate.value = d }
function goToday() { currentDate.value = new Date() }
function openQuickBook() {
  const dateStr = formatDate(currentDate.value)
  bookingRoomId.value = undefined
  bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T09:00:00`
  bookingEndTime.value = `${dateStr}T10:00:00`
  bookingVisible.value = true
}
function onBookingSuccess() {
  loadData()
}
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info' }
function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function showDetail(r: any) { currentReservation.value = r; detailVisible.value = true }

watch([viewMode, currentDate], loadData)
watch(viewMode, async (mode) => {
  await nextTick()
  if (mode === 'day') {
    observeDayBodyResize()
  }
  scrollToDefaultHour()
})

onMounted(async () => {
  await nextTick()
  observeDayBodyResize()
  nowTimer = window.setInterval(() => {
    nowTimestamp.value = Date.now()
  }, 60000)
  loadData()
})

onBeforeUnmount(() => {
  cancelDayInertia()
  cancelWeekInertia()
  dayBodyResizeObserver?.disconnect()
  if (nowTimer) {
    clearInterval(nowTimer)
  }
})
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.control-bar { display: flex; justify-content: space-between; align-items: center; background: #fff; border-radius: 12px; padding: 12px 20px; border: 1px solid #f0f0f0; }
.control-left { display: flex; align-items: center; gap: 12px; }
.date-display { font-size: 14px; color: #303133; font-weight: 500; margin-left: 8px; }

/* ========== 日视图 ========== */
.day-view {
  --room-column-width: 120px;
  --hour-width: 60px;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.day-header { display: flex; border-bottom: 2px solid #e5e7eb; background: #fafbfc; flex-shrink: 0; }
.room-col-header {
  width: var(--room-column-width);
  box-sizing: border-box;
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  flex-shrink: 0;
  border-right: 1px solid #e5e7eb;
  position: sticky;
  left: 0;
  z-index: 3;
  background: #fafbfc;
}
.day-ticks-wrap { flex: 1; overflow: hidden; }
.day-ticks {
  display: flex;
  position: relative;
  min-height: 34px;
  box-sizing: border-box;
  padding: 0 var(--day-edge-gap);
}
.tick {
  width: var(--hour-width);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding-top: 8px;
  flex-shrink: 0;
}
.tick-label { font-size: 11px; color: #6b7280; font-weight: 500; }
.tick-mark { width: 1px; height: 6px; background: #d1d5db; margin-top: 4px; }
.tick-end { position: relative; }
.tick-end .tick-label {
  transform: translateX(-50%);
}

.day-body-wrap {
  flex: 1;
  overflow: auto;
  max-height: 600px;
  scrollbar-gutter: stable both-edges;
  scrollbar-color: #cbd5e1 #f3f4f6;
  cursor: grab;
}
.day-body-wrap::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}
.day-body-wrap::-webkit-scrollbar-track {
  background: #f3f4f6;
}
.day-body-wrap::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
  border: 2px solid #f3f4f6;
}
.day-body { position: relative; min-width: fit-content; }
.day-work-hours-band {
  position: absolute;
  top: 0;
  bottom: 0;
  background: rgba(59, 130, 246, 0.05);
  pointer-events: none;
  z-index: 0;
}
.day-work-hours-boundary {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: rgba(59, 130, 246, 0.15);
  pointer-events: none;
  z-index: 1;
}
.day-now-column {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: rgba(59, 130, 246, 0.8);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.9);
  z-index: 3;
  pointer-events: none;
}
.day-now-column::after {
  content: '';
  position: absolute;
  top: -4px;
  left: -4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3b82f6;
}
.day-now-label {
  position: absolute;
  top: 6px;
  left: 8px;
  padding: 2px 6px;
  border-radius: 999px;
  background: #3b82f6;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}
.day-row { display: flex; height: 64px; border-bottom: 1px solid #f0f0f0; position: relative; }
.day-view.dragging,
.day-view.dragging * {
  cursor: grabbing !important;
  user-select: none;
}
.room-label {
  width: var(--room-column-width);
  box-sizing: border-box;
  padding: 6px 12px;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex-shrink: 0;
  background: #fff;
  z-index: 4;
  position: sticky;
  left: 0;
}
.room-name { font-size: 12px; font-weight: 600; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.room-meta { font-size: 10px; color: #9ca3af; margin-top: 2px; }
.day-grid-wrap {
  position: relative;
  flex-shrink: 0;
  box-sizing: border-box;
  padding: 0 var(--day-edge-gap);
  overflow: hidden;
}
.day-grid { display: flex; flex-shrink: 0; height: 100%; }
.grid-cell {
  width: var(--hour-width);
  box-sizing: border-box;
  border-right: 1px solid #f3f4f6;
  cursor: pointer;
  flex-shrink: 0;
}
.grid-cell:hover { background: #f9fafb; }

.day-event { position: absolute; top: 4px; bottom: 4px; border-radius: 6px; padding: 3px 6px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 1; transition: box-shadow 0.15s; }
.day-event:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.day-event { z-index: 2; }


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

.week-body-wrap {
  flex: 1;
  overflow: auto;
  max-height: 600px;
  scrollbar-gutter: stable both-edges;
  scrollbar-color: #cbd5e1 #f3f4f6;
  cursor: grab;
}
.week-body-wrap::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}
.week-body-wrap::-webkit-scrollbar-track {
  background: #f3f4f6;
}
.week-body-wrap::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
  border: 2px solid #f3f4f6;
}
.week-view.dragging,
.week-view.dragging * {
  cursor: grabbing !important;
  user-select: none;
}
.week-body { display: flex; position: relative; }
.wk-times {
  width: 40px;
  flex-shrink: 0;
  position: sticky;
  left: 0;
  z-index: 2;
  background: #fff;
  box-sizing: border-box;
  padding: var(--week-edge-gap) 0;
}
.wk-time { height: 60px; display: flex; align-items: flex-start; padding-top: 0; border-bottom: 1px solid #f0f0f0; position: relative; }
.wk-time-label { font-size: 11px; color: #9ca3af; position: absolute; top: -7px; left: 4px; background: #fff; padding: 0 2px; }
.wk-time-now-label { color: #ef4444; font-weight: 600; }
.wk-time-mark { position: absolute; top: 0; left: 0; width: 6px; height: 1px; background: #d1d5db; }
.wk-time-now .wk-time-mark { background: #ef4444; width: 10px; }

.wk-grid-wrap { flex: 1; }
.wk-grid {
  position: relative;
  display: flex;
  box-sizing: border-box;
  padding: var(--week-edge-gap) 0;
}
.wk-work-hours-band {
  position: absolute;
  left: 0;
  right: 0;
  background: rgba(59, 130, 246, 0.05);
  pointer-events: none;
  z-index: 0;
}
.wk-work-hours-boundary {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(59, 130, 246, 0.15);
  pointer-events: none;
  z-index: 1;
}
.wk-now-line {
  position: absolute;
  height: 2px;
  background: rgba(59, 130, 246, 0.8);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.9);
  z-index: 1;
  pointer-events: none;
}
.wk-now-line::before {
  content: '';
  position: absolute;
  left: -6px;
  top: -4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3b82f6;
}
.wk-now-label {
  position: absolute;
  top: -12px;
  left: 8px;
  padding: 2px 6px;
  border-radius: 999px;
  background: #3b82f6;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}
.wk-col { flex: 1; display: flex; flex-direction: column; position: relative; }
.wk-cell { height: 60px; border-right: 1px solid #f3f4f6; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.wk-cell:last-child { border-right: none; }
.wk-cell:hover { background: #f9fafb; }

.week-event { position: absolute; left: 0; border-radius: 6px; padding: 3px 6px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 1; transition: box-shadow 0.15s; }
.week-event:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.week-event { z-index: 2; }

/* ========== 通用预约块 ========== */
.s0 { background: #fef3cd; border-left: 3px solid #f59e0b; }
.s1 { background: #d1fae5; border-left: 3px solid #10b981; }
.s2 { background: #f3f4f6; border-left: 3px solid #9ca3af; }
.evt-inner { height: 100%; display: flex; flex-direction: column; justify-content: center; overflow: hidden; }
.evt-title { font-weight: 500; color: #374151; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 12px; }
.evt-time { font-size: 10px; color: #6b7280; margin-top: 1px; white-space: nowrap; }
.evt-room { font-size: 10px; color: #6b7280; margin-top: 1px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
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
