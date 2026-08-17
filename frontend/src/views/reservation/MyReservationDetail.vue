<template>
  <div class="page-view">
    <div v-loading="loading" class="detail-card">
      <div class="detail-card-header">
        <div class="header-left">
          <el-button class="back-btn" @click="router.back()">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回</span>
          </el-button>
          <span class="header-title">{{ pageTitle }}</span>
        </div>
        <div v-if="reservation" class="header-actions">
          <el-button v-if="reservation.status === 2 || reservation.status === 3" type="danger" plain @click="handleDelete">
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </el-button>
          <!-- 管理员审批操作：管理员模式或当前用户为管理员，且预约处于待确认状态 -->
          <template v-if="(isAdminMode || userStore.isAdmin()) && reservation.status === 0">
            <el-button type="danger" plain @click="handleReject">
              <el-icon><Close /></el-icon>
              <span>拒绝</span>
            </el-button>
            <el-button type="primary" plain @click="handleApprove">
              <el-icon><Check /></el-icon>
              <span>通过</span>
            </el-button>
          </template>
          <!-- 取消预约：管理员模式始终可用（状态允许时）；用户模式仅创建者或管理员可取消 -->
          <template v-else-if="canCancel">
            <el-button type="danger" plain @click="handleCancel">
              <el-icon><Close /></el-icon>
              <span>取消预约</span>
            </el-button>
          </template>
          <!-- 拒绝邀请：用户模式下，当前用户是参会人且尚未拒绝时可拒绝 -->
          <el-button v-if="canDecline" type="warning" plain @click="handleDecline">
            <el-icon><Close /></el-icon>
            <span>拒绝邀请</span>
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
          <!-- 管理员模式显示预约人 -->
          <el-descriptions-item v-if="isAdminMode" label="预约人">
            {{ reservation.username || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="参会人数">
            {{ reservation.attendeeCount }} 人
          </el-descriptions-item>
          <el-descriptions-item label="预约时段" :span="isAdminMode ? 2 : 2">
            {{ formatDateTime(reservation.startTime) }} ~ {{ formatDateTime(reservation.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(reservation.createTime) }}
          </el-descriptions-item>
          <!-- 管理员模式显示更新时间 -->
          <el-descriptions-item v-if="isAdminMode" label="更新时间">
            {{ formatDateTime(reservation.updateTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="isAdminMode ? 2 : 2">
            {{ reservation.remark || '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="reservation.status === 3" label="拒绝原因" :span="2">
            <span style="color: var(--el-color-danger)">{{ reservation.rejectReason || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 参会人员列表（按参会状态分组、可折叠） -->
        <div v-if="reservation" class="attendee-section">
          <div class="section-title-bar">
            <span class="section-title">参会人员</span>
            <el-button v-if="canInvite" type="primary" plain size="small" @click="openInviteDialog">
              <el-icon><Plus /></el-icon>
              <span>邀请参会人</span>
            </el-button>
          </div>
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
                  <el-table-column label="邀请时间" width="170" align="center">
                    <template #default="{ row }">{{ row.createTime ? formatDateTime(row.createTime) : '-' }}</template>
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

    <!-- 邀请参会人弹窗 -->
    <el-dialog
      v-model="inviteDialogVisible"
      title="邀请参会人"
      width="680px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-tabs v-model="activeInviteTab">
        <el-tab-pane label="按用户" name="user">
          <div class="invite-toolbar">
            <el-input
              v-model="userKeyword"
              placeholder="搜索用户名/真实姓名"
              clearable
              style="width: 260px"
              @keyup.enter="searchUsers"
              @clear="searchUsers"
            >
              <template #append>
                <el-button :icon="Search" @click="searchUsers" />
              </template>
            </el-input>
            <span class="invite-tip">已邀请的用户不可重复选择</span>
          </div>
          <el-table
            :data="userList"
            v-loading="userLoading"
            size="small"
            border
            max-height="340"
            row-key="id"
            @selection-change="onSelectionChange"
          >
            <el-table-column type="selection" width="42" :selectable="canSelectUser" />
            <el-table-column label="姓名" min-width="120">
              <template #default="{ row }">{{ row.realName || row.username }}</template>
            </el-table-column>
            <el-table-column label="用户名" min-width="120" prop="username" />
            <el-table-column label="部门" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.departmentName || '未分配' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag v-if="isExistingAttendee(row.id)" type="info" size="small">已邀请</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="按部门" name="department">
          <div class="invite-toolbar">
            <el-select
              v-model="selectedDepartmentId"
              placeholder="选择部门"
              style="width: 280px"
              filterable
              clearable
            >
              <el-option
                v-for="d in departmentList"
                :key="d.id"
                :label="d.name"
                :value="d.id"
              />
            </el-select>
            <span class="invite-tip">将邀请所选部门的所有成员（已邀请的会自动跳过）</span>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="inviteSubmitting" @click="confirmInvite">确定邀请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Close, Delete, Plus, Search } from '@element-plus/icons-vue'
import {
  getMyReservationDetail,
  getReservationDetail,
  approveReservation,
  rejectReservation,
  cancelReservation,
  deleteReservation,
  adminDeleteReservation,
} from '@/api/reservation'
import { respondInvitation, inviteAttendees, inviteDepartment } from '@/api/attendee'
import { listContacts } from '@/api/user'
import { listDepartments } from '@/api/department'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/datetime'
import type { Reservation, Attendee } from '@/types/reservation'
import type { UserInfo } from '@/types/user'
import type { Department } from '@/types/department'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const reservation = ref<Reservation | null>(null)
const id = computed(() => String(route.params.id))
// 默认展开所有参会状态分组
const activeGroups = ref<number[]>([0, 1, 2])

// 管理员模式：路由路径以 /admin/ 开头
const isAdminMode = computed(() => route.path.startsWith('/admin/'))
// 详情页标题：优先使用来源页传入的页面类型（预约详情 / 会议详情）
const pageTitle = computed(() => (route.query.dt as string) || '预约详情')

const canCancel = computed(() => {
  if (!reservation.value) return false
  // 已取消(2) / 已拒绝(3) 不可取消
  if (reservation.value.status === 2 || reservation.value.status === 3) return false
  // 管理员模式始终可取消（状态允许时）
  if (isAdminMode.value) return new Date(reservation.value.startTime) > new Date()
  // 用户模式：仅预约创建者或管理员可取消
  const isCreator = reservation.value.userId === userStore.userInfo?.id
  const isAdmin = userStore.isAdmin()
  if (!isCreator && !isAdmin) return false
  return new Date(reservation.value.startTime) > new Date()
})

/**
 * 拒绝邀请条件（用户模式）：
 * - 非管理员模式
 * - 预约已确认
 * - 当前用户是参会人但不是创建者
 * - 用户查阅状态不是「已拒绝」
 * - 会议尚未结束
 */
const canDecline = computed(() => {
  if (isAdminMode.value || !reservation.value) return false
  if (reservation.value.status !== 1) return false
  if (new Date(reservation.value.endTime) <= new Date()) return false
  const isCreator = reservation.value.userId === userStore.userInfo?.id
  if (isCreator) return false
  const me = reservation.value.attendees?.find(a => a.userId === userStore.userInfo?.id)
  return me != null && me.status !== 2
})

/**
 * 邀请参会人条件（用户模式）：
 * - 非管理员模式
 * - 当前用户是预约创建者
 * - 预约状态为待确认(0) 或 已确认(1)
 * - 会议尚未结束
 */
const canInvite = computed(() => {
  if (isAdminMode.value || !reservation.value) return false
  if (reservation.value.userId !== userStore.userInfo?.id) return false
  if (reservation.value.status !== 0 && reservation.value.status !== 1) return false
  return new Date(reservation.value.endTime) > new Date()
})

// 参会人按状态分组（排序：待查阅 → 已查阅 → 已拒绝）
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
  return { 0: '待查阅', 1: '已查阅', 2: '已拒绝' }[s] || '未知'
}

function attendeeStatusType(s: number) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[s] as any || 'info'
}

async function loadDetail() {
  loading.value = true
  try {
    // 管理员模式调用管理员接口，用户模式调用用户接口
    const res = isAdminMode.value
      ? await getReservationDetail(id.value)
      : await getMyReservationDetail(id.value)
    reservation.value = res.data
    // 用户模式：进入详情页自动将参会状态从「待响应」改为「已接受」
    if (!isAdminMode.value) {
      await autoAcceptInvitation()
    }
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

async function handleApprove() {
  try {
    await ElMessageBox.confirm('确定通过该预约？', '提示', { type: 'warning' })
    await approveReservation(id.value)
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
    await rejectReservation(id.value, value || '')
    ElMessage.success('已拒绝')
    loadDetail()
  } catch {
    // 用户取消
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

async function handleDecline() {
  try {
    await ElMessageBox.confirm('确定拒绝该会议邀请？拒绝后可在详情页重新查阅。', '拒绝邀请', { type: 'warning' })
    await respondInvitation(id.value, 2)
    ElMessage.success('已拒绝')
    loadDetail()
  } catch {
    // 用户取消
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除该预约？删除后不可恢复。', '确认删除', { type: 'warning' })
    // 管理员模式调用管理员删除接口，用户模式调用用户删除接口
    if (isAdminMode.value) {
      await adminDeleteReservation(id.value)
    } else {
      await deleteReservation(id.value)
    }
    ElMessage.success('已删除')
    router.back()
  } catch {
    // 用户取消
  }
}

// ============ 邀请参会人 ============
const inviteDialogVisible = ref(false)
const activeInviteTab = ref<'user' | 'department'>('user')
const inviteSubmitting = ref(false)
const userKeyword = ref('')
const userList = ref<UserInfo[]>([])
const userLoading = ref(false)
const selectedUsers = ref<UserInfo[]>([])
const departmentList = ref<Department[]>([])
const selectedDepartmentId = ref<string | null>(null)

/** 已存在的参会人 + 预约创建者，均不可重复邀请 */
const existingAttendeeIds = computed(() => {
  const set = new Set<string>()
  if (reservation.value?.userId) set.add(reservation.value.userId)
  for (const a of reservation.value?.attendees || []) {
    set.add(a.userId)
  }
  return set
})

function isExistingAttendee(userId: string) {
  return existingAttendeeIds.value.has(userId)
}

function canSelectUser(row: UserInfo) {
  return !isExistingAttendee(row.id)
}

async function openInviteDialog() {
  inviteDialogVisible.value = true
  activeInviteTab.value = 'user'
  userKeyword.value = ''
  userList.value = []
  selectedUsers.value = []
  selectedDepartmentId.value = null
  // 默认拉一次用户列表（不带关键字，返回全部）
  await searchUsers()
  // 部门列表懒加载
  if (departmentList.value.length === 0) {
    try {
      const res = await listDepartments()
      departmentList.value = res.data || []
    } catch {
      // 部门加载失败不阻塞用户切到按部门 Tab
    }
  }
}

async function searchUsers() {
  userLoading.value = true
  try {
    const res = await listContacts({ keyword: userKeyword.value.trim() || undefined })
    userList.value = res.data || []
  } catch {
    ElMessage.error('查询用户失败')
  } finally {
    userLoading.value = false
  }
}

function onSelectionChange(rows: UserInfo[]) {
  selectedUsers.value = rows
}

async function confirmInvite() {
  if (!reservation.value) return
  if (activeInviteTab.value === 'user') {
    if (selectedUsers.value.length === 0) {
      ElMessage.warning('请选择要邀请的用户')
      return
    }
    inviteSubmitting.value = true
    try {
      const res = await inviteAttendees(
        reservation.value.id,
        selectedUsers.value.map(u => u.id)
      )
      ElMessage.success(`已成功邀请 ${res.data} 位参会人`)
      inviteDialogVisible.value = false
      loadDetail()
    } catch (error: any) {
      ElMessage.error(error?.message || '邀请失败')
    } finally {
      inviteSubmitting.value = false
    }
  } else {
    if (!selectedDepartmentId.value) {
      ElMessage.warning('请选择部门')
      return
    }
    inviteSubmitting.value = true
    try {
      const res = await inviteDepartment(reservation.value.id, selectedDepartmentId.value)
      ElMessage.success(`已成功邀请 ${res.data} 位部门成员`)
      inviteDialogVisible.value = false
      loadDetail()
    } catch (error: any) {
      ElMessage.error(error?.message || '邀请失败')
    } finally {
      inviteSubmitting.value = false
    }
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
.section-title-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.section-title { font-size: 15px; font-weight: 600; color: var(--el-text-color-primary); padding-left: 8px; border-left: 3px solid var(--el-color-primary); }
.attendee-collapse { border: none; }
.attendee-collapse :deep(.el-collapse-item__header) { padding-left: 4px; border-bottom: 1px solid var(--border-light); }
.attendee-collapse :deep(.el-collapse-item__content) { padding-bottom: 8px; }
.group-header { display: flex; align-items: center; gap: 8px; }
.group-count { font-size: 13px; color: var(--text-muted); }
.invite-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.invite-tip { font-size: 12px; color: var(--el-text-color-secondary); }
</style>
