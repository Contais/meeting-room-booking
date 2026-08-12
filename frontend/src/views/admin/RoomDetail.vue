<template>
  <div class="page-view">
    <div v-loading="loading" class="detail-card">
      <div class="detail-card-header">
        <div class="header-left">
          <el-button class="back-btn" @click="router.back()">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回</span>
          </el-button>
          <span v-if="room" class="header-title">{{ room.name }}</span>
        </div>
        <div v-if="room" class="header-actions">
          <el-button type="danger" plain @click="handleDelete">
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </el-button>
          <el-button :type="room.status === 1 ? 'warning' : 'success'" plain @click="handleToggleStatus">
            <el-icon><component :is="room.status === 1 ? 'Warning' : 'CircleCheck'" /></el-icon>
            <span>{{ room.status === 1 ? '禁用' : '启用' }}</span>
          </el-button>
          <el-button type="primary" plain @click="openEditDialog">
            <el-icon><Edit /></el-icon>
            <span>编辑</span>
          </el-button>
        </div>
      </div>

      <div class="detail-card-body">
        <div v-if="room" class="room-header">
          <div class="room-avatar">
            <img v-if="room.imageUrl" :src="room.imageUrl" class="room-avatar-img" alt="会议室图片" />
            <el-icon v-else :size="28"><OfficeBuilding /></el-icon>
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
    </div>

    <!-- 关联设备 -->
    <div v-if="room" class="detail-card">
      <div class="detail-card-body">
        <div class="section-header">
          <h4 class="section-title">关联设备</h4>
          <el-button class="btn-outline" size="small" @click="openEquipmentDialog">
            <el-icon><Plus /></el-icon>管理设备
          </el-button>
        </div>
        <div v-if="equipments.length" class="equipment-list">
          <div v-for="eq in equipments" :key="eq.id" class="equipment-item" @click="router.push(`/admin/equipments/${eq.id}`)">
            <div class="equipment-item-icon"><el-icon><Box /></el-icon></div>
            <div class="equipment-item-info">
              <span class="equipment-item-name">{{ eq.name }}</span>
              <span class="equipment-item-meta">{{ [eq.category, eq.brand, eq.model].filter(Boolean).join(' / ') || '无品牌型号' }}</span>
            </div>
            <el-tag type="success" size="small" effect="light" round>×{{ eq.quantity || 1 }}</el-tag>
          </div>
        </div>
        <el-empty v-else description="暂未关联任何设备" :image-size="80" />
      </div>
    </div>

    <!-- 管理关联设备 -->
    <el-dialog v-model="equipmentDialogVisible" title="管理关联设备" width="540px" :close-on-click-modal="false">
      <div class="assign-tip">勾选要关联的设备，取消勾选可解除关联。</div>
      <el-select v-model="selectedEquipmentIds" multiple placeholder="选择设备" filterable style="width:100%">
        <el-option v-for="e in equipmentOptions" :key="e.id" :label="e.name + (e.code ? ` (${e.code})` : '')" :value="e.id" />
      </el-select>
      <template #footer>
        <el-button @click="equipmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleEquipmentSubmit">保存</el-button>
      </template>
    </el-dialog>

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
import { ArrowLeft, Edit, Delete, OfficeBuilding, Plus, Box } from '@element-plus/icons-vue'
import { getRoomDetailAdmin, updateRoom, toggleRoomStatus, deleteRoom } from '@/api/meeting'
import { listEquipmentsByRoom, listActiveEquipments, assignEquipments } from '@/api/equipment'
import FormDrawer from '@/components/FormDrawer.vue'
import { formatDateTime } from '@/utils/datetime'
import type { MeetingRoom } from '@/types/meeting'
import type { Equipment } from '@/types/equipment'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const room = ref<MeetingRoom | null>(null)
const id = String(route.params.id)

// 关联设备
const equipments = ref<Equipment[]>([])
const equipmentDialogVisible = ref(false)
const equipmentOptions = ref<Equipment[]>([])
const selectedEquipmentIds = ref<string[]>([])
const assigning = ref(false)

// 编辑弹窗
const editDialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ id: undefined as string | undefined, name: '', location: '', capacity: 10, equipment: '', imageUrl: '', description: '', bookableStart: '08:00', bookableEnd: '20:00', maxDuration: 480, advanceDays: 7, needApproval: 0 })
const rules: FormRules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }], capacity: [{ required: true, message: '请输入人数', trigger: 'blur' }] }

async function loadDetail() {
  loading.value = true
  try {
    const res = await getRoomDetailAdmin(id)
    room.value = res.data
    // 并行加载关联设备
    loadEquipments()
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function loadEquipments() {
  try {
    const res = await listEquipmentsByRoom(id)
    equipments.value = res.data || []
  } catch { /* */ }
}

async function openEquipmentDialog() {
  try {
    const res = await listActiveEquipments()
    equipmentOptions.value = res.data || []
  } catch { /* */ }
  selectedEquipmentIds.value = equipments.value.map(e => e.id)
  equipmentDialogVisible.value = true
}

async function handleEquipmentSubmit() {
  assigning.value = true
  try {
    await assignEquipments(id, selectedEquipmentIds.value)
    ElMessage.success('设备关联更新成功')
    equipmentDialogVisible.value = false
    loadEquipments()
  } catch { /* */ } finally {
    assigning.value = false
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
  overflow: hidden;
}

.room-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0; }

.equipment-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.equipment-item { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid var(--border-light); border-radius: 10px; cursor: pointer; transition: all 0.2s; }
.equipment-item:hover { border-color: var(--primary); background: var(--primary-light, #f5f7ff); }
.equipment-item-icon { width: 36px; height: 36px; border-radius: 8px; background: var(--primary-light, #eef0ff); color: var(--primary, #667eea); display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
.equipment-item-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.equipment-item-name { font-size: 14px; font-weight: 500; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.equipment-item-meta { font-size: 12px; color: var(--text-muted); }

.assign-tip { font-size: 13px; color: var(--text-secondary); margin-bottom: 12px; padding: 8px 12px; background: var(--bg-card); border-radius: 6px; border: 1px solid var(--border-light); }
</style>
