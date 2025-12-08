import { apiClient } from '@/utils/apiClient'
import type { AxiosResponse } from 'axios'

export interface CreateUserRequest {
  username: string
  email: string
  phone: string
  password: string
  role?: 'USER' | 'ADMIN'
}

export interface AuthResponse {
  success: boolean
  message: string
  data?: {
    user: {
      id: number
      username: string
      email: string
      phone: string
      role: string
      status: string
    }
    token: string
  }
}

export interface LoginRequest {
  identifier: string // username or email or phone
  password: string
}

class AuthService {
  async register(userData: CreateUserRequest): Promise<AuthResponse> {
    try {
      const response: AxiosResponse<AuthResponse> = await apiClient.post('/v1/auth/register', userData)
      return response.data
    } catch (error: any) {
      console.error('注册请求失败:', error)
      throw new Error(error.response?.data?.message || '注册失败，请稍后重试')
    }
  }

  async login(credentials: LoginRequest): Promise<AuthResponse> {
    try {
      const response: AxiosResponse<AuthResponse> = await apiClient.post('/v1/auth/login', credentials)
      return response.data
    } catch (error: any) {
      console.error('登录请求失败:', error)
      throw new Error(error.response?.data?.message || '登录失败，请稍后重试')
    }
  }

  async logout(): Promise<void> {
    try {
      await apiClient.post('/v1/auth/logout')
    } catch (error: any) {
      console.error('登出请求失败:', error)
      throw new Error(error.response?.data?.message || '登出失败，请稍后重试')
    }
  }

  async refreshToken(): Promise<AuthResponse> {
    try {
      const response: AxiosResponse<AuthResponse> = await apiClient.post('/v1/auth/refresh')
      return response.data
    } catch (error: any) {
      console.error('刷新令牌失败:', error)
      throw new Error(error.response?.data?.message || '令牌刷新失败，请重新登录')
    }
  }

  async getCurrentUser(): Promise<any> {
    try {
      const response = await apiClient.get('/v1/auth/me')
      return response.data
    } catch (error: any) {
      console.error('获取当前用户信息失败:', error)
      throw new Error(error.response?.data?.message || '获取用户信息失败')
    }
  }
}

export const authService = new AuthService()