<template>
  <div class="page-view">
    <div class="page-header">
      <el-button @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div v-if="room" class="header-actions">
        <el-button type="primary" @click="openEditDialog"><el-icon><Edit /></el-icon>编辑</el-button>
        <el-button :type="room.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus">{{ room.status === 1 ? '禁用' : '启用' }}</el-button>
        <el-button type="danger" @click="handleDelete"><el-icon><Delete /></el-icon>删除</el-button>
      </div>
    </div>

    <div v-loading="loading" class="detail-card">
      <div v-if="room" class="room-header">
        <div class="room-avatar">
          <el-icon :size="28"><OfficeBuilding /></el-icon>
        </div>
        <div class="room-info">
          <h3>{{ room.name }}</h3>
          <p>{{ room.location || '未设置位置' }}</p>
          <el-tag :type="room.status === 1 ? 'success' : 'warning'" size="small">
            {{ room.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </div>
      </div>

      <el-descriptions v-if="room" :column="2" border class="mt-20">
        <el-descriptions-item label="会议室ID">
          {{ room.id }}
        </el-descriptions-item>
        <el-descriptions-item label="容纳人数">
          {{ room.capacity }} 人
        </el-descriptions-item>
        <el-descriptions-item label="设备">
          {{ room.equipment || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="审批模式">
          <el-tag :type="room.needApproval === 1 ? 'warning' : 'success'" size="small">
            {{ room.needApproval === 1 ? '需审批' : '免审批' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="可预约时段">
          {{ room.bookableStart || '08:00' }} ~ {{ room.bookableEnd || '20:00' }}
        </el-descriptions-item>
        <el-descriptions-item label="最大预约时长">
          {{ room.maxDuration || 480 }} 分钟
        </el-descriptions-item>
        <el-descriptions-item label="提前预约天数">
          {{ room.advanceDays || 7 }} 天
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(room.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          {{ room.description || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-empty v-else description="暂无数据" />
    </div>

    <FormDrawer v-model:visible="editDialogVisible" title="编辑会议室" :loading="submitting" @submit="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="请输入会议室名称" /></el-form-item>
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
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit, Delete, OfficeBuilding } from '@element-plus/icons-vue'
import { getRoomDetailAdmin, updateRoom, toggleRoomStatus, deleteRoom } from '@/api/meeting'
import FormDrawer from '@/components/FormDrawer.vue'
import { formatDateTime } from '@/utils/datetime'
import type { MeetingRoom } from '@/types/meeting'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const room = ref<MeetingRoom | null>(null)
const id = Number(route.params.id)

// 编辑弹窗
const editDialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ id: undefined as number | undefined, name: '', location: '', capacity: 10, equipment: '', imageUrl: '', description: '', bookableStart: '08:00', bookableEnd: '20:00', maxDuration: 480, advanceDays: 7, needApproval: 0 })
const rules: FormRules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }], capacity: [{ required: true, message: '请输入人数', trigger: 'blur' }] }

async function loadDetail() {
  loading.value = true
  try {
    const res = await getRoomDetailAdmin(id)
    room.value = res.data
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openEditDialog() {
  if (!room.value) return
  Object.assign(form, {
    id: room.value.id,
    name: room.value.name,
    location: room.value.location || '',
    capacity: room.value.capacity || 10,
    equipment: room.value.equipment || '',
    imageUrl: room.value.imageUrl || '',
    description: room.value.description || '',
    bookableStart: room.value.bookableStart || '08:00',
    bookableEnd: room.value.bookableEnd || '20:00',
    maxDuration: room.value.maxDuration || 480,
    advanceDays: room.value.advanceDays || 7,
    needApproval: room.value.needApproval || 0
  })
  editDialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateRoom(form)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadDetail()
  } catch { /* */ } finally {
    submitting.value = false
  }
}

async function handleToggleStatus() {
  const action = room.value?.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}该会议室？`, '提示', { type: 'warning' })
    await toggleRoomStatus(id)
    ElMessage.success(`${action}成功`)
    loadDetail()
  } catch {
    // 用户取消
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除该会议室？', '提示', { type: 'warning' })
    await deleteRoom(id)
    ElMessage.success('删除成功')
    router.push('/admin/rooms')
  } catch {
    // 用户取消
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.detail-card {
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  padding: 24px;
}

.room-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.room-avatar {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.room-info h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.room-info p {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 8px 0;
}

.mt-20 {
  margin-top: 20px;
}
</style>
