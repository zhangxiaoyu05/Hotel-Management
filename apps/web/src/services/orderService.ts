import { apiClient } from '@/utils/apiClient'
import type {
  CreateOrderRequest,
  UpdateOrderRequest,
  OrderResponse,
  OrderListResponse,
  Order
} from '@/types/order'

export const orderService = {
  async createOrder(request: CreateOrderRequest): Promise<OrderResponse> {
    const response = await apiClient.post('/orders', request)
    return response.data.data
  },

  async getOrder(orderId: number): Promise<OrderResponse> {
    const response = await apiClient.get(`/orders/${orderId}`)
    return response.data.data
  },

  async getOrderList(
    status?: string,
    page?: number,
    size?: number,
    sortBy?: string,
    sortOrder?: string,
    search?: string
  ): Promise<OrderListResponse[]> {
    const params: any = {}
    if (status) params.status = status
    if (page) params.page = page
    if (size) params.size = size
    if (sortBy) params.sortBy = sortBy
    if (sortOrder) params.sortOrder = sortOrder
    if (search) params.search = search

    const response = await apiClient.get('/orders', { params })
    return response.data.data
  },

  async getUserOrders(status?: string): Promise<OrderListResponse[]> {
    return this.getOrderList(status)
  },

  async getOrderByNumber(orderNumber: string): Promise<OrderResponse> {
    const response = await apiClient.get(`/orders/number/${orderNumber}`)
    return response.data.data
  },

  async updateOrder(orderId: number, request: UpdateOrderRequest): Promise<OrderResponse> {
    const response = await apiClient.put(`/orders/${orderId}`, request)
    return response.data.data
  },

  async cancelOrder(orderId: number, cancelReason?: string): Promise<OrderResponse> {
    const response = await apiClient.put(`/orders/${orderId}/cancel`, cancelReason)
    return response.data.data
  }
}