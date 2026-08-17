import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { KnowledgeEntry, KnowledgePageQuery, KnowledgePageResult } from '@/types/knowledge'

export function listKnowledge(params: KnowledgePageQuery): Promise<Result<KnowledgePageResult>> {
  return request.get('/api/platform/kb/admin/list', { params })
}

export function createKnowledge(data: {
  category: string
  title: string
  question: string
  answer: string
  tags?: string
  sort?: number
  status?: number
}): Promise<Result<void>> {
  return request.post('/api/platform/kb/admin/create', data)
}

export function updateKnowledge(data: {
  id: string
  category: string
  title: string
  question: string
  answer: string
  tags?: string
  sort?: number
  status?: number
}): Promise<Result<void>> {
  return request.put('/api/platform/kb/admin/update', data)
}

export function toggleKnowledgeStatus(id: string): Promise<Result<void>> {
  return request.put(`/api/platform/kb/admin/toggle-status/${id}`)
}

export function deleteKnowledge(id: string): Promise<Result<void>> {
  return request.delete(`/api/platform/kb/admin/delete/${id}`)
}

export type { KnowledgeEntry, KnowledgePageQuery, KnowledgePageResult }
