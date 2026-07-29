<template>
  <div class="page-view">
    <SearchBar :default-expanded="hasInitialQuery" @search="onFilterChange" @reset="resetQuery">
      <template #collapsed>
        <el-input v-model="query.keyword" placeholder="搜索会议主题" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
      </template>
      <template #expanded>
        <div class="search-item">
          <label>会议主题</label>
          <el-input v-model="query.subject" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
        </div>
        <div class="search-item">
          <label>查阅状态</label>
          <el-select v-model="query.attendeeStatus" placeholder="全部" clearable @change="onFilterChange">
            <el-option label="待查阅" :value="0" />
            <el-option label="已查阅" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </div>
        <div class="search-item">
          <label>时间范围</label>
          <el-select v-model="timeScope" placeholder="全部" clearable @change="onTimeScopeChange">
            <el-option label="即将到来" value="upcoming" />
            <el-option label="今日" value="today" />
          </el-select>
        </div>
        <div class="search-item is-wide">
          <label>会议时段</label>
          <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至"
            start-placeholder="开始时间" end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss" @change="onTimeRangeChange" />
        </div>
      </template>
    </SearchBar>

    <TableCard :total="total" v-model:page="query.page" v-model:size="query.size" @size-change="onSizeChange" @current-change="loadData">
      <template #toolbar-right>
        <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
      </template>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="会议主题" min-width="150">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="goDetail(row.id)">{{ row.subject }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="roomName" label="会议室" min-width="110" />
        <el-table-column prop="username" label="组织者" min-width="100" />
        <el-table-column prop="attendeeCount" label="人数" width="70" align="center" />
        <el-table-column label="会议时段" min-width="160">
          <template #default="{ row }">
            <div class="time-slot-cell">
              <div class="ts-date">{{ formatDate(row.startTime) }}</div>
              <div class="ts-range">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="会议状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="meetingStatusType(row)" size="small" effect="light">{{ meetingStatusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="查阅状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="reviewStatusType(row.myAttendeeStatus)" size="small" effect="light">{{ reviewStatusText(row.myAttendeeStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.id)">
              <el-icon><View /></el-icon>详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </TableCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh, View } from '@element-plus/icons-vue'
import { listMyMeetings } from '@/api/reservation'
import SearchBar from '@/components/SearchBar.vue'
import TableCard from '@/components/TableCard.vue'
import { formatDate, formatTime } from '@/utils/datetime'
import type { Reservation } from '@/types/reservation'

const route = useRoute()
const router = useRouter()

// 从首页统计跳转携带查询参数时自动展开搜索栏
const hasInitialQuery = Object.keys(route.query).length > 0

const loading = ref(false)
const tableData = ref<Reservation[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  subject: '',
  startTime: '',
  endTime: '',
  attendeeStatus: undefined as number | undefined,
  upcoming: undefined as boolean | undefined
})
const timeRange = ref<string[]>([])
const timeScope = ref<string>('')

/** 根据时间判断会议状态：进行中 / 即将到来 / 已结束 */
function meetingStatusText(row: Reservation): string {
  const now = Date.now()
  const start = new Date(row.startTime).getTime()
  const end = new Date(row.endTime).getTime()
  if (now < start) return '即将到来'
  if (now >= start && now <= end) return '进行中'
  return '已结束'
}
function meetingStatusType(row: Reservation): any {
  const now = Date.now()
  const start = new Date(row.startTime).getTime()
  const end = new Date(row.endTime).getTime()
  if (now < start) return 'success'
  if (now >= start && now <= end) return 'warning'
  return 'info'
}

/** 查阅状态文本与样式 */
function reviewStatusText(s?: number): string {
  return { 0: '待查阅', 1: '已查阅', 2: '已拒绝' }[s ?? -1] || '未知'
}
function reviewStatusType(s?: number): any {
  return { 0: 'info', 1: 'success', 2: 'danger' }[s ?? -1] || 'info'
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
function goDetail(id: number) {
  router.push({ path: `/reservation/my/${id}`, query: { from: '/my-meetings', fromTitle: '我的会议', dt: '会议详情' } })
}
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { query.page = 1; loadData() }, 300)
}
function onFilterChange() { query.page = 1; loadData() }
function onSizeChange() { query.page = 1; loadData() }
function onTimeRangeChange(val: string[] | null) {
  query.startTime = val && val.length === 2 ? val[0] : ''
  query.endTime = val && val.length === 2 ? val[1] : ''
  // 手动选择时段时清除快捷范围
  timeScope.value = ''
  query.upcoming = undefined
  onFilterChange()
}
function onTimeScopeChange(val: string | null) {
  // 清除手动时段
  timeRange.value = []
  query.startTime = ''
  query.endTime = ''
  if (val === 'upcoming') {
    query.upcoming = true
  } else if (val === 'today') {
    query.upcoming = undefined
    const today = new Date()
    const pad = (n: number) => String(n).padStart(2, '0')
    const dayStart = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}T00:00:00`
    const dayEnd = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}T23:59:59`
    query.startTime = dayStart
    query.endTime = dayEnd
  } else {
    query.upcoming = undefined
  }
  onFilterChange()
}
function resetQuery() {
  query.keyword = ''
  query.subject = ''
  query.startTime = ''
  query.endTime = ''
  query.attendeeStatus = undefined
  query.upcoming = undefined
  query.page = 1
  timeRange.value = []
  timeScope.value = ''
  loadData()
}
async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.subject) params.subject = query.subject
    if (query.attendeeStatus != null) params.attendeeStatus = query.attendeeStatus
    if (query.upcoming) params.upcoming = true
    if (query.startTime) params.startTime = query.startTime
    if (query.endTime) params.endTime = query.endTime
    const res = await listMyMeetings(params)
    tableData.value = res.data.records
    total.value = Number(res.data.total) || 0
    // 同步查询条件到 URL（便于从首页统计跳转后保留筛选状态）
    router.replace({ query: buildRouteQuery() })
  } catch { /* */ } finally { loading.value = false }
}

function buildRouteQuery(): Record<string, string> {
  const q: Record<string, string> = {}
  if (query.keyword) q.keyword = query.keyword
  if (query.subject) q.subject = query.subject
  if (query.attendeeStatus != null) q.attendeeStatus = String(query.attendeeStatus)
  if (query.upcoming) q.upcoming = '1'
  if (query.startTime) q.startTime = query.startTime
  if (query.endTime) q.endTime = query.endTime
  if (timeScope.value) q.scope = timeScope.value
  return q
}

/** 从首页统计跳转携带的查询参数初始化筛选条件 */
function applyRouteQuery() {
  const q = route.query
  if (q.keyword) query.keyword = String(q.keyword)
  if (q.subject) query.subject = String(q.subject)
  if (q.attendeeStatus != null) query.attendeeStatus = Number(q.attendeeStatus)
  if (q.upcoming === '1') {
    query.upcoming = true
    timeScope.value = 'upcoming'
  }
  if (q.scope === 'today') {
    timeScope.value = 'today'
    const today = new Date()
    const pad = (n: number) => String(n).padStart(2, '0')
    query.startTime = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}T00:00:00`
    query.endTime = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}T23:59:59`
  } else if (q.startTime) {
    query.startTime = String(q.startTime)
  }
  if (q.endTime) query.endTime = String(q.endTime)
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
