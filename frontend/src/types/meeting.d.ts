export interface MeetingRoom {
  id: string
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
  currentAvailable?: boolean
}

export interface MeetingRoomPageQuery {
  page?: number
  size?: number
  keyword?: string
  name?: string
  location?: string
  equipment?: string
  minCapacity?: number
  bookableStart?: string
  bookableEnd?: string
  needApproval?: number
  status?: number
  createTimeStart?: string
  createTimeEnd?: string
}

export interface MeetingRoomPageResult {
  records: MeetingRoom[]
  total: number
  page: number
  size: number
}
