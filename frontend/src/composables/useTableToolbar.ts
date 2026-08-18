import { ref } from 'vue'
import type { SortOrder, TableColumnOption } from '@/types/table'

export function useTableToolbar(columns: TableColumnOption[]) {
  const sortOrder = ref<SortOrder>('asc')
  const visibleColumns = ref<string[]>(columns.map((item) => item.key))

  function isColumnVisible(key: string) {
    return visibleColumns.value.includes(key)
  }

  return {
    sortOrder,
    visibleColumns,
    isColumnVisible
  }
}
