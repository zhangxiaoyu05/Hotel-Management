import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { authService } from '@/services/authService'
import type { CreateUserRequest, LoginRequest } from '@/services/authService'

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// Mock authService
vi.mock('@/services/authService', () => ({
  authService: {
    register: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    refreshToken: vi.fn(),
    getCurrentUser: vi.fn()
  }
}))

const mockAuthService = authService as any

describe('Auth Store', () => {
  beforeEach(() => {
    // Create a fresh Pinia instance for each test
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      const authStore = useAuthStore()

      expect(authStore.user).toBe(null)
      expect(authStore.token).toBe(null)
      expect(authStore.permissions).toEqual([])
      expect(authStore.loading).toBe(false)
      expect(authStore.error).toBe(null)
      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.isAdmin).toBe(false)
      expect(authStore.isActive).toBe(false)
    })
  })

  describe('Computed Properties', () => {
    it('should compute isAuthenticated correctly', () => {
      const authStore = useAuthStore()

      // Initially false
      expect(authStore.isAuthenticated).toBe(false)

      // Set token but no user
      ;(authStore as any).token.value = 'mock-token'
      expect(authStore.isAuthenticated).toBe(false)

      // Set user but no token
      ;(authStore as any).token.value = null
      ;(authStore as any).user.value = { id: 1, username: 'test' } as any
      expect(authStore.isAuthenticated).toBe(false)

      // Set both token and user
      ;(authStore as any).token.value = 'mock-token'
      ;(authStore as any).user.value = { id: 1, username: 'test' } as any
      expect(authStore.isAuthenticated).toBe(true)
    })

    it('should compute isAdmin correctly', () => {
      const authStore = useAuthStore()

      // Initially false
      expect(authStore.isAdmin).toBe(false)

      // Set user as USER
      ;(authStore as any).user.value = { role: 'USER' } as any
      expect(authStore.isAdmin).toBe(false)

      // Set user as ADMIN
      ;(authStore as any).user.value = { role: 'ADMIN' } as any
      expect(authStore.isAdmin).toBe(true)
    })

    it('should compute isActive correctly', () => {
      const authStore = useAuthStore()

      // Initially false
      expect(authStore.isActive).toBe(false)

      // Set user as INACTIVE
      ;(authStore as any).user.value = { status: 'INACTIVE' } as any
      expect(authStore.isActive).toBe(false)

      // Set user as ACTIVE
      ;(authStore as any).user.value = { status: 'ACTIVE' } as any
      expect(authStore.isActive).toBe(true)
    })
  })

  describe('initializeAuth', () => {
    it('should initialize auth from localStorage with valid data', () => {
      const authStore = useAuthStore()
      const mockUser = { id: 1, username: 'testuser', email: 'test@example.com' }

      localStorageMock.getItem
        .mockReturnValueOnce('mock-token') // auth_token
        .mockReturnValueOnce(JSON.stringify(mockUser)) // auth_user

      authStore.initializeAuth()

      expect(authStore.token).toBe('mock-token')
      expect(authStore.user).toEqual(mockUser)
      expect(localStorageMock.getItem).toHaveBeenCalledWith('auth_token')
      expect(localStorageMock.getItem).toHaveBeenCalledWith('auth_user')
    })

    it('should handle invalid user data in localStorage', () => {
      const authStore = useAuthStore()
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      localStorageMock.getItem
        .mockReturnValueOnce('mock-token') // auth_token
        .mockReturnValueOnce('invalid-json') // auth_user

      authStore.initializeAuth()

      expect(authStore.token).toBe(null)
      expect(authStore.user).toBe(null)
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_user')
      expect(consoleSpy).toHaveBeenCalledWith('解析用户数据失败:', expect.any(Error))

      consoleSpy.mockRestore()
    })

    it('should not initialize if token or user is missing', () => {
      const authStore = useAuthStore()

      // Missing token
      localStorageMock.getItem
        .mockReturnValueOnce(null) // auth_token
        .mockReturnValueOnce(JSON.stringify({ id: 1 })) // auth_user

      authStore.initializeAuth()

      expect(authStore.token).toBe(null)
      expect(authStore.user).toBe(null)

      // Missing user
      localStorageMock.getItem
        .mockReturnValueOnce('mock-token') // auth_token
        .mockReturnValueOnce(null) // auth_user

      authStore.initializeAuth()

      expect(authStore.token).toBe(null)
      expect(authStore.user).toBe(null)
    })
  })

  describe('clearAuth', () => {
    it('should clear all auth data', () => {
      const authStore = useAuthStore()

      // Set some initial data
      ;(authStore as any).token.value = 'mock-token'
      ;(authStore as any).user.value = { id: 1 } as any
      ;(authStore as any).error.value = 'some error'

      authStore.clearAuth()

      expect(authStore.token).toBe(null)
      expect(authStore.user).toBe(null)
      expect(authStore.error).toBe(null)
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_user')
    })
  })

  describe('saveAuth', () => {
    it('should save auth data correctly', () => {
      const authStore = useAuthStore()
      const mockResponse = {
        success: true,
        data: {
          user: { id: 1, username: 'testuser' },
          token: 'mock-token'
        }
      }

      authStore.saveAuth(mockResponse)

      expect(authStore.user).toEqual(mockResponse.data.user)
      expect(authStore.token).toBe('mock-token')
      expect(authStore.error).toBe(null)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('auth_token', 'mock-token')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('auth_user', JSON.stringify(mockResponse.data.user))
    })

    it('should not save if response is not successful', () => {
      const authStore = useAuthStore()
      const mockResponse = {
        success: false,
        message: 'Login failed'
      }

      authStore.saveAuth(mockResponse)

      expect(authStore.user).toBe(null)
      expect(authStore.token).toBe(null)
      expect(localStorageMock.setItem).not.toHaveBeenCalled()
    })
  })

  describe('login', () => {
    it('should login successfully', async () => {
      const authStore = useAuthStore()
      const credentials: LoginRequest = {
        identifier: 'testuser',
        password: 'password123'
      }

      const mockResponse = {
        success: true,
        data: {
          user: { id: 1, username: 'testuser', role: 'USER', status: 'ACTIVE' },
          token: 'mock-token'
        }
      }

      mockAuthService.login.mockResolvedValue(mockResponse)

      await authStore.login(credentials)

      expect(authStore.loading).toBe(false)
      expect(authStore.user).toEqual(mockResponse.data.user)
      expect(authStore.token).toBe('mock-token')
      expect(authStore.error).toBe(null)
      expect(mockAuthService.login).toHaveBeenCalledWith(credentials)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('auth_token', 'mock-token')
    })

    it('should handle login error', async () => {
      const authStore = useAuthStore()
      const credentials: LoginRequest = {
        identifier: 'testuser',
        password: 'wrongpassword'
      }

      const errorMessage = '用户名或密码错误'
      mockAuthService.login.mockRejectedValue(new Error(errorMessage))

      await expect(authStore.login(credentials)).rejects.toThrow(errorMessage)

      expect(authStore.loading).toBe(false)
      expect(authStore.user).toBe(null)
      expect(authStore.token).toBe(null)
      expect(authStore.error).toBe(errorMessage)
      expect(mockAuthService.login).toHaveBeenCalledWith(credentials)
    })
  })

  describe('register', () => {
    it('should register successfully', async () => {
      const authStore = useAuthStore()
      const userData: CreateUserRequest = {
        username: 'newuser',
        email: 'newuser@example.com',
        phone: '13800138000',
        password: 'password123'
      }

      const mockResponse = {
        success: true,
        message: '注册成功'
      }

      mockAuthService.register.mockResolvedValue(mockResponse)

      await authStore.register(userData)

      expect(authStore.loading).toBe(false)
      expect(authStore.error).toBe(null)
      expect(mockAuthService.register).toHaveBeenCalledWith(userData)
    })

    it('should handle registration error', async () => {
      const authStore = useAuthStore()
      const userData: CreateUserRequest = {
        username: 'existinguser',
        email: 'existing@example.com',
        phone: '13800138000',
        password: 'password123'
      }

      const errorMessage = '用户名已存在'
      mockAuthService.register.mockRejectedValue(new Error(errorMessage))

      await expect(authStore.register(userData)).rejects.toThrow(errorMessage)

      expect(authStore.loading).toBe(false)
      expect(authStore.error).toBe(errorMessage)
      expect(mockAuthService.register).toHaveBeenCalledWith(userData)
    })
  })

  describe('logout', () => {
    it('should logout successfully', async () => {
      const authStore = useAuthStore()

      // Set some initial auth data
      ;(authStore as any).token.value = 'mock-token'
      ;(authStore as any).user.value = { id: 1 } as any

      mockAuthService.logout.mockResolvedValue({})

      await authStore.logout()

      expect(authStore.loading).toBe(false)
      expect(authStore.token).toBe(null)
      expect(authStore.user).toBe(null)
      expect(mockAuthService.logout).toHaveBeenCalled()
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_user')
    })

    it('should clear local data even if server logout fails', async () => {
      const authStore = useAuthStore()
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      // Set some initial auth data
      ;(authStore as any).token.value = 'mock-token'
      ;(authStore as any).user.value = { id: 1 } as any

      mockAuthService.logout.mockRejectedValue(new Error('Server error'))

      await authStore.logout()

      expect(authStore.loading).toBe(false)
      expect(authStore.token).toBe(null)
      expect(authStore.user).toBe(null)
      expect(consoleSpy).toHaveBeenCalledWith('登出请求失败:', expect.any(Error))
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_user')

      consoleSpy.mockRestore()
    })
  })

  describe('refreshToken', () => {
    it('should refresh token successfully', async () => {
      const authStore = useAuthStore()

      // Set initial token
      ;(authStore as any).token.value = 'old-token'

      const mockResponse = {
        success: true,
        data: {
          user: { id: 1, username: 'testuser' },
          token: 'new-token'
        }
      }

      mockAuthService.refreshToken.mockResolvedValue(mockResponse)

      await authStore.refreshToken()

      expect(authStore.token).toBe('new-token')
      expect(authStore.user).toEqual(mockResponse.data.user)
      expect(mockAuthService.refreshToken).toHaveBeenCalled()
    })

    it('should not refresh if no token exists', async () => {
      const authStore = useAuthStore()

      await authStore.refreshToken()

      expect(mockAuthService.refreshToken).not.toHaveBeenCalled()
    })

    it('should clear auth on refresh failure', async () => {
      const authStore = useAuthStore()
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      // Set initial token
      ;(authStore as any).token.value = 'old-token'
      ;(authStore as any).user.value = { id: 1 } as any

      mockAuthService.refreshToken.mockRejectedValue(new Error('Token expired'))

      await expect(authStore.refreshToken()).rejects.toThrow('Token expired')

      expect(authStore.token).toBe(null)
      expect(authStore.user).toBe(null)
      expect(consoleSpy).toHaveBeenCalledWith('刷新token失败:', expect.any(Error))

      consoleSpy.mockRestore()
    })
  })

  describe('Permission Methods', () => {
    it('should check hasPermission correctly', () => {
      const authStore = useAuthStore()

      // No user - should return false
      expect(authStore.hasPermission('read:users')).toBe(false)

      // Admin user - should return true for any permission
      ;(authStore as any).user.value = { role: 'ADMIN' } as any
      expect(authStore.hasPermission('any:permission')).toBe(true)

      // Regular user with specific permissions
      ;(authStore as any).user.value = { role: 'USER' } as any
      ;(authStore as any).permissions.value = ['read:users', 'write:posts']
      expect(authStore.hasPermission('read:users')).toBe(true)
      expect(authStore.hasPermission('write:posts')).toBe(true)
      expect(authStore.hasPermission('delete:users')).toBe(false)
    })

    it('should check hasRole correctly', () => {
      const authStore = useAuthStore()

      ;(authStore as any).user.value = { role: 'ADMIN' } as any
      expect(authStore.hasRole('ADMIN')).toBe(true)
      expect(authStore.hasRole('USER')).toBe(false)

      ;(authStore as any).user.value = { role: 'USER' } as any
      expect(authStore.hasRole('USER')).toBe(true)
      expect(authStore.hasRole('ADMIN')).toBe(false)
    })

    it('should check canAccess correctly', () => {
      const authStore = useAuthStore()

      // No user - should return false
      expect(authStore.canAccess('users', '1')).toBe(false)

      // Admin user - should return true for any resource
      ;(authStore as any).user.value = { role: 'ADMIN', id: 1 } as any
      expect(authStore.canAccess('users', '999')).toBe(true)

      // Regular user accessing own resource
      ;(authStore as any).user.value = { role: 'USER', id: 1 } as any
      expect(authStore.canAccess('users', '1')).toBe(true)

      // Regular user accessing other user's resource
      ;(authStore as any).user.value = { role: 'USER', id: 1 } as any
      expect(authStore.canAccess('users', '2')).toBe(false)

      // Regular user accessing resource without ID
      ;(authStore as any).user.value = { role: 'USER', id: 1 } as any
      expect(authStore.canAccess('users')).toBe(false)
    })
  })

  describe('updatePermissions', () => {
    it('should update permissions correctly', () => {
      const authStore = useAuthStore()
      const newPermissions = ['read:users', 'write:posts', 'delete:comments']

      authStore.updatePermissions(newPermissions)

      expect(authStore.permissions).toEqual(newPermissions)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('user_permissions', JSON.stringify(newPermissions))
    })
  })

  describe('clearPermissions', () => {
    it('should clear permissions correctly', () => {
      const authStore = useAuthStore()

      // Set initial permissions
      ;(authStore as any).permissions.value = ['read:users', 'write:posts']

      authStore.clearPermissions()

      expect(authStore.permissions).toEqual([])
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('user_permissions')
    })
  })

  describe('setUser', () => {
    it('should update user correctly', () => {
      const authStore = useAuthStore()
      const newUser = { id: 2, username: 'newuser', email: 'newuser@example.com' } as any

      authStore.setUser(newUser)

      expect(authStore.user).toEqual(newUser)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('auth_user', JSON.stringify(newUser))
    })
  })

  describe('checkAuthStatus', () => {
    it('should return true for valid auth status', async () => {
      const authStore = useAuthStore()

      // Set initial token
      ;(authStore as any).token.value = 'mock-token'

      const mockResponse = {
        success: true,
        data: {
          user: { id: 1, username: 'testuser' }
        }
      }

      mockAuthService.getCurrentUser.mockResolvedValue(mockResponse)

      const result = await authStore.checkAuthStatus()

      expect(result).toBe(true)
      expect(authStore.user).toEqual(mockResponse.data.user)
      expect(mockAuthService.getCurrentUser).toHaveBeenCalled()
    })

    it('should return false for no token', async () => {
      const authStore = useAuthStore()

      const result = await authStore.checkAuthStatus()

      expect(result).toBe(false)
      expect(mockAuthService.getCurrentUser).not.toHaveBeenCalled()
    })

    it('should clear auth and return false on auth check failure', async () => {
      const authStore = useAuthStore()

      // Set initial token and user
      ;(authStore as any).token.value = 'mock-token'
      ;(authStore as any).user.value = { id: 1 } as any

      mockAuthService.getCurrentUser.mockRejectedValue(new Error('Token invalid'))

      const result = await authStore.checkAuthStatus()

      expect(result).toBe(false)
      expect(authStore.token).toBe(null)
      expect(authStore.user).toBe(null)
    })
  })
})