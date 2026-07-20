import request from '@/utils/request'
import type { Result } from '@/types/api'

export function login(username: string, password: string): Promise<Result<string>> {
  return request.post('/api/auth/login', null, { params: { username, password } })
}

export function refreshToken(token: string): Promise<Result<string>> {
  return request.post('/api/auth/refresh', null, { params: { token } })
}
