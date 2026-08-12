<template>
  <div class="v2-page">
    <!-- ============ 左侧任务栏 ============ -->
    <aside class="v2-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
        <el-icon><component :is="sidebarCollapsed ? 'Expand' : 'Fold'" /></el-icon>
      </button>
      <template v-if="!sidebarCollapsed">
        <!-- 迷你月历 -->
        <div class="mini-cal">
          <div class="mini-cal-head">
            <button class="mini-nav" @click="miniPrev"><el-icon><ArrowLeft /></el-icon></button>
            <span class="mini-title">{{ miniYear }}年{{ miniMonth + 1 }}月</span>
            <button class="mini-nav" @click="miniNext"><el-icon><ArrowRight /></el-icon></button>
          </div>
          <div class="mini-week">
            <span v-for="w in ['日','一','二','三','四','五','六']" :key="w">{{ w }}</span>
          </div>
          <div class="mini-grid">
            <button
              v-for="(d, i) in miniDays"
              :key="i"
              class="mini-cell"
              :class="{ other: !d.currentMonth, today: d.isToday, selected: d.dateStr === selectedMiniDate }"
              @click="onMiniPick(d.dateStr)"
            >
              <span class="mini-day">{{ d.date }}</span>
              <span v-if="d.hasEvent" class="mini-dot"></span>
            </button>
          </div>
        </div>

        <!-- Tab 切换 -->
        <div class="tab-segment">
          <button class="seg-btn" :class="{ active: tab === 'my' }" @click="switchTab('my')">我的日历</button>
          <button v-if="isAdmin" class="seg-btn" :class="{ active: tab === 'room' }" @click="switchTab('room')">会议室日历</button>
        </div>

        <!-- 工作时段开关 -->
        <div class="filter-row">
          <el-switch v-model="workHoursOnly" size="small" />
          <span class="filter-label">仅工作时段</span>
          <span class="filter-time">09-18</span>
        </div>

        <!-- 今日卡片 -->
        <div class="today-card">
          <div class="today-card-head">
            <span class="today-label">今日</span>
            <span class="today-date">{{ todayLabel }}</span>
          </div>
          <div class="today-card-body">
            <template v-if="todayNext">
              <div class="today-next">下一场</div>
              <div class="today-next-title">{{ todayNext.subject || '未命名' }}</div>
              <div class="today-next-time">{{ formatTime(todayNext.startTime) }} - {{ formatTime(todayNext.endTime) }}</div>
            </template>
            <div v-else-if="todayCount > 0" class="today-empty">今日 {{ todayEndedCount }} 场已结束</div>
            <div v-else class="today-empty">今日暂无会议</div>
          </div>
          <div class="today-card-foot">共 {{ todayCount }} 场会议</div>
        </div>
      </template>
    </aside>

    <!-- ============ 主视图 ============ -->
    <main class="v2-main">
      <!-- 控制栏 -->
      <div class="v2-control-bar">
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
            <el-icon><Plus /></el-icon><span>预约</span>
          </button>
        </div>
      </div>

      <!-- ====== 日视图 ====== -->
      <div v-if="viewMode === 'day'" class="day-view v2-card"
        :class="{ dragging: dayDrag.active }"
        :style="{ '--hour-width': `${hourWidth}px`, '--day-edge-gap': `${dayEdgeGap}px` }">
        <!-- 我的日历：纵向议程视图 -->
        <template v-if="tab === 'my'">
          <div class="my-agenda">
            <div class="my-agenda-head">
              <span class="my-agenda-date">{{ formatDayLabel(currentDate) }}</span>
              <span class="my-agenda-weekday">{{ weekdayText(currentDate) }}</span>
              <span v-if="isCurrentDay(currentDate)" class="my-agenda-today-badge">今天</span>
              <span class="my-agenda-count">{{ myDayReservations.length }} 场会议</span>
            </div>
            <div ref="myDayBodyRef" class="my-agenda-body">
              <div class="my-agenda-scroll" @click="onMyDayAreaClick">
                <!-- 时间刻度槽 -->
                <div class="my-agenda-grid">
                  <div v-for="h in allHours" :key="h" class="my-agenda-hour">
                    <span class="my-agenda-hour-label">{{ String(h).padStart(2, '0') }}:00</span>
                  </div>
                </div>
                <!-- 事件层 -->
                <div class="my-agenda-events" ref="myDayEventsRef">
                  <!-- 工作时段底色 -->
                  <div v-if="workHoursOnly" class="my-agenda-work-band"
                    :style="{ top: `${WORK_START_HOUR * MY_HOUR_HEIGHT}px`, height: `${(WORK_END_HOUR - WORK_START_HOUR) * MY_HOUR_HEIGHT}px` }"></div>
                  <!-- 整点分割线 -->
                  <div v-for="h in allHours" :key="'l'+h" class="my-agenda-line"
                    :style="{ top: `${h * MY_HOUR_HEIGHT}px` }"></div>
                  <!-- 当前时间线 -->
                  <div v-if="showMyNowLine" class="my-agenda-now" :style="{ top: `${myNowLineTop}px` }">
                    <span class="my-now-dot"></span>
                    <span class="my-now-label">{{ currentTimeLabel }}</span>
                  </div>
                  <!-- 事件块 -->
                  <div v-for="r in myDayReservations" :key="r.id"
                    class="my-agenda-event evt-block" :class="'s' + r.status"
                    :style="myAgendaEventStyle(r)" @click.stop="onEventClick(r)">
                    <div class="evt-bar"></div>
                    <div class="evt-body">
                      <div class="evt-title">{{ r.subject || '未命名' }}</div>
                      <div class="evt-time">{{ formatTime(r.startTime) }} - {{ formatTime(r.endTime) }}</div>
                      <div v-if="r.roomName" class="evt-meta">{{ r.roomName }}</div>
                    </div>
                  </div>
                  <!-- 空状态 -->
                  <div v-if="!myDayReservations.length" class="my-agenda-empty">
                    <el-icon><Calendar /></el-icon>
                    <span>当日暂无会议，点击空白时段快速预约</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
        <!-- 会议室日历：横向多行 -->
        <template v-else>
          <div class="day-header">
            <div class="room-col-header">会议室</div>
            <div class="day-ticks-wrap">
              <div class="day-ticks" :style="{ width: timelineWidth + 'px', transform: `translateX(${-headerScrollX}px)` }">
                <div v-for="h in allHours" :key="h" class="tick">
                  <span class="tick-label">{{ String(h).padStart(2, '0') }}:00</span>
                </div>
                <div class="tick tick-end"><span class="tick-label">24:00</span></div>
              </div>
            </div>
          </div>
          <div ref="dayBodyRef" class="day-body-wrap" @scroll="onDayScroll"
            @pointerdown="onDayPointerDown" @pointermove="onDayPointerMove"
            @pointerup="onDayPointerUp" @pointercancel="onDayPointerUp">
            <div class="day-body">
              <div class="day-work-hours-band"
                :style="{ left: `${ROOM_COLUMN_WIDTH + dayWorkHoursLeft}px`, width: `${dayWorkHoursWidth}px` }"></div>
              <div v-if="showDayNowLine" class="day-now-column"
                :style="{ left: `${ROOM_COLUMN_WIDTH + dayNowLineLeft}px` }">
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
                    :content="`${r.subject || '未命名'}\n${formatTime(r.startTime)}-${formatTime(r.endTime)}\n${r.userName || ''}`"
                    placement="top" raw-content>
                    <div class="day-event evt-block" :class="'s' + r.status"
                      :style="dayEventStyle(r)" @click.stop="onEventClick(r)">
                      <div class="evt-inner">
                        <div class="evt-title">{{ r.subject || '未命名' }}</div>
                        <div class="evt-time">{{ formatTime(r.startTime) }}-{{ formatTime(r.endTime) }}</div>
                      </div>
                    </div>
                  </el-tooltip>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- ====== 周视图 ====== -->
      <div v-if="viewMode === 'week'" class="week-view v2-card"
        :class="{ dragging: weekDrag.active }"
        :style="{ '--week-edge-gap': `${weekEdgeGap}px` }">
        <div class="week-header">
          <div class="wk-corner"></div>
          <div v-for="d in weekDays" :key="d.dateStr" class="wk-day" :class="{ today: d.isToday }">
            <div class="wk-day-name">{{ d.dayName }}</div>
            <div class="wk-day-num">{{ d.dayNum }}</div>
          </div>
        </div>
        <div ref="weekBodyRef" class="week-body-wrap" @scroll="onWeekScroll"
          @pointerdown="onWeekPointerDown" @pointermove="onWeekPointerMove"
          @pointerup="onWeekPointerUp" @pointercancel="onWeekPointerUp">
          <div class="week-body">
            <div class="wk-times" :style="{ height: weekTrackHeight + 'px' }">
              <div v-for="h in allHours" :key="h" class="wk-time" :class="{ 'wk-time-now': isCurrentHour(h) }">
                <span class="wk-time-label" :class="{ 'wk-time-now-label': isCurrentHour(h) }">{{ String(h).padStart(2, '0') }}:00</span>
              </div>
            </div>
            <div class="wk-grid-wrap">
              <div class="wk-grid" :style="{ height: weekTrackHeight + 'px' }">
                <div class="wk-work-hours-band" v-if="workHoursOnly"
                  :style="{ top: `${weekWorkHoursTop}px`, height: `${weekWorkHoursHeight}px` }"></div>
                <div v-for="d in weekDays" :key="d.dateStr" class="wk-col">
                  <div v-for="h in allHours" :key="h" class="wk-cell" @click="onWeekCellClick(d.dateStr, h)"></div>
                </div>
                <div v-if="showWeekNowLine" class="wk-now-line"
                  :style="{ top: `${weekNowLineTop}px`, left: weekNowLineLeft, width: weekNowLineWidth }">
                  <span class="wk-now-label">{{ currentTimeLabel }}</span>
                </div>
                <el-tooltip v-for="r in weekReservations" :key="r.id"
                  :content="`${r.roomName || ''} | ${r.subject || '未命名'}\n${formatTime(r.startTime)}-${formatTime(r.endTime)}\n${r.userName || ''}`"
                  placement="top" raw-content>
                  <div class="week-event evt-block" :class="'s' + r.status"
                    :style="weekEventStyle(r)" @click.stop="onEventClick(r)">
                    <div class="evt-inner">
                      <div class="evt-title">{{ r.subject || '未命名' }}</div>
                      <div class="evt-time">{{ formatTime(r.startTime) }}-{{ formatTime(r.endTime) }}</div>
                      <div class="evt-room" v-if="(weekEventLayoutMap.get(r.id)?.totalColumns ?? 1) > 1">{{ r.roomName || '' }}</div>
                    </div>
                  </div>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ====== 月视图 ====== -->
      <div v-if="viewMode === 'month'" class="month-view v2-card">
        <div class="month-header">
          <div v-for="d in ['日','一','二','三','四','五','六']" :key="d" class="mh-cell">{{ d }}</div>
        </div>
        <div class="month-grid">
          <div v-for="(day, idx) in monthDays" :key="idx"
            class="month-cell" :class="{ 'other-month': !day.currentMonth, today: day.isToday }"
            @click="onMonthCellClick(day)">
            <div class="mc-date">{{ day.date }}</div>
            <div class="mc-events">
              <div v-for="r in getDayReservations(day)" :key="r.id"
                class="mc-event evt-block" :class="'s' + r.status"
                @click.stop="onEventClick(r)">
                <span class="mc-event-time">{{ formatTime(r.startTime) }}</span>
                <span class="mc-event-title">{{ r.subject || '未命名' }}</span>
              </div>
              <el-popover v-if="day.reservations.length > 3"
                :visible="morePopoverDay === day.dateStr"
                trigger="click" placement="top" :width="260"
                popper-class="mc-more-popover"
                @update:visible="(v: boolean) => onMorePopoverToggle(day.dateStr, v)">
                <template #reference>
                  <button class="mc-more" @click.stop="onMorePopoverToggle(day.dateStr, morePopoverDay !== day.dateStr)">+{{ day.reservations.length - 3 }} 更多</button>
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
    </main>

    <!-- ============ 右侧详情抽屉 ============ -->
    <transition name="drawer-slide">
      <aside v-if="detailVisible" class="v2-drawer">
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
                <div class="detail-row-val">{{ currentDetail.roomName || '-' }}</div>
                <div class="detail-row-sub">会议室</div>
              </div>
            </div>
            <div class="detail-row">
              <el-icon><User /></el-icon>
              <div>
                <div class="detail-row-val">{{ currentDetail.username || '-' }}</div>
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
      </aside>
    </transition>

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
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Plus, Close, Clock, OfficeBuilding, User, UserFilled, Document, WarningFilled, Calendar } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSchedule, listMyReservations, getMyReservationDetail, getReservationDetail, cancelReservation } from '@/api/reservation'
import { useUserStore } from '@/stores/user'
import BookingDialog from '@/components/BookingDialog.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { formatTimeRange } from '@/utils/datetime'
import type { Reservation } from '@/types/reservation'

