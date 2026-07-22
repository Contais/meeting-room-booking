import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { UserInfo, UserPageQuery, UserPageResult } from '@/types/user'

export function getUserById(id: number): Promise<Result<UserInfo>> {
  return request.get(`/api/user/${id}`)
}

export function getCurrentUser(): Promise<Result<UserInfo>> {
  return request.get('/api/user/me')
}

export function updateProfile(data: { phone?: string; realName?: string }): Promise<Result<void>> {
  return request.put('/api/user/me/profile', data)
}

export function changePassword(data: { oldPassword: string; newPassword: string }): Promise<Result<void>> {
  return request.put('/api/user/me/password', data)
}

// 管理接口
export function listUsers(params: UserPageQuery): Promise<Result<UserPageResult>> {
  return request.get('/api/user/admin/list', { params })
}

export function getUserDetail(id: number): Promise<Result<UserInfo>> {
  return request.get(`/api/user/admin/detail/${id}`)
}

export function createUser(data: Partial<UserInfo> & { password: string }): Promise<Result<void>> {
  return request.post('/api/user/admin/create', data)
}

export function updateUser(data: Partial<UserInfo>): Promise<Result<void>> {
  return request.put('/api/user/admin/update', data)
}

export function toggleUserStatus(id: number): Promise<Result<void>> {
  return request.put(`/api/user/admin/toggle-status/${id}`)
}

export function deleteUser(id: number): Promise<Result<void>> {
  return request.delete(`/api/user/admin/delete/${id}`)
}
