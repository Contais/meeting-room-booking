<template>
  <div class="room-manage">
    <div class="page-header">
      <h2>会议室管理</h2>
      <el-button type="primary" class="btn-gradient" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>新增会议室
      </el-button>
    </div>

    <div class="search-card page-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="搜索">
          <el-input v-model="query.keyword" placeholder="名称/位置" clearable @keyup.enter="loadData" style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card page-card">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="名称" width="130" />
        <el-table-column prop="location" label="位置" width="120" />
        <el-table-column prop="capacity" label="容量" width="70" />
        <el-table-column prop="equipment" label="设备" width="150" show-overflow-tooltip />
        <el-table-column label="可预约时段" width="130">
          <template #default="{ row }">
            {{ row.bookableStart || '08:00' }} ~ {{ row.bookableEnd || '20:00' }}
          </template>
        </el-table-column>
        <el-table-column label="最大时长" width="90">
          <template #default="{ row }">
            {{ row.maxDuration || 480 }}分钟
          </template>
        </el-table-column>
        <el-table-column label="审批" width="70">
          <template #default="{ row }">
            <el-tag :type="row.needApproval === 1 ? 'warning' : 'success'" size="small" effect="dark" round>
              {{ row.needApproval === 1 ? '需审批' : '免审批' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark" round size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link size="small" @click="handleToggle(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑会议室' : '新增会议室'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：大会议室A" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="如：3楼A301" />
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="设备设施">
          <el-input v-model="form.equipment" placeholder="如：投影仪,白板,视频会议系统" />
        </el-form-item>
        <!-- 图片上传功能暂未开放 -->
        <!-- <el-form-item label="图片URL">
          <el-input v-model="form.imageUrl" placeholder="会议室实景图片地址" />
        </el-form-item> -->
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="会议室详细描述" />
        </el-form-item>

        <el-divider content-position="left">使用规则</el-divider>
        <el-form-item label="可预约时段">
          <div style="display: flex; align-items: center; gap: 8px">
            <el-time-picker v-model="form.bookableStart" format="HH:mm" value-format="HH:mm" placeholder="开始时间" style="width: 140px" />
            <span>~</span>
            <el-time-picker v-model="form.bookableEnd" format="HH:mm" value-format="HH:mm" placeholder="结束时间" style="width: 140px" />
          </div>
        </el-form-item>
        <el-form-item label="最大预约时长">
          <el-input-number v-model="form.maxDuration" :min="30" :max="1440" :step="30" style="width: 200px" />
          <span style="margin-left: 8px; color: #9ca3af; font-size: 13px">分钟</span>
        </el-form-item>
        <el-form-item label="提前预约天数">
          <el-input-number v-model="form.advanceDays" :min="1" :max="90" style="width: 200px" />
          <span style="margin-left: 8px; color: #9ca3af; font-size: 13px">天</span>
        </el-form-item>
        <el-form-item label="审批模式">
          <el-radio-group v-model="form.needApproval">
            <el-radio :value="0">免审批</el-radio>
            <el-radio :value="1">需管理员审批</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-gradient" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listRoomsAdmin, createRoom, updateRoom, toggleRoomStatus, deleteRoom } from '@/api/meeting'
import type { MeetingRoom } from '@/types/meeting'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<MeetingRoom[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const query = reactive({ page: 1, size: 10, keyword: '', status: undefined as number | undefined })

const form = reactive({
  id: undefined as number | undefined,
  name: '', location: '', capacity: 10,
  equipment: '', imageUrl: '', description: '',
  bookableStart: '08:00', bookableEnd: '20:00',
  maxDuration: 480, advanceDays: 7, needApproval: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入会议室名称', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容纳人数', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await listRoomsAdmin(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch { /* */ } finally { loading.value = false }
}

function resetQuery() {
  query.keyword = ''
  query.status = undefined
  query.page = 1
  loadData()
}

function showCreateDialog() {
  isEdit.value = false
  Object.assign(form, {
    id: undefined, name: '', location: '', capacity: 10,
    equipment: '', imageUrl: '', description: '',
    bookableStart: '08:00', bookableEnd: '20:00',
    maxDuration: 480, advanceDays: 7, needApproval: 0,
  })
  dialogVisible.value = true
}

function showEditDialog(row: MeetingRoom) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id, name: row.name, location: row.location || '', capacity: row.capacity || 10,
    equipment: row.equipment || '', imageUrl: row.imageUrl || '', description: row.description || '',
    bookableStart: row.bookableStart || '08:00', bookableEnd: row.bookableEnd || '20:00',
    maxDuration: row.maxDuration || 480, advanceDays: row.advanceDays || 7, needApproval: row.needApproval || 0,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) { await updateRoom(form); ElMessage.success('更新成功') }
    else { await createRoom(form); ElMessage.success('创建成功') }
    dialogVisible.value = false
    loadData()
  } catch { /* */ } finally { submitting.value = false }
}

async function handleToggle(row: MeetingRoom) {
  try { await toggleRoomStatus(row.id); ElMessage.success('操作成功'); loadData() } catch { /* */ }
}

async function handleDelete(id: number) {
  try { await deleteRoom(id); ElMessage.success('删除成功'); loadData() } catch { /* */ }
}

onMounted(loadData)
</script>

<style scoped>
.room-manage { display: flex; flex-direction: column; gap: 20px; }
.search-card :deep(.el-form-item) { margin-bottom: 0; }
</style>
