import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * 权限检查组合式函数
 * 提供便捷的权限检查功能
 */
export function usePermissions() {
  const authStore = useAuthStore()

  // 检查是否为管理员
  const isAdmin = computed(() => authStore.isAdmin)

  // 检查是否已认证
  const isAuthenticated = computed(() => authStore.isAuthenticated)

  // 获取当前用户
  const currentUser = computed(() => authStore.user)

  // 检查是否有指定权限
  const hasPermission = (permission: string) => {
    return authStore.hasPermission(permission)
  }

  // 检查是否有指定角色
  const hasRole = (role: string) => {
    return authStore.hasRole(role)
  }

  // 检查是否可以访问指定资源
  const canAccess = (resource: string, resourceId?: string) => {
    return authStore.canAccess(resource, resourceId)
  }

  // 检查是否可以管理用户
  const canManageUsers = computed(() => {
    return hasPermission('user:read') && hasPermission('user:write')
  })

  // 检查是否可以管理房间
  const canManageRooms = computed(() => {
    return hasPermission('room:read') && hasPermission('room:write')
  })

  // 检查是否可以管理预订
  const canManageBookings = computed(() => {
    return hasPermission('booking:read') && hasPermission('booking:write')
  })

  // 检查是否可以管理评价
  const canManageReviews = computed(() => {
    return hasPermission('review:read') && hasPermission('review:write')
  })

  // 获取用户ID
  const getUserId = computed(() => currentUser.value?.id)

  return {
    isAdmin,
    isAuthenticated,
    currentUser,
    getUserId,
    hasPermission,
    hasRole,
    canAccess,
    canManageUsers,
    canManageRooms,
    canManageBookings,
    canManageReviews
  }
}