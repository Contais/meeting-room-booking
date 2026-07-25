<template>
  <el-dialog v-model="visible" title="预约会议室" width="560px" destroy-on-close @close="handleClose">
    <div v-if="room" class="dialog-rules-tip">
      <el-icon><InfoFilled /></el-icon>
      可预约时段: {{ room.bookableStart || '08:00' }}~{{ room.bookableEnd || '20:00' }}，最长 {{ room.maxDuration || 480 }} 分钟，最多提前 {{ room.advanceDays || 7 }} 天
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
      <div v-if="!room" class="dialog-form-item">
        <label>会议室</label>
        <el-select v-model="selectedRoomId" placeholder="请选择会议室" style="width: 100%" @change="onRoomChange">
          <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
      </div>
      <div v-else class="dialog-form-item"><label>会议室</label><el-input :value="room?.name" disabled /></div>
      <div class="dialog-form-item"><label>会议主题</label><el-input v-model="form.subject" placeholder="请输入会议主题" /></div>
      <div class="dialog-form-item">
        <label>预约日期</label>
        <el-date-picker v-model="form.selectedDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" :disabled-date="disableFutureDate" @change="onDateChange" />
      </div>
      <el-form-item label="预约时间" prop="startMinute">
        <div class="time-selector">
          <!-- 顶部信息栏：已选时段 + 时长 + 步长切换 -->
          <div class="time-selector-header">
            <div class="time-summary">
              <template v-if="form.startMinute && form.endMinute">
                <span class="time-range-text">{{ form.startMinute }} <span class="arrow">→</span> {{ form.endMinute }}</span>
                <span class="duration-badge" :class="{ over: isOverMaxDuration }">{{ durationText }}</span>
              </template>
              <template v-else-if="form.startMinute">
                <span class="time-range-text">{{ form.startMinute }} <span class="arrow muted">→ 选择结束</span></span>
                <span class="duration-badge placeholder">{{ durationText }}</span>
              </template>
              <template v-else>
                <span class="time-range-text placeholder">请选择开始时间</span>
              </template>
            </div>
            <el-radio-group v-model="timeStep" size="small">
              <el-radio-button :value="15">15分钟</el-radio-button>
              <el-radio-button :value="30">30分钟</el-radio-button>
            </el-radio-group>
          </div>

          <!-- 可视化时间轴：显示当天已预约与已选区间 -->
          <div class="time-ruler" v-if="timeOptions.length">
            <div class="ruler-track">
              <div
                v-for="seg in rulerSegments"
                :key="seg.start"
                class="ruler-segment"
                :class="seg.cls"
                :style="{ left: seg.leftPct + '%', width: seg.widthPct + '%' }"
              >
                <el-tooltip v-if="seg.booked" :content="seg.tooltip" placement="top" :show-after="200">
                  <div class="ruler-booked-marker"></div>
                </el-tooltip>
              </div>
              <!-- 已选区间高亮 -->
              <div
                v-if="form.startMinute && form.endMinute"
                class="ruler-selected"
                :style="selectedRangeStyle"
              ></div>
              <!-- 悬浮预览区间 -->
              <div
                v-else-if="form.startMinute && hoverMinute"
                class="ruler-preview"
                :style="previewRangeStyle"
              ></div>
            </div>
            <div class="ruler-ticks">
              <span v-for="t in hourTicks" :key="t" class="ruler-tick" :style="{ left: tickPct(t) + '%' }">{{ t }}</span>
            </div>
          </div>

          <!-- 时段网格 -->
          <div class="time-options" v-if="timeOptions.length">
            <el-tooltip
              v-for="t in timeOptions"
              :key="t"
              :content="getSlotTooltip(t)"
              :disabled="!isTimeDisabled(t)"
              placement="top"
              :show-after="200"
            >
              <div
                class="time-option"
                :class="{
                  active: t === form.startMinute || t === form.endMinute,
                  'in-range': isInRange(t),
                  'preview-range': isInPreviewRange(t),
                  disabled: isTimeDisabled(t),
                  'start-edge': t === form.startMinute,
                  'end-edge': t === form.endMinute
                }"
                @click="handleTimeOptionClick(t)"
                @mouseenter="onCellHover(t)"
                @mouseleave="onCellLeave"
              >
                {{ t }}
              </div>
            </el-tooltip>
          </div>
          <div v-else class="time-empty">请先选择预约日期</div>

          <!-- 图例 -->
          <div class="time-legend">
            <span class="legend-item"><i class="dot available"></i>可选</span>
            <span class="legend-item"><i class="dot booked"></i>已预约</span>
            <span class="legend-item"><i class="dot selected"></i>已选</span>
            <span class="legend-item"><i class="dot disabled"></i>不可选</span>
          </div>
        </div>
      </el-form-item>
      <div class="dialog-form-item"><label>参会人数</label><el-input-number v-model="form.attendeeCount" :min="1" :max="currentRoom?.capacity || 100" style="width: 100%" /></div>
      <div class="dialog-form-item"><label>联系电话</label><el-input v-model="form.contactPhone" placeholder="请输入联系电话" @input="form.contactPhone = form.contactPhone.replace(/[^0-9]/g, '')" /></div>
      <div class="dialog-form-item"><label>备注</label><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" /></div>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { createReservation } from '@/api/reservation'
