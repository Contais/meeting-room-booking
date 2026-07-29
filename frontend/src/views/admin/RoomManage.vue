<template>
  <div class="page-view">
    <SearchBar @search="onFilterChange" @reset="resetQuery">
      <template #collapsed>
        <el-input v-model="query.keyword" placeholder="搜索会议室名称 / 位置" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
      </template>
      <template #expanded>
        <div class="search-item"><label>会议室名称</label><el-input v-model="query.name" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        <div class="search-item"><label>位置</label><el-input v-model="query.location" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        <div class="search-item"><label>设备</label><el-input v-model="query.equipment" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        <div class="search-item"><label>可容纳人数</label><el-input-number v-model="query.minCapacity" :min="1" :max="1000" controls-position="right" style="width:100%" @change="onFilterChange" /></div>
        <div class="search-item"><label>状态</label><el-select v-model="query.status" placeholder="全部" clearable @change="onFilterChange"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></div>
        <div class="search-item"><label>当天状态</label><el-select v-model="query.availability" placeholder="全部" clearable @change="onFilterChange"><el-option label="空闲" value="available" /><el-option label="使用中" value="busy" /><el-option label="禁用" value="inactive" /></el-select></div>
        <div class="search-item"><label>审批</label><el-select v-model="query.needApproval" placeholder="全部" clearable @change="onFilterChange"><el-option label="需审批" :value="1" /><el-option label="免审批" :value="0" /></el-select></div>
        <div class="search-item is-wide"><label>创建时间</label><el-date-picker v-model="createTimeRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss" @change="onCreateTimeRangeChange" /></div>
      </template>
    </SearchBar>

    <TableCard :total="total" v-model:page="query.page" v-model:size="query.size" @size-change="onSizeChange" @current-change="loadData">
      <template #toolbar-left>
        <el-button class="btn-outline" @click="showCreateDialog"><el-icon><Plus /></el-icon>新增会议室</el-button>
      </template>
      <template #toolbar-right>
        <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
      </template>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="location" label="位置" min-width="100" />
        <el-table-column prop="capacity" label="容量" width="80" align="center" />
        <el-table-column prop="equipment" label="设备" min-width="150" show-overflow-tooltip />
        <el-table-column label="时段" width="120"><template #default="{ row }">{{ row.bookableStart || '08:00' }}~{{ row.bookableEnd || '20:00' }}</template></el-table-column>
        <el-table-column label="审批" width="90" align="center"><template #default="{ row }"><el-tag :type="row.needApproval === 1 ? 'warning' : 'success'" size="small" effect="light">{{ row.needApproval === 1 ? '需审批' : '免审批' }}</el-tag></template></el-table-column>
        <el-table-column label="创建时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="当前" width="80" align="center"><template #default="{ row }"><el-tag v-if="row.status === 1" :type="row.currentAvailable ? 'success' : 'warning'" size="small" effect="light" round>{{ row.currentAvailable ? '空闲' : '使用中' }}</el-tag><span v-else style="color: var(--text-muted)">-</span></template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-links">
              <el-button type="primary" link @click="router.push({ path: `/admin/rooms/${row.id}`, query: { from: '/admin/rooms', fromTitle: '会议室管理', dt: '会议室详情' } })">
                <el-icon><View /></el-icon>详情
              </el-button>
              <el-button type="primary" link @click="showEditDialog(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button type="danger" link @click="handleDelete(row.id)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </TableCard>

    <FormDrawer v-model:visible="dialogVisible" :title="isEdit ? '编辑会议室' : '新增会议室'" :loading="submitting" @submit="handleSubmit">
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
    </FormDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, View } from '@element-plus/icons-vue'
import { listRoomsAdmin, createRoom, updateRoom, deleteRoom } from '@/api/meeting'
import SearchBar from '@/components/SearchBar.vue'
import TableCard from '@/components/TableCard.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { formatDateTime } from '@/utils/datetime'
import type { MeetingRoom } from '@/types/meeting'

const router = useRouter()
const loading = ref(false); const submitting = ref(false)
const tableData = ref<MeetingRoom[]>([]); const total = ref(0)
const dialogVisible = ref(false); const isEdit = ref(false); const formRef = ref<FormInstance>()
const query = reactive({ page: 1, size: 10, keyword: '', name: '', location: '', equipment: '', minCapacity: undefined as number | undefined, bookableStart: '', bookableEnd: '', needApproval: undefined as number | undefined, status: undefined as number | undefined, createTimeStart: '', createTimeEnd: '', availability: '' as string })
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
    const res = await listRoomsAdmin(params)
    let records = res.data.records
    // 当天状态为前端客户端过滤（基于 currentAvailable 字段）
    if (query.availability) {
      records = records.filter((r: MeetingRoom) => roomAvailabilityClass(r) === query.availability)
    }
    tableData.value = records
    total.value = Number(res.data.total) || 0
  } catch { /* */ } finally { loading.value = false }
}
function roomAvailabilityClass(room: MeetingRoom): string {
  if (room.status !== 1) return 'inactive'
  return room.currentAvailable ? 'available' : 'busy'
}
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { query.page = 1; loadData() }, 300)
}

function resetQuery() { query.keyword = ''; query.name = ''; query.location = ''; query.equipment = ''; query.minCapacity = undefined; query.bookableStart = ''; query.bookableEnd = ''; query.needApproval = undefined; query.status = undefined; query.createTimeStart = ''; query.createTimeEnd = ''; query.availability = ''; query.page = 1; createTimeRange.value = []; loadData() }
function showCreateDialog() { isEdit.value = false; Object.assign(form, { id: undefined, name: '', location: '', capacity: 10, equipment: '', imageUrl: '', description: '', bookableStart: '08:00', bookableEnd: '20:00', maxDuration: 480, advanceDays: 7, needApproval: 0 }); dialogVisible.value = true }
function showEditDialog(row: MeetingRoom) { isEdit.value = true; Object.assign(form, { id: row.id, name: row.name, location: row.location || '', capacity: row.capacity || 10, equipment: row.equipment || '', imageUrl: row.imageUrl || '', description: row.description || '', bookableStart: row.bookableStart || '08:00', bookableEnd: row.bookableEnd || '20:00', maxDuration: row.maxDuration || 480, advanceDays: row.advanceDays || 7, needApproval: row.needApproval || 0 }); dialogVisible.value = true }
async function handleSubmit() { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true; try { if (isEdit.value) { await updateRoom(form); ElMessage.success('更新成功') } else { await createRoom(form); ElMessage.success('创建成功') }; dialogVisible.value = false; loadData() } catch { /* */ } finally { submitting.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除该会议室?', '提示', { type: 'warning' }); await deleteRoom(id); ElMessage.success('删除成功'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
</style>
