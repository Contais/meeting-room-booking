<template>
  <div class="page-view" v-loading="loading">
    <template v-if="equipment">
      <!-- 设备基础信息 -->
      <div class="detail-card">
        <div class="detail-card-header">
          <div class="header-left">
            <el-button class="back-btn" @click="router.back()">
              <el-icon><ArrowLeft /></el-icon>
              <span>返回</span>
            </el-button>
            <span class="header-title">{{ equipment.name }}</span>
          </div>
          <div class="header-actions">
            <el-button :type="equipment.status === 1 ? 'warning' : 'success'" plain @click="handleToggle">
              <el-icon><Switch /></el-icon>
              <span>{{ equipment.status === 1 ? '禁用' : '启用' }}</span>
            </el-button>
            <el-button type="primary" plain @click="openEditDialog">
              <el-icon><Edit /></el-icon>
              <span>编辑</span>
            </el-button>
            <el-button type="danger" plain @click="handleDelete">
              <el-icon><Delete /></el-icon>
              <span>删除</span>
            </el-button>
          </div>
        </div>

        <div class="detail-card-body">
          <div class="equipment-header">
            <div class="equipment-icon">
              <el-icon :size="28"><Box /></el-icon>
            </div>
            <div class="equipment-info">
              <h2>{{ equipment.name }}</h2>
              <p class="equipment-code"><el-icon><Key /></el-icon> {{ equipment.code }}</p>
            </div>
            <el-tag :type="equipment.status === 1 ? 'success' : 'warning'" size="small" effect="light" round>
              {{ equipment.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>

          <div class="info-grid">
            <div class="info-item"><span class="info-label">设备分类</span><span class="info-value">{{ equipment.category || '-' }}</span></div>
            <div class="info-item"><span class="info-label">品牌</span><span class="info-value">{{ equipment.brand || '-' }}</span></div>
            <div class="info-item"><span class="info-label">型号</span><span class="info-value">{{ equipment.model || '-' }}</span></div>
            <div class="info-item"><span class="info-label">购置日期</span><span class="info-value">{{ equipment.purchaseDate || '-' }}</span></div>
            <div class="info-item"><span class="info-label">创建时间</span><span class="info-value">{{ formatDateTime(equipment.createTime) }}</span></div>
          </div>

          <div v-if="equipment.description" class="description">
            <h4>设备描述</h4>
            <p>{{ equipment.description }}</p>
          </div>
        </div>
      </div>

      <!-- 关联会议室 -->
      <div class="detail-card">
        <div class="detail-card-body">
          <div class="section-header">
            <h4 class="section-title">关联会议室</h4>
            <el-button class="btn-outline" size="small" @click="openAssignDialog">
              <el-icon><Plus /></el-icon>管理关联
            </el-button>
          </div>
          <div v-if="equipment.rooms && equipment.rooms.length" class="room-list">
            <div v-for="room in equipment.rooms" :key="room.id" class="room-item" @click="router.push(`/meeting/rooms/${room.id}`)">
              <div class="room-item-icon"><el-icon><OfficeBuilding /></el-icon></div>
              <div class="room-item-info">
                <span class="room-item-name">{{ room.name }}</span>
                <span class="room-item-location">{{ room.location || '暂无位置' }}</span>
              </div>
              <el-tag type="success" size="small" effect="light" round>×{{ room.quantity || 1 }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂未关联任何会议室" :image-size="80" />
        </div>
      </div>
    </template>

    <!-- 编辑抽屉 -->
    <FormDrawer v-model:visible="editDialogVisible" title="编辑设备" :loading="submitting" @submit="handleEditSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="设备编码"><el-input v-model="form.code" disabled /></el-form-item>
        <el-form-item label="设备名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker v-model="form.purchaseDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">禁用</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </FormDrawer>

    <!-- 管理关联会议室 -->
    <el-dialog v-model="assignDialogVisible" title="管理关联会议室" width="540px" :close-on-click-modal="false">
      <div class="assign-tip">勾选要关联的会议室，取消勾选可解除关联。</div>
      <el-select v-model="selectedRoomIds" multiple placeholder="选择会议室" filterable style="width:100%">
        <el-option v-for="r in roomOptions" :key="r.id" :label="r.name + (r.location ? ` (${r.location})` : '')" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssignSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Box, Key, Edit, Delete, Switch, Plus, OfficeBuilding } from '@element-plus/icons-vue'
import {
  getEquipmentDetail,
  updateEquipment,
  deleteEquipment,
  toggleEquipmentStatus,
  assignRooms,
} from '@/api/equipment'
import { listRoomsAdmin } from '@/api/meeting'
import FormDrawer from '@/components/FormDrawer.vue'
import { formatDateTime } from '@/utils/datetime'
import type { Equipment, RoomEquipmentItem } from '@/types/equipment'
import type { MeetingRoom } from '@/types/meeting'

const route = useRoute()
const router = useRouter()
const equipment = ref<Equipment | null>(null)
const loading = ref(false)
const submitting = ref(false)

const categories = ['投影仪', '白板', '电视', '音响', '视频会议', '空调', '其他']

// 编辑
const editDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  code: '',
  name: '',
  category: '',
  brand: '',
  model: '',
  status: 1,
  purchaseDate: '',
  description: '',
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
}

// 关联会议室
const assignDialogVisible = ref(false)
const assigning = ref(false)
const roomOptions = ref<MeetingRoom[]>([])
const selectedRoomIds = ref<number[]>([])

async function loadDetail() {
  loading.value = true
  try {
    const res = await getEquipmentDetail(Number(route.params.id))
    equipment.value = res.data
  } catch { /* */ } finally {
    loading.value = false
  }
}

function openEditDialog() {
  if (!equipment.value) return
  Object.assign(form, {
    id: equipment.value.id,
    code: equipment.value.code,
    name: equipment.value.name,
    category: equipment.value.category || '',
    brand: equipment.value.brand || '',
    model: equipment.value.model || '',
    status: equipment.value.status,
    purchaseDate: equipment.value.purchaseDate || '',
    description: equipment.value.description || '',
  })
  editDialogVisible.value = true
}

async function handleEditSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
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
    editDialogVisible.value = false
    loadDetail()
  } catch { /* */ } finally {
    submitting.value = false
  }
}

