<template>
  <div class="page-view">
    <div class="page-header"><h2>会议室管理</h2></div>
    <div class="search-bar">
      <div class="search-fields">
        <!-- 收起：关键字搜索 -->
        <template v-if="!expanded">
          <div class="search-item search-item-wide"><el-input v-model="query.keyword" placeholder="搜索会议室名称或位置" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        </template>
        <!-- 展开：所有具体字段 -->
        <template v-else>
          <div class="search-item"><label>关键字</label><el-input v-model="query.keyword" placeholder="名称/位置" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
          <div class="search-item"><label>会议室名称</label><el-input v-model="query.name" placeholder="请输入名称" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
          <div class="search-item"><label>位置</label><el-input v-model="query.location" placeholder="请输入位置" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
          <div class="search-item"><label>设备</label><el-input v-model="query.equipment" placeholder="请输入设备" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
          <div class="search-item"><label>可容纳人数</label><el-input-number v-model="query.minCapacity" :min="1" :max="1000" controls-position="right" style="width:100%" @change="onFilterChange" /></div>
          <div class="search-item"><label>状态</label><el-select v-model="query.status" placeholder="请选择" clearable @change="onFilterChange"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></div>
          <div class="search-item"><label>审批</label><el-select v-model="query.needApproval" placeholder="请选择" clearable @change="onFilterChange"><el-option label="需审批" :value="1" /><el-option label="免审批" :value="0" /></el-select></div>
          <div class="search-item"><label>创建时间</label><el-date-picker v-model="createTimeRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss" @change="onCreateTimeRangeChange" /></div>
        </template>
      </div>
      <div class="search-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="onFilterChange">查询</el-button>
        <el-button link type="primary" @click="toggleExpand">{{ expanded ? '收起' : '展开' }} <el-icon><ArrowDown v-if="!expanded" /><ArrowUp v-else /></el-icon></el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button class="btn-outline" @click="showCreateDialog"><el-icon><Plus /></el-icon>新增会议室</el-button>
        </div>
        <div class="toolbar-right">
          <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" :header-cell-style="{ background: '#fafbfc', color: '#606266', fontWeight: 500 }">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="location" label="位置" min-width="100" />
        <el-table-column prop="capacity" label="容量" width="80" align="center" />
        <el-table-column prop="equipment" label="设备" min-width="150" show-overflow-tooltip />
        <el-table-column label="时段" width="120"><template #default="{ row }">{{ row.bookableStart || '08:00' }}~{{ row.bookableEnd || '20:00' }}</template></el-table-column>
        <el-table-column label="审批" width="90" align="center"><template #default="{ row }"><el-tag :type="row.needApproval === 1 ? 'warning' : 'success'" size="small" effect="light">{{ row.needApproval === 1 ? '需审批' : '免审批' }}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="编辑"><el-button type="primary" link circle size="small" @click="showEditDialog(row)"><el-icon><Edit /></el-icon></el-button></el-tooltip>
              <el-tooltip content="删除"><el-button type="danger" link circle size="small" @click="handleDelete(row.id)"><el-icon><Delete /></el-icon></el-button></el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <span class="total-text">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="prev, pager, next, sizes, jumper" @size-change="onSizeChange" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑会议室' : '新增会议室'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="搜索会议室名称或位置" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" placeholder="如：3楼A301" /></el-form-item>
        <el-form-item label="容纳人数" prop="capacity"><el-input-number v-model="form.capacity" :min="1" :max="1000" style="width:100%" /></el-form-item>
        <el-form-item label="设备"><el-input v-model="form.equipment" placeholder="投影仪,白板,视频会议系统" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" placeholder="详细描述" /></el-form-item>
        <el-divider content-position="left">使用规则</el-divider>
        <el-form-item label="预约时段"><div style="display:flex;align-items:center;gap:8px"><el-time-picker v-model="form.bookableStart" format="HH:mm" value-format="HH:mm" placeholder="开始" style="width:130px" /><span>~</span><el-time-picker v-model="form.bookableEnd" format="HH:mm" value-format="HH:mm" placeholder="结束" style="width:130px" /></div></el-form-item>
        <el-form-item label="最大时长"><el-input-number v-model="form.maxDuration" :min="30" :max="1440" :step="30" style="width:180px" /><span style="margin-left:6px;color:var(--text-muted);font-size:13px">分钟</span></el-form-item>
        <el-form-item label="提前天数"><el-input-number v-model="form.advanceDays" :min="1" :max="90" style="width:180px" /><span style="margin-left:6px;color:var(--text-muted);font-size:13px">天</span></el-form-item>
        <el-form-item label="审批"><el-radio-group v-model="form.needApproval"><el-radio :value="0">免审批</el-radio><el-radio :value="1">需审批</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { listRoomsAdmin, createRoom, updateRoom, deleteRoom } from '@/api/meeting'
import type { MeetingRoom } from '@/types/meeting'

