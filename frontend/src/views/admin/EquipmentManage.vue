<template>
  <div class="page-view">
    <SearchBar @search="onFilterChange" @reset="resetQuery">
      <template #collapsed>
        <el-input v-model="query.keyword" placeholder="搜索设备名称 / 编码 / 品牌" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
      </template>
      <template #expanded>
        <div class="search-item"><label>设备名称</label><el-input v-model="query.name" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        <div class="search-item"><label>分类</label><el-select v-model="query.category" placeholder="全部" clearable @change="onFilterChange"><el-option v-for="c in categories" :key="c" :label="c" :value="c" /></el-select></div>
        <div class="search-item"><label>品牌</label><el-input v-model="query.brand" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        <div class="search-item"><label>状态</label><el-select v-model="query.status" placeholder="全部" clearable @change="onFilterChange"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></div>
      </template>
    </SearchBar>

    <TableCard :total="total" v-model:page="query.page" v-model:size="query.size" @size-change="onSizeChange" @current-change="loadData">
      <template #toolbar-left>
        <el-button class="btn-outline" @click="showCreateDialog"><el-icon><Plus /></el-icon>新增设备</el-button>
      </template>
      <template #toolbar-right>
        <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
      </template>

      <el-table :data="tableData" v-loading="loading" empty-text="暂无设备数据，点击左上角「新增设备」创建">
        <el-table-column type="index" :index="(index: number) => (query.page - 1) * query.size + index + 1" label="序号" width="70" align="center" />
        <el-table-column prop="code" label="设备编码" width="150" />
        <el-table-column prop="name" label="设备名称" min-width="120" />
        <el-table-column prop="category" label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.category" type="info" size="small" effect="light" round>{{ row.category }}</el-tag>
            <span v-else style="color: var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="品牌型号" min-width="160">
          <template #default="{ row }">
            <span v-if="row.brand || row.model">{{ [row.brand, row.model].filter(Boolean).join(' / ') }}</span>
            <span v-else style="color: var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="关联会议室" width="120" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.rooms && row.rooms.length" :content="row.rooms.map((r: RoomBrief) => r.name).join('、')" placement="top">
              <el-tag type="success" size="small" effect="light" round>{{ row.rooms.length }} 间</el-tag>
            </el-tooltip>
            <span v-else style="color: var(--text-muted)">未关联</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-links">
              <el-button type="primary" link @click="router.push({ path: `/admin/equipments/${row.id}`, query: { from: '/admin/equipments', fromTitle: '设备管理', dt: '设备详情' } })">
                <el-icon><View /></el-icon>详情
              </el-button>
              <el-button type="primary" link @click="showEditDialog(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-dropdown trigger="click" popper-class="action-menu-popper" @command="(cmd: string) => handleRowCommand(cmd, row)">
                <el-button type="primary" link>
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="toggle">
                      <el-icon><Switch /></el-icon>{{ row.status === 1 ? '禁用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided class="danger-item">
                      <el-icon><Delete /></el-icon>删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </TableCard>

    <!-- 新增/编辑设备 -->
    <FormDrawer v-model:visible="dialogVisible" :title="isEdit ? '编辑设备' : '新增设备'" :loading="submitting" @submit="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="设备编码" prop="code">
          <el-input v-model="form.code" placeholder="如：EQ-PROJ-001" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="如：激光投影仪" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" placeholder="如：Epson" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" placeholder="如：CB-FH52" /></el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker v-model="form.purchaseDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="设备描述" />
        </el-form-item>
        <el-form-item label="状态" v-if="isEdit">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider content-position="left">关联会议室</el-divider>
        <el-form-item label="会议室">
          <el-select v-model="form.roomIds" multiple placeholder="选择关联的会议室（可多选）" style="width:100%" filterable>
            <el-option v-for="r in roomOptions" :key="r.id" :label="r.name + (r.location ? ` (${r.location})` : '')" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </FormDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, View, Switch, ArrowDown } from '@element-plus/icons-vue'
import {
  listEquipments,
  createEquipment,
  updateEquipment,
  deleteEquipment,
  toggleEquipmentStatus,
} from '@/api/equipment'
import { listRoomsAdmin } from '@/api/meeting'
import SearchBar from '@/components/SearchBar.vue'
import TableCard from '@/components/TableCard.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { formatDateTime } from '@/utils/datetime'
import type { Equipment, RoomBrief, RoomEquipmentItem } from '@/types/equipment'
import type { MeetingRoom } from '@/types/meeting'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Equipment[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const roomOptions = ref<MeetingRoom[]>([])

const categories = ['投影仪', '白板', '电视', '音响', '视频会议', '空调', '其他']

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  name: '',
  category: undefined as string | undefined,
  brand: '',
  status: undefined as number | undefined,
})

