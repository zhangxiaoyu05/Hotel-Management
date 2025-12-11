import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePermissions } from '@/composables/usePermissions'
import { useAuthStore } from '@/stores/auth'

// Mock authStore
vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn()
}))

describe('usePermissions', () => {
  let mockAuthStore: any

  beforeEach(() => {
    setActivePinia(createPinia())

    // 创建mock authStore
    mockAuthStore = {
      user: {
        id: 1,
        username: 'testuser',
        role: 'USER',
        status: 'ACTIVE'
      },
      token: 'test-token',
      isAuthenticated: true,
      hasPermission: vi.fn(),
      hasRole: vi.fn(),
      canAccess: vi.fn()
    }

    // Mock useAuthStore返回值
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore)
  })

  it('应该正确返回当前用户信息', () => {
    const { currentUser, getUserId, isAuthenticated } = usePermissions()

    expect(currentUser.value).toEqual(mockAuthStore.user)
    expect(getUserId.value).toBe(1)
    expect(isAuthenticated.value).toBe(true)
  })

  it('应该正确检查管理员权限', () => {
    mockAuthStore.isAdmin = true
    const { isAdmin } = usePermissions()

    expect(isAdmin.value).toBe(true)
  })

  it('应该正确调用权限检查方法', () => {
    const { hasPermission, hasRole, canAccess } = usePermissions()

    hasPermission('user:read')
    hasRole('ADMIN')
    canAccess('user', '1')

    expect(mockAuthStore.hasPermission).toHaveBeenCalledWith('user:read')
    expect(mockAuthStore.hasRole).toHaveBeenCalledWith('ADMIN')
    expect(mockAuthStore.canAccess).toHaveBeenCalledWith('user', '1')
  })

  it('应该正确检查用户管理权限', () => {
    mockAuthStore.hasPermission.mockImplementation((permission: string) => {
      return permission === 'user:read' || permission === 'user:write'
    })

    const { canManageUsers } = usePermissions()

    expect(canManageUsers.value).toBe(true)
    expect(mockAuthStore.hasPermission).toHaveBeenCalledWith('user:read')
    expect(mockAuthStore.hasPermission).toHaveBeenCalledWith('user:write')
  })

  it('应该正确检查房间管理权限', () => {
    mockAuthStore.hasPermission.mockImplementation((permission: string) => {
      return permission === 'room:read' && permission === 'room:write'
    })

    const { canManageRooms } = usePermissions()

    expect(canManageRooms.value).toBe(false) // 因为mock返回false
  })

  it('应该正确检查预订管理权限', () => {
    mockAuthStore.hasPermission.mockImplementation((permission: string) => {
      return permission === 'booking:read' || permission === 'booking:write'
    })

    const { canManageBookings } = usePermissions()

    expect(canManageBookings.value).toBe(true)
  })

  it('应该正确检查评价管理权限', () => {
    mockAuthStore.hasPermission.mockImplementation((permission: string) => {
      return permission === 'review:read' || permission === 'review:write'
    })

    const { canManageReviews } = usePermissions()

    expect(canManageReviews.value).toBe(true)
  })

  it('管理员应该拥有所有管理权限', () => {
    mockAuthStore.user.role = 'ADMIN'
    mockAuthStore.hasPermission.mockReturnValue(true) // 管理员拥有所有权限

    const { canManageUsers, canManageRooms, canManageBookings, canManageReviews } = usePermissions()

    expect(canManageUsers.value).toBe(true)
    expect(canManageRooms.value).toBe(true)
    expect(canManageBookings.value).toBe(true)
    expect(canManageReviews.value).toBe(true)
  })

  it('未认证用户不应该拥有任何管理权限', () => {
    mockAuthStore.isAuthenticated = false
    mockAuthStore.hasPermission.mockReturnValue(false)

    const { canManageUsers, canManageRooms, canManageBookings, canManageReviews } = usePermissions()

    expect(canManageUsers.value).toBe(false)
    expect(canManageRooms.value).toBe(false)
    expect(canManageBookings.value).toBe(false)
    expect(canManageReviews.value).toBe(false)
  })
})