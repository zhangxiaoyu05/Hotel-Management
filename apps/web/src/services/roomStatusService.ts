import { roomApi } from '@/api/room';
import type { ApiResponse } from '@/types/common';

export interface RoomStatusUpdateRequest {
  status: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'CLEANING';
  reason: string;
  orderId?: number;
  expectedVersion?: number;
}

export interface RoomStatusLog {
  id: number;
  roomId: number;
  oldStatus: string;
  newStatus: string;
  reason: string;
  changedBy: number;
  orderId?: number;
  createdAt: string;
}

export interface RoomStatusLogListResponse {
  records: RoomStatusLog[];
  total: number;
  current: number;
  size: number;
}

export interface RoomStatusConflict {
  roomId: number;
  expectedVersion: number;
  actualVersion: number;
  message: string;
}

export class RoomStatusService {
  private static instance: RoomStatusService;
  private statusChangeCallbacks: Map<number, ((roomId: number, oldStatus: string, newStatus: string) => void)[]> = new Map();

  private constructor() {}

  static getInstance(): RoomStatusService {
    if (!this.instance) {
      this.instance = new RoomStatusService();
    }
    return this.instance;
  }

  /**
   * 更新房间状态
   */
  async updateRoomStatus(
    roomId: number,
    data: RoomStatusUpdateRequest
  ): Promise<{ success: boolean; conflict?: RoomStatusConflict }> {
    try {
      const response = await roomApi.updateRoomStatus(roomId, data);

      if (response.success) {
        return { success: true };
      } else {
        return {
          success: false,
          conflict: {
            roomId,
            expectedVersion: data.expectedVersion || 0,
            actualVersion: 0,
            message: response.message || '更新失败'
          }
        };
      }
    } catch (error: any) {
      // 检查是否是版本冲突
      if (error.response?.status === 409 ||
          (error.response?.data?.message &&
           error.response.data.message.includes('version'))) {
        return {
          success: false,
          conflict: {
            roomId,
            expectedVersion: data.expectedVersion || 0,
            actualVersion: 0,
            message: error.response.data.message || '房间已被其他用户修改，请刷新后重试'
          }
        };
      }

      throw error;
    }
  }

  /**
   * 获取房间状态变更日志（分页）
   */
  async getRoomStatusLogs(
    roomId: number,
    params: {
      page?: number;
      size?: number;
      startDate?: string;
      endDate?: string;
    } = {}
  ): Promise<RoomStatusLogListResponse> {
    const response = await roomApi.getRoomStatusLogs(roomId, params);
    return response.data;
  }

  /**
   * 获取最近状态变更记录
   */
  async getRecentStatusLogs(roomId: number, limit: number = 10): Promise<RoomStatusLog[]> {
    const response = await roomApi.getRecentStatusLogs(roomId, limit);
    return response.data;
  }

  /**
   * 检查房间可用性
   */
  async checkRoomAvailability(roomId: number): Promise<boolean> {
    const response = await roomApi.checkRoomAvailability(roomId);
    return response.data;
  }

  /**
   * 批量检查房间可用性
   */
  async checkRoomsAvailability(roomIds: number[]): Promise<Record<number, boolean>> {
    const response = await roomApi.checkRoomsAvailability(roomIds);
    return response.data;
  }

  /**
   * 订阅房间状态变更事件
   */
  subscribeToStatusChanges(
    roomId: number,
    callback: (roomId: number, oldStatus: string, newStatus: string) => void
  ): () => void {
    if (!this.statusChangeCallbacks.has(roomId)) {
      this.statusChangeCallbacks.set(roomId, []);
    }

    this.statusChangeCallbacks.get(roomId)!.push(callback);

    // 返回取消订阅函数
    return () => {
      const callbacks = this.statusChangeCallbacks.get(roomId);
      if (callbacks) {
        const index = callbacks.indexOf(callback);
        if (index > -1) {
          callbacks.splice(index, 1);
        }
        if (callbacks.length === 0) {
          this.statusChangeCallbacks.delete(roomId);
        }
      }
    };
  }

  /**
   * 通知房间状态变更（由WebSocket或轮询触发）
   */
  private notifyStatusChange(roomId: number, oldStatus: string, newStatus: string): void {
    const callbacks = this.statusChangeCallbacks.get(roomId);
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(roomId, oldStatus, newStatus);
        } catch (error) {
          console.error('Error in status change callback:', error);
        }
      });
    }
  }

  /**
   * 模拟状态变更通知（实际由WebSocket服务调用）
   */
  onRoomStatusChanged(roomId: number, oldStatus: string, newStatus: string): void {
    this.notifyStatusChange(roomId, oldStatus, newStatus);
  }

  /**
   * 验证状态流转是否合法
   */
  isValidStatusTransition(currentStatus: string, newStatus: string): boolean {
    if (currentStatus === newStatus) {
      return false;
    }

    const validTransitions: Record<string, string[]> = {
      'AVAILABLE': ['OCCUPIED', 'MAINTENANCE'],
      'OCCUPIED': ['CLEANING'],
      'CLEANING': ['AVAILABLE', 'MAINTENANCE'],
      'MAINTENANCE': ['AVAILABLE']
    };

    return validTransitions[currentStatus]?.includes(newStatus) || false;
  }

  /**
   * 获取状态显示文本
   */
  getStatusDisplayText(status: string): string {
    const statusMap: Record<string, string> = {
      'AVAILABLE': '可用',
      'OCCUPIED': '已占用',
      'MAINTENANCE': '维护中',
      'CLEANING': '清洁中'
    };
    return statusMap[status] || status;
  }

  /**
   * 获取状态颜色
   */
  getStatusColor(status: string): string {
    const colorMap: Record<string, string> = {
      'AVAILABLE': 'success',
      'OCCUPIED': 'error',
      'MAINTENANCE': 'warning',
      'CLEANING': 'info'
    };
    return colorMap[status] || 'default';
  }
}

// 导出单例实例
export const roomStatusService = RoomStatusService.getInstance();