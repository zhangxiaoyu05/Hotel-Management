import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { roomApi } from '@/api/room';
import type { Room, RoomSearchRequest, RoomListResponse, RoomType } from '@/types/room';
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
    clearSearchCache: cleanExpiredCache
  };
});