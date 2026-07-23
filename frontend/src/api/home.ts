import request from '@/utils/request'
import type { Result } from '@/types/api'

export interface HomeStats {
  roomCount: number
  todayReservations: number
  pendingApproval: number
  weekReservations: number
  totalReservations: number
}

export function getHomeStats(): Promise<Result<HomeStats>> {
  return request.get('/api/meeting/home/stats')
}
