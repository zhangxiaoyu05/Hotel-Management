// 价格策略相关类型定义

// 价格规则类型
export interface PricingRule {
  id?: number
  hotelId: number
  roomTypeId?: number
  name: string
  ruleType: 'WEEKEND' | 'HOLIDAY' | 'SEASONAL' | 'CUSTOM'
  adjustmentType: 'PERCENTAGE' | 'FIXED_AMOUNT'
  adjustmentValue: number
  startDate?: string
  endDate?: string
  daysOfWeek?: number[]
  isActive: boolean
  priority?: number
  createdBy?: number
  createdAt?: string
  updatedAt?: string
  roomTypeName?: string // 关联查询字段
  updating?: boolean // UI状态
}

// 创建价格规则请求
export interface CreatePricingRuleRequest {
  hotelId: number
  roomTypeId?: number
  name: string
  ruleType: 'WEEKEND' | 'HOLIDAY' | 'SEASONAL' | 'CUSTOM'
  adjustmentType: 'PERCENTAGE' | 'FIXED_AMOUNT'
  adjustmentValue: number
  startDate?: string
  endDate?: string
  daysOfWeek?: number[]
  isActive: boolean
  priority: number
}

// 更新价格规则请求
export interface UpdatePricingRuleRequest {
  name: string
  ruleType: 'WEEKEND' | 'HOLIDAY' | 'SEASONAL' | 'CUSTOM'
  adjustmentType: 'PERCENTAGE' | 'FIXED_AMOUNT'
  adjustmentValue: number
  startDate?: string
  endDate?: string
  daysOfWeek?: number[]
  isActive: boolean
  priority: number
  roomTypeId?: number
}

// 价格规则查询参数
export interface PricingRuleQuery {
  hotelId: number
  page?: number
  size?: number
  keyword?: string
  ruleType?: string
  roomTypeId?: number
  activeOnly?: boolean
}

// 特殊价格类型
export interface SpecialPrice {
  id?: number
  hotelId: number
  roomTypeId: number
  roomId?: number
  date: string
  price: number
  reason?: string
  createdBy?: number
  createdAt?: string
  updatedAt?: string
  roomTypeName?: string // 关联查询字段
  roomNumber?: string // 关联查询字段
  basePrice?: number // 计算字段
}

// 创建特殊价格请求
export interface CreateSpecialPriceRequest {
  hotelId: number
  roomTypeId: number
  roomId?: number
  date: string
  price: number
  reason?: string
}

// 更新特殊价格请求
export interface UpdateSpecialPriceRequest {
  price: number
  reason?: string
  roomId?: number
}

// 特殊价格查询参数
export interface SpecialPriceQuery {
  hotelId: number
  roomTypeId?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

// 价格历史类型
export interface PriceHistory {
  id?: number
  hotelId: number
  roomTypeId: number
  roomId?: number
  oldPrice?: number
  newPrice: number
  changeType: 'BASE_PRICE' | 'DYNAMIC_RULE' | 'SPECIAL_PRICE' | 'MANUAL'
  changeReason?: string
  changedBy?: number
  createdAt?: string
  roomTypeName?: string // 关联查询字段
  roomNumber?: string // 关联查询字段
  changedByName?: string // 关联查询字段
}

// 价格历史查询参数
export interface PriceHistoryQuery {
  hotelId: number
  roomId?: number
  roomTypeId?: number
  changeType?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

// 节假日类型
export interface Holiday {
  id?: number
  name: string
  date: string
  isNationalHoliday: boolean
  createdAt?: string
  updatedAt?: string
}

// 价格计算请求
export interface PriceCalculationRequest {
  hotelId: number
  roomId?: number
  roomTypeId?: number
  startDate: string
  endDate: string
  includeBreakfast?: boolean
  guestCount?: number
  promoCode?: string
}

// 价格计算响应
export interface PriceCalculationResponse {
  hotelId: number
  roomId?: number
  roomTypeId: number
  roomNumber?: string
  roomTypeName?: string
  basePrice: number
  dailyPrices: Record<string, number>
  totalPrice: number
  averageDailyPrice: number
  minDailyPrice: number
  maxDailyPrice: number
  totalDays: number
  appliedRules?: AppliedPricingRule[]
  specialPrices?: SpecialPriceInfo[]
  containsHolidays?: boolean
  holidayDates?: string[]
}

// 应用的价格规则信息
export interface AppliedPricingRule {
  ruleId: number
  ruleName: string
  ruleType: string
  adjustmentType: string
  adjustmentValue: number
  appliedDates: string[]
}

// 特殊价格信息
export interface SpecialPriceInfo {
  date: string
  specialPrice: number
  reason?: string
}

// 价格变更统计信息
export interface PriceChangeStatistics {
  totalChanges: number
  increaseCount: number
  decreaseCount: number
  totalIncrease: number
  totalDecrease: number
  changeTypeCount: Record<string, number>
  changeTypeAmount: Record<string, number>
  period: string
}

// 价格变更趋势数据
export interface PriceChangeTrend {
  dailyChangeCount: Record<string, number>
  dailyChangeAmount: Record<string, number>
  period: string
}

// API响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  success: boolean
  timestamp: number
}

// 分页响应类型
export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

// 批量操作请求
export interface BatchOperationRequest {
  ids: number[]
  operation: 'activate' | 'deactivate' | 'delete'
}

// 复制特殊价格请求
export interface CopySpecialPriceRequest {
  targetDates: string[]
}

// 价格规则类型枚举
export enum PricingRuleType {
  WEEKEND = 'WEEKEND',
  HOLIDAY = 'HOLIDAY',
  SEASONAL = 'SEASONAL',
  CUSTOM = 'CUSTOM'
}

// 调整类型枚举
export enum AdjustmentType {
  PERCENTAGE = 'PERCENTAGE',
  FIXED_AMOUNT = 'FIXED_AMOUNT'
}

// 价格变更类型枚举
export enum PriceChangeType {
  BASE_PRICE = 'BASE_PRICE',
  DYNAMIC_RULE = 'DYNAMIC_RULE',
  SPECIAL_PRICE = 'SPECIAL_PRICE',
  MANUAL = 'MANUAL'
}

// 价格规则预设配置
export interface PricingRulePreset {
  name: string
  ruleType: PricingRuleType
  adjustmentType: AdjustmentType
  adjustmentValue: number
  daysOfWeek?: number[]
  priority: number
}

// 价格预览数据
export interface PricePreviewData {
  date: string
  basePrice: number
  finalPrice: number
  appliedRules: string[]
  isSpecialPrice: boolean
  isHoliday: boolean
  isWeekend: boolean
}

// 价格策略配置
export interface PricingStrategyConfig {
  enableDynamicPricing: boolean
  defaultWeekendSurcharge: number
  defaultHolidaySurcharge: number
  advanceBookingDays: number
  maxPriceChangePercentage: number
  priceHistoryRetentionDays: number
}