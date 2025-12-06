import request from '@/utils/request'
import type { User } from '@/types/user'

// API响应接口
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  success: boolean
  timestamp: number
}

// 分页响应接口
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 请求参数接口
export interface GetUsersParams {
  page: number
  size: number
  username?: string
  role?: string
  status?: string
}

export interface UpdateRoleRequest {
  role: 'USER' | 'ADMIN'
}

export interface UpdateStatusRequest {
  status: 'ACTIVE' | 'INACTIVE'
}

// 管理员API
export const adminApi = {
  // 获取用户列表
  getUsers(params: GetUsersParams): Promise<ApiResponse<PageResult<User>>> {
    return request.get('/v1/admin/users', { params })
  },

  // 获取用户详情
  getUser(id: number): Promise<ApiResponse<User>> {
    return request.get(`/v1/admin/users/${id}`)
  },

  // 更新用户角色
  updateUserRole(id: number, data: UpdateRoleRequest): Promise<ApiResponse<User>> {
    return request.put(`/v1/admin/users/${id}/role`, data)
  },

  // 更新用户状态
  updateUserStatus(id: number, data: UpdateStatusRequest): Promise<ApiResponse<User>> {
    return request.put(`/v1/admin/users/${id}/status`, data)
  },

  // 批量更新用户状态
  batchUpdateUserStatus(userIds: number[], status: string): Promise<ApiResponse<number>> {
    return request.put('/v1/admin/users/batch/status', { userIds }, { params: { status } })
  }
}