export interface UserInfo {
  id: string
  username: string
  phone: string
  realName: string
  role: string
  status: number
  createTime: string
  updateTime?: string
  departmentId?: string
  departmentName?: string
  email?: string
  avatar?: string
}

export interface UserPageQuery {
  page?: number
  size?: number
  keyword?: string
  username?: string
  phone?: string
  status?: number
  createTimeStart?: string
  createTimeEnd?: string
}

export interface UserPageResult {
  records: UserInfo[]
  total: number
  page: number
  size: number
}
