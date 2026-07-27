<template>
  <div class="page-view">
    <div class="page-header">
      <div class="header-left">
        <el-button class="back-btn" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </el-button>
      </div>
      <div v-if="reservation" class="header-actions">
        <el-button v-if="reservation.status === 2" type="danger" plain @click="handleDelete">
          <el-icon><Delete /></el-icon>
          <span>删除</span>
        </el-button>
        <el-button v-if="canCancel" type="danger" plain @click="handleCancel">
          <el-icon><Close /></el-icon>
          <span>取消预约</span>
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="detail-card">
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
        <el-descriptions-item label="联系电话">
          {{ reservation.contactPhone || '-' }}
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
      </el-descriptions>

      <el-empty v-else description="暂无数据" />
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
  if (reservation.value.status === 2) return false
  return new Date(reservation.value.startTime) > new Date()
})

function statusText(s: number) {
  return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知'
}

function statusType(s: number) {
  return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info'
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
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.back-btn {
  /* 完全使用 Element Plus 默认样式 */
}

.header-actions {
  display: flex;
  gap: 2px;
}

.header-actions .el-button {
  font-weight: 500;
  transition: all 0.2s ease;
}
.header-actions .el-button:hover {
  transform: translateY(-1px);
}

.detail-card {
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  padding: 24px;
}
</style>
