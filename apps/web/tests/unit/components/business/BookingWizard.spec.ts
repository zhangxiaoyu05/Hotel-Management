import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import BookingWizard from '@/pages/booking/BookingWizard.vue'
import { useOrderStore } from '@/stores/order'
import type { Room } from '@/types/room'
import type { CreateOrderRequest } from '@/types/order'

// Mock router
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  })
}))

// Mock order service
vi.mock('@/services/orderService', () => ({
  default: {
    createOrder: vi.fn(),
    calculatePrice: vi.fn()
  }
}))

describe('BookingWizard.vue', () => {
  let wrapper: any
  let orderStore: any

  const mockRoom: Room = {
    id: 1,
    name: '豪华大床房',
    type: 'DELUXE',
    price: 298,
    maxGuests: 2,
    amenities: ['WiFi', '空调', '迷你吧'],
    images: ['room1.jpg'],
    status: 'AVAILABLE',
    hotelId: 1,
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01'
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    orderStore = useOrderStore()

    // Reset store state
    orderStore.$reset()
    orderStore.selectedRoom = mockRoom
    orderStore.dates = {
      checkIn: '2024-01-01',
      checkOut: '2024-01-02'
    }

    wrapper = mount(BookingWizard, {
      global: {
        plugins: [createPinia()],
        stubs: ['router-link', 'router-view']
      }
    })
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it('renders correctly with initial step', () => {
    expect(wrapper.find('.booking-wizard').exists()).toBe(true)
    expect(wrapper.find('.step-indicator').exists()).toBe(true)
    expect(wrapper.text()).toContain('房间确认')
  })

  it('displays correct step indicator', async () => {
    const steps = ['房间确认', '入住信息', '特殊要求']
    for (let i = 0; i < steps.length; i++) {
      const stepIndicator = wrapper.find(`.step-${i + 1}`)
      if (i === 0) {
        expect(stepIndicator.classes()).toContain('active')
      } else {
        expect(stepIndicator.classes()).toContain('pending')
      }
    }
  })

  it('validates guest information form', async () => {
    // Navigate to step 2
    await wrapper.find('[data-test="next-step"]').trigger('click')

    // Try to proceed without filling form
    await wrapper.find('[data-test="next-step"]').trigger('click')

    // Should show validation errors
    expect(wrapper.text()).toContain('请输入客人姓名')
    expect(wrapper.text()).toContain('请输入联系电话')
  })

  it('updates guest information correctly', async () => {
    // Navigate to step 2
    await wrapper.find('[data-test="next-step"]').trigger('click')

    // Fill guest form
    await wrapper.find('[data-test="guest-name"]').setValue('张三')
    await wrapper.find('[data-test="guest-phone"]').setValue('13800138000')
    await wrapper.find('[data-test="guest-email"]').setValue('zhang@example.com')

    // Verify store is updated
    expect(orderStore.guestInfo).toEqual({
      name: '张三',
      phone: '13800138000',
      email: 'zhang@example.com'
    })
  })

  it('handles special requests correctly', async () => {
    // Navigate to step 3
    await wrapper.find('[data-test="next-step"]').trigger('click')
    await wrapper.find('[data-test="guest-name"]').setValue('张三')
    await wrapper.find('[data-test="guest-phone"]').setValue('13800138000')
    await wrapper.find('[data-test="next-step"]').trigger('click')

    // Add special request
    await wrapper.find('[data-test="special-requests"]').setValue('需要无烟房')

    expect(orderStore.specialRequests).toBe('需要无烟房')
  })

  it('submits order successfully', async () => {
    const mockOrderResponse = {
      success: true,
      data: {
        order: {
          id: 123,
          orderNumber: 'ORD-20240101-00123',
          status: 'CONFIRMED'
        }
      }
    }

    const { default: orderService } = await import('@/services/orderService')
    vi.mocked(orderService.createOrder).mockResolvedValue(mockOrderResponse)

    // Navigate through all steps
    await wrapper.find('[data-test="next-step"]').trigger('click')
    await wrapper.find('[data-test="guest-name"]').setValue('张三')
    await wrapper.find('[data-test="guest-phone"]').setValue('13800138000')
    await wrapper.find('[data-test="next-step"]').trigger('click')
    await wrapper.find('[data-test="next-step"]').trigger('click')

    // Submit order
    await wrapper.find('[data-test="submit-order"]').trigger('click')

    // Verify service was called
    expect(orderService.createOrder).toHaveBeenCalledWith(
      expect.objectContaining({
        roomId: 1,
        guestName: '张三',
        guestPhone: '13800138000'
      })
    )
  })

  it('handles order submission error', async () => {
    const { default: orderService } = await import('@/services/orderService')
    vi.mocked(orderService.createOrder).mockRejectedValue(
      new Error('预订失败')
    )

    // Navigate through all steps
    await wrapper.find('[data-test="next-step"]').trigger('click')
    await wrapper.find('[data-test="guest-name"]').setValue('张三')
    await wrapper.find('[data-test="guest-phone"]').setValue('13800138000')
    await wrapper.find('[data-test="next-step"]').trigger('click')
    await wrapper.find('[data-test="next-step"]').trigger('click')

    // Submit order
    await wrapper.find('[data-test="submit-order"]').trigger('click')

    // Should show error message
    expect(wrapper.text()).toContain('预订失败')
  })

  it('navigates back correctly', async () => {
    // Go to step 2
    await wrapper.find('[data-test="next-step"]').trigger('click')

    // Go back to step 1
    await wrapper.find('[data-test="prev-step"]').trigger('click')

    expect(wrapper.text()).toContain('房间确认')
    expect(wrapper.find('.step-1').classes()).toContain('active')
  })

  it('resets form data correctly', async () => {
    // Fill some data
    await wrapper.find('[data-test="next-step"]').trigger('click')
    await wrapper.find('[data-test="guest-name"]').setValue('张三')

    // Reset
    await wrapper.find('[data-test="reset-form"]').trigger('click')

    // Should be back at step 1 with empty form
    expect(wrapper.text()).toContain('房间确认')
    expect(orderStore.guestInfo.name).toBe('')
  })
})