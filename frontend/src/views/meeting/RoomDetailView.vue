<template>
  <div class="page-view" v-loading="loading">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px">
        <el-button text @click="router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      </div>
    </div>

    <template v-if="room">
      <!-- 基础信息卡片 -->
      <div class="detail-card">
        <div class="card-header">
          <div class="room-icon"><el-icon :size="28"><OfficeBuilding /></el-icon></div>
          <div class="room-info">
            <h2>{{ room.name }}</h2>
            <p class="room-location"><el-icon><Location /></el-icon> {{ room.location || '暂无位置' }}</p>
          </div>
          <el-tag :type="room.status === 1 ? 'success' : 'info'" size="small" effect="light" round>
            {{ room.status === 1 ? '可用' : '禁用' }}
          </el-tag>
        </div>

        <div class="info-grid">
          <div class="info-item"><span class="info-label">容纳人数</span><span class="info-value">{{ room.capacity || '-' }} 人</span></div>
          <div class="info-item"><span class="info-label">设备设施</span><span class="info-value">{{ room.equipment || '暂无' }}</span></div>
          <div class="info-item"><span class="info-label">可预约时段</span><span class="info-value">{{ room.bookableStart || '08:00' }} ~ {{ room.bookableEnd || '20:00' }}</span></div>
          <div class="info-item"><span class="info-label">最大预约时长</span><span class="info-value">{{ room.maxDuration || 480 }} 分钟</span></div>
          <div class="info-item"><span class="info-label">提前预约</span><span class="info-value">最多 {{ room.advanceDays || 7 }} 天</span></div>
          <div class="info-item"><span class="info-label">审批模式</span><el-tag :type="room.needApproval === 1 ? 'warning' : 'success'" size="small" effect="light">{{ room.needApproval === 1 ? '需审批' : '免审批' }}</el-tag></div>
        </div>

        <div v-if="room.description" class="description">
          <h4>会议室描述</h4>
          <p>{{ room.description }}</p>
        </div>

        <div class="action-row">
          <el-button type="primary" :disabled="room.status !== 1" @click="showReserveDialog">
            <el-icon><Calendar /></el-icon> 立即预约
          </el-button>
        </div>
      </div>

      <!-- 预约日历 -->
      <div class="detail-card">
        <h4 class="section-title">预约日历</h4>
        <TimeSlotCalendar ref="calendarRef" :room-id="room.id" :bookable-start="room.bookableStart" :bookable-end="room.bookableEnd" @select="handleTimeSelect" />
      </div>
    </template>

    <!-- 预约弹窗 -->
    <el-dialog v-model="reserveDialogVisible" title="预约会议室" width="520px" destroy-on-close @close="onDialogClose">
      <div v-if="room" class="dialog-rules-tip">
        <el-icon><InfoFilled /></el-icon>
        可预约时段: {{ room.bookableStart || '08:00' }}~{{ room.bookableEnd || '20:00' }}，最长 {{ room.maxDuration || 480 }} 分钟，最多提前 {{ room.advanceDays || 7 }} 天
      </div>
      <el-form ref="reserveFormRef" :model="reserveForm" :rules="reserveRules" label-width="0">
        <div class="dialog-form-item"><label>会议室</label><el-input :value="room?.name" disabled /></div>
        <div class="dialog-form-item"><label>会议主题</label><el-input v-model="reserveForm.subject" placeholder="请输入会议主题" /></div>
        <div class="dialog-form-item">
          <label>预约时间 {{ selectedDateStr }}</label>
          <div class="time-selector">
            <div class="time-selector-header">
              <span v-if="reserveForm.startMinute && reserveForm.endMinute" class="time-range-text">{{ reserveForm.startMinute }} ~ {{ reserveForm.endMinute }}</span>
              <span v-else-if="reserveForm.startMinute" class="time-range-text">{{ reserveForm.startMinute }} ~ 请选择结束时间</span>
              <span v-else class="time-range-text placeholder">请选择开始时间</span>
              <el-radio-group v-model="timeStep" size="small">
                <el-radio-button :value="15">15分钟</el-radio-button>
                <el-radio-button :value="30">30分钟</el-radio-button>
              </el-radio-group>
            </div>
            <div class="time-options">
              <div v-for="t in timeOptions" :key="t" class="time-option"
                :class="{ active: t === reserveForm.startMinute || t === reserveForm.endMinute, 'in-range': isInRange(t) }"
                @click="handleTimeOptionClick(t)">
                {{ t }}
              </div>
            </div>
          </div>
        </div>
        <div class="dialog-form-item"><label>参会人数</label><el-input-number v-model="reserveForm.attendeeCount" :min="1" :max="room?.capacity || 100" style="width: 100%" /></div>
        <div class="dialog-form-item"><label>联系电话</label><el-input v-model="reserveForm.contactPhone" placeholder="请输入联系电话" /></div>
        <div class="dialog-form-item"><label>备注</label><el-input v-model="reserveForm.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" /></div>
      </el-form>
      <template #footer>
        <el-button @click="reserveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reserveLoading" @click="handleReserve">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, OfficeBuilding, Location, Calendar, InfoFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { getRoomById } from '@/api/meeting'