async function handleToggle() {
  if (!equipment.value) return
  const action = equipment.value.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}该设备吗？`, '提示', { type: 'warning' })
    await toggleEquipmentStatus(equipment.value.id)
    ElMessage.success(`${action}成功`)
    loadDetail()
  } catch { /* */ }
}

async function handleDelete() {
  if (!equipment.value) return
  try {
    await ElMessageBox.confirm('确定删除该设备吗？将同时解除与会议室的关联，此操作不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error',
    })
    await deleteEquipment(equipment.value.id)
    ElMessage.success('删除成功')
    router.back()
  } catch { /* */ }
}

async function openAssignDialog() {
  if (!equipment.value) return
  // 加载会议室选项
  try {
    const res = await listRoomsAdmin({ page: 1, size: 200 })
    roomOptions.value = res.data.records || []
  } catch { /* */ }
  selectedRoomIds.value = (equipment.value.rooms || []).map(r => r.id)
  assignDialogVisible.value = true
}

async function handleAssignSubmit() {
  if (!equipment.value) return
  assigning.value = true
  try {
    const rooms: RoomEquipmentItem[] = selectedRoomIds.value.map(id => ({ roomId: id, quantity: 1 }))
    await assignRooms(equipment.value.id, rooms)
    ElMessage.success('关联更新成功')
    assignDialogVisible.value = false
    loadDetail()
  } catch { /* */ } finally {
    assigning.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }

.equipment-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.equipment-icon { width: 48px; height: 48px; border-radius: 12px; background: linear-gradient(135deg, #667eea, #764ba2); display: flex; align-items: center; justify-content: center; color: #fff; }
.equipment-info h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 0; }
.equipment-code { font-size: 13px; color: var(--text-secondary); margin: 4px 0 0 0; display: flex; align-items: center; gap: 4px; }

.info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; padding: 16px 0; border-top: 1px solid var(--border-light); border-bottom: 1px solid var(--border-light); }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 12px; color: var(--text-muted); }
.info-value { font-size: 14px; color: var(--text-primary); font-weight: 500; }

.description { margin-top: 16px; }
.description h4 { font-size: 14px; font-weight: 600; color: var(--text-primary); margin: 0 0 8px 0; }
.description p { font-size: 13px; color: var(--text-secondary); line-height: 1.7; margin: 0; }

.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0; }

.room-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.room-item { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid var(--border-light); border-radius: 10px; cursor: pointer; transition: all 0.2s; }
.room-item:hover { border-color: var(--primary); background: var(--primary-light, #f5f7ff); }
.room-item-icon { width: 36px; height: 36px; border-radius: 8px; background: var(--primary-light, #eef0ff); color: var(--primary, #667eea); display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
.room-item-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.room-item-name { font-size: 14px; font-weight: 500; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.room-item-location { font-size: 12px; color: var(--text-muted); }

.assign-tip { font-size: 13px; color: var(--text-secondary); margin-bottom: 12px; padding: 8px 12px; background: var(--bg-card); border-radius: 6px; border: 1px solid var(--border-light); }

@media (max-width: 768px) { .info-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
