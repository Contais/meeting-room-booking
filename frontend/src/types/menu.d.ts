export interface MenuItem {
  id: string
  name: string
  path: string
  icon: string
  parentId: string
  sortOrder: number
  visible: number
  status: number
  createTime: string
  children?: MenuItem[]
}
