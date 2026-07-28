<template>
  <el-popover
    ref="popoverRef"
    placement="bottom-end"
    :width="360"
    trigger="click"
    popper-class="notification-popover"
    @show="onPopoverShow"
  >
    <template #reference>
      <el-tooltip content="消息通知" placement="bottom">
        <el-button class="icon-btn notification-btn">
          <el-icon><Bell /></el-icon>
          <span v-if="unreadCount > 0" class="notification-count">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </el-button>
      </el-tooltip>
    </template>

    <div class="notification-panel">
      <div class="panel-header">
        <span class="panel-title">消息通知</span>
        <el-button v-if="unreadCount > 0" text size="small" @click="handleReadAll">全部已读</el-button>
      </div>
      <div v-loading="loading" class="panel-list">
        <div v-if="recentList.length === 0 && !loading" class="panel-empty">
          <el-icon :size="32" color="#cbd5e1"><BellFilled /></el-icon>
          <p>暂无消息</p>
        </div>
        <div
          v-for="item in recentList"
          :key="item.id"
          class="notification-item"
          :class="{ unread: item.isRead === 0 }"
          @click="handleClickItem(item)"
        >
          <div class="item-dot" :class="typeClass(item.type)"></div>
          <div class="item-body">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-time">{{ formatTime(item.createTime) }}</div>
          </div>
        </div>
      </div>
      <div class="panel-footer" @click="goAll">
        <span>查看全部消息</span>
        <el-icon><Right /></el-icon>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, BellFilled, Right } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import { getNotifications, type NotificationItem } from '@/api/notification'

const router = useRouter()
const notificationStore = useNotificationStore()
const unreadCount = computed(() => notificationStore.unreadCount)

const loading = ref(false)
const recentList = ref<NotificationItem[]>([])
const popoverRef = ref()

async function onPopoverShow() {
  await loadRecent()
}

async function loadRecent() {
  loading.value = true
  try {
    const res = await getNotifications({ page: 1, size: 5 })
    recentList.value = res.data?.records || []
  } catch { /* */ } finally {
    loading.value = false
  }
}

async function handleClickItem(item: NotificationItem) {
  if (item.isRead === 0) {
    await notificationStore.readOne(item.id)
    item.isRead = 1
  }
  // 跳转到关联业务页面
  if (item.refType === 'reservation' && item.refId) {
    popoverRef.value?.hide()
    router.push(`/reservation/my/${item.refId}`)
  }
}

async function handleReadAll() {
  await notificationStore.readAll()
  recentList.value.forEach(n => { n.isRead = 1 })
}

function goAll() {
  popoverRef.value?.hide()
  router.push('/notifications')
}

function typeClass(type: string): string {
  if (type.startsWith('RESERVATION_APPROVED')) return 'success'
  if (type.startsWith('RESERVATION_REJECTED')) return 'danger'
  if (type.startsWith('RESERVATION_CANCELLED')) return 'warning'
  return 'primary'
}

function formatTime(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}小时前`
  if (diff < 7 * 86_400_000) return `${Math.floor(diff / 86_400_000)}天前`
  return d.toLocaleDateString()
}
</script>

<style scoped>
.notification-btn {
  position: relative;
}

.notification-count {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: var(--el-color-danger, #f56c6c);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  border-radius: 8px;
  border: 1.5px solid var(--bg-card, #fff);
  z-index: 2;
  pointer-events: none;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 8px;
  border-bottom: 1px solid var(--border-light);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.panel-list {
  max-height: 360px;
  overflow-y: auto;
  padding: 4px 0;
}

.panel-empty {
  text-align: center;
  padding: 32px 0;
  color: var(--text-muted);
}
.panel-empty p {
  margin-top: 8px;
  font-size: 13px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.notification-item:hover {
  background: var(--bg-page);
}
.notification-item.unread {
  background: rgba(102, 126, 234, 0.04);
}

.item-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}
.item-dot.primary { background: var(--primary); }
.item-dot.success { background: var(--success, #10b981); }
.item-dot.danger { background: var(--danger, #ef4444); }
.item-dot.warning { background: var(--warning, #f59e0b); }

.item-body {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.notification-item.unread .item-title {
  font-weight: 600;
}

.item-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.panel-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 10px;
  border-top: 1px solid var(--border-light);
  font-size: 13px;
  color: var(--primary);
  cursor: pointer;
  transition: background 0.15s;
}
.panel-footer:hover {
  background: var(--bg-page);
}

/* 暗黑模式适配 */
:global(html.dark) .notification-item:hover {
  background: #252542;
}
:global(html.dark) .notification-item.unread {
  background: rgba(102, 126, 234, 0.12);
}
:global(html.dark) .panel-footer:hover {
  background: #252542;
}
:global(html.dark) .notification-count {
  border-color: var(--bg-card, #1c1c2e);
}
</style>
