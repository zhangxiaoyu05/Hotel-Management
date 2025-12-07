import { useRoomStore } from '@/stores/roomStore';
import { useUserStore } from '@/stores/user';
import { ElMessage } from 'element-plus';

export interface WebSocketMessage {
  type: string;
  data: any;
  timestamp: string;
  roomId?: number;
  userId?: number;
}

export interface RoomStatusChangedMessage extends WebSocketMessage {
  type: 'ROOM_STATUS_CHANGED';
  data: {
    roomId: number;
    oldStatus: string;
    newStatus: string;
    timestamp: string;
    changedBy: number;
    reason?: string;
  };
}

export interface WebSocketConfig {
  url: string;
  protocols?: string[];
  reconnectAttempts?: number;
  reconnectInterval?: number;
  heartbeatInterval?: number;
}

export class WebSocketService {
  private static instance: WebSocketService;
  private ws: WebSocket | null = null;
  private config: WebSocketConfig;
  private reconnectAttempts = 0;
  private maxReconnectAttempts: number;
  private reconnectInterval: number;
  private heartbeatInterval: number;
  private heartbeatTimer: NodeJS.Timeout | null = null;
  private messageHandlers: Map<string, ((message: WebSocketMessage) => void)[]> = new Map();
  private connectionPromise: Promise<void> | null = null;
  private isManualClose = false;

  private constructor(config: WebSocketConfig) {
    this.config = {
      reconnectAttempts: 5,
      reconnectInterval: 3000,
      heartbeatInterval: 30000,
      ...config
    };

    this.maxReconnectAttempts = this.config.reconnectAttempts || 5;
    this.reconnectInterval = this.config.reconnectInterval || 3000;
    this.heartbeatInterval = this.config.heartbeatInterval || 30000;

    // 注册默认消息处理器
    this.registerMessageHandler('ROOM_STATUS_CHANGED', this.handleRoomStatusChanged.bind(this));
    this.registerMessageHandler('PONG', this.handlePong.bind(this));

    // 监听页面关闭事件，主动断开连接
    if (typeof window !== 'undefined') {
      window.addEventListener('beforeunload', () => {
        this.isManualClose = true;
        this.disconnect();
      });

      window.addEventListener('online', () => {
        if (!this.isConnected()) {
          this.connect();
        }
      });
    }
  }

  static getInstance(config?: WebSocketConfig): WebSocketService {
    if (!this.instance) {
      if (!config) {
        throw new Error('WebSocket configuration is required for first initialization');
      }
      this.instance = new WebSocketService(config);
    }
    return this.instance;
  }

