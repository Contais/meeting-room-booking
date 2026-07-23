export interface Department {
  id: number
  name: string
  parentId: number
  sortOrder: number
  status: number
  createTime: string
  children?: Department[]
}
