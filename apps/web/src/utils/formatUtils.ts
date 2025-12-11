/**
 * 数据格式化工具函数
 */

/**
 * 格式化货币
 */
export function formatCurrency(value: number | string, currency = '¥'): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return `${currency}0`
  return `${currency}${num.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
}

/**
 * 格式化百分比
 */
export function formatPercent(value: number | string, decimals = 1): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0%'
  return `${num.toFixed(decimals)}%`
}

/**
 * 格式化数字，添加千分位分隔符
 */
export function formatNumber(value: number | string): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0'
  return num.toLocaleString('zh-CN')
}

/**
 * 格式化日期
 */
export function formatDate(date: string | Date, format = 'YYYY-MM-DD'): string {
  const d = typeof date === 'string' ? new Date(date) : date
  if (isNaN(d.getTime())) return '--'

  const formatMap: Record<string, string> = {
    'YYYY': d.getFullYear().toString(),
    'MM': (d.getMonth() + 1).toString().padStart(2, '0'),
    'DD': d.getDate().toString().padStart(2, '0'),
    'HH': d.getHours().toString().padStart(2, '0'),
    'mm': d.getMinutes().toString().padStart(2, '0'),
    'ss': d.getSeconds().toString().padStart(2, '0')
  }

  return format.replace(/YYYY|MM|DD|HH|mm|ss/g, (match) => formatMap[match])
}

/**
 * 格式化时间
 */
export function formatTime(date: string | Date): string {
  return formatDate(date, 'HH:mm:ss')
}

/**
 * 格式化日期时间
 */
export function formatDateTime(date: string | Date): string {
  return formatDate(date, 'YYYY-MM-DD HH:mm:ss')
}

/**
 * 格式化相对时间（如：2小时前、3天前）
 */
export function formatRelativeTime(date: string | Date): string {
  const now = new Date()
  const target = typeof date === 'string' ? new Date(date) : date
  const diffMs = now.getTime() - target.getTime()

  const diffMinutes = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffMinutes < 1) {
    return '刚刚'
  } else if (diffMinutes < 60) {
    return `${diffMinutes}分钟前`
  } else if (diffHours < 24) {
    return `${diffHours}小时前`
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else {
    return formatDate(target)
  }
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 格式化手机号（隐藏中间4位）
 */
export function formatPhoneNumber(phone: string): string {
  if (!phone || phone.length < 11) return phone

  const start = phone.substring(0, 3)
  const end = phone.substring(7)
  return `${start}****${end}`
}

/**
 * 格式化邮箱地址
 */
export function formatEmail(email: string): string {
  if (!email || !email.includes('@')) return email

  const [username, domain] = email.split('@')
  const usernameLength = Math.min(3, username.length)
  const formattedUsername = username.substring(0, usernameLength)

  return `${formattedUsername}***@${domain}`
}

/**
 * 格式化状态文本
 */
export function formatStatus(status: string): string {
  const statusMap: Record<string, string> = {
    'PENDING': '待处理',
    'CONFIRMED': '已确认',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
    'AVAILABLE': '可用',
    'OCCUPIED': '已入住',
    'MAINTENANCE': '维护中',
    'CLEANING': '清洁中',
    'ACTIVE': '正常',
    'INACTIVE': '停用'
  }

  return statusMap[status] || status
}

/**
 * 格式化评分
 */
export function formatRating(rating: number, maxRating: number = 5): string {
  if (isNaN(rating) || rating < 0 || rating > maxRating) return '0.0'
  return rating.toFixed(1)
}

/**
 * 格式化订单状态
 */
export function formatOrderStatus(status: string): { text: string; type: string } {
  const statusMap: Record<string, { text: string; type: string }> = {
    'PENDING': { text: '待处理', type: 'warning' },
    'CONFIRMED': { text: '已确认', type: 'primary' },
    'COMPLETED': { text: '已完成', type: 'success' },
    'CANCELLED': { text: '已取消', type: 'danger' }
  }

  return statusMap[status] || { text: status, type: 'info' }
}

/**
 * 格式化房间状态
 */
export function formatRoomStatus(status: string): { text: string; type: string } {
  const statusMap: Record<string, { text: string; type: string }> = {
    'AVAILABLE': { text: '可用', type: 'success' },
    'OCCUPIED': { text: '已入住', type: 'info' },
    'MAINTENANCE': { text: '维护中', type: 'warning' },
    'CLEANING': { text: '清洁中', type: 'info' }
  }

  return statusMap[status] || { text: status, type: 'info' }
}

/**
 * 格式化用户角色
 */
export function formatUserRole(role: string): string {
  const roleMap: Record<string, string> = {
    'USER': '用户',
    'ADMIN': '管理员'
  }

  return roleMap[role] || role
}

/**
 * 格式化趋势数据
 */
export function formatTrendValue(value: number, isPercent: boolean = false): string {
  if (isPercent) {
    return `${value > 0 ? '+' : ''}${formatPercent(Math.abs(value))}`
  }

  return `${value > 0 ? '+' : ''}${formatNumber(value)}`
}

/**
 * 格式化大数字（如：1.2K, 1.5M）
 */
export function formatLargeNumber(value: number): string {
  if (Math.abs(value) < 1000) {
    return formatNumber(value)
  } else if (Math.abs(value) < 1000000) {
    return `${(value / 1000).toFixed(1)}K`
  } else if (Math.abs(value) < 1000000000) {
    return `${(value / 1000000).toFixed(1)}M`
  } else {
    return `${(value / 1000000000).toFixed(1)}B`
  }
}

/**
 * 格式化时长（秒转换为时分秒）
 */
export function formatDuration(seconds: number): string {
  if (!seconds || seconds < 0) return '0秒'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = seconds % 60

  if (hours > 0) {
    if (minutes > 0) {
      return `${hours}小时${minutes}分钟${remainingSeconds}秒`
    } else {
      return `${hours}小时${remainingSeconds}秒`
    }
  } else if (minutes > 0) {
    return `${minutes}分钟${remainingSeconds}秒`
  } else {
    return `${remainingSeconds}秒`
  }
}

/**
 * 截断文本，添加省略号
 */
export function truncateText(text: string, maxLength: number = 50): string {
  if (!text || text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}