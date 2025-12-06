import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'
import { authService, type AuthResponse, type CreateUserRequest, type LoginRequest } from '@/services/authService'

export interface User {
  id: number
  username: string
  email: string
  phone: string
  role: 'USER' | 'ADMIN'
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
  updatedAt: string
}

export interface Permission {
  code: string
  description: string
}

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const permissions = ref<string[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 计算属性
  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isActive = computed(() => user.value?.status === 'ACTIVE')

  // 权限检查方法
  const hasPermission = (permission: string): boolean => {
    if (!user.value || !permissions.value) return false
    // 管理员拥有所有权限
    if (user.value.role === 'ADMIN') return true
    return permissions.value.includes(permission)
  }

  const hasRole = (role: string): boolean => {
    return user.value?.role === role
  }

  const canAccess = (resource: string, resourceId?: string): boolean => {
    if (!user.value) return false

    // 管理员可以访问所有资源
    if (user.value.role === 'ADMIN') return true

    // 普通用户只能访问自己的资源
    if (resourceId && user.value.id.toString() === resourceId) return true

    return false
  }

  // 初始化 - 从localStorage恢复状态
  const initializeAuth = () => {
    const savedToken = localStorage.getItem('auth_token')
    const savedUser = localStorage.getItem('auth_user')

    if (savedToken && savedUser) {
      token.value = savedToken
      try {
        user.value = JSON.parse(savedUser)
      } catch (e) {
        console.error('解析用户数据失败:', e)
        clearAuth()
      }
    }
  }

  // 清除认证信息
  const clearAuth = () => {
    user.value = null
    token.value = null
    error.value = null
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
  }

  // 保存认证信息
  const saveAuth = (authData: AuthResponse) => {
    if (authData.success && authData.data) {
      user.value = authData.data.user
      token.value = authData.data.token

      localStorage.setItem('auth_token', authData.data.token)
      localStorage.setItem('auth_user', JSON.stringify(authData.data.user))

      error.value = null
    }
  }

  // 注册
  const register = async (userData: CreateUserRequest): Promise<void> => {
    loading.value = true
    error.value = null

    try {
      const response = await authService.register(userData)
      // 注册成功后不清除当前状态，只是显示成功消息
      // 用户需要跳转到登录页面进行登录
      loading.value = false
    } catch (err: any) {
      error.value = err.message
      loading.value = false
      throw err
    }
  }

  // 登录
  const login = async (credentials: LoginRequest): Promise<void> => {
    loading.value = true
    error.value = null

    try {
      const response = await authService.login(credentials)
      saveAuth(response)
      loading.value = false
    } catch (err: any) {
      error.value = err.message
      loading.value = false
      throw err
    }
  }

  // 登出
  const logout = async (): Promise<void> => {
    loading.value = true

    try {
      await authService.logout()
    } catch (err: any) {
      console.error('登出请求失败:', err)
      // 即使服务器登出失败，也要清除本地状态
    } finally {
      clearAuth()
      loading.value = false
    }
  }

  // 刷新token
  const refreshToken = async (): Promise<void> => {
    if (!token.value) return

    try {
      const response = await authService.refreshToken()
      saveAuth(response)
    } catch (err: any) {
      console.error('刷新token失败:', err)
      clearAuth()
      throw err
    }
  }

  // 获取当前用户信息
  const getCurrentUser = async (): Promise<void> => {
    if (!token.value) return

    try {
      const response = await authService.getCurrentUser()
      if (response.success && response.data) {
        user.value = response.data.user
        localStorage.setItem('auth_user', JSON.stringify(response.data.user))
      }
    } catch (err: any) {
      console.error('获取用户信息失败:', err)
      clearAuth()
      throw err
    }
  }

  // 检查认证状态
  const checkAuthStatus = async (): Promise<boolean> => {
    if (!token.value) return false

    try {
      await getCurrentUser()
      return true
    } catch (error) {
      clearAuth()
      return false
    }
  }

  // 更新权限信息
  const updatePermissions = (newPermissions: string[]) => {
    permissions.value = newPermissions
    localStorage.setItem('user_permissions', JSON.stringify(newPermissions))
  }

  // 清除权限信息
  const clearPermissions = () => {
    permissions.value = []
    localStorage.removeItem('user_permissions')
  }

  return {
    // 状态
    user: readonly(user),
    token: readonly(token),
    permissions: readonly(permissions),
    loading: readonly(loading),
    error: readonly(error),

    // 计算属性
    isAuthenticated,
    isAdmin,
    isActive,

    // 权限检查方法
    hasPermission,
    hasRole,
    canAccess,

    // 方法
    initializeAuth,
    clearAuth,
    register,
    login,
    logout,
    refreshToken,
    getCurrentUser,
    checkAuthStatus,
    updatePermissions,
    clearPermissions
  }
})