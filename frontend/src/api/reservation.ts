import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { Reservation, ReservationCreateParams, ReservationPageQuery, ReservationPageResult } from '@/types/reservation'

export function createReservation(data: ReservationCreateParams): Promise<Result<string>> {
  return request.post('/api/meeting/reservation/create', data)
}

export function cancelReservation(id: number): Promise<Result<void>> {
  return request.put(`/api/meeting/reservation/cancel/${id}`)
}

export function deleteReservation(id: number): Promise<Result<void>> {
  return request.delete(`/api/meeting/reservation/${id}`)
}

export function listMyReservations(params: ReservationPageQuery): Promise<Result<ReservationPageResult>> {
  return request.get('/api/meeting/reservation/my', { params })
}

export function listMyMeetings(params: ReservationPageQuery): Promise<Result<ReservationPageResult>> {
  return request.get('/api/meeting/reservation/my-meetings', { params })
}

export function listByRoomAndDate(roomId: number, date: string): Promise<Result<Reservation[]>> {
  return request.get(`/api/meeting/reservation/room/${roomId}/date/${date}`)
}

export function getMyReservationDetail(id: number): Promise<Result<Reservation>> {
  return request.get(`/api/meeting/reservation/detail/${id}`)
}

// 管理接口
export function listAllReservations(params: ReservationPageQuery): Promise<Result<ReservationPageResult>> {
  return request.get('/api/meeting/reservation/admin/list', { params })
}

export function getReservationDetail(id: number): Promise<Result<Reservation>> {
  return request.get(`/api/meeting/reservation/admin/detail/${id}`)
}

export function approveReservation(id: number): Promise<Result<void>> {
  return request.put(`/api/meeting/reservation/admin/approve/${id}`)
}

export function rejectReservation(id: number, reason?: string): Promise<Result<void>> {
  return request.put(`/api/meeting/reservation/admin/reject/${id}`, { reason: reason || '' })
}

export function adminDeleteReservation(id: number): Promise<Result<void>> {
  return request.delete(`/api/meeting/reservation/admin/${id}`)
}

export function getSchedule(params: { date?: string; startDate?: string; endDate?: string }): Promise<Result<any>> {
  return request.get('/api/meeting/reservation/schedule', { params })
}
