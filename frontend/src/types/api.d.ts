/** 统一 API 响应体 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
}

/** 分页请求参数 */
export interface PageQuery {
  page?: number
  size?: number
}

/** 分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}
