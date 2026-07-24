export interface MenuItem {
  id: number
  name: string
  path: string
  icon: string
  parentId: number
  sortOrder: number
  visible: number
  status: number
  createTime: string
  children?: MenuItem[]
}
