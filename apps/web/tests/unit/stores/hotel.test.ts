import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useHotelStore } from '@/stores/hotelStore'
import { hotelService } from '@/services/hotelService'
import type { Hotel, CreateHotelRequest, UpdateHotelRequest } from '@/services/hotelService'

// Mock hotelService
vi.mock('@/services/hotelService', () => ({
  hotelService: {
    getHotels: vi.fn(),
    getHotelById: vi.fn(),
    createHotel: vi.fn(),
    updateHotel: vi.fn(),
    deleteHotel: vi.fn(),
    updateHotelStatus: vi.fn()
  }
}))

describe('HotelStore', () => {
  let hotelStore: ReturnType<typeof useHotelStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    hotelStore = useHotelStore()
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have correct initial state', () => {
      expect(hotelStore.hotels).toEqual([])
      expect(hotelStore.currentHotel).toBeNull()
      expect(hotelStore.loading).toBe(false)
      expect(hotelStore.error).toBeNull()
      expect(hotelStore.totalElements).toBe(0)
      expect(hotelStore.totalPages).toBe(0)
      expect(hotelStore.currentPage).toBe(0)
      expect(hotelStore.pageSize).toBe(20)
      expect(hotelStore.searchQuery).toBe('')
      expect(hotelStore.statusFilter).toBe('')
      expect(hotelStore.sortBy).toBe('createdAt')
      expect(hotelStore.sortDir).toBe('DESC')
    })
  })

  describe('fetchHotels', () => {
    it('should successfully fetch hotels', async () => {
      const mockHotels: Hotel[] = [
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
      ]

      const mockResponse = {
        success: true,
        data: {
          content: mockHotels,
          totalElements: 1,
          totalPages: 1,
          size: 20,
          number: 0
        }
      }

      vi.mocked(hotelService.getHotels).mockResolvedValue(mockResponse)

      await hotelStore.fetchHotels()

      expect(hotelStore.loading).toBe(false)
      expect(hotelStore.hotels).toEqual(mockHotels)
      expect(hotelStore.totalElements).toBe(1)
      expect(hotelStore.totalPages).toBe(1)
      expect(hotelStore.currentPage).toBe(0)
      expect(hotelStore.error).toBeNull()
    })

    it('should handle fetch hotels error', async () => {
      const errorMessage = '获取酒店列表失败'
      vi.mocked(hotelService.getHotels).mockRejectedValue(new Error(errorMessage))

      await hotelStore.fetchHotels()

      expect(hotelStore.loading).toBe(false)
      expect(hotelStore.hotels).toEqual([])
      expect(hotelStore.error).toBe(errorMessage)
    })

    it('should use correct query parameters', async () => {
      hotelStore.searchQuery = 'Test Hotel'
      hotelStore.statusFilter = 'ACTIVE'
      hotelStore.currentPage = 1
      hotelStore.pageSize = 10
      hotelStore.sortBy = 'name'
      hotelStore.sortDir = 'ASC'

      const mockResponse = {
        success: true,
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 10,
          number: 1
        }
      }

      vi.mocked(hotelService.getHotels).mockResolvedValue(mockResponse)

      await hotelStore.fetchHotels()

      expect(hotelService.getHotels).toHaveBeenCalledWith({
        page: 1,
        size: 10,
        search: 'Test Hotel',
        status: 'ACTIVE',
        sortBy: 'name',
        sortDir: 'ASC'
      })
    })
  })

  describe('fetchHotelById', () => {
    it('should successfully fetch hotel by id', async () => {
      const mockHotel: Hotel = {
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

      const mockResponse = {
        success: true,
        data: mockHotel
      }

      vi.mocked(hotelService.getHotelById).mockResolvedValue(mockResponse)

      await hotelStore.fetchHotelById(1)

      expect(hotelStore.currentHotel).toEqual(mockHotel)
      expect(hotelStore.error).toBeNull()
    })

    it('should handle fetch hotel by id error', async () => {
      const errorMessage = '酒店不存在'
      vi.mocked(hotelService.getHotelById).mockRejectedValue(new Error(errorMessage))

      await hotelStore.fetchHotelById(999)

      expect(hotelStore.currentHotel).toBeNull()
      expect(hotelStore.error).toBe(errorMessage)
    })
  })

  describe('createHotel', () => {
    it('should successfully create hotel', async () => {
      const hotelData: CreateHotelRequest = {
        name: 'New Hotel',
        address: 'New Address',
        phone: '13800138001',
        description: 'New Description',
        facilities: ['WiFi', 'Pool'],
        images: ['hotel1.jpg']
      }

      const createdHotel: Hotel = {
        id: 2,
        ...hotelData,
        status: 'ACTIVE',
        createdBy: 1,
        createdAt: '2025-12-06T11:00:00Z',
        updatedAt: '2025-12-06T11:00:00Z'
      }

      const mockResponse = {
        success: true,
        message: '酒店创建成功',
        data: createdHotel
      }

      vi.mocked(hotelService.createHotel).mockResolvedValue(mockResponse)

      const result = await hotelStore.createHotel(hotelData)

      expect(result).toEqual(mockResponse)
      expect(hotelStore.error).toBeNull()
    })

    it('should handle create hotel error', async () => {
      const hotelData: CreateHotelRequest = {
        name: 'New Hotel',
        address: 'New Address'
      }

      const errorMessage = '酒店名称已存在'
      vi.mocked(hotelService.createHotel).mockRejectedValue(new Error(errorMessage))

      await hotelStore.createHotel(hotelData)

      expect(hotelStore.error).toBe(errorMessage)
    })
  })

  describe('updateHotel', () => {
    it('should successfully update hotel', async () => {
      const hotelId = 1
      const updateData: UpdateHotelRequest = {
        name: 'Updated Hotel Name',
        description: 'Updated Description'
      }

      const updatedHotel: Hotel = {
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

      const mockResponse = {
        success: true,
        message: '酒店更新成功',
        data: updatedHotel
      }

      vi.mocked(hotelService.updateHotel).mockResolvedValue(mockResponse)

      const result = await hotelStore.updateHotel(hotelId, updateData)

      expect(result).toEqual(mockResponse)
      expect(hotelStore.error).toBeNull()
    })

    it('should handle update hotel error', async () => {
      const hotelId = 1
      const updateData: UpdateHotelRequest = {
        name: 'Updated Name'
      }

      const errorMessage = '酒店不存在'
      vi.mocked(hotelService.updateHotel).mockRejectedValue(new Error(errorMessage))

      await hotelStore.updateHotel(hotelId, updateData)

      expect(hotelStore.error).toBe(errorMessage)
    })
  })

  describe('deleteHotel', () => {
    it('should successfully delete hotel', async () => {
      const hotelId = 1

      const mockResponse = {
        success: true,
        message: '酒店删除成功'
      }

      vi.mocked(hotelService.deleteHotel).mockResolvedValue(mockResponse)

      const result = await hotelStore.deleteHotel(hotelId)

      expect(result).toEqual(mockResponse)
      expect(hotelStore.error).toBeNull()
    })

    it('should handle delete hotel error', async () => {
      const hotelId = 1

      const errorMessage = '酒店不存在'
      vi.mocked(hotelService.deleteHotel).mockRejectedValue(new Error(errorMessage))

      await hotelStore.deleteHotel(hotelId)

      expect(hotelStore.error).toBe(errorMessage)
    })
  })

  describe('updateHotelStatus', () => {
    it('should successfully update hotel status', async () => {
      const hotelId = 1
      const status = 'INACTIVE'

      const mockResponse = {
        success: true,
        message: '酒店状态更新成功',
        data: {
          id: hotelId,
          name: 'Test Hotel',
          address: 'Test Address',
          status: status
        }
      }

      vi.mocked(hotelService.updateHotelStatus).mockResolvedValue(mockResponse)

      const result = await hotelStore.updateHotelStatus(hotelId, status)

      expect(result).toEqual(mockResponse)
      expect(hotelStore.error).toBeNull()
    })

    it('should handle update hotel status error', async () => {
      const hotelId = 1
      const status = 'INACTIVE'

      const errorMessage = '酒店不存在'
      vi.mocked(hotelService.updateHotelStatus).mockRejectedValue(new Error(errorMessage))

      await hotelStore.updateHotelStatus(hotelId, status)

      expect(hotelStore.error).toBe(errorMessage)
    })
  })

  describe('setSearchQuery', () => {
    it('should set search query and reset pagination', () => {
      hotelStore.currentPage = 2
      hotelStore.setSearchQuery('Test Hotel')

      expect(hotelStore.searchQuery).toBe('Test Hotel')
      expect(hotelStore.currentPage).toBe(0)
    })
  })

  describe('setStatusFilter', () => {
    it('should set status filter and reset pagination', () => {
      hotelStore.currentPage = 2
      hotelStore.setStatusFilter('ACTIVE')

      expect(hotelStore.statusFilter).toBe('ACTIVE')
      expect(hotelStore.currentPage).toBe(0)
    })
  })

  describe('setSort', () => {
    it('should set sort options', () => {
      hotelStore.setSort('name', 'ASC')

      expect(hotelStore.sortBy).toBe('name')
      expect(hotelStore.sortDir).toBe('ASC')
    })
  })

  describe('resetFilters', () => {
    it('should reset all filters and pagination', () => {
      hotelStore.searchQuery = 'Test'
      hotelStore.statusFilter = 'ACTIVE'
      hotelStore.currentPage = 2
      hotelStore.pageSize = 10
      hotelStore.sortBy = 'name'
      hotelStore.sortDir = 'ASC'

      hotelStore.resetFilters()

      expect(hotelStore.searchQuery).toBe('')
      expect(hotelStore.statusFilter).toBe('')
      expect(hotelStore.currentPage).toBe(0)
      expect(hotelStore.pageSize).toBe(20)
      expect(hotelStore.sortBy).toBe('createdAt')
      expect(hotelStore.sortDir).toBe('DESC')
    })
  })

  describe('clearError', () => {
    it('should clear error state', () => {
      hotelStore.error = 'Test error'
      hotelStore.clearError()

      expect(hotelStore.error).toBeNull()
    })
  })

  describe('computed properties', () => {
    beforeEach(() => {
      hotelStore.hotels = [
        {
          id: 1,
          name: 'Active Hotel 1',
          address: 'Address 1',
          status: 'ACTIVE',
          facilities: ['WiFi', 'Parking'],
          images: ['hotel1.jpg'],
          phone: '13800138001',
          description: 'Description 1',
          createdBy: 1,
          createdAt: '2025-12-06T10:00:00Z',
          updatedAt: '2025-12-06T10:00:00Z'
        },
        {
          id: 2,
          name: 'Inactive Hotel 2',
          address: 'Address 2',
          status: 'INACTIVE',
          facilities: ['WiFi'],
          images: ['hotel2.jpg'],
          phone: '13800138002',
          description: 'Description 2',
          createdBy: 1,
          createdAt: '2025-12-06T11:00:00Z',
          updatedAt: '2025-12-06T11:00:00Z'
        }
      ]
    })

    it('activeHotels should return only active hotels', () => {
      expect(hotelStore.activeHotels).toHaveLength(1)
      expect(hotelStore.activeHotels[0].name).toBe('Active Hotel 1')
      expect(hotelStore.activeHotels[0].status).toBe('ACTIVE')
    })

    it('filteredHotels should apply search filter', () => {
      hotelStore.searchQuery = 'Active'

      const filtered = hotelStore.filteredHotels
      expect(filtered).toHaveLength(1)
      expect(filtered[0].name).toBe('Active Hotel 1')
    })

    it('filteredHotels should apply status filter', () => {
      hotelStore.statusFilter = 'INACTIVE'

      const filtered = hotelStore.filteredHotels
      expect(filtered).toHaveLength(1)
      expect(filtered[0].name).toBe('Inactive Hotel 2')
    })

    it('filteredHotels should apply both search and status filters', () => {
      hotelStore.searchQuery = 'Hotel'
      hotelStore.statusFilter = 'ACTIVE'

      const filtered = hotelStore.filteredHotels
      expect(filtered).toHaveLength(1)
      expect(filtered[0].name).toBe('Active Hotel 1')
      expect(filtered[0].status).toBe('ACTIVE')
    })

    it('filteredHotels should apply sorting', () => {
      hotelStore.sortBy = 'name'
      hotelStore.sortDir = 'ASC'

      const filtered = hotelStore.filteredHotels
      expect(filtered[0].name).toBe('Active Hotel 1')
      expect(filtered[1].name).toBe('Inactive Hotel 2')
    })

    it('hasNextPage should return correct value', () => {
      hotelStore.currentPage = 0
      hotelStore.totalPages = 3
      expect(hotelStore.hasNextPage).toBe(true)

      hotelStore.currentPage = 2
      hotelStore.totalPages = 3
      expect(hotelStore.hasNextPage).toBe(false)
    })

    it('hasPrevPage should return correct value', () => {
      hotelStore.currentPage = 0
      expect(hotelStore.hasPrevPage).toBe(false)

      hotelStore.currentPage = 1
      expect(hotelStore.hasPrevPage).toBe(true)
    })
  })
})