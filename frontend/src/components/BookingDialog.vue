<template>
  <el-drawer v-model="visible" title="预约会议室" size="600px" direction="rtl" destroy-on-close @close="handleClose">
    <div v-if="room" class="dialog-rules-tip">
      <el-icon><InfoFilled /></el-icon>
      可预约时段: {{ room.bookableStart || '08:00' }}~{{ room.bookableEnd || '20:00' }}，最长 {{ room.maxDuration || 480 }} 分钟，最多提前 {{ room.advanceDays || 7 }} 天
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="booking-form">
      <div v-if="!room" class="dialog-form-item">
        <label>会议室</label>
        <el-select v-model="selectedRoomId" placeholder="请选择会议室" style="width: 100%" @change="onRoomChange">
            <el-option v-for="r in roomList" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
      </div>
      <div v-else class="dialog-form-item"><label>会议室</label><el-input :value="room?.name" disabled /></div>
      <div class="dialog-form-item"><label>会议主题</label><el-input v-model="form.subject" placeholder="请输入会议主题" /></div>
      <div class="dialog-form-item">
        <label>预约日期</label>
        <el-date-picker v-model="form.selectedDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" :disabled-date="disableFutureDate" @change="onDateChange" />
      </div>
      <el-form-item label="预约时间" prop="startMinute">
        <div class="time-picker">
          <!-- 顶部信息栏 -->
          <div class="tp-header">
            <div class="tp-summary">
              <template v-if="form.startMinute && form.endMinute">
                <span class="tp-range">{{ form.startMinute }} <span class="tp-arrow">→</span> {{ form.endMinute }}</span>
                <span class="tp-dur" :class="{ over: isOverMaxDuration }">{{ durationText }}</span>
              </template>
              <template v-else-if="form.startMinute">
                <span class="tp-range">{{ form.startMinute }} <span class="tp-arrow muted">→ 选择结束</span></span>
              </template>
              <template v-else>
                <span class="tp-range placeholder">在下方时间轴拖动选择时段</span>
              </template>
            </div>
            <el-radio-group v-model="timeStep" size="small">
              <el-radio-button :value="15">15分</el-radio-button>
              <el-radio-button :value="30">30分</el-radio-button>
            </el-radio-group>
          </div>

          <!-- 纵向时间轴：拖拽选区 -->
          <div v-if="timeSlots.length" ref="timelineRef" class="tp-timeline" @pointermove="onPointerMove">
            <div
              v-for="s in timeSlots"
              :key="s.time"
              class="tp-slot"
              :class="slotClass(s)"
              :data-time="s.time"
              @pointerdown="onPointerDown($event, s.time)"
            >
              <span class="tp-label">{{ s.time }}</span>
              <div class="tp-cell">
                <template v-if="s.booked">
                  <el-tooltip :content="`${s.bookedSubject} · ${s.bookedUser || '未知'} (${s.bookedStart}-${s.bookedEnd})`" placement="right" :show-after="200">
                    <div class="tp-booked">
                      <span class="tp-booked-text">{{ s.bookedSubject }}</span>
                      <span v-if="s.bookedUser" class="tp-booked-user">{{ s.bookedUser }}</span>
                    </div>
                  </el-tooltip>
                </template>
                <template v-else-if="isSlotInSelection(s.time) && !isEndEdge(s.time)">
                  <div class="tp-selected"></div>
                </template>
                <template v-else-if="isStartEdge(s.time)">
                  <div class="tp-selected tp-selected-start">
                    <span class="tp-edge-label">开始 {{ form.startMinute }}</span>
                  </div>
                </template>
                <template v-else-if="isEndEdge(s.time)">
                  <div class="tp-selected tp-selected-end">
                    <span class="tp-edge-label">结束 {{ form.endMinute }}</span>
                  </div>
                </template>
              </div>
            </div>
            <!-- 当前时间指示线 -->
            <div v-if="nowLinePct >= 0" class="tp-now-line" :style="{ top: nowLinePct + '%' }"><span class="tp-now-label">现在</span></div>
          </div>
          <div v-else class="tp-empty">{{ emptyHint }}</div>

          <div class="tp-legend">
            <span class="lg"><i class="dot free"></i>空闲</span>
            <span class="lg"><i class="dot busy"></i>已约</span>
            <span class="lg"><i class="dot sel"></i>已选</span>
            <span class="tp-hint">支持拖动选择 / 点击起点再点击终点</span>
          </div>
        </div>
      </el-form-item>
      <div class="dialog-form-item">
        <label>参会人员<span v-if="form.attendeeUserIds.length" class="attendee-count">已选 {{ form.attendeeUserIds.length }} 人</span></label>
        <div class="attendee-display" @click="userSelectVisible = true">
          <template v-if="selectedAttendees.length">
            <el-tag v-for="u in selectedAttendees" :key="u.id" size="small" closable @close.stop="removeAttendee(u.id)">
              {{ u.realName || u.username }}
            </el-tag>
          </template>
          <span v-else class="attendee-placeholder">点击选择参会人员（选填）</span>
        </div>
        <UserSelectDialog v-model="userSelectVisible" :selected-ids="form.attendeeUserIds" @confirm="onAttendeesConfirm" />
      </div>
      <div class="dialog-form-item"><label>备注</label><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" /></div>
    </el-form>
    <template #footer>
      <div style="display: flex; justify-content: flex-end; gap: 12px; width: 100%;">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { createReservation } from '@/api/reservation'
