<template>
  <div class="room-list-container">
    <div class="page-header">
      <h2>会议室列表</h2>
    </div>
    <el-table :data="rooms" stripe v-loading="loading">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="location" label="位置" />
      <el-table-column prop="capacity" label="容纳人数" width="120" />
      <el-table-column prop="equipment" label="设备" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" link @click="goDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listRooms } from '@/api/meeting'
import type { MeetingRoom } from '@/types/meeting'

const router = useRouter()
const rooms = ref<MeetingRoom[]>([])
const loading = ref(false)

function goDetail(id: number) {
  router.push(`/meeting/rooms/${id}`)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await listRooms()
    rooms.value = res.data
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.room-list-container {
  padding: 20px;
}
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
}
</style>
