// 日志类型
export type LogType = 'operation' | 'login' | 'error'

// 日志搜索参数
export interface LogSearchParams {
  username?: string
  startTime?: string | null
  endTime?: string | null
  ip?: string
  status?: string
  operation?: string
  level?: string
  module?: string
  loginType?: string
}

// 分页参数
export interface Pagination {
  page: number
  size: number
  total: number
}

// 操作日志
export interface OperationLog {
  id: number
  userId?: number
  username?: string
  operation?: string
  method?: string
  params?: string
  time?: number
  ip?: string
  userAgent?: string
  status?: string
  errorMessage?: string
  createTime?: string
}

// 登录日志
export interface LoginLog {
  id: number
  username?: string
  loginType?: string
  ip?: string
  location?: string
  browser?: string
  os?: string
  status?: string
  message?: string
  userAgent?: string
  sessionId?: string
  createTime?: string
}

// 错误日志
export interface ErrorLog {
  id: number
  exceptionType?: string
  message?: string
  stackTrace?: string
  className?: string
  methodName?: string
  fileName?: string
  lineNumber?: number
  url?: string
  params?: string
  ip?: string
  userAgent?: string
  userId?: number
  username?: string
  level?: string
  module?: string
  createTime?: string
}

// 日志导出请求
export interface LogExportRequest {
  logType: LogType
  exportFormat: 'excel' | 'csv'
  maxRecords?: number
  includeSensitiveInfo?: boolean
  username?: string
  operation?: string
  startTime?: string | null
  endTime?: string | null
  ip?: string
  status?: string
  level?: string
  module?: string
  loginType?: string
}

// API响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}