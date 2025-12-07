import { apiClient } from '../utils/apiClient'
import type {
  PricingRule,
  CreatePricingRuleRequest,
  UpdatePricingRuleRequest,
  PricingRuleQuery,
  SpecialPrice,
  CreateSpecialPriceRequest,
  UpdateSpecialPriceRequest,
  SpecialPriceQuery,
  PriceHistory,
  PriceHistoryQuery,
  Holiday,
  PriceCalculationRequest,
  PriceCalculationResponse,
  CopySpecialPriceRequest,
  ApiResponse,
  PaginatedResponse
} from '../types/pricing'

class PricingService {
  private baseURL = '/api/v1/pricing'

  // 价格规则相关API
  async getPricingRules(query: PricingRuleQuery): Promise<PaginatedResponse<PricingRule>> {
    const params = new URLSearchParams()

    if (query.page !== undefined) params.append('pageNum', (query.page + 1).toString())
    if (query.size !== undefined) params.append('pageSize', query.size.toString())
    if (query.keyword) params.append('keyword', query.keyword)
    if (query.ruleType) params.append('ruleType', query.ruleType)
    if (query.roomTypeId) params.append('roomTypeId', query.roomTypeId.toString())
    if (query.activeOnly !== undefined) params.append('activeOnly', query.activeOnly.toString())

    const url = `${this.baseURL}/rules?${params.toString()}`
    const response = await apiClient.get<ApiResponse<PricingRule[]>>(url)

    // 模拟分页响应格式
    return {
      data: response.data.data || [],
      total: response.data.data?.length || 0,
      page: query.page || 0,
      size: query.size || 20,
      totalPages: 1
    }
  }

  async getPricingRuleById(id: number): Promise<PricingRule> {
    const response = await apiClient.get<ApiResponse<PricingRule>>(`${this.baseURL}/rules/${id}`)
    return response.data.data
  }

  async createPricingRule(data: CreatePricingRuleRequest): Promise<ApiResponse<PricingRule>> {
    const response = await apiClient.post<ApiResponse<PricingRule>>(`${this.baseURL}/rules`, data)
    return response.data
  }

  async updatePricingRule(id: number, data: UpdatePricingRuleRequest): Promise<ApiResponse<PricingRule>> {
    const response = await apiClient.put<ApiResponse<PricingRule>>(`${this.baseURL}/rules/${id}`, data)
    return response.data
  }

  async deletePricingRule(id: number): Promise<void> {
    await apiClient.delete(`${this.baseURL}/rules/${id}`)
  }

  async toggleRuleStatus(id: number, active: boolean): Promise<void> {
    await apiClient.patch(`${this.baseURL}/rules/${id}/status?active=${active}`)
  }

  async batchToggleRules(ids: number[], active: boolean): Promise<void> {
    await apiClient.patch(`${this.baseURL}/rules/batch/status?active=${active}`, { ids })
  }

  async batchDeleteRules(ids: number[]): Promise<void> {
    await apiClient.delete(`${this.baseURL}/rules/batch`, { data: { ids } })
  }

  // 特殊价格相关API
  async getSpecialPrices(query: SpecialPriceQuery): Promise<PaginatedResponse<SpecialPrice>> {
    const params = new URLSearchParams()

    if (query.page !== undefined) params.append('pageNum', (query.page + 1).toString())
    if (query.size !== undefined) params.append('pageSize', query.size.toString())
    if (query.startDate) params.append('startDate', query.startDate)
    if (query.endDate) params.append('endDate', query.endDate)
    if (query.roomTypeId) params.append('roomTypeId', query.roomTypeId.toString())

    const url = `${this.baseURL}/special-prices?${params.toString()}`
    const response = await apiClient.get<ApiResponse<SpecialPrice[]>>(url)

    return {
      data: response.data.data || [],
      total: response.data.data?.length || 0,
      page: query.page || 0,
      size: query.size || 20,
      totalPages: 1
    }
  }

  async getSpecialPriceById(id: number): Promise<SpecialPrice> {
    const response = await apiClient.get<ApiResponse<SpecialPrice>>(`${this.baseURL}/special-prices/${id}`)
    return response.data.data
  }

  async createSpecialPrice(data: CreateSpecialPriceRequest): Promise<ApiResponse<SpecialPrice>> {
    const response = await apiClient.post<ApiResponse<SpecialPrice>>(`${this.baseURL}/special-prices`, data)
    return response.data
  }

  async updateSpecialPrice(id: number, data: UpdateSpecialPriceRequest): Promise<ApiResponse<SpecialPrice>> {
    const response = await apiClient.put<ApiResponse<SpecialPrice>>(`${this.baseURL}/special-prices/${id}`, data)
    return response.data
  }

  async deleteSpecialPrice(id: number): Promise<void> {
    await apiClient.delete(`${this.baseURL}/special-prices/${id}`)
  }

  async copySpecialPrice(id: number, targetDates: string[]): Promise<ApiResponse<SpecialPrice[]>> {
    const response = await apiClient.post<ApiResponse<SpecialPrice[]>>(`${this.baseURL}/special-prices/${id}/copy`, { targetDates })
    return response.data
  }

  async getFutureSpecialPrices(hotelId: number, fromDate?: string): Promise<ApiResponse<SpecialPrice[]>> {
    const params = fromDate ? `?fromDate=${fromDate}` : ''
    const response = await apiClient.get<ApiResponse<SpecialPrice[]>>(`${this.baseURL}/special-prices/future?hotelId=${hotelId}${params}`)
    return response.data
  }

  // 价格历史相关API
  async getPriceHistory(query: PriceHistoryQuery): Promise<PaginatedResponse<PriceHistory>> {
    const params = new URLSearchParams()

    params.append('hotelId', query.hotelId.toString())
    if (query.roomId) params.append('roomId', query.roomId.toString())
    if (query.roomTypeId) params.append('roomTypeId', query.roomTypeId.toString())
    if (query.changeType) params.append('changeType', query.changeType)
    if (query.startTime) params.append('startTime', query.startTime)
    if (query.endTime) params.append('endTime', query.endTime)
    if (query.page !== undefined) params.append('pageNum', (query.page + 1).toString())
    if (query.size !== undefined) params.append('pageSize', query.size.toString())
    if (query.limit) params.append('limit', query.limit.toString())

    const url = `${this.baseURL}/history?${params.toString()}`
    const response = await apiClient.get<ApiResponse<PriceHistory[]>>(url)

    return {
      data: response.data.data || [],
      total: response.data.data?.length || 0,
      page: query.page || 0,
      size: query.size || 20,
      totalPages: 1
    }
  }

  async getPriceChangeStatistics(hotelId: number, days: number): Promise<ApiResponse<any>> {
    const response = await apiClient.get<ApiResponse<any>>(`${this.baseURL}/history/statistics?hotelId=${hotelId}&days=${days}`)
    return response.data
  }

  async getPriceChangeTrend(hotelId: number, days: number): Promise<ApiResponse<any>> {
    const response = await apiClient.get<ApiResponse<any>>(`${this.baseURL}/history/trend?hotelId=${hotelId}&days=${days}`)
    return response.data
  }

  // 节假日相关API
  async getHolidays(query: { year?: number; startDate?: string; endDate?: string }): Promise<ApiResponse<Holiday[]>> {
    const params = new URLSearchParams()

    if (query.year) params.append('year', query.year.toString())
    if (query.startDate) params.append('startDate', query.startDate)
    if (query.endDate) params.append('endDate', query.endDate)

    const url = `${this.baseURL}/holidays${params.toString() ? `?${params.toString()}` : ''}`
    const response = await apiClient.get<ApiResponse<Holiday[]>>(url)
    return response.data
  }

  async checkHoliday(date: string): Promise<ApiResponse<any>> {
    const response = await apiClient.get<ApiResponse<any>>(`${this.baseURL}/holidays/check?date=${date}`)
    return response.data
  }

  // 价格计算相关API
  async calculateRoomPrice(roomId: number, date: string): Promise<ApiResponse<number>> {
    const response = await apiClient.get<ApiResponse<number>>(`${this.baseURL}/calculate/room/${roomId}?date=${date}`)
    return response.data
  }

  async calculateRoomTypePrice(roomTypeId: number, hotelId: number, date: string): Promise<ApiResponse<number>> {
    const response = await apiClient.get<ApiResponse<number>>(`${this.baseURL}/calculate/room-type/${roomTypeId}?hotelId=${hotelId}&date=${date}`)
    return response.data
  }

  async calculatePriceRange(request: PriceCalculationRequest): Promise<ApiResponse<PriceCalculationResponse>> {
    const response = await apiClient.post<ApiResponse<PriceCalculationResponse>>(`${this.baseURL}/calculate/range`, request)
    return response.data
  }

  // 价格预览API
  async getPricePreview(hotelId: number, roomTypeId: number, startDate: string, endDate: string): Promise<ApiResponse<any>> {
    const response = await apiClient.get<ApiResponse<any>>(
      `${this.baseURL}/preview?hotelId=${hotelId}&roomTypeId=${roomTypeId}&startDate=${startDate}&endDate=${endDate}`
    )
    return response.data
  }

  // 价格策略配置API
  async getPricingConfig(hotelId: number): Promise<ApiResponse<any>> {
    const response = await apiClient.get<ApiResponse<any>>(`${this.baseURL}/config?hotelId=${hotelId}`)
    return response.data
  }

  async updatePricingConfig(hotelId: number, config: any): Promise<ApiResponse<any>> {
    const response = await apiClient.put<ApiResponse<any>>(`${this.baseURL}/config?hotelId=${hotelId}`, config)
    return response.data
  }
}

export const pricingService = new PricingService()