const router = useRouter()
const userStore = useUserStore()
const isAdmin = userStore.isAdmin()

// ====== 视图状态 ======
const viewMode = ref<'day' | 'week' | 'month'>('day')
const currentDate = ref(new Date())
const tab = ref<'my' | 'room'>(isAdmin ? 'my' : 'my')
const workHoursOnly = ref(false)
const sidebarCollapsed = ref(false)
const nowTimestamp = ref(Date.now())

// ====== 数据 ======
const rooms = ref<any[]>([])
const roomReservations = ref<any[]>([]) // 会议室日历 Tab 的预约
const myReservations = ref<Reservation[]>([]) // 我的日历 Tab 的预约
const todayReservations = ref<Reservation[]>([]) // 今日数据（独立加载，不受视图范围影响）

// ====== 详情抽屉 ======
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<Reservation | null>(null)
// 预约时段紧凑展示：2026-07-29 09:00～10:30（同天）/ 跨天则完整日期
const detailTimeText = computed(() => {
  if (!currentDetail.value) return ''
  return formatTimeRange(currentDetail.value.startTime, currentDetail.value.endTime).full
})

// ====== 预约弹窗 ======
const bookingVisible = ref(false)
const bookingRoomId = ref<string | undefined>(undefined)
const bookingDate = ref('')
const bookingStartTime = ref('')
const bookingEndTime = ref('')

// ====== 迷你月历 ======
const miniYear = ref(new Date().getFullYear())
const miniMonth = ref(new Date().getMonth())
const selectedMiniDate = ref(formatDate(new Date()))

// ====== 时间常量 ======
const START_HOUR = 0
const END_HOUR = 24
const TOTAL_HOURS = END_HOUR - START_HOUR
const ROOM_COLUMN_WIDTH = 120
const VISIBLE_HOURS = 9
const DEFAULT_HOUR_WIDTH = 60
const WEEK_HOUR_HEIGHT = 60
const MY_HOUR_HEIGHT = 56
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

const allHours = computed(() => { const h = []; for (let i = START_HOUR; i < END_HOUR; i++) h.push(i); return h })

// ====== 拖拽 ======
const dayBodyRef = ref<HTMLElement>()
const weekBodyRef = ref<HTMLElement>()
const myDayBodyRef = ref<HTMLElement>()
const myDayEventsRef = ref<HTMLElement>()
const headerScrollX = ref(0)
let dayBodyResizeObserver: ResizeObserver | null = null
let dayInertiaFrame = 0
let weekInertiaFrame = 0
let nowTimer: number | undefined

const dayDrag = reactive({ active: false, pointerId: -1, startX: 0, startScrollLeft: 0, lastX: 0, lastTime: 0, velocity: 0, moved: false })
const weekDrag = reactive({ active: false, pointerId: -1, startY: 0, startScrollTop: 0, lastY: 0, lastTime: 0, velocity: 0, moved: false })

// ====== 时间线 ======
const todayString = computed(() => formatDate(new Date(nowTimestamp.value)))
const currentMinutes = computed(() => { const n = new Date(nowTimestamp.value); return n.getHours() * 60 + n.getMinutes() })
const currentTimeLabel = computed(() => { const n = new Date(nowTimestamp.value); return `${String(n.getHours()).padStart(2, '0')}:${String(n.getMinutes()).padStart(2, '0')}` })
const showDayNowLine = computed(() => formatDate(currentDate.value) === todayString.value)
const dayNowLineLeft = computed(() => dayEdgeGap.value + (currentMinutes.value / (TOTAL_HOURS * 60)) * gridWidth.value)
const showMyNowLine = computed(() => formatDate(currentDate.value) === todayString.value)
const myNowLineTop = computed(() => (currentMinutes.value / 60) * MY_HOUR_HEIGHT)
const showWeekNowLine = computed(() => weekDays.value.some(d => d.dateStr === todayString.value))
const weekNowLineTop = computed(() => weekEdgeGap + (currentMinutes.value / (TOTAL_HOURS * 60)) * weekHoursHeight)
const weekNowLineTodayIndex = computed(() => weekDays.value.findIndex(d => d.dateStr === todayString.value))
const weekNowLineLeft = computed(() => (weekNowLineTodayIndex.value / 7) * 100 + '%')
const weekNowLineWidth = computed(() => (100 / 7) + '%')
const dayWorkHoursLeft = computed(() => dayEdgeGap.value + (WORK_START_HOUR - START_HOUR) * hourWidth.value)
const dayWorkHoursWidth = computed(() => (WORK_END_HOUR - WORK_START_HOUR) * hourWidth.value)
const weekWorkHoursTop = computed(() => weekEdgeGap + (WORK_START_HOUR - START_HOUR) * WEEK_HOUR_HEIGHT)
const weekWorkHoursHeight = computed(() => (WORK_END_HOUR - WORK_START_HOUR) * WEEK_HOUR_HEIGHT)

