export interface Department {
  id: string
  name: string
  parentId: string
  sortOrder: number
  status: number
  createTime: string
  children?: Department[]
}
