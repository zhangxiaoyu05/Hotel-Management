import type { App, Directive, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * 权限指令
 * 用法：v-permission="'user:write'" 或 v-permission="['user:read', 'user:write']"
 */
export const permissionDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    checkPermission(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    checkPermission(el, binding)
  }
}

function checkPermission(el: HTMLElement, binding: DirectiveBinding) {
  const authStore = useAuthStore()
  const { value } = binding

  if (!value) {
    return
  }

  let hasPermission = false

  if (Array.isArray(value)) {
    // 数组形式，需要满足其中任意一个权限
    hasPermission = value.some(permission => authStore.hasPermission(permission))
  } else if (typeof value === 'string') {
    // 字符串形式，检查单个权限
    hasPermission = authStore.hasPermission(value)
  }

  // 如果没有权限，隐藏元素
  if (!hasPermission) {
    el.style.display = 'none'
    // 或者从DOM中移除元素
    // el.parentNode?.removeChild(el)
  } else {
    el.style.display = ''
  }
}

/**
 * 角色指令
 * 用法：v-role="'ADMIN'" 或 v-role="['ADMIN', 'USER']"
 */
export const roleDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    checkRole(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    checkRole(el, binding)
  }
}

function checkRole(el: HTMLElement, binding: DirectiveBinding) {
  const authStore = useAuthStore()
  const { value } = binding

  if (!value) {
    return
  }

  let hasRole = false

  if (Array.isArray(value)) {
    // 数组形式，需要满足其中任意一个角色
    hasRole = value.some(role => authStore.hasRole(role))
  } else if (typeof value === 'string') {
    // 字符串形式，检查单个角色
    hasRole = authStore.hasRole(value)
  }

  // 如果没有角色，隐藏元素
  if (!hasRole) {
    el.style.display = 'none'
  } else {
    el.style.display = ''
  }
}

/**
 * 管理员指令
 * 用法：v-admin
 */
export const adminDirective: Directive = {
  mounted(el: HTMLElement) {
    checkAdmin(el)
  },
  updated(el: HTMLElement) {
    checkAdmin(el)
  }
}

function checkAdmin(el: HTMLElement) {
  const authStore = useAuthStore()

  if (!authStore.isAdmin) {
    el.style.display = 'none'
  } else {
    el.style.display = ''
  }
}

// 注册指令
export function setupPermissionDirectives(app: App) {
  app.directive('permission', permissionDirective)
  app.directive('role', roleDirective)
  app.directive('admin', adminDirective)
}