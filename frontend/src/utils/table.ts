import type { SortOrder } from '@/types/table'

type SortableValue = string | number | null | undefined

function compareValues(a: SortableValue, b: SortableValue): number {
  if (a == null && b == null) return 0
  if (a == null) return 1
  if (b == null) return -1
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b))
}

/**
 * 对扁平列表做本地排序，返回新数组，不修改原数组。
 */
export function sortByProperty<T>(
  items: T[],
  order: SortOrder,
  getValue: (item: T) => SortableValue
): T[] {
  return [...items].sort((a, b) => {
    const diff = compareValues(getValue(a), getValue(b))
    return order === 'asc' ? diff : -diff
  })
}

/**
 * 对树形列表递归排序，返回新树，不修改原数据。
 */
export function sortTreeByProperty<T extends { children?: T[] }>(
  items: T[],
  order: SortOrder,
  getValue: (item: T) => SortableValue
): T[] {
  return [...items]
    .map((item) => item.children && item.children.length > 0
      ? { ...item, children: sortTreeByProperty(item.children, order, getValue) }
      : { ...item })
    .sort((a, b) => {
      const diff = compareValues(getValue(a), getValue(b))
      return order === 'asc' ? diff : -diff
    })
}
