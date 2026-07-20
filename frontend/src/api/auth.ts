import request from '@/utils/request'
import type { Result } from '@/types/api'

export interface LoginParams {
  username: string
  password: string
}

export function login(data: LoginParams): Promise<Result<string>> {
  return request.post('/api/auth/login', data)
}

export function refreshToken(token: string): Promise<Result<string>> {
  return request.post('/api/auth/refresh', null, { params: { token } })
}
