<template>
  <div class="page-view">
    <SearchBar @search="onFilterChange" @reset="resetQuery">
      <template #collapsed>
        <el-input v-model="query.keyword" placeholder="搜索预约编号 / 会议主题" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
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
          <label>状态</label>
          <el-select v-model="query.status" placeholder="全部" clearable @change="onFilterChange">
            <el-option label="已确认" :value="1" />
            <el-option label="待确认" :value="0" />
            <el-option label="已取消" :value="2" />
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

    <div class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left"></div>
        <div class="toolbar-right">
          <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" :header-cell-style="{ background: '#fafbfc', color: '#606266', fontWeight: 500 }">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="reservationCode" label="预约编号" width="170" />
        <el-table-column prop="roomName" label="会议室" min-width="120" />
        <el-table-column prop="subject" label="会议主题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="人数" width="70" align="center" />
        <el-table-column label="预约时段" min-width="160">
          <template #default="{ row }">
            <div class="time-slot-cell">
              <div class="ts-date">{{ formatDate(row.startTime) }}</div>
              <div class="ts-range">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="statusType(row.status)" size="small" effect="light">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="创建时间" width="160"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="row.status !== 2" type="danger" link size="small" @click="handleCancel(row.id)">取消</el-button>
              <span v-else style="color: #c0c4cc">-</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <span class="total-text">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="prev, pager, next, sizes, jumper" @size-change="onSizeChange" @current-change="loadData" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listMyReservations, cancelReservation } from '@/api/reservation'
import SearchBar from '@/components/SearchBar.vue'
import { formatDateTime, formatDate, formatTime } from '@/utils/datetime'
import type { Reservation } from '@/types/reservation'

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
  reservationCode: '',
  createTimeStart: '',
  createTimeEnd: '',
  status: undefined as number | undefined
})
const timeRange = ref<string[]>([])
const createTimeRange = ref<string[]>([])

function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info' }
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
    const params: Record<string, any> = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.subject) params.subject = query.subject
    if (query.reservationCode) params.reservationCode = query.reservationCode
    if (query.status != null) params.status = query.status
    if (query.startTime) params.startTime = query.startTime
    if (query.endTime) params.endTime = query.endTime
    if (query.createTimeStart) params.createTimeStart = query.createTimeStart
    if (query.createTimeEnd) params.createTimeEnd = query.createTimeEnd
    const res = await listMyReservations(params)
    tableData.value = res.data.records
    total.value = Number(res.data.total) || 0
  } catch { /* */ } finally { loading.value = false }
}
async function handleCancel(id: number) { try { await cancelReservation(id); ElMessage.success('已取消'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.table-card { background: #fff; border-radius: 12px; border: 1px solid #f0f0f0; overflow: hidden; }
.table-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f5f5f5; }
.toolbar-right { display: flex; gap: 4px; }
.action-buttons { display: flex; justify-content: center; gap: 4px; }
.pagination-wrap { display: flex; align-items: center; justify-content: flex-end; gap: 16px; padding: 14px 20px; border-top: 1px solid #f5f5f5; }
.total-text { font-size: 13px; color: #909399; }
.time-slot-cell { display: flex; flex-direction: column; gap: 2px; line-height: 1.4; }
.ts-date { font-size: 13px; color: #303133; }
.ts-range { font-size: 12px; color: #909399; }
</style>
