/**
 * 日期时间格式化工具
 * 统一处理后端返回的 ISO 格式（如 2026-07-22T21:36:47）转换为可读格式
 */

/** 完整日期时间：2026-07-22 21:36:47 */
export function formatDateTime(t?: string | null): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

/** 仅日期：2026-07-22 */
export function formatDate(t?: string | null): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 10)
}

/** 仅时间：21:36 */
export function formatTime(t?: string | null): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(11, 16)
}

/**
 * 时间段智能显示：
 * - 同一天：2026-07-22 21:36 ~ 22:00（日期一次，时间范围紧凑）
 * - 跨天：2026-07-22 21:36 ~ 2026-07-23 10:00
 */
export function formatTimeRange(start?: string | null, end?: string | null): { date: string; range: string; full: string } {
  if (!start || !end) return { date: '', range: '', full: '' }
  const startDate = start.substring(0, 10)
  const endDate = end.substring(0, 10)
  const startTime = start.substring(11, 16)
  const endTime = end.substring(11, 16)
  if (startDate === endDate) {
    return {
      date: startDate,
      range: `${startTime} ~ ${endTime}`,
      full: `${startDate} ${startTime} ~ ${endTime}`
    }
  }
  return {
    date: '',
    range: '',
    full: `${startDate} ${startTime} ~ ${endDate} ${endTime}`
  }
}
