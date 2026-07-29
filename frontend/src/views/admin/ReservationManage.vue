<template>
  <div class="page-view">
    <SearchBar :default-expanded="hasInitialQuery" @search="onFilterChange" @reset="resetQuery">
      <template #collapsed>
        <el-input v-model="query.keyword" placeholder="搜索预约编号 / 会议主题 / 联系电话" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
      </template>
      <template #expanded>
        <div class="search-item">
          <label>预约编号</label>
          <el-input v-model="query.reservationCode" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
        </div>
        <div class="search-item">
          <label>会议主题</label>
          <el-input v-model="query.subject" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
        </div>
        <div class="search-item">
          <label>预约人</label>
          <el-input v-model="query.username" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
        </div>
        <div class="search-item">
          <label>状态</label>
          <el-select v-model="query.status" placeholder="全部" clearable @change="onFilterChange">
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已拒绝" :value="3" />
          </el-select>
        </div>
        <div class="search-item is-wide">
          <label>预约时段</label>
          <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至"
            start-placeholder="开始时间" end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss" @change="onTimeRangeChange" />
        </div>
        <div class="search-item is-wide">
          <label>创建时间</label>
          <el-date-picker v-model="createTimeRange" type="datetimerange" range-separator="至"
            start-placeholder="开始时间" end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss" @change="onCreateTimeRangeChange" />
        </div>
      </template>
    </SearchBar>

    <TableCard :total="total" v-model:page="query.page" v-model:size="query.size" @size-change="onSizeChange" @current-change="loadData">
      <template #toolbar-left>
        <el-button class="btn-outline" @click="bookingDialogVisible = true"><el-icon><Plus /></el-icon>预约会议室</el-button>
      </template>
      <template #toolbar-right>
        <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
      </template>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="预约编号" width="170">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="goDetail(row.id)">{{ row.reservationCode }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="roomName" label="会议室" min-width="110" />
        <el-table-column prop="subject" label="会议主题" min-width="130" show-overflow-tooltip />
        <el-table-column prop="username" label="预约人" min-width="100" />
        <el-table-column prop="attendeeCount" label="人数" width="70" align="center" />
        <el-table-column label="预约时段" min-width="110">
          <template #default="{ row }">
            <div class="time-slot-cell">
              <div class="ts-date">{{ formatDate(row.startTime) }}</div>
              <div class="ts-range">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="statusType(row.status)" size="small" effect="light">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="创建时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-links">
              <el-button type="primary" link @click="goDetail(row.id)">
                <el-icon><View /></el-icon>详情
              </el-button>
              <template v-if="row.status === 0">
                <el-button type="success" link @click="handleApprove(row.id)">
                  <el-icon><Check /></el-icon>通过
                </el-button>
                <el-button type="danger" link @click="handleReject(row)">
                  <el-icon><Close /></el-icon>拒绝
                </el-button>
              </template>
              <el-button v-else-if="canCancel(row)" type="danger" link @click="handleCancel(row)">
                <el-icon><Close /></el-icon>取消
              </el-button>
              <el-button v-if="row.status === 2 || row.status === 3" type="danger" link @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </TableCard>

    <BookingDialog v-model="bookingDialogVisible" @success="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Check, Close, Refresh, View, Delete } from '@element-plus/icons-vue'
import { listAllReservations, approveReservation, rejectReservation, cancelReservation, adminDeleteReservation } from '@/api/reservation'
import SearchBar from '@/components/SearchBar.vue'
import TableCard from '@/components/TableCard.vue'
import BookingDialog from '@/components/BookingDialog.vue'
import { formatDateTime, formatDate, formatTime } from '@/utils/datetime'
import type { Reservation } from '@/types/reservation'

const route = useRoute()
const router = useRouter()

// 从首页统计跳转携带查询参数时自动展开搜索栏
const hasInitialQuery = Object.keys(route.query).length > 0
const bookingDialogVisible = ref(false)
const loading = ref(false)
const tableData = ref<Reservation[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  subject: '',
  username: '',
  startTime: '',
  endTime: '',
  reservationCode: '',
  createTimeStart: '',
  createTimeEnd: '',
  status: undefined as number | undefined
})
// 时间范围选择器使用本地数组，避免后端语义混乱
const timeRange = ref<string[]>([])
const createTimeRange = ref<string[]>([])

