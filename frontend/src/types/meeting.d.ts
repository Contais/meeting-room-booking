export interface MeetingRoom {
  id: number
  name: string
  location: string
  capacity: number
  equipment: string
  imageUrl: string
  description: string
  status: number
  bookableStart: string
  bookableEnd: string
  maxDuration: number
  advanceDays: number
  needApproval: number
  createTime: string
}

export interface MeetingRoomPageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

export interface MeetingRoomPageResult {
  records: MeetingRoom[]
  total: number
  page: number
  size: number
}

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
