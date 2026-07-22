export interface MeetingRoom {
  id: number
  name: string
  location: string
  capacity: number
  equipment: string
  imageUrl: string
  description: string
  status: number
  createTime: string
}

export interface MeetingRoomPageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
  minCapacity?: number
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
  userId: number
  startTime: string
  endTime: string
  status: number
  createTime: string
  updateTime: string
}
