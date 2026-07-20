export interface UserInfo {
  id: number
  username: string
  phone: string
  realName: string
  role: string
  status: number
  createTime: string
}

export interface UserPageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

export interface UserPageResult {
  records: UserInfo[]
  total: number
  page: number
  size: number
}
