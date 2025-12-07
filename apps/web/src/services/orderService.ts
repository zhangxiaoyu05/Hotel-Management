import { apiClient } from '@/utils/apiClient'
import type {
  CreateOrderRequest,
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

  async getUserOrders(status?: string): Promise<OrderListResponse[]> {
    const params = status ? { status } : {}
    const response = await apiClient.get('/orders', { params })
    return response.data.data
  },

  async getOrderByNumber(orderNumber: string): Promise<OrderResponse> {
    const response = await apiClient.get(`/orders/number/${orderNumber}`)
    return response.data.data
  },

  async cancelOrder(orderId: number): Promise<boolean> {
    const response = await apiClient.put(`/orders/${orderId}/cancel`)
    return response.data.data
  }
}