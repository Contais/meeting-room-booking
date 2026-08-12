export interface Attendee {
  userId: string
  username: string
  realName: string
  phone: string
  email: string
  departmentId?: string
  departmentName?: string
  /** 头像（presigned URL） */
  avatar?: string
  /** 查阅状态: 0-待查阅, 1-已查阅, 2-已拒绝 */
  status: number
  /** 邀请时间（attendee 记录创建时间，用于列表排序与展示） */
  createTime?: string
}

export interface Reservation {
  id: string
  reservationCode: string
  roomId: string
  roomName: string
  userId: string
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
  roomId: string
  subject: string
  /** 参会人用户ID列表 */
  attendeeUserIds?: string[]
  remark?: string
  startTime: string
  endTime: string
}

export interface ReservationPageQuery {
  page?: number
  size?: number
  keyword?: string
  subject?: string
  roomId?: string
  userId?: string
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
