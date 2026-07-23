<template>
  <div class="page-view">
    <div class="welcome-banner">
      <div class="welcome-text">
        <h1>欢迎使用会议室预约系统</h1>
        <p>高效管理会议室资源，轻松预约，智能协作</p>
      </div>
      <div class="welcome-decor">
        <div class="decor-circle c1"></div>
        <div class="decor-circle c2"></div>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="userStore.isAdmin() ? 4 : 6" v-for="(item, idx) in statItems" :key="idx">
        <div class="stat-card">
          <div class="stat-icon" :style="{ background: item.bg }"><el-icon :size="20"><component :is="item.icon" /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ item.value }}</div><div class="stat-label">{{ item.label }}</div></div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card quick-actions">
      <h3 class="section-title">快捷操作</h3>
      <div class="action-grid">
        <div class="action-item" v-for="item in actionItems" :key="item.label" @click="$router.push(item.path)">
          <div class="action-icon" :style="{ background: item.bg }"><el-icon :size="18"><component :is="item.icon" /></el-icon></div>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, onMounted } from 'vue'
import { OfficeBuilding, Calendar, Clock, User, Setting, Bell, DataLine } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getHomeStats } from '@/api/home'

const userStore = useUserStore()
const stats = reactive({ roomCount: 0, todayReservations: 0, pendingApproval: 0, weekReservations: 0, totalReservations: 0 })

const statItems = computed(() => [
  { label: '会议室', value: stats.roomCount, icon: OfficeBuilding, bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { label: '今日预约', value: stats.todayReservations, icon: Calendar, bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
  { label: '本周预约', value: stats.weekReservations, icon: Clock, bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { label: '总预约数', value: stats.totalReservations, icon: DataLine, bg: 'linear-gradient(135deg, #10b981, #059669)' },
  ...(userStore.isAdmin() ? [{ label: '待审批', value: stats.pendingApproval, icon: Bell, bg: 'linear-gradient(135deg, #f59e0b, #d97706)' }] : []),
])

const actionItems = computed(() => [
  { label: '预约会议室', path: '/meeting/rooms', icon: OfficeBuilding, bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { label: '我的预约', path: '/reservation/my', icon: Calendar, bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  ...(userStore.isAdmin() ? [
    { label: '会议室管理', path: '/admin/rooms', icon: Setting, bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
    { label: '用户管理', path: '/admin/users', icon: User, bg: 'linear-gradient(135deg, #10b981, #059669)' },
    { label: '预约审批', path: '/admin/reservations', icon: Bell, bg: 'linear-gradient(135deg, #f59e0b, #d97706)' },
  ] : []),
])

onMounted(async () => { try { const res = await getHomeStats(); Object.assign(stats, res.data) } catch { /* */ } })
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }

.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 36px 40px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.welcome-text { position: relative; z-index: 1; }
.welcome-text h1 { font-size: 22px; font-weight: 700; margin: 0 0 6px 0; }
.welcome-text p { font-size: 14px; opacity: 0.85; margin: 0; }

.welcome-decor {
  position: absolute;
  right: 40px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 0;
}

.decor-circle {
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.c1 { width: 120px; height: 120px; }
.c2 { width: 80px; height: 80px; position: absolute; top: -40px; left: -60px; }

.stat-row { margin: 0; }

.stat-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-value { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.quick-actions { padding: 20px 24px; }

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: var(--text-primary);
}

.action-grid { display: flex; gap: 12px; flex-wrap: wrap; }

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 24px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid var(--border-light);
  background: #fff;
}

.action-item:hover {
  border-color: var(--primary);
}

.action-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.action-item span {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}
</style>