const loading = ref(false); const submitting = ref(false); const expanded = ref(false)
const tableData = ref<MeetingRoom[]>([]); const total = ref(0)
const dialogVisible = ref(false); const isEdit = ref(false); const formRef = ref<FormInstance>()
const query = reactive({ page: 1, size: 10, keyword: '', name: '', location: '', equipment: '', minCapacity: undefined as number | undefined, bookableStart: '', bookableEnd: '', needApproval: undefined as number | undefined, status: undefined as number | undefined, createTimeStart: '', createTimeEnd: '' })
const createTimeRange = ref<string[]>([])
const form = reactive({ id: undefined as number | undefined, name: '', location: '', capacity: 10, equipment: '', imageUrl: '', description: '', bookableStart: '08:00', bookableEnd: '20:00', maxDuration: 480, advanceDays: 7, needApproval: 0 })
const rules: FormRules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }], capacity: [{ required: true, message: '请输入人数', trigger: 'blur' }] }

function onSizeChange() { query.page = 1; loadData() }
function onFilterChange() { query.page = 1; loadData() }
function onCreateTimeRangeChange(val: string[] | null) {
  query.createTimeStart = val && val.length === 2 ? val[0] : ''
  query.createTimeEnd = val && val.length === 2 ? val[1] : ''
  onFilterChange()
}
function toggleExpand() { expanded.value = !expanded.value }
async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.name) params.name = query.name
    if (query.location) params.location = query.location
    if (query.equipment) params.equipment = query.equipment
    if (query.minCapacity != null) params.minCapacity = query.minCapacity
    if (query.needApproval != null) params.needApproval = query.needApproval
    if (query.status != null) params.status = query.status
    if (query.createTimeStart) params.createTimeStart = query.createTimeStart
    if (query.createTimeEnd) params.createTimeEnd = query.createTimeEnd
    const res = await listRoomsAdmin(params); tableData.value = res.data.records; total.value = Number(res.data.total) || 0
  } catch { /* */ } finally { loading.value = false }
}
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { query.page = 1; loadData() }, 300)
}

function resetQuery() { query.keyword = ''; query.name = ''; query.location = ''; query.equipment = ''; query.minCapacity = undefined; query.bookableStart = ''; query.bookableEnd = ''; query.needApproval = undefined; query.status = undefined; query.createTimeStart = ''; query.createTimeEnd = ''; query.page = 1; createTimeRange.value = []; loadData() }
function showCreateDialog() { isEdit.value = false; Object.assign(form, { id: undefined, name: '', location: '', capacity: 10, equipment: '', imageUrl: '', description: '', bookableStart: '08:00', bookableEnd: '20:00', maxDuration: 480, advanceDays: 7, needApproval: 0 }); dialogVisible.value = true }
function showEditDialog(row: MeetingRoom) { isEdit.value = true; Object.assign(form, { id: row.id, name: row.name, location: row.location || '', capacity: row.capacity || 10, equipment: row.equipment || '', imageUrl: row.imageUrl || '', description: row.description || '', bookableStart: row.bookableStart || '08:00', bookableEnd: row.bookableEnd || '20:00', maxDuration: row.maxDuration || 480, advanceDays: row.advanceDays || 7, needApproval: row.needApproval || 0 }); dialogVisible.value = true }
async function handleSubmit() { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true; try { if (isEdit.value) { await updateRoom(form); ElMessage.success('更新成功') } else { await createRoom(form); ElMessage.success('创建成功') }; dialogVisible.value = false; loadData() } catch { /* */ } finally { submitting.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除该会议室?', '提示', { type: 'warning' }); await deleteRoom(id); ElMessage.success('删除成功'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-header { margin-bottom: 0; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-view { display: flex; flex-direction: column; gap: 16px; }
.search-bar { background: #fff; border-radius: 12px; padding: 20px 24px; display: flex; align-items: flex-end; justify-content: space-between; border: 1px solid #f0f0f0; }
.search-fields { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; flex: 1; align-items: end; }
    .search-item { display: flex; flex-direction: column; gap: 6px; }
    .search-item-wide { width: 100%; }
    .search-item-wide :deep(.el-input) { width: 100%; }
    .search-item label { font-size: 13px; color: #606266; font-weight: 500; }
    .search-item :deep(.el-input),
    .search-item :deep(.el-select),
    .search-item :deep(.el-date-editor),
    .search-item :deep(.el-input-number) { width: 100%; }
.search-actions { display: flex; gap: 8px; }
.table-card { background: #fff; border-radius: 12px; border: 1px solid #f0f0f0; overflow: hidden; }
.table-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f5f5f5; }
.toolbar-left { display: flex; gap: 8px; }
.toolbar-right { display: flex; gap: 4px; }
.action-buttons { display: flex; justify-content: center; gap: 4px; }
.pagination-wrap { display: flex; align-items: center; justify-content: flex-end; gap: 16px; padding: 14px 20px; border-top: 1px solid #f5f5f5; }
.total-text { font-size: 13px; color: #909399; }
</style>
