import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { roomApi } from '@/api/room';
import { roomStatusService } from '@/services/roomStatusService';
import type { Room, RoomSearchRequest, RoomListResponse, RoomType } from '@/types/room';
import type { RoomStatusLog, RoomStatusUpdateRequest } from '@/services/roomStatusService';
import { ElMessage } from 'element-plus';

// 房间搜索历史类型
interface SearchHistory {
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  hotelId?: number;
  roomTypeId?: number;
  timestamp: number;
}

export const useRoomStore = defineStore('room', () => {
  // 状态
  const rooms = ref<Room[]>([]);
  const currentRoom = ref<Room | null>(null);
  const roomTypes = ref<RoomType[]>([]);
  const total = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(0);
  const pageSize = ref(20);
  const loading = ref(false);
  const searchResults = ref<any>(null);

  // 搜索条件
  const searchParams = ref<RoomSearchRequest>({
    page: 0,
    size: 20,
    sortBy: 'roomNumber',
    sortDir: 'ASC'
  });

  // 搜索历史
  const searchHistory = ref<SearchHistory[]>([]);
  const maxSearchHistory = 10;

  // 状态管理相关状态
  const statusLogs = ref<RoomStatusLog[]>([]);
  const statusLoading = ref(false);
  const roomAvailabilityCache = ref<Map<number, boolean>>(new Map());

  // 计算属性
  const hasNextPage = computed(() => currentPage.value + 1 < totalPages.value);
  const hasPreviousPage = computed(() => currentPage.value > 0);

  // 获取房间列表
  const fetchRooms = async (params?: Partial<RoomSearchRequest>) => {
    loading.value = true;
    try {
      const searchParamsUpdated = { ...searchParams.value, ...params };
      searchParams.value = searchParamsUpdated;

      const response = await roomApi.searchRooms(searchParamsUpdated);

      if (response.data.success) {
        const roomList = response.data.data;
        rooms.value = roomList.content;
        total.value = roomList.totalElements;
        totalPages.value = roomList.totalPages;
        currentPage.value = roomList.number;
        pageSize.value = roomList.size;
      } else {
        ElMessage.error(response.data.message || '获取房间列表失败');
      }
    } catch (error) {
      console.error('获取房间列表失败:', error);
      ElMessage.error('获取房间列表失败');
    } finally {
      loading.value = false;
    }
  };

  // 获取房间详情
  const fetchRoom = async (id: number) => {
    loading.value = true;
    try {
      const response = await roomApi.getRoom(id);

      if (response.data.success) {
        currentRoom.value = response.data.data;
        return response.data.data;
      } else {
        ElMessage.error(response.data.message || '获取房间详情失败');
        return null;
      }
    } catch (error) {
      console.error('获取房间详情失败:', error);
      ElMessage.error('获取房间详情失败');
      return null;
    } finally {
      loading.value = false;
    }
  };

  // 创建房间
  const createRoom = async (roomData: Omit<Room, 'id' | 'createdAt' | 'updatedAt' | 'hotelId'>) => {
    loading.value = true;
    try {
      const response = await roomApi.createRoom(roomData);

      if (response.data.success) {
        ElMessage.success('房间创建成功');
        await fetchRooms(); // 刷新列表
        return response.data.data;
      } else {
        ElMessage.error(response.data.message || '房间创建失败');
        return null;
      }
    } catch (error) {
      console.error('房间创建失败:', error);
      ElMessage.error('房间创建失败');
      return null;
    } finally {
      loading.value = false;
    }
  };

  // 更新房间
  const updateRoom = async (id: number, roomData: Partial<Room>) => {
    loading.value = true;
    try {
      const response = await roomApi.updateRoom(id, roomData);

      if (response.data.success) {
        ElMessage.success('房间更新成功');

        // 更新列表中的房间
        const index = rooms.value.findIndex(room => room.id === id);
        if (index !== -1) {
          rooms.value[index] = response.data.data;
        }

        // 如果是当前房间，也更新
        if (currentRoom.value?.id === id) {
          currentRoom.value = response.data.data;
        }

        return response.data.data;
      } else {
        ElMessage.error(response.data.message || '房间更新失败');
        return null;
      }
    } catch (error) {
      console.error('房间更新失败:', error);
      ElMessage.error('房间更新失败');
      return null;
    } finally {
      loading.value = false;
    }
  };

  // 删除房间
  const deleteRoom = async (id: number) => {
    loading.value = true;
    try {
      const response = await roomApi.deleteRoom(id);

      if (response.data.success) {
        ElMessage.success('房间删除成功');

        // 从列表中移除
        const index = rooms.value.findIndex(room => room.id === id);
        if (index !== -1) {
          rooms.value.splice(index, 1);
        }

        // 如果是当前房间，清空
        if (currentRoom.value?.id === id) {
          currentRoom.value = null;
        }

        total.value = Math.max(0, total.value - 1);

        return true;
      } else {
        ElMessage.error(response.data.message || '房间删除失败');
        return false;
      }
    } catch (error) {
      console.error('房间删除失败:', error);
      ElMessage.error('房间删除失败');
      return false;
    } finally {
      loading.value = false;
    }
  };

  // 批量更新房间
  const batchUpdateRooms = async (roomIds: number[], updates: { status?: string; price?: number }) => {
    loading.value = true;
    try {
      const response = await roomApi.batchUpdateRooms({
        roomIds,
        updates
      });

      if (response.data.success) {
        ElMessage.success('批量更新成功');
        await fetchRooms(); // 刷新列表
        return true;
      } else {
        ElMessage.error(response.data.message || '批量更新失败');
        return false;
      }
    } catch (error) {
      console.error('批量更新失败:', error);
      ElMessage.error('批量更新失败');
      return false;
    } finally {
      loading.value = false;
    }
  };

  // 重置搜索条件
  const resetSearchParams = () => {
    searchParams.value = {
      page: 0,
      size: 20,
      sortBy: 'roomNumber',
      sortDir: 'ASC'
    };
  };

  // 设置页码
  const setPage = (page: number) => {
    currentPage.value = page;
    fetchRooms({ page });
  };

  // 设置页面大小
  const setPageSize = (size: number) => {
    pageSize.value = size;
    fetchRooms({ size, page: 0 });
  };

  // 根据状态筛选房间
  const getRoomsByStatus = (status: string) => {
    return rooms.value.filter(room => room.status === status);
  };

  // 清空当前房间
  const clearCurrentRoom = () => {
    currentRoom.value = null;
  };

  // 搜索结果缓存
  const searchCache = ref<Map<string, { data: any; timestamp: number }>>(new Map());
  const CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5分钟缓存
  const MAX_CACHE_SIZE = 50; // 最大缓存条目数

  // 生成缓存键
  const generateCacheKey = (searchRequest: any): string => {
    const keyParams = {
      hotelId: searchRequest.hotelId,
      roomTypeId: searchRequest.roomTypeId,
      checkInDate: searchRequest.checkInDate,
      checkOutDate: searchRequest.checkOutDate,
      guestCount: searchRequest.guestCount,
      priceMin: searchRequest.priceMin,
      priceMax: searchRequest.priceMax,
      facilities: searchRequest.facilities?.sort() || [],
      sortBy: searchRequest.sortBy || 'PRICE',
      sortOrder: searchRequest.sortOrder || 'ASC',
      page: searchRequest.page || 0,
      size: searchRequest.size || 20
    };
    return btoa(JSON.stringify(keyParams));
  };

  // 检查缓存是否有效
  const isCacheValid = (cacheEntry: { data: any; timestamp: number }): boolean => {
    if (!cacheEntry) return false;
    return Date.now() - cacheEntry.timestamp < CACHE_EXPIRY_TIME;
  };

  // 从缓存获取搜索结果
  const getCachedSearchResult = (searchRequest: any) => {
    const cacheKey = generateCacheKey(searchRequest);
    const cacheEntry = searchCache.value.get(cacheKey);

    if (isCacheValid(cacheEntry)) {
      console.log('从缓存返回搜索结果:', cacheKey.substring(0, 20) + '...');
      return cacheEntry.data;
    }

    // 清理过期缓存
    if (cacheEntry) {
      searchCache.value.delete(cacheKey);
    }

    return null;
  };

  // 缓存搜索结果
  const cacheSearchResult = (searchRequest: any, result: any) => {
    const cacheKey = generateCacheKey(searchRequest);

    // 如果缓存已满，删除最旧的条目
    if (searchCache.value.size >= MAX_CACHE_SIZE) {
      const oldestKey = searchCache.value.keys().next().value;
      searchCache.value.delete(oldestKey);
    }

    searchCache.value.set(cacheKey, {
      data: result,
      timestamp: Date.now()
    });

    console.log('缓存搜索结果:', cacheKey.substring(0, 20) + '...');
  };

  // 清理过期缓存
  const cleanExpiredCache = () => {
    const now = Date.now();
    for (const [key, entry] of searchCache.value.entries()) {
      if (now - entry.timestamp >= CACHE_EXPIRY_TIME) {
        searchCache.value.delete(key);
      }
    }
  };

  // 搜索可用房间（带缓存）
  const searchAvailableRooms = async (searchRequest: any) => {
    // 先检查缓存
    const cachedResult = getCachedSearchResult(searchRequest);
    if (cachedResult) {
      searchResults.value = cachedResult;
      return cachedResult;
    }

    loading.value = true;
    try {
      const response = await roomApi.searchAvailableRooms(searchRequest);

      if (response.data.success) {
        const result = response.data.data;
        searchResults.value = result;

        // 缓存结果
        cacheSearchResult(searchRequest, result);

        return result;
      } else {
        ElMessage.error(response.data.message || '搜索可用房间失败');
        return null;
      }
    } catch (error) {
      console.error('搜索可用房间失败:', error);
      ElMessage.error('搜索可用房间失败');
      return null;
    } finally {
      loading.value = false;
    }
  };

  // 获取房间类型列表
  const fetchRoomTypes = async () => {
    try {
      const response = await roomApi.getRoomTypes();

      if (response.data.success) {
        roomTypes.value = response.data.data;
        return response.data.data;
      } else {
        ElMessage.error('获取房间类型失败');
        return [];
      }
    } catch (error) {
      console.error('获取房间类型失败:', error);
      ElMessage.error('获取房间类型失败');
      return [];
    }
  };

  // 保存搜索历史到本地存储
  const saveSearchHistory = (history: SearchHistory) => {
    try {
      // 检查是否已存在相同的搜索
      const existingIndex = searchHistory.value.findIndex(
        h => h.checkInDate === history.checkInDate &&
             h.checkOutDate === history.checkOutDate &&
             h.guestCount === history.guestCount &&
             h.hotelId === history.hotelId &&
             h.roomTypeId === history.roomTypeId
      );

      if (existingIndex !== -1) {
        // 如果存在，移除旧的
        searchHistory.value.splice(existingIndex, 1);
      }

      // 添加新的搜索历史到开头
      searchHistory.value.unshift(history);

      // 限制历史记录数量
      if (searchHistory.value.length > maxSearchHistory) {
        searchHistory.value = searchHistory.value.slice(0, maxSearchHistory);
      }

      // 保存到 localStorage
      localStorage.setItem('room_search_history', JSON.stringify(searchHistory.value));
    } catch (error) {
      console.error('保存搜索历史失败:', error);
    }
  };

  // 从本地存储加载搜索历史
  const loadSearchHistory = () => {
    try {
      const saved = localStorage.getItem('room_search_history');
      if (saved) {
        searchHistory.value = JSON.parse(saved);
      }
    } catch (error) {
      console.error('加载搜索历史失败:', error);
      searchHistory.value = [];
    }
  };

  // 清空搜索历史
  const clearSearchHistory = () => {
    searchHistory.value = [];
    localStorage.removeItem('room_search_history');
  };

  // 更新房间状态
  const updateRoomStatus = async (roomId: number, data: RoomStatusUpdateRequest) => {
    statusLoading.value = true;
    try {
      const result = await roomStatusService.updateRoomStatus(roomId, data);

      if (result.success) {
        ElMessage.success('房间状态更新成功');

        // 更新本地房间数据
        const roomIndex = rooms.value.findIndex(room => room.id === roomId);
        if (roomIndex !== -1) {
          const oldStatus = rooms.value[roomIndex].status;
          rooms.value[roomIndex].status = data.status;
          rooms.value[roomIndex].version = (rooms.value[roomIndex].version || 1) + 1;
        }

        // 更新当前房间
        if (currentRoom.value?.id === roomId) {
          currentRoom.value.status = data.status;
          currentRoom.value.version = (currentRoom.value.version || 1) + 1;
        }

        return true;
      } else if (result.conflict) {
        ElMessage.error(result.conflict.message || '房间已被其他用户修改，请刷新后重试');
        return false;
      }
    } catch (error: any) {
      console.error('更新房间状态失败:', error);
      ElMessage.error(error.message || '更新房间状态失败');
      return false;
    } finally {
      statusLoading.value = false;
    }
  };

  // 获取房间状态变更日志
  const fetchRoomStatusLogs = async (
    roomId: number,
    params: { page?: number; size?: number; startDate?: string; endDate?: string } = {}
  ) => {
    statusLoading.value = true;
    try {
      const logs = await roomStatusService.getRoomStatusLogs(roomId, params);
      statusLogs.value = logs.records;
      return logs;
    } catch (error) {
      console.error('获取房间状态日志失败:', error);
      ElMessage.error('获取房间状态日志失败');
      return null;
    } finally {
      statusLoading.value = false;
    }
  };

  // 获取最近状态变更记录
  const fetchRecentStatusLogs = async (roomId: number, limit: number = 10) => {
    try {
      const logs = await roomStatusService.getRecentStatusLogs(roomId, limit);
      return logs;
    } catch (error) {
      console.error('获取最近状态变更记录失败:', error);
      ElMessage.error('获取最近状态变更记录失败');
      return [];
    }
  };

  // 检查房间可用性（带缓存）
  const checkRoomAvailability = async (roomId: number, useCache: boolean = true) => {
    // 检查缓存
    if (useCache && roomAvailabilityCache.value.has(roomId)) {
      return roomAvailabilityCache.value.get(roomId);
    }

    try {
      const available = await roomStatusService.checkRoomAvailability(roomId);

      // 更新缓存
      roomAvailabilityCache.value.set(roomId, available);

      // 5分钟后清除缓存
      setTimeout(() => {
        roomAvailabilityCache.value.delete(roomId);
      }, 5 * 60 * 1000);

      return available;
    } catch (error) {
      console.error('检查房间可用性失败:', error);
      return false;
    }
  };

  // 批量检查房间可用性
  const checkRoomsAvailability = async (roomIds: number[]) => {
    try {
      const availability = await roomStatusService.checkRoomsAvailability(roomIds);

      // 更新缓存
      Object.entries(availability).forEach(([roomId, isAvailable]) => {
        roomAvailabilityCache.value.set(Number(roomId), isAvailable);

        // 5分钟后清除缓存
        setTimeout(() => {
          roomAvailabilityCache.value.delete(Number(roomId));
        }, 5 * 60 * 1000);
      });

      return availability;
    } catch (error) {
      console.error('批量检查房间可用性失败:', error);
      ElMessage.error('检查房间可用性失败');
      return {};
    }
  };

  // 订阅房间状态变更
  const subscribeToRoomStatusChanges = (
    roomId: number,
    callback: (roomId: number, oldStatus: string, newStatus: string) => void
  ) => {
    return roomStatusService.subscribeToStatusChanges(roomId, callback);
  };

  // 处理房间状态变更通知（由WebSocket调用）
  const handleRoomStatusChanged = (roomId: number, oldStatus: string, newStatus: string) => {
    // 更新本地房间数据
    const roomIndex = rooms.value.findIndex(room => room.id === roomId);
    if (roomIndex !== -1) {
      rooms.value[roomIndex].status = newStatus;
    }

    // 更新当前房间
    if (currentRoom.value?.id === roomId) {
      currentRoom.value.status = newStatus;
    }

    // 更新可用性缓存
    roomAvailabilityCache.value.set(roomId, newStatus === 'AVAILABLE');

    // 通知订阅者
    roomStatusService.onRoomStatusChanged(roomId, oldStatus, newStatus);
  };

  // 获取状态显示文本
  const getStatusDisplayText = (status: string) => {
    return roomStatusService.getStatusDisplayText(status);
  };

  // 获取状态颜色
  const getStatusColor = (status: string) => {
    return roomStatusService.getStatusColor(status);
  };

  // 验证状态流转是否合法
  const isValidStatusTransition = (currentStatus: string, newStatus: string) => {
    return roomStatusService.isValidStatusTransition(currentStatus, newStatus);
  };

  // 清除可用性缓存
  const clearAvailabilityCache = (roomId?: number) => {
    if (roomId) {
      roomAvailabilityCache.value.delete(roomId);
    } else {
      roomAvailabilityCache.value.clear();
    }
  };

  // 初始化时加载搜索历史
  loadSearchHistory();

  return {
    // 状态
    rooms,
    currentRoom,
    roomTypes,
    total,
    totalPages,
    currentPage,
    pageSize,
    loading,
    searchParams,
    searchResults,
    searchHistory,
    statusLogs,
    statusLoading,

    // 计算属性
    hasNextPage,
    hasPreviousPage,

    // 方法
    fetchRooms,
    fetchRoom,
    createRoom,
    updateRoom,
    deleteRoom,
    batchUpdateRooms,
    resetSearchParams,
    setPage,
    setPageSize,
    getRoomsByStatus,
    clearCurrentRoom,
    searchAvailableRooms,
    fetchRoomTypes,
    saveSearchHistory,
    loadSearchHistory,
    clearSearchHistory,
    // 缓存相关方法
    clearSearchCache: cleanExpiredCache,
    // 状态管理相关方法
    updateRoomStatus,
    fetchRoomStatusLogs,
    fetchRecentStatusLogs,
    checkRoomAvailability,
    checkRoomsAvailability,
    subscribeToRoomStatusChanges,
    handleRoomStatusChanged,
    getStatusDisplayText,
    getStatusColor,
    isValidStatusTransition,
    clearAvailabilityCache
  };
});