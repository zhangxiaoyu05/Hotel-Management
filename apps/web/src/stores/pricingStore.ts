import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { pricingService } from '../services/pricingService'
import type {
  PricingRule,
  SpecialPrice,
  PriceHistory,
  Holiday,
  CreatePricingRuleRequest,
  UpdatePricingRuleRequest,
  CreateSpecialPriceRequest,
  UpdateSpecialPriceRequest,
  PriceCalculationRequest,
  PriceCalculationResponse
} from '../types/pricing'

export const usePricingStore = defineStore('pricing', () => {
  // 价格规则状态
  const rules = ref<PricingRule[]>([])
  const total = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 特殊价格状态
  const specialPrices = ref<SpecialPrice[]>([])
  const specialPricesTotal = ref(0)

  // 价格历史状态
  const priceHistory = ref<PriceHistory[]>([])
  const priceHistoryTotal = ref(0)

  // 节假日状态
  const holidays = ref<Holiday[]>([])

  // 统计数据
  const priceChangeStatistics = ref<any>({})
  const priceChangeTrend = ref<any>({})

  // 计算属性
  const activeRules = computed(() => rules.value.filter(rule => rule.isActive))
  const inactiveRules = computed(() => rules.value.filter(rule => !rule.isActive))

  // 价格规则相关方法
  async function fetchPricingRules(query: any) {
    loading.value = true
    error.value = null

    try {
      const response = await pricingService.getPricingRules(query)
      rules.value = response.data || []
      total.value = response.total || rules.value.length
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取价格规则失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createPricingRule(data: CreatePricingRuleRequest) {
    loading.value = true
    error.value = null

    try {
      const response = await pricingService.createPricingRule(data)
      rules.value.unshift(response.data)
      total.value += 1
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '创建价格规则失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updatePricingRule(id: number, data: UpdatePricingRuleRequest) {
    loading.value = true
    error.value = null

    try {
      const response = await pricingService.updatePricingRule(id, data)
      const index = rules.value.findIndex(rule => rule.id === id)
      if (index !== -1) {
        rules.value[index] = response.data
      }
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '更新价格规则失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deletePricingRule(id: number) {
    loading.value = true
    error.value = null

    try {
      await pricingService.deletePricingRule(id)
      rules.value = rules.value.filter(rule => rule.id !== id)
      total.value -= 1
    } catch (err: any) {
      error.value = err.response?.data?.message || '删除价格规则失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function toggleRuleStatus(id: number, active: boolean) {
    try {
      await pricingService.toggleRuleStatus(id, active)
      const rule = rules.value.find(r => r.id === id)
      if (rule) {
        rule.isActive = active
      }
    } catch (err: any) {
      error.value = err.response?.data?.message || '切换规则状态失败'
      throw err
    }
  }

  async function batchToggleRules(ids: number[], active: boolean) {
    loading.value = true
    try {
      await pricingService.batchToggleRules(ids, active)
      // 更新本地状态
      ids.forEach(id => {
        const rule = rules.value.find(r => r.id === id)
        if (rule) {
          rule.isActive = active
        }
      })
    } catch (err: any) {
      error.value = err.response?.data?.message || '批量切换规则状态失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function batchDeleteRules(ids: number[]) {
    loading.value = true
    try {
      await pricingService.batchDeleteRules(ids)
      rules.value = rules.value.filter(rule => !ids.includes(rule.id!))
      total.value -= ids.length
    } catch (err: any) {
      error.value = err.response?.data?.message || '批量删除规则失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 特殊价格相关方法
  async function fetchSpecialPrices(query: any) {
    loading.value = true
    error.value = null

    try {
      const response = await pricingService.getSpecialPrices(query)
      specialPrices.value = response.data || []
      specialPricesTotal.value = response.total || specialPrices.value.length
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取特殊价格失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createSpecialPrice(data: CreateSpecialPriceRequest) {
    loading.value = true
    error.value = null

    try {
      const response = await pricingService.createSpecialPrice(data)
      specialPrices.value.unshift(response.data)
      specialPricesTotal.value += 1
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '创建特殊价格失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateSpecialPrice(id: number, data: UpdateSpecialPriceRequest) {
    loading.value = true
    error.value = null

    try {
      const response = await pricingService.updateSpecialPrice(id, data)
      const index = specialPrices.value.findIndex(price => price.id === id)
      if (index !== -1) {
        specialPrices.value[index] = response.data
      }
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '更新特殊价格失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteSpecialPrice(id: number) {
    loading.value = true
    error.value = null

    try {
      await pricingService.deleteSpecialPrice(id)
      specialPrices.value = specialPrices.value.filter(price => price.id !== id)
      specialPricesTotal.value -= 1
    } catch (err: any) {
      error.value = err.response?.data?.message || '删除特殊价格失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function copySpecialPrice(id: number, targetDates: string[]) {
    loading.value = true
    try {
      const response = await pricingService.copySpecialPrice(id, targetDates)
      specialPrices.value.unshift(...response.data)
      specialPricesTotal.value += response.data.length
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '复制特殊价格失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 价格历史相关方法
  async function fetchPriceHistory(query: any) {
    loading.value = true
    error.value = null

    try {
      const response = await pricingService.getPriceHistory(query)
      priceHistory.value = response.data || []
      priceHistoryTotal.value = response.total || priceHistory.value.length
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取价格历史失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPriceChangeStatistics(hotelId: number, days: number) {
    try {
      const response = await pricingService.getPriceChangeStatistics(hotelId, days)
      priceChangeStatistics.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取价格统计失败'
      throw err
    }
  }

  async function fetchPriceChangeTrend(hotelId: number, days: number) {
    try {
      const response = await pricingService.getPriceChangeTrend(hotelId, days)
      priceChangeTrend.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取价格趋势失败'
      throw err
    }
  }

  // 节假日相关方法
  async function fetchHolidays(query: any) {
    try {
      const response = await pricingService.getHolidays(query)
      holidays.value = response.data || []
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取节假日失败'
      throw err
    }
  }

  // 价格计算相关方法
  async function calculateRoomPrice(roomId: number, date: string) {
    try {
      const response = await pricingService.calculateRoomPrice(roomId, date)
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '计算房间价格失败'
      throw err
    }
  }

  async function calculateRoomTypePrice(roomTypeId: number, hotelId: number, date: string) {
    try {
      const response = await pricingService.calculateRoomTypePrice(roomTypeId, hotelId, date)
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '计算房间类型价格失败'
      throw err
    }
  }

  async function calculatePriceRange(request: PriceCalculationRequest) {
    loading.value = true
    try {
      const response = await pricingService.calculatePriceRange(request)
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '批量计算价格失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 工具方法
  function clearError() {
    error.value = null
  }

  function resetPricingStore() {
    rules.value = []
    specialPrices.value = []
    priceHistory.value = []
    holidays.value = []
    priceChangeStatistics.value = {}
    priceChangeTrend.value = {}
    total.value = 0
    specialPricesTotal.value = 0
    priceHistoryTotal.value = 0
    loading.value = false
    error.value = null
  }

  return {
    // 状态
    rules,
    total,
    loading,
    error,
    specialPrices,
    specialPricesTotal,
    priceHistory,
    priceHistoryTotal,
    holidays,
    priceChangeStatistics,
    priceChangeTrend,

    // 计算属性
    activeRules,
    inactiveRules,

    // 价格规则方法
    fetchPricingRules,
    createPricingRule,
    updatePricingRule,
    deletePricingRule,
    toggleRuleStatus,
    batchToggleRules,
    batchDeleteRules,

    // 特殊价格方法
    fetchSpecialPrices,
    createSpecialPrice,
    updateSpecialPrice,
    deleteSpecialPrice,
    copySpecialPrice,

    // 价格历史方法
    fetchPriceHistory,
    fetchPriceChangeStatistics,
    fetchPriceChangeTrend,

    // 节假日方法
    fetchHolidays,

    // 价格计算方法
    calculateRoomPrice,
    calculateRoomTypePrice,
    calculatePriceRange,

    // 工具方法
    clearError,
    resetPricingStore
  }
})