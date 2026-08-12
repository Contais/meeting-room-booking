<template>
  <div class="room-schedule">
    <!-- 控制栏 -->
    <div class="schedule-controls">
      <div class="control-left">
        <button class="ctrl-btn" @click="goToday">今天</button>
        <button class="ctrl-icon" @click="goPrev"><el-icon><ArrowLeft /></el-icon></button>
        <button class="ctrl-icon" @click="goNext"><el-icon><ArrowRight /></el-icon></button>
        <div class="view-segment">
          <button class="seg-item" :class="{ active: viewMode === 'day' }" @click="viewMode = 'day'">日</button>
          <button class="seg-item" :class="{ active: viewMode === 'week' }" @click="viewMode = 'week'">周</button>
          <button class="seg-item" :class="{ active: viewMode === 'month' }" @click="viewMode = 'month'">月</button>
        </div>
        <span class="date-display">{{ dateDisplay }}</span>
      </div>
      <div class="control-right">
        <button class="ctrl-btn primary" @click="openQuickBook">
          <el-icon><Plus /></el-icon> 预约
        </button>
      </div>
    </div>

    <!-- 日视图 -->
    <div
      v-if="viewMode === 'day'"
      class="day-view"
      :class="{ dragging: dayDrag.active }"
      :style="{ '--hour-width': `${hourWidth}px`, '--day-edge-gap': `${dayEdgeGap}px` }"
    >
      <div class="day-header">
        <div class="time-col-header">时间</div>
        <div class="day-ticks-wrap">
          <div class="day-ticks" :style="{ width: timelineWidth + 'px', transform: `translateX(${-headerScrollX}px)` }">
            <div v-for="h in allHours" :key="h" class="tick">
              <span class="tick-label">{{ String(h).padStart(2, '0') }}:00</span>
              <span class="tick-mark"></span>
            </div>
            <div class="tick tick-end">
              <span class="tick-label">24:00</span>
              <span class="tick-mark"></span>
            </div>
          </div>
        </div>
      </div>
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
            :style="{ left: `${dayWorkHoursLeft}px`, width: `${dayWorkHoursWidth}px` }"
          ></div>
          <div
            class="day-work-hours-boundary"
            :style="{ left: `${dayWorkHoursLeft}px` }"
          ></div>
          <div
            class="day-work-hours-boundary"
            :style="{ left: `${dayWorkHoursLeft + dayWorkHoursWidth}px` }"
          ></div>
          <div
            v-if="showDayNowLine"
            class="day-now-column"
            :style="{ left: `${dayNowLineLeft}px` }"
          >
            <span class="day-now-label">{{ currentTimeLabel }}</span>
          </div>
          <div class="day-grid-wrap" :style="{ width: dayTrackWidth + 'px' }">
            <div class="day-grid" :style="{ width: gridWidth + 'px' }">
              <div v-for="h in allHours" :key="h" class="grid-cell" @click="onDayCellClick(h)"></div>
            </div>
            <el-tooltip v-for="r in dayReservations" :key="r.id"
              :content="`${r.subject || '未命名'}\n${formatTime(r.startTime)}-${formatTime(r.endTime)}\n${r.userName || ''}`"
              placement="top" raw-content>
              <div class="day-event evt-block" :class="'s' + r.status"
                :style="dayEventStyle(r)"
                @click.stop="onDayEventClick(r)">
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

    <!-- 周视图 -->
    <div
      v-if="viewMode === 'week'"
      class="week-view"
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
          <div class="wk-times" :style="{ height: weekTrackHeight + 'px' }">
            <div v-for="h in allHours" :key="h" class="wk-time" :class="{ 'wk-time-now': isCurrentHour(h) }">
              <span class="wk-time-label" :class="{ 'wk-time-now-label': isCurrentHour(h) }">{{ String(h).padStart(2, '0') }}:00</span>
              <span class="wk-time-mark"></span>
            </div>
          </div>
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
                :content="`${r.subject || '未命名'}\n${formatTime(r.startTime)}-${formatTime(r.endTime)}\n${r.userName || ''}`"
                placement="top" raw-content>
                <div class="week-event evt-block" :class="'s' + r.status"
                  :style="weekEventStyle(r)"
                  @click.stop="onWeekEventClick(r)">
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

    <!-- 月视图 -->
    <div v-if="viewMode === 'month'" class="month-view">
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
              class="mc-event evt-block" :class="'s' + r.status"
              @click.stop="showDetail(r)">
              <span class="mc-event-time">{{ formatTime(r.startTime) }}</span>
              <span class="mc-event-title">{{ r.subject || '未命名' }}</span>
            </div>
            <el-popover v-if="day.reservations.length > 3"
              :visible="morePopoverDay === day.dateStr"
              trigger="click" placement="top" :width="260"
              popper-class="mc-more-popover"
              @update:visible="(v: boolean) => onMorePopoverToggle(day.dateStr, v)">
              <template #reference>
                <button class="mc-more" @click.stop="onMorePopoverToggle(day.dateStr, morePopoverDay !== day.dateStr)">+{{ day.reservations.length - 2 }} 更多</button>
              </template>
              <div class="mc-pop-list">
                <div class="mc-pop-head">{{ day.dateStr }} · 共 {{ day.reservations.length }} 场</div>
                <div v-for="r in day.reservations" :key="r.id"
                  class="mc-pop-item evt-block" :class="'s' + r.status"
                  @click="onPopoverEventClick(r)">
                  <span class="mc-pop-time">{{ formatTime(r.startTime) }}</span>
                  <span class="mc-pop-title">{{ r.subject || '未命名' }}</span>
                </div>
              </div>
            </el-popover>
          </div>
        </div>
      </div>
    </div>

    <!-- 预约详情抽屉（与日历视图交互一致） -->
    <el-drawer v-model="detailVisible" direction="rtl" size="380px" :with-header="false" :body-style="{ padding: '0', overflow: 'hidden' }" class="rs-detail-drawer">
      <div class="rs-drawer">
        <div class="drawer-head">
          <span class="drawer-title">预约详情</span>
          <button class="drawer-close" @click="detailVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div v-loading="detailLoading" class="drawer-body">
          <template v-if="currentDetail">
            <div class="detail-status-bar" :class="'s' + currentDetail.status">
              <span class="status-dot"></span>
              <span class="status-text">{{ statusText(currentDetail.status) }}</span>
            </div>
            <h3 class="detail-subject">{{ currentDetail.subject || '未命名会议' }}</h3>
            <div class="detail-row">
              <el-icon><Clock /></el-icon>
              <div>
                <div class="detail-row-val">{{ detailTimeText }}</div>
                <div class="detail-row-sub">会议时间</div>
              </div>
            </div>
            <div class="detail-row">
              <el-icon><OfficeBuilding /></el-icon>
              <div>
                <div class="detail-row-val">{{ currentDetail.roomName || roomInfo?.name || '-' }}</div>
                <div class="detail-row-sub">会议室</div>
              </div>
            </div>
            <div class="detail-row">
              <el-icon><User /></el-icon>
              <div>
                <div class="detail-row-val">{{ currentDetail.username || currentDetail.userName || '-' }}</div>
                <div class="detail-row-sub">预约人</div>
              </div>
            </div>
            <div class="detail-row">
              <el-icon><UserFilled /></el-icon>
              <div>
                <div class="detail-row-val">{{ currentDetail.attendeeCount || 0 }} 人</div>
                <div class="detail-row-sub">参会人</div>
              </div>
            </div>
            <div v-if="currentDetail.remark" class="detail-row">
              <el-icon><Document /></el-icon>
              <div>
                <div class="detail-row-val">{{ currentDetail.remark }}</div>
                <div class="detail-row-sub">备注</div>
              </div>
            </div>
            <div v-if="currentDetail.rejectReason" class="detail-row">
              <el-icon><WarningFilled /></el-icon>
              <div>
                <div class="detail-row-val danger">{{ currentDetail.rejectReason }}</div>
                <div class="detail-row-sub">拒绝原因</div>
              </div>
            </div>
            <div v-if="currentDetail.attendees && currentDetail.attendees.length" class="detail-attendees">
              <div class="attendees-title">参会人 ({{ currentDetail.attendees.length }})</div>
              <div v-for="a in currentDetail.attendees" :key="a.userId" class="attendee-item">
                <div class="attendee-avatar"><UserAvatar :avatar="a.avatar" :username="a.realName || a.username" size="sm" /></div>
                <div class="attendee-info">
                  <div class="attendee-name">{{ a.realName || a.username }}</div>
                  <div class="attendee-dept">{{ a.departmentName || '未分配部门' }}</div>
                </div>
                <el-tag size="small" :type="attendeeStatusType(a.status)" effect="light">
                  {{ attendeeStatusText(a.status) }}
                </el-tag>
              </div>
            </div>
          </template>
          <el-empty v-else-if="!detailLoading" description="暂无详情" :image-size="80" />
        </div>
        <div v-if="currentDetail" class="drawer-foot">
          <button v-if="canCancel" class="drawer-btn danger" @click="handleCancel">取消预约</button>
          <button class="drawer-btn primary" @click="handleViewFull">查看完整详情</button>
        </div>
      </div>
    </el-drawer>

    <!-- 预约弹窗 -->
    <BookingDialog
      v-model="bookingVisible"
      :room="roomInfo"
      :room-id="props.roomId"
      :date="bookingDate"
      :start-time="bookingStartTime"
      :end-time="bookingEndTime"
      @success="onBookingSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Plus, Clock, OfficeBuilding, User, UserFilled, Document, WarningFilled, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSchedule, getMyReservationDetail, getReservationDetail, cancelReservation } from '@/api/reservation'
