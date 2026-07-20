import request from '@/utils/request'
import type { Result } from '@/types/api'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  userId: number
  username: string
  role: string
}

export function login(data: LoginParams): Promise<Result<LoginVO>> {
  return request.post('/api/auth/login', data)
}

export function refreshToken(token: string): Promise<Result<string>> {
  return request.post('/api/auth/refresh', { token })
}

export function logout(): Promise<Result<void>> {
  return request.post('/api/auth/logout')
}
