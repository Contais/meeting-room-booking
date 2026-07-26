export interface UserInfo {
  id: number
  username: string
  phone: string
  realName: string
  role: string
  status: number
  createTime: string
  departmentId?: number
  departmentName?: string
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
