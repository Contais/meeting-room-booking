import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { UserInfo, UserPageQuery, UserPageResult } from '@/types/user'

export function getUserById(id: string): Promise<Result<UserInfo>> {
  return request.get(`/api/uc/user/${id}`)
}

export function getCurrentUser(): Promise<Result<UserInfo>> {
  return request.get('/api/uc/user/me')
}

export function updateProfile(data: { phone?: string; realName?: string; email?: string; avatar?: string }): Promise<Result<void>> {
  return request.put('/api/uc/user/me/profile', data)
}

export function changePassword(data: { oldPassword: string; newPassword: string }): Promise<Result<void>> {
  return request.put('/api/uc/user/me/password', data)
}

// 通讯录
export function listContacts(params?: { keyword?: string; departmentId?: string }): Promise<Result<UserInfo[]>> {
  return request.get('/api/uc/user/contacts', { params })
}

// 管理接口
export function listUsers(params: UserPageQuery): Promise<Result<UserPageResult>> {
  return request.get('/api/uc/user/admin/list', { params })
}

export function getUserDetail(id: string): Promise<Result<UserInfo>> {
  return request.get(`/api/uc/user/admin/detail/${id}`)
}

export function createUser(data: Partial<UserInfo> & { password: string }): Promise<Result<void>> {
  return request.post('/api/uc/user/admin/create', data)
}

export function updateUser(data: Partial<UserInfo>): Promise<Result<void>> {
  return request.put('/api/uc/user/admin/update', data)
}

export function toggleUserStatus(id: string): Promise<Result<void>> {
  return request.put(`/api/uc/user/admin/toggle-status/${id}`)
}

export function deleteUser(id: string): Promise<Result<void>> {
  return request.delete(`/api/uc/user/admin/delete/${id}`)
}

export function resetPassword(id: string, newPassword: string): Promise<Result<void>> {
  return request.put(`/api/uc/user/admin/reset-password/${id}`, { newPassword })
}

export function sendPasswordResetCode(username: string): Promise<Result<void>> {
  return request.post('/api/uc/user/forgot-password/send-code', { username })
}

export function resetPasswordByCode(data: { username: string; code: string; newPassword: string }): Promise<Result<void>> {
  return request.post('/api/uc/user/forgot-password/reset', data)
}
