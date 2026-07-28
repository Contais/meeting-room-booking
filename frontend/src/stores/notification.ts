import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount, markAsRead, markAllAsRead } from '@/api/notification'
import { useUserStore } from '@/stores/user'
import type { NotificationItem } from '@/api/notification'

/**
 * 站内信通知 Store
 *
 * 二期：WebSocket 实时推送 + 轮询降级
 * - start() 优先建立 WebSocket 连接，连接失败或断开后自动降级为 30s 轮询
 * - WebSocket 在线时实时更新未读数，离线时轮询兜底
 * - 组件层接口不变（start/stop/readOne/readAll）
 */
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  /** 最近一条通知（供组件弹 toast） */
  const latestNotification = ref<NotificationItem | null>(null)

  let ws: WebSocket | null = null
  let pollTimer: ReturnType<typeof setInterval> | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  const MAX_RECONNECT = 5
  const RECONNECT_DELAY = 3000
  const POLL_INTERVAL = 30_000

  /** 拉取未读消息数 */
  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data || 0
    } catch { /* */ }
  }

  /** 启动通知（登录后调用） */
  function start() {
    fetchUnreadCount()
    connectWebSocket()
  }

  /** 停止（登出/卸载时调用） */
  function stop() {
    cleanupWebSocket()
    cleanupPolling()
    unreadCount.value = 0
    reconnectAttempts = 0
  }

  /** 建立 WebSocket 连接 */
  function connectWebSocket() {
    const userStore = useUserStore()
    if (!userStore.token) return

    const wsBaseUrl = import.meta.env.VITE_API_BASE_URL.replace(/^http/, 'ws')
    const wsUrl = `${wsBaseUrl}/ws/notification?token=${userStore.token}`

    try {
      ws = new WebSocket(wsUrl)
    } catch {
      fallbackToPolling()
      return
    }

    ws.onopen = () => {
      reconnectAttempts = 0
      // WebSocket 连接成功，停止轮询
      cleanupPolling()
    }

    ws.onmessage = (event) => {
      try {
        const data: NotificationItem = JSON.parse(event.data)
        unreadCount.value++
        latestNotification.value = data
      } catch { /* */ }
    }

    ws.onclose = () => {
      ws = null
      // 非主动关闭时尝试重连
      if (reconnectAttempts < MAX_RECONNECT) {
        reconnectTimer = setTimeout(() => {
          reconnectAttempts++
          connectWebSocket()
        }, RECONNECT_DELAY)
      } else {
        // 超过重连上限，降级为轮询
        fallbackToPolling()
      }
    }

    ws.onerror = () => {
      // onclose 会随后触发，重连逻辑在 onclose 中处理
    }
  }

  /** 降级为轮询模式 */
  function fallbackToPolling() {
    if (pollTimer) return
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, POLL_INTERVAL)
  }

  /** 清理 WebSocket */
  function cleanupWebSocket() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.onclose = null  // 阻止重连
      ws.close()
      ws = null
    }
  }

  /** 清理轮询 */
  function cleanupPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
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

  return { unreadCount, latestNotification, fetchUnreadCount, start, stop, readOne, readAll }
})
