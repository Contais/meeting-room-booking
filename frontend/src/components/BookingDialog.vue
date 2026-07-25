<template>
  <el-dialog v-model="visible" title="预约会议室" width="520px" destroy-on-close @close="handleClose">
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
          <div class="time-selector-header">
            <span v-if="form.startMinute && form.endMinute" class="time-range-text">{{ form.startMinute }} ~ {{ form.endMinute }}</span>
            <span v-else-if="form.startMinute" class="time-range-text">{{ form.startMinute }} ~ 请选择结束时间</span>
            <span v-else class="time-range-text placeholder">请选择开始时间</span>
            <el-radio-group v-model="timeStep" size="small">
              <el-radio-button :value="15">15分钟</el-radio-button>
              <el-radio-button :value="30">30分钟</el-radio-button>
            </el-radio-group>
          </div>
          <div class="time-options">
            <div v-for="t in timeOptions" :key="t" class="time-option"
              :class="{ active: t === form.startMinute || t === form.endMinute, 'in-range': isInRange(t), disabled: isTimeDisabled(t) }"
              @click="handleTimeOptionClick(t)">
              {{ t }}
            </div>
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

const timeOptions = computed(() => {
  if (!form.selectedDate || !currentRoom.value) return []
  const startHour = parseInt((currentRoom.value.bookableStart || '08:00').split(':')[0])
  const endHour = parseInt((currentRoom.value.bookableEnd || '20:00').split(':')[0])
  const options: string[] = []
  for (let h = startHour; h <= endHour; h++) {
    for (let m = 0; m < 60; m += timeStep.value) {
      if (h === endHour && m > 0) break
      options.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`)
    }
  }
  return options
})

function isTimeDisabled(t: string): boolean {
  if (!form.selectedDate || !currentRoom.value) return true
  const now = new Date()
  const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  if (form.selectedDate === todayStr) {
    const [h, m] = t.split(':').map(Number)
    const slotMinutes = h * 60 + m
    const nowMinutes = now.getHours() * 60 + now.getMinutes()
    if (slotMinutes < nowMinutes) return true
  }
  for (const r of bookedReservations.value) {
    if (r.status === 2) continue
    const rStart = r.startTime.substring(11, 16)
    const rEnd = r.endTime.substring(11, 16)
    if (t >= rStart && t < rEnd) return true
  }
  return false
}

function isInRange(t: string) {
  if (!form.startMinute || !form.endMinute) return false
  return t > form.startMinute && t < form.endMinute
}

function handleTimeOptionClick(t: string) {
  if (isTimeDisabled(t)) return
  if (!form.startMinute) {
    form.startMinute = t
  } else if (!form.endMinute) {
    if (t <= form.startMinute) {
      form.endMinute = form.startMinute
      form.startMinute = t
    } else {
      form.endMinute = t
    }
  } else {
    form.startMinute = t
    form.endMinute = ''
  }
  formRef.value?.validateField('startMinute').catch(() => false)
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
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
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
</script>

<style scoped>
.dialog-form-item { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.dialog-form-item label { font-size: 13px; color: #606266; font-weight: 500; }

.dialog-rules-tip { background: #ecf5ff; border-radius: 8px; padding: 10px 14px; margin-bottom: 16px; font-size: 13px; color: #409eff; display: flex; align-items: center; gap: 6px; }

/* 时间段选择器 */
.time-selector { border: 1px solid #dcdfe6; border-radius: 8px; overflow: hidden; }
.time-selector-header { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: #fafbfc; border-bottom: 1px solid #ebeef5; font-size: 13px; color: #606266; }
.time-options { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; padding: 10px 12px; max-height: 140px; overflow-y: auto; }
.time-option { text-align: center; }
.time-option {
  padding: 6px 12px; border-radius: 6px; border: 1px solid #dcdfe6;
  font-size: 13px; color: #606266; cursor: pointer; transition: all 0.15s;
}
.time-option:hover:not(.disabled) { border-color: #409eff; color: #409eff; }
.time-option.active { background: #409eff; color: #fff; border-color: #409eff; }
.time-option.in-range { background: #ecf5ff; border-color: #b3d8ff; color: #409eff; }
.time-option.disabled { background: #f5f5f5; color: #c0c4cc; border-color: #ebeef5; cursor: not-allowed; }
.time-range-text { font-size: 14px; font-weight: 500; color: #303133; }
.time-range-text.placeholder { color: #c0c4cc; font-weight: 400; }
</style>
