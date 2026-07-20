/** 会议室 */
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
  updateTime: string
}

/** 预约记录 */
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
