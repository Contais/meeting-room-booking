import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { Reservation, ReservationCreateParams, ReservationPageQuery, ReservationPageResult } from '@/types/reservation'

export function createReservation(data: ReservationCreateParams): Promise<Result<void>> {
  return request.post('/api/meeting/reservation/create', data)
}

export function cancelReservation(id: number): Promise<Result<void>> {
  return request.put(`/api/meeting/reservation/cancel/${id}`)
}

export function listMyReservations(params: ReservationPageQuery): Promise<Result<ReservationPageResult>> {
  return request.get('/api/meeting/reservation/my', { params })
}

export function listByRoomAndDate(roomId: number, date: string): Promise<Result<Reservation[]>> {
  return request.get(`/api/meeting/reservation/room/${roomId}/date/${date}`)
}

// 管理接口
export function listAllReservations(params: ReservationPageQuery): Promise<Result<ReservationPageResult>> {
  return request.get('/api/meeting/reservation/admin/list', { params })
}

export function approveReservation(id: number): Promise<Result<void>> {
  return request.put(`/api/meeting/reservation/admin/approve/${id}`)
}

export function rejectReservation(id: number): Promise<Result<void>> {
  return request.put(`/api/meeting/reservation/admin/reject/${id}`)
}
