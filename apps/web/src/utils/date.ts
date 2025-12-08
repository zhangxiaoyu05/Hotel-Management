/**
 * 日期工具函数
 */

/**
 * 格式化日期
 */
export function formatDate(dateString: string, format: 'YYYY-MM-DD' | 'MM-DD' | 'YYYY年MM月DD日' = 'YYYY-MM-DD'): string {
  if (!dateString) return ''

  const date = new Date(dateString)

  switch (format) {
    case 'YYYY-MM-DD':
      return date.toISOString().split('T')[0]
    case 'MM-DD':
      return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    case 'YYYY年MM月DD日':
      return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
    default:
      return date.toISOString().split('T')[0]
  }
}

/**
 * 格式化日期时间
 */
export function formatDateTime(dateString: string): string {
  if (!dateString) return ''

  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

/**
 * 获取相对时间
 */
export function getRelativeTime(dateString: string): string {
  if (!dateString) return ''

  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  return '刚刚'
}

/**
 * 检查日期是否在指定范围内
 */
export function isDateInRange(dateString: string, startDate: string, endDate: string): boolean {
  const date = new Date(dateString)
  const start = new Date(startDate)
  const end = new Date(endDate)

  return date >= start && date <= end
}

/**
 * 计算两个日期之间的天数
 */
export function getDaysBetween(startDate: string, endDate: string): number {
  const start = new Date(startDate)
  const end = new Date(endDate)
  const diffTime = Math.abs(end.getTime() - start.getTime())
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}