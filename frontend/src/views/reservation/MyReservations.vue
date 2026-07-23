<template>
  <div class="page-view">
    <div class="page-header"><h2>我的预约</h2></div>

    <FilterBar :model="query" @search="loadData" @reset="resetQuery">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable filterable style="width: 130px">
          <el-option label="已确认" :value="1" /><el-option label="待确认" :value="0" /><el-option label="已取消" :value="2" />
        </el-select>
      </el-form-item>
    </FilterBar>

    <div class="table-card page-card">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="roomName" label="会议室" min-width="120" />
        <el-table-column prop="subject" label="会议主题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="人数" width="65" align="center" />
        <el-table-column label="预约时段" min-width="180"><template #default="{ row }"><span class="time-text">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</span></template></el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="statusType(row.status)" effect="dark" round size="small">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="right">
          <template #default="{ row }">
            <el-popconfirm v-if="row.status !== 2" title="确定取消?" @confirm="handleCancel(row.id)"><template #reference><el-button type="danger" link size="small">取消</el-button></template></el-popconfirm>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import FilterBar from '@/components/FilterBar.vue'
import { listMyReservations, cancelReservation } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const loading = ref(false); const tableData = ref<Reservation[]>([]); const total = ref(0)
const query = reactive({ page: 1, size: 10, status: undefined as number | undefined })
function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info' }
function formatTime(t: string) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function resetQuery() { query.status = undefined; query.page = 1; loadData() }
async function loadData() { loading.value = true; try { const res = await listMyReservations(query); tableData.value = res.data.records; total.value = res.data.total } catch { /* */ } finally { loading.value = false } }
async function handleCancel(id: number) { try { await cancelReservation(id); ElMessage.success('已取消'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.table-footer { display: flex; justify-content: flex-end; padding: 14px 20px; border-top: 1px solid var(--border-light); }
.time-text { font-size: 13px; color: var(--text-primary); }
.text-muted { color: var(--text-muted); }
</style>
