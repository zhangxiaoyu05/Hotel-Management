export interface Room {
  id: number;
  hotelId: number;
  roomTypeId: number;
  roomNumber: string;
  floor: number;
  area: number;
  status: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'CLEANING';
  price: number;
  images: string[];
  createdAt: string;
  updatedAt: string;
  roomTypeName?: string;
}

export interface CreateRoomRequest {
  roomNumber: string;
  roomTypeId: number;
  floor: number;
  area: number;
  price: number;
  status?: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'CLEANING';
  images?: string[];
}

export interface UpdateRoomRequest {
  roomNumber?: string;
  roomTypeId?: number;
  floor?: number;
  area?: number;
  price?: number;
  status?: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'CLEANING';
  images?: string[];
}

export interface RoomSearchRequest {
  hotelId?: number;
  roomTypeId?: number;
  status?: string;
  roomNumber?: string;
  floor?: number;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'ASC' | 'DESC';
}

export interface RoomListResponse {
  content: Room[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface BatchUpdateRequest {
  roomIds: number[];
  updates: {
    status?: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'CLEANING';
    price?: number;
  };
}

export interface RoomStatus {
  value: string;
  label: string;
  color: string;
}

export const ROOM_STATUS_OPTIONS: RoomStatus[] = [
  { value: 'AVAILABLE', label: '可用', color: 'success' },
  { value: 'OCCUPIED', label: '已预订', color: 'danger' },
  { value: 'MAINTENANCE', label: '维护中', color: 'warning' },
  { value: 'CLEANING', label: '清洁中', color: 'info' }
];