// ====== 计算属性 ======
const todayLabel = computed(() => { const n = new Date(); return `${n.getMonth() + 1}月${n.getDate()}日` })
const dateDisplay = computed(() => {
  const d = currentDate.value
  const f = (dt: Date) => `${dt.getFullYear()}年${String(dt.getMonth() + 1).padStart(2, '0')}月${String(dt.getDate()).padStart(2, '0')}日`
  if (viewMode.value === 'day') return f(d)
  if (viewMode.value === 'week') { const e = new Date(d); e.setDate(e.getDate() + 6); return `${f(d)} ~ ${f(e)}` }
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})

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

// 当前视图下的所有预约（合并 my/room Tab）
const allReservations = computed(() => tab.value === 'my' ? myReservations.value : roomReservations.value)

const weekReservations = computed(() => allReservations.value.filter(r => weekDays.value.some(d => d.dateStr === r.startTime.split('T')[0])))

// 我的日历 - 日视图
const myDayReservations = computed(() => {
  const today = formatDate(currentDate.value)
  return myReservations.value.filter(r => r.startTime.split('T')[0] === today)
})

// 今日数据（独立加载，用于今日卡片，不受当前视图范围影响）
const todayCount = computed(() => todayReservations.value.length)
const todayNext = computed(() => {
  const now = nowTimestamp.value
  return todayReservations.value
    .filter(r => new Date(r.endTime).getTime() > now)
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())[0]
})
const todayEndedCount = computed(() => {
  const now = nowTimestamp.value
  return todayReservations.value.filter(r => new Date(r.endTime).getTime() <= now).length
})

// 月视图
const monthDays = ref<any[]>([])
const morePopoverDay = ref<string>('')

// 周视图重叠布局
const weekEventLayoutMap = computed(() => {
  const layout = new Map<string, { columnIndex: number; totalColumns: number }>()
  const eventsByDay = new Map<string, any[]>()
  for (const r of weekReservations.value) {
    const dayStr = r.startTime.split('T')[0]
    if (!eventsByDay.has(dayStr)) eventsByDay.set(dayStr, [])
    eventsByDay.get(dayStr)!.push(r)
  }
  for (const [, dayEvents] of eventsByDay) {
    if (dayEvents.length === 0) continue
    dayEvents.sort((a, b) => {
      const aStart = new Date(a.startTime).getTime()
      const bStart = new Date(b.startTime).getTime()
      if (aStart !== bStart) return aStart - bStart
      const aDur = new Date(a.endTime).getTime() - aStart
      const bDur = new Date(b.endTime).getTime() - bStart
      return bDur - aDur
    })
    const eventColumns: number[] = []
    const columnEnds: number[] = []
    for (const event of dayEvents) {
      const eStart = new Date(event.startTime).getTime()
      let placed = false
      for (let ci = 0; ci < columnEnds.length; ci++) {
        if (eStart >= columnEnds[ci]) { columnEnds[ci] = new Date(event.endTime).getTime(); eventColumns.push(ci); placed = true; break }
      }
      if (!placed) { eventColumns.push(columnEnds.length); columnEnds.push(new Date(event.endTime).getTime()) }
    }
    const n = dayEvents.length
    const parent = Array.from({ length: n }, (_, i) => i)
    function find(i: number): number { while (parent[i] !== i) { parent[i] = parent[parent[i]]; i = parent[i] } return i }
    function union(i: number, j: number) { parent[find(i)] = find(j) }
    for (let i = 0; i < n; i++) {
      for (let j = i + 1; j < n; j++) {
        const iEnd = new Date(dayEvents[i].endTime).getTime()
        const jStart = new Date(dayEvents[j].startTime).getTime()
        if (jStart < iEnd) { union(i, j) } else { break }
      }
    }
    const groupMaxCol = new Map<number, number>()
    for (let i = 0; i < n; i++) { const root = find(i); groupMaxCol.set(root, Math.max(groupMaxCol.get(root) ?? 0, eventColumns[i] + 1)) }
    for (let i = 0; i < n; i++) { layout.set(dayEvents[i].id, { columnIndex: eventColumns[i], totalColumns: groupMaxCol.get(find(i))! }) }
  }
  return layout
})

// 我的日历 - 日视图重叠布局（并排分栏，复用周视图并查集算法）
const myDayLayoutMap = computed(() => {
  const layout = new Map<string, { columnIndex: number; totalColumns: number }>()
  const events = [...myDayReservations.value]
  if (events.length === 0) return layout
  events.sort((a, b) => {
    const aStart = new Date(a.startTime).getTime()
    const bStart = new Date(b.startTime).getTime()
    if (aStart !== bStart) return aStart - bStart
    const aDur = new Date(a.endTime).getTime() - aStart
    const bDur = new Date(b.endTime).getTime() - bStart
    return bDur - aDur
  })
  const eventColumns: number[] = []
  const columnEnds: number[] = []
  for (const event of events) {
    const eStart = new Date(event.startTime).getTime()
    let placed = false
    for (let ci = 0; ci < columnEnds.length; ci++) {
      if (eStart >= columnEnds[ci]) { columnEnds[ci] = new Date(event.endTime).getTime(); eventColumns.push(ci); placed = true; break }
    }
    if (!placed) { eventColumns.push(columnEnds.length); columnEnds.push(new Date(event.endTime).getTime()) }
  }
  const n = events.length
  const parent = Array.from({ length: n }, (_, i) => i)
  function find(i: number): number { while (parent[i] !== i) { parent[i] = parent[parent[i]]; i = parent[i] } return i }
  function union(i: number, j: number) { parent[find(i)] = find(j) }
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      const iEnd = new Date(events[i].endTime).getTime()
      const jStart = new Date(events[j].startTime).getTime()
      if (jStart < iEnd) { union(i, j) } else { break }
    }
  }
  const groupMaxCol = new Map<number, number>()
  for (let i = 0; i < n; i++) { const root = find(i); groupMaxCol.set(root, Math.max(groupMaxCol.get(root) ?? 0, eventColumns[i] + 1)) }
  for (let i = 0; i < n; i++) { layout.set(events[i].id, { columnIndex: eventColumns[i], totalColumns: groupMaxCol.get(find(i))! }) }
  return layout
})

