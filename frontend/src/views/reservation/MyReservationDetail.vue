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

        <!-- 参会人员列表（按参会状态分组、可折叠） -->
        <div v-if="reservation" class="attendee-section">
          <div class="section-title">参会人员</div>
          <template v-if="reservation.attendees && reservation.attendees.length">
            <el-collapse v-model="activeGroups" class="attendee-collapse">
              <el-collapse-item
                v-for="g in attendeeGroups"
                :key="g.status"
                :name="g.status"
              >
                <template #title>
                  <div class="group-header">
                    <el-tag :type="attendeeStatusType(g.status)" size="small">{{ attendeeStatusText(g.status) }}</el-tag>
                    <span class="group-count">{{ g.items.length }} 人</span>
                  </div>
                </template>
                <el-table :data="g.items" size="small" border>
                  <el-table-column label="姓名" min-width="120">
                    <template #default="{ row }">{{ row.realName || row.username }}</template>
                  </el-table-column>
                  <el-table-column label="部门" min-width="140" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.departmentName || '未分配' }}</template>
                  </el-table-column>
                  <el-table-column label="联系方式" min-width="200">
                    <template #default="{ row }">
                      <span v-if="row.phone">{{ row.phone }}</span>
                      <span v-if="row.phone && row.email"> · </span>
                      <span v-if="row.email">{{ row.email }}</span>
                      <span v-if="!row.phone && !row.email" style="color: var(--el-text-color-secondary)">-</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="参会状态" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag :type="attendeeStatusType(row.status)" size="small">{{ attendeeStatusText(row.status) }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </el-collapse-item>
            </el-collapse>
          </template>
          <el-empty v-else description="暂无参会人员" :image-size="60" />
        </div>

        <el-empty v-else description="暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Close, Delete } from '@element-plus/icons-vue'
import { getMyReservationDetail, cancelReservation, deleteReservation } from '@/api/reservation'
import { respondInvitation } from '@/api/attendee'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/datetime'
import type { Reservation, Attendee } from '@/types/reservation'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const reservation = ref<Reservation | null>(null)
const id = computed(() => Number(route.params.id))
// 默认展开所有参会状态分组
const activeGroups = ref<number[]>([0, 1, 2])

const canCancel = computed(() => {
  if (!reservation.value) return false
  // 仅预约创建者或管理员可取消
  const isCreator = reservation.value.userId === userStore.userInfo?.id
  const isAdmin = userStore.isAdmin()
  if (!isCreator && !isAdmin) return false
  // 已取消(2) / 已拒绝(3) 不可取消
  if (reservation.value.status === 2 || reservation.value.status === 3) return false
  return new Date(reservation.value.startTime) > new Date()
})

// 参会人按状态分组（排序：待响应 → 已接受 → 已拒绝）
const attendeeGroups = computed(() => {
  const attendees = reservation.value?.attendees || []
  const groups: { status: number; items: Attendee[] }[] = [
    { status: 0, items: [] },
    { status: 1, items: [] },
    { status: 2, items: [] },
  ]
  for (const a of attendees) {
    const g = groups.find(g => g.status === a.status)
    if (g) g.items.push(a)
  }
  return groups.filter(g => g.items.length > 0)
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
    const res = await getMyReservationDetail(id.value)
    reservation.value = res.data
    // 进入详情页自动将参会状态从「待响应」改为「已接受」
    await autoAcceptInvitation()
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 进入详情页时，若当前用户是参会人且状态为「待响应(0)」，自动接受邀请
 * 已接受/已拒绝状态保持不变，避免覆盖用户已做出的明确选择
 */
async function autoAcceptInvitation() {
  const r = reservation.value
  if (!r || !r.attendees || !userStore.userInfo?.id) return
  const me = r.attendees.find(a => a.userId === userStore.userInfo!.id)
  if (!me || me.status !== 0) return
  try {
    await respondInvitation(r.id, 1)
    me.status = 1
  } catch {
    // 自动接受失败不影响页面浏览
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定取消该预约？', '提示', { type: 'warning' })
    await cancelReservation(id.value)
    ElMessage.success('已取消')
    loadDetail()
  } catch {
    // 用户取消
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除该预约？删除后不可恢复。', '确认删除', { type: 'warning' })
    await deleteReservation(id.value)
    ElMessage.success('已删除')
    router.back()
  } catch {
    // 用户取消
  }
}

// 站内信点击跳转相同路由不同 id 时，组件不会重新挂载，需监听 id 变化重新加载
watch(id, (newId, oldId) => {
  if (newId !== oldId) loadDetail()
})

onMounted(loadDetail)
</script>

<style scoped>
.attendee-section { margin-top: 20px; }
.section-title { font-size: 15px; font-weight: 600; color: var(--el-text-color-primary); margin-bottom: 12px; padding-left: 8px; border-left: 3px solid var(--el-color-primary); }
.attendee-collapse { border: none; }
.attendee-collapse :deep(.el-collapse-item__header) { padding-left: 4px; border-bottom: 1px solid var(--border-light); }
.attendee-collapse :deep(.el-collapse-item__content) { padding-bottom: 8px; }
.group-header { display: flex; align-items: center; gap: 8px; }
.group-count { font-size: 13px; color: var(--text-muted); }
</style>
