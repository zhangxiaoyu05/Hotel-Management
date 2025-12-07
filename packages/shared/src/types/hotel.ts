/**
 * 酒店相关类型定义
 */

// 酒店状态枚举
export enum HotelStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED'
}

// 酒店实体接口
export interface Hotel {
  id: number;
  name: string;
  address: string;
  phone: string;
  email?: string;
  description?: string;
  facilities: string[];
  images: string[];
  status: HotelStatus;
  rating?: number;
  reviewCount?: number;
  checkInTime: string; // HH:mm
  checkOutTime: string; // HH:mm
  latitude?: number;
  longitude?: number;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

// 酒店创建请求DTO
export interface CreateHotelRequest {
  name: string;
  address: string;
  phone: string;
  email?: string;
  description?: string;
  facilities: string[];
  checkInTime: string;
  checkOutTime: string;
  latitude?: number;
  longitude?: number;
}

// 酒店更新请求DTO
export interface UpdateHotelRequest {
  name?: string;
  address?: string;
  phone?: string;
  email?: string;
  description?: string;
  facilities?: string[];
  status?: HotelStatus;
  checkInTime?: string;
  checkOutTime?: string;
  latitude?: number;
  longitude?: number;
}

// 酒店搜索请求
export interface HotelSearchRequest {
  name?: string;
  address?: string;
  facilities?: string[];
  minRating?: number;
  maxRating?: number;
  sortBy?: 'NAME' | 'RATING' | 'PRICE' | 'DISTANCE';
  sortOrder?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
}

// 酒店搜索结果
export interface HotelSearchResult {
  hotels: Hotel[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

// 酒店统计信息
export interface HotelStats {
  totalHotels: number;
  activeHotels: number;
  totalRooms: number;
  averageRating: number;
  totalReviews: number;
}