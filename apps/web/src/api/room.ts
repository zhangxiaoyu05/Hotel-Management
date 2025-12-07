import { request } from '@/utils/request';
import type {
  Room,
  CreateRoomRequest,
  UpdateRoomRequest,
  RoomSearchRequest,
  RoomListResponse,
  BatchUpdateRequest
} from '@/types/room';
import type { ApiResponse } from '@/types/common';

export const roomApi = {
  // 获取房间列表
  getRooms: (params: {
    page?: number;
    size?: number;
    search?: string;
    hotelId?: number;
    status?: string;
    sortBy?: string;
    sortDir?: string;
  } = {}) => {
    return request.get<ApiResponse<RoomListResponse>>('/api/rooms', { params });
  },

  // 搜索房间
  searchRooms: (data: RoomSearchRequest) => {
    return request.post<ApiResponse<RoomListResponse>>('/api/rooms/search', data);
  },

  // 根据酒店ID获取房间
  getRoomsByHotel: (hotelId: number) => {
    return request.get<ApiResponse<Room[]>>(`/api/rooms/hotel/${hotelId}`);
  },

  // 获取房间详情
  getRoom: (id: number) => {
    return request.get<ApiResponse<Room>>(`/api/rooms/${id}`);
  },

  // 创建房间
  createRoom: (data: CreateRoomRequest) => {
    return request.post<ApiResponse<Room>>('/api/rooms', data);
  },

  // 更新房间
  updateRoom: (id: number, data: UpdateRoomRequest) => {
    return request.put<ApiResponse<Room>>(`/api/rooms/${id}`, data);
  },

  // 删除房间
  deleteRoom: (id: number) => {
    return request.delete<ApiResponse<void>>(`/api/rooms/${id}`);
  },

  // 批量更新房间
  batchUpdateRooms: (data: BatchUpdateRequest) => {
    return request.post<ApiResponse<void>>('/api/rooms/batch-update', data);
  },

  // 根据房间类型获取房间
  getRoomsByRoomType: (roomTypeId: number) => {
    return request.get<ApiResponse<Room[]>>(`/api/rooms/room-types/${roomTypeId}`);
  },

  // 获取可用房间
  getAvailableRooms: () => {
    return request.get<ApiResponse<Room[]>>('/api/rooms/available');
  },

  // 搜索可用房间
  searchAvailableRooms: (data: any) => {
    return request.post<ApiResponse<any>>('/api/rooms/search-available', data);
  },

  // 获取房间类型列表
  getRoomTypes: () => {
    return request.get<ApiResponse<any[]>>('/api/room-types');
  },

  // 获取房间可用性日历数据
  getRoomAvailability: (roomId: number, params: { startDate: string; endDate: string }) =>
    request.get<ApiResponse<any[]>>(`/api/rooms/${roomId}/availability`, { params }),

  // 更新房间状态
  updateRoomStatus: (roomId: number, data: {
    status: string;
    reason: string;
    orderId?: number;
    expectedVersion?: number;
  }) => {
    return request.put<ApiResponse<boolean>>(`/api/v1/rooms/${roomId}/status`, data);
  },

  // 获取房间状态变更日志
  getRoomStatusLogs: (roomId: number, params: {
    page?: number;
    size?: number;
    startDate?: string;
    endDate?: string;
  } = {}) => {
    return request.get<ApiResponse<any>>(`/api/v1/rooms/${roomId}/status/logs`, { params });
  },

  // 获取最近状态变更记录
  getRecentStatusLogs: (roomId: number, limit: number = 10) => {
    return request.get<ApiResponse<any[]>>(`/api/v1/rooms/${roomId}/status/recent`, {
      params: { limit }
    });
  },

  // 检查房间可用性
  checkRoomAvailability: (roomId: number) => {
    return request.get<ApiResponse<boolean>>(`/api/v1/rooms/${roomId}/available`);
  },

  // 批量检查房间可用性
  checkRoomsAvailability: (roomIds: number[]) => {
    return request.post<ApiResponse<Record<number, boolean>>>(`/api/v1/rooms/availability/check`, {
      roomIds
    });
  }
};