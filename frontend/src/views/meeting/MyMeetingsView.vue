<template>
  <div class="page-view">
    <SearchBar @search="onFilterChange" @reset="resetQuery">
      <template #collapsed>
        <el-input v-model="query.keyword" placeholder="搜索会议主题" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
      </template>
      <template #expanded>
        <div class="search-item">
          <label>会议主题</label>
          <el-input v-model="query.subject" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
        </div>
        <div class="search-item">
          <label>状态</label>
          <el-select v-model="query.status" placeholder="全部" clearable @change="onFilterChange">
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
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
            <el-link type="primary" :underline="false" @click="router.push(`/reservation/my/${row.id}`)">{{ row.subject }}</el-link>
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
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(`/reservation/my/${row.id}`)">
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
import { useRouter } from 'vue-router'
import { Refresh, View } from '@element-plus/icons-vue'
import { listMyMeetings } from '@/api/reservation'
import SearchBar from '@/components/SearchBar.vue'
import TableCard from '@/components/TableCard.vue'
import { formatDate, formatTime } from '@/utils/datetime'
import type { Reservation } from '@/types/reservation'

const router = useRouter()

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
  status: undefined as number | undefined
})
const timeRange = ref<string[]>([])

function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已拒绝' }[s] || '未知' }
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
function resetQuery() {
  query.keyword = ''
  query.subject = ''
  query.startTime = ''
  query.endTime = ''
  query.status = undefined
  query.page = 1
  timeRange.value = []
  loadData()
}
async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.subject) params.subject = query.subject
    if (query.status != null) params.status = query.status
    if (query.startTime) params.startTime = query.startTime
    if (query.endTime) params.endTime = query.endTime
    const res = await listMyMeetings(params)
    tableData.value = res.data.records
    total.value = Number(res.data.total) || 0
  } catch { /* */ } finally { loading.value = false }
}
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.time-slot-cell { display: flex; flex-direction: column; gap: 2px; line-height: 1.4; }
.ts-date { font-size: 13px; color: var(--text-primary); }
.ts-range { font-size: 12px; color: var(--text-muted); }
</style>
