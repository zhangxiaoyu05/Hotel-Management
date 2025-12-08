import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useRoomStore } from '@/stores/roomStore'
import { roomApi } from '@/api/room'

// Mock room API
vi.mock('@/api/room', () => ({
  roomApi: {
    getRooms: vi.fn(),
    getRoom: vi.fn(),
    createRoom: vi.fn(),
    updateRoom: vi.fn(),
    deleteRoom: vi.fn(),
    searchAvailableRooms: vi.fn(),
    getRoomTypes: vi.fn(),
    batchUpdateRooms: vi.fn()
  }
}))

// Mock ElMessage
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

describe('RoomStore', () => {
  let roomStore: any

  beforeEach(() => {
    setActivePinia(createPinia())
    roomStore = useRoomStore()

    // Mock localStorage
    const localStorageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn()
    }
    global.localStorage = localStorageMock
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('搜索功能', () => {
    it('应该成功搜索可用房间', async () => {
      // Given
      const mockSearchRequest = {
        hotelId: 1,
        checkInDate: '2024-12-10',
        checkOutDate: '2024-12-11',
        guestCount: 2
      }

      const mockResponse = {
        data: {
          success: true,
          data: {
            rooms: [
              { id: 1, roomNumber: '101', price: 299 },
              { id: 2, roomNumber: '102', price: 399 }
            ],
            total: 2,
            page: 0,
            size: 20,
            totalPages: 1
          }
        }
      }

      vi.mocked(roomApi.searchAvailableRooms).mockResolvedValue(mockResponse)

      // When
      await roomStore.searchAvailableRooms(mockSearchRequest)

      // Then
      expect(roomStore.loading).toBe(false)
      expect(roomStore.searchResults).toHaveLength(2)
      expect(roomStore.searchResults[0].roomNumber).toBe('101')
      expect(roomStore.searchTotal).toBe(2)
    })

    it('应该处理搜索失败', async () => {
      // Given
      const mockSearchRequest = {
        hotelId: 1,
        checkInDate: '2024-12-10',
        checkOutDate: '2024-12-11',
        guestCount: 2
      }

      vi.mocked(roomApi.searchAvailableRooms).mockRejectedValue(new Error('网络错误'))

      // When
      await roomStore.searchAvailableRooms(mockSearchRequest)

      // Then
      expect(roomStore.loading).toBe(false)
      expect(roomStore.searchResults).toEqual([])
    })

    it('应该清空搜索结果', () => {
      // Given
      roomStore.searchResults = [{ id: 1, roomNumber: '101' }]

      // When
      roomStore.clearSearchResults()

      // Then
      expect(roomStore.searchResults).toEqual([])
    })
  })

  describe('搜索历史功能', () => {
    it('应该添加搜索历史到本地存储', () => {
      // Given
      const searchRequest = {
        hotelId: 1,
        checkInDate: '2024-12-10',
        checkOutDate: '2024-12-11',
        guestCount: 2
      }

      // When
      roomStore.addToSearchHistory(searchRequest)

      // Then
      expect(localStorage.setItem).toHaveBeenCalledWith(
        'room_search_history',
        expect.any(String)
      )
    })

    it('应该从本地存储加载搜索历史', () => {
      // Given
      const mockHistory = [
        {
          hotelId: 1,
          checkInDate: '2024-12-10',
          checkOutDate: '2024-12-11',
          guestCount: 2
        }
      ]
      vi.mocked(localStorage.getItem).mockReturnValue(JSON.stringify(mockHistory))

      // When
      roomStore.loadSearchHistory()

      // Then
      expect(roomStore.searchHistory).toEqual(mockHistory)
    })

    it('应该限制搜索历史数量', () => {
      // Given
      const searchRequest = {
        hotelId: 1,
        checkInDate: '2024-12-10',
        checkOutDate: '2024-12-11',
        guestCount: 2
      }

      // When - 添加超过限制的历史记录
      for (let i = 0; i < 15; i++) {
        roomStore.addToSearchHistory({
          ...searchRequest,
          hotelId: i + 1
        })
      }

      // Then
      expect(roomStore.searchHistory.length).toBeLessThanOrEqual(10)
    })

    it('应该清除搜索历史', () => {
      // Given
      roomStore.searchHistory = [
        {
          hotelId: 1,
          checkInDate: '2024-12-10',
          checkOutDate: '2024-12-11',
          guestCount: 2
        }
      ]

      // When
      roomStore.clearSearchHistory()

      // Then
      expect(roomStore.searchHistory).toEqual([])
      expect(localStorage.removeItem).toHaveBeenCalledWith('room_search_history')
    })
  })

  describe('房间管理功能', () => {
    it('应该获取房间列表', async () => {
      // Given
      const mockResponse = {
        data: {
          success: true,
          data: {
            content: [
              { id: 1, roomNumber: '101' },
              { id: 2, roomNumber: '102' }
            ]
          }
        }
      }
      vi.mocked(roomApi.getRooms).mockResolvedValue(mockResponse)

      // When
      await roomStore.fetchRooms()

      // Then
      expect(roomStore.rooms).toHaveLength(2)
      expect(roomStore.rooms[0].roomNumber).toBe('101')
    })

    it('应该创建房间', async () => {
      // Given
      const roomData = {
        roomNumber: '201',
        hotelId: 1,
        roomTypeId: 1
      }

      const mockResponse = {
        data: {
          success: true,
          data: { id: 3, ...roomData }
        }
      }
      vi.mocked(roomApi.createRoom).mockResolvedValue(mockResponse)

      // When
      await roomStore.createRoom(roomData)

      // Then
      expect(roomStore.rooms).toContainEqual({ id: 3, ...roomData })
    })

    it('应该更新房间', async () => {
      // Given
      roomStore.rooms = [{ id: 1, roomNumber: '101' }]

      const updateData = {
        id: 1,
        roomNumber: '101-A'
      }

      const mockResponse = {
        data: {
          success: true,
          data: updateData
        }
      }
      vi.mocked(roomApi.updateRoom).mockResolvedValue(mockResponse)

      // When
      await roomStore.updateRoom(1, updateData)

      // Then
      const updatedRoom = roomStore.rooms.find(r => r.id === 1)
      expect(updatedRoom?.roomNumber).toBe('101-A')
    })

    it('应该删除房间', async () => {
      // Given
      roomStore.rooms = [{ id: 1, roomNumber: '101' }]

      const mockResponse = {
        data: {
          success: true
        }
      }
      vi.mocked(roomApi.deleteRoom).mockResolvedValue(mockResponse)

      // When
      await roomStore.deleteRoom(1)

      // Then
      expect(roomStore.rooms).not.toContainEqual({ id: 1, roomNumber: '101' })
    })
  })

  describe('房间类型功能', () => {
    it('应该获取房间类型列表', async () => {
      // Given
      const mockResponse = {
        data: {
          success: true,
          data: [
            { id: 1, name: '标准间' },
            { id: 2, name: '豪华间' }
          ]
        }
      }
      vi.mocked(roomApi.getRoomTypes).mockResolvedValue(mockResponse)

      // When
      await roomStore.fetchRoomTypes()

      // Then
      expect(roomStore.roomTypes).toHaveLength(2)
      expect(roomStore.roomTypes[0].name).toBe('标准间')
    })
  })

  describe('批量操作功能', () => {
    it('应该批量更新房间', async () => {
      // Given
      const updateData = {
        roomIds: [1, 2],
        status: 'MAINTENANCE'
      }

      const mockResponse = {
        data: {
          success: true,
          data: {
            updated: 2
          }
        }
      }
      vi.mocked(roomApi.batchUpdateRooms).mockResolvedValue(mockResponse)

      // When
      const result = await roomStore.batchUpdateRooms(updateData)

      // Then
      expect(result.updated).toBe(2)
    })
  })

  describe('状态管理', () => {
    it('应该正确设置加载状态', async () => {
      // Given
      vi.mocked(roomApi.getRooms).mockImplementation(() => new Promise(resolve => {
        setTimeout(() => resolve({
          data: { success: true, data: { content: [] } }
        }), 100)
      }))

      // When
      const fetchPromise = roomStore.fetchRooms()

      // Then - 加载中
      expect(roomStore.loading).toBe(true)

      // Wait for completion
      await fetchPromise

      // Then - 加载完成
      expect(roomStore.loading).toBe(false)
    })

    it('应该重置store状态', () => {
      // Given - 设置一些状态
      roomStore.rooms = [{ id: 1, roomNumber: '101' }]
      roomStore.searchResults = [{ id: 1, roomNumber: '101' }]
      roomStore.loading = true

      // When
      roomStore.$reset()

      // Then
      expect(roomStore.rooms).toEqual([])
      expect(roomStore.searchResults).toEqual([])
      expect(roomStore.loading).toBe(false)
    })
  })
})