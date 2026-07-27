export interface Reservation {
  id: number
  reservationCode: string
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
  rejectReason?: string
  createTime: string
  updateTime?: string
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
  keyword?: string
  subject?: string
  roomId?: number
  userId?: number
  status?: number
  contactPhone?: string
  startTime?: string
  endTime?: string
  reservationCode?: string
  createTimeStart?: string
  createTimeEnd?: string
}

export interface ReservationPageResult {
  records: Reservation[]
  total: number
  page: number
  size: number
}
