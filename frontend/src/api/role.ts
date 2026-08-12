import request from '@/utils/request'
import type { Result } from '@/types/api'

export interface RoleInfo {
  id: string
  roleCode: string
  roleName: string
  description?: string
  status: number
  isSystem: number
  sort: number
  createTime: string
  menuIds?: string[]
}

export interface RolePageQuery {
  pageNum: number
  pageSize: number
  keyword?: string
}

export interface RolePageResult {
  records: RoleInfo[]
  total: number
  size: number
  current: number
}

export function listRoles(params: RolePageQuery): Promise<Result<RolePageResult>> {
  return request.get('/api/uc/admin/role/page', { params })
}

export function listAllRoles(): Promise<Result<RoleInfo[]>> {
  return request.get('/api/uc/admin/role/list')
}

export function getRoleDetail(id: string): Promise<Result<RoleInfo>> {
  return request.get(`/api/uc/admin/role/${id}`)
}

export function createRole(data: { roleCode: string; roleName: string; description?: string; sort?: number }): Promise<Result<void>> {
  return request.post('/api/uc/admin/role/create', data)
}

export function updateRole(data: { id: string; roleName: string; description?: string; sort?: number }): Promise<Result<void>> {
  return request.put('/api/uc/admin/role/update', data)
}

export function deleteRole(id: string): Promise<Result<void>> {
  return request.delete(`/api/uc/admin/role/delete/${id}`)
}

export function toggleRoleStatus(id: string): Promise<Result<void>> {
  return request.put(`/api/uc/admin/role/toggle-status/${id}`)
}

export function assignRoleMenus(data: { roleId: string; menuIds: string[] }): Promise<Result<void>> {
  return request.put('/api/uc/admin/role/assign-menus', data)
}

export function getRoleMenuIds(id: string): Promise<Result<string[]>> {
  return request.get(`/api/uc/admin/role/${id}/menu-ids`)
}
