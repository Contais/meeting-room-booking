import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { MenuItem } from '@/types/menu'

export function getMenuTree(): Promise<Result<MenuItem[]>> {
  return request.get('/api/uc/menu/tree')
}

export function getMyMenus(): Promise<Result<MenuItem[]>> {
  return request.get('/api/uc/menu/my')
}

export function createMenu(data: { name: string; path?: string; icon?: string; parentId?: number; sortOrder?: number; visible?: number }): Promise<Result<void>> {
  return request.post('/api/uc/menu/admin/create', data)
}

export function updateMenu(data: { id: number; name: string; path?: string; icon?: string; parentId?: number; sortOrder?: number; visible?: number }): Promise<Result<void>> {
  return request.put('/api/uc/menu/admin/update', data)
}

export function deleteMenu(id: number): Promise<Result<void>> {
  return request.delete(`/api/uc/menu/admin/delete/${id}`)
}

export function saveRoleMenus(role: string, menuIds: number[]): Promise<Result<void>> {
  return request.put('/api/uc/menu/admin/role-menus', { role, menuIds })
}
