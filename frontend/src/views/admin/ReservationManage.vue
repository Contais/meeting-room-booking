<template>
  <div class="reservation-manage">
    <div class="page-header">
      <h2>预约管理</h2>
    </div>

    <div class="filter-bar page-card">
      <el-select v-model="query.status" placeholder="预约状态" clearable style="width: 130px">
        <el-option label="待确认" :value="0" />
        <el-option label="已确认" :value="1" />
        <el-option label="已取消" :value="2" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="会议主题/联系电话" clearable style="width: 200px" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <div class="table-card page-card">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="roomName" label="会议室" width="120" />
        <el-table-column prop="subject" label="会议主题" width="150" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="人数" width="60" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column prop="remark" label="备注" width="120" show-overflow-tooltip />
        <el-table-column label="预约时段" min-width="190">
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
        <el-table-column prop="createTime" label="创建时间" width="155" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" link size="small" @click="handleApprove(row.id)">通过</el-button>
              <el-popconfirm title="确定拒绝该预约?" @confirm="handleReject(row.id)">
                <template #reference>
                  <el-button type="danger" link size="small">拒绝</el-button>
                </template>
              </el-popconfirm>
            </template>
            <template v-else-if="row.status === 1">
              <el-popconfirm title="确定取消该预约?" @confirm="handleCancel(row.id)">
                <template #reference>
                  <el-button type="warning" link size="small">取消预约</el-button>
                </template>
              </el-popconfirm>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
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
import { ElMessage } from 'element-plus'
import { listAllReservations, approveReservation, rejectReservation, cancelReservation } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const loading = ref(false)
const tableData = ref<Reservation[]>([])
const total = ref(0)
const query = reactive({
  page: 1, size: 10,
  status: undefined as number | undefined,
  keyword: '',
})

function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info' }
function formatTime(t: string) { return t ? t.replace('T', ' ').substring(0, 16) : '' }

function resetQuery() {
  query.status = undefined
  query.keyword = ''
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

async function handleApprove(id: number) {
  try { await approveReservation(id); ElMessage.success('审批通过'); loadData() } catch { /* */ }
}

async function handleReject(id: number) {
  try { await rejectReservation(id); ElMessage.success('已拒绝'); loadData() } catch { /* */ }
}

async function handleCancel(id: number) {
  try { await cancelReservation(id); ElMessage.success('已取消'); loadData() } catch { /* */ }
}

onMounted(loadData)
</script>

<style scoped>
.reservation-manage { display: flex; flex-direction: column; gap: 20px; }
.filter-bar { display: flex; align-items: center; gap: 12px; }
.time-range { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #374151; }
.time-sep { color: #9ca3af; }
.text-muted { color: #d1d5db; }
</style>
