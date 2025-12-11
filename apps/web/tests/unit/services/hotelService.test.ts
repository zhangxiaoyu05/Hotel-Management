import { describe, it, expect, vi, beforeEach } from 'vitest'
import { hotelService } from '@/services/hotelService'
import { apiClient } from '@/utils/apiClient'
import type { CreateHotelRequest, UpdateHotelRequest, HotelListQuery } from '@/services/hotelService'

// Mock apiClient
vi.mock('@/utils/apiClient', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('HotelService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getHotels', () => {
    it('should successfully get hotel list with default parameters', async () => {
      const mockResponse = {
        data: {
          success: true,
          data: {
            content: [
              {
                id: 1,
                name: 'Test Hotel 1',
                address: 'Test Address 1',
                phone: '13800138001',
                description: 'Test Description 1',
                facilities: ['WiFi', 'Parking'],
                images: ['hotel1.jpg'],
                status: 'ACTIVE',
                createdBy: 1,
                createdAt: '2025-12-06T10:00:00Z',
                updatedAt: '2025-12-06T10:00:00Z'
              }
            ],
            totalElements: 1,
            totalPages: 1,
            size: 20,
            number: 0
          }
        }
      }

      ;(apiClient.get as any).mockResolvedValue(mockResponse)

      const result = await hotelService.getHotels()

      expect(apiClient.get).toHaveBeenCalledWith('/hotels', {
        params: {
          page: 0,
          size: 20
        }
      })
      expect(result).toEqual(mockResponse.data)
    })

    it('should successfully get hotel list with custom parameters', async () => {
      const queryParams: HotelListQuery = {
        page: 1,
        size: 10,
        search: 'Test Hotel',
        status: 'ACTIVE',
        sortBy: 'name',
        sortDir: 'ASC'
      }

      const mockResponse = {
        data: {
          success: true,
          data: {
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 10,
            number: 1
          }
        }
      }

      ;(apiClient.get as any).mockResolvedValue(mockResponse)

      const result = await hotelService.getHotels(queryParams)

      expect(apiClient.get).toHaveBeenCalledWith('/hotels', {
        params: queryParams
      })
      expect(result).toEqual(mockResponse.data)
    })

    it('should handle get hotels error', async () => {
      const mockError = {
        response: {
          data: {
            message: '获取酒店列表失败'
          }
        }
      }

      ;(apiClient.get as any).mockRejectedValue(mockError)

      await expect(hotelService.getHotels()).rejects.toThrow('获取酒店列表失败')
    })

    it('should handle network error', async () => {
      ;(apiClient.get as any).mockRejectedValue(new Error('Network Error'))

      await expect(hotelService.getHotels()).rejects.toThrow('获取酒店列表失败，请稍后重试')
    })
  })

  describe('getHotelById', () => {
    it('should successfully get hotel by id', async () => {
      const hotelId = 1
      const mockResponse = {
        data: {
          success: true,
          data: {
            id: 1,
            name: 'Test Hotel 1',
            address: 'Test Address 1',
            phone: '13800138001',
            description: 'Test Description 1',
            facilities: ['WiFi', 'Parking'],
            images: ['hotel1.jpg'],
            status: 'ACTIVE',
            createdBy: 1,
            createdAt: '2025-12-06T10:00:00Z',
            updatedAt: '2025-12-06T10:00:00Z'
          }
        }
      }

      ;(apiClient.get as any).mockResolvedValue(mockResponse)

      const result = await hotelService.getHotelById(hotelId)

      expect(apiClient.get).toHaveBeenCalledWith(`/hotels/${hotelId}`)
      expect(result).toEqual(mockResponse.data)
    })

    it('should handle hotel not found error', async () => {
      const hotelId = 999
      const mockError = {
        response: {
          data: {
            message: '酒店不存在'
          }
        }
      }

      ;(apiClient.get as any).mockRejectedValue(mockError)

      await expect(hotelService.getHotelById(hotelId)).rejects.toThrow('酒店不存在')
    })

    it('should handle network error', async () => {
      const hotelId = 1
      ;(apiClient.get as any).mockRejectedValue(new Error('Network Error'))

      await expect(hotelService.getHotelById(hotelId)).rejects.toThrow('获取酒店详情失败，请稍后重试')
    })
  })

  describe('createHotel', () => {
    it('should successfully create a hotel', async () => {
      const hotelData: CreateHotelRequest = {
        name: 'New Hotel',
        address: 'New Address',
        phone: '13800138001',
        description: 'New Description',
        facilities: ['WiFi', 'Pool', 'Gym'],
        images: ['hotel1.jpg', 'hotel2.jpg']
      }

      const mockResponse = {
        data: {
          success: true,
          message: '酒店创建成功',
          data: {
            id: 2,
            name: hotelData.name,
            address: hotelData.address,
            phone: hotelData.phone,
            description: hotelData.description,
            facilities: hotelData.facilities,
            images: hotelData.images,
            status: 'ACTIVE',
            createdBy: 1,
            createdAt: '2025-12-06T11:00:00Z',
            updatedAt: '2025-12-06T11:00:00Z'
          }
        }
      }

      ;(apiClient.post as any).mockResolvedValue(mockResponse)

      const result = await hotelService.createHotel(hotelData)

      expect(apiClient.post).toHaveBeenCalledWith('/hotels', hotelData)
      expect(result).toEqual(mockResponse.data)
    })

    it('should handle validation error', async () => {
      const hotelData: CreateHotelRequest = {
        name: '',
        address: ''
      }

      const mockError = {
        response: {
          data: {
            message: '酒店名称和地址不能为空'
          }
        }
      }

      ;(apiClient.post as any).mockRejectedValue(mockError)

      await expect(hotelService.createHotel(hotelData)).rejects.toThrow('酒店名称和地址不能为空')
    })

    it('should handle duplicate hotel name error', async () => {
      const hotelData: CreateHotelRequest = {
        name: 'Existing Hotel',
        address: 'Test Address'
      }

      const mockError = {
        response: {
          data: {
            message: '酒店名称已存在'
          }
        }
      }

      ;(apiClient.post as any).mockRejectedValue(mockError)

      await expect(hotelService.createHotel(hotelData)).rejects.toThrow('酒店名称已存在')
    })

    it('should handle network error', async () => {
      const hotelData: CreateHotelRequest = {
        name: 'Test Hotel',
        address: 'Test Address'
      }

      ;(apiClient.post as any).mockRejectedValue(new Error('Network Error'))

      await expect(hotelService.createHotel(hotelData)).rejects.toThrow('创建酒店失败，请稍后重试')
    })
  })

  describe('updateHotel', () => {
    it('should successfully update a hotel', async () => {
      const hotelId = 1
      const updateData: UpdateHotelRequest = {
        name: 'Updated Hotel Name',
        description: 'Updated Description'
      }

      const mockResponse = {
        data: {
          success: true,
          message: '酒店更新成功',
          data: {
            id: hotelId,
            name: updateData.name,
            address: 'Original Address',
            phone: '13800138001',
            description: updateData.description,
            facilities: ['WiFi'],
            images: ['hotel1.jpg'],
            status: 'ACTIVE',
            createdBy: 1,
            createdAt: '2025-12-06T10:00:00Z',
            updatedAt: '2025-12-06T12:00:00Z'
          }
        }
      }

      ;(apiClient.put as any).mockResolvedValue(mockResponse)

      const result = await hotelService.updateHotel(hotelId, updateData)

      expect(apiClient.put).toHaveBeenCalledWith(`/hotels/${hotelId}`, updateData)
      expect(result).toEqual(mockResponse.data)
    })

    it('should handle hotel not found error', async () => {
      const hotelId = 999
      const updateData: UpdateHotelRequest = {
        name: 'Updated Name'
      }

      const mockError = {
        response: {
          data: {
            message: '酒店不存在'
          }
        }
      }

      ;(apiClient.put as any).mockRejectedValue(mockError)

      await expect(hotelService.updateHotel(hotelId, updateData)).rejects.toThrow('酒店不存在')
    })

    it('should handle network error', async () => {
      const hotelId = 1
      const updateData: UpdateHotelRequest = {
        name: 'Updated Name'
      }

      ;(apiClient.put as any).mockRejectedValue(new Error('Network Error'))

      await expect(hotelService.updateHotel(hotelId, updateData)).rejects.toThrow('更新酒店失败，请稍后重试')
    })
  })

  describe('deleteHotel', () => {
    it('should successfully delete a hotel', async () => {
      const hotelId = 1
      const mockResponse = {
        data: {
          success: true,
          message: '酒店删除成功'
        }
      }

      ;(apiClient.delete as any).mockResolvedValue(mockResponse)

      const result = await hotelService.deleteHotel(hotelId)

      expect(apiClient.delete).toHaveBeenCalledWith(`/hotels/${hotelId}`)
      expect(result).toEqual(mockResponse.data)
    })

    it('should handle hotel not found error', async () => {
      const hotelId = 999
      const mockError = {
        response: {
          data: {
            message: '酒店不存在'
          }
        }
      }

      ;(apiClient.delete as any).mockRejectedValue(mockError)

      await expect(hotelService.deleteHotel(hotelId)).rejects.toThrow('酒店不存在')
    })

    it('should handle network error', async () => {
      const hotelId = 1
      ;(apiClient.delete as any).mockRejectedValue(new Error('Network Error'))

      await expect(hotelService.deleteHotel(hotelId)).rejects.toThrow('删除酒店失败，请稍后重试')
    })
  })

  describe('updateHotelStatus', () => {
    it('should successfully update hotel status', async () => {
      const hotelId = 1
      const status = 'INACTIVE'
      const mockResponse = {
        data: {
          success: true,
          message: '酒店状态更新成功',
          data: {
            id: hotelId,
            name: 'Test Hotel',
            address: 'Test Address',
            status: status
          }
        }
      }

      ;(apiClient.put as any).mockResolvedValue(mockResponse)

      const result = await hotelService.updateHotelStatus(hotelId, status)

      expect(apiClient.put).toHaveBeenCalledWith(`/hotels/${hotelId}/status`, { status })
      expect(result).toEqual(mockResponse.data)
    })

    it('should handle hotel not found error', async () => {
      const hotelId = 999
      const status = 'INACTIVE'
      const mockError = {
        response: {
          data: {
            message: '酒店不存在'
          }
        }
      }

      ;(apiClient.put as any).mockRejectedValue(mockError)

      await expect(hotelService.updateHotelStatus(hotelId, status)).rejects.toThrow('酒店不存在')
    })

    it('should handle network error', async () => {
      const hotelId = 1
      const status = 'INACTIVE'
      ;(apiClient.put as any).mockRejectedValue(new Error('Network Error'))

      await expect(hotelService.updateHotelStatus(hotelId, status)).rejects.toThrow('更新酒店状态失败，请稍后重试')
    })
  })
})