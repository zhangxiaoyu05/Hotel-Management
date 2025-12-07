import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { roomTypeApi } from '../api/roomType'
import type { RoomType, RoomTypeListQuery, CreateRoomTypeRequest, UpdateRoomTypeRequest } from '../types/roomType'
import { ElMessage } from 'element-plus'

export const useRoomTypeStore = defineStore('roomType', () => {
  // State
  const roomTypes = ref<RoomType[]>([])
  const currentRoomType = ref<RoomType | null>(null)
  const loading = ref(false)
  const error = ref('')
  const pagination = ref({
    totalElements: 0,
    totalPages: 0,
    size: 20,
    number: 0
  })

  // Getters
  const activeRoomTypes = computed(() =>
    roomTypes.value.filter(rt => rt.status === 'ACTIVE')
  )

  const roomTypesByHotel = computed(() => {
    const byHotel: Record<number, RoomType[]> = {}
    roomTypes.value.forEach(rt => {
      if (!byHotel[rt.hotelId]) {
        byHotel[rt.hotelId] = []
      }
      byHotel[rt.hotelId].push(rt)
    })
    return byHotel
  })

  // Actions
  async function fetchRoomTypes(query?: RoomTypeListQuery) {
    loading.value = true
    error.value = ''

    try {
      const response = await roomTypeApi.getRoomTypes(query)
      roomTypes.value = response.content
      pagination.value = {
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        size: response.size,
        number: response.number
      }
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '获取房间类型列表失败'
      ElMessage.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchRoomTypesByHotel(hotelId: number) {
    loading.value = true
    error.value = ''

    try {
      const hotelRoomTypes = await roomTypeApi.getRoomTypesByHotel(hotelId)
      // 更新store中的数据，但不覆盖其他酒店的房间类型
      hotelRoomTypes.forEach(rt => {
        const existingIndex = roomTypes.value.findIndex(existing => existing.id === rt.id)
        if (existingIndex >= 0) {
          roomTypes.value[existingIndex] = rt
        } else {
          roomTypes.value.push(rt)
        }
      })
      return hotelRoomTypes
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '获取酒店房间类型失败'
      ElMessage.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchRoomType(id: number) {
    loading.value = true
    error.value = ''

    try {
      const roomType = await roomTypeApi.getRoomType(id)
      currentRoomType.value = roomType

      // 更新列表中的数据
      const index = roomTypes.value.findIndex(rt => rt.id === id)
      if (index >= 0) {
        roomTypes.value[index] = roomType
      } else {
        roomTypes.value.push(roomType)
      }

      return roomType
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '获取房间类型详情失败'
      ElMessage.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createRoomType(data: CreateRoomTypeRequest) {
    loading.value = true
    error.value = ''

    try {
      const newRoomType = await roomTypeApi.createRoomType(data)
      roomTypes.value.unshift(newRoomType)
      ElMessage.success('房间类型创建成功')
      return newRoomType
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '创建房间类型失败'
      ElMessage.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateRoomType(id: number, data: UpdateRoomTypeRequest) {
    loading.value = true
    error.value = ''

    try {
      const updatedRoomType = await roomTypeApi.updateRoomType(id, data)

      // 更新列表中的数据
      const index = roomTypes.value.findIndex(rt => rt.id === id)
      if (index >= 0) {
        roomTypes.value[index] = updatedRoomType
      }

      // 更新当前房间类型
      if (currentRoomType.value?.id === id) {
        currentRoomType.value = updatedRoomType
      }

      ElMessage.success('房间类型更新成功')
      return updatedRoomType
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '更新房间类型失败'
      ElMessage.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteRoomType(id: number) {
    loading.value = true
    error.value = ''

    try {
      await roomTypeApi.deleteRoomType(id)

      // 从列表中移除
      const index = roomTypes.value.findIndex(rt => rt.id === id)
      if (index >= 0) {
        roomTypes.value.splice(index, 1)
      }

      // 清空当前房间类型
      if (currentRoomType.value?.id === id) {
        currentRoomType.value = null
      }

      ElMessage.success('房间类型删除成功')
    } catch (err: any) {
      error.value = err.response?.data?.error?.message || '删除房间类型失败'
      ElMessage.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateRoomTypeStatus(id: number, status: 'ACTIVE' | 'INACTIVE') {
    return updateRoomType(id, { status })
  }

  function clearCurrentRoomType() {
    currentRoomType.value = null
  }

  function clearError() {
    error.value = ''
  }

  return {
    // State
    roomTypes,
    currentRoomType,
    loading,
    error,
    pagination,

    // Getters
    activeRoomTypes,
    roomTypesByHotel,

    // Actions
    fetchRoomTypes,
    fetchRoomTypesByHotel,
    fetchRoomType,
    createRoomType,
    updateRoomType,
    deleteRoomType,
    updateRoomTypeStatus,
    clearCurrentRoomType,
    clearError
  }
})