import { listByRoomAndDate } from '@/api/reservation'
import type { MeetingRoom } from '@/types/meeting'

const props = defineProps<{
  modelValue: boolean
  room?: MeetingRoom | null
  rooms?: MeetingRoom[]
  roomId?: number
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
const selectedRoomId = ref<number | undefined>(props.roomId)
const hoverMinute = ref<string>('')

const currentRoom = computed(() => {
  if (props.room) return props.room
  if (props.rooms && selectedRoomId.value) {
    return props.rooms.find(r => r.id === selectedRoomId.value) || null
  }
  return null
})

const form = reactive({
  subject: '',
  selectedDate: '',
  startMinute: '',
  endMinute: '',
  attendeeCount: 1,
  contactPhone: '',
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
  ],
  contactPhone: [{ pattern: /^[0-9]*$/, message: '联系电话只能输入数字', trigger: 'blur' }]
}

// 将 HH:mm 转为当天分钟数
function toMinutes(t: string): number {
  const [h, m] = t.split(':').map(Number)
  return h * 60 + m
}

function toTime(min: number): string {
  const h = Math.floor(min / 60)
  const m = min % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

// 可选时间范围（分钟）
const rangeBounds = computed(() => {
  const startStr = currentRoom.value?.bookableStart || '08:00'
  const endStr = currentRoom.value?.bookableEnd || '20:00'
  return { start: toMinutes(startStr), end: toMinutes(endStr) }
})

const timeOptions = computed(() => {
  if (!form.selectedDate || !currentRoom.value) return []
  const { start, end } = rangeBounds.value
  const options: string[] = []
  for (let m = start; m <= end; m += timeStep.value) {
    options.push(toTime(m))
  }
  return options
})

// 整点刻度（用于时间轴下方标签）
const hourTicks = computed(() => {
  if (!timeOptions.value.length) return []
  const { start, end } = rangeBounds.value
  const ticks: string[] = []
  for (let m = start; m <= end; m += 60) {
    ticks.push(toTime(m))
  }
  return ticks
})

function tickPct(t: string): number {
  const { start, end } = rangeBounds.value
  const total = end - start
  if (total <= 0) return 0
  return ((toMinutes(t) - start) / total) * 100
}

// 已预约区间映射（按分钟粒度标记）
const bookedRanges = computed(() => {
  return bookedReservations.value
    .filter(r => r.status !== 2)
    .map(r => {
      const rStart = r.startTime.substring(11, 16)
      const rEnd = r.endTime.substring(11, 16)
      return {
        start: toMinutes(rStart),
        end: toMinutes(rEnd),
        subject: r.subject || '已预约',
        startStr: rStart,
        endStr: rEnd
      }
    })
})

// 时间轴上的段（已预约段）
const rulerSegments = computed(() => {
  const { start, end } = rangeBounds.value
  const total = end - start
  if (total <= 0) return []
  return bookedRanges.value.map(r => {
    const left = Math.max(r.start, start)
    const right = Math.min(r.end, end)
    if (right <= left) return null
    return {
      start: r.startStr,
      leftPct: ((left - start) / total) * 100,
      widthPct: ((right - left) / total) * 100,
      cls: 'booked',
      booked: true,
      tooltip: `${r.subject} (${r.startStr}-${r.endStr})`
    }
  }).filter(Boolean) as any[]
})

// 已选区间样式
const selectedRangeStyle = computed(() => {
  if (!form.startMinute || !form.endMinute) return {}
  const { start, end } = rangeBounds.value
  const total = end - start
  if (total <= 0) return {}
  const s = toMinutes(form.startMinute)
  const e = toMinutes(form.endMinute)
  return {
    left: ((s - start) / total) * 100 + '%',
    width: ((e - s) / total) * 100 + '%'
  }
})

// 悬浮预览区间样式
const previewRangeStyle = computed(() => {
  if (!form.startMinute || !hoverMinute.value) return {}
  const { start, end } = rangeBounds.value
  const total = end - start
  if (total <= 0) return {}
  const s = toMinutes(form.startMinute)
  const h = toMinutes(hoverMinute.value)
  const left = Math.min(s, h)
  const right = Math.max(s, h)
  return {
    left: ((left - start) / total) * 100 + '%',
    width: ((right - left) / total) * 100 + '%'
  }
})

// 时长（分钟）
const durationMinutes = computed(() => {
  if (!form.startMinute || !form.endMinute) return 0
  return toMinutes(form.endMinute) - toMinutes(form.startMinute)
})

const isOverMaxDuration = computed(() => {
  const max = currentRoom.value?.maxDuration || 0
  return max > 0 && durationMinutes.value > max
})

const durationText = computed(() => {
  if (!form.startMinute) return ''
  if (!form.endMinute) {
    // 悬浮预览时长
    if (hoverMinute.value) {
      const d = Math.abs(toMinutes(hoverMinute.value) - toMinutes(form.startMinute))
      return d > 0 ? `${d} 分钟` : ''
    }
    return ''
  }
  const d = durationMinutes.value
  if (d <= 0) return ''
  const h = Math.floor(d / 60)
  const m = d % 60
  return h > 0 ? `${h}小时${m > 0 ? m + '分钟' : ''}` : `${m}分钟`
})

function isTimeDisabled(t: string): boolean {
  if (!form.selectedDate || !currentRoom.value) return true
  const now = new Date()
  const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  if (form.selectedDate === todayStr) {
    const slotMinutes = toMinutes(t)
    const nowMinutes = now.getHours() * 60 + now.getMinutes()
    if (slotMinutes < nowMinutes) return true
  }
  for (const r of bookedReservations.value) {
    if (r.status === 2) continue
    const rStart = r.startTime.substring(11, 16)
    const rEnd = r.endTime.substring(11, 16)
    // 已预约区间 [rStart, rEnd) 内的刻度不可作为开始；结束刻度等于 rEnd 时可选
    if (t >= rStart && t < rEnd) return true
  }
  return false
}

function isInRange(t: string) {
  if (!form.startMinute || !form.endMinute) return false
  return t > form.startMinute && t < form.endMinute
}

function isInPreviewRange(t: string) {
  if (!form.startMinute || !hoverMinute.value || form.endMinute) return false
  const s = toMinutes(form.startMinute)
  const h = toMinutes(hoverMinute.value)
  const lo = Math.min(s, h)
  const hi = Math.max(s, h)
  const tm = toMinutes(t)
  return tm > lo && tm < hi
}

function getSlotTooltip(t: string): string {
  if (!isTimeDisabled(t)) return ''
  for (const r of bookedReservations.value) {
    if (r.status === 2) continue
    const rStart = r.startTime.substring(11, 16)
    const rEnd = r.endTime.substring(11, 16)
    if (t >= rStart && t < rEnd) {
      return `${r.subject || '已预约'} ${rStart}-${rEnd}`
    }
  }
  return '该时段不可选'
}

function onCellHover(t: string) {
  if (form.startMinute && !form.endMinute && !isTimeDisabled(t)) {
    hoverMinute.value = t
  }
}

function onCellLeave() {
  hoverMinute.value = ''
}

function handleTimeOptionClick(t: string) {
  if (isTimeDisabled(t)) return
  if (!form.startMinute) {
    form.startMinute = t
    hoverMinute.value = ''
  } else if (!form.endMinute) {
    if (t === form.startMinute) {
      // 再次点击同一刻度：取消选择
      form.startMinute = ''
      hoverMinute.value = ''
    } else if (t < form.startMinute) {
      form.endMinute = form.startMinute
      form.startMinute = t
      hoverMinute.value = ''
    } else {
      // 校验中间是否跨越已预约时段
      if (hasBookedGap(form.startMinute, t)) {
        ElMessage.warning('所选区间包含已预约时段，请重新选择')
        return
      }
      form.endMinute = t
      hoverMinute.value = ''
    }
  } else {
    // 已有完整区间，重新开始
    form.startMinute = t
    form.endMinute = ''
    hoverMinute.value = ''
  }
  // 超长提示
  if (form.startMinute && form.endMinute && isOverMaxDuration.value) {
    ElMessage.warning(`单次预约最长 ${currentRoom.value?.maxDuration} 分钟`)
  }
  formRef.value?.validateField('startMinute').catch(() => false)
}

// 检查 [start, end] 之间是否跨越已预约时段
function hasBookedGap(start: string, end: string): boolean {
  const s = toMinutes(start)
  const e = toMinutes(end)
  for (const r of bookedRanges.value) {
    if (r.start < e && r.end > s) return true
  }
  return false
}

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
    hoverMinute.value = ''
    loadBookedReservations()
  }
}

