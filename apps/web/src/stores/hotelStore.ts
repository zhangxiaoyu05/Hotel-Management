import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { hotelService } from '../services/hotelService'
import type { Hotel, CreateHotelRequest, UpdateHotelRequest, HotelListQuery, HotelListResponse } from '../types/hotel'

export const useHotelStore = defineStore('hotel', () => {
  const hotels = ref<Hotel[]>([])
  const currentHotel = ref<Hotel | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = ref({
    totalElements: 0,
    totalPages: 0,
    size: 20,
    number: 0
  })

  const activeHotels = computed(() => hotels.value.filter(hotel => hotel.status === 'ACTIVE'))
  const inactiveHotels = computed(() => hotels.value.filter(hotel => hotel.status === 'INACTIVE'))

  async function fetchHotels(query?: HotelListQuery) {
    loading.value = true
    error.value = null

    try {
      const response: HotelListResponse = await hotelService.getHotels(query)
      hotels.value = response.content
      pagination.value = {
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        size: response.size,
        number: response.number
      }
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '获取酒店列表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchHotelById(id: number) {
    loading.value = true
    error.value = null

    try {
      currentHotel.value = await hotelService.getHotelById(id)
      return currentHotel.value
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '获取酒店详情失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createHotel(hotelData: CreateHotelRequest) {
    loading.value = true
    error.value = null

    try {
      const newHotel = await hotelService.createHotel(hotelData)
      hotels.value.unshift(newHotel)
      return newHotel
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '创建酒店失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateHotel(id: number, hotelData: UpdateHotelRequest) {
    loading.value = true
    error.value = null

    try {
      const updatedHotel = await hotelService.updateHotel(id, hotelData)
      const index = hotels.value.findIndex(hotel => hotel.id === id)
      if (index !== -1) {
        hotels.value[index] = updatedHotel
      }
      if (currentHotel.value?.id === id) {
        currentHotel.value = updatedHotel
      }
      return updatedHotel
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '更新酒店失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteHotel(id: number) {
    loading.value = true
    error.value = null

    try {
      await hotelService.deleteHotel(id)
      hotels.value = hotels.value.filter(hotel => hotel.id !== id)
      if (currentHotel.value?.id === id) {
        currentHotel.value = null
      }
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '删除酒店失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateHotelStatus(id: number, status: 'ACTIVE' | 'INACTIVE') {
    loading.value = true
    error.value = null

    try {
      const updatedHotel = await hotelService.updateHotelStatus(id, status)
      const index = hotels.value.findIndex(hotel => hotel.id === id)
      if (index !== -1) {
        hotels.value[index] = updatedHotel
      }
      if (currentHotel.value?.id === id) {
        currentHotel.value = updatedHotel
      }
      return updatedHotel
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '更新酒店状态失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function setCurrentHotel(hotel: Hotel | null) {
    currentHotel.value = hotel
  }

  return {
    hotels,
    currentHotel,
    loading,
    error,
    pagination,
    activeHotels,
    inactiveHotels,
    fetchHotels,
    fetchHotelById,
    createHotel,
    updateHotel,
    deleteHotel,
    updateHotelStatus,
    clearError,
    setCurrentHotel
  }
})