// ====== 工具函数 ======
function formatDate(dt: Date) { return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}` }
function formatTime(t: string) { return t ? t.substring(11, 16) : '' }
function formatDayLabel(dt: Date) { return `${dt.getMonth() + 1}/${dt.getDate()}` }
function isCurrentHour(h: number) { return new Date().getHours() === h }
function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已拒绝' }[s] || '未知' }
function attendeeStatusText(s: number) { return { 0: '待查阅', 1: '已查阅', 2: '已拒绝' }[s] || '未知' }
function attendeeStatusType(s: number) { return ({ 0: 'info', 1: 'success', 2: 'danger' } as const)[s] || 'info' }

// ====== 滚动 ======
function onDayScroll() { if (dayBodyRef.value) headerScrollX.value = dayBodyRef.value.scrollLeft }
function onWeekScroll() { /* 周视图时间刻度固定，无需同步 */ }
function setDayScrollLeft(s: number) { if (dayBodyRef.value) { dayBodyRef.value.scrollLeft = s; headerScrollX.value = s } }
function setWeekScrollTop(s: number) { if (weekBodyRef.value) weekBodyRef.value.scrollTop = s }
function cancelDayInertia() { if (dayInertiaFrame) { cancelAnimationFrame(dayInertiaFrame); dayInertiaFrame = 0 } }
function cancelWeekInertia() { if (weekInertiaFrame) { cancelAnimationFrame(weekInertiaFrame); weekInertiaFrame = 0 } }
function startDayInertia(v: number) {
  if (!dayBodyRef.value || Math.abs(v) < 0.02) return
  cancelDayInertia()
  let velocity = v, lastTime = performance.now()
  const step = (now: number) => {
    if (!dayBodyRef.value) return
    const dt = now - lastTime; lastTime = now
    setDayScrollLeft(dayBodyRef.value.scrollLeft + velocity * dt)
    velocity *= Math.pow(0.95, dt / 16)
    const atEdge = dayBodyRef.value.scrollLeft <= 0 || dayBodyRef.value.scrollLeft >= dayBodyRef.value.scrollWidth - dayBodyRef.value.clientWidth
    if (Math.abs(velocity) < 0.02 || atEdge) { dayInertiaFrame = 0; return }
    dayInertiaFrame = requestAnimationFrame(step)
  }
  dayInertiaFrame = requestAnimationFrame(step)
}
function startWeekInertia(v: number) {
  if (!weekBodyRef.value || Math.abs(v) < 0.02) return
  cancelWeekInertia()
  let velocity = v, lastTime = performance.now()
  const step = (now: number) => {
    if (!weekBodyRef.value) return
    const dt = now - lastTime; lastTime = now
    weekBodyRef.value.scrollTop += velocity * dt
    velocity *= Math.pow(0.95, dt / 16)
    const atEdge = weekBodyRef.value.scrollTop <= 0 || weekBodyRef.value.scrollTop >= weekBodyRef.value.scrollHeight - weekBodyRef.value.clientHeight
    if (Math.abs(velocity) < 0.02 || atEdge) { weekInertiaFrame = 0; return }
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
  const myScrollTop = Math.max(0, (VIEW_START - 1) * MY_HOUR_HEIGHT)
  headerScrollX.value = scrollLeft
  const tryScroll = (attempt: number) => {
    if (attempt > 10) return
    nextTick(() => {
      if (dayBodyRef.value) setDayScrollLeft(scrollLeft)
      if (weekBodyRef.value) setWeekScrollTop(scrollTop)
      if (myDayBodyRef.value) myDayBodyRef.value.scrollTop = myScrollTop
      const dayPending = !!dayBodyRef.value && dayBodyRef.value.scrollLeft !== scrollLeft
      const weekPending = !!weekBodyRef.value && weekBodyRef.value.scrollTop !== scrollTop
      const myPending = !!myDayBodyRef.value && Math.abs(myDayBodyRef.value.scrollTop - myScrollTop) > 2
      if (dayPending || weekPending || myPending) setTimeout(() => tryScroll(attempt + 1), 100)
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
    nextTick(() => setDayScrollLeft(currentHourOffset * hourWidth.value))
  })
  dayBodyResizeObserver.observe(dayBodyRef.value)
}

// 拖拽事件（仅在真正移动后捕获指针，避免拦截事件块/单元格的 click）
function onDayPointerDown(e: PointerEvent) {
  if (e.button !== 0 || !dayBodyRef.value) return
  cancelDayInertia()
  Object.assign(dayDrag, { active: true, pointerId: e.pointerId, startX: e.clientX, startScrollLeft: dayBodyRef.value.scrollLeft, lastX: e.clientX, lastTime: performance.now(), velocity: 0, moved: false })
}
function onDayPointerMove(e: PointerEvent) {
  if (!dayDrag.active || dayDrag.pointerId !== e.pointerId || !dayBodyRef.value) return
  const deltaX = e.clientX - dayDrag.startX
  if (!dayDrag.moved && Math.abs(deltaX) > 4) {
    dayDrag.moved = true
    if (!dayBodyRef.value.hasPointerCapture(e.pointerId)) dayBodyRef.value.setPointerCapture(e.pointerId)
  }
  if (!dayDrag.moved) return
  const now = performance.now()
  const dt = Math.max(now - dayDrag.lastTime, 1)
  dayDrag.velocity = -((e.clientX - dayDrag.lastX) / dt)
  dayDrag.lastX = e.clientX; dayDrag.lastTime = now
  setDayScrollLeft(dayDrag.startScrollLeft - deltaX)
  e.preventDefault()
}
function onDayPointerUp(e: PointerEvent) {
  if (!dayDrag.active || dayDrag.pointerId !== e.pointerId || !dayBodyRef.value) return
  const didMove = dayDrag.moved
  if (dayBodyRef.value.hasPointerCapture(e.pointerId)) dayBodyRef.value.releasePointerCapture(e.pointerId)
  Object.assign(dayDrag, { active: false, pointerId: -1, moved: false })
  if (didMove) { startDayInertia(dayDrag.velocity * 18) }
}
function onWeekPointerDown(e: PointerEvent) {
  if (e.button !== 0 || !weekBodyRef.value) return
  cancelWeekInertia()
  Object.assign(weekDrag, { active: true, pointerId: e.pointerId, startY: e.clientY, startScrollTop: weekBodyRef.value.scrollTop, lastY: e.clientY, lastTime: performance.now(), velocity: 0, moved: false })
}
function onWeekPointerMove(e: PointerEvent) {
  if (!weekDrag.active || weekDrag.pointerId !== e.pointerId || !weekBodyRef.value) return
  const deltaY = e.clientY - weekDrag.startY
  if (!weekDrag.moved && Math.abs(deltaY) > 4) {
    weekDrag.moved = true
    if (!weekBodyRef.value.hasPointerCapture(e.pointerId)) weekBodyRef.value.setPointerCapture(e.pointerId)
  }
  if (!weekDrag.moved) return
  const now = performance.now()
  const dt = Math.max(now - weekDrag.lastTime, 1)
  weekDrag.velocity = -((e.clientY - weekDrag.lastY) / dt)
  weekDrag.lastY = e.clientY; weekDrag.lastTime = now
  weekBodyRef.value.scrollTop = weekDrag.startScrollTop - deltaY
  e.preventDefault()
}
function onWeekPointerUp(e: PointerEvent) {
  if (!weekDrag.active || weekDrag.pointerId !== e.pointerId || !weekBodyRef.value) return
  const didMove = weekDrag.moved
  if (weekBodyRef.value.hasPointerCapture(e.pointerId)) weekBodyRef.value.releasePointerCapture(e.pointerId)
  Object.assign(weekDrag, { active: false, pointerId: -1, moved: false })
  if (didMove) { startWeekInertia(weekDrag.velocity * 18) }
}

// ====== 迷你月历 ======
const miniDays = computed(() => {
  const y = miniYear.value, m = miniMonth.value
  const first = new Date(y, m, 1)
  const start = new Date(first); start.setDate(start.getDate() - start.getDay())
  const today = formatDate(new Date())
  const allDates = new Set(allReservations.value.map(r => r.startTime.split('T')[0]))
  return Array.from({ length: 42 }, (_, i) => {
    const dt = new Date(start); dt.setDate(dt.getDate() + i)
    const ds = formatDate(dt)
    return { date: dt.getDate(), dateStr: ds, currentMonth: dt.getMonth() === m, isToday: ds === today, hasEvent: allDates.has(ds) }
  })
})
function miniPrev() { if (miniMonth.value === 0) { miniYear.value--; miniMonth.value = 11 } else miniMonth.value-- }
function miniNext() { if (miniMonth.value === 11) { miniYear.value++; miniMonth.value = 0 } else miniMonth.value++ }
function onMiniPick(ds: string) {
  selectedMiniDate.value = ds
  currentDate.value = new Date(ds + 'T00:00:00')
}

// ====== 数据加载 ======
async function loadData() {
  const d = currentDate.value
  if (tab.value === 'my') {
    const p: Record<string, string | number> = { page: 1, size: 500 }
    // 注意：后端过滤为 r.start_time >= startTime AND r.end_time <= endTime（完全包含语义）
    // endTime 必须补到当天 23:59:59，否则当天的会议会因 end_time <= '当天 00:00:00' 被全部过滤
    if (viewMode.value === 'day') { p.startTime = formatDate(d); p.endTime = formatDate(d) + ' 23:59:59' }
    else if (viewMode.value === 'week') { p.startTime = formatDate(weekDays.value[0].date); p.endTime = formatDate(weekDays.value[6].date) + ' 23:59:59' }
    else {
      const ms = new Date(d.getFullYear(), d.getMonth(), 1); ms.setDate(ms.getDate() - ms.getDay())
      const me = new Date(ms); me.setDate(me.getDate() + 41)
      p.startTime = formatDate(ms); p.endTime = formatDate(me) + ' 23:59:59'
    }
    try { const r = await listMyReservations(p as any); myReservations.value = (r.data.records || []).filter((x: any) => x.status === 0 || x.status === 1) } catch { myReservations.value = [] }
  } else {
    const p: Record<string, string> = {}
    if (viewMode.value === 'day') p.date = formatDate(d)
    else if (viewMode.value === 'week') { p.startDate = formatDate(weekDays.value[0].date); p.endDate = formatDate(weekDays.value[6].date) }
    else {
      const ms = new Date(d.getFullYear(), d.getMonth(), 1); ms.setDate(ms.getDate() - ms.getDay())
      const me = new Date(ms); me.setDate(me.getDate() + 41)
      p.startDate = formatDate(ms); p.endDate = formatDate(me)
    }
    // 会议室日历仅展示已确认(1)的预约；待确认(0)尚未审批，不占用会议室时段
    try { const r = await getSchedule(p); rooms.value = r.data.rooms || []; roomReservations.value = (r.data.reservations || []).filter((x: any) => x.status === 1) } catch { /* */ }
  }
  if (viewMode.value === 'month') buildMonthDays()
  await nextTick()
  scrollToDefaultHour()
}

// ====== 日/周/月视图辅助 ======
function getRoomReservations(roomId: string) {
  const today = formatDate(currentDate.value)
  return roomReservations.value.filter(r => r.roomId === roomId && r.startTime.split('T')[0] === today)
}
function dayEventStyle(r: any) {
  const start = new Date(r.startTime), end = new Date(r.endTime)
  const startMinutes = (start.getHours() - START_HOUR) * 60 + start.getMinutes()
  const durationMinutes = (end.getTime() - start.getTime()) / 60000
  return { left: (dayEdgeGap.value + (startMinutes / (TOTAL_HOURS * 60)) * gridWidth.value) + 'px', width: ((durationMinutes / (TOTAL_HOURS * 60)) * gridWidth.value) + 'px' }
}
function myAgendaEventStyle(r: any) {
  const start = new Date(r.startTime), end = new Date(r.endTime)
  const startMinutes = start.getHours() * 60 + start.getMinutes()
  const durationMinutes = (end.getTime() - start.getTime()) / 60000
  const topPx = (startMinutes / 60) * MY_HOUR_HEIGHT
  const heightPx = Math.max((durationMinutes / 60) * MY_HOUR_HEIGHT, 32)
  // 重叠事件并排分栏
  const lay = myDayLayoutMap.value.get(r.id)
  if (lay && lay.totalColumns > 1) {
    const gap = 4
    const totalW = 100
    const colW = (totalW + gap) / lay.totalColumns
    return { top: topPx + 'px', height: heightPx + 'px', left: `calc(${lay.columnIndex * colW}% + 8px)`, width: `calc(${colW - gap}% - 12px)` }
  }
  return { top: topPx + 'px', height: heightPx + 'px', left: '8px', right: '8px' }
}
function weekEventStyle(r: any) {
  const di = weekDays.value.findIndex(d => d.dateStr === r.startTime.split('T')[0])
  if (di < 0) return { display: 'none' }
  const start = new Date(r.startTime), end = new Date(r.endTime)
  const startMinutes = (start.getHours() - START_HOUR) * 60 + start.getMinutes()
  const durationMinutes = (end.getTime() - start.getTime()) / 60000
  const topPx = weekEdgeGap + (startMinutes / (TOTAL_HOURS * 60)) * weekHoursHeight
  const heightPx = Math.max((durationMinutes / (TOTAL_HOURS * 60)) * weekHoursHeight, 16)
  const dayWidth = 100 / 7
  const info = weekEventLayoutMap.value.get(r.id)
  const col = info?.columnIndex ?? 0
  const total = info?.totalColumns ?? 1
  const gapPct = total > 1 ? 0.3 : 0
  const eventWidth = (dayWidth - (total - 1) * gapPct) / total
  const eventLeft = di * dayWidth + col * (eventWidth + gapPct)
  return { left: eventLeft + '%', width: eventWidth + '%', top: topPx + 'px', height: heightPx + 'px' }
}
function getDayReservations(day: any) { return day.reservations.length > 3 ? day.reservations.slice(0, 2) : day.reservations }
function buildMonthDays() {
  const d = currentDate.value, m = d.getMonth(), today = formatDate(new Date())
  const ms = new Date(d.getFullYear(), m, 1); ms.setDate(ms.getDate() - ms.getDay())
  monthDays.value = Array.from({ length: 42 }, (_, i) => {
    const dt = new Date(ms); dt.setDate(dt.getDate() + i); const ds = formatDate(dt)
    return { date: dt.getDate(), dateStr: ds, currentMonth: dt.getMonth() === m, isToday: ds === today, reservations: allReservations.value.filter(r => r.startTime.split('T')[0] === ds) }
  })
}

// ====== 交互 ======
function onDayCellClick(room: any, h: number) {
  const dateStr = formatDate(currentDate.value)
  bookingRoomId.value = room.id; bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T${String(h).padStart(2, '0')}:00:00`
  bookingEndTime.value = `${dateStr}T${String(h + 1).padStart(2, '0')}:00:00`
  bookingVisible.value = true
}
function onWeekCellClick(dateStr: string, h: number) {
  bookingRoomId.value = undefined; bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T${String(h).padStart(2, '0')}:00:00`
  bookingEndTime.value = `${dateStr}T${String(h + 1).padStart(2, '0')}:00:00`
  bookingVisible.value = true
}
function onMonthCellClick(day: any) {
  bookingRoomId.value = undefined; bookingDate.value = day.dateStr
  bookingStartTime.value = ''; bookingEndTime.value = ''
  bookingVisible.value = true
}
// 我的日历-日视图：点击空白时段按 Y 坐标推算时间，快速预约
function onMyDayAreaClick(e: MouseEvent) {
  const layer = myDayEventsRef.value
  if (!layer) return
  const rect = layer.getBoundingClientRect()
  const y = e.clientY - rect.top
  const hour = Math.max(0, Math.min(23, Math.floor(y / MY_HOUR_HEIGHT)))
  const dateStr = formatDate(currentDate.value)
  bookingRoomId.value = undefined; bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T${String(hour).padStart(2, '0')}:00:00`
  bookingEndTime.value = `${dateStr}T${String(hour + 1).padStart(2, '0')}:00:00`
  bookingVisible.value = true
}
// 月视图「更多」浮层控制
function onMorePopoverToggle(ds: string, v: boolean) { morePopoverDay.value = v ? ds : '' }
function onPopoverEventClick(r: any) { morePopoverDay.value = ''; onEventClick(r) }
// 工具
function weekdayText(dt: Date) { return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dt.getDay()] }
function isCurrentDay(dt: Date) { return formatDate(dt) === formatDate(new Date()) }
function openQuickBook() {
  const dateStr = formatDate(currentDate.value)
  bookingRoomId.value = undefined; bookingDate.value = dateStr
  bookingStartTime.value = `${dateStr}T09:00:00`; bookingEndTime.value = `${dateStr}T10:00:00`
  bookingVisible.value = true
}
function onBookingSuccess() { loadData(); fetchTodayData() }

