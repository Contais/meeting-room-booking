export interface RoomBrief {
  id: number
  name: string
  location: string
  quantity: number
}

export interface RoomEquipmentItem {
  roomId: number
  quantity: number
}

export interface Equipment {
  id: number
  code: string
  name: string
  category?: string
  brand?: string
  model?: string
  status: number
  purchaseDate?: string
  description?: string
  createTime: string
  quantity?: number
  rooms?: RoomBrief[]
}

export interface EquipmentPageQuery {
  page?: number
  size?: number
  keyword?: string
  name?: string
  category?: string
  brand?: string
  status?: number
  roomId?: number
  createTimeStart?: string
  createTimeEnd?: string
}

export interface EquipmentPageResult {
  records: Equipment[]
  total: number
  page: number
  size: number
}
