<template>
  <div class="notification-page">
    <div class="page-card">
      <div class="page-header">
        <h2 class="page-title">消息通知</h2>
        <div class="header-actions">
          <el-radio-group v-model="filter" @change="handleFilterChange">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="unread">未读</el-radio-button>
          </el-radio-group>
          <el-button v-if="unreadCount > 0" type="primary" plain @click="handleReadAll">
            <el-icon><Check /></el-icon>全部已读
          </el-button>
        </div>
      </div>

      <div v-loading="loading" class="notification-list">
        <div v-if="list.length === 0 && !loading" class="empty-state">
          <el-icon :size="48" color="#cbd5e1"><BellFilled /></el-icon>
          <p>暂无消息</p>
        </div>

        <div
          v-for="item in list"
          :key="item.id"
          class="notification-item"
          :class="{ unread: item.isRead === 0 }"
        >
          <div class="item-icon" :class="typeClass(item.type)">
            <el-icon><component :is="typeIcon(item.type)" /></el-icon>
          </div>
          <div class="item-content" @click="handleClickItem(item)">
            <div class="item-header">
              <span class="item-title">{{ item.title }}</span>
              <el-tag size="small" :type="tagType(item.type)" effect="light">{{ typeLabel(item.type) }}</el-tag>
            </div>
            <div class="item-body">{{ item.content }}</div>
            <div class="item-time">{{ item.createTime }}</div>
          </div>
          <div class="item-actions">
            <el-button v-if="item.isRead === 0" text size="small" @click="handleReadOne(item)">标为已读</el-button>
            <el-button text size="small" type="danger" @click="handleDelete(item)">删除</el-button>
          </div>
        </div>
      </div>

      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BellFilled, Check, Calendar, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getNotifications, deleteNotification, type NotificationItem } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const loading = ref(false)
const list = ref<NotificationItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filter = ref<'all' | 'unread'>('all')
const unreadCount = ref(0)

async function loadList() {
  loading.value = true
  try {
    const res = await getNotifications({
      page: page.value,
      size: size.value,
      isRead: filter.value === 'unread' ? 0 : undefined
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
    await notificationStore.fetchUnreadCount()
    unreadCount.value = notificationStore.unreadCount
  } catch { /* */ } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  page.value = 1
  loadList()
}

async function handleClickItem(item: NotificationItem) {
  if (item.isRead === 0) {
    await notificationStore.readOne(item.id)
    item.isRead = 1
    unreadCount.value = notificationStore.unreadCount
  }
  if (item.refType === 'reservation' && item.refId) {
    router.push(`/reservation/my/${item.refId}`)
  }
}

async function handleReadOne(item: NotificationItem) {
  await notificationStore.readOne(item.id)
  item.isRead = 1
  unreadCount.value = notificationStore.unreadCount
}

async function handleReadAll() {
  await notificationStore.readAll()
  list.value.forEach(n => { n.isRead = 1 })
  unreadCount.value = 0
  ElMessage.success('已全部标记为已读')
}

async function handleDelete(item: NotificationItem) {
  try {
    await ElMessageBox.confirm('确定删除该消息？', '提示', { type: 'warning' })
    await deleteNotification(item.id)
    ElMessage.success('已删除')
    if (item.isRead === 0) {
      await notificationStore.fetchUnreadCount()
      unreadCount.value = notificationStore.unreadCount
    }
    loadList()
  } catch { /* */ }
}

function typeLabel(type: string): string {
  if (type.startsWith('RESERVATION_CREATED')) return '预约'
  if (type.startsWith('RESERVATION_APPROVED')) return '审批'
  if (type.startsWith('RESERVATION_REJECTED')) return '审批'
  if (type.startsWith('RESERVATION_CANCELLED')) return '取消'
  return '系统'
}

function tagType(type: string): any {
  if (type.startsWith('RESERVATION_APPROVED')) return 'success'
  if (type.startsWith('RESERVATION_REJECTED')) return 'danger'
  if (type.startsWith('RESERVATION_CANCELLED')) return 'warning'
  return 'info'
}

function typeClass(type: string): string {
  if (type.startsWith('RESERVATION_APPROVED')) return 'success'
  if (type.startsWith('RESERVATION_REJECTED')) return 'danger'
  if (type.startsWith('RESERVATION_CANCELLED')) return 'warning'
  return 'primary'
}

function typeIcon(type: string) {
  if (type.startsWith('RESERVATION_APPROVED')) return CircleCheck
  if (type.startsWith('RESERVATION_REJECTED')) return CircleClose
  if (type.startsWith('RESERVATION')) return Calendar
  return BellFilled
}

onMounted(loadList)
</script>

<style scoped>
.notification-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-card {
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-light);
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notification-list {
  min-height: 300px;
  padding: 8px 0;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
}
.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-light);
  transition: background 0.15s;
}
.notification-item:last-child {
  border-bottom: none;
}
.notification-item:hover {
  background: var(--bg-page);
}
.notification-item.unread {
  background: rgba(102, 126, 234, 0.03);
}

.item-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.item-icon.primary { background: rgba(102, 126, 234, 0.1); color: var(--primary); }
.item-icon.success { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.item-icon.danger { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.item-icon.warning { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }

.item-content {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}
.notification-item.unread .item-title {
  font-weight: 600;
}

.item-body {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  white-space: pre-line;
  margin-bottom: 4px;
}

.item-time {
  font-size: 12px;
  color: var(--text-muted);
}

.item-actions {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;
}

.pagination-wrapper {
  padding: 16px 24px;
  display: flex;
  justify-content: center;
}
</style>
