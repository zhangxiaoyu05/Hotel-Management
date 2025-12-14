import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  UserListDTO,
  UserSearchDTO,
  UserDetailDTO,
  UserOperationHistoryDTO,
  UserStatisticsDTO,
  SearchFilters,
  BatchOperationResultDTO
} from '@/types/userManagement'
import UserManagementService from '@/services/userManagementService'

export const useUserManagementStore = defineStore('userManagement', () => {
  // 状态定义
  const userList = ref<UserListDTO[]>([])
  const userDetail = ref<UserDetailDTO | null>(null)
  const operationHistory = ref<UserOperationHistoryDTO[]>([])
  const statistics = ref<UserStatisticsDTO | null>(null)
  const batchOperationResult = ref<BatchOperationResultDTO | null>(null)

  // 分页信息
  const pagination = ref({
    page: 0,
    size: 20,
    total: 0,
    totalPages: 0
  })

  // 搜索条件
  const searchFilters = ref<SearchFilters>({
    keyword: '',
    role: '',
    status: '',
    registrationDateRange: null,
    lastLoginDateRange: null
  })

  // 排序信息
  const sorting = ref({
    sortBy: 'createdAt',
    sortDirection: 'desc'
  })

  // 加载状态
  const loading = ref(false)
  const detailLoading = ref(false)
  const historyLoading = ref(false)
  const statisticsLoading = ref(false)
  const batchOperationLoading = ref(false)

  // 选中的用户ID列表
  const selectedUserIds = ref<number[]>([])

  // 是否全选
  const isAllSelected = computed(() => {
    return userList.value.length > 0 && selectedUserIds.value.length === userList.value.length
  })

  // 部分选中
  const isIndeterminate = computed(() => {
    return selectedUserIds.value.length > 0 && selectedUserIds.value.length < userList.value.length
  })

  // ========== Actions ==========

  /**
   * 获取用户列表
   */
  const fetchUserList = async (resetPage = false) => {
    try {
      loading.value = true
      if (resetPage) {
        pagination.value.page = 0
      }

      const searchParams = UserManagementService.buildSearchParams(
        searchFilters.value,
        pagination.value,
        sorting.value
      )

      const response = await UserManagementService.getUserList(searchParams)

      userList.value = response.content
      pagination.value.total = response.totalElements
      pagination.value.totalPages = response.totalPages
      pagination.value.size = response.size
      pagination.value.page = response.number

    } catch (error) {
      console.error('获取用户列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 搜索用户
   */
  const searchUsers = async (searchData: UserSearchDTO) => {
    try {
      loading.value = true
      const response = await UserManagementService.searchUsers(searchData)

      userList.value = response.content
      pagination.value.total = response.totalElements
      pagination.value.totalPages = response.totalPages
      pagination.value.size = response.size
      pagination.value.page = response.number

    } catch (error) {
      console.error('搜索用户失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取用户详情
   */
  const fetchUserDetail = async (userId: number) => {
    try {
      detailLoading.value = true
      const detail = await UserManagementService.getUserDetail(userId)
      userDetail.value = detail
      return detail
    } catch (error) {
      console.error('获取用户详情失败:', error)
      throw error
    } finally {
      detailLoading.value = false
    }
  }

  /**
   * 获取用户操作历史
   */
  const fetchUserOperationHistory = async (userId: number, page = 0, reset = false) => {
    try {
      historyLoading.value = true
      const response = await UserManagementService.getUserOperationHistory(userId, page, 20)

      if (reset || page === 0) {
        operationHistory.value = response.content
      } else {
        operationHistory.value.push(...response.content)
      }

      return response
    } catch (error) {
      console.error('获取用户操作历史失败:', error)
      throw error
    } finally {
      historyLoading.value = false
    }
  }

  /**
   * 获取用户统计信息
   */
  const fetchUserStatistics = async () => {
    try {
      statisticsLoading.value = true
      const stats = await UserManagementService.getUserStatistics()
      statistics.value = stats
      return stats
    } catch (error) {
      console.error('获取用户统计信息失败:', error)
      throw error
    } finally {
      statisticsLoading.value = false
    }
  }

  /**
   * 更新用户状态
   */
  const updateUserStatus = async (userId: number, newStatus: string, reason: string) => {
    try {
      const managementData = {
        userId,
        newStatus,
        reason,
        operatedAt: new Date().toISOString()
      }

      const updatedUser = await UserManagementService.updateUserStatus(userId, managementData)

      // 更新列表中的用户信息
      const index = userList.value.findIndex(user => user.id === userId)
      if (index !== -1) {
        userList.value[index] = { ...userList.value[index], ...updatedUser }
      }

      // 如果正在查看该用户详情，也更新详情信息
      if (userDetail.value?.id === userId) {
        userDetail.value.status = newStatus
      }

      return updatedUser
    } catch (error) {
      console.error('更新用户状态失败:', error)
      throw error
    }
  }

  /**
   * 删除用户
   */
  const deleteUser = async (userId: number, reason: string) => {
    try {
      await UserManagementService.deleteUser(userId, reason)

      // 从列表中移除用户
      userList.value = userList.value.filter(user => user.id !== userId)

      // 更新统计信息
      if (statistics.value) {
        statistics.value.totalUsers--
        if (userDetail.value?.status === 'ACTIVE') {
          statistics.value.activeUsers--
        } else {
          statistics.value.inactiveUsers--
        }
      }

      // 从选中列表中移除
      selectedUserIds.value = selectedUserIds.value.filter(id => id !== userId)

    } catch (error) {
      console.error('删除用户失败:', error)
      throw error
    }
  }

  /**
   * 批量更新用户状态
   */
  const batchUpdateUserStatus = async (operation: string, reason: string) => {
    try {
      batchOperationLoading.value = true

      const batchData = {
        userIds: selectedUserIds.value,
        operation,
        reason,
        operatedAt: new Date().toISOString()
      }

      const result = await UserManagementService.batchUpdateUserStatus(batchData)
      batchOperationResult.value = result

      // 更新列表中的用户状态
      userList.value = userList.value.map(user => {
        if (selectedUserIds.value.includes(user.id)) {
          return {
            ...user,
            status: operation === 'ENABLE' ? 'ACTIVE' : 'INACTIVE'
          }
        }
        return user
      })

      // 清空选中状态
      selectedUserIds.value = []

      return result
    } catch (error) {
      console.error('批量更新用户状态失败:', error)
      throw error
    } finally {
      batchOperationLoading.value = false
    }
  }

  /**
   * 批量删除用户
   */
  const batchDeleteUsers = async (reason: string) => {
    try {
      batchOperationLoading.value = true

      const batchData = {
        userIds: selectedUserIds.value,
        operation: 'DELETE',
        reason,
        operatedAt: new Date().toISOString()
      }

      const result = await UserManagementService.batchDeleteUsers(batchData)
      batchOperationResult.value = result

      // 从列表中移除已删除的用户
      userList.value = userList.value.filter(user => !selectedUserIds.value.includes(user.id))

      // 更新统计信息
      if (statistics.value) {
        statistics.value.totalUsers -= result.successCount
      }

      // 清空选中状态
      selectedUserIds.value = []

      return result
    } catch (error) {
      console.error('批量删除用户失败:', error)
      throw error
    } finally {
      batchOperationLoading.value = false
    }
  }

  // ========== 选择相关操作 ==========

  /**
   * 切换用户选中状态
   */
  const toggleUserSelection = (userId: number) => {
    const index = selectedUserIds.value.indexOf(userId)
    if (index === -1) {
      selectedUserIds.value.push(userId)
    } else {
      selectedUserIds.value.splice(index, 1)
    }
  }

  /**
   * 全选/取消全选
   */
  const toggleAllSelection = () => {
    if (isAllSelected.value) {
      selectedUserIds.value = []
    } else {
      selectedUserIds.value = userList.value.map(user => user.id)
    }
  }

  /**
   * 清空选中状态
   */
  const clearSelection = () => {
    selectedUserIds.value = []
  }

  // ========== 搜索和筛选 ==========

  /**
   * 更新搜索条件
   */
  const updateSearchFilters = (filters: Partial<SearchFilters>) => {
    searchFilters.value = { ...searchFilters.value, ...filters }
  }

  /**
   * 重置搜索条件
   */
  const resetSearchFilters = () => {
    searchFilters.value = {
      keyword: '',
      role: '',
      status: '',
      registrationDateRange: null,
      lastLoginDateRange: null
    }
  }

  /**
   * 更新排序
   */
  const updateSorting = (sortBy: string, sortDirection: string) => {
    sorting.value = { sortBy, sortDirection }
  }

  /**
   * 更新分页
   */
  const updatePagination = (page: number, size: number) => {
    pagination.value.page = page
    pagination.value.size = size
  }

  return {
    // 状态
    userList,
    userDetail,
    operationHistory,
    statistics,
    batchOperationResult,
    pagination,
    searchFilters,
    sorting,
    loading,
    detailLoading,
    historyLoading,
    statisticsLoading,
    batchOperationLoading,
    selectedUserIds,
    isAllSelected,
    isIndeterminate,

    // Actions
    fetchUserList,
    searchUsers,
    fetchUserDetail,
    fetchUserOperationHistory,
    fetchUserStatistics,
    updateUserStatus,
    deleteUser,
    batchUpdateUserStatus,
    batchDeleteUsers,
    toggleUserSelection,
    toggleAllSelection,
    clearSelection,
    updateSearchFilters,
    resetSearchFilters,
    updateSorting,
    updatePagination
  }
})