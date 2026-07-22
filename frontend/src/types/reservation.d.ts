export interface Reservation {
  id: number
  roomId: number
  roomName: string
  userId: number
  username: string
  subject: string
  attendeeCount: number
  contactPhone: string
  remark: string
  startTime: string
  endTime: string
  status: number
  createTime: string
}

export interface ReservationCreateParams {
  roomId: number
  subject: string
  attendeeCount?: number
  contactPhone?: string
  remark?: string
  startTime: string
  endTime: string
}

export interface ReservationPageQuery {
  page?: number
  size?: number
  roomId?: number
  userId?: number
  status?: number
}

export interface ReservationPageResult {
  records: Reservation[]
  total: number
  page: number
  size: number
}
