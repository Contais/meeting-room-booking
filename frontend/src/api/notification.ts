import request from '@/utils/request'
import type { Result } from '@/types/api'

export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  refType?: string
  refId?: number
  isRead: number
  createTime: string
}

export interface NotificationPageResult {
  records: NotificationItem[]
  total: number
  page: number
  size: number
}

export function getNotifications(params: {
  page?: number
  size?: number
  type?: string
  isRead?: number
}): Promise<Result<NotificationPageResult>> {
  return request.get('/api/platform/notification/page', { params })
}

export function getUnreadCount(): Promise<Result<number>> {
  return request.get('/api/platform/notification/unread-count')
}

export function markAsRead(id: number): Promise<Result<void>> {
  return request.post(`/api/platform/notification/read/${id}`)
}

export function markAllAsRead(): Promise<Result<void>> {
  return request.post('/api/platform/notification/read-all')
}

export function deleteNotification(id: number): Promise<Result<void>> {
  return request.delete(`/api/platform/notification/${id}`)
}
