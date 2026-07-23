<template>
  <div class="room-detail" v-loading="loading">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px">
        <el-button text @click="router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <h2>{{ room?.name || '会议室详情' }}</h2>
      </div>
    </div>

    <div v-if="room" class="detail-layout">
      <div class="detail-main">
        <!-- 基础信息 -->
        <div class="detail-card page-card">
          <div class="detail-header">
            <div class="detail-icon"><el-icon :size="32"><OfficeBuilding /></el-icon></div>
            <div>
              <h3>{{ room.name }}</h3>
              <p class="location-text"><el-icon><Location /></el-icon> {{ room.location || '暂无位置信息' }}</p>
            </div>
            <div class="status-badge">
              <el-tag :type="room.status === 1 ? 'success' : 'info'" effect="dark" round size="large">
                {{ room.status === 1 ? '可用' : '禁用' }}
              </el-tag>
            </div>
          </div>
          <el-divider />
          <div class="info-grid">
            <div class="info-item">
              <div class="info-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)"><el-icon :size="18"><User /></el-icon></div>
              <div><span class="info-label">容纳人数</span><span class="info-value">{{ room.capacity }} 人</span></div>
            </div>
            <div class="info-item">
              <div class="info-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)"><el-icon :size="18"><Monitor /></el-icon></div>
              <div><span class="info-label">设备设施</span><span class="info-value">{{ room.equipment || '暂无' }}</span></div>
            </div>
          </div>
          <div v-if="room.description" class="description-section">
            <h4>会议室描述</h4>
            <p>{{ room.description }}</p>
          </div>
        </div>

        <!-- 使用规则 -->
        <div class="page-card" style="margin-top: 20px">
          <h4 style="margin: 0 0 16px 0; font-size: 15px; font-weight: 600">使用规则</h4>
          <div class="rules-grid">
            <div class="rule-item">
              <el-icon><Clock /></el-icon>
              <div>
                <span class="rule-label">可预约时段</span>
                <span class="rule-value">{{ room.bookableStart || '08:00' }} ~ {{ room.bookableEnd || '20:00' }}</span>
              </div>
            </div>
            <div class="rule-item">
              <el-icon><Timer /></el-icon>
              <div>
                <span class="rule-label">最大预约时长</span>
                <span class="rule-value">{{ room.maxDuration || 480 }} 分钟</span>
              </div>
            </div>
            <div class="rule-item">
              <el-icon><Calendar /></el-icon>
              <div>
                <span class="rule-label">提前预约</span>
                <span class="rule-value">最多 {{ room.advanceDays || 7 }} 天</span>
              </div>
            </div>
            <div class="rule-item">
              <el-icon><Checked /></el-icon>
              <div>
                <span class="rule-label">审批模式</span>
                <el-tag :type="room.needApproval === 1 ? 'warning' : 'success'" size="small" effect="dark" round>
                  {{ room.needApproval === 1 ? '需审批' : '免审批' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 今日预约 -->
        <div class="page-card" style="margin-top: 20px">
          <h4 style="margin: 0 0 16px 0; font-size: 15px; font-weight: 600">今日预约</h4>
          <div v-if="todayReservations.length === 0" class="empty-text">暂无预约</div>
          <div v-for="r in todayReservations" :key="r.id" class="reservation-item">
            <div class="reservation-time"><el-icon><Clock /></el-icon> {{ formatTime(r.startTime) }} ~ {{ formatTime(r.endTime) }}</div>
            <div class="reservation-info">
              <span class="reservation-subject">{{ r.subject }}</span>
              <el-tag :type="statusType(r.status)" size="small" effect="dark" round>{{ statusText(r.status) }}</el-tag>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-sidebar">
        <div class="action-card page-card">
          <h4>预约此会议室</h4>
          <p class="action-desc">选择时间段，快速预约</p>
          <el-button type="primary" class="btn-gradient" size="large" :disabled="room.status !== 1" @click="showReserveDialog">
            <el-icon><Calendar /></el-icon> 立即预约
          </el-button>
        </div>
      </div>
    </div>

    <!-- 预约弹窗 -->
    <el-dialog v-model="reserveDialogVisible" title="预约会议室" width="520px" destroy-on-close>
      <div v-if="room" class="dialog-rules-tip">
        <el-icon><InfoFilled /></el-icon>
        可预约时段: {{ room.bookableStart || '08:00' }}~{{ room.bookableEnd || '20:00' }}，
        最长 {{ room.maxDuration || 480 }} 分钟，
        最多提前 {{ room.advanceDays || 7 }} 天
      </div>
      <el-form ref="reserveFormRef" :model="reserveForm" :rules="reserveRules" label-width="80px">
        <el-form-item label="会议室"><el-input :value="room?.name" disabled /></el-form-item>
        <el-form-item label="会议主题" prop="subject"><el-input v-model="reserveForm.subject" placeholder="请输入会议主题" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="reserveForm.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" :disabled-date="disablePastDate" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="reserveForm.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" :disabled-date="disablePastDate" />
        </el-form-item>
        <el-form-item label="参会人数">
          <el-input-number v-model="reserveForm.attendeeCount" :min="1" :max="room?.capacity || 100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="联系电话"><el-input v-model="reserveForm.contactPhone" placeholder="请输入联系电话" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="reserveForm.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reserveDialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-gradient" :loading="reserveLoading" @click="handleReserve">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, OfficeBuilding, User, Monitor, Location, Calendar, Clock, Timer, Checked, InfoFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { getRoomById } from '@/api/meeting'
import { createReservation, listByRoomAndDate } from '@/api/reservation'
import type { MeetingRoom } from '@/types/meeting'
import type { Reservation } from '@/types/reservation'

const route = useRoute()
const router = useRouter()
const room = ref<MeetingRoom | null>(null)
const loading = ref(false)
const todayReservations = ref<Reservation[]>([])

const reserveDialogVisible = ref(false)
const reserveLoading = ref(false)
const reserveFormRef = ref<FormInstance>()
const reserveForm = reactive({ subject: '', startTime: '', endTime: '', attendeeCount: 1, contactPhone: '', remark: '' })
const reserveRules: FormRules = {
  subject: [{ required: true, message: '请输入会议主题', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info' }
function formatTime(t: string) { return t ? t.replace('T', ' ').substring(0, 16) : '' }

function disablePastDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

function showReserveDialog() {
  reserveForm.subject = ''
  reserveForm.startTime = ''
  reserveForm.endTime = ''
  reserveForm.attendeeCount = 1
  reserveForm.contactPhone = ''
  reserveForm.remark = ''
  reserveDialogVisible.value = true
}

async function handleReserve() {
  const valid = await reserveFormRef.value?.validate().catch(() => false)
  if (!valid) return
  reserveLoading.value = true
  try {
    await createReservation({ roomId: room.value!.id, ...reserveForm })
    ElMessage.success(room.value?.needApproval === 1 ? '预约已提交，等待管理员审批' : '预约成功')
    reserveDialogVisible.value = false
    loadTodayReservations()
  } catch { /* */ } finally { reserveLoading.value = false }
}

async function loadTodayReservations() {
  if (!room.value) return
  const today = new Date().toISOString().substring(0, 10)
  try { const res = await listByRoomAndDate(room.value.id, today); todayReservations.value = res.data } catch { /* */ }
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getRoomById(Number(route.params.id))
    room.value = res.data
    loadTodayReservations()
  } catch { /* */ } finally { loading.value = false }
})
</script>

<style scoped>
.room-detail { display: flex; flex-direction: column; gap: 20px; }
.detail-layout { display: grid; grid-template-columns: 1fr 320px; gap: 20px; align-items: start; }
.detail-header { display: flex; align-items: center; gap: 16px; }
.detail-icon { width: 56px; height: 56px; border-radius: 14px; background: linear-gradient(135deg, #667eea, #764ba2); display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.detail-header h3 { font-size: 22px; font-weight: 600; color: #1a1a2e; margin: 0; }
.location-text { font-size: 14px; color: #9ca3af; margin: 4px 0 0 0; display: flex; align-items: center; gap: 4px; }
.status-badge { margin-left: auto; }
.info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
.info-item { display: flex; align-items: center; gap: 12px; }
.info-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.info-label { font-size: 12px; color: #9ca3af; display: block; }
.info-value { font-size: 15px; color: #1a1a2e; font-weight: 500; }
.description-section { margin-top: 20px; }
.description-section h4, .rules-section h4 { font-size: 14px; font-weight: 600; color: #374151; margin: 0 0 8px 0; }
.description-section p { font-size: 14px; color: #4b5563; line-height: 1.7; margin: 0; }

.rules-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.rule-item { display: flex; align-items: center; gap: 10px; padding: 12px; background: #f9fafb; border-radius: 10px; }
.rule-item .el-icon { font-size: 18px; color: #667eea; }
.rule-label { font-size: 12px; color: #9ca3af; display: block; }
.rule-value { font-size: 14px; color: #1a1a2e; font-weight: 500; }

.action-card h4 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0 0 8px 0; }
.action-desc { font-size: 13px; color: #9ca3af; margin: 0 0 16px 0; }
.empty-text { font-size: 13px; color: #9ca3af; text-align: center; padding: 20px 0; }
.reservation-item { padding: 10px 0; border-bottom: 1px solid #f3f4f6; }
.reservation-item:last-child { border-bottom: none; }
.reservation-time { font-size: 13px; color: #6b7280; display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.reservation-info { display: flex; align-items: center; justify-content: space-between; }
.reservation-subject { font-size: 14px; color: #1a1a2e; font-weight: 500; }
.dialog-rules-tip { background: #eff6ff; border-radius: 8px; padding: 10px 14px; margin-bottom: 16px; font-size: 13px; color: #3b82f6; display: flex; align-items: center; gap: 6px; }
@media (max-width: 768px) { .detail-layout { grid-template-columns: 1fr; } .rules-grid { grid-template-columns: 1fr; } }
</style>