import { getRoomById } from '@/api/meeting'
import { useUserStore } from '@/stores/user'
import { formatDate as formatDateStr, formatTimeRange, toDate } from '@/utils/datetime'
import BookingDialog from '@/components/BookingDialog.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import type { MeetingRoom } from '@/types/meeting'

const props = defineProps<{
  roomId: string
}>()

const emit = defineEmits<{
  (e: 'book', startTime: string, endTime: string): void
}>()

const router = useRouter()
const userStore = useUserStore()
const isAdmin = userStore.isAdmin()

const viewMode = ref<'day' | 'week' | 'month'>('day')
const currentDate = ref(new Date())
const reservations = ref<any[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<any>(null)
const roomInfo = ref<MeetingRoom | null>(null)
// 预约时段紧凑展示：2026-07-29 09:00～10:30（同天）/ 跨天则完整日期
const detailTimeText = computed(() => {
  if (!currentDetail.value) return ''
  return formatTimeRange(currentDetail.value.startTime, currentDetail.value.endTime).full
})

// 预约弹窗相关
const bookingVisible = ref(false)
const bookingDate = ref('')
const bookingStartTime = ref('')
const bookingEndTime = ref('')

// ====== 时间配置 ======
const START_HOUR = 0
const END_HOUR = 24
const TOTAL_HOURS = END_HOUR - START_HOUR
const VISIBLE_HOURS = 9
const DEFAULT_HOUR_WIDTH = 60
const WEEK_HOUR_HEIGHT = 60
const DAY_EDGE_GAP_RATIO = 0.35
const WEEK_EDGE_GAP_RATIO = 0.35
const VIEW_START = 9
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
  const viewportWidth = dayBodyRef.value.clientWidth
  if (viewportWidth <= 0) return
  hourWidth.value = Number((viewportWidth / (VISIBLE_HOURS + DAY_EDGE_GAP_RATIO * 2)).toFixed(2))
}