function onRoomChange() {
  form.startMinute = ''
  form.endMinute = ''
  hoverMinute.value = ''
  loadBookedReservations()
}

async function loadBookedReservations() {
  const roomId = props.roomId || selectedRoomId.value
  if (!roomId || !form.selectedDate) return
  try {
    const res = await listByRoomAndDate(roomId, form.selectedDate)
    bookedReservations.value = res.data || []
  } catch { /* */ }
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
    await createReservation({
      roomId,
      subject: form.subject,
      startTime: `${dateStr}T${form.startMinute}:00`,
      endTime: `${dateStr}T${form.endMinute}:00`,
      attendeeCount: form.attendeeCount,
      contactPhone: form.contactPhone,
      remark: form.remark,
    })
    ElMessage.success(currentRoom.value?.needApproval === 1 ? '预约已提交，等待管理员审批' : '预约成功')
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
  form.attendeeCount = 1
  form.contactPhone = ''
  form.remark = ''
  bookedReservations.value = []
  hoverMinute.value = ''
  formRef.value?.clearValidate()
  if (!props.roomId) {
    selectedRoomId.value = undefined
  }
}

// 监听弹窗打开，初始化数据
watch(visible, async (val) => {
  if (val) {
    formRef.value?.clearValidate()
    if (props.date) {
      form.selectedDate = props.date
      if (props.startTime) {
        form.startMinute = props.startTime.substring(11, 16)
      }
      if (props.endTime) {
        form.endMinute = props.endTime.substring(11, 16)
      }
    }
    const roomId = props.roomId || selectedRoomId.value
    if (roomId && form.selectedDate) {
      await loadBookedReservations()
    }
  }
})

