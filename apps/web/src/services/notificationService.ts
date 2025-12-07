import { apiClient } from '@/utils/apiClient'

export interface Notification {
  id: number
  userId: number
  title: string
  content: string
  type: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
  isRead: boolean
  createdAt: string
  relatedEntityType?: string
  relatedEntityId?: number
}

export interface NotificationSettings {
  emailNotifications: boolean
  bookingConfirmations: boolean
  bookingReminders: boolean
  promotionalOffers: boolean
  systemUpdates: boolean
}

export const notificationService = {
  // 获取用户通知列表
  async getUserNotifications(page = 1, limit = 20): Promise<{
    notifications: Notification[]
    total: number
    unreadCount: number
  }> {
    const response = await apiClient.get('/notifications', {
      params: { page, limit }
    })
    return response.data.data
  },

  // 标记通知为已读
  async markAsRead(notificationId: number): Promise<boolean> {
    const response = await apiClient.put(`/notifications/${notificationId}/read`)
    return response.data.success
  },

  // 标记所有通知为已读
  async markAllAsRead(): Promise<boolean> {
    const response = await apiClient.put('/notifications/read-all')
    return response.data.success
  },

  // 删除通知
  async deleteNotification(notificationId: number): Promise<boolean> {
    const response = await apiClient.delete(`/notifications/${notificationId}`)
    return response.data.success
  },

  // 获取通知设置
  async getNotificationSettings(): Promise<NotificationSettings> {
    const response = await apiClient.get('/notifications/settings')
    return response.data.data
  },

  // 更新通知设置
  async updateNotificationSettings(settings: Partial<NotificationSettings>): Promise<NotificationSettings> {
    const response = await apiClient.put('/notifications/settings', settings)
    return response.data.data
  },

  // 订阅推送通知（使用浏览器 Push API）
  async subscribeToPushNotifications(): Promise<boolean> {
    try {
      if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        throw new Error('当前浏览器不支持推送通知')
      }

      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: this.urlBase64ToUint8Array(process.env.VUE_APP_VAPID_PUBLIC_KEY || '')
      })

      const response = await apiClient.post('/notifications/subscribe', {
        subscription
      })

      return response.data.success
    } catch (error) {
      console.error('订阅推送通知失败:', error)
      return false
    }
  },

  // 取消订阅推送通知
  async unsubscribeFromPushNotifications(): Promise<boolean> {
    try {
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.getSubscription()

      if (subscription) {
        await subscription.unsubscribe()
        await apiClient.post('/notifications/unsubscribe', {
          subscription
        })
      }

      return true
    } catch (error) {
      console.error('取消订阅推送通知失败:', error)
      return false
    }
  },

  // 工具方法：将 VAPID 公钥转换为 Uint8Array
  urlBase64ToUint8Array(base64String: string): Uint8Array {
    const padding = '='.repeat((4 - base64String.length % 4) % 4)
    const base64 = (base64String + padding)
      .replace(/-/g, '+')
      .replace(/_/g, '/')

    const rawData = window.atob(base64)
    const outputArray = new Uint8Array(rawData.length)

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i)
    }

    return outputArray
  }
}

// 前端通知管理器
export class NotificationManager {
  private static instance: NotificationManager
  private notifications: Notification[] = []
  private unreadCount = 0
  private listeners: Array<(notifications: Notification[], unreadCount: number) => void> = []

  private constructor() {
    this.init()
  }

  static getInstance(): NotificationManager {
    if (!NotificationManager.instance) {
      NotificationManager.instance = new NotificationManager()
    }
    return NotificationManager.instance
  }

  private async init() {
    // 请求通知权限
    if ('Notification' in window && Notification.permission === 'default') {
      await Notification.requestPermission()
    }

    // 定期检查新通知
    this.startPolling()
  }

  // 添加监听器
  addListener(listener: (notifications: Notification[], unreadCount: number) => void) {
    this.listeners.push(listener)
  }

  // 移除监听器
  removeListener(listener: (notifications: Notification[], unreadCount: number) => void) {
    const index = this.listeners.indexOf(listener)
    if (index > -1) {
      this.listeners.splice(index, 1)
    }
  }

  // 通知监听器
  private notifyListeners() {
    this.listeners.forEach(listener => {
      listener(this.notifications, this.unreadCount)
    })
  }

  // 刷新通知列表
  async refreshNotifications() {
    try {
      const result = await notificationService.getUserNotifications()
      this.notifications = result.notifications
      this.unreadCount = result.unreadCount
      this.notifyListeners()

      // 如果有新通知且权限允许，显示浏览器通知
      if (this.unreadCount > 0 && Notification.permission === 'granted') {
        this.showBrowserNotification(result.notifications[0])
      }
    } catch (error) {
      console.error('刷新通知失败:', error)
    }
  }

  // 显示浏览器通知
  private showBrowserNotification(notification: Notification) {
    if (!notification.isRead) {
      new Notification(notification.title, {
        body: notification.content,
        icon: '/favicon.ico',
        tag: `notification-${notification.id}`
      })
    }
  }

  // 开始轮询检查新通知
  private startPolling() {
    setInterval(() => {
      this.refreshNotifications()
    }, 30000) // 30秒检查一次
  }

  // 获取当前通知列表
  getNotifications(): Notification[] {
    return this.notifications
  }

  // 获取未读数量
  getUnreadCount(): number {
    return this.unreadCount
  }
}