function scrollToDefaultHour() {
  updateDayHourWidth()
  const scrollLeft = VIEW_START * hourWidth.value
  const scrollTop = weekEdgeGap + VIEW_START * WEEK_HOUR_HEIGHT
  headerScrollX.value = scrollLeft
  const tryScroll = (attempt: number) => {
    if (attempt > 10) return
    nextTick(() => {
      if (dayBodyRef.value) {
        setDayScrollLeft(scrollLeft)
      }
      if (weekBodyRef.value) {
        setWeekScrollTop(scrollTop)
      }
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
}

function onDayPointerMove(event: PointerEvent) {
  if (!dayDrag.active || dayDrag.pointerId !== event.pointerId || !dayBodyRef.value) return
  const deltaX = event.clientX - dayDrag.startX
  if (!dayDrag.moved && Math.abs(deltaX) > 4) {
    dayDrag.moved = true
    if (!dayBodyRef.value.hasPointerCapture(event.pointerId)) dayBodyRef.value.setPointerCapture(event.pointerId)
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
}

function onWeekPointerMove(event: PointerEvent) {
  if (!weekDrag.active || weekDrag.pointerId !== event.pointerId || !weekBodyRef.value) return
  const deltaY = event.clientY - weekDrag.startY
  if (!weekDrag.moved && Math.abs(deltaY) > 4) {
    weekDrag.moved = true
    if (!weekBodyRef.value.hasPointerCapture(event.pointerId)) weekBodyRef.value.setPointerCapture(event.pointerId)
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

const weekReservations = computed(() => reservations.value.filter(r => weekDays.value.some(d => d.dateStr === formatDateStr(r.startTime))))

const monthDays = ref<any[]>([])
const morePopoverDay = ref<string>('')

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
  const d = currentDate.value
  const p: Record<string, string> = {}
  if (viewMode.value === 'day') { p.date = formatDate(d) }
  else if (viewMode.value === 'week') { p.startDate = formatDate(weekDays.value[0].date); p.endDate = formatDate(weekDays.value[6].date) }
  else { const ms = new Date(d.getFullYear(), d.getMonth(), 1); ms.setDate(ms.getDate() - ms.getDay()); const me = new Date(ms); me.setDate(me.getDate() + 41); p.startDate = formatDate(ms); p.endDate = formatDate(me) }
  try {
    const r = await getSchedule(p)
    // 只保留当前会议室的预约；仅展示已确认(1)，待确认(0)尚未审批不占用时段
    reservations.value = (r.data.reservations || []).filter((res: any) => res.roomId === props.roomId && res.status === 1)
    if (viewMode.value === 'month') buildMonthDays()
  } catch { /* */ }
  await nextTick()
  scrollToDefaultHour()
}

// ====== 日视图 ======
const dayReservations = computed(() => {
  const today = formatDate(currentDate.value)
  return reservations.value.filter(r => formatDateStr(r.startTime) === today)
})

function dayEventStyle(r: any) {
  const start = toDate(r.startTime)
  const end = toDate(r.endTime)
  const startMinutes = (start.getHours() - START_HOUR) * 60 + start.getMinutes()
  const durationMinutes = (end.getTime() - start.getTime()) / 60000
  const leftPx = dayEdgeGap.value + (startMinutes / (TOTAL_HOURS * 60)) * gridWidth.value
  const widthPx = (durationMinutes / (TOTAL_HOURS * 60)) * gridWidth.value
  return { left: leftPx + 'px', width: widthPx + 'px' }
}
function onDayCellClick(h: number) {
  if (ignoreDayClick.value) return
  const dateStr = formatDate(currentDate.value)
  bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T${String(h).padStart(2, '0')}:00`
  bookingEndTime.value = `${dateStr}T${String(h + 1).padStart(2, '0')}:00`
  bookingVisible.value = true
}
function onDayEventClick(r: any) {
  if (ignoreDayClick.value) return
  showDetail(r)
}

// ====== 周视图 ======
function weekEventStyle(r: any) {
  const di = weekDays.value.findIndex(d => d.dateStr === formatDateStr(r.startTime))
  if (di < 0) return { display: 'none' }
  const start = toDate(r.startTime)
  const end = toDate(r.endTime)
  const startMinutes = (start.getHours() - START_HOUR) * 60 + start.getMinutes()
  const durationMinutes = (end.getTime() - start.getTime()) / 60000
  const topPx = weekEdgeGap + (startMinutes / (TOTAL_HOURS * 60)) * weekHoursHeight
  const heightPx = Math.max((durationMinutes / (TOTAL_HOURS * 60)) * weekHoursHeight, 16)
  return {
    left: (di / 7 * 100) + '%',
    width: (100 / 7) + '%',
    top: topPx + 'px',
    height: heightPx + 'px'
  }
}
function onWeekCellClick(dateStr: string, h: number) {
  if (ignoreWeekClick.value) return
  bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T${String(h).padStart(2, '0')}:00`
  bookingEndTime.value = `${dateStr}T${String(h + 1).padStart(2, '0')}:00`
  bookingVisible.value = true
}
function onWeekEventClick(r: any) {
  if (ignoreWeekClick.value) return
  showDetail(r)
}

// ====== 月视图 ======
function getDayReservations(day: any) { return day.reservations.length > 3 ? day.reservations.slice(0, 2) : day.reservations }
function onMorePopoverToggle(ds: string, v: boolean) { morePopoverDay.value = v ? ds : '' }
function onPopoverEventClick(r: any) { morePopoverDay.value = ''; showDetail(r) }
function buildMonthDays() {
  const d = currentDate.value, m = d.getMonth(), today = formatDate(new Date())
  const ms = new Date(d.getFullYear(), m, 1); ms.setDate(ms.getDate() - ms.getDay())
  monthDays.value = Array.from({ length: 42 }, (_, i) => { const dt = new Date(ms); dt.setDate(dt.getDate() + i); const ds = formatDate(dt); return { date: dt.getDate(), dateStr: ds, currentMonth: dt.getMonth() === m, isToday: ds === today, reservations: reservations.value.filter(r => formatDateStr(r.startTime) === ds) } })
}
function onMonthCellClick(day: any) {
  bookingDate.value = day.dateStr
  bookingStartTime.value = `${day.dateStr}T09:00:00`
  bookingEndTime.value = `${day.dateStr}T10:00:00`
  bookingVisible.value = true
}

// ====== 导航 ======
function goPrev() { const d = new Date(currentDate.value); if (viewMode.value === 'day') d.setDate(d.getDate() - 1); else if (viewMode.value === 'week') d.setDate(d.getDate() - 7); else d.setMonth(d.getMonth() - 1); currentDate.value = d }
function goNext() { const d = new Date(currentDate.value); if (viewMode.value === 'day') d.setDate(d.getDate() + 1); else if (viewMode.value === 'week') d.setDate(d.getDate() + 7); else d.setMonth(d.getMonth() + 1); currentDate.value = d }
function goToday() { currentDate.value = new Date() }
function openQuickBook() {
  const dateStr = formatDate(currentDate.value)
  bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T09:00:00`
  bookingEndTime.value = `${dateStr}T10:00:00`
  bookingVisible.value = true
}
function onBookingSuccess() { loadData() }
function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已拒绝' }[s] || '未知' }
function attendeeStatusText(s: number) { return { 0: '待查阅', 1: '已查阅', 2: '已拒绝' }[s] || '未知' }
function attendeeStatusType(s: number) { return ({ 0: 'info', 1: 'success', 2: 'danger' } as const)[s] || 'info' }
// 点击事件块：拉取完整详情（管理员可查全部；普通用户仅本人可查，失败则回退展示日程数据）
async function showDetail(r: any) {
  detailVisible.value = true
  detailLoading.value = true
  currentDetail.value = r
  try {
    const api = isAdmin ? getReservationDetail : getMyReservationDetail
    const res = await api(r.id)
    currentDetail.value = res.data
  } catch {
    currentDetail.value = r
  } finally {
    detailLoading.value = false
  }
}
const canCancel = computed(() => {
  if (!currentDetail.value) return false
  if (currentDetail.value.status !== 1) return false
  return new Date(currentDetail.value.startTime).getTime() > Date.now()
})
async function handleCancel() {
  if (!currentDetail.value) return
  try {
    await ElMessageBox.confirm('确定取消该预约？', '提示', { type: 'warning' })
    await cancelReservation(currentDetail.value.id)
    ElMessage.success('取消成功')
    detailVisible.value = false
    loadData()
  } catch { /* */ }
}
function handleViewFull() {
  if (!currentDetail.value) return
  const id = currentDetail.value.id
  if (isAdmin) router.push(`/admin/reservations/${id}`)
  else router.push(`/reservation/my/${id}`)
}

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
  // 加载会议室信息
  try {
    const res = await getRoomById(props.roomId)
    roomInfo.value = res.data
  } catch { /* */ }
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

defineExpose({ loadData })
</script>

<style scoped>
.room-schedule { display: flex; flex-direction: column; gap: 0; }

.schedule-controls { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.control-left { display: flex; align-items: center; gap: 8px; }
.control-right { display: flex; align-items: center; gap: 8px; }
.ctrl-btn { display: inline-flex; align-items: center; justify-content: center; gap: 4px; height: 32px; padding: 0 14px; border: 1px solid #e5e7eb; background: #fff; color: #303133; border-radius: 8px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
.ctrl-btn:hover { border-color: #409eff; color: #409eff; }
.ctrl-btn.primary { background: #409eff; color: #fff; border-color: #409eff; }
.ctrl-btn.primary:hover { opacity: 0.9; }
.ctrl-icon { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; border: 1px solid #e5e7eb; background: #fff; color: #6b7280; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
.ctrl-icon:hover { border-color: #409eff; color: #409eff; }
.view-segment { display: inline-flex; align-items: center; height: 32px; margin: 0 4px; padding: 2px; background: #f3f4f6; border-radius: 8px; }
.seg-item { height: 28px; min-width: 40px; padding: 0 14px; border: none; background: transparent; color: #6b7280; font-size: 13px; border-radius: 6px; cursor: pointer; transition: all 0.2s; }
.seg-item:hover { color: #409eff; }
.seg-item.active { background: #fff; color: #409eff; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.date-display { font-size: 14px; color: #303133; font-weight: 500; margin-left: 8px; }

/* ========== 日视图 ========== */
.day-view {
  --hour-width: 60px;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}
.day-header { display: flex; border-bottom: 2px solid #e5e7eb; background: #fafbfc; flex-shrink: 0; }
.time-col-header {
  width: 60px;
  box-sizing: border-box;
  padding: 10px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  flex-shrink: 0;
  border-right: 1px solid #e5e7eb;
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
  max-height: 500px;
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
.day-view.dragging,
.day-view.dragging * {
  cursor: grabbing !important;
  user-select: none;
}
.day-grid-wrap {
  position: relative;
  flex-shrink: 0;
  box-sizing: border-box;
  padding: 0 var(--day-edge-gap);
  overflow: hidden;
}
.day-grid { display: flex; flex-shrink: 0; height: 64px; }
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
.week-view { padding: 0; overflow: hidden; display: flex; flex-direction: column; border: 1px solid #e5e7eb; border-radius: 8px; background: #fff; margin-top: 0; }
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
  max-height: 500px;
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

/* ========== 通用预约块（飞书实色渐变） ========== */
.evt-block { color: #fff; border: none; box-shadow: 0 1px 3px rgba(0,0,0,0.12); }
.evt-block .evt-title { color: #fff; }
.evt-block .evt-time { color: rgba(255,255,255,0.92); }
.evt-block .evt-user { color: rgba(255,255,255,0.8); }
.evt-block:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(0,0,0,0.2); filter: brightness(1.05); }
.evt-block.s0 { background: linear-gradient(135deg, #f59e0b, #d97706); }
.evt-block.s1 { background: linear-gradient(135deg, #10b981, #059669); }
.evt-block.s2 { background: linear-gradient(135deg, #9ca3af, #6b7280); }
.evt-block.s3 { background: linear-gradient(135deg, #ef4444, #dc2626); }
.evt-inner { height: 100%; display: flex; flex-direction: column; justify-content: center; overflow: hidden; }
.evt-title { font-weight: 500; color: #374151; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 12px; }
.evt-time { font-size: 10px; color: #6b7280; margin-top: 1px; white-space: nowrap; }
.evt-user { font-size: 10px; color: #9ca3af; margin-top: 1px; white-space: nowrap; }

/* ========== 月视图 ========== */
.month-view { padding: 0; border: 1px solid #e5e7eb; border-radius: 8px; background: #fff; }
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
.mc-event { display: flex; gap: 4px; align-items: center; font-size: 11px; padding: 2px 6px; border-radius: 4px; cursor: pointer; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mc-event-time { font-weight: 600; opacity: 0.9; flex-shrink: 0; }
.mc-event-title { overflow: hidden; text-overflow: ellipsis; }
.mc-more { font-size: 11px; color: #409eff; padding: 2px 8px; cursor: pointer; border: none; background: transparent; border-radius: 4px; transition: background 0.15s; width: 100%; text-align: left; }
.mc-more:hover { background: rgba(64,158,255,0.08); }
.mc-pop-list { display: flex; flex-direction: column; gap: 4px; max-height: 320px; overflow-y: auto; }
.mc-pop-list::-webkit-scrollbar { width: 6px; }
.mc-pop-list::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }
.mc-pop-head { font-size: 12px; font-weight: 600; color: #6b7280; padding-bottom: 6px; border-bottom: 1px solid #f0f0f0; margin-bottom: 4px; }
.mc-pop-item { display: flex; gap: 8px; align-items: center; padding: 6px 8px; border-radius: 6px; cursor: pointer; font-size: 12px; transition: all 0.15s; }
.mc-pop-time { font-weight: 600; flex-shrink: 0; min-width: 40px; }
.mc-pop-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ========== 预约详情抽屉（与日历视图一致） ========== */
.rs-drawer { display: flex; flex-direction: column; height: 100%; }
.drawer-head { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e5e7eb; flex-shrink: 0; }
.drawer-title { font-size: 14px; font-weight: 600; color: #303133; }
.drawer-close { border: none; background: transparent; cursor: pointer; color: #9ca3af; padding: 4px; border-radius: 4px; display: inline-flex; align-items: center; }
.drawer-close:hover { background: #f3f4f6; color: #303133; }
.drawer-body { flex: 1; overflow-y: auto; padding: 16px 20px; }
.detail-status-bar { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: 999px; font-size: 12px; margin-bottom: 12px; }
.detail-status-bar .status-dot { width: 6px; height: 6px; border-radius: 50%; }
.detail-status-bar.s0 { background: rgba(245,158,11,0.15); color: #92400e; }
.detail-status-bar.s0 .status-dot { background: #f59e0b; }
.detail-status-bar.s1 { background: rgba(16,185,129,0.15); color: #065f46; }
.detail-status-bar.s1 .status-dot { background: #10b981; }
.detail-status-bar.s2 { background: rgba(156,163,175,0.15); color: #6b7280; }
.detail-status-bar.s2 .status-dot { background: #9ca3af; }
.detail-status-bar.s3 { background: rgba(239,68,68,0.15); color: #991b1b; }
.detail-status-bar.s3 .status-dot { background: #ef4444; }
.detail-subject { font-size: 18px; font-weight: 600; color: #303133; margin: 0 0 16px; }
.detail-row { display: flex; gap: 12px; padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.detail-row .el-icon { color: #9ca3af; margin-top: 2px; }
.detail-row-val { font-size: 13px; color: #303133; font-weight: 500; }
.detail-row-val.danger { color: #ef4444; }
.detail-row-sub { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.detail-attendees { margin-top: 16px; }
.attendees-title { font-size: 12px; font-weight: 600; color: #6b7280; margin-bottom: 8px; }
.attendee-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.attendee-avatar { width: 32px; height: 32px; flex-shrink: 0; }
.attendee-info { flex: 1; min-width: 0; }
.attendee-name { font-size: 13px; color: #303133; font-weight: 500; }
.attendee-dept { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.drawer-foot { display: flex; gap: 8px; padding: 12px 20px; border-top: 1px solid #e5e7eb; flex-shrink: 0; }
.drawer-btn { flex: 1; padding: 8px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; transition: all 0.2s; border: 1px solid #e5e7eb; background: #fff; color: #303133; }
.drawer-btn:hover { border-color: #409eff; color: #409eff; }
.drawer-btn.primary { background: #409eff; color: #fff; border-color: #409eff; }
.drawer-btn.primary:hover { opacity: 0.9; }
.drawer-btn.danger { color: #ef4444; border-color: #fecaca; }
.drawer-btn.danger:hover { background: #ef4444; color: #fff; border-color: #ef4444; }
</style>
