import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { MeetingRoom, MeetingRoomPageQuery, MeetingRoomPageResult } from '@/types/meeting'

// 公开接口
export function listActiveRooms(): Promise<Result<MeetingRoom[]>> {
  return request.get('/api/meeting/room/list')
}

export function getRoomById(id: string): Promise<Result<MeetingRoom>> {
  return request.get(`/api/meeting/room/${id}`)
}

// 管理接口
export function listRoomsAdmin(params: MeetingRoomPageQuery): Promise<Result<MeetingRoomPageResult>> {
  return request.get('/api/meeting/room/admin/list', { params })
}

export function getRoomDetailAdmin(id: string): Promise<Result<MeetingRoom>> {
  return request.get(`/api/meeting/room/admin/detail/${id}`)
}

export function createRoom(data: Partial<MeetingRoom>): Promise<Result<void>> {
  return request.post('/api/meeting/room/admin/create', data)
}

export function updateRoom(data: Partial<MeetingRoom>): Promise<Result<void>> {
  return request.put('/api/meeting/room/admin/update', data)
}

export function toggleRoomStatus(id: string): Promise<Result<void>> {
  return request.put(`/api/meeting/room/admin/toggle-status/${id}`)
}

export function deleteRoom(id: string): Promise<Result<void>> {
  return request.delete(`/api/meeting/room/admin/delete/${id}`)
}
