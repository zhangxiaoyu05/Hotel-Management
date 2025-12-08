import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import WaitingListManager from '@/components/business/WaitingListManager.vue'
import { bookingConflictService } from '@/services/bookingConflictService'

// Mock dependencies
vi.mock('@/services/bookingConflictService')
vi.mock('@/utils/dateUtils', () => ({
  formatDate: vi.fn((date) => '2024-01-01'),
  formatDateTime: vi.fn((date) => '2024-01-01 12:00')
}))

const mockBookingConflictService = vi.mocked(bookingConflictService)

describe('WaitingListManager.vue', () => {
  let wrapper: any

  beforeEach(() => {
    vi.clearAllMocks()

    // Setup booking conflict service mock
    mockBookingConflictService.getUserWaitingList = vi.fn()
    mockBookingConflictService.confirmWaitingListBooking = vi.fn()
    mockBookingConflictService.removeWaitingListItem = vi.fn()
    mockBookingConflictService.getWaitingListPosition = vi.fn()
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  const createWrapper = (props = {}) => {
    return mount(WaitingListManager, {
      props: {
        userId: 123,
        show: true,
        ...props
      },
      global: {
        stubs: ['font-awesome-icon', 'router-link']
      }
    })
  }

  const mockWaitingListData = [
    {
      id: 1,
      roomId: 100,
      roomNumber: '101',
      roomType: 'Standard',
      hotelName: 'Test Hotel',
      requestedCheckInDate: '2024-01-10T10:00:00',
      requestedCheckOutDate: '2024-01-12T12:00:00',
      guestCount: 2,
      priority: 50,
      status: 'WAITING',
      position: 3,
      createdAt: '2024-01-01T10:00:00',
      expiresAt: null,
      notifiedAt: null
    },
    {
      id: 2,
      roomId: 101,
      roomNumber: '102',
      roomType: 'Deluxe',
      hotelName: 'Test Hotel',
      requestedCheckInDate: '2024-01-15T14:00:00',
      requestedCheckOutDate: '2024-01-17T12:00:00',
      guestCount: 1,
      priority: 80,
      status: 'NOTIFIED',
      position: null,
      createdAt: '2024-01-02T10:00:00',
      expiresAt: '2024-01-16T14:00:00',
      notifiedAt: '2024-01-14T10:00:00'
    }
  ]

  describe('Component Rendering', () => {
    it('should render waiting list when show is true', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: mockWaitingListData,
          totalElements: 2,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.find('.waiting-list-manager').exists()).toBe(true)
      expect(wrapper.findAll('.waiting-list-item')).toHaveLength(2)
    })

    it('should not render when show is false', () => {
      wrapper = createWrapper({ show: false })

      expect(wrapper.find('.waiting-list-manager').exists()).toBe(false)
    })

    it('should display empty state when no waiting list items', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.find('.empty-state').exists()).toBe(true)
      expect(wrapper.text()).toContain('暂无等待列表项')
    })
  })

  describe('Data Loading', () => {
    it('should load waiting list data on mount', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: mockWaitingListData,
          totalElements: 2,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(mockBookingConflictService.getUserWaitingList).toHaveBeenCalledWith({
        userId: 123,
        status: null,
        page: 1,
        size: 10
      })

      expect(wrapper.vm.waitingList).toEqual(mockWaitingListData)
      expect(wrapper.vm.pagination.totalElements).toBe(2)
    })

    it('should handle loading state correctly', async () => {
      mockBookingConflictService.getUserWaitingList.mockImplementation(() =>
        new Promise(resolve => setTimeout(resolve, 100))
      )

      wrapper = createWrapper()
      await nextTick()

      expect(wrapper.vm.loading).toBe(true)
      expect(wrapper.find('.loading-spinner').exists()).toBe(true)
    })

    it('should handle load error gracefully', async () => {
      mockBookingConflictService.getUserWaitingList.mockRejectedValue(
        new Error('API Error')
      )

      wrapper = createWrapper()
      await nextTick()

      // Wait for error handling
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.vm.error).toBeTruthy()
      expect(wrapper.find('.error-message').exists()).toBe(true)
    })
  })

  describe('Status Display', () => {
    it('should display correct status for WAITING items', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[0]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      const statusElement = wrapper.find('.status-badge')
      expect(statusElement.text()).toContain('等待中')
      expect(statusElement.classes()).toContain('status-waiting')
    })

    it('should display correct status for NOTIFIED items', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[1]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      const statusElement = wrapper.find('.status-badge')
      expect(statusElement.text()).toContain('已通知')
      expect(statusElement.classes()).toContain('status-notified')
    })

    it('should display position for WAITING items', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[0]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.text()).toContain('排队位置：第 3 位')
    })

    it('should display expiration time for NOTIFIED items', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[1]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.text()).toContain('有效期至')
    })
  })

  describe('Actions', () => {
    it('should show confirm button for NOTIFIED items', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[1]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.find('.confirm-btn').exists()).toBe(true)
    })

    it('should show remove button for WAITING items', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[0]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.find('.remove-btn').exists()).toBe(true)
    })

    it('should confirm waiting list booking successfully', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[1]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)
      mockBookingConflictService.confirmWaitingListBooking.mockResolvedValue({
        success: true,
        data: true
      })

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      // Mock order ID input
      wrapper.vm.selectedOrderId = 456

      await wrapper.find('.confirm-btn').trigger('click')
      await nextTick()

      // Wait for async operation
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(mockBookingConflictService.confirmWaitingListBooking).toHaveBeenCalledWith({
        waitingListId: 2,
        orderId: 456
      })

      expect(wrapper.emitted('confirmed')).toBeTruthy()
    })

    it('should remove waiting list item successfully', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[0]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)
      mockBookingConflictService.removeWaitingListItem.mockResolvedValue({
        success: true,
        data: true
      })

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      await wrapper.find('.remove-btn').trigger('click')
      await nextTick()

      // Confirm removal in dialog
      await wrapper.find('.confirm-remove-btn').trigger('click')
      await nextTick()

      // Wait for async operation
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(mockBookingConflictService.removeWaitingListItem).toHaveBeenCalledWith(1)
      expect(wrapper.emitted('removed')).toBeTruthy()
    })
  })

  describe('Filtering', () => {
    it('should filter by status', async () => {
      mockBookingConflictService.getUserWaitingList.mockResolvedValue({
        success: true,
        data: {
          content: [mockWaitingListData[0]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      })

      wrapper = createWrapper()
      await nextTick()

      // Set status filter
      wrapper.vm.selectedStatus = 'WAITING'
      wrapper.vm.filterWaitingList()

      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(mockBookingConflictService.getUserWaitingList).toHaveBeenCalledWith({
        userId: 123,
        status: 'WAITING',
        page: 1,
        size: 10
      })
    })
  })

  describe('Pagination', () => {
    it('should handle page changes', async () => {
      mockBookingConflictService.getUserWaitingList.mockResolvedValue({
        success: true,
        data: {
          content: [],
          totalElements: 25,
          totalPages: 3,
          currentPage: 2,
          pageSize: 10
        }
      })

      wrapper = createWrapper()
      await nextTick()

      wrapper.vm.changePage(2)

      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(mockBookingConflictService.getUserWaitingList).toHaveBeenCalledWith({
        userId: 123,
        status: wrapper.vm.selectedStatus,
        page: 2,
        size: 10
      })
    })
  })

  describe('Refreshing', () => {
    it('should refresh data when refresh is called', async () => {
      mockBookingConflictService.getUserWaitingList.mockResolvedValue({
        success: true,
        data: {
          content: mockWaitingListData,
          totalElements: 2,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      })

      wrapper = createWrapper()
      await nextTick()

      // Reset mock to track new calls
      mockBookingConflictService.getUserWaitingList.mockClear()

      wrapper.vm.refresh()

      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(mockBookingConflictService.getUserWaitingList).toHaveBeenCalledTimes(1)
    })
  })

  describe('Auto-refresh', () => {
    it('should set up auto-refresh timer', () => {
      const spy = vi.spyOn(global, 'setInterval')

      wrapper = createWrapper()

      expect(spy).toHaveBeenCalledWith(
        expect.any(Function),
        30000 // 30 seconds
      )

      spy.mockRestore()
    })

    it('should clear timer on unmount', () => {
      const spy = vi.spyOn(global, 'clearInterval')

      wrapper = createWrapper()
      wrapper.unmount()

      expect(spy).toHaveBeenCalled()

      spy.mockRestore()
    })
  })

  describe('Error Handling', () => {
    it('should handle confirm booking error', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[1]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)
      mockBookingConflictService.confirmWaitingListBooking.mockRejectedValue(
        new Error('Confirm failed')
      )

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      wrapper.vm.selectedOrderId = 456
      await wrapper.find('.confirm-btn').trigger('click')
      await nextTick()

      // Wait for error handling
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.vm.error).toBeTruthy()
      expect(wrapper.find('.error-message').exists()).toBe(true)
    })

    it('should handle remove item error', async () => {
      const mockResponse = {
        success: true,
        data: {
          content: [mockWaitingListData[0]],
          totalElements: 1,
          totalPages: 1,
          currentPage: 1,
          pageSize: 10
        }
      }

      mockBookingConflictService.getUserWaitingList.mockResolvedValue(mockResponse)
      mockBookingConflictService.removeWaitingListItem.mockRejectedValue(
        new Error('Remove failed')
      )

      wrapper = createWrapper()
      await nextTick()

      // Wait for data loading
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      await wrapper.find('.remove-btn').trigger('click')
      await nextTick()

      await wrapper.find('.confirm-remove-btn').trigger('click')
      await nextTick()

      // Wait for error handling
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.vm.error).toBeTruthy()
    })
  })
})