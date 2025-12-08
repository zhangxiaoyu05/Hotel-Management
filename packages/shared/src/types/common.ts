/**
 * 通用类型定义
 */

// API响应基础接口
export interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data: T;
  code?: string;
  timestamp?: string;
}

// 分页请求基础接口
export interface PageRequest {
  page: number;
  size: number;
  sortBy?: string;
  sortOrder?: 'ASC' | 'DESC';
}

// 分页响应基础接口
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// 错误响应接口
export interface ErrorResponse {
  success: false;
  message: string;
  code?: string;
  details?: any;
  timestamp: string;
  path?: string;
}

// 搜索历史接口
export interface SearchHistory {
  id: string;
  searchParams: any;
  timestamp: string;
}

// 文件上传响应
export interface FileUploadResponse {
  url: string;
  filename: string;
  size: number;
  contentType: string;
}

// 选项接口
export interface Option {
  label: string;
  value: any;
  disabled?: boolean;
  children?: Option[];
}

// 范围选择接口
export interface Range {
  start: string | number | Date;
  end: string | number | Date;
}

// 日期范围接口
export interface DateRange {
  startDate: string;
  endDate: string;
}

// 价格范围接口
export interface PriceRange {
  min: number;
  max: number;
}

// 坐标接口
export interface Coordinates {
  latitude: number;
  longitude: number;
}

// 地址接口
export interface Address {
  street: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  coordinates?: Coordinates;
}

// 联系信息接口
export interface ContactInfo {
  phone: string;
  email: string;
  website?: string;
}

// 业务规则验证结果
export interface ValidationResult {
  valid: boolean;
  message?: string;
  code?: string;
}

// 批量操作结果
export interface BatchOperationResult<T> {
  success: number;
  failed: number;
  results: T[];
  errors: string[];
}