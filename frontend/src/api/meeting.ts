import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { MeetingRoom, Reservation } from '@/types/meeting'

export function getRoomById(id: number): Promise<Result<MeetingRoom>> {
  return request.get(`/api/meeting/room/${id}`)
}

export function listRooms(): Promise<Result<MeetingRoom[]>> {
  return request.get('/api/meeting/room/list')
}

export function addRoom(room: Partial<MeetingRoom>): Promise<Result<void>> {
  return request.post('/api/meeting/room/add', room)
}

export function getReservationById(id: number): Promise<Result<Reservation>> {
  return request.get(`/api/meeting/reservation/${id}`)
}

export function listReservationsByRoom(roomId: number): Promise<Result<Reservation[]>> {
  return request.get(`/api/meeting/reservation/list/${roomId}`)
}

export function createReservation(reservation: Partial<Reservation>): Promise<Result<void>> {
  return request.post('/api/meeting/reservation/create', reservation)
}