import { createReservation } from '@/api/reservation'
import TimeSlotCalendar from '@/components/TimeSlotCalendar.vue'
import type { MeetingRoom } from '@/types/meeting'

const route = useRoute()
const router = useRouter()
const room = ref<MeetingRoom | null>(null)
const loading = ref(false)
const calendarRef = ref<InstanceType<typeof TimeSlotCalendar>>()

// 时间段选择
const timeStep = ref(30)
const reserveDialogVisible = ref(false)
const reserveLoading = ref(false)
const reserveFormRef = ref<FormInstance>()
const reserveForm = reactive({
  subject: '', startTime: '', endTime: '',
  startMinute: '', endMinute: '',
  attendeeCount: 1, contactPhone: '', remark: ''
})
const reserveRules: FormRules = {
  subject: [{ required: true, message: '请输入会议主题', trigger: 'blur' }],
  startMinute: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endMinute: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

const selectedDateStr = computed(() => {
  if (!reserveForm.startTime) return '请选择日期'
  const p = reserveForm.startTime.substring(0, 10).split('-'); return p[0] + '年' + p[1] + '月' + p[2] + '日'
})

const timeOptions = computed(() => {
  if (!reserveForm.startTime) return []
  const startHour = parseInt((room.value?.bookableStart || '08:00').split(':')[0])
  const endHour = parseInt((room.value?.bookableEnd || '20:00').split(':')[0])
  const options: string[] = []
  for (let h = startHour; h <= endHour; h++) {
    for (let m = 0; m < 60; m += timeStep.value) {
      if (h === endHour && m > 0) break
      options.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`)
    }
  }
  return options
})

function isInRange(t: string) {
  if (!reserveForm.startMinute || !reserveForm.endMinute) return false
  return t > reserveForm.startMinute && t < reserveForm.endMinute
}

function handleTimeOptionClick(t: string) {
  if (!reserveForm.startMinute || (reserveForm.startMinute && reserveForm.endMinute)) {
    reserveForm.startMinute = t
    reserveForm.endMinute = ''
  } else {
    if (t <= reserveForm.startMinute) {
      reserveForm.endMinute = reserveForm.startMinute
      reserveForm.startMinute = t
    } else {
      reserveForm.endMinute = t
    }
  }
}

function handleTimeSelect(startTime: string, endTime: string) {
  const startMin = startTime.substring(11, 16)
  const endMin = endTime.substring(11, 16)
  Object.assign(reserveForm, {
    startTime, endTime,
    startMinute: startMin, endMinute: endMin,
    subject: '', attendeeCount: 1, contactPhone: '', remark: ''
  })
  reserveDialogVisible.value = true
}

function onDialogClose() {
  // 关闭弹窗时清空日历选中状态
  calendarRef.value?.clearSelection()
}

function showReserveDialog() {
  const dateStr = new Date().toISOString().substring(0, 10)
  Object.assign(reserveForm, {
    startTime: `${dateStr}T09:00:00`, endTime: '',
    startMinute: '09:00', endMinute: '10:00',
    subject: '', attendeeCount: 1, contactPhone: '', remark: ''
  })
  reserveDialogVisible.value = true
}

async function handleReserve() {
  const valid = await reserveFormRef.value?.validate().catch(() => false)
  if (!valid) return
  reserveLoading.value = true
  try {
    const dateStr = reserveForm.startTime.substring(0, 10)
    await createReservation({
      roomId: room.value!.id,
      subject: reserveForm.subject,
      startTime: `${dateStr}T${reserveForm.startMinute}:00`,
      endTime: `${dateStr}T${reserveForm.endMinute}:00`,
      attendeeCount: reserveForm.attendeeCount,
      contactPhone: reserveForm.contactPhone,
      remark: reserveForm.remark,
    })
    ElMessage.success(room.value?.needApproval === 1 ? '预约已提交，等待管理员审批' : '预约成功')
    reserveDialogVisible.value = false
  } catch { /* */ } finally { reserveLoading.value = false }
}

onMounted(async () => {
  loading.value = true
  try { const res = await getRoomById(Number(route.params.id)); room.value = res.data } catch { /* */ } finally { loading.value = false }
})
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.page-header { margin-bottom: 0; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }

.detail-card { background: #fff; border-radius: 12px; border: 1px solid #f0f0f0; padding: 24px; }
.card-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.room-icon { width: 48px; height: 48px; border-radius: 12px; background: linear-gradient(135deg, #667eea, #764ba2); display: flex; align-items: center; justify-content: center; color: #fff; }
.room-info h2 { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.room-location { font-size: 13px; color: #909399; margin: 4px 0 0 0; display: flex; align-items: center; gap: 4px; }

.info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; padding: 16px 0; border-top: 1px solid #f5f5f5; border-bottom: 1px solid #f5f5f5; }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 12px; color: #909399; }
.info-value { font-size: 14px; color: #303133; font-weight: 500; }

.description { margin-top: 16px; }
.description h4 { font-size: 14px; font-weight: 600; color: #303133; margin: 0 0 8px 0; }
.description p { font-size: 13px; color: #606266; line-height: 1.7; margin: 0; }
.action-row { margin-top: 20px; display: flex; justify-content: flex-end; }
.section-title { font-size: 15px; font-weight: 600; color: #303133; margin: 0 0 16px 0; }

.dialog-form-item { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.dialog-form-item label { font-size: 13px; color: #606266; font-weight: 500; }

.dialog-rules-tip { background: #ecf5ff; border-radius: 8px; padding: 10px 14px; margin-bottom: 16px; font-size: 13px; color: #409eff; display: flex; align-items: center; gap: 6px; }

/* 时间段选择器 */
.time-selector { border: 1px solid #dcdfe6; border-radius: 8px; overflow: hidden; }
.time-selector-header { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: #fafbfc; border-bottom: 1px solid #ebeef5; font-size: 13px; color: #606266; }
.time-options { display: grid; grid-template-columns: repeat(auto-fill, minmax(64px, 1fr)); gap: 6px; padding: 10px 12px; max-height: 160px; overflow-y: auto; }
.time-option { text-align: center; }
.time-option {
  padding: 6px 12px; border-radius: 6px; border: 1px solid #dcdfe6;
  font-size: 13px; color: #606266; cursor: pointer; transition: all 0.15s;
}
.time-option:hover { border-color: #409eff; color: #409eff; }
.time-option.active { background: #409eff; color: #fff; border-color: #409eff; }
.time-option.in-range { background: #ecf5ff; border-color: #b3d8ff; color: #409eff; }
.time-range-text { font-size: 14px; font-weight: 500; color: #303133; }
.time-range-text.placeholder { color: #c0c4cc; font-weight: 400; }

@media (max-width: 768px) { .info-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
