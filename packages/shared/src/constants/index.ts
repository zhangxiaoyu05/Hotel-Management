/**
 * 应用常量定义
 */

// API相关常量
export const API_CONSTANTS = {
  // HTTP状态码
  STATUS_CODES: {
    OK: 200,
    CREATED: 201,
    NO_CONTENT: 204,
    BAD_REQUEST: 400,
    UNAUTHORIZED: 401,
    FORBIDDEN: 403,
    NOT_FOUND: 404,
    CONFLICT: 409,
    INTERNAL_SERVER_ERROR: 500,
    SERVICE_UNAVAILABLE: 503
  },

  // 分页默认值
  PAGINATION: {
    DEFAULT_PAGE: 0,
    DEFAULT_SIZE: 20,
    MAX_SIZE: 100
  },

  // 缓存配置
  CACHE: {
    DEFAULT_TTL: 5 * 60 * 1000, // 5分钟
    SEARCH_CACHE_TTL: 10 * 60 * 1000, // 10分钟
    MAX_CACHE_SIZE: 100
  },

  // 请求超时
  TIMEOUT: {
    SHORT: 5000, // 5秒
    MEDIUM: 10000, // 10秒
    LONG: 30000 // 30秒
  }
} as const;

// 业务相关常量
export const BUSINESS_CONSTANTS = {
  // 房间
  ROOM: {
    MAX_GUESTS: 10,
    MAX_IMAGES: 10,
    MIN_AREA: 10,
    MAX_AREA: 200
  },

  // 酒店
  HOTEL: {
    MAX_FACILITIES: 20,
    MAX_IMAGES: 20,
    MIN_RATING: 0,
    MAX_RATING: 5
  },

  // 订单
  ORDER: {
    MIN_STAY_NIGHTS: 1,
    MAX_STAY_NIGHTS: 365,
    CANCELLATION_HOURS: 24
  },

  // 价格
  PRICE: {
    MIN_ROOM_PRICE: 0,
    MAX_ROOM_PRICE: 999999,
    DECIMAL_PLACES: 2
  },

  // 文件上传
  FILE_UPLOAD: {
    MAX_SIZE: 5 * 1024 * 1024, // 5MB
    ALLOWED_TYPES: ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'],
    MAX_DIMENSION: 2048
  }
} as const;

// 验证规则常量
export const VALIDATION_CONSTANTS = {
  // 字符串长度
  STRING_LENGTHS: {
    USERNAME_MIN: 3,
    USERNAME_MAX: 50,
    PASSWORD_MIN: 6,
    PASSWORD_MAX: 100,
    NAME_MIN: 2,
    NAME_MAX: 100,
    DESCRIPTION_MAX: 1000,
    PHONE_MAX: 20,
    ROOM_NUMBER_MAX: 10
  },

  // 正则表达式
  REGEX: {
    EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
    PHONE: /^[\+]?[1-9][\d]{0,15}$/,
    USERNAME: /^[a-zA-Z0-9_]+$/,
    ROOM_NUMBER: /^[a-zA-Z0-9-]+$/,
    PASSWORD: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/
  },

  // 数值范围
  NUMBERS: {
    RATING_MIN: 0,
    RATING_MAX: 5,
    FLOOR_MIN: 1,
    FLOOR_MAX: 100,
    GUESTS_MIN: 1,
    GUESTS_MAX: 10
  }
} as const;

// UI相关常量
export const UI_CONSTANTS = {
  // 动画
  ANIMATION: {
    DURATION_SHORT: 200,
    DURATION_MEDIUM: 300,
    DURATION_LONG: 500
  },

  // 断点
  BREAKPOINTS: {
    XS: 576,
    SM: 768,
    MD: 992,
    LG: 1200,
    XL: 1400
  },

  // Z-index层级
  Z_INDEX: {
    MODAL: 1000,
    DROPDOWN: 1050,
    TOOLTIP: 1100,
    NOTIFICATION: 1200,
    LOADING: 1300
  },

  // 颜色
  COLORS: {
    PRIMARY: '#409EFF',
    SUCCESS: '#67C23A',
    WARNING: '#E6A23C',
    DANGER: '#F56C6C',
    INFO: '#909399'
  }
} as const;

// 应用配置常量
export const APP_CONFIG = {
  // 本地存储键
  STORAGE_KEYS: {
    ACCESS_TOKEN: 'access_token',
    REFRESH_TOKEN: 'refresh_token',
    USER_INFO: 'user_info',
    SEARCH_HISTORY: 'room_search_history',
    LANGUAGE: 'language',
    THEME: 'theme'
  },

  // 应用名称和版本
  APP: {
    NAME: '酒店管理系统',
    VERSION: '1.0.0',
    DESCRIPTION: '现代化的酒店管理解决方案'
  },

  // 支持的语言
  SUPPORTED_LANGUAGES: ['zh-CN', 'en-US'],

  // 默认设置
  DEFAULTS: {
    LANGUAGE: 'zh-CN',
    THEME: 'light',
    PAGE_SIZE: 20,
    TIMEZONE: 'Asia/Shanghai'
  }
} as const;