export interface Hotel {
  id: number
  name: string
  address: string
  phone: string
  description: string
  facilities: string[]
  images: string[]
  status: 'ACTIVE' | 'INACTIVE'
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface CreateHotelRequest {
  name: string
  address: string
  phone?: string
  description?: string
  facilities?: string[]
  images?: string[]
}

export interface UpdateHotelRequest {
  name?: string
  address?: string
  phone?: string
  description?: string
  facilities?: string[]
  images?: string[]
  status?: 'ACTIVE' | 'INACTIVE'
}

export interface HotelListResponse {
  content: Hotel[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface HotelListQuery {
  page?: number
  size?: number
  search?: string
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