import { apiClient } from '@/utils/apiClient'
import type { AxiosResponse } from 'axios'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data?: T
  code: number
  timestamp: number
}

// 包装 apiClient 以统一处理 ApiResponse 格式
export const client = {
  async get<T = any>(url: string, config?: any): Promise<ApiResponse<T>> {
    const response: AxiosResponse<ApiResponse<T>> = await apiClient.get(url, config)
    return response.data
  },

  async post<T = any>(url: string, data?: any, config?: any): Promise<ApiResponse<T>> {
    const response: AxiosResponse<ApiResponse<T>> = await apiClient.post(url, data, config)
    return response.data
  },

  async put<T = any>(url: string, data?: any, config?: any): Promise<ApiResponse<T>> {
    const response: AxiosResponse<ApiResponse<T>> = await apiClient.put(url, data, config)
    return response.data
  },

  async delete<T = any>(url: string, config?: any): Promise<ApiResponse<T>> {
    const response: AxiosResponse<ApiResponse<T>> = await apiClient.delete(url, config)
    return response.data
  },

  async patch<T = any>(url: string, data?: any, config?: any): Promise<ApiResponse<T>> {
    const response: AxiosResponse<ApiResponse<T>> = await apiClient.patch(url, data, config)
    return response.data
  }
}

// 向后兼容，直接导出 apiClient
export { apiClient }