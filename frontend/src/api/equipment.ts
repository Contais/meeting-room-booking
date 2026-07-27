import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { Equipment, EquipmentPageQuery, EquipmentPageResult, RoomBrief, RoomEquipmentItem } from '@/types/equipment'

// 设备管理接口
export function listEquipments(params: EquipmentPageQuery): Promise<Result<EquipmentPageResult>> {
  return request.get('/api/meeting/equipment/admin/list', { params })
}

export function getEquipmentDetail(id: number): Promise<Result<Equipment>> {
  return request.get(`/api/meeting/equipment/admin/detail/${id}`)
}

export function listActiveEquipments(): Promise<Result<Equipment[]>> {
  return request.get('/api/meeting/equipment/admin/active')
}

export function listEquipmentsByRoom(roomId: number): Promise<Result<Equipment[]>> {
  return request.get(`/api/meeting/equipment/admin/room/${roomId}`)
}

export function createEquipment(data: Partial<Equipment> & { rooms?: RoomEquipmentItem[] }): Promise<Result<void>> {
  return request.post('/api/meeting/equipment/admin/create', data)
}

export function updateEquipment(data: Partial<Equipment>): Promise<Result<void>> {
  return request.put('/api/meeting/equipment/admin/update', data)
}

export function toggleEquipmentStatus(id: number): Promise<Result<void>> {
  return request.put(`/api/meeting/equipment/admin/toggle-status/${id}`)
}

export function deleteEquipment(id: number): Promise<Result<void>> {
  return request.delete(`/api/meeting/equipment/admin/delete/${id}`)
}

export function assignRooms(equipmentId: number, rooms: RoomEquipmentItem[]): Promise<Result<void>> {
  return request.put(`/api/meeting/equipment/admin/${equipmentId}/assign-rooms`, rooms)
}

export function assignEquipments(roomId: number, equipmentIds: number[]): Promise<Result<void>> {
  return request.put(`/api/meeting/equipment/admin/room/${roomId}/assign-equipments`, equipmentIds)
}

export type { Equipment, RoomBrief, RoomEquipmentItem }

