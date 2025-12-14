import {
  UserListDTO,
  UserSearchDTO,
  UserDetailDTO,
  UserManagementDTO,
  UserOperationHistoryDTO,
  UserBatchOperationDTO,
  BatchOperationResultDTO,
  UserStatisticsDTO
} from '@/types/userManagement'
import { client } from '@/api/client'

/**
 * 用户管理API服务
 */
export class UserManagementService {

  /**
   * 获取用户列表
   */
  static async getUserList(params: Partial<UserSearchDTO>): Promise<{
    content: UserListDTO[]
    totalElements: number
    totalPages: number
    size: number
    number: number
  }> {
    const response = await client.get('/v1/admin/users', { params })
    return response.data.data
  }

  /**
   * 高级用户搜索
   */
  static async searchUsers(searchData: UserSearchDTO): Promise<{
    content: UserListDTO[]
    totalElements: number
    totalPages: number
    size: number
    number: number
  }> {
    const response = await client.post('/v1/admin/users/search', searchData)
    return response.data.data
  }

  /**
   * 获取用户详情
   */
  static async getUserDetail(userId: number): Promise<UserDetailDTO> {
    const response = await client.get(`/v1/admin/users/${userId}`)
    return response.data.data
  }

  /**
   * 更新用户状态
   */
  static async updateUserStatus(userId: number, data: UserManagementDTO): Promise<UserListDTO> {
    const response = await client.put(`/v1/admin/users/${userId}/status`, data)
    return response.data.data
  }

  /**
   * 删除用户
   */
  static async deleteUser(userId: number, reason: string): Promise<void> {
    await client.delete(`/v1/admin/users/${userId}`, { params: { reason } })
  }

  /**
   * 批量更新用户状态
   */
  static async batchUpdateUserStatus(data: UserBatchOperationDTO): Promise<BatchOperationResultDTO> {
    const response = await client.put('/v1/admin/users/batch/status', data)
    return response.data.data
  }

  /**
   * 批量删除用户
   */
  static async batchDeleteUsers(data: UserBatchOperationDTO): Promise<BatchOperationResultDTO> {
    const response = await client.delete('/v1/admin/users/batch', { data })
    return response.data.data
  }

  /**
   * 获取用户操作历史
   */
  static async getUserOperationHistory(userId: number, page = 0, size = 20): Promise<{
    content: UserOperationHistoryDTO[]
    totalElements: number
    totalPages: number
    size: number
    number: number
  }> {
    const response = await client.get(`/v1/admin/users/${userId}/operations`, {
      params: { page, size }
    })
    return response.data.data
  }

  /**
   * 获取用户统计信息
   */
  static async getUserStatistics(): Promise<UserStatisticsDTO> {
    const response = await client.get('/v1/admin/users/statistics')
    return response.data.data
  }

  // ========== 辅助方法 ==========

  /**
   * 构建搜索参数
   */
  static buildSearchParams(filters: any, pagination: any, sorting: any): UserSearchDTO {
    return {
      keyword: filters.keyword || '',
      username: filters.username || '',
      email: filters.email || '',
      phone: filters.phone || '',
      role: filters.role || '',
      status: filters.status || '',
      registrationDateStart: filters.registrationDateRange?.[0] || undefined,
      registrationDateEnd: filters.registrationDateRange?.[1] || undefined,
      lastLoginDateStart: filters.lastLoginDateRange?.[0] || undefined,
      lastLoginDateEnd: filters.lastLoginDateRange?.[1] || undefined,
      page: pagination.page || 0,
      size: pagination.size || 20,
      sortBy: sorting.sortBy || 'createdAt',
      sortDirection: sorting.sortDirection || 'desc'
    }
  }

  /**
   * 格式化用户状态
   */
  static formatUserStatus(status: string): { text: string; type: string } {
    switch (status) {
      case 'ACTIVE':
        return { text: '正常', type: 'success' }
      case 'INACTIVE':
        return { text: '禁用', type: 'danger' }
      default:
        return { text: '未知', type: 'info' }
    }
  }

  /**
   * 格式化用户角色
   */
  static formatUserRole(role: string): { text: string; type: string } {
    switch (role) {
      case 'ADMIN':
        return { text: '管理员', type: 'warning' }
      case 'USER':
        return { text: '普通用户', type: 'primary' }
      default:
        return { text: '未知', type: 'info' }
    }
  }

  /**
   * 格式化操作类型
   */
  static formatOperationType(operation: string): string {
    const operationMap: Record<string, string> = {
      'LOGIN': '登录',
      'LOGOUT': '登出',
      'STATUS_CHANGE': '状态变更',
      'DELETE': '删除',
      'PASSWORD_CHANGE': '密码修改',
      'PROFILE_UPDATE': '资料更新'
    }
    return operationMap[operation] || operation
  }

  /**
   * 生成用户头像URL
   */
  static getUserAvatarUrl(avatar?: string): string {
    if (!avatar) {
      return '/src/assets/default-avatar.png'
    }
    if (avatar.startsWith('http')) {
      return avatar
    }
    // 如果是相对路径，添加基础URL
    return `${import.meta.env.VITE_API_BASE_URL}/uploads/${avatar}`
  }

  /**
   * 隐藏手机号中间四位
   */
  static maskPhone(phone?: string): string {
    if (!phone) return '-'
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
  }

  /**
   * 隐藏邮箱部分信息
   */
  static maskEmail(email?: string): string {
    if (!email) return '-'
    const [username, domain] = email.split('@')
    if (username.length <= 2) return email
    return `${username.substring(0, 2)}***@${domain}`
  }
}

export default UserManagementService