// 独立加载今日数据（用于左侧今日卡片，不受视图范围影响）
async function fetchTodayData() {
  const today = formatDate(new Date())
  try {
    const r = await listMyReservations({ page: 1, size: 500, startTime: today, endTime: today + ' 23:59:59' })
    todayReservations.value = (r.data.records || []).filter((x: any) => x.status === 0 || x.status === 1)
  } catch { todayReservations.value = [] }
}

async function onEventClick(r: any) {
  detailVisible.value = true
  detailLoading.value = true
  currentDetail.value = null
  try {
    const api = isAdmin ? getReservationDetail : getMyReservationDetail
    const res = await api(r.id)
    currentDetail.value = res.data
  } catch { ElMessage.error('加载详情失败') }
  finally { detailLoading.value = false }
}

const canCancel = computed(() => {
  if (!currentDetail.value) return false
  if (currentDetail.value.status !== 1) return false
  const now = Date.now()
  return new Date(currentDetail.value.startTime).getTime() > now
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

function switchTab(t: 'my' | 'room') { if (tab.value !== t) { tab.value = t; loadData() } }
function goPrev() { const d = new Date(currentDate.value); if (viewMode.value === 'day') d.setDate(d.getDate() - 1); else if (viewMode.value === 'week') d.setDate(d.getDate() - 7); else d.setMonth(d.getMonth() - 1); currentDate.value = d }
function goNext() { const d = new Date(currentDate.value); if (viewMode.value === 'day') d.setDate(d.getDate() + 1); else if (viewMode.value === 'week') d.setDate(d.getDate() + 7); else d.setMonth(d.getMonth() + 1); currentDate.value = d }
function goToday() { currentDate.value = new Date(); miniYear.value = new Date().getFullYear(); miniMonth.value = new Date().getMonth() }

// 同步迷你月历到主视图
watch(currentDate, (d) => { miniYear.value = d.getFullYear(); miniMonth.value = d.getMonth(); selectedMiniDate.value = formatDate(d) })

watch([viewMode, currentDate], loadData)
watch(viewMode, async (mode) => {
  await nextTick()
  if (mode === 'day') observeDayBodyResize()
  scrollToDefaultHour()
})
watch(workHoursOnly, () => { /* 仅视觉切换 */ })

onMounted(async () => {
  await nextTick()
  observeDayBodyResize()
  nowTimer = window.setInterval(() => { nowTimestamp.value = Date.now() }, 60000)
  loadData()
  fetchTodayData()
})

onBeforeUnmount(() => {
  cancelDayInertia(); cancelWeekInertia()
  dayBodyResizeObserver?.disconnect()
  if (nowTimer) clearInterval(nowTimer)
})
</script>

<style scoped>
.v2-page { display: flex; height: calc(100vh - 60px); background: var(--bg-page, #f5f6fa); overflow: hidden; }

/* ============ 左侧任务栏 ============ */
.v2-sidebar {
  width: 240px; flex-shrink: 0; background: var(--bg-card, #fff);
  border-right: 1px solid var(--border-light, #e5e7eb);
  padding: 16px; overflow-y: auto; transition: width 0.2s; position: relative;
}
.v2-sidebar.collapsed { width: 48px; padding: 16px 8px; }
.sidebar-toggle {
  position: absolute; top: 12px; right: 8px;
  width: 24px; height: 24px; border: none; background: transparent;
  color: var(--text-muted, #9ca3af); cursor: pointer; border-radius: 4px;
}
.sidebar-toggle:hover { background: var(--bg-hover, #f3f4f6); color: var(--primary, #667eea); }

.mini-cal { margin-top: 28px; }
.mini-cal-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.mini-nav { border: none; background: transparent; cursor: pointer; color: var(--text-secondary); padding: 4px; border-radius: 4px; }
.mini-nav:hover { background: var(--bg-hover, #f3f4f6); color: var(--primary, #667eea); }
.mini-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.mini-week { display: grid; grid-template-columns: repeat(7, 1fr); margin-bottom: 4px; }
.mini-week span { text-align: center; font-size: 11px; color: var(--text-muted); padding: 4px 0; }
.mini-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; }
.mini-cell {
  position: relative; aspect-ratio: 1; border: none; background: transparent;
  cursor: pointer; border-radius: 6px; display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: var(--text-primary);
}
.mini-cell:hover { background: var(--bg-hover, #f3f4f6); }
.mini-cell.other { color: var(--text-muted); opacity: 0.5; }
.mini-cell.today { background: var(--primary, #667eea); color: #fff; }
.mini-cell.selected { background: rgba(102, 126, 234, 0.15); color: var(--primary, #667eea); font-weight: 600; }
.mini-cell.today.selected { background: var(--primary, #667eea); color: #fff; }
.mini-dot { position: absolute; bottom: 2px; width: 4px; height: 4px; border-radius: 50%; background: var(--primary, #667eea); }
.mini-cell.today .mini-dot { background: #fff; }

.tab-segment { display: flex; margin: 20px 0 12px; background: var(--bg-hover, #f3f4f6); border-radius: 8px; padding: 2px; }
.seg-btn { flex: 1; border: none; background: transparent; padding: 6px 8px; font-size: 12px; cursor: pointer; color: var(--text-secondary); border-radius: 6px; transition: all 0.2s; }
.seg-btn.active { background: #fff; color: var(--primary, #667eea); font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }

.filter-row { display: flex; align-items: center; gap: 8px; padding: 8px 0; }
.filter-label { font-size: 12px; color: var(--text-secondary); }
.filter-time { font-size: 11px; color: var(--text-muted); margin-left: auto; }

.today-card { margin-top: 16px; padding: 12px; background: linear-gradient(135deg, rgba(102,126,234,0.08), rgba(102,126,234,0.02)); border-radius: 10px; border: 1px solid rgba(102,126,234,0.15); }
.today-card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.today-label { font-size: 11px; color: var(--text-muted); }
.today-date { font-size: 12px; font-weight: 600; color: var(--primary, #667eea); }
.today-card-body { padding: 8px 0; }
.today-next { font-size: 10px; color: var(--text-muted); margin-bottom: 2px; }
.today-next-title { font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 2px; }
.today-next-time { font-size: 11px; color: var(--text-secondary); }
.today-empty { font-size: 12px; color: var(--text-muted); text-align: center; padding: 8px 0; }
.today-card-foot { font-size: 11px; color: var(--text-muted); padding-top: 8px; border-top: 1px solid var(--border-light); }

/* ============ 主视图 ============ */
.v2-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.v2-control-bar { display: flex; justify-content: space-between; align-items: center; padding: 10px 20px; background: var(--bg-card, #fff); border-bottom: 1px solid var(--border-light, #e5e7eb); flex-shrink: 0; }
.control-left, .control-right { display: flex; align-items: center; gap: 8px; }
.ctrl-btn { display: inline-flex; align-items: center; justify-content: center; gap: 4px; height: 32px; padding: 0 14px; border: 1px solid var(--border, #e5e7eb); background: #fff; color: var(--text-primary); border-radius: 8px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
.ctrl-btn:hover { border-color: var(--primary, #667eea); color: var(--primary, #667eea); }
.ctrl-btn.primary { background: var(--primary, #667eea); color: #fff; border-color: var(--primary, #667eea); }
.ctrl-btn.primary:hover { opacity: 0.9; }
.ctrl-icon { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; border: 1px solid var(--border, #e5e7eb); background: #fff; color: var(--text-secondary); border-radius: 8px; cursor: pointer; transition: all 0.2s; }
.ctrl-icon:hover { border-color: var(--primary, #667eea); color: var(--primary, #667eea); }
.view-segment { display: inline-flex; align-items: center; height: 32px; margin: 0 4px; padding: 2px; background: var(--bg-hover, #f3f4f6); border-radius: 8px; }
.seg-item { height: 28px; min-width: 40px; padding: 0 14px; border: none; background: transparent; color: var(--text-secondary); font-size: 13px; border-radius: 6px; cursor: pointer; transition: all 0.2s; }
.seg-item:hover { color: var(--primary, #667eea); }
.seg-item.active { background: #fff; color: var(--primary, #667eea); font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.date-display { font-size: 14px; font-weight: 500; color: var(--text-primary); margin-left: 8px; }

.v2-card { flex: 1; background: var(--bg-card, #fff); margin: 12px; border-radius: 12px; border: 1px solid var(--border-light, #e5e7eb); overflow: hidden; display: flex; flex-direction: column; }

/* ============ 日视图 - 我的日历（纵向议程） ============ */
.my-agenda { display: flex; flex-direction: column; flex: 1; overflow: hidden; }
.my-agenda-head { display: flex; align-items: center; gap: 10px; padding: 14px 20px 10px; border-bottom: 1px solid var(--border-light); flex-shrink: 0; }
.my-agenda-date { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.my-agenda-weekday { font-size: 13px; color: var(--text-secondary); }
.my-agenda-today-badge { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: var(--primary, #667eea); color: #fff; }
.my-agenda-count { margin-left: auto; font-size: 12px; color: var(--text-muted); }
.my-agenda-body { flex: 1; overflow-y: auto; overflow-x: hidden; }
.my-agenda-body::-webkit-scrollbar { width: 8px; }
.my-agenda-body::-webkit-scrollbar-thumb { background: var(--border, #cbd5e1); border-radius: 4px; }
.my-agenda-scroll { display: flex; position: relative; min-height: 100%; }
.my-agenda-grid { width: 64px; flex-shrink: 0; position: sticky; left: 0; z-index: 2; background: var(--bg-card, #fff); }
.my-agenda-hour { height: 56px; position: relative; }
.my-agenda-hour-label { font-size: 11px; color: var(--text-muted); position: absolute; top: -7px; right: 8px; background: var(--bg-card, #fff); padding: 0 4px; }
.my-agenda-events { flex: 1; position: relative; height: 1344px; }
.my-agenda-work-band { position: absolute; left: 0; right: 0; background: rgba(102,126,234,0.05); pointer-events: none; z-index: 0; }
.my-agenda-line { position: absolute; left: 0; right: 0; height: 1px; background: var(--border-light, #f0f0f0); pointer-events: none; }
.my-agenda-now { position: absolute; left: 0; right: 0; height: 2px; background: #ef4444; z-index: 3; pointer-events: none; }
.my-now-dot { position: absolute; left: -5px; top: -4px; width: 10px; height: 10px; border-radius: 50%; background: #ef4444; }
.my-now-label { position: absolute; left: 10px; top: -9px; padding: 1px 6px; border-radius: 999px; background: #ef4444; color: #fff; font-size: 11px; }
.my-agenda-event { position: absolute; display: flex; gap: 8px; padding: 6px 10px; border-radius: 8px; cursor: pointer; transition: all 0.2s; z-index: 2; overflow: hidden; }
.my-agenda-event .evt-bar { width: 3px; border-radius: 2px; flex-shrink: 0; }
.my-agenda-event .evt-body { flex: 1; min-width: 0; overflow: hidden; }
.my-agenda-empty { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); display: flex; flex-direction: column; align-items: center; gap: 8px; color: var(--text-muted); font-size: 13px; pointer-events: none; }
.my-agenda-empty .el-icon { font-size: 36px; opacity: 0.4; }

/* 事件块通用样式（飞书实色渐变） */
.evt-block { color: #fff; border: none; box-shadow: 0 1px 3px rgba(0,0,0,0.12); }
.evt-block .evt-bar { background: rgba(255,255,255,0.6); }
.evt-block .evt-title { color: #fff; }
.evt-block .evt-time { color: rgba(255,255,255,0.92); }
.evt-block .evt-meta { color: rgba(255,255,255,0.8); }
.evt-block .evt-room { color: rgba(255,255,255,0.85); }
.evt-block:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(0,0,0,0.2); filter: brightness(1.05); }
.evt-block.s0 { background: linear-gradient(135deg, #f59e0b, #d97706); }
.evt-block.s1 { background: linear-gradient(135deg, #10b981, #059669); }
.evt-block.s2 { background: linear-gradient(135deg, #9ca3af, #6b7280); }
.evt-block.s3 { background: linear-gradient(135deg, #ef4444, #dc2626); }
.evt-title { font-size: 12px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.evt-time { font-size: 10px; color: var(--text-secondary); margin-top: 2px; }
.evt-meta { font-size: 10px; color: var(--text-muted); margin-top: 1px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ============ 日视图 - 会议室日历（横向多行） ============ */
.day-view.dragging, .day-view.dragging *, .week-view.dragging, .week-view.dragging * { cursor: grabbing !important; user-select: none; }
.day-body-wrap { flex: 1; overflow: auto; cursor: grab; scrollbar-gutter: stable; }
.day-body-wrap::-webkit-scrollbar { width: 10px; height: 10px; }
.day-body-wrap::-webkit-scrollbar-track { background: transparent; }
.day-body-wrap::-webkit-scrollbar-thumb { background: var(--border, #cbd5e1); border-radius: 4px; }
.day-body-wrap::-webkit-scrollbar-thumb:hover { background: var(--text-muted, #9ca3af); }
.day-body { position: relative; min-width: fit-content; }
.day-header { display: flex; border-bottom: 2px solid var(--border-light, #e5e7eb); background: var(--bg-hover, #fafbfc); flex-shrink: 0; }
.room-col-header { width: 120px; padding: 10px 12px; font-size: 12px; font-weight: 600; color: var(--text-secondary); flex-shrink: 0; border-right: 1px solid var(--border-light); position: sticky; left: 0; z-index: 3; background: var(--bg-hover, #fafbfc); }
.day-ticks-wrap { flex: 1; overflow: hidden; }
.day-ticks { display: flex; position: relative; min-height: 34px; padding: 0 var(--day-edge-gap, 24px); }
.tick { width: var(--hour-width, 60px); flex-shrink: 0; padding-top: 8px; }
.tick-label { font-size: 11px; color: var(--text-muted); }
.tick-end .tick-label { transform: translateX(-50%); }
.day-work-hours-band { position: absolute; top: 0; bottom: 0; background: rgba(102,126,234,0.06); pointer-events: none; z-index: 0; }
.day-now-column { position: absolute; top: 0; bottom: 0; width: 2px; background: rgba(59,130,246,0.8); z-index: 3; pointer-events: none; }
.day-now-label { position: absolute; top: 6px; left: 8px; padding: 2px 6px; border-radius: 999px; background: #3b82f6; color: #fff; font-size: 11px; }
.day-row { display: flex; height: 64px; border-bottom: 1px solid var(--border-light, #f0f0f0); position: relative; }
.room-label { width: 120px; padding: 6px 12px; border-right: 1px solid var(--border-light); display: flex; flex-direction: column; justify-content: center; flex-shrink: 0; background: #fff; z-index: 4; position: sticky; left: 0; }
.room-name { font-size: 12px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.room-meta { font-size: 10px; color: var(--text-muted); margin-top: 2px; }
.day-grid-wrap { position: relative; flex-shrink: 0; padding: 0 var(--day-edge-gap, 24px); overflow: hidden; }
.day-grid { display: flex; height: 100%; }
.grid-cell { width: var(--hour-width, 60px); border-right: 1px solid var(--border-light, #f3f4f6); cursor: pointer; flex-shrink: 0; }
.grid-cell:hover { background: rgba(102,126,234,0.04); }
.day-event { position: absolute; top: 4px; bottom: 4px; border-radius: 6px; padding: 4px 6px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 2; transition: all 0.2s; }
.day-event:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.evt-inner { height: 100%; display: flex; flex-direction: column; justify-content: center; overflow: hidden; }

/* ============ 周视图 ============ */
.week-header { display: flex; border-bottom: 1px solid var(--border-light); background: var(--bg-hover, #fafbfc); flex-shrink: 0; }
.wk-corner { width: 40px; flex-shrink: 0; border-right: 1px solid var(--border-light); position: sticky; left: 0; z-index: 3; background: var(--bg-hover, #fafbfc); }
.wk-day { flex: 1; padding: 8px 4px; text-align: center; border-right: 1px solid var(--border-light); }
.wk-day:last-child { border-right: none; }
.wk-day.today { background: rgba(102,126,234,0.08); }
.wk-day.today .wk-day-num { background: var(--primary, #667eea); color: #fff; border-radius: 50%; }
.wk-day-name { font-size: 11px; color: var(--text-muted); }
.wk-day-num { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-top: 2px; display: inline-block; width: 28px; height: 28px; line-height: 28px; }
.week-body-wrap { flex: 1; overflow: auto; max-height: 100%; cursor: grab; }
.week-body-wrap::-webkit-scrollbar { width: 8px; height: 8px; }
.week-body-wrap::-webkit-scrollbar-thumb { background: var(--border, #cbd5e1); border-radius: 4px; }
.week-body { display: flex; position: relative; }
.wk-times { width: 40px; flex-shrink: 0; position: sticky; left: 0; z-index: 2; background: #fff; padding: var(--week-edge-gap, 24px) 0; }
.wk-time { height: 60px; position: relative; border-bottom: 1px solid var(--border-light, #f0f0f0); }
.wk-time-label { font-size: 11px; color: var(--text-muted); position: absolute; top: -7px; left: 4px; background: #fff; padding: 0 2px; }
.wk-time-now-label { color: #ef4444; font-weight: 600; }
.wk-grid-wrap { flex: 1; }
.wk-grid { position: relative; display: flex; padding: var(--week-edge-gap, 24px) 0; }
.wk-work-hours-band { position: absolute; left: 0; right: 0; background: rgba(102,126,234,0.06); pointer-events: none; z-index: 0; }
.wk-now-line { position: absolute; height: 2px; background: #3b82f6; z-index: 1; pointer-events: none; }
.wk-now-line::before { content: ''; position: absolute; left: -6px; top: -4px; width: 10px; height: 10px; border-radius: 50%; background: #3b82f6; }
.wk-now-label { position: absolute; top: -12px; left: 8px; padding: 2px 6px; border-radius: 999px; background: #3b82f6; color: #fff; font-size: 11px; }
.wk-col { flex: 1; display: flex; flex-direction: column; position: relative; }
.wk-cell { height: 60px; border-right: 1px solid var(--border-light, #f3f4f6); border-bottom: 1px solid var(--border-light, #f0f0f0); cursor: pointer; }
.wk-cell:last-child { border-right: none; }
.wk-cell:hover { background: rgba(102,126,234,0.04); }
.week-event { position: absolute; border-radius: 6px; padding: 3px 6px; font-size: 11px; overflow: hidden; cursor: pointer; z-index: 2; transition: all 0.2s; }
.week-event:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.evt-room { font-size: 10px; color: var(--text-muted); margin-top: 1px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ============ 月视图 ============ */
.month-view { padding: 0; }
.month-header { display: grid; grid-template-columns: repeat(7, 1fr); border-bottom: 1px solid var(--border-light); }
.mh-cell { padding: 10px; text-align: center; font-size: 12px; font-weight: 600; color: var(--text-secondary); }
.month-grid { display: grid; grid-template-columns: repeat(7, 1fr); flex: 1; overflow: auto; }
.month-cell { min-height: 100px; border-right: 1px solid var(--border-light); border-bottom: 1px solid var(--border-light); padding: 4px; cursor: pointer; transition: background 0.15s; overflow: hidden; }
.month-cell:hover { background: rgba(102,126,234,0.04); }
.month-cell.other-month { background: var(--bg-hover, #fafbfc); }
.month-cell.other-month .mc-date { color: var(--text-muted); opacity: 0.5; }
.month-cell.today { background: rgba(102,126,234,0.08); }
.month-cell.today .mc-date { color: var(--primary, #667eea); font-weight: 600; }
.mc-date { font-size: 12px; padding: 4px; color: var(--text-primary); }
.mc-events { display: flex; flex-direction: column; gap: 2px; margin-top: 4px; }
.mc-event { display: flex; gap: 4px; align-items: center; font-size: 11px; padding: 2px 6px; border-radius: 4px; cursor: pointer; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mc-event-time { font-weight: 600; opacity: 0.9; flex-shrink: 0; }
.mc-event-title { overflow: hidden; text-overflow: ellipsis; }
.mc-more { font-size: 11px; color: var(--primary, #667eea); padding: 2px 8px; cursor: pointer; border: none; background: transparent; border-radius: 4px; transition: background 0.15s; width: 100%; text-align: left; }
.mc-more:hover { background: rgba(102,126,234,0.08); }
/* 月视图 Popover 浮层 */
.mc-pop-list { display: flex; flex-direction: column; gap: 4px; max-height: 320px; overflow-y: auto; }
.mc-pop-list::-webkit-scrollbar { width: 6px; }
.mc-pop-list::-webkit-scrollbar-thumb { background: var(--border, #cbd5e1); border-radius: 3px; }
.mc-pop-head { font-size: 12px; font-weight: 600; color: var(--text-secondary); padding-bottom: 6px; border-bottom: 1px solid var(--border-light); margin-bottom: 4px; }
.mc-pop-item { display: flex; gap: 8px; align-items: center; padding: 6px 8px; border-radius: 6px; cursor: pointer; font-size: 12px; transition: all 0.15s; }
.mc-pop-time { font-weight: 600; flex-shrink: 0; min-width: 40px; }
.mc-pop-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ============ 右侧抽屉 ============ */
.v2-drawer {
  width: 360px; flex-shrink: 0; background: var(--bg-card, #fff);
  border-left: 1px solid var(--border-light, #e5e7eb);
  display: flex; flex-direction: column; box-shadow: -4px 0 16px rgba(0,0,0,0.04);
}
.drawer-slide-enter-active, .drawer-slide-leave-active { transition: transform 0.3s ease, opacity 0.3s ease; }
.drawer-slide-enter-from, .drawer-slide-leave-to { transform: translateX(20px); opacity: 0; }
.drawer-head { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--border-light); }
.drawer-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.drawer-close { border: none; background: transparent; cursor: pointer; color: var(--text-muted); padding: 4px; border-radius: 4px; }
.drawer-close:hover { background: var(--bg-hover, #f3f4f6); color: var(--text-primary); }
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
.detail-subject { font-size: 18px; font-weight: 600; color: var(--text-primary); margin: 0 0 16px; }
.detail-row { display: flex; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--border-light); }
.detail-row .el-icon { color: var(--text-muted); margin-top: 2px; }
.detail-row-val { font-size: 13px; color: var(--text-primary); font-weight: 500; }
.detail-row-val.danger { color: #ef4444; }
.detail-row-sub { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.detail-attendees { margin-top: 16px; }
.attendees-title { font-size: 12px; font-weight: 600; color: var(--text-secondary); margin-bottom: 8px; }
.attendee-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border-light); }
.attendee-avatar { width: 32px; height: 32px; flex-shrink: 0; }
.attendee-info { flex: 1; min-width: 0; }
.attendee-name { font-size: 13px; color: var(--text-primary); font-weight: 500; }
.attendee-dept { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.drawer-foot { display: flex; gap: 8px; padding: 12px 20px; border-top: 1px solid var(--border-light); }
.drawer-btn { flex: 1; padding: 8px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; transition: all 0.2s; border: 1px solid var(--border, #e5e7eb); background: #fff; color: var(--text-primary); }
.drawer-btn:hover { border-color: var(--primary, #667eea); color: var(--primary, #667eea); }
.drawer-btn.primary { background: var(--primary, #667eea); color: #fff; border-color: var(--primary, #667eea); }
.drawer-btn.primary:hover { opacity: 0.9; }
.drawer-btn.danger { color: #ef4444; border-color: #fecaca; }
.drawer-btn.danger:hover { background: #ef4444; color: #fff; border-color: #ef4444; }

/* ============ 暗色模式 ============ */
html.dark .v2-page { background: var(--bg-page, #1a1a2e); }
html.dark .v2-sidebar { background: var(--bg-card, #161628); border-color: var(--border, #2a2a44); }
html.dark .sidebar-toggle { color: var(--text-muted); }
html.dark .sidebar-toggle:hover { background: #252542; color: #818cf8; }
html.dark .mini-title { color: var(--text-primary); }
html.dark .mini-cell { color: var(--text-primary); }
html.dark .mini-cell:hover { background: #252542; }
html.dark .mini-cell.today { background: #667eea; color: #fff; }
html.dark .mini-cell.selected { background: rgba(102,126,234,0.25); color: #a5b4fc; }
html.dark .tab-segment { background: #1c1c2e; }
html.dark .seg-btn { color: var(--text-secondary); }
html.dark .seg-btn.active { background: #252542; color: #a5b4fc; }
html.dark .today-card { background: linear-gradient(135deg, rgba(102,126,234,0.15), rgba(102,126,234,0.05)); border-color: rgba(102,126,234,0.3); }
html.dark .today-label { color: var(--text-muted); }
html.dark .today-date { color: #a5b4fc; }
html.dark .today-next-title { color: var(--text-primary); }
html.dark .today-next-time { color: var(--text-secondary); }
html.dark .today-card-foot { border-color: var(--border); color: var(--text-muted); }
html.dark .v2-control-bar { background: var(--bg-card, #161628); border-color: var(--border, #2a2a44); }
html.dark .ctrl-btn, html.dark .ctrl-icon { background: #252542; border-color: var(--border); color: var(--text-primary); }
html.dark .ctrl-btn:hover, html.dark .ctrl-icon:hover { border-color: #667eea; color: #a5b4fc; }
html.dark .ctrl-btn.primary { background: #667eea; color: #fff; border-color: #667eea; }
html.dark .view-segment { background: #1c1c2e; }
html.dark .seg-item { color: var(--text-secondary); }
html.dark .seg-item.active { background: #252542; color: #a5b4fc; }
html.dark .date-display { color: var(--text-primary); }
html.dark .v2-card { background: var(--bg-card, #161628); border-color: var(--border, #2a2a44); }
html.dark .my-agenda-head { border-color: var(--border); }
html.dark .my-agenda-date { color: var(--text-primary); }
html.dark .my-agenda-weekday { color: var(--text-secondary); }
html.dark .my-agenda-count { color: var(--text-muted); }
html.dark .my-agenda-grid { background: var(--bg-card, #161628); }
html.dark .my-agenda-hour-label { background: var(--bg-card, #161628); color: var(--text-muted); }
html.dark .my-agenda-line { background: var(--border); }
html.dark .day-header, html.dark .week-header { background: #161628; border-color: var(--border); }
html.dark .tick-label, html.dark .wk-day-name, html.dark .wk-time-label, html.dark .mh-cell { color: var(--text-muted); }
html.dark .day-row, html.dark .wk-time, html.dark .wk-cell, html.dark .month-cell { border-color: var(--border); }
html.dark .room-label, html.dark .wk-times, html.dark .room-col-header, html.dark .wk-corner { background: var(--bg-card, #161628); border-color: var(--border); }
html.dark .room-name, html.dark .wk-day-num, html.dark .mc-date { color: var(--text-primary); }
html.dark .grid-cell, html.dark .wk-cell { border-color: var(--border); }
html.dark .grid-cell:hover, html.dark .wk-cell:hover, html.dark .month-cell:hover { background: #252542; }
html.dark .month-cell.other-month { background: #161628; }
html.dark .month-cell.today { background: rgba(102,126,234,0.15); }
html.dark .month-cell.today .mc-date { color: #a5b4fc; }
html.dark .mc-more { color: #a5b4fc; }
html.dark .mc-more:hover { background: rgba(102,126,234,0.18); }
html.dark .mc-pop-head { color: var(--text-secondary); border-color: var(--border); }
html.dark .v2-drawer { background: var(--bg-card, #161628); border-color: var(--border, #2a2a44); }
html.dark .drawer-head, html.dark .drawer-foot { border-color: var(--border); }
html.dark .drawer-title { color: var(--text-primary); }
html.dark .drawer-close { color: var(--text-muted); }
html.dark .drawer-close:hover { background: #252542; color: var(--text-primary); }
html.dark .detail-row { border-color: var(--border); }
html.dark .detail-subject { color: var(--text-primary); }
html.dark .detail-row-val { color: var(--text-primary); }
html.dark .attendees-title { color: var(--text-secondary); }
html.dark .attendee-item { border-color: var(--border); }
html.dark .attendee-name { color: var(--text-primary); }
html.dark .attendee-dept { color: var(--text-muted); }
html.dark .drawer-btn { background: #252542; border-color: var(--border); color: var(--text-primary); }
html.dark .drawer-btn:hover { border-color: #667eea; color: #a5b4fc; }
html.dark .drawer-btn.primary { background: #667eea; color: #fff; border-color: #667eea; }
html.dark .drawer-btn.danger { color: #fca5a5; border-color: #7f1d1d; }
html.dark .drawer-btn.danger:hover { background: #ef4444; color: #fff; border-color: #ef4444; }

/* 响应式 */
@media (max-width: 1280px) {
  .v2-sidebar { width: 200px; }
  .v2-drawer { width: 320px; }
}
@media (max-width: 1024px) {
  .v2-sidebar { position: absolute; left: 0; top: 0; bottom: 0; z-index: 10; box-shadow: 4px 0 16px rgba(0,0,0,0.08); }
  .v2-sidebar.collapsed { transform: translateX(-100%); width: 240px; }
}
</style>
