import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { roomApi } from '@/api/room';
import type { Room, RoomSearchRequest, RoomListResponse } from '@/types/room';
import { ElMessage } from 'element-plus';

export const useRoomStore = defineStore('room', () => {
  // 状态
  const rooms = ref<Room[]>([]);
  const currentRoom = ref<Room | null>(null);
  const total = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(0);
  const pageSize = ref(20);
  const loading = ref(false);

  // 搜索条件
  const searchParams = ref<RoomSearchRequest>({
    page: 0,
    size: 20,
    sortBy: 'roomNumber',
    sortDir: 'ASC'
  });

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

  return {
    // 状态
    rooms,
    currentRoom,
    total,
    totalPages,
    currentPage,
    pageSize,
    loading,
    searchParams,

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
    clearCurrentRoom
  };
});