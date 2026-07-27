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
          <template v-if="reservation.status === 0">
            <el-button type="danger" plain @click="handleReject">
              <el-icon><Close /></el-icon>
              <span>拒绝</span>
            </el-button>
            <el-button type="primary" plain @click="handleApprove">
              <el-icon><Check /></el-icon>
              <span>通过</span>
            </el-button>
          </template>
          <template v-else-if="canCancel">
            <el-button type="danger" plain @click="handleCancel">
              <el-icon><Close /></el-icon>
              <span>取消预约</span>
            </el-button>
          </template>
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
          <el-descriptions-item v-if="reservation.status === 3" label="拒绝原因" :span="2">
            <span style="color: var(--el-color-danger)">{{ reservation.rejectReason || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-empty v-else description="暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Close, Delete } from '@element-plus/icons-vue'
import { getReservationDetail, approveReservation, rejectReservation, cancelReservation, adminDeleteReservation } from '@/api/reservation'
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
    const { value } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '拒绝预约', {
      confirmButtonText: '确定拒绝',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入拒绝原因，留空将默认为「管理员拒绝」',
      type: 'warning'
    })
    await rejectReservation(id, value || '')
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

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除该预约？删除后不可恢复。', '确认删除', { type: 'warning' })
    await adminDeleteReservation(id)
    ElMessage.success('已删除')
    router.back()
  } catch {
    // 用户取消
  }
}

onMounted(loadDetail)
</script>

<style scoped>
</style>
