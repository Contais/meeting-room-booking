<template>
  <div class="page-view">
    <div class="page-header">
      <el-button class="back-btn" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </el-button>
      <div v-if="reservation" class="header-actions">
        <el-button v-if="reservation.status !== 2" class="action-btn action-danger-outline" @click="handleCancel">
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Close } from '@element-plus/icons-vue'
import { getReservationDetail, cancelReservation } from '@/api/reservation'
import { formatDateTime } from '@/utils/datetime'
import type { Reservation } from '@/types/reservation'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reservation = ref<Reservation | null>(null)
const id = Number(route.params.id)

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

onMounted(loadDetail)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.back-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  border: 1px solid var(--border-light);
  background: var(--bg-card);
  color: var(--text-secondary);
  transition: all 0.25s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.back-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--bg-card);
  transform: translateX(-2px);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  height: 38px;
  padding: 0 20px;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.25s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  cursor: pointer;
}
.action-danger-outline {
  background: var(--bg-card);
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}
.action-danger-outline:hover {
  background: rgba(245, 108, 108, 0.06);
  border-color: #f56c6c;
  transform: translateY(-2px);
}

.detail-card {
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border-light);
  padding: 28px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
</style>
