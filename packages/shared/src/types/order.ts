/**
 * 订单相关类型定义
 */

// 订单状态枚举
export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  REFUNDED = 'REFUNDED'
}

// 支付状态枚举
export enum PaymentStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED'
}

// 订单实体接口
export interface Order {
  id: number;
  orderNumber: string; // 唯一订单号
  roomId: number;
  userId: number;
  hotelId: number;
  checkInDate: string; // YYYY-MM-DD
  checkOutDate: string; // YYYY-MM-DD
  guestCount: number;
  totalPrice: number;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  specialRequests?: string;
  cancellationReason?: string;
  cancelledAt?: string;
  confirmedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}

// 创建订单请求DTO
export interface CreateOrderRequest {
  roomId: number;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  specialRequests?: string;
}

// 订单更新请求DTO
export interface UpdateOrderRequest {
  status?: OrderStatus;
  paymentStatus?: PaymentStatus;
  specialRequests?: string;
  cancellationReason?: string;
}

// 订单搜索请求
export interface OrderSearchRequest {
  orderNumber?: string;
  userId?: number;
  roomId?: number;
  hotelId?: number;
  status?: OrderStatus;
  paymentStatus?: PaymentStatus;
  checkInDateFrom?: string;
  checkInDateTo?: string;
  checkOutDateFrom?: string;
  checkOutDateTo?: string;
  sortBy?: 'CREATED_AT' | 'CHECK_IN_DATE' | 'TOTAL_PRICE';
  sortOrder?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
}

// 订单搜索结果
export interface OrderSearchResult {
  orders: Order[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

// 订单统计信息
export interface OrderStats {
  totalOrders: number;
  pendingOrders: number;
  confirmedOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  totalRevenue: number;
  averageOrderValue: number;
  occupancyRate: number;
}