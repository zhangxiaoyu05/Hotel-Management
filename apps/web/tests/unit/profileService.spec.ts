import { describe, it, expect, vi, beforeEach } from 'vitest'
import { profileService } from '@/services/profileService'
import { apiClient } from '@/utils/apiClient'
import { client } from '@/api/client'
import type { User, UpdateProfileRequest, ChangePasswordRequest } from '@/types/user'

// Mock dependencies
vi.mock('@/utils/apiClient', () => ({
  apiClient: {
    post: vi.fn()
  }
}))

vi.mock('@/api/client', () => ({
  client: {
    get: vi.fn(),
    put: vi.fn()
  }
}))

describe('profileService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getCurrentUser', () => {
    it('should get current user successfully', async () => {
      const mockUser: User = {
        id: 1,
        username: 'testuser',
        email: 'test@example.com',
        phone: '13800138000',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        nickname: '测试用户'
      }

      const mockResponse = {
        success: true,
        message: '获取用户信息成功',
        data: mockUser
      }

      ;(client.get as any).mockResolvedValue(mockResponse)

      const result = await profileService.getCurrentUser()

      expect(client.get).toHaveBeenCalledWith('/users/me')
      expect(result).toEqual(mockUser)
    })

    it('should throw error when getting current user fails', async () => {
      const mockResponse = {
        success: false,
        message: '用户不存在'
      }

      ;(client.get as any).mockResolvedValue(mockResponse)

      await expect(profileService.getCurrentUser()).rejects.toThrow('用户不存在')
    })
  })

  describe('updateProfile', () => {
    it('should update profile successfully', async () => {
      const updateData: UpdateProfileRequest = {
        nickname: '新昵称',
        email: 'new@example.com'
      }

      const mockUpdatedUser: User = {
        id: 1,
        username: 'testuser',
        email: 'new@example.com',
        phone: '13800138000',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        nickname: '新昵称'
      }

      const mockResponse = {
        success: true,
        message: '更新用户信息成功',
        data: mockUpdatedUser
      }

      ;(client.put as any).mockResolvedValue(mockResponse)

      const result = await profileService.updateProfile(updateData)

      expect(client.put).toHaveBeenCalledWith('/users/me', updateData)
      expect(result).toEqual(mockUpdatedUser)
    })

    it('should throw error when updating profile fails', async () => {
      const updateData: UpdateProfileRequest = {
        nickname: '新昵称'
      }

      const mockResponse = {
        success: false,
        message: '邮箱已被其他用户使用'
      }

      ;(client.put as any).mockResolvedValue(mockResponse)

      await expect(profileService.updateProfile(updateData)).rejects.toThrow('邮箱已被其他用户使用')
    })
  })

  describe('changePassword', () => {
    it('should change password successfully', async () => {
      const passwordData: ChangePasswordRequest = {
        currentPassword: 'oldPassword',
        newPassword: 'newPassword',
        confirmPassword: 'newPassword'
      }

      const mockResponse = {
        success: true,
        message: '密码修改成功',
        data: null
      }

      ;(client.put as any).mockResolvedValue(mockResponse)

      await expect(profileService.changePassword(passwordData)).resolves.toBeUndefined()

      expect(client.put).toHaveBeenCalledWith('/users/me/password', passwordData)
    })

    it('should throw error when changing password fails', async () => {
      const passwordData: ChangePasswordRequest = {
        currentPassword: 'wrongPassword',
        newPassword: 'newPassword',
        confirmPassword: 'newPassword'
      }

      const mockResponse = {
        success: false,
        message: '当前密码不正确'
      }

      ;(client.put as any).mockResolvedValue(mockResponse)

      await expect(profileService.changePassword(passwordData)).rejects.toThrow('当前密码不正确')
    })
  })

  describe('uploadAvatar', () => {
    it('should upload avatar successfully', async () => {
      const file = new File(['content'], 'avatar.jpg', { type: 'image/jpeg' })
      const expectedUrl = 'http://localhost:8080/api/files/upload-url/avatar.jpg'

      const mockResponse = {
        data: {
          success: true,
          message: '文件上传成功',
          data: {
            url: expectedUrl
          }
        }
      }

      ;(apiClient.post as any).mockResolvedValue(mockResponse)

      const result = await profileService.uploadAvatar(file)

      expect(apiClient.post).toHaveBeenCalledWith(
        '/files/upload',
        expect.any(FormData),
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )
      expect(result).toBe(expectedUrl)
    })

    it('should throw error when uploading avatar fails', async () => {
      const file = new File(['content'], 'avatar.jpg', { type: 'image/jpeg' })

      const mockResponse = {
        data: {
          success: false,
          message: '文件格式不支持'
        }
      }

      ;(apiClient.post as any).mockResolvedValue(mockResponse)

      await expect(profileService.uploadAvatar(file)).rejects.toThrow('文件格式不支持')
    })

    it('should create FormData with correct file', async () => {
      const file = new File(['content'], 'avatar.jpg', { type: 'image/jpeg' })
      const expectedUrl = 'http://localhost:8080/api/files/upload-url/avatar.jpg'

      const mockResponse = {
        data: {
          success: true,
          message: '文件上传成功',
          data: {
            url: expectedUrl
          }
        }
      }

      ;(apiClient.post as any).mockImplementation((url, data) => {
        // Verify FormData
        expect(data).toBeInstanceOf(FormData)
        expect(data.get('file')).toBe(file)
        return Promise.resolve(mockResponse)
      })

      await profileService.uploadAvatar(file)

      expect(apiClient.post).toHaveBeenCalled()
    })
  })
})