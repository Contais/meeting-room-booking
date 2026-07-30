export interface Attendee {
  userId: number
  username: string
  realName: string
  phone: string
  email: string
  departmentId?: number
  departmentName?: string
  /** 头像（presigned URL） */
  avatar?: string
  /** 查阅状态: 0-待查阅, 1-已查阅, 2-已拒绝 */
  status: number
}

export interface Reservation {
  id: number
  reservationCode: string
  roomId: number
  roomName: string
  userId: number
  username: string
  subject: string
  attendeeCount: number
  attendees?: Attendee[]
  remark: string
  startTime: string
  endTime: string
  status: number
  rejectReason?: string
  createTime: string
  updateTime?: string
  /** 当前用户对此预约的查阅状态（仅「我的会议」列表返回）: 0-待查阅, 1-已查阅, 2-已拒绝 */
  myAttendeeStatus?: number
}

export interface ReservationCreateParams {
  roomId: number
  subject: string
  /** 参会人用户ID列表 */
  attendeeUserIds?: number[]
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
