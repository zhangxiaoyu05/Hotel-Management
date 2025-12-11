export interface RoomType {
  id: number
  hotelId: number
  name: string
  capacity: number
  basePrice: number
  facilities: string[]
  description: string
  iconUrl?: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
  updatedAt?: string
}

export interface CreateRoomTypeRequest {
  name: string
  capacity: number
  basePrice: number
  facilities?: string[]
  description?: string
  iconUrl?: string
}

export interface UpdateRoomTypeRequest {
  name?: string
  capacity?: number
  basePrice?: number
  facilities?: string[]
  description?: string
  iconUrl?: string
  status?: 'ACTIVE' | 'INACTIVE'
}

export interface RoomTypeListResponse {
  content: RoomType[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface RoomTypeListQuery {
  page?: number
  size?: number
  search?: string
  hotelId?: number
  status?: string
  sortBy?: string
  sortDir?: 'ASC' | 'DESC'
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  error?: {
    code: string
    message: string
    timestamp: string
    requestId: string
  }
}