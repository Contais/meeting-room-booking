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

    <div class="stat-row">
      <div class="stat-card" v-for="(item, idx) in statItems" :key="idx" @click="goStat(item)">
        <div class="stat-icon" :style="{ background: item.bg }"><el-icon :size="20"><component :is="item.icon" /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ item.value }}</div><div class="stat-label">{{ item.label }}</div></div>
      </div>
    </div>


    <el-row :gutter="16" v-if="userStore.isAdmin()">
      <el-col :span="12">
        <div class="page-card chart-card">
          <h3 class="section-title">会议室使用率（今日）</h3>
          <v-chart class="chart" :option="usageChartOption" autoresize />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card chart-card">
          <h3 class="section-title">高峰时段分布</h3>
          <v-chart class="chart" :option="peakChartOption" autoresize />
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
import { reactive, computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, Calendar, User, Setting, Bell, DataLine, Tickets } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getHomeStats, getRoomUsage, getPeakHours } from '@/api/home'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent])

const router = useRouter()
const userStore = useUserStore()
const stats = reactive({ roomCount: 0, todayReservations: 0, pendingApproval: 0, weekReservations: 0, totalReservations: 0, myUpcomingMeetings: 0, myPendingMeetings: 0 })

const usageChartOption = ref({})
const peakChartOption = ref({})

interface StatItem {
  label: string
  value: number
  icon: any
  bg: string
  path: string
  query?: Record<string, string>
}

const statItems = computed<StatItem[]>(() => {
  // 今日 00:00:00 ~ 23:59:59
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const dayStart = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T00:00:00`
  const dayEnd = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T23:59:59`
  const items: StatItem[] = [
    { label: '会议室', value: stats.roomCount, icon: OfficeBuilding, bg: 'linear-gradient(135deg, #667eea, #764ba2)', path: '/meeting/rooms' },
    // 即将到来的会议：传递 upcoming=1，列表页自动过滤 start_time > NOW()
    { label: '即将到来的会议', value: stats.myUpcomingMeetings, icon: Tickets, bg: 'linear-gradient(135deg, #f093fb, #f5576c)', path: '/my-meetings', query: { upcoming: '1' } },
    // 今日预约：传递今日时段，列表页按创建时段过滤
    { label: '今日预约', value: stats.todayReservations, icon: Calendar, bg: 'linear-gradient(135deg, #4facfe, #00f2fe)', path: '/reservation/my', query: { startTime: dayStart, endTime: dayEnd, status: '1' } },
    // 待响应邀请：传递 attendeeStatus=0（待响应）
    { label: '待响应邀请', value: stats.myPendingMeetings, icon: Bell, bg: 'linear-gradient(135deg, #f59e0b, #d97706)', path: '/my-meetings', query: { attendeeStatus: '0' } },
  ]
  if (userStore.isAdmin()) {
    items.push({ label: '待审批', value: stats.pendingApproval, icon: DataLine, bg: 'linear-gradient(135deg, #10b981, #059669)', path: '/admin/reservations', query: { status: '0' } })
  }
  return items
})

function goStat(item: StatItem) {
  router.push({ path: item.path, query: item.query || {} })
}

const actionItems = computed(() => [
  { label: '预约会议室', path: '/meeting/rooms', icon: OfficeBuilding, bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { label: '我的会议', path: '/my-meetings', icon: Tickets, bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
  { label: '我的预约', path: '/reservation/my', icon: Calendar, bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  ...(userStore.isAdmin() ? [
    { label: '会议室管理', path: '/admin/rooms', icon: Setting, bg: 'linear-gradient(135deg, #f59e0b, #d97706)' },
    { label: '用户管理', path: '/admin/users', icon: User, bg: 'linear-gradient(135deg, #10b981, #059669)' },
    { label: '预约审批', path: '/admin/reservations', icon: Bell, bg: 'linear-gradient(135deg, #f59e0b, #d97706)' },
  ] : []),
])

onMounted(async () => {
  try {
    const res = await getHomeStats(); Object.assign(stats, res.data)
  } catch { /* */ }
  if (userStore.isAdmin()) {
    try {
      const res = await getRoomUsage()
      usageChartOption.value = {
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, bottom: 40, top: 10 },
        xAxis: { type: 'category', data: res.data.map((r: any) => r.roomName), axisLabel: { fontSize: 11 } },
        yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
        series: [{ type: 'bar', data: res.data.map((r: any) => Math.round(r.usageRate * 100)), itemStyle: { borderRadius: [4, 4, 0, 0], color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }] } } }]
      }
    } catch { /* */ }
    try {
      const res = await getPeakHours()
      peakChartOption.value = {
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, bottom: 40, top: 10 },
        xAxis: { type: 'category', data: res.data.map((r: any) => r.hour + ':00'), axisLabel: { fontSize: 11 } },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: res.data.map((r: any) => r.count), itemStyle: { borderRadius: [4, 4, 0, 0], color: '#4facfe' } }]
      }
    } catch { /* */ }
  }
})
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

.stat-row { display: flex; gap: 16px; margin: 0; }

.stat-card {
  flex: 1;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.stat-card:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
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
  background: var(--bg-card);
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

.chart-card { padding: 20px 24px; }
.chart { width: 100%; height: 260px; }

.action-item span {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}
</style>
