/**
 * 房间相关类型定义
 */

// 房间状态枚举
export enum RoomStatus {
  AVAILABLE = 'AVAILABLE',
  OCCUPIED = 'OCCUPIED',
  MAINTENANCE = 'MAINTENANCE',
  CLEANING = 'CLEANING'
}

// 房间实体接口
export interface Room {
  id: number;
  hotelId: number;
  roomTypeId: number;
  roomNumber: string;
  floor: number;
  area: number;
  status: RoomStatus;
  price: number;
  images: string[];
  createdAt: string;
  updatedAt: string;
}

// 房间类型接口
export interface RoomType {
  id: number;
  hotelId: number;
  name: string;
  capacity: number;
  basePrice: number;
  facilities: string[];
  description: string;
  createdAt: string;
}

// 搜索请求DTO
export interface RoomSearchRequest {
  hotelId?: number;
  roomTypeId?: number;
  checkInDate: string; // YYYY-MM-DD
  checkOutDate: string; // YYYY-MM-DD
  guestCount: number;
  priceMin?: number;
  priceMax?: number;
  facilities?: string[];
  sortBy?: 'PRICE' | 'RATING' | 'DISTANCE' | 'ROOM_NUMBER';
  sortOrder?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
}

// 房间搜索结果（包含类型和酒店信息）
export interface RoomWithTypeInfo extends Room {
  roomTypeName: string;
  roomTypeCapacity: number;
  hotelName: string;
  hotelAddress: string;
  hotelRating?: number;
  distance?: number; // 计算得到的距离
}

// 搜索响应DTO
export interface RoomSearchResponse {
  rooms: RoomWithTypeInfo[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

// 分页搜索结果DTO
export interface RoomSearchResult {
  rooms: RoomWithTypeInfo[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// 房间可用性接口
export interface RoomAvailability {
  roomId: number;
  date: string; // YYYY-MM-DD
  available: boolean;
  status?: 'AVAILABLE' | 'BOOKED' | 'MAINTENANCE' | 'BLOCKED';
  price?: number; // 当天的价格（动态定价）
}

// 批量更新房间请求
export interface BatchUpdateRequest {
  roomIds: number[];
  updates: Partial<Room>;
}

// 房间统计信息
export interface RoomStats {
  totalRooms: number;
  availableRooms: number;
  occupiedRooms: number;
  maintenanceRooms: number;
  occupancyRate: number; // 入住率百分比
  averagePrice: number;
  revenue: number;
}

// 房间图片接口
export interface RoomImage {
  id: number;
  roomId: number;
  url: string;
  alt: string;
  sortOrder: number;
  isPrimary: boolean;
}

// 设施接口
export interface Facility {
  id: number;
  name: string;
  icon: string;
  category: string;
}