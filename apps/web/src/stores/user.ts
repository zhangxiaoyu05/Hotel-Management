import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface User {
  id: number
  username: string
  email: string
  phone: string
  role: 'USER' | 'ADMIN'
  status: 'ACTIVE' | 'INACTIVE'
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  const login = async (credentials: { username: string; password: string }) => {
    try {
      // TODO: Implement actual API call
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials),
      })

      if (response.ok) {
        const data = await response.json()
        token.value = data.token
        user.value = data.user
        localStorage.setItem('token', data.token)
        return true
      }
      return false
    } catch (error) {
      console.error('Login error:', error)
      return false
    }
  }

  const logout = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
  }

  const register = async (userData: {
    username: string
    email: string
    phone: string
    password: string
  }) => {
    try {
      // TODO: Implement actual API call
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userData),
      })

      return response.ok
    } catch (error) {
      console.error('Register error:', error)
      return false
    }
  }

  const fetchProfile = async () => {
    if (!token.value) return

    try {
      // TODO: Implement actual API call
      const response = await fetch('/api/auth/profile', {
        headers: {
          Authorization: `Bearer ${token.value}`,
        },
      })

      if (response.ok) {
        const userData = await response.json()
        user.value = userData
      }
    } catch (error) {
      console.error('Fetch profile error:', error)
    }
  }

  return {
    user,
    token,
    isAuthenticated,
    isAdmin,
    login,
    logout,
    register,
    fetchProfile,
  }
})