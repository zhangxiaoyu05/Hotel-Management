import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ConflictDetection from '@/components/business/ConflictDetection.vue'
import { bookingConflictService } from '@/services/bookingConflictService'
import { useUserStore } from '@/stores/user'

// Mock dependencies
vi.mock('@/services/bookingConflictService')
vi.mock('@/stores/user')
vi.mock('@/utils/dateUtils', () => ({
  formatDate: vi.fn((date) => '2024-01-01')
}))

const mockBookingConflictService = vi.mocked(bookingConflictService)
const mockUserStore = vi.mocked(useUserStore)

describe('ConflictDetection.vue', () => {
  let wrapper: any

  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks()

    // Setup user store mock
    mockUserStore.mockReturnValue({
      currentUser: { id: 123, name: 'Test User' },
      fetchCurrentUser: vi.fn().mockResolvedValue({ id: 123, name: 'Test User' })
    } as any)

    // Setup booking conflict service mock
    mockBookingConflictService.checkBookingConflict = vi.fn()
    mockBookingConflictService.joinWaitingList = vi.fn()
    mockBookingConflictService.getAlternativeRooms = vi.fn()
    mockBookingConflictService.getWaitingListPosition = vi.fn()
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  const createWrapper = (props = {}) => {
    return mount(ConflictDetection, {
      props: {
        show: true,
        roomId: 100,
        checkInDate: '2024-01-10',
        checkOutDate: '2024-01-12',
        guestCount: 2,
        conflictDetails: {
          conflictType: 'TIME_OVERLAP',
          conflictingOrderId: 456,
          message: '时间冲突'
        },
        alternativeRooms: [
          {
            roomId: 101,
            roomNumber: '102',
            roomType: 'Standard',
            price: 200,
            available: true
          }
        ],
        ...props
      },
      global: {
        stubs: ['font-awesome-icon']
      }
    })
  }

  describe('Component Rendering', () => {
    it('should render conflict details when show is true', () => {
      wrapper = createWrapper()

      expect(wrapper.find('.conflict-detection').exists()).toBe(true)
      expect(wrapper.find('.conflict-details').exists()).toBe(true)
      expect(wrapper.text()).toContain('时间冲突')
      expect(wrapper.text()).toContain('#456')
    })

    it('should not render when show is false', () => {
      wrapper = createWrapper({ show: false })

      expect(wrapper.find('.conflict-detection').exists()).toBe(false)
    })

    it('should display correct conflict title based on conflict type', () => {
      wrapper = createWrapper({
        conflictDetails: {
          conflictType: 'DOUBLE_BOOKING',
          message: '重复预订'
        }
      })

      expect(wrapper.text()).toContain('重复预订')
    })

    it('should display alternative rooms when available', () => {
      wrapper = createWrapper()

      expect(wrapper.find('.alternative-rooms').exists()).toBe(true)
      expect(wrapper.findAll('.room-card')).toHaveLength(1)
      expect(wrapper.text()).toContain('102')
    })

    it('should hide alternative rooms when not available', () => {
      wrapper = createWrapper({ alternativeRooms: [] })

      expect(wrapper.find('.alternative-rooms').exists()).toBe(false)
    })
  })

  describe('Conflict Type Handling', () => {
    it('should display correct title for TIME_OVERLAP', () => {
      wrapper = createWrapper({
        conflictDetails: { conflictType: 'TIME_OVERLAP' }
      })

      expect(wrapper.text()).toContain('时间冲突')
    })

    it('should display correct title for DOUBLE_BOOKING', () => {
      wrapper = createWrapper({
        conflictDetails: { conflictType: 'DOUBLE_BOOKING' }
      })

      expect(wrapper.text()).toContain('重复预订')
    })

    it('should display correct title for CONCURRENT_REQUEST', () => {
      wrapper = createWrapper({
        conflictDetails: { conflictType: 'CONCURRENT_REQUEST' }
      })

      expect(wrapper.text()).toContain('并发请求冲突')
    })

    it('should display default title for unknown conflict type', () => {
      wrapper = createWrapper({
        conflictDetails: { conflictType: 'UNKNOWN' }
      })

      expect(wrapper.text()).toContain('预订冲突')
    })

    it('should apply correct alert classes based on conflict type', () => {
      wrapper = createWrapper({
        conflictDetails: { conflictType: 'TIME_OVERLAP' }
      })

      expect(wrapper.find('.alert--warning').exists()).toBe(true)
    })
  })

  describe('Alternative Room Selection', () => {
    it('should emit room-selected event when alternative room is selected', async () => {
      wrapper = createWrapper()

      await wrapper.find('.select-room-btn').trigger('click')
      await nextTick()

      expect(wrapper.emitted('room-selected')).toBeTruthy()
      expect(wrapper.emitted('room-selected')[0]).toEqual([{
        roomId: 101,
        roomNumber: '102',
        roomType: 'Standard',
        price: 200,
        available: true
      }])
    })

    it('should not emit event for unavailable rooms', async () => {
      wrapper = createWrapper({
        alternativeRooms: [
          {
            roomId: 101,
            roomNumber: '102',
            roomType: 'Standard',
            price: 200,
            available: false
          }
        ]
      })

      const selectButton = wrapper.find('.select-room-btn')
      expect(selectButton.exists()).toBe(false)
    })
  })

  describe('Waiting List Management', () => {
    it('should show waiting list modal when join waiting list is clicked', async () => {
      wrapper = createWrapper()

      await wrapper.find('.join-waiting-list-btn').trigger('click')
      await nextTick()

      expect(wrapper.find('.waiting-list-modal').exists()).toBe(true)
    })

    it('should close waiting list modal when cancel is clicked', async () => {
      wrapper = createWrapper()

      // Open modal first
      await wrapper.find('.join-waiting-list-btn').trigger('click')
      await nextTick()

      // Then close it
      await wrapper.find('.cancel-btn').trigger('click')
      await nextTick()

      expect(wrapper.find('.waiting-list-modal').exists()).toBe(false)
    })

    it('should successfully join waiting list with valid user', async () => {
      const mockResponse = {
        success: true,
        data: {
          id: 1,
          roomId: 100,
          userId: 123,
          status: 'WAITING'
        }
      }

      mockBookingConflictService.joinWaitingList.mockResolvedValue(mockResponse)

      wrapper = createWrapper()

      // Open modal
      await wrapper.find('.join-waiting-list-btn').trigger('click')
      await nextTick()

      // Confirm joining
      await wrapper.find('.confirm-btn').trigger('click')
      await nextTick()

      // Wait for async operation
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(mockBookingConflictService.joinWaitingList).toHaveBeenCalledWith({
        roomId: 100,
        userId: 123,
        checkInDate: expect.any(String),
        checkOutDate: expect.any(String),
        guestCount: 2,
        specialRequests: ''
      })

      expect(wrapper.emitted('waiting-list-joined')).toBeTruthy()
      expect(wrapper.find('.success-message').exists()).toBe(true)
    })

    it('should handle user not logged in error', async () => {
      // Mock user store with no current user
      mockUserStore.mockReturnValue({
        currentUser: null,
        fetchCurrentUser: vi.fn().mockResolvedValue(null)
      } as any)

      wrapper = createWrapper()

      // Open modal
      await wrapper.find('.join-waiting-list-btn').trigger('click')
      await nextTick()

      // Confirm joining should fail
      await wrapper.find('.confirm-btn').trigger('click')
      await nextTick()

      // Wait for async operation
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.find('.error-message').exists()).toBe(true)
      expect(wrapper.text()).toContain('用户未登录')
    })

    it('should handle API error when joining waiting list', async () => {
      mockBookingConflictService.joinWaitingList.mockRejectedValue(
        new Error('API Error')
      )

      wrapper = createWrapper()

      // Open modal
      await wrapper.find('.join-waiting-list-btn').trigger('click')
      await nextTick()

      // Confirm joining should fail
      await wrapper.find('.confirm-btn').trigger('click')
      await nextTick()

      // Wait for async operation
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(wrapper.find('.error-message').exists()).toBe(true)
    })
  })

  describe('Date Change Handling', () => {
    it('should emit dates-changed event when new dates are selected', async () => {
      wrapper = createWrapper()

      const newCheckIn = '2024-01-15'
      const newCheckOut = '2024-01-17'

      // Mock date picker (simplified)
      wrapper.vm.tempDates = {
        checkIn: new Date(newCheckIn),
        checkOut: new Date(newCheckOut)
      }

      await wrapper.find('.apply-dates-btn').trigger('click')
      await nextTick()

      expect(wrapper.emitted('dates-changed')).toBeTruthy()
      expect(wrapper.emitted('dates-changed')[0]).toEqual([{
        checkInDate: newCheckIn,
        checkOutDate: newCheckOut
      }])
    })
  })

  describe('Position Calculation', () => {
    it('should fetch and display waiting list position', async () => {
      mockBookingConflictService.getWaitingListPosition.mockResolvedValue({
        success: true,
        data: 3
      })

      wrapper = createWrapper()

      // Trigger position calculation
      wrapper.vm.calculateWaitingPosition()

      // Wait for async operation
      await new Promise(resolve => setTimeout(resolve, 100))
      await nextTick()

      expect(mockBookingConflictService.getWaitingListPosition).toHaveBeenCalled()
      expect(wrapper.vm.waitingPosition).toBe(3)
    })

    it('should estimate wait time when position is available', async () => {
      wrapper = createWrapper()
      wrapper.vm.waitingPosition = 3

      wrapper.vm.calculateEstimatedWaitTime()

      // Should estimate wait time based on position
      expect(wrapper.vm.estimatedWaitTime).toBeGreaterThan(0)
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA labels', () => {
      wrapper = createWrapper()

      expect(wrapper.find('[role="alert"]').exists()).toBe(true)
      expect(wrapper.find('.conflict-details').attributes('aria-label')).toBeTruthy()
    })

    it('should support keyboard navigation', async () => {
      wrapper = createWrapper()

      const selectButton = wrapper.find('.select-room-btn')

      // Test keyboard interaction
      await selectButton.trigger('keydown', { key: 'Enter' })
      await nextTick()

      expect(wrapper.emitted('room-selected')).toBeTruthy()
    })
  })

  describe('Error Handling', () => {
    it('should handle missing conflict details gracefully', () => {
      wrapper = createWrapper({ conflictDetails: null })

      expect(wrapper.text()).not.toContain('null')
      expect(wrapper.text()).not.toContain('undefined')
    })

    it('should handle empty alternative rooms gracefully', () => {
      wrapper = createWrapper({ alternativeRooms: null })

      expect(wrapper.find('.alternative-rooms').exists()).toBe(false)
    })
  })

  describe('Component Lifecycle', () => {
    it('should clean up timers on unmount', () => {
      wrapper = createWrapper()

      const spy = vi.spyOn(global, 'clearTimeout')

      wrapper.unmount()

      expect(spy).toHaveBeenCalled()
      spy.mockRestore()
    })
  })
})