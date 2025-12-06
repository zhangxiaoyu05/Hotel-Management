import { ref } from 'vue'

type EventHandler = (...args: any[]) => void

class EventBus {
  private events = new Map<string, EventHandler[]>()

  /**
   * 订阅事件
   */
  on(event: string, handler: EventHandler) {
    if (!this.events.has(event)) {
      this.events.set(event, [])
    }
    this.events.get(event)!.push(handler)
  }

  /**
   * 取消订阅事件
   */
  off(event: string, handler?: EventHandler) {
    if (!this.events.has(event)) return

    if (!handler) {
      // 移除所有处理器
      this.events.delete(event)
    } else {
      // 移除特定处理器
      const handlers = this.events.get(event)!
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
      if (handlers.length === 0) {
        this.events.delete(event)
      }
    }
  }

  /**
   * 发布事件
   */
  emit(event: string, ...args: any[]) {
    if (!this.events.has(event)) return

    const handlers = this.events.get(event)!
    handlers.forEach(handler => {
      try {
        handler(...args)
      } catch (error) {
        console.error(`事件处理器执行错误 [${event}]:`, error)
      }
    })
  }

  /**
   * 一次性订阅事件
   */
  once(event: string, handler: EventHandler) {
    const onceHandler = (...args: any[]) => {
      handler(...args)
      this.off(event, onceHandler)
    }
    this.on(event, onceHandler)
  }

  /**
   * 清除所有事件
   */
  clear() {
    this.events.clear()
  }
}

// 创建全局事件总线实例
export const eventBus = new EventBus()

// 权限相关事件常量
export const PERMISSION_EVENTS = {
  PERMISSION_CHANGED: 'permission:changed',
  ROLE_CHANGED: 'role:changed',
  USER_STATUS_CHANGED: 'user:status:changed',
  TOKEN_REFRESHED: 'token:refreshed',
  LOGOUT: 'logout'
} as const