/**
 * 用户相关类型定义
 */

// 用户角色枚举
export enum UserRole {
  ADMIN = 'ADMIN',
  HOTEL_MANAGER = 'HOTEL_MANAGER',
  RECEPTIONIST = 'RECEPTIONIST',
  CUSTOMER = 'CUSTOMER'
}

// 用户状态枚举
export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED',
  PENDING = 'PENDING'
}

// 用户实体接口
export interface User {
  id: number;
  username: string;
  email: string;
  phone?: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  status: UserStatus;
  hotelId?: number; // 关联的酒店ID（非管理员用户）
  avatar?: string;
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

// 用户创建请求DTO
export interface CreateUserRequest {
  username: string;
  email: string;
  phone?: string;
  firstName: string;
  lastName: string;
  password: string;
  role: UserRole;
  hotelId?: number;
}

// 用户更新请求DTO
export interface UpdateUserRequest {
  email?: string;
  phone?: string;
  firstName?: string;
  lastName?: string;
  avatar?: string;
  status?: UserStatus;
  hotelId?: number;
}

// 登录请求DTO
export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

// 登录响应DTO
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

// 用户注册请求DTO
export interface RegisterRequest {
  username: string;
  email: string;
  phone?: string;
  firstName: string;
  lastName: string;
  password: string;
  confirmPassword: string;
}

// 用户搜索请求
export interface UserSearchRequest {
  username?: string;
  email?: string;
  role?: UserRole;
  status?: UserStatus;
  hotelId?: number;
  sortBy?: 'USERNAME' | 'EMAIL' | 'CREATED_AT';
  sortOrder?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
}

// 用户搜索结果
export interface UserSearchResult {
  users: User[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

// JWT载荷接口
export interface JwtPayload {
  sub: string; // 用户ID
  username: string;
  email: string;
  role: UserRole;
  hotelId?: number;
  iat: number; // 签发时间
  exp: number; // 过期时间
}