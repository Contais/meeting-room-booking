<template>
  <div class="page-view">
    <div v-loading="loading" class="detail-card">
      <div class="detail-card-header">
        <div class="header-left">
          <el-button class="back-btn" @click="router.back()">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回</span>
          </el-button>
          <span v-if="reservation" class="header-title">{{ reservation.subject }}</span>
        </div>
        <div v-if="reservation" class="header-actions">
          <el-button v-if="reservation.status === 2 || reservation.status === 3" type="danger" plain @click="handleDelete">
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </el-button>
          <el-button v-if="canCancel" type="danger" plain @click="handleCancel">
            <el-icon><Close /></el-icon>
            <span>取消预约</span>
          </el-button>
        </div>
      </div>

      <div class="detail-card-body">
        <el-descriptions v-if="reservation" :column="2" border>
          <el-descriptions-item label="预约编号">
            <el-tag type="primary">{{ reservation.reservationCode }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(reservation.status)">{{ statusText(reservation.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="会议主题">
            {{ reservation.subject }}
          </el-descriptions-item>
          <el-descriptions-item label="会议室">
            {{ reservation.roomName }}
          </el-descriptions-item>
          <el-descriptions-item label="参会人数">
            {{ reservation.attendeeCount }} 人
          </el-descriptions-item>
          <el-descriptions-item label="预约时段" :span="2">
            {{ formatDateTime(reservation.startTime) }} ~ {{ formatDateTime(reservation.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(reservation.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ reservation.remark || '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="reservation.status === 3" label="拒绝原因" :span="2">
            <span style="color: var(--el-color-danger)">{{ reservation.rejectReason || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 参会人员列表 -->
        <div v-if="reservation" class="attendee-section">
          <div class="section-title">参会人员</div>
          <el-table v-if="reservation.attendees && reservation.attendees.length" :data="reservation.attendees" size="small" border>
            <el-table-column label="姓名" min-width="120">
              <template #default="{ row }">{{ row.realName || row.username }}</template>
            </el-table-column>
            <el-table-column label="部门" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.departmentName || '未分配' }}</template>
            </el-table-column>
            <el-table-column label="参会状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="attendeeStatusType(row.status)" size="small">{{ attendeeStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无参会人员" :image-size="60" />
        </div>

        <el-empty v-else description="暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Close, Delete } from '@element-plus/icons-vue'
import { getReservationDetail, cancelReservation, deleteReservation } from '@/api/reservation'
import { formatDateTime } from '@/utils/datetime'
import type { Reservation } from '@/types/reservation'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reservation = ref<Reservation | null>(null)
const id = Number(route.params.id)

const canCancel = computed(() => {
  if (!reservation.value) return false
  // 已取消(2) / 已拒绝(3) 不可取消
  if (reservation.value.status === 2 || reservation.value.status === 3) return false
  return new Date(reservation.value.startTime) > new Date()
})

function statusText(s: number) {
  return { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已拒绝' }[s] || '未知'
}

function statusType(s: number) {
  return { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }[s] as any || 'info'
}

function attendeeStatusText(s: number) {
  return { 0: '待响应', 1: '已接受', 2: '已拒绝' }[s] || '未知'
}

function attendeeStatusType(s: number) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[s] as any || 'info'
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getReservationDetail(id)
    reservation.value = res.data
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定取消该预约？', '提示', { type: 'warning' })
    await cancelReservation(id)
    ElMessage.success('已取消')
    loadDetail()
  } catch {
    // 用户取消
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除该预约？删除后不可恢复。', '确认删除', { type: 'warning' })
    await deleteReservation(id)
    ElMessage.success('已删除')
    router.back()
  } catch {
    // 用户取消
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.attendee-section { margin-top: 20px; }
.section-title { font-size: 15px; font-weight: 600; color: var(--el-text-color-primary); margin-bottom: 12px; padding-left: 8px; border-left: 3px solid var(--el-color-primary); }
</style>
