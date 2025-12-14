import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserManagementStore } from '@/stores/userManagement'
import { userManagementService } from '@/services/userManagementService'
import type { UserManagementDTO, UserSearchCriteria, UserStatisticsDTO } from '@/types/userManagement'

// Mock the service
vi.mock('@/services/userManagementService')

const mockUserManagementService = vi.mocked(userManagementService)

describe('userManagementStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  const mockUser: UserManagementDTO = {
    id: 1,
    username: 'testuser1',
    email: 'test1@example.com',
    phoneNumber: '138****8001',
    status: 'ACTIVE',
    realName: '张*',
    idCard: '123456********5678',
    createdAt: '2024-01-01T10:00:00',
    lastLoginAt: '2024-01-01T10:00:00'
  }

  const mockStatistics: UserStatisticsDTO = {
    totalUsers: 100,
    activeUsers: 80,
    inactiveUsers: 20,
    newUsersThisMonth: 10
  }

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const store = useUserManagementStore()

      expect(store.users).toEqual([])
      expect(store.statistics).toEqual({
        totalUsers: 0,
        activeUsers: 0,
        inactiveUsers: 0,
        newUsersThisMonth: 0
      })
      expect(store.currentPage).toBe(1)
      expect(store.pageSize).toBe(10)
      expect(store.totalUsers).toBe(0)
      expect(store.searchCriteria).toEqual({})
      expect(store.loading).toBe(false)
      expect(store.error).toBe('')
    })
  })

  describe('searchUsers', () => {
    it('should search users and update state', async () => {
      const store = useUserManagementStore()
      const criteria: UserSearchCriteria = { username: 'test' }

      mockUserManagementService.searchUsers.mockResolvedValue({
        content: [mockUser],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0
      })

      await store.searchUsers(criteria)

      expect(store.loading).toBe(false)
      expect(store.users).toEqual([mockUser])
      expect(store.totalUsers).toBe(1)
      expect(store.searchCriteria).toEqual(criteria)
      expect(store.error).toBe('')
    })

    it('should handle search error', async () => {
      const store = useUserManagementStore()
      const criteria: UserSearchCriteria = { username: 'test' }

      mockUserManagementService.searchUsers.mockRejectedValue(new Error('Search failed'))

      await store.searchUsers(criteria)

      expect(store.loading).toBe(false)
      expect(store.users).toEqual([])
      expect(store.error).toBe('Search failed')
    })
  })

  describe('getUserById', () => {
    it('should get user by ID', async () => {
      const store = useUserManagementStore()

      mockUserManagementService.getUserById.mockResolvedValue(mockUser)

      const result = await store.getUserById(1)

      expect(result).toEqual(mockUser)
      expect(mockUserManagementService.getUserById).toHaveBeenCalledWith(1)
    })

    it('should return null when user not found', async () => {
      const store = useUserManagementStore()

      mockUserManagementService.getUserById.mockResolvedValue(null)

      const result = await store.getUserById(999)

      expect(result).toBeNull()
    })
  })

  describe('updateUserStatus', () => {
    it('should update user status and refresh list', async () => {
      const store = useUserManagementStore()
      store.users = [mockUser]

      mockUserManagementService.updateUserStatus.mockResolvedValue(true)

      const result = await store.updateUserStatus(1, 'INACTIVE')

      expect(result).toBe(true)
      expect(mockUserManagementService.updateUserStatus).toHaveBeenCalledWith(1, 'INACTIVE')
    })

    it('should handle update failure', async () => {
      const store = useUserManagementStore()

      mockUserManagementService.updateUserStatus.mockResolvedValue(false)

      const result = await store.updateUserStatus(1, 'INACTIVE')

      expect(result).toBe(false)
    })
  })

  describe('loadStatistics', () => {
    it('should load statistics', async () => {
      const store = useUserManagementStore()

      mockUserManagementService.getUserStatistics.mockResolvedValue(mockStatistics)

      await store.loadStatistics()

      expect(store.statistics).toEqual(mockStatistics)
      expect(mockUserManagementService.getUserStatistics).toHaveBeenCalled()
    })

    it('should handle statistics loading error', async () => {
      const store = useUserManagementStore()

      mockUserManagementService.getUserStatistics.mockRejectedValue(new Error('Stats failed'))

      await store.loadStatistics()

      expect(store.error).toBe('Stats failed')
    })
  })

  describe('batchUpdateUserStatus', () => {
    it('should batch update user status', async () => {
      const store = useUserManagementStore()

      mockUserManagementService.batchUpdateUserStatus.mockResolvedValue(2)

      const result = await store.batchUpdateUserStatus([1, 2], 'INACTIVE')

      expect(result).toBe(2)
      expect(mockUserManagementService.batchUpdateUserStatus).toHaveBeenCalledWith([1, 2], 'INACTIVE')
    })
  })

  describe('setSearchCriteria', () => {
    it('should set search criteria', () => {
      const store = useUserManagementStore()
      const criteria: UserSearchCriteria = { username: 'test', status: 'ACTIVE' }

      store.setSearchCriteria(criteria)

      expect(store.searchCriteria).toEqual(criteria)
    })
  })

  describe('setCurrentPage', () => {
    it('should set current page', () => {
      const store = useUserManagementStore()

      store.setCurrentPage(2)

      expect(store.currentPage).toBe(2)
    })
  })

  describe('clearError', () => {
    it('should clear error', () => {
      const store = useUserManagementStore()
      store.error = 'Test error'

      store.clearError()

      expect(store.error).toBe('')
    })
  })

  describe('reset', () => {
    it('should reset store to initial state', () => {
      const store = useUserManagementStore()
      store.users = [mockUser]
      store.statistics = mockStatistics
      store.currentPage = 2
      store.searchCriteria = { username: 'test' }
      store.error = 'Test error'

      store.$reset()

      expect(store.users).toEqual([])
      expect(store.statistics).toEqual({
        totalUsers: 0,
        activeUsers: 0,
        inactiveUsers: 0,
        newUsersThisMonth: 0
      })
      expect(store.currentPage).toBe(1)
      expect(store.searchCriteria).toEqual({})
      expect(store.error).toBe('')
    })
  })
})