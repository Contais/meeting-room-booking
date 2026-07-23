<template>
  <div class="home-container">
    <div class="welcome-banner">
      <div class="welcome-text">
        <h1>欢迎使用会议室预约系统</h1>
        <p>高效管理会议室资源，轻松预约，智能协作</p>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="userStore.isAdmin() ? 4 : 6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
            <el-icon :size="22"><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.roomCount }}</div>
            <div class="stat-label">会议室</div>
          </div>
        </div>
      </el-col>
      <el-col :span="userStore.isAdmin() ? 4 : 6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
            <el-icon :size="22"><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayReservations }}</div>
            <div class="stat-label">今日预约</div>
          </div>
        </div>
      </el-col>
      <el-col :span="userStore.isAdmin() ? 4 : 6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
            <el-icon :size="22"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.weekReservations }}</div>
            <div class="stat-label">本周预约</div>
          </div>
        </div>
      </el-col>
      <el-col :span="userStore.isAdmin() ? 4 : 6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #10b981, #059669)">
            <el-icon :size="22"><DataLine /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalReservations }}</div>
            <div class="stat-label">总预约数</div>
          </div>
        </div>
      </el-col>
      <el-col v-if="userStore.isAdmin()" :span="4">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f59e0b, #d97706)">
            <el-icon :size="22"><Bell /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pendingApproval }}</div>
            <div class="stat-label">待审批</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="quick-actions page-card">
      <h3>快捷操作</h3>
      <div class="action-grid">
        <div class="action-item" @click="$router.push('/meeting/rooms')">
          <div class="action-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
            <el-icon :size="20"><OfficeBuilding /></el-icon>
          </div>
          <span>预约会议室</span>
        </div>
        <div class="action-item" @click="$router.push('/reservation/my')">
          <div class="action-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
            <el-icon :size="20"><Calendar /></el-icon>
          </div>
          <span>我的预约</span>
        </div>
        <div v-if="userStore.isAdmin()" class="action-item" @click="$router.push('/admin/rooms')">
          <div class="action-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
            <el-icon :size="20"><Setting /></el-icon>
          </div>
          <span>会议室管理</span>
        </div>
        <div v-if="userStore.isAdmin()" class="action-item" @click="$router.push('/admin/users')">
          <div class="action-icon" style="background: linear-gradient(135deg, #10b981, #059669)">
            <el-icon :size="20"><User /></el-icon>
          </div>
          <span>用户管理</span>
        </div>
        <div v-if="userStore.isAdmin()" class="action-item" @click="$router.push('/admin/reservations')">
          <div class="action-icon" style="background: linear-gradient(135deg, #f59e0b, #d97706)">
            <el-icon :size="20"><Bell /></el-icon>
          </div>
          <span>预约审批</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { OfficeBuilding, Calendar, Clock, User, Setting, Bell, DataLine } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getHomeStats } from '@/api/home'

const userStore = useUserStore()
const stats = reactive({ roomCount: 0, todayReservations: 0, pendingApproval: 0, weekReservations: 0, totalReservations: 0 })

onMounted(async () => {
  try { const res = await getHomeStats(); Object.assign(stats, res.data) } catch { /* */ }
})
</script>

<style scoped>
.home-container { display: flex; flex-direction: column; gap: 20px; }
.welcome-banner { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 16px; padding: 40px; color: #fff; }
.welcome-text h1 { font-size: 24px; font-weight: 700; margin: 0 0 8px 0; }
.welcome-text p { font-size: 15px; opacity: 0.85; margin: 0; }
.stat-row { margin: 0; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px 16px; display: flex; align-items: center; gap: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); transition: transform 0.2s, box-shadow 0.2s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.stat-value { font-size: 24px; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 12px; color: #9ca3af; margin-top: 2px; }
.quick-actions h3 { font-size: 16px; font-weight: 600; margin: 0 0 20px 0; color: #1a1a2e; }
.action-grid { display: flex; gap: 16px; flex-wrap: wrap; }
.action-item { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 20px 28px; border-radius: 12px; cursor: pointer; transition: all 0.2s; background: #f9fafb; }
.action-item:hover { background: #f3f4f6; transform: translateY(-2px); }
.action-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; }
.action-item span { font-size: 13px; color: #374151; font-weight: 500; }
</style>
