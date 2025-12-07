import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { BookingWizardState, CreateOrderRequest, OrderResponse } from '@/types/order'
import type { Room } from '@/types/room'
import { orderService } from '@/services/orderService'

export const useOrderStore = defineStore('order', () => {
  // 预订向导状态
  const bookingState = ref<BookingWizardState>({
    currentStep: 1,
    selectedRoom: null,
    checkInDate: '',
    checkOutDate: '',
    guestCount: 1,
    guestInfo: {
      guestName: '',
      guestPhone: '',
      guestEmail: '',
      specialRequests: ''
    },
    couponCode: '',
    priceBreakdown: null
  })

  // 当前订单
  const currentOrder = ref<OrderResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 计算属性
  const canProceedToStep2 = computed(() => {
    return bookingState.value.selectedRoom &&
           bookingState.value.checkInDate &&
           bookingState.value.checkOutDate
  })

  const canProceedToStep3 = computed(() => {
    return canProceedToStep2.value &&
           bookingState.value.guestInfo.guestName.trim() !== '' &&
           bookingState.value.guestInfo.guestPhone.trim() !== ''
  })

  const totalSteps = 3

  // Actions
  const setSelectedRoom = (room: Room) => {
    bookingState.value.selectedRoom = room
  }

  const setDates = (checkInDate: string, checkOutDate: string) => {
    bookingState.value.checkInDate = checkInDate
    bookingState.value.checkOutDate = checkOutDate
  }

  const setGuestCount = (count: number) => {
    bookingState.value.guestCount = count
  }

  const setGuestInfo = (info: Partial<BookingWizardState['guestInfo']>) => {
    Object.assign(bookingState.value.guestInfo, info)
  }

  const setCouponCode = (code: string) => {
    bookingState.value.couponCode = code
  }

  const setPriceBreakdown = (breakdown: BookingWizardState['priceBreakdown']) => {
    bookingState.value.priceBreakdown = breakdown
  }

  const nextStep = () => {
    if (bookingState.value.currentStep < totalSteps) {
      bookingState.value.currentStep++
    }
  }

  const prevStep = () => {
    if (bookingState.value.currentStep > 1) {
      bookingState.value.currentStep--
    }
  }

  const goToStep = (step: number) => {
    if (step >= 1 && step <= totalSteps) {
      bookingState.value.currentStep = step
    }
  }

  const resetBookingState = () => {
    bookingState.value = {
      currentStep: 1,
      selectedRoom: null,
      checkInDate: '',
      checkOutDate: '',
      guestCount: 1,
      guestInfo: {
        guestName: '',
        guestPhone: '',
        guestEmail: '',
        specialRequests: ''
      },
      couponCode: '',
      priceBreakdown: null
    }
    currentOrder.value = null
    error.value = null
  }

  const createOrder = async () => {
    if (!bookingState.value.selectedRoom) {
      error.value = '请选择房间'
      return null
    }

    const request: CreateOrderRequest = {
      roomId: bookingState.value.selectedRoom.id,
      checkInDate: bookingState.value.checkInDate,
      checkOutDate: bookingState.value.checkOutDate,
      guestCount: bookingState.value.guestCount,
      guestName: bookingState.value.guestInfo.guestName,
      guestPhone: bookingState.value.guestInfo.guestPhone,
      guestEmail: bookingState.value.guestInfo.guestEmail,
      specialRequests: bookingState.value.guestInfo.specialRequests,
      couponCode: bookingState.value.couponCode
    }

    loading.value = true
    error.value = null

    try {
      const orderResponse = await orderService.createOrder(request)
      currentOrder.value = orderResponse
      return orderResponse
    } catch (err: any) {
      error.value = err.response?.data?.message || '预订失败，请重试'
      return null
    } finally {
      loading.value = false
    }
  }

  const loadUserOrders = async (status?: string) => {
    loading.value = true
    error.value = null

    try {
      const orders = await orderService.getUserOrders(status)
      return orders
    } catch (err: any) {
      error.value = err.response?.data?.message || '加载订单失败'
      return []
    } finally {
      loading.value = false
    }
  }

  const cancelOrder = async (orderId: number) => {
    loading.value = true
    error.value = null

    try {
      const result = await orderService.cancelOrder(orderId)
      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || '取消订单失败'
      return false
    } finally {
      loading.value = false
    }
  }

  // 从session storage恢复预订状态
  const restoreBookingState = () => {
    const saved = sessionStorage.getItem('bookingState')
    if (saved) {
      try {
        const parsed = JSON.parse(saved)
        bookingState.value = { ...bookingState.value, ...parsed }
      } catch (e) {
        console.error('Failed to restore booking state:', e)
      }
    }
  }

  // 保存预订状态到session storage
  const saveBookingState = () => {
    sessionStorage.setItem('bookingState', JSON.stringify(bookingState.value))
  }

  // 清除session storage中的预订状态
  const clearBookingState = () => {
    sessionStorage.removeItem('bookingState')
  }

  return {
    // State
    bookingState,
    currentOrder,
    loading,
    error,

    // Computed
    canProceedToStep2,
    canProceedToStep3,
    totalSteps,

    // Actions
    setSelectedRoom,
    setDates,
    setGuestCount,
    setGuestInfo,
    setCouponCode,
    setPriceBreakdown,
    nextStep,
    prevStep,
    goToStep,
    resetBookingState,
    createOrder,
    loadUserOrders,
    cancelOrder,
    restoreBookingState,
    saveBookingState,
    clearBookingState
  }
})