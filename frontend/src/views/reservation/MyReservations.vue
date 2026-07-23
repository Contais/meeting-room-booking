<template>
  <div class="my-reservations">
    <div class="page-header"><h2>我的预约</h2></div>

    <FilterTable :filters="filterDefs" :filter-model="query" @search="loadData" @reset="resetQuery">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="roomName" label="会议室" width="130" />
        <el-table-column prop="subject" label="会议主题" width="150" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="人数" width="70" />
        <el-table-column label="预约时段" min-width="190">
          <template #default="{ row }">
            <span>{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark" round size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm v-if="row.status !== 2" title="确定取消该预约?" @confirm="handleCancel(row.id)">
              <template #reference><el-button type="danger" link size="small">取消</el-button></template>
            </el-popconfirm>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next" style="margin-top: 16px; justify-content: flex-end" @size-change="loadData" @current-change="loadData" />
    </FilterTable>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import FilterTable from '@/components/FilterTable.vue'
import { listMyReservations, cancelReservation } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const loading = ref(false); const tableData = ref<Reservation[]>([]); const total = ref(0)
const query = reactive<Record<string, any>>({ page: 1, size: 10, status: undefined })

const filterDefs = [
  { prop: 'status', label: '预约状态', type: 'select' as const, options: [{ label: '已确认', value: 1 }, { label: '待确认', value: 0 }, { label: '已取消', value: 2 }] },
]

function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info' }
function formatTime(t: string) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function resetQuery() { query.status = undefined; query.page = 1 }
async function loadData() { loading.value = true; try { const res = await listMyReservations(query); tableData.value = res.data.records; total.value = res.data.total } catch { /* */ } finally { loading.value = false } }
async function handleCancel(id: number) { try { await cancelReservation(id); ElMessage.success('已取消'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.my-reservations { display: flex; flex-direction: column; gap: 20px; }
.text-muted { color: #d1d5db; }
</style>
