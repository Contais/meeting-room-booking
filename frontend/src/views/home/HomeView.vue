<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>会议室总数</span>
          </template>
          <div class="stat-value">{{ stats.roomCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>今日预约</span>
          </template>
          <div class="stat-value">{{ stats.todayReservations }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>待审批</span>
          </template>
          <div class="stat-value">{{ stats.pendingApproval }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

const stats = reactive({
  roomCount: 0,
  todayReservations: 0,
  pendingApproval: 0,
})
</script>

<style scoped>
.home-container {
  padding: 20px;
}
.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #409eff;
  text-align: center;
}
</style>
