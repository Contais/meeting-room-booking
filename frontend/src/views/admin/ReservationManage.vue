<template>
  <div class="page-view">
    <div class="page-header"><h2>预约管理</h2></div>
    <FilterBar :model="query" @search="loadData" @reset="resetQuery">
      <el-form-item label="状态"><el-select v-model="query.status" placeholder="请选择状态" clearable filterable><el-option label="待确认" :value="0" /><el-option label="已确认" :value="1" /><el-option label="已取消" :value="2" /></el-select></el-form-item>
    </FilterBar>
    <div class="table-card page-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="roomName" label="会议室" min-width="110" />
        <el-table-column prop="subject" label="主题" min-width="130" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="人数" width="60" align="center" />
        <el-table-column prop="contactPhone" label="电话" min-width="110" />
        <el-table-column label="时段" min-width="170"><template #default="{ row }">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }"><div class="action-btns">
            <template v-if="row.status === 0">
              <el-tooltip content="通过"><div class="action-btn success" @click="handleApprove(row.id)"><el-icon><Check /></el-icon></div></el-tooltip>
              <el-tooltip content="拒绝"><div class="action-btn danger" @click="handleReject(row.id)"><el-icon><Close /></el-icon></div></el-tooltip>
            </template>
            <template v-else-if="row.status === 1">
              <el-tooltip content="取消预约"><div class="action-btn danger" @click="handleCancel(row.id)"><el-icon><Close /></el-icon></div></el-tooltip>
            </template>
            <span v-else style="color:var(--text-muted)">-</span>
          </div></template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="loadData" @current-change="loadData" /></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import FilterBar from '@/components/FilterBar.vue'
import { listAllReservations, approveReservation, rejectReservation, cancelReservation } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const loading = ref(false); const tableData = ref<Reservation[]>([]); const total = ref(0)
const query = reactive({ page: 1, size: 10, status: undefined as number | undefined })
function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info' }
function formatTime(t: string) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function resetQuery() { query.status = undefined; query.page = 1; loadData() }
async function loadData() { loading.value = true; try { const res = await listAllReservations(query); tableData.value = res.data.records; total.value = res.data.total } catch { /* */ } finally { loading.value = false } }
async function handleApprove(id: number) { try { await approveReservation(id); ElMessage.success('通过'); loadData() } catch { /* */ } }
async function handleReject(id: number) { try { await rejectReservation(id); ElMessage.success('已拒绝'); loadData() } catch { /* */ } }
async function handleCancel(id: number) { try { await cancelReservation(id); ElMessage.success('已取消'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.table-footer { display: flex; justify-content: flex-end; padding: 14px 20px; border-top: 1px solid var(--border-light); }
</style>
