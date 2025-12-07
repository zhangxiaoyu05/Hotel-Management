export interface CreateOrderRequest {
  roomId: number
  checkInDate: string // YYYY-MM-DD
  checkOutDate: string // YYYY-MM-DD
  guestCount: number
  guestName: string
  guestPhone: string
  guestEmail?: string
  specialRequests?: string
  couponCode?: string
}

export interface UpdateOrderRequest {
  guestName?: string
  guestPhone?: string
  guestEmail?: string
  specialRequests?: string
}

export interface Order {
  id: number
  orderNumber: string
  userId: number
  roomId: number
  checkInDate: string
  checkOutDate: string
  guestCount: number
  totalPrice: number
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED'
  specialRequests?: string
  cancelReason?: string
  refundAmount?: number
  modifiedAt?: string
  createdAt: string
  updatedAt: string
}

export interface PriceBreakdown {
  roomFee: number
  serviceFee: number
  discountAmount: number
  totalPrice: number
  nights: number
}

export interface RefundInfo {
  refundAmount: number
  cancelReason: string
}

export interface OrderResponse {
  id: number
  orderNumber: string
  userId: number
  roomId: number
  checkInDate: string
  checkOutDate: string
  guestCount: number
  totalPrice: number
  status: string
  specialRequests?: string
  createdAt: string
  updatedAt: string
  room: Room
  hotel: Hotel
  priceBreakdown?: PriceBreakdown
  refundInfo?: RefundInfo
}

export interface OrderListResponse {
  id: number
  orderNumber: string
  roomId: number
  roomName: string
  roomNumber: string
  hotelName: string
  checkInDate: string
  checkOutDate: string
  guestCount: number
  totalPrice: number
  status: string
  createdAt: string
}

export interface BookingWizardState {
  currentStep: number
  selectedRoom: Room | null
  checkInDate: string
  checkOutDate: string
  guestCount: number
  guestInfo: {
    guestName: string
    guestPhone: string
    guestEmail: string
    specialRequests: string
  }
  couponCode: string
  priceBreakdown: PriceBreakdown | null
}