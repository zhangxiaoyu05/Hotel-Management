import { apiClient } from './client'
import type {
  RoomType,
  CreateRoomTypeRequest,
  UpdateRoomTypeRequest,
  RoomTypeListResponse,
  RoomTypeListQuery,
  ApiResponse
} from '../types/roomType'

export const roomTypeApi = {
  // 获取房间类型列表（带分页和搜索）
  async getRoomTypes(query?: RoomTypeListQuery): Promise<RoomTypeListResponse> {
    const params = new URLSearchParams()

    if (query?.page !== undefined) params.append('page', query.page.toString())
    if (query?.size !== undefined) params.append('size', query.size.toString())
    if (query?.search) params.append('search', query.search)
    if (query?.hotelId) params.append('hotelId', query.hotelId.toString())
    if (query?.status) params.append('status', query.status)
    if (query?.sortBy) params.append('sortBy', query.sortBy)
    if (query?.sortDir) params.append('sortDir', query.sortDir)

    const response = await apiClient.get<ApiResponse<RoomTypeListResponse>>(
      `/room-types${params.toString() ? `?${params.toString()}` : ''}`
    )
    return response.data.data
  },

  // 根据酒店ID获取房间类型
  async getRoomTypesByHotel(hotelId: number): Promise<RoomType[]> {
    const response = await apiClient.get<ApiResponse<RoomType[]>>(
      `/room-types/hotel/${hotelId}`
    )
    return response.data.data
  },

  // 获取房间类型详情
  async getRoomType(id: number): Promise<RoomType> {
    const response = await apiClient.get<ApiResponse<RoomType>>(`/room-types/${id}`)
    return response.data.data
  },

  // 创建房间类型
  async createRoomType(data: CreateRoomTypeRequest): Promise<RoomType> {
    const response = await apiClient.post<ApiResponse<RoomType>>('/room-types', data)
    return response.data.data
  },

  // 更新房间类型
  async updateRoomType(id: number, data: UpdateRoomTypeRequest): Promise<RoomType> {
    const response = await apiClient.put<ApiResponse<RoomType>>(`/room-types/${id}`, data)
    return response.data.data
  },

  // 删除房间类型
  async deleteRoomType(id: number): Promise<void> {
    await apiClient.delete(`/room-types/${id}`)
  },

  // 获取与房间类型关联的房间
  async getRoomsByRoomType(id: number): Promise<any[]> {
    const response = await apiClient.get<ApiResponse<any[]>>(`/room-types/${id}/rooms`)
    return response.data.data
  }
}