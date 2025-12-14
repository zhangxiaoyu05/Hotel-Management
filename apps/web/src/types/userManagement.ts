// 用户管理相关类型定义

export interface UserListDTO {
  id: number
  username: string
  email: string
  phone: string
  nickname: string
  avatar: string
  role: string
  status: string
  registrationDate: string
  lastLoginAt?: string
  totalOrders: number
  totalReviews: number
  isDeleted: boolean
  createdAt: string
  updatedAt: string
}

export interface UserSearchDTO {
  username?: string
  email?: string
  phone?: string
  role?: string
  status?: string
  registrationDateStart?: string
  registrationDateEnd?: string
  lastLoginDateStart?: string
  lastLoginDateEnd?: string
  page: number
  size: number
  sortBy: string
  sortDirection: string
  keyword?: string
}

export interface UserDetailDTO {
  id: number
  username: string
  email: string
  phone: string
  nickname: string
  avatar: string
  realName?: string
  gender?: string
  birthDate?: string
  role: string
  status: string
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
  lastLoginIp?: string
  totalOrders: number
  totalReviews: number
  totalSpent: number
  averageRating: number
  memberLevel: string
}

export interface UserManagementDTO {
  userId: number
  newStatus: string
  reason: string
  operatedBy?: number
  operatedAt?: string
}

export interface UserOperationHistoryDTO {
  id: number
  userId: number
  operation: string
  operator: number
  operationTime: string
  details: string
  ipAddress?: string
  userAgent?: string
  operatorUsername?: string
}

export interface UserBatchOperationDTO {
  userIds: number[]
  operation: string
  reason?: string
  operatedBy?: number
  operatedAt?: string
}

export interface BatchOperationResultDTO {
  operation: string
  totalCount: number
  successCount: number
  failureCount: number
  successUserIds: number[]
  failureReasons: Record<number, string>
  operatedAt: string
  operatedBy?: number
  operatorUsername?: string
  isCompleted: boolean
  status: string
}

export interface UserStatisticsDTO {
  totalUsers: number
  activeUsers: number
  inactiveUsers: number
  adminUsers: number
  regularUsers: number
  newUsersToday: number
  newUsersThisMonth: number
  newUsersThisYear: number
  activeRate: number
  growthRateToday: number
  growthRateThisMonth: number
  growthRateThisYear: number
}

// 用户状态枚举
export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE'
}

// 用户角色枚举
export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN'
}

// 操作类型枚举
export enum OperationType {
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  STATUS_CHANGE = 'STATUS_CHANGE',
  DELETE = 'DELETE',
  PASSWORD_CHANGE = 'PASSWORD_CHANGE',
  PROFILE_UPDATE = 'PROFILE_UPDATE'
}

// 批量操作类型枚举
export enum BatchOperationType {
  ENABLE = 'ENABLE',
  DISABLE = 'DISABLE',
  DELETE = 'DELETE'
}

// 搜索条件接口
export interface SearchFilters {
  keyword: string
  role: string
  status: string
  registrationDateRange: [string, string] | null
  lastLoginDateRange: [string, string] | null
}

// 表格列配置
export interface TableColumn {
  prop: string
  label: string
  width?: number
  minWidth?: number
  sortable?: boolean
  fixed?: 'left' | 'right'
}