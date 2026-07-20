import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { UserInfo } from '@/types/user'

export function getUserById(id: number): Promise<Result<UserInfo>> {
  return request.get(`/api/user/${id}`)
}