// 监听 props.roomId 变化，同步 selectedRoomId
watch(() => props.roomId, (val) => {
  if (val) {
    selectedRoomId.value = val
  }
})
</script>

<style scoped>
.dialog-form-item { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.dialog-form-item label { font-size: 13px; color: #606266; font-weight: 500; }

.dialog-rules-tip { background: #ecf5ff; border-radius: 8px; padding: 10px 14px; margin-bottom: 16px; font-size: 13px; color: #409eff; display: flex; align-items: center; gap: 6px; }

/* 时间段选择器 */
.time-selector { border: 1px solid #dcdfe6; border-radius: 10px; overflow: hidden; background: #fff; }

/* 顶部信息栏 */
.time-selector-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: linear-gradient(180deg, #fafbfc 0%, #f5f7fa 100%); border-bottom: 1px solid #ebeef5; }
.time-summary { display: flex; align-items: center; gap: 10px; min-height: 28px; }
.time-range-text { font-size: 15px; font-weight: 600; color: #303133; display: flex; align-items: center; gap: 6px; }
.time-range-text .arrow { color: #409eff; font-size: 14px; }
.time-range-text .arrow.muted { color: #c0c4cc; font-weight: 400; }
.time-range-text.placeholder { color: #c0c4cc; font-weight: 400; font-size: 14px; }

.duration-badge { display: inline-flex; align-items: center; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; background: #ecf5ff; color: #409eff; border: 1px solid #d9ecff; transition: all 0.2s; }
.duration-badge.over { background: #fef0f0; color: #f56c6c; border-color: #fbc4c4; }
.duration-badge.placeholder { color: #909399; background: #f4f4f5; border-color: #e9e9eb; }

/* 可视化时间轴 */
.time-ruler { padding: 14px 14px 4px; background: #fff; }
.ruler-track { position: relative; height: 14px; background: #f0f2f5; border-radius: 7px; overflow: hidden; }
.ruler-segment { position: absolute; top: 0; bottom: 0; }
.ruler-booked-marker { width: 100%; height: 100%; background: repeating-linear-gradient(45deg, #f56c6c, #f56c6c 4px, #f78989 4px, #f78989 8px); border-radius: 4px; cursor: help; }
.ruler-selected { position: absolute; top: 0; bottom: 0; background: linear-gradient(90deg, #409eff, #66b1ff); border-radius: 4px; box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.4); transition: all 0.2s ease; }
.ruler-preview { position: absolute; top: 0; bottom: 0; background: rgba(64, 158, 255, 0.25); border: 1px dashed #409eff; border-radius: 4px; transition: all 0.1s ease; }
.ruler-ticks { position: relative; height: 18px; margin-top: 4px; }
.ruler-tick { position: absolute; transform: translateX(-50%); font-size: 11px; color: #909399; }

/* 时段网格 */
.time-options { display: grid; grid-template-columns: repeat(auto-fill, minmax(64px, 1fr)); gap: 6px; padding: 12px 14px; max-height: 180px; overflow-y: auto; }
.time-option { padding: 7px 0; border-radius: 8px; border: 1px solid #dcdfe6; background: #fff; font-size: 13px; color: #606266; cursor: pointer; transition: all 0.18s ease; text-align: center; user-select: none; }
.time-option:hover:not(.disabled) { border-color: #409eff; color: #409eff; transform: translateY(-1px); box-shadow: 0 2px 6px rgba(64, 158, 255, 0.15); }
.time-option.active { background: linear-gradient(135deg, #409eff, #66b1ff); color: #fff; border-color: #409eff; font-weight: 600; box-shadow: 0 2px 8px rgba(64, 158, 255, 0.35); }
.time-option.in-range { background: #ecf5ff; border-color: #b3d8ff; color: #409eff; }
.time-option.preview-range { background: #f0f7ff; border-color: #c6e2ff; color: #79bbff; border-style: dashed; }
.time-option.disabled { background: #f5f5f5; color: #c0c4cc; border-color: #ebeef5; cursor: not-allowed; }
.time-option.disabled:hover { transform: none; box-shadow: none; }

.time-empty { padding: 24px; text-align: center; color: #c0c4cc; font-size: 13px; }

/* 图例 */
.time-legend { display: flex; gap: 14px; padding: 8px 14px 12px; border-top: 1px solid #f0f2f5; background: #fafbfc; flex-wrap: wrap; }
.legend-item { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; color: #909399; }
.legend-item .dot { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }
.legend-item .dot.available { background: #fff; border: 1px solid #dcdfe6; }
.legend-item .dot.booked { background: repeating-linear-gradient(45deg, #f56c6c, #f56c6c 2px, #f78989 2px, #f78989 4px); }
.legend-item .dot.selected { background: #409eff; }
.legend-item .dot.disabled { background: #f5f5f5; border: 1px solid #ebeef5; }
</style>
