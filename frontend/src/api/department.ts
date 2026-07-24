import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { Department } from '@/types/department'

export function getDepartmentTree(): Promise<Result<Department[]>> {
  return request.get('/api/uc/department/tree')
}

export function listDepartments(): Promise<Result<Department[]>> {
  return request.get('/api/uc/department/list')
}

export function createDepartment(data: { name: string; parentId?: number; sortOrder?: number }): Promise<Result<void>> {
  return request.post('/api/uc/department/admin/create', data)
}

export function updateDepartment(data: { id: number; name: string; parentId?: number; sortOrder?: number }): Promise<Result<void>> {
  return request.put('/api/uc/department/admin/update', data)
}

export function deleteDepartment(id: number): Promise<Result<void>> {
  return request.delete(`/api/uc/department/admin/delete/${id}`)
}
