import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../pages/Home.vue'),
  },
  {
    path: '/hotels',
    name: 'Hotels',
    component: () => import('../pages/Hotels.vue'),
  },
  {
    path: '/hotels/:id',
    name: 'HotelDetail',
    component: () => import('../pages/HotelDetail.vue'),
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../pages/Profile.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../pages/Login.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/auth/login',
    redirect: '/login'
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../pages/Register.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/auth/register',
    redirect: '/register'
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard for protected routes
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 初始化认证状态
  if (!authStore.isAuthenticated && authStore.token) {
    authStore.initializeAuth()
  }

  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      next({
        name: 'Login',
        query: { redirect: to.fullPath }
      })
    } else {
      next()
    }
  }
  // 检查是否是仅限游客访问的页面（如登录、注册）
  else if (to.meta.guestOnly && authStore.isAuthenticated) {
    next({ path: '/' })
  } else {
    next()
  }
})

export default router