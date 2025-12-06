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

export interface CreateUserRequest {
  username: string
  email: string
  phone: string
  password: string
  role?: 'USER' | 'ADMIN'
}

export interface UpdateUserRequest {
  username?: string
  email?: string
  phone?: string
  role?: 'USER' | 'ADMIN'
  status?: 'ACTIVE' | 'INACTIVE'
}

export interface LoginRequest {
  identifier: string // username or email or phone
  password: string
}

export interface AuthResponse {
  success: boolean
  message: string
  data?: {
    user: User
    token: string
  }
}

export interface UserListResponse {
  success: boolean
  message: string
  data?: {
    users: User[]
    total: number
    page: number
    pageSize: number
  }
}

export interface ApiError {
  success: false
  message: string
  errors?: {
    field: string
    message: string
  }[]
}