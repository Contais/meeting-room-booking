export interface KnowledgeEntry {
  id: string
  category: string
  categoryName: string
  title: string
  question: string
  answer: string
  tags?: string
  sort: number
  status: number
  createTime: string
  updateTime?: string
}

export interface KnowledgePageQuery {
  page?: number
  size?: number
  keyword?: string
  category?: string
  status?: number
}

export interface KnowledgePageResult {
  records: KnowledgeEntry[]
  total: number
  page: number
  size: number
}
