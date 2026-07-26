<template>
  <div class="page-view">
    <div class="page-header">
      <h2>预约详情</h2>
      <el-button @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
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
        <el-descriptions-item label="预约人">
          {{ reservation.username }}
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
        <el-descriptions-item label="更新时间">
          {{ formatDateTime(reservation.updateTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ reservation.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-empty v-else description="暂无数据" />
    </div>

    <div v-if="reservation" class="action-bar">
      <template v-if="reservation.status === 0">
        <el-button type="success" @click="handleApprove">
          <el-icon><Check /></el-icon>
          通过
        </el-button>
        <el-button type="danger" @click="handleReject">
          <el-icon><Close /></el-icon>
          拒绝
        </el-button>
      </template>
      <template v-else-if="reservation.status === 1">
        <el-button type="danger" @click="handleCancel">
          <el-icon><Close /></el-icon>
          取消预约
        </el-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Close } from '@element-plus/icons-vue'
import { getReservationDetail, approveReservation, rejectReservation, cancelReservation } from '@/api/reservation'
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

async function handleApprove() {
  try {
    await ElMessageBox.confirm('确定通过该预约？', '提示', { type: 'warning' })
    await approveReservation(id)
    ElMessage.success('已通过')
    loadDetail()
  } catch {
    // 用户取消
  }
}

async function handleReject() {
  try {
    await ElMessageBox.confirm('确定拒绝该预约？', '提示', { type: 'warning' })
    await rejectReservation(id)
    ElMessage.success('已拒绝')
    loadDetail()
  } catch {
    // 用户取消
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
  margin-bottom: 16px;
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.detail-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  padding: 24px;
}

.action-bar {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