const form = reactive({
  id: undefined as string | undefined,
  code: '',
  name: '',
  category: '',
  brand: '',
  model: '',
  status: 1,
  purchaseDate: '',
  description: '',
  roomIds: [] as string[],
})

const rules: FormRules = {
  code: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
}

function onSizeChange() { query.page = 1; loadData() }
function onFilterChange() { query.page = 1; loadData() }
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { query.page = 1; loadData() }, 300)
}

function resetQuery() {
  query.keyword = ''
  query.name = ''
  query.category = undefined
  query.brand = ''
  query.status = undefined
  query.page = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.name) params.name = query.name
    if (query.category) params.category = query.category
    if (query.brand) params.brand = query.brand
    if (query.status != null) params.status = query.status
    const res = await listEquipments(params)
    tableData.value = res.data.records
    total.value = Number(res.data.total) || 0
  } catch { /* */ } finally {
    loading.value = false
  }
}

async function loadRoomOptions() {
  try {
    // 获取全部会议室用于关联选择（取启用 + 禁用，size 设大一些）
    const res = await listRoomsAdmin({ page: 1, size: 200, status: undefined })
    roomOptions.value = res.data.records || []
  } catch { /* */ }
}

function showCreateDialog() {
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    code: '',
    name: '',
    category: '',
    brand: '',
    model: '',
    status: 1,
    purchaseDate: '',
    description: '',
    roomIds: [],
  })
  dialogVisible.value = true
  loadRoomOptions()
}

function showEditDialog(row: Equipment) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    code: row.code,
    name: row.name,
    category: row.category || '',
    brand: row.brand || '',
    model: row.model || '',
    status: row.status,
    purchaseDate: row.purchaseDate || '',
    description: row.description || '',
    roomIds: (row.rooms || []).map((r: RoomBrief) => r.id),
  })
  dialogVisible.value = true
  loadRoomOptions()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      // 编辑：仅更新设备字段，会议室关联通过详情页或 assign-rooms 单独管理
      await updateEquipment({
        id: form.id,
        name: form.name,
        category: form.category,
        brand: form.brand,
        model: form.model,
        status: form.status,
        purchaseDate: form.purchaseDate,
        description: form.description,
      })
      ElMessage.success('更新成功')
    } else {
      // 新增：可一次性关联会议室
      const rooms: RoomEquipmentItem[] = form.roomIds.map(id => ({ roomId: id, quantity: 1 }))
      await createEquipment({
        code: form.code,
        name: form.name,
        category: form.category,
        brand: form.brand,
        model: form.model,
        status: form.status,
        purchaseDate: form.purchaseDate,
        description: form.description,
        rooms,
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch { /* */ } finally {
    submitting.value = false
  }
}

async function handleToggle(row: Equipment) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}该设备吗？`, '提示', { type: 'warning' })
    await toggleEquipmentStatus(row.id)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch { /* */ }
}

async function handleDelete(row: Equipment) {
  try {
    await ElMessageBox.confirm('确定删除该设备吗？将同时解除与会议室的关联，此操作不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error',
    })
    await deleteEquipment(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* */ }
}
function handleRowCommand(command: string, row: Equipment) {
  if (command === 'toggle') handleToggle(row)
  else if (command === 'delete') handleDelete(row)
}

onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
</style>
