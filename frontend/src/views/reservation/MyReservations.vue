<template>
  <div class="page-view">
    <div class="page-header"><h2>我的预约</h2></div>
    <div class="search-bar">
      <div class="search-fields">
        <template v-if="!expanded">
          <div class="search-item"><el-input class="search-keyword-input" v-model="query.keyword" placeholder="搜索会议主题" clearable @input="onSearchInput" /></div>
        </template>
        <template v-else>
          <div class="search-item"><label>会议主题</label><el-input v-model="query.subject" placeholder="搜索会议主题" clearable @input="onSearchInput" /></div>
          <div class="search-item"><label>状态</label><el-select v-model="query.status" placeholder="请选择" clearable @change="loadData"><el-option label="已确认" :value="1" /><el-option label="待确认" :value="0" /><el-option label="已取消" :value="2" /></el-select></div>
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
        <el-table-column prop="roomName" label="会议室" min-width="120" />
        <el-table-column prop="subject" label="会议主题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="人数" width="70" align="center" />
        <el-table-column label="预约时段" min-width="190"><template #default="{ row }">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</template></el-table-column>
        <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="statusType(row.status)" size="small" effect="light">{{ statusText(row.status) }}</el-tag></template></el-table-column>
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
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" layout="prev, pager, next, sizes" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { listMyReservations, cancelReservation } from '@/api/reservation'
import type { Reservation } from '@/types/reservation'

const loading = ref(false); const expanded = ref(false); const tableData = ref<Reservation[]>([]); const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '', subject: '', status: undefined as number | undefined })
function statusText(s: number) { return { 0: '待确认', 1: '已确认', 2: '已取消' }[s] || '未知' }
function statusType(s: number) { return { 0: 'warning', 1: 'success', 2: 'info' }[s] as any || 'info' }
function formatTime(t: string) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() { if (searchTimer) clearTimeout(searchTimer); searchTimer = setTimeout(() => { query.page = 1; loadData() }, 300) }

function resetQuery() { query.keyword = ''; query.subject = ''; query.status = undefined; query.page = 1; loadData() }
async function loadData() { loading.value = true; try { const res = await listMyReservations(query); tableData.value = res.data.records; total.value = res.data.total } catch { /* */ } finally { loading.value = false } }
async function handleCancel(id: number) { try { await cancelReservation(id); ElMessage.success('已取消'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-header { margin-bottom: 0; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-view { display: flex; flex-direction: column; gap: 16px; }
.search-bar { background: #fff; border-radius: 12px; padding: 20px 24px; display: flex; align-items: flex-end; justify-content: space-between; border: 1px solid #f0f0f0; }
.search-fields { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; flex: 1; align-items: end; }
    .search-item { display: flex; flex-direction: column; gap: 6px; }
    .search-item label { font-size: 13px; color: #606266; font-weight: 500; }
    .search-item :deep(.el-input),
    .search-item :deep(.el-select) { width: 260px; }
    .search-keyword-input { width: 640px !important; }
.search-actions { display: flex; gap: 8px; }
.table-card { background: #fff; border-radius: 12px; border: 1px solid #f0f0f0; overflow: hidden; }
.table-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f5f5f5; }
.toolbar-right { display: flex; gap: 4px; }
.action-buttons { display: flex; justify-content: center; gap: 4px; }
.pagination-wrap { display: flex; align-items: center; justify-content: flex-end; gap: 16px; padding: 14px 20px; border-top: 1px solid #f5f5f5; }
.total-text { font-size: 13px; color: #909399; }
</style>
