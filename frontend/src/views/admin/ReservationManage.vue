<template>
  <div class="reservation-manage">
    <div class="page-header"><h2>预约管理</h2></div>

    <FilterTable :filters="filterDefs" :filter-model="query" @search="loadData" @reset="resetQuery">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="roomName" label="会议室" width="120" />
        <el-table-column prop="subject" label="会议主题" width="140" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="人数" width="60" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column prop="remark" label="备注" width="110" show-overflow-tooltip />
        <el-table-column label="预约时段" min-width="180">
          <template #default="{ row }"><span>{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark" round size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="155" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" link size="small" @click="handleApprove(row.id)">通过</el-button>
              <el-popconfirm title="确定拒绝?" @confirm="handleReject(row.id)"><template #reference><el-button type="danger" link size="small">拒绝</el-button></template></el-popconfirm>
            </template>
            <template v-else-if="row.status === 1">
              <el-popconfirm title="确定取消?" @confirm="handleCancel(row.id)"><template #reference><el-button type="warning" link size="small">取消</el-button></template></el-popconfirm>
            </template>
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
import { listAllReservations, approveReservation, rejectReservation, cancelReservation } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const loading = ref(false); const tableData = ref<Reservation[]>([]); const total = ref(0)
const query = reactive<Record<string, any>>({ page: 1, size: 10, status: undefined, keyword: '' })

const filterDefs = [
  { prop: 'status', label: '预约状态', type: 'select' as const, options: [{ label: '待确认', value: 0 }, { label: '已确认', value: 1 }, { label: '已取消', value: 2 }] },
]

function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info' }
function formatTime(t: string) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function resetQuery() { query.status = undefined; query.keyword = ''; query.page = 1 }
async function loadData() { loading.value = true; try { const res = await listAllReservations(query); tableData.value = res.data.records; total.value = res.data.total } catch { /* */ } finally { loading.value = false } }
async function handleApprove(id: number) { try { await approveReservation(id); ElMessage.success('审批通过'); loadData() } catch { /* */ } }
async function handleReject(id: number) { try { await rejectReservation(id); ElMessage.success('已拒绝'); loadData() } catch { /* */ } }
async function handleCancel(id: number) { try { await cancelReservation(id); ElMessage.success('已取消'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.reservation-manage { display: flex; flex-direction: column; gap: 20px; }
.text-muted { color: #d1d5db; }
</style>
