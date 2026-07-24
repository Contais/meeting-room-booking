<template>
  <div class="page-view">
    <div class="page-header"><h2>预约管理</h2></div>
    <div class="search-bar">
      <div class="search-fields">
        <div class="search-item"><el-input v-model="query.keyword" placeholder="请输入会议主题" clearable /></div>
        <template v-if="expanded">
          <div class="search-item"><label>联系电话</label><el-input v-model="query.contactPhone" placeholder="请输入联系电话" clearable /></div>
          <div class="search-item"><label>状态</label><el-select v-model="query.status" placeholder="请选择" clearable><el-option label="待确认" :value="0" /><el-option label="已确认" :value="1" /><el-option label="已取消" :value="2" /></el-select></div>
        </template>
      </div>
      <div class="search-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button link type="primary" @click="expanded = !expanded">{{ expanded ? '收起' : '展开' }} <el-icon><ArrowDown v-if="!expanded" /><ArrowUp v-else /></el-icon></el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left"></div>
        <div class="toolbar-right">
          <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" :header-cell-style="{ background: '#fafbfc', color: '#606266', fontWeight: 500 }">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="roomName" label="会议室" min-width="110" />
        <el-table-column prop="username" label="预约人" min-width="90" />
        <el-table-column prop="subject" label="主题" min-width="130" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="人数" width="70" align="center" />
        <el-table-column prop="contactPhone" label="电话" min-width="120" />
        <el-table-column label="时段" min-width="180"><template #default="{ row }">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</template></el-table-column>
        <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="statusType(row.status)" size="small" effect="light">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <template v-if="row.status === 0">
                <el-tooltip content="通过"><el-button type="success" link circle size="small" @click="handleApprove(row.id)"><el-icon><Check /></el-icon></el-button></el-tooltip>
                <el-tooltip content="拒绝"><el-button type="danger" link circle size="small" @click="handleReject(row.id)"><el-icon><Close /></el-icon></el-button></el-tooltip>
              </template>
              <template v-else-if="row.status === 1">
                <el-tooltip content="取消预约"><el-button type="danger" link circle size="small" @click="handleCancel(row.id)"><el-icon><Close /></el-icon></el-button></el-tooltip>
              </template>
              <span v-else style="color: #c0c4cc">-</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <span class="total-text">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" layout="prev, pager, next, sizes" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close, Refresh, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { listAllReservations, approveReservation, rejectReservation, cancelReservation } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const loading = ref(false); const expanded = ref(false); const tableData = ref<Reservation[]>([]); const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '', contactPhone: '', status: undefined as number | undefined })
function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info' }
function formatTime(t: string) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function resetQuery() { query.keyword = ''; query.contactPhone = ''; query.status = undefined; query.page = 1; loadData() }
async function loadData() { loading.value = true; try { const res = await listAllReservations(query); tableData.value = res.data.records; total.value = res.data.total } catch { /* */ } finally { loading.value = false } }
async function handleApprove(id: number) { try { await approveReservation(id); ElMessage.success('通过'); loadData() } catch { /* */ } }
async function handleReject(id: number) { try { await rejectReservation(id); ElMessage.success('已拒绝'); loadData() } catch { /* */ } }
async function handleCancel(id: number) { try { await cancelReservation(id); ElMessage.success('已取消'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-header { margin-bottom: 0; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-view { display: flex; flex-direction: column; gap: 16px; }
.search-bar { background: #fff; border-radius: 12px; padding: 20px 24px; display: flex; align-items: flex-end; justify-content: space-between; border: 1px solid #f0f0f0; }
.search-fields { display: flex; gap: 16px; flex: 1; flex-wrap: wrap; align-items: flex-end; }
.search-item { display: flex; flex-direction: column; gap: 6px; flex: 1; min-width: 180px; max-width: 280px; }
.search-item label { font-size: 13px; color: #606266; font-weight: 500; }
.search-item :deep(.el-input) { width: 200px; }
.search-actions { display: flex; gap: 8px; }
.table-card { background: #fff; border-radius: 12px; border: 1px solid #f0f0f0; overflow: hidden; }
.table-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f5f5f5; }
.toolbar-right { display: flex; gap: 4px; }
.action-buttons { display: flex; justify-content: center; gap: 4px; }
.pagination-wrap { display: flex; align-items: center; justify-content: flex-end; gap: 16px; padding: 14px 20px; border-top: 1px solid #f5f5f5; }
.total-text { font-size: 13px; color: #909399; }
</style>
