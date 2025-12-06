import { describe, it, expect, vi, beforeEach } from 'vitest'
import { authService } from '@/services/authService'
import { apiClient } from '@/utils/apiClient'
import type { CreateUserRequest, LoginRequest } from '@/services/authService'

// Mock apiClient
vi.mock('@/utils/apiClient', () => ({
  apiClient: {
    post: vi.fn(),
    get: vi.fn()
  }
}))

describe('AuthService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('register', () => {
    it('should successfully register a user', async () => {
      const userData: CreateUserRequest = {
        username: 'testuser',
        email: 'test@example.com',
        phone: '13800138000',
        password: 'Test123!@#'
      }

      const mockResponse = {
        data: {
          success: true,
          message: '注册成功',
          data: {
            user: {
              id: 1,
              username: 'testuser',
              email: 'test@example.com',
              phone: '13800138000',
              role: 'USER',
              status: 'ACTIVE'
            },
            token: 'mock-jwt-token'
          }
        }
      }

      ;(apiClient.post as any).mockResolvedValue(mockResponse)

      const result = await authService.register(userData)

      expect(apiClient.post).toHaveBeenCalledWith('/v1/auth/register', userData)
      expect(result).toEqual(mockResponse.data)
    })

    it('should handle registration error', async () => {
      const userData: CreateUserRequest = {
        username: 'testuser',
        email: 'test@example.com',
        phone: '13800138000',
        password: 'Test123!@#'
      }

      const mockError = {
        response: {
          data: {
            message: '用户名已存在'
          }
        }
      }

      ;(apiClient.post as any).mockRejectedValue(mockError)

      await expect(authService.register(userData)).rejects.toThrow('用户名已存在')
    })

    it('should handle network error', async () => {
      const userData: CreateUserRequest = {
        username: 'testuser',
        email: 'test@example.com',
        phone: '13800138000',
        password: 'Test123!@#'
      }

      ;(apiClient.post as any).mockRejectedValue(new Error('Network Error'))

      await expect(authService.register(userData)).rejects.toThrow('注册失败，请稍后重试')
    })
  })

  describe('login', () => {
    it('should successfully login a user', async () => {
      const credentials: LoginRequest = {
        identifier: 'testuser',
        password: 'Test123!@#'
      }

      const mockResponse = {
        data: {
          success: true,
          message: '登录成功',
          data: {
            user: {
              id: 1,
              username: 'testuser',
              email: 'test@example.com',
              phone: '13800138000',
              role: 'USER',
              status: 'ACTIVE'
            },
            token: 'mock-jwt-token'
          }
        }
      }

      ;(apiClient.post as any).mockResolvedValue(mockResponse)

      const result = await authService.login(credentials)

      expect(apiClient.post).toHaveBeenCalledWith('/v1/auth/login', credentials)
      expect(result).toEqual(mockResponse.data)
    })

    it('should handle login error', async () => {
      const credentials: LoginRequest = {
        identifier: 'testuser',
        password: 'wrongpassword'
      }

      const mockError = {
        response: {
          data: {
            message: '用户名或密码错误'
          }
        }
      }

      ;(apiClient.post as any).mockRejectedValue(mockError)

      await expect(authService.login(credentials)).rejects.toThrow('用户名或密码错误')
    })
  })

  describe('logout', () => {
    it('should successfully logout', async () => {
      const mockResponse = {
        data: {
          success: true,
          message: '登出成功'
        }
      }

      ;(apiClient.post as any).mockResolvedValue(mockResponse)

      await authService.logout()

      expect(apiClient.post).toHaveBeenCalledWith('/v1/auth/logout')
    })
  })

  describe('refreshToken', () => {
    it('should successfully refresh token', async () => {
      const mockResponse = {
        data: {
          success: true,
          message: 'Token刷新成功',
          data: {
            user: {
              id: 1,
              username: 'testuser',
              email: 'test@example.com',
              phone: '13800138000',
              role: 'USER',
              status: 'ACTIVE'
            },
            token: 'new-mock-jwt-token'
          }
        }
      }

      ;(apiClient.post as any).mockResolvedValue(mockResponse)

      const result = await authService.refreshToken()

      expect(apiClient.post).toHaveBeenCalledWith('/v1/auth/refresh')
      expect(result).toEqual(mockResponse.data)
    })
  })

  describe('getCurrentUser', () => {
    it('should successfully get current user', async () => {
      const mockResponse = {
        data: {
          success: true,
          data: {
            id: 1,
            username: 'testuser',
            email: 'test@example.com',
            phone: '13800138000',
            role: 'USER',
            status: 'ACTIVE'
          }
        }
      }

      ;(apiClient.get as any).mockResolvedValue(mockResponse)

      const result = await authService.getCurrentUser()

      expect(apiClient.get).toHaveBeenCalledWith('/v1/auth/me')
      expect(result).toEqual(mockResponse.data)
    })
  })
})