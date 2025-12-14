import { describe, it, expect, vi, beforeEach } from 'vitest'
import { userManagementService } from '@/services/userManagementService'
import type { UserManagementDTO, UserSearchCriteria, UserStatisticsDTO } from '@/types/userManagement'

// Mock API响应
const mockUsers: UserManagementDTO[] = [
  {
    id: 1,
    username: 'testuser1',
    email: 'test1@example.com',
    phoneNumber: '138****8001',
    status: 'ACTIVE',
    realName: '张*',
    idCard: '123456********5678',
    createdAt: '2024-01-01T10:00:00',
    lastLoginAt: '2024-01-01T10:00:00'
  },
  {
    id: 2,
    username: 'testuser2',
    email: 'test2@example.com',
    phoneNumber: '138****8002',
    status: 'INACTIVE',
    realName: '李*',
    idCard: '987654********4321',
    createdAt: '2024-01-02T10:00:00',
    lastLoginAt: '2024-01-02T10:00:00'
  }
]

const mockStatistics: UserStatisticsDTO = {
  totalUsers: 100,
  activeUsers: 80,
  inactiveUsers: 20,
  newUsersThisMonth: 10
}

// Mock fetch API
const mockFetch = vi.fn()
global.fetch = mockFetch

describe('userManagementService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('searchUsers', () => {
    it('should search users successfully', async () => {
      const criteria: UserSearchCriteria = {
        username: 'test',
        status: 'ACTIVE'
      }
      const mockResponse = {
        ok: true,
        json: vi.fn().mockResolvedValue({
          content: [mockUsers[0]],
          totalElements: 1,
          totalPages: 1,
          size: 10,
          number: 0
        })
      }
      mockFetch.mockResolvedValue(mockResponse)

      const result = await userManagementService.searchUsers(criteria, 0, 10)

      expect(mockFetch).toHaveBeenCalledWith('/api/admin/users/search?page=0&size=10', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(criteria)
      })
      expect(result).toEqual({
        content: [mockUsers[0]],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0
      })
    })

    it('should handle empty search results', async () => {
      const criteria: UserSearchCriteria = {}
      const mockResponse = {
        ok: true,
        json: vi.fn().mockResolvedValue({
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 10,
          number: 0
        })
      }
      mockFetch.mockResolvedValue(mockResponse)

      const result = await userManagementService.searchUsers(criteria, 0, 10)

      expect(result.content).toEqual([])
      expect(result.totalElements).toBe(0)
    })

    it('should throw error when API call fails', async () => {
      const criteria: UserSearchCriteria = {}
      mockFetch.mockRejectedValue(new Error('Network error'))

      await expect(userManagementService.searchUsers(criteria, 0, 10))
        .rejects.toThrow('Network error')
    })
  })

  describe('getUserById', () => {
    it('should get user by ID successfully', async () => {
      const mockResponse = {
        ok: true,
        json: vi.fn().mockResolvedValue(mockUsers[0])
      }
      mockFetch.mockResolvedValue(mockResponse)

      const result = await userManagementService.getUserById(1)

      expect(mockFetch).toHaveBeenCalledWith('/api/admin/users/1', {
        method: 'GET'
      })
      expect(result).toEqual(mockUsers[0])
    })

    it('should return null when user not found', async () => {
      const mockResponse = {
        ok: false,
        status: 404
      }
      mockFetch.mockResolvedValue(mockResponse)

      const result = await userManagementService.getUserById(999)

      expect(result).toBeNull()
    })
  })

  describe('updateUserStatus', () => {
    it('should update user status successfully', async () => {
      const mockResponse = {
        ok: true,
        json: vi.fn().mockResolvedValue({
          success: true,
          message: '用户状态更新成功'
        })
      }
      mockFetch.mockResolvedValue(mockResponse)

      const result = await userManagementService.updateUserStatus(1, 'INACTIVE')

      expect(mockFetch).toHaveBeenCalledWith('/api/admin/users/1/status', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status: 'INACTIVE' })
      })
      expect(result).toBe(true)
    })

    it('should return false when user not found', async () => {
      const mockResponse = {
        ok: true,
        json: vi.fn().mockResolvedValue({
          success: false,
          message: '用户未找到'
        })
      }
      mockFetch.mockResolvedValue(mockResponse)

      const result = await userManagementService.updateUserStatus(999, 'INACTIVE')

      expect(result).toBe(false)
    })
  })

  describe('getUserStatistics', () => {
    it('should get user statistics successfully', async () => {
      const mockResponse = {
        ok: true,
        json: vi.fn().mockResolvedValue(mockStatistics)
      }
      mockFetch.mockResolvedValue(mockResponse)

      const result = await userManagementService.getUserStatistics()

      expect(mockFetch).toHaveBeenCalledWith('/api/admin/users/statistics', {
        method: 'GET'
      })
      expect(result).toEqual(mockStatistics)
    })

    it('should handle API error gracefully', async () => {
      mockFetch.mockRejectedValue(new Error('API Error'))

      await expect(userManagementService.getUserStatistics())
        .rejects.toThrow('API Error')
    })
  })

  describe('batchUpdateUserStatus', () => {
    it('should batch update user status successfully', async () => {
      const userIds = [1, 2]
      const mockResponse = {
        ok: true,
        json: vi.fn().mockResolvedValue({
          success: true,
          message: '成功更新2个用户的状态',
          updatedCount: 2
        })
      }
      mockFetch.mockResolvedValue(mockResponse)

      const result = await userManagementService.batchUpdateUserStatus(userIds, 'INACTIVE')

      expect(mockFetch).toHaveBeenCalledWith('/api/admin/users/batch-status', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userIds,
          status: 'INACTIVE'
        })
      })
      expect(result).toBe(2)
    })

    it('should handle empty user list', async () => {
      const mockResponse = {
        ok: true,
        json: vi.fn().mockResolvedValue({
          success: true,
          message: '成功更新0个用户的状态',
          updatedCount: 0
        })
      }
      mockFetch.mockResolvedValue(mockResponse)

      const result = await userManagementService.batchUpdateUserStatus([], 'INACTIVE')

      expect(result).toBe(0)
    })
  })
})