import { listByRoomAndDate } from '@/api/reservation'
import { listActiveRooms } from '@/api/meeting'
import { listContacts } from '@/api/user'
import UserSelectDialog from '@/components/UserSelectDialog.vue'
import type { MeetingRoom } from '@/types/meeting'
import type { UserInfo } from '@/types/user'

const props = defineProps<{
  modelValue: boolean
  room?: MeetingRoom | null
  rooms?: MeetingRoom[]
  roomId?: string
  date?: string
  startTime?: string
  endTime?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref<FormInstance>()
const submitting = ref(false)
const timeStep = ref(30)
const bookedReservations = ref<any[]>([])
const selectedRoomId = ref<string | undefined>(props.roomId)
const timelineRef = ref<HTMLElement | null>(null)
const contacts = ref<UserInfo[]>([])
const contactsLoading = ref(false)
/** 参会人员选择弹窗 */
const userSelectVisible = ref(false)
// 自动拉取的会议室列表（当外部未传入 rooms 时使用）
const fetchedRooms = ref<MeetingRoom[]>([])

// 拖拽状态
const dragAnchor = ref<string>('')
const isDragging = ref(false)
let movedDuringDrag = false

// 会议室列表：优先使用外部传入的 rooms，否则使用自动拉取的列表
const roomList = computed(() => props.rooms && props.rooms.length > 0 ? props.rooms : fetchedRooms.value)

const currentRoom = computed(() => {
  if (props.room) return props.room
  if (roomList.value && selectedRoomId.value) {
    return roomList.value.find(r => r.id === selectedRoomId.value) || null
  }
  return null
})

const form = reactive({
  subject: '',
  selectedDate: '',
  startMinute: '',
  endMinute: '',
  attendeeUserIds: [] as string[],
  remark: ''
})

const rules: FormRules = {
  subject: [{ required: true, message: '请输入会议主题', trigger: 'blur' }],
  startMinute: [
    {
      validator: (_rule, _value, callback) => {
        if (!form.startMinute || !form.endMinute) {
          callback(new Error('请选择完整的预约时间段'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

function toMinutes(t: string): number {
  const [h, m] = t.split(':').map(Number)
  return h * 60 + m
}
function toTime(min: number): string {
  const h = Math.floor(min / 60)
  const m = min % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

const rangeBounds = computed(() => {
  const startStr = currentRoom.value?.bookableStart || '08:00'
  const endStr = currentRoom.value?.bookableEnd || '20:00'
  return { start: toMinutes(startStr), end: toMinutes(endStr) }
})

const bookedRanges = computed(() => {
  return bookedReservations.value
    .filter(r => r.status !== 2)
    .map(r => {
      const rStart = r.startTime.substring(11, 16)
      const rEnd = r.endTime.substring(11, 16)
      return { start: toMinutes(rStart), end: toMinutes(rEnd), subject: r.subject || '已预约', startStr: rStart, endStr: rEnd, user: r.username || '' }
    })
})

// 时间轴槽位：每个槽位代表一个可选时刻
interface Slot { time: string; minute: number; booked: boolean; bookedSubject?: string; bookedUser?: string; bookedStart?: string; bookedEnd?: string }
const timeSlots = computed<Slot[]>(() => {
  if (!form.selectedDate || !currentRoom.value) return []
  const { start, end } = rangeBounds.value
  const list: Slot[] = []
  for (let m = start; m <= end; m += timeStep.value) {
    const time = toTime(m)
    const booked = bookedRanges.value.find(r => m >= r.start && m < r.end)
    list.push({
      time, minute: m,
      booked: !!booked,
      bookedSubject: booked?.subject,
      bookedUser: booked?.user,
      bookedStart: booked?.startStr,
      bookedEnd: booked?.endStr
    })
  }
  return list
})

// 时间轴为空时的引导提示：优先提醒选择会议室，其次提醒选择日期
const emptyHint = computed(() => {
  if (!currentRoom.value) return '请先选择会议室'
  if (!form.selectedDate) return '请先选择预约日期'
  return ''
})

// 判断是否过期
function isExpired(t: string): boolean {
  if (!form.selectedDate) return false
  const now = new Date()
  const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  if (form.selectedDate !== todayStr) return false
  return toMinutes(t) < now.getHours() * 60 + now.getMinutes()
}

function isTimeDisabled(t: string): boolean {
  return isExpired(t) || bookedRanges.value.some(r => {
    const ts = toMinutes(t)
    return ts >= r.start && ts < r.end
  })
}

function slotClass(s: Slot): Record<string, boolean> {
  const inSel = isSlotInSelection(s.time)
  return {
    booked: s.booked,
    expired: !s.booked && isExpired(s.time),
    'in-selection': inSel,
    'start-edge': isStartEdge(s.time),
    'end-edge': isEndEdge(s.time),
    disabled: !s.booked && isExpired(s.time)
  }
}

function isSlotInSelection(t: string): boolean {
  if (!form.startMinute || !form.endMinute) return false
  const tm = toMinutes(t)
  return tm >= toMinutes(form.startMinute) && tm < toMinutes(form.endMinute)
}
function isStartEdge(t: string): boolean {
  return !!form.startMinute && t === form.startMinute
}
function isEndEdge(t: string): boolean {
  // 结束时刻不在 timeSlots 的可选区间内时，高亮其前一个槽位作为结束边
  return !!form.endMinute && t === form.endMinute
}

// 当前时间指示线位置
const nowLinePct = computed(() => {
  if (!form.selectedDate || !timeSlots.value.length) return -1
  const now = new Date()
  const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  if (form.selectedDate !== todayStr) return -1
  const { start, end } = rangeBounds.value
  const nowMin = now.getHours() * 60 + now.getMinutes()
  if (nowMin < start || nowMin > end) return -1
  return ((nowMin - start) / (end - start)) * 100
})

// 时长
const durationMinutes = computed(() => {
  if (!form.startMinute || !form.endMinute) return 0
  return toMinutes(form.endMinute) - toMinutes(form.startMinute)
})
const isOverMaxDuration = computed(() => {
  const max = currentRoom.value?.maxDuration || 0
  return max > 0 && durationMinutes.value > max
})
const durationText = computed(() => {
  const d = durationMinutes.value
  if (d <= 0) return ''
  const h = Math.floor(d / 60)
  const m = d % 60
  return h > 0 ? `${h}小时${m > 0 ? m + '分钟' : ''}` : `${m}分钟`
})

// 拖拽：根据锚点和当前点计算合法区间，自动避开已约时段
function computeDragRange(anchor: string, current: string): { start: string; end: string } {
  const a = toMinutes(anchor)
  const c = toMinutes(current)
  if (a === c) return { start: anchor, end: '' }
  if (c > a) {
    let end = c
    for (const r of bookedRanges.value) {
      if (r.start > a && r.start < end) end = r.start
    }
    return end > a ? { start: anchor, end: toTime(end) } : { start: anchor, end: '' }
  } else {
    let start = c
    for (const r of bookedRanges.value) {
      if (r.end < a && r.end > start) start = r.end
    }
    return start < a ? { start: toTime(start), end: anchor } : { start: anchor, end: '' }
  }
}

function getTimeFromEvent(e: PointerEvent): string | null {
  const el = document.elementFromPoint(e.clientX, e.clientY) as HTMLElement | null
  if (!el) return null
  const cell = el.closest('[data-time]') as HTMLElement | null
  return cell?.dataset.time ?? null
}

function isBookedRangeStart(t: string): boolean {
  const tm = toMinutes(t)
  return bookedRanges.value.some(r => r.start === tm)
}

function onPointerDown(e: PointerEvent, t: string) {
  // 选结束时刻时，允许点击「已约区间起点」（结束时刻为预约开始瞬间，不构成冲突）
  // 例如已有 08:30-08:45，选 08:15-08:30 时可点击 08:30 作为结束
  const selectingEnd = !!form.startMinute && !form.endMinute
  const allowed = selectingEnd ? (!isTimeDisabled(t) || isBookedRangeStart(t)) : !isTimeDisabled(t)
  if (!allowed) return
  e.preventDefault()
  // 注意：这里不立即修改 form.startMinute，否则点击（pointerup 无移动）时
  // handleCellClick 会误判为"再次点击同一槽位"而取消选择。选区由 pointermove（拖拽）
  // 或 handleCellClick（点击）负责设置。
  dragAnchor.value = t
  isDragging.value = true
  movedDuringDrag = false
}

function onPointerMove(e: PointerEvent) {
  if (!isDragging.value) return
  const t = getTimeFromEvent(e)
  if (!t || t === dragAnchor.value) return
  // computeDragRange 会自动夹紧到已约/过期边界
  movedDuringDrag = true
  const range = computeDragRange(dragAnchor.value, t)
  form.startMinute = range.start
  form.endMinute = range.end
}

function onPointerUp() {
  if (!isDragging.value) return
  isDragging.value = false
  if (!movedDuringDrag) {
    // 视为点击：点击-点击模式
    handleCellClick(dragAnchor.value)
  } else if (form.endMinute && isOverMaxDuration.value) {
    ElMessage.warning(`单次预约最长 ${currentRoom.value?.maxDuration} 分钟`)
  }
  formRef.value?.validateField('startMinute').catch(() => false)
}

// 点击-点击回退模式
function handleCellClick(t: string) {
  if (!form.startMinute || (form.startMinute && form.endMinute)) {
    form.startMinute = t
    form.endMinute = ''
  } else if (!form.endMinute) {
    if (t === form.startMinute) {
      form.startMinute = ''
      form.endMinute = ''
    } else {
      const range = computeDragRange(form.startMinute, t)
      form.startMinute = range.start
      form.endMinute = range.end
      if (form.endMinute && isOverMaxDuration.value) {
        ElMessage.warning(`单次预约最长 ${currentRoom.value?.maxDuration} 分钟`)
      }
    }
  }
}

// 全局监听 pointerup，确保拖出时间轴也能结束
if (typeof window !== 'undefined') {
  window.addEventListener('pointerup', onPointerUp)
}
onBeforeUnmount(() => {
  if (typeof window !== 'undefined') window.removeEventListener('pointerup', onPointerUp)
})

function disableFutureDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const maxDate = new Date(today)
  maxDate.setDate(maxDate.getDate() + (currentRoom.value?.advanceDays || 7))
  return date < today || date > maxDate
}

function onDateChange(val: string) {
  if (val) {
    form.startMinute = ''
    form.endMinute = ''
    loadBookedReservations()
  }
}
function onRoomChange() {
  form.startMinute = ''
  form.endMinute = ''
  loadBookedReservations()
}

async function loadBookedReservations() {
  const roomId = props.roomId || selectedRoomId.value
  if (!roomId || !form.selectedDate) return
  try {
    const res = await listByRoomAndDate(roomId, form.selectedDate)
    bookedReservations.value = res.data || []
  } catch { /* */ }
  scrollToWorkHour()
}

async function loadContacts() {
  contactsLoading.value = true
  try {
    const res = await listContacts()
    contacts.value = res.data || []
  } catch { /* */ } finally {
    contactsLoading.value = false
  }
}

/** 已选参会人员完整信息（按选择顺序回显） */
const selectedAttendees = computed(() =>
  form.attendeeUserIds
    .map(id => contacts.value.find(u => u.id === id))
    .filter((u): u is UserInfo => !!u)
)

/** 移除单个已选参会人 */
function removeAttendee(id: string) {
  form.attendeeUserIds = form.attendeeUserIds.filter(i => i !== id)
}

/** 选人弹窗确认回调 */
function onAttendeesConfirm(ids: string[]) {
  form.attendeeUserIds = ids
}

// 默认滚动到 09:00 或当前时间
function scrollToWorkHour() {
  nextTick(() => {
    if (!timelineRef.value || !timeSlots.value.length) return
    const { start } = rangeBounds.value
    const now = new Date()
    const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    let targetMin = 9 * 60
    if (form.selectedDate === todayStr) {
      const nowMin = now.getHours() * 60 + now.getMinutes()
      if (nowMin > start) targetMin = nowMin
    }
    const ratio = (targetMin - start) / (rangeBounds.value.end - start)
    const el = timelineRef.value
    el.scrollTop = Math.max(0, ratio * el.scrollHeight - 40)
  })
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (isOverMaxDuration.value) {
    ElMessage.error(`单次预约最长 ${currentRoom.value?.maxDuration} 分钟`)
    return
  }
  const roomId = props.roomId || selectedRoomId.value
  if (!roomId) {
    ElMessage.error('请选择会议室')
    return
  }
  submitting.value = true
  try {
    const dateStr = form.selectedDate
    const res = await createReservation({
      roomId,
      subject: form.subject,
      startTime: `${dateStr} ${form.startMinute}:00`,
      endTime: `${dateStr} ${form.endMinute}:00`,
      attendeeUserIds: form.attendeeUserIds,
      remark: form.remark,
    })
    const code = res?.data
    const msg = currentRoom.value?.needApproval === 1
      ? (code ? `预约已提交，等待管理员审批，编号：${code}` : '预约已提交，等待管理员审批')
      : (code ? `预约成功，编号：${code}` : '预约成功')
    ElMessage.success(msg)
    visible.value = false
    emit('success')
  } catch { /* */ } finally {
    submitting.value = false
  }
}

function handleClose() {
  form.subject = ''
  form.selectedDate = ''
  form.startMinute = ''
  form.endMinute = ''
  form.attendeeUserIds = []
  form.remark = ''
  bookedReservations.value = []
  dragAnchor.value = ''
  isDragging.value = false
  userSelectVisible.value = false
  formRef.value?.clearValidate()
  if (!props.roomId) selectedRoomId.value = undefined
}

watch(visible, async (val) => {
  if (val) {
    formRef.value?.clearValidate()
    if (props.date) {
      form.selectedDate = props.date
      if (props.startTime) form.startMinute = props.startTime.substring(11, 16)
      if (props.endTime) form.endMinute = props.endTime.substring(11, 16)
    }
    // 未指定具体会议室且未外部传入 rooms 时，自动拉取可用会议室列表
    if (!props.room && !(props.rooms && props.rooms.length > 0) && !fetchedRooms.value.length) {
      try {
        const res = await listActiveRooms()
        fetchedRooms.value = res.data || []
      } catch {
        // 拉取失败不阻塞表单使用
      }
    }
    const roomId = props.roomId || selectedRoomId.value
    if (roomId && form.selectedDate) {
      await loadBookedReservations()
    } else {
      scrollToWorkHour()
    }
    // 懒加载通讯录人员列表（仅首次打开加载，用于已选参会人回显）
    if (!contacts.value.length) {
      loadContacts()
    }
  }
})

watch(() => props.roomId, (val) => { if (val) selectedRoomId.value = val })
</script>

<style scoped>
.dialog-form-item { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.dialog-form-item label { font-size: 13px; color: #606266; font-weight: 500; display: flex; align-items: center; justify-content: space-between; }
.attendee-count { font-size: 12px; color: var(--el-color-primary); font-weight: 400; }
.attendee-display { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; min-height: 34px; padding: 5px 10px; border: 1px solid #dcdfe6; border-radius: 6px; cursor: pointer; transition: border-color 0.2s; }
.attendee-display:hover { border-color: var(--el-color-primary); }
.attendee-placeholder { font-size: 13px; color: #a8abb2; }

.dialog-rules-tip { background: var(--el-color-primary-light-9, #ecf5ff); border-radius: 8px; padding: 10px 14px; margin-bottom: 16px; font-size: 13px; color: var(--primary); display: flex; align-items: center; gap: 6px; }

/* 表单标签统一上置，与其它输入项对齐 */
.booking-form :deep(.el-form-item) { margin-bottom: 16px; }
.booking-form :deep(.el-form-item__label) { font-size: 13px; color: #606266; font-weight: 500; padding: 0 0 6px 0; line-height: 1.4; }
.booking-form :deep(.el-form-item__content) { margin-left: 0 !important; line-height: normal; }

/* 时间选择器：el-form-item__content 为 flex 行布局，子项需显式 100% 才能与上方输入框右边对齐 */
.time-picker { width: 100%; border: 1px solid #dcdfe6; border-radius: 10px; overflow: hidden; background: var(--bg-card); box-sizing: border-box; }

.tp-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: var(--bg-page); border-bottom: 1px solid var(--border-light); }
.tp-summary { display: flex; align-items: center; gap: 10px; min-height: 26px; }
.tp-range { font-size: 15px; font-weight: 600; color: #303133; display: flex; align-items: center; gap: 6px; }
.tp-range .tp-arrow { color: #409eff; }
.tp-range .tp-arrow.muted { color: #c0c4cc; font-weight: 400; }
.tp-range.placeholder { color: #c0c4cc; font-weight: 400; font-size: 13px; }
.tp-dur { padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; background: #ecf5ff; color: #409eff; border: 1px solid #d9ecff; }
.tp-dur.over { background: #fef0f0; color: #f56c6c; border-color: #fbc4c4; }

/* 纵向时间轴 */
.tp-timeline { position: relative; max-height: 320px; overflow-y: auto; padding: 6px 0; user-select: none; touch-action: pan-y; }
.tp-timeline::-webkit-scrollbar { width: 6px; }
.tp-timeline::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 3px; }

.tp-slot { display: flex; align-items: stretch; height: 32px; cursor: pointer; position: relative; }
.tp-slot + .tp-slot { border-top: 1px solid #f2f4f7; }
/* 整点加重 */
.tp-slot[data-time$=":00"] { border-top-color: #e4e7ed; }
.tp-slot .tp-label { width: 54px; flex-shrink: 0; text-align: right; padding-right: 10px; font-size: 12px; color: #909399; line-height: 32px; font-variant-numeric: tabular-nums; }
.tp-slot[data-time$=":00"] .tp-label { color: #606266; font-weight: 600; }
.tp-cell { flex: 1; position: relative; display: flex; align-items: center; padding: 3px 8px 3px 0; }

/* 已约 */
.tp-booked { width: 100%; height: 22px; background: repeating-linear-gradient(45deg, #fef0f0, #fef0f0 6px, #fde2e2 6px, #fde2e2 12px); border-left: 3px solid #f56c6c; border-radius: 0 4px 4px 0; display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 0 8px; cursor: not-allowed; }
.tp-booked-text { flex: 1; font-size: 11px; color: #c45656; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tp-booked-user { flex-shrink: 0; max-width: 80px; font-size: 11px; color: #fff; background: #e6a23c; padding: 1px 6px; border-radius: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 已选区间 */
.tp-selected { width: 100%; height: 22px; background: linear-gradient(90deg, rgba(64,158,255,0.12), rgba(102,177,255,0.18)); border-left: 3px solid #409eff; display: flex; align-items: center; padding: 0 8px; }
.tp-selected-start { background: linear-gradient(90deg, #409eff, rgba(64,158,255,0.15)); }
.tp-selected-end { background: linear-gradient(270deg, #409eff, rgba(64,158,255,0.15)); border-left: none; border-right: 3px solid #409eff; justify-content: flex-end; }
.tp-edge-label { font-size: 11px; color: #fff; font-weight: 600; }

/* 悬浮 */
.tp-slot:not(.booked):not(.disabled):hover .tp-cell::before { content: ''; position: absolute; left: 0; right: 8px; top: 3px; bottom: 3px; background: rgba(64,158,255,0.08); border-left: 3px solid #a0cfff; border-radius: 0 4px 4px 0; pointer-events: none; }

.tp-slot.booked { cursor: not-allowed; }
.tp-slot.disabled { cursor: not-allowed; opacity: 0.45; }
.tp-slot.disabled .tp-label { text-decoration: line-through; color: #c0c4cc; }

/* 当前时间线 */
.tp-now-line { position: absolute; left: 54px; right: 0; height: 0; border-top: 1.5px dashed #f56c6c; z-index: 2; pointer-events: none; }
.tp-now-label { position: absolute; right: 4px; top: -8px; font-size: 10px; color: #fff; background: #f56c6c; padding: 0 4px; border-radius: 3px; }

.tp-empty { padding: 28px; text-align: center; color: #c0c4cc; font-size: 13px; }

.tp-legend { display: flex; align-items: center; gap: 12px; padding: 8px 14px; border-top: 1px solid #f0f2f5; background: #fafbfc; flex-wrap: wrap; }
.tp-legend .lg { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; color: #909399; }
.tp-legend .dot { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }
.tp-legend .dot.free { background: #fff; border: 1px solid #dcdfe6; }
.tp-legend .dot.busy { background: repeating-linear-gradient(45deg, #fef0f0, #fef0f0 2px, #fde2e2 2px, #fde2e2 4px); border-left: 3px solid #f56c6c; }
.tp-legend .dot.sel { background: rgba(64,158,255,0.18); border-left: 3px solid #409eff; }
.tp-hint { margin-left: auto; font-size: 11px; color: #c0c4cc; }
</style>
