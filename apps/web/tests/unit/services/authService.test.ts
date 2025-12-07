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
    it('should successfully login with username', async () => {
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
            token: 'mock-jwt-token',
            accessToken: 'mock-access-token',
            refreshToken: 'mock-refresh-token',
            expiresIn: 86400
          }
        }
      }

      ;(apiClient.post as any).mockResolvedValue(mockResponse)

      const result = await authService.login(credentials)

      expect(apiClient.post).toHaveBeenCalledWith('/v1/auth/login', credentials)
      expect(result).toEqual(mockResponse.data)
    })

    it('should successfully login with email', async () => {
      const credentials: LoginRequest = {
        identifier: 'test@example.com',
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

    it('should successfully login with phone', async () => {
      const credentials: LoginRequest = {
        identifier: '13800138000',
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

    it('should handle login error with wrong credentials', async () => {
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

    it('should handle login error with non-existent user', async () => {
      const credentials: LoginRequest = {
        identifier: 'nonexistent',
        password: 'Test123!@#'
      }

      const mockError = {
        response: {
          data: {
            message: '用户不存在'
          }
        }
      }

      ;(apiClient.post as any).mockRejectedValue(mockError)

      await expect(authService.login(credentials)).rejects.toThrow('用户不存在')
    })

    it('should handle login error with disabled account', async () => {
      const credentials: LoginRequest = {
        identifier: 'disableduser',
        password: 'Test123!@#'
      }

      const mockError = {
        response: {
          data: {
            message: '账户已被禁用'
          }
        }
      }

      ;(apiClient.post as any).mockRejectedValue(mockError)

      await expect(authService.login(credentials)).rejects.toThrow('账户已被禁用')
    })

    it('should handle login network error', async () => {
      const credentials: LoginRequest = {
        identifier: 'testuser',
        password: 'Test123!@#'
      }

      ;(apiClient.post as any).mockRejectedValue(new Error('Network Error'))

      await expect(authService.login(credentials)).rejects.toThrow('登录失败，请稍后重试')
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

    it('should handle logout error', async () => {
      const mockError = {
        response: {
          data: {
            message: '登出失败'
          }
        }
      }

      ;(apiClient.post as any).mockRejectedValue(mockError)

      await expect(authService.logout()).rejects.toThrow('登出失败')
    })

    it('should handle logout network error', async () => {
      ;(apiClient.post as any).mockRejectedValue(new Error('Network Error'))

      await expect(authService.logout()).rejects.toThrow('登出失败，请稍后重试')
    })
  })

  describe('refreshToken', () => {
    it('should successfully refresh token', async () => {
      const mockResponse = {
        data: {
          success: true,
          message: '令牌刷新成功',
          data: {
            user: {
              id: 1,
              username: 'testuser',
              email: 'test@example.com',
              phone: '13800138000',
              role: 'USER',
              status: 'ACTIVE'
            },
            token: 'new-mock-jwt-token',
            refreshToken: 'new-refresh-token',
            expiresIn: 86400
          }
        }
      }

      ;(apiClient.post as any).mockResolvedValue(mockResponse)

      const result = await authService.refreshToken()

      expect(apiClient.post).toHaveBeenCalledWith('/v1/auth/refresh')
      expect(result).toEqual(mockResponse.data)
    })

    it('should handle refresh token error with expired token', async () => {
      const mockError = {
        response: {
          data: {
            message: '令牌已过期'
          }
        }
      }

      ;(apiClient.post as any).mockRejectedValue(mockError)

      await expect(authService.refreshToken()).rejects.toThrow('令牌已过期')
    })

    it('should handle refresh token error with invalid token', async () => {
      const mockError = {
        response: {
          data: {
            message: '无效的刷新令牌'
          }
        }
      }

      ;(apiClient.post as any).mockRejectedValue(mockError)

      await expect(authService.refreshToken()).rejects.toThrow('无效的刷新令牌')
    })

    it('should handle refresh token network error', async () => {
      ;(apiClient.post as any).mockRejectedValue(new Error('Network Error'))

      await expect(authService.refreshToken()).rejects.toThrow('令牌刷新失败，请重新登录')
    })
  })

  describe('getCurrentUser', () => {
    it('should successfully get current user', async () => {
      const mockResponse = {
        data: {
          success: true,
          message: '获取用户信息成功',
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

    it('should handle get current user error with unauthorized access', async () => {
      const mockError = {
        response: {
          status: 401,
          data: {
            message: '未授权访问'
          }
        }
      }

      ;(apiClient.get as any).mockRejectedValue(mockError)

      await expect(authService.getCurrentUser()).rejects.toThrow('未授权访问')
    })

    it('should handle get current user error with token expired', async () => {
      const mockError = {
        response: {
          status: 401,
          data: {
            message: '令牌已过期'
          }
        }
      }

      ;(apiClient.get as any).mockRejectedValue(mockError)

      await expect(authService.getCurrentUser()).rejects.toThrow('令牌已过期')
    })

    it('should handle get current user error with user not found', async () => {
      const mockError = {
        response: {
          data: {
            message: '用户信息不存在'
          }
        }
      }

      ;(apiClient.get as any).mockRejectedValue(mockError)

      await expect(authService.getCurrentUser()).rejects.toThrow('用户信息不存在')
    })

    it('should handle get current user network error', async () => {
      ;(apiClient.get as any).mockRejectedValue(new Error('Network Error'))

      await expect(authService.getCurrentUser()).rejects.toThrow('获取用户信息失败')
    })
  })
})
})