function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已拒绝' }[s] || '未知' }
function goDetail(id: number) {
  router.push({ path: `/admin/reservations/${id}`, query: { from: '/admin/reservations', fromTitle: '预约管理', dt: '预约详情' } })
}
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }[s] as any || 'info' }
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { query.page = 1; loadData() }, 300)
}
function onFilterChange() { query.page = 1; loadData() }
function onSizeChange() { query.page = 1; loadData() }
function onTimeRangeChange(val: string[] | null) {
  query.startTime = val && val.length === 2 ? val[0] : ''
  query.endTime = val && val.length === 2 ? val[1] : ''
  onFilterChange()
}
function onCreateTimeRangeChange(val: string[] | null) {
  query.createTimeStart = val && val.length === 2 ? val[0] : ''
  query.createTimeEnd = val && val.length === 2 ? val[1] : ''
  onFilterChange()
}

function resetQuery() {
  query.keyword = ''
  query.subject = ''
  query.username = ''
  query.startTime = ''
  query.endTime = ''
  query.reservationCode = ''
  query.createTimeStart = ''
  query.createTimeEnd = ''
  query.status = undefined
  query.page = 1
  timeRange.value = []
  createTimeRange.value = []
  loadData()
}
async function loadData() {
  loading.value = true
  try {
    // 关键字在展开态作为可见字段，无需按模式排除；只发送非空字段
    const params: Record<string, any> = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.subject) params.subject = query.subject
    if (query.username) params.username = query.username
    if (query.reservationCode) params.reservationCode = query.reservationCode
    if (query.status != null) params.status = query.status
    if (query.startTime) params.startTime = query.startTime
    if (query.endTime) params.endTime = query.endTime
    if (query.createTimeStart) params.createTimeStart = query.createTimeStart
    if (query.createTimeEnd) params.createTimeEnd = query.createTimeEnd
    const res = await listAllReservations(params)
    tableData.value = res.data.records
    total.value = Number(res.data.total) || 0
  } catch { /* */ } finally { loading.value = false }
}
async function handleApprove(id: number) { try { await approveReservation(id); ElMessage.success('通过'); loadData() } catch { /* */ } }
async function handleReject(row: Reservation) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因（可选）', `拒绝预约"${row.subject}"`, {
      confirmButtonText: '确定拒绝',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入拒绝原因，留空将默认为「管理员拒绝」',
      type: 'warning'
    })
    await rejectReservation(row.id, value || '')
    ElMessage.success('已拒绝')
    loadData()
  } catch { /* 用户取消 */ }
}
function canCancel(row: Reservation): boolean {
  // 已取消(2) / 已拒绝(3) 不可取消
  if (row.status === 2 || row.status === 3) return false
  return new Date(row.startTime) > new Date()
}
async function handleCancel(row: Reservation) {
  try {
    await ElMessageBox.confirm(`确定要取消预约"${row.subject}"吗？`, '提示', { type: 'warning' })
    await cancelReservation(row.id)
    ElMessage.success('已取消')
    loadData()
  } catch { /* */ }
}
async function handleDelete(row: Reservation) {
  try {
    await ElMessageBox.confirm(`确定要删除预约"${row.subject}"吗？删除后不可恢复。`, '确认删除', { type: 'warning' })
    await adminDeleteReservation(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* */ }
}
/** 从首页「待审批」统计跳转携带 status=0 时初始化筛选条件 */
function applyRouteQuery() {
  const q = route.query
  if (q.status != null) query.status = Number(q.status)
}

onMounted(() => {
  applyRouteQuery()
  loadData()
})
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.time-slot-cell { display: flex; flex-direction: column; gap: 2px; line-height: 1.4; }
.ts-date { font-size: 13px; color: var(--text-primary); }
.ts-range { font-size: 12px; color: var(--text-muted); }
</style>
