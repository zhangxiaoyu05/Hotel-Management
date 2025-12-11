import { roomApi } from '@/api/room';
import type { Room, CreateRoomRequest, UpdateRoomRequest, RoomSearchRequest, RoomListResponse } from '@/types/room';

export class RoomService {
  // 获取房间列表
  static async getRooms(params: {
    page?: number;
    size?: number;
    search?: string;
    hotelId?: number;
    status?: string;
    sortBy?: string;
    sortDir?: string;
  } = {}) {
    const response = await roomApi.getRooms(params);
    return response.data;
  }

  // 搜索房间
  static async searchRooms(searchRequest: RoomSearchRequest): Promise<RoomListResponse> {
    const response = await roomApi.searchRooms(searchRequest);
    if (response.data.success) {
      return response.data.data;
    }
    throw new Error(response.data.message || '搜索房间失败');
  }

  // 根据酒店ID获取房间
  static async getRoomsByHotel(hotelId: number): Promise<Room[]> {
    const response = await roomApi.getRoomsByHotel(hotelId);
    if (response.data.success) {
      return response.data.data;
    }
    throw new Error(response.data.message || '获取酒店房间失败');
  }

  // 获取房间详情
  static async getRoomById(id: number): Promise<Room> {
    const response = await roomApi.getRoom(id);
    if (response.data.success) {
      return response.data.data;
    }
    throw new Error(response.data.message || '获取房间详情失败');
  }

  // 创建房间
  static async createRoom(roomData: CreateRoomRequest): Promise<Room> {
    const response = await roomApi.createRoom(roomData);
    if (response.data.success) {
      return response.data.data;
    }
    throw new Error(response.data.message || '创建房间失败');
  }

  // 更新房间
  static async updateRoom(id: number, roomData: UpdateRoomRequest): Promise<Room> {
    const response = await roomApi.updateRoom(id, roomData);
    if (response.data.success) {
      return response.data.data;
    }
    throw new Error(response.data.message || '更新房间失败');
  }

  // 删除房间
  static async deleteRoom(id: number): Promise<void> {
    const response = await roomApi.deleteRoom(id);
    if (!response.data.success) {
      throw new Error(response.data.message || '删除房间失败');
    }
  }

  // 批量更新房间
  static async batchUpdateRooms(roomIds: number[], updates: {
    status?: string;
    price?: number;
  }): Promise<void> {
    const response = await roomApi.batchUpdateRooms({
      roomIds,
      updates
    });
    if (!response.data.success) {
      throw new Error(response.data.message || '批量更新房间失败');
    }
  }

  // 根据房间类型获取房间
  static async getRoomsByRoomType(roomTypeId: number): Promise<Room[]> {
    const response = await roomApi.getRoomsByRoomType(roomTypeId);
    if (response.data.success) {
      return response.data.data;
    }
    throw new Error(response.data.message || '获取房间类型房间失败');
  }

  // 获取可用房间
  static async getAvailableRooms(): Promise<Room[]> {
    const response = await roomApi.getAvailableRooms();
    if (response.data.success) {
      return response.data.data;
    }
    throw new Error(response.data.message || '获取可用房间失败');
  }

  // 验证房间号唯一性
  static async validateRoomNumber(roomNumber: string, hotelId?: number, excludeRoomId?: number): Promise<boolean> {
    try {
      const searchRequest: RoomSearchRequest = {
        roomNumber,
        hotelId,
        page: 0,
        size: 1
      };

      const result = await this.searchRooms(searchRequest);

      // 如果没有找到房间，说明房间号可用
      if (result.content.length === 0) {
        return true;
      }

      // 如果找到房间，检查是否是要排除的房间（编辑时）
      if (excludeRoomId && result.content[0].id === excludeRoomId) {
        return true;
      }

      return false;
    } catch (error) {
      console.error('验证房间号失败:', error);
      return false;
    }
  }

  // 格式化房间状态
  static formatRoomStatus(status: string): { label: string; color: string } {
    const statusMap: Record<string, { label: string; color: string }> = {
      'AVAILABLE': { label: '可用', color: 'success' },
      'OCCUPIED': { label: '已预订', color: 'danger' },
      'MAINTENANCE': { label: '维护中', color: 'warning' },
      'CLEANING': { label: '清洁中', color: 'info' }
    };

    return statusMap[status] || { label: status, color: 'default' };
  }

  // 获取房间状态选项
  static getRoomStatusOptions() {
    return [
      { value: 'AVAILABLE', label: '可用', color: 'success' },
      { value: 'OCCUPIED', label: '已预订', color: 'danger' },
      { value: 'MAINTENANCE', label: '维护中', color: 'warning' },
      { value: 'CLEANING', label: '清洁中', color: 'info' }
    ];
  }

  // 计算房间价格区间
  static calculatePriceRange(rooms: Room[]): { min: number; max: number } {
    if (rooms.length === 0) {
      return { min: 0, max: 0 };
    }

    const prices = rooms.map(room => room.price);
    return {
      min: Math.min(...prices),
      max: Math.max(...prices)
    };
  }

  // 按楼层分组房间
  static groupRoomsByFloor(rooms: Room[]): Record<number, Room[]> {
    return rooms.reduce((groups, room) => {
      const floor = room.floor;
      if (!groups[floor]) {
        groups[floor] = [];
      }
      groups[floor].push(room);
      return groups;
    }, {} as Record<number, Room[]>);
  }

  // 按状态统计房间
  static getRoomStatistics(rooms: Room[]): Record<string, number> {
    return rooms.reduce((stats, room) => {
      stats[room.status] = (stats[room.status] || 0) + 1;
      return stats;
    }, {} as Record<string, number>);
  }
}