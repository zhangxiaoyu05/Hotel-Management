import { apiClient } from '../utils/apiClient'
import type {
  Hotel,
  CreateHotelRequest,
  UpdateHotelRequest,
  HotelListResponse,
  HotelListQuery,
  ApiResponse
} from '../types/hotel'

class HotelService {
  private baseURL = '/api/hotels'

  async getHotels(query?: HotelListQuery): Promise<HotelListResponse> {
    const params = new URLSearchParams()

    if (query?.page !== undefined) params.append('page', query.page.toString())
    if (query?.size !== undefined) params.append('size', query.size.toString())
    if (query?.search) params.append('search', query.search)
    if (query?.status) params.append('status', query.status)
    if (query?.sortBy) params.append('sortBy', query.sortBy)
    if (query?.sortDir) params.append('sortDir', query.sortDir)

    const url = params.toString() ? `${this.baseURL}?${params}` : this.baseURL
    const response = await apiClient.get<ApiResponse<HotelListResponse>>(url)
    return response.data.data
  }

  async getHotelById(id: number): Promise<Hotel> {
    const response = await apiClient.get<ApiResponse<Hotel>>(`${this.baseURL}/${id}`)
    return response.data.data
  }

  async createHotel(hotel: CreateHotelRequest): Promise<Hotel> {
    const response = await apiClient.post<ApiResponse<Hotel>>(this.baseURL, hotel)
    return response.data.data
  }

  async updateHotel(id: number, hotel: UpdateHotelRequest): Promise<Hotel> {
    const response = await apiClient.put<ApiResponse<Hotel>>(`${this.baseURL}/${id}`, hotel)
    return response.data.data
  }

  async deleteHotel(id: number): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`${this.baseURL}/${id}`)
  }

  async updateHotelStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Promise<Hotel> {
    const response = await apiClient.patch<ApiResponse<Hotel>>(`${this.baseURL}/${id}/status`, { status })
    return response.data.data
  }
}

export const hotelService = new HotelService()