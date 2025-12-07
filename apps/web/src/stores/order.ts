import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  BookingWizardState,
  CreateOrderRequest,
  OrderResponse,
  OrderListResponse,
  UpdateOrderRequest
} from '@/types/order'
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
  const orderList = ref<OrderListResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 订单管理相关状态
  const filters = ref({
    status: '',
    search: '',
    sortBy: 'createdAt',
    sortOrder: 'desc' as 'asc' | 'desc'
  })
  const pagination = ref({
    currentPage: 1,
    pageSize: 10,
    totalItems: 0,
    totalPages: 0
  })

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

  // 订单管理相关计算属性
  const filteredOrders = computed(() => {
    let filtered = [...orderList.value]

    // 按状态筛选
    if (filters.value.status) {
      filtered = filtered.filter(order => order.status === filters.value.status)
    }

    // 按搜索词筛选
    if (filters.value.search) {
      const searchTerm = filters.value.search.toLowerCase()
      filtered = filtered.filter(order =>
        order.orderNumber.toLowerCase().includes(searchTerm) ||
        order.hotelName.toLowerCase().includes(searchTerm) ||
        order.roomName.toLowerCase().includes(searchTerm)
      )
    }

    // 排序
    filtered.sort((a, b) => {
      let aValue: any = a[filters.value.sortBy as keyof OrderListResponse]
      let bValue: any = b[filters.value.sortBy as keyof OrderListResponse]

      if (typeof aValue === 'string') {
        aValue = new Date(aValue).getTime()
      }
      if (typeof bValue === 'string') {
        bValue = new Date(bValue).getTime()
      }

      if (filters.value.sortOrder === 'desc') {
        return bValue - aValue
      }
      return aValue - bValue
    })

    return filtered
  })

  const paginatedOrders = computed(() => {
    const start = (pagination.value.currentPage - 1) * pagination.value.pageSize
    const end = start + pagination.value.pageSize
    return filteredOrders.value.slice(start, end)
  })

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

  // 订单列表管理
  const loadOrderList = async (
    status?: string,
    page?: number,
    size?: number,
    sortBy?: string,
    sortOrder?: string,
    search?: string
  ) => {
    loading.value = true
    error.value = null

    try {
      const orders = await orderService.getOrderList(
        status,
        page,
        size,
        sortBy,
        sortOrder,
        search
      )
      orderList.value = orders

      // 更新筛选器和分页状态
      if (status !== undefined) filters.value.status = status
      if (search !== undefined) filters.value.search = search
      if (sortBy !== undefined) filters.value.sortBy = sortBy
      if (sortOrder !== undefined) filters.value.sortOrder = sortOrder

      // 更新分页信息
      if (page !== undefined) pagination.value.currentPage = page
      if (size !== undefined) pagination.value.pageSize = size

      pagination.value.totalItems = filteredOrders.value.length
      pagination.value.totalPages = Math.ceil(pagination.value.totalItems / pagination.value.pageSize)

      return orders
    } catch (err: any) {
      error.value = err.response?.data?.message || '加载订单列表失败'
      return []
    } finally {
      loading.value = false
    }
  }

  const loadUserOrders = async (status?: string) => {
    return loadOrderList(status)
  }

  const refreshOrderList = () => {
    return loadOrderList(
      filters.value.status,
      pagination.value.currentPage,
      pagination.value.pageSize,
      filters.value.sortBy,
      filters.value.sortOrder,
      filters.value.search
    )
  }

  // 订单详情
  const loadOrderDetail = async (orderId: number) => {
    loading.value = true
    error.value = null

    try {
      const order = await orderService.getOrder(orderId)
      currentOrder.value = order
      return order
    } catch (err: any) {
      error.value = err.response?.data?.message || '加载订单详情失败'
      return null
    } finally {
      loading.value = false
    }
  }

  // 订单修改
  const updateOrder = async (orderId: number, request: UpdateOrderRequest) => {
    loading.value = true
    error.value = null

    try {
      const updatedOrder = await orderService.updateOrder(orderId, request)

      // 更新列表中的订单
      const index = orderList.value.findIndex(order => order.id === orderId)
      if (index !== -1 && 'hotel' in updatedOrder) {
        // 如果返回的是OrderResponse，需要转换
        const orderListItem: OrderListResponse = {
          id: updatedOrder.id,
          orderNumber: updatedOrder.orderNumber,
          roomId: updatedOrder.roomId,
          roomName: updatedOrder.room.name,
          roomNumber: updatedOrder.room.roomNumber,
          hotelName: updatedOrder.hotel.name,
          checkInDate: updatedOrder.checkInDate,
          checkOutDate: updatedOrder.checkOutDate,
          guestCount: updatedOrder.guestCount,
          totalPrice: updatedOrder.totalPrice,
          status: updatedOrder.status,
          createdAt: updatedOrder.createdAt
        }
        orderList.value[index] = orderListItem
      }

      // 更新当前订单
      if (currentOrder.value?.id === orderId) {
        currentOrder.value = updatedOrder
      }

      return updatedOrder
    } catch (err: any) {
      error.value = err.response?.data?.message || '修改订单失败'
      return null
    } finally {
      loading.value = false
    }
  }

  // 订单取消
  const cancelOrder = async (orderId: number, cancelReason?: string) => {
    loading.value = true
    error.value = null

    try {
      const result = await orderService.cancelOrder(orderId, cancelReason)

      // 更新列表中的订单状态
      const index = orderList.value.findIndex(order => order.id === orderId)
      if (index !== -1 && 'hotel' in result) {
        // 如果返回的是OrderResponse，需要转换
        const orderListItem: OrderListResponse = {
          id: result.id,
          orderNumber: result.orderNumber,
          roomId: result.roomId,
          roomName: result.room.name,
          roomNumber: result.room.roomNumber,
          hotelName: result.hotel.name,
          checkInDate: result.checkInDate,
          checkOutDate: result.checkOutDate,
          guestCount: result.guestCount,
          totalPrice: result.totalPrice,
          status: result.status,
          createdAt: result.createdAt
        }
        orderList.value[index] = orderListItem
      }

      // 更新当前订单
      if (currentOrder.value?.id === orderId) {
        currentOrder.value = result
      }

      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || '取消订单失败'
      return null
    } finally {
      loading.value = false
    }
  }

  // 筛选器和分页管理
  const setFilters = (newFilters: Partial<typeof filters.value>) => {
    Object.assign(filters.value, newFilters)
    pagination.value.currentPage = 1 // 重置到第一页
  }

  const setPagination = (newPagination: Partial<typeof pagination.value>) => {
    Object.assign(pagination.value, newPagination)
  }

  const resetFilters = () => {
    filters.value = {
      status: '',
      search: '',
      sortBy: 'createdAt',
      sortOrder: 'desc'
    }
    pagination.value.currentPage = 1
  }

  // 通知相关
  const sendOrderStatusNotification = (order: OrderResponse, status: string) => {
    // 这里可以集成通知服务
    console.log(`Order ${order.orderNumber} status changed to ${status}`)

    // 可以发送邮件、短信或站内消息
    // 例如：notificationService.sendOrderStatusChange(order, status)
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
    orderList,
    loading,
    error,
    filters,
    pagination,

    // Computed
    canProceedToStep2,
    canProceedToStep3,
    totalSteps,
    filteredOrders,
    paginatedOrders,

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

    // 订单管理
    loadOrderList,
    loadUserOrders,
    refreshOrderList,
    loadOrderDetail,
    updateOrder,
    cancelOrder,
    setFilters,
    setPagination,
    resetFilters,
    sendOrderStatusNotification,

    // Session storage
    restoreBookingState,
    saveBookingState,
    clearBookingState
  }
})