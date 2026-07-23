import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { Department } from '@/types/department'

export function getDepartmentTree(): Promise<Result<Department[]>> {
  return request.get('/api/department/tree')
}

export function listDepartments(): Promise<Result<Department[]>> {
  return request.get('/api/department/list')
}

export function createDepartment(data: { name: string; parentId?: number; sortOrder?: number }): Promise<Result<void>> {
  return request.post('/api/department/admin/create', data)
}

export function updateDepartment(data: { id: number; name: string; parentId?: number; sortOrder?: number }): Promise<Result<void>> {
  return request.put('/api/department/admin/update', data)
}

export function deleteDepartment(id: number): Promise<Result<void>> {
  return request.delete(`/api/department/admin/delete/${id}`)
}