  /**
   * 连接WebSocket
   */
  async connect(): Promise<void> {
    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    this.connectionPromise = new Promise((resolve, reject) => {
      try {
        const userStore = useUserStore();
        const token = userStore.token;

        if (!token) {
          console.warn('WebSocket: No authentication token available');
          reject(new Error('No authentication token'));
          return;
        }

        // 构建WebSocket URL，包含认证token
        const wsUrl = `${this.config.url}?token=${encodeURIComponent(token)}`;

        this.ws = new WebSocket(wsUrl, this.config.protocols);

        this.ws.onopen = (event) => {
          console.log('WebSocket connected');
          this.reconnectAttempts = 0;
          this.isManualClose = false;
          this.startHeartbeat();
          resolve();
        };

        this.ws.onmessage = (event) => {
          this.handleMessage(event);
        };

        this.ws.onclose = (event) => {
          console.log('WebSocket disconnected:', event.code, event.reason);
          this.stopHeartbeat();
          this.connectionPromise = null;

          if (!this.isManualClose && this.reconnectAttempts < this.maxReconnectAttempts) {
            this.scheduleReconnect();
          }
        };

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error);
          if (this.connectionPromise) {
            reject(error);
            this.connectionPromise = null;
          }
        };

      } catch (error) {
        console.error('Failed to create WebSocket connection:', error);
        reject(error);
        this.connectionPromise = null;
      }
    });

    return this.connectionPromise;
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    this.isManualClose = true;
    this.stopHeartbeat();

    if (this.ws) {
      this.ws.close(1000, 'Manual disconnect');
      this.ws = null;
    }

    this.connectionPromise = null;
  }

  /**
   * 发送消息
   */
  send(message: WebSocketMessage): boolean {
    if (!this.isConnected()) {
      console.warn('WebSocket not connected, message not sent:', message);
      return false;
    }

    try {
      this.ws!.send(JSON.stringify(message));
      return true;
    } catch (error) {
      console.error('Failed to send WebSocket message:', error);
      return false;
    }
  }

  /**
   * 检查连接状态
   */
  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  /**
   * 注册消息处理器
   */
  registerMessageHandler(type: string, handler: (message: WebSocketMessage) => void): () => void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, []);
    }

    const handlers = this.messageHandlers.get(type)!;
    handlers.push(handler);

    // 返回取消注册函数
    return () => {
      const index = handlers.indexOf(handler);
      if (index > -1) {
        handlers.splice(index, 1);
      }
      if (handlers.length === 0) {
        this.messageHandlers.delete(type);
      }
    };
  }

  /**
   * 订阅房间状态变更
   */
  subscribeToRoomStatus(roomId: number, callback: (roomId: number, oldStatus: string, newStatus: string) => void): () => void {
    // 发送订阅消息
    this.send({
      type: 'SUBSCRIBE_ROOM_STATUS',
      data: { roomId },
      timestamp: new Date().toISOString()
    });

    // 注册消息处理器
    return this.registerMessageHandler('ROOM_STATUS_CHANGED', (message: WebSocketMessage) => {
      const statusMessage = message as RoomStatusChangedMessage;
      if (statusMessage.data.roomId === roomId) {
        callback(roomId, statusMessage.data.oldStatus, statusMessage.data.newStatus);
      }
    });
  }

  /**
   * 取消订阅房间状态
   */
  unsubscribeFromRoomStatus(roomId: number): void {
    this.send({
      type: 'UNSUBSCRIBE_ROOM_STATUS',
      data: { roomId },
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(event: MessageEvent): void {
    try {
      const message: WebSocketMessage = JSON.parse(event.data);
      console.log('WebSocket message received:', message);

      const handlers = this.messageHandlers.get(message.type);
      if (handlers) {
        handlers.forEach(handler => {
          try {
            handler(message);
          } catch (error) {
            console.error('Error in message handler for type', message.type, ':', error);
          }
        });
      } else {
        console.warn('No handler for WebSocket message type:', message.type);
      }
    } catch (error) {
      console.error('Failed to parse WebSocket message:', error);
    }
  }

  /**
   * 处理房间状态变更消息
   */
  private handleRoomStatusChanged(message: RoomStatusChangedMessage): void {
    const roomStore = useRoomStore();
    const { roomId, oldStatus, newStatus } = message.data;

    // 更新store中的房间状态
    roomStore.handleRoomStatusChanged(roomId, oldStatus, newStatus);

    // 显示通知（如果不是当前用户触发的）
    const userStore = useUserStore();
    if (message.data.changedBy !== userStore.user?.id) {
      ElMessage.info(`房间 ${roomId} 状态已更新：${oldStatus} → ${newStatus}`);
    }
  }

  /**
   * 处理PONG响应
   */
  private handlePong(): void {
    console.log('WebSocket PONG received');
  }

  /**
   * 安排重连
   */
  private scheduleReconnect(): void {
    this.reconnectAttempts++;
    console.log(`WebSocket reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);

    setTimeout(() => {
      if (!this.isManualClose) {
        this.connect().catch(error => {
          console.error('WebSocket reconnect failed:', error);
        });
      }
    }, this.reconnectInterval);
  }

  /**
   * 开始心跳
   */
  private startHeartbeat(): void {
    this.stopHeartbeat();

    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected()) {
        this.send({
          type: 'PING',
          data: {},
          timestamp: new Date().toISOString()
        });
      }
    }, this.heartbeatInterval);
  }

  /**
   * 停止心跳
   */
  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }
}

// 创建WebSocket实例的工厂函数
export function createWebSocketService(baseURL: string): WebSocketService {
  const wsUrl = baseURL.replace(/^http/, 'ws') + '/ws';

  return WebSocketService.getInstance({
    url: wsUrl,
    reconnectAttempts: 5,
    reconnectInterval: 3000,
    heartbeatInterval: 30000
  });
}

// 导出单例实例（需要在应用初始化时设置）
export let websocketService: WebSocketService | null = null;

// 初始化WebSocket服务的函数
export function initializeWebSocket(baseURL: string): void {
  if (!websocketService) {
    websocketService = createWebSocketService(baseURL);

    // 自动连接（如果用户已登录）
    const userStore = useUserStore();
    if (userStore.isAuthenticated) {
      websocketService.connect().catch(error => {
        console.warn('Failed to initialize WebSocket:', error);
      });
    }
  }
}