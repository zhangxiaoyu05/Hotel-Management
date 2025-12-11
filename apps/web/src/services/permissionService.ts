import request from '@/utils/request'
import { eventBus, PERMISSION_EVENTS } from '@/utils/eventBus'
import { useAuthStore } from '@/stores/auth'

// 权限变更事件类型
export interface PermissionChangeEvent {
  userId: number
  newRole?: string
  newStatus?: string
  changedBy: number
  timestamp: number
}

/**
 * 权限服务
 * 处理权限变更的实时通知和刷新
 */
class PermissionService {
  private eventSource: EventSource | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000

  /**
   * 启动权限变更监听
   */
  startPermissionListener() {
    if (this.eventSource) {
      this.stopPermissionListener()
    }

    const authStore = useAuthStore()
    const token = authStore.token

    if (!token) {
      console.warn('未找到访问令牌，无法启动权限监听')
      return
    }

    try {
      this.eventSource = new EventSource('/api/v1/permissions/events', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      this.eventSource.addEventListener('permission-changed', (event) => {
        try {
          const data: PermissionChangeEvent = JSON.parse(event.data)
          this.handlePermissionChange(data)
        } catch (error) {
          console.error('解析权限变更事件失败:', error)
        }
      })

      this.eventSource.addEventListener('role-changed', (event) => {
        try {
          const data: PermissionChangeEvent = JSON.parse(event.data)
          this.handleRoleChange(data)
        } catch (error) {
          console.error('解析角色变更事件失败:', error)
        }
      })

      this.eventSource.addEventListener('user-status-changed', (event) => {
        try {
          const data: PermissionChangeEvent = JSON.parse(event.data)
          this.handleUserStatusChange(data)
        } catch (error) {
          console.error('解析用户状态变更事件失败:', error)
        }
      })

      this.eventSource.onopen = () => {
        console.log('权限监听连接已建立')
        this.reconnectAttempts = 0
      }

      this.eventSource.onerror = (error) => {
        console.error('权限监听连接错误:', error)
        this.handleConnectionError()
      }

    } catch (error) {
      console.error('启动权限监听失败:', error)
    }
  }

  /**
   * 停止权限变更监听
   */
  stopPermissionListener() {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
      console.log('权限监听连接已关闭')
    }
  }

  /**
   * 处理连接错误
   */
  private handleConnectionError() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`尝试重新连接权限监听 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

      setTimeout(() => {
        this.startPermissionListener()
      }, this.reconnectDelay * this.reconnectAttempts)
    } else {
      console.error('权限监听连接重试次数已达上限')
      this.stopPermissionListener()
    }
  }

  /**
   * 处理权限变更
   */
  private async handlePermissionChange(event: PermissionChangeEvent) {
    const authStore = useAuthStore()

    // 如果变更的是当前用户，需要刷新用户信息和权限
    if (event.userId === authStore.user?.id) {
      try {
        await authStore.getCurrentUser()
        await this.refreshUserPermissions()

        eventBus.emit(PERMISSION_EVENTS.PERMISSION_CHANGED, event)
        console.log('用户权限已更新:', event)
      } catch (error) {
        console.error('刷新用户权限失败:', error)
      }
    } else {
      // 其他用户的权限变更，仅通知
      eventBus.emit(PERMISSION_EVENTS.PERMISSION_CHANGED, event)
    }
  }

  /**
   * 处理角色变更
   */
  private async handleRoleChange(event: PermissionChangeEvent) {
    const authStore = useAuthStore()

    // 如果变更的是当前用户，需要刷新用户信息和权限
    if (event.userId === authStore.user?.id) {
      try {
        await authStore.getCurrentUser()
        await this.refreshUserPermissions()

        eventBus.emit(PERMISSION_EVENTS.ROLE_CHANGED, event)
        console.log('用户角色已更新:', event)

        // 如果角色变更为非管理员，可能需要重定向
        if (event.newRole !== 'ADMIN' && window.location.pathname.startsWith('/admin')) {
          window.location.href = '/'
        }
      } catch (error) {
        console.error('刷新用户角色失败:', error)
      }
    } else {
      // 其他用户的角色变更，仅通知
      eventBus.emit(PERMISSION_EVENTS.ROLE_CHANGED, event)
    }
  }

  /**
   * 处理用户状态变更
   */
  private async handleUserStatusChange(event: PermissionChangeEvent) {
    const authStore = useAuthStore()

    // 如果变更的是当前用户
    if (event.userId === authStore.user?.id) {
      try {
        await authStore.getCurrentUser()

        eventBus.emit(PERMISSION_EVENTS.USER_STATUS_CHANGED, event)
        console.log('用户状态已更新:', event)

        // 如果用户被禁用，需要登出
        if (event.newStatus === 'INACTIVE') {
          await authStore.logout()
          window.location.href = '/login'
        }
      } catch (error) {
        console.error('刷新用户状态失败:', error)
      }
    } else {
      // 其他用户的状态变更，仅通知
      eventBus.emit(PERMISSION_EVENTS.USER_STATUS_CHANGED, event)
    }
  }

  /**
   * 刷新用户权限
   */
  private async refreshUserPermissions(): Promise<void> {
    try {
      const response = await request.get('/v1/auth/permissions')
      if (response.success && response.data) {
        const authStore = useAuthStore()
        authStore.updatePermissions(response.data.permissions || [])
      }
    } catch (error) {
      console.error('获取用户权限失败:', error)
    }
  }

  /**
   * 手动刷新权限
   */
  async refreshPermissions(): Promise<void> {
    const authStore = useAuthStore()

    try {
      await authStore.getCurrentUser()
      await this.refreshUserPermissions()

      eventBus.emit(PERMISSION_EVENTS.PERMISSION_CHANGED, {
        userId: authStore.user?.id,
        changedBy: authStore.user?.id,
        timestamp: Date.now()
      })
    } catch (error) {
      console.error('手动刷新权限失败:', error)
      throw error
    }
  }

  /**
   * 检查权限缓存是否有效
   */
  isPermissionCacheValid(): boolean {
    const authStore = useAuthStore()
    const lastUpdate = localStorage.getItem('permissions_last_update')

    if (!lastUpdate) return false

    const updateTime = parseInt(lastUpdate)
    const now = Date.now()
    const cacheTimeout = 5 * 60 * 1000 // 5分钟缓存超时

    return (now - updateTime) < cacheTimeout
  }

  /**
   * 清除权限缓存
   */
  clearPermissionCache(): void {
    localStorage.removeItem('user_permissions')
    localStorage.removeItem('permissions_last_update')

    const authStore = useAuthStore()
    authStore.clearPermissions()
  }
}

// 创建全局权限服务实例
export const permissionService = new PermissionService()

// 导出权限服务类
export { PermissionService }