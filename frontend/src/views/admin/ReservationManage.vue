<template>
  <div class="reservation-manage">
    <div class="page-header">
      <h2>预约管理</h2>
    </div>

    <div class="filter-bar page-card">
      <el-select v-model="query.status" placeholder="预约状态" clearable style="width: 130px">
        <el-option label="已确认" :value="1" />
        <el-option label="待确认" :value="0" />
        <el-option label="已取消" :value="2" />
      </el-select>
      <el-input-number v-model="query.roomId" :min="1" placeholder="会议室ID" controls-position="right" style="width: 140px" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <div class="table-card page-card">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="roomName" label="会议室" width="130" />
        <el-table-column prop="subject" label="会议主题" width="160" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="人数" width="70" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column label="预约时段" min-width="200">
          <template #default="{ row }">
            <div class="time-range">
              <span>{{ formatTime(row.startTime) }}</span>
              <span class="time-sep">~</span>
              <span>{{ formatTime(row.endTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark" round size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listAllReservations } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const loading = ref(false)
const tableData = ref<Reservation[]>([])
const total = ref(0)
const query = reactive({
  page: 1, size: 10,
  status: undefined as number | undefined,
  roomId: undefined as number | undefined,
})

function statusText(s: number) {
  return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知'
}
function statusType(s: number) {
  return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info'
}
function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function resetQuery() {
  query.status = undefined
  query.roomId = undefined
  query.page = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await listAllReservations(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch { /* */ } finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.reservation-manage { display: flex; flex-direction: column; gap: 20px; }
.filter-bar { display: flex; align-items: center; gap: 12px; }
.time-range { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #374151; }
.time-sep { color: #9ca3af; }
</style>
