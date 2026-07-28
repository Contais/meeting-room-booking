import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount, markAsRead, markAllAsRead } from '@/api/notification'

/**
 * 站内信通知 Store
 *
 * 一期：轮询模式（30s 定时拉取未读数）
 * 二期演进：将 start() 内部实现替换为 WebSocket 连接，组件层零改动
 */
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  let pollTimer: ReturnType<typeof setInterval> | null = null

  /** 拉取未读消息数 */
  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data || 0
    } catch { /* */ }
  }

  /** 启动通知轮询（登录后调用） */
  function start() {
    if (pollTimer) return
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, 30_000)
  }

  /** 停止轮询（登出时调用） */
  function stop() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
    unreadCount.value = 0
  }

  /** 标记单条已读并刷新角标 */
  async function readOne(id: number) {
    try {
      await markAsRead(id)
      if (unreadCount.value > 0) unreadCount.value--
    } catch { /* */ }
  }

  /** 全部已读 */
  async function readAll() {
    try {
      await markAllAsRead()
      unreadCount.value = 0
    } catch { /* */ }
  }

  return { unreadCount, fetchUnreadCount, start, stop, readOne, readAll }
})
