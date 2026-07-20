<template>
  <div class="room-detail-container" v-loading="loading">
    <el-page-header @back="router.back()" title="返回列表">
      <template #content>
        <span class="page-header-title">{{ room?.name }}</span>
      </template>
    </el-page-header>

    <el-card v-if="room" class="detail-card">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称">{{ room.name }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ room.location }}</el-descriptions-item>
        <el-descriptions-item label="容纳人数">{{ room.capacity }} 人</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="room.status === 1 ? 'success' : 'info'">
            {{ room.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="设备" :span="2">{{ room.equipment || '无' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ room.description || '暂无描述' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRoomById } from '@/api/meeting'
import type { MeetingRoom } from '@/types/meeting'

const route = useRoute()
const router = useRouter()
const room = ref<MeetingRoom | null>(null)
const loading = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const res = await getRoomById(id)
    room.value = res.data
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.room-detail-container {
  padding: 20px;
}
.page-header-title {
  font-size: 18px;
  font-weight: bold;
}
.detail-card {
  margin-top: 20px;
}
</style>
