<template>
  <div class="page-view">
    <div class="page-header">
      <h2>会议室详情</h2>
      <el-button @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
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

    <div v-if="room" class="action-bar">
      <el-button type="primary" @click="handleEdit">
        <el-icon><Edit /></el-icon>
        编辑
      </el-button>
      <el-button :type="room.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus">
        {{ room.status === 1 ? '禁用' : '启用' }}
      </el-button>
      <el-button type="danger" @click="handleDelete">
        <el-icon><Delete /></el-icon>
        删除
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit, Delete, OfficeBuilding } from '@element-plus/icons-vue'
import { getRoomDetailAdmin, toggleRoomStatus, deleteRoom } from '@/api/meeting'
import { formatDateTime } from '@/utils/datetime'
import type { MeetingRoom } from '@/types/meeting'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const room = ref<MeetingRoom | null>(null)

const id = Number(route.params.id)

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

function handleEdit() {
  router.push({ path: '/admin/rooms', query: { edit: String(id) } })
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

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.detail-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  padding: 24px;
}

.room-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
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
  color: #303133;
  margin: 0 0 4px 0;
}

.room-info p {
  font-size: 14px;
  color: #909399;
  margin: 0 0 8px 0;
}

.mt-20 {
  margin-top: 20px;
}

.action-bar {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
