/**
 * 通用工具函数
 */

import { VALIDATION_CONSTANTS, BUSINESS_CONSTANTS } from '../constants';

// 日期工具函数
export const dateUtils = {
  /**
   * 格式化日期为 YYYY-MM-DD
   */
  formatDate(date: Date | string): string {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  /**
   * 格式化日期时间
   */
  formatDateTime(date: Date | string): string {
    const d = new Date(date);
    return d.toLocaleString('zh-CN');
  },

  /**
   * 计算两个日期之间的天数
   */
  getDaysBetween(startDate: Date | string, endDate: Date | string): number {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const timeDiff = Math.abs(end.getTime() - start.getTime());
    return Math.ceil(timeDiff / (1000 * 60 * 60 * 24));
  },

  /**
   * 添加天数到日期
   */
  addDays(date: Date | string, days: number): Date {
    const d = new Date(date);
    d.setDate(d.getDate() + days);
    return d;
  },

  /**
   * 检查日期是否是今天
   */
  isToday(date: Date | string): boolean {
    const d = new Date(date);
    const today = new Date();
    return d.toDateString() === today.toDateString();
  },

  /**
   * 检查日期是否是过去的日期
   */
  isPast(date: Date | string): boolean {
    const d = new Date(date);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return d < today;
  },

  /**
   * 获取月份的第一天
   */
  getFirstDayOfMonth(date: Date | string): Date {
    const d = new Date(date);
    return new Date(d.getFullYear(), d.getMonth(), 1);
  },

  /**
   * 获取月份的最后一天
   */
  getLastDayOfMonth(date: Date | string): Date {
    const d = new Date(date);
    return new Date(d.getFullYear(), d.getMonth() + 1, 0);
  }
};

// 价格工具函数
export const priceUtils = {
  /**
   * 格式化价格显示
   */
  formatPrice(price: number, currency: string = '元'): string {
    return `${price.toFixed(2)}${currency}`;
  },

  /**
   * 计算总价（住宿天数 × 每晚价格）
   */
  calculateTotalPrice(pricePerNight: number, nights: number): number {
    return Math.round(pricePerNight * nights * 100) / 100; // 保留两位小数
  },

  /**
   * 应用折扣
   */
  applyDiscount(price: number, discountPercent: number): number {
    const discount = price * (discountPercent / 100);
    return Math.round((price - discount) * 100) / 100;
  },

  /**
   * 计算税费
   */
  calculateTax(amount: number, taxRate: number): number {
    return Math.round(amount * (taxRate / 100) * 100) / 100;
  }
};

// 验证工具函数
export const validationUtils = {
  /**
   * 验证邮箱格式
   */
  isValidEmail(email: string): boolean {
    return VALIDATION_CONSTANTS.REGEX.EMAIL.test(email);
  },

  /**
   * 验证手机号格式
   */
  isValidPhone(phone: string): boolean {
    return VALIDATION_CONSTANTS.REGEX.PHONE.test(phone);
  },

  /**
   * 验证用户名格式
   */
  isValidUsername(username: string): boolean {
    return VALIDATION_CONSTANTS.REGEX.USERNAME.test(username) &&
           username.length >= VALIDATION_CONSTANTS.STRING_LENGTHS.USERNAME_MIN &&
           username.length <= VALIDATION_CONSTANTS.STRING_LENGTHS.USERNAME_MAX;
  },

  /**
   * 验证密码强度
   */
  isValidPassword(password: string): boolean {
    return password.length >= VALIDATION_CONSTANTS.STRING_LENGTHS.PASSWORD_MIN &&
           VALIDATION_CONSTANTS.REGEX.PASSWORD.test(password);
  },

  /**
   * 验证日期范围是否有效
   */
  isValidDateRange(startDate: string, endDate: string): boolean {
    const start = new Date(startDate);
    const end = new Date(endDate);
    return start < end && !dateUtils.isPast(startDate);
  },

  /**
   * 验证价格范围
   */
  isValidPrice(price: number): boolean {
    return price >= BUSINESS_CONSTANTS.PRICE.MIN_ROOM_PRICE &&
           price <= BUSINESS_CONSTANTS.PRICE.MAX_ROOM_PRICE;
  },

  /**
   * 验证房号格式
   */
  isValidRoomNumber(roomNumber: string): boolean {
    return VALIDATION_CONSTANTS.REGEX.ROOM_NUMBER.test(roomNumber) &&
           roomNumber.length <= VALIDATION_CONSTANTS.STRING_LENGTHS.ROOM_NUMBER_MAX;
  }
};

// 字符串工具函数
export const stringUtils = {
  /**
   * 首字母大写
   */
  capitalize(str: string): string {
    return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
  },

  /**
   * 驼峰命名转换
   */
  toCamelCase(str: string): string {
    return str.replace(/([-_][a-z])/g, (group) =>
      group.toUpperCase()
        .replace('-', '')
        .replace('_', '')
    );
  },

  /**
   * 生成随机字符串
   */
  generateRandomString(length: number = 10): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  },

  /**
   * 截断文本
   */
  truncate(text: string, maxLength: number, suffix: string = '...'): string {
    if (text.length <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength - suffix.length) + suffix;
  },

  /**
   * 移除HTML标签
   */
  stripHtml(html: string): string {
    return html.replace(/<[^>]*>/g, '');
  }
};

// 数组工具函数
export const arrayUtils = {
  /**
   * 数组去重
   */
  unique<T>(array: T[]): T[] {
    return [...new Set(array)];
  },

  /**
   * 按属性分组
   */
  groupBy<T, K extends keyof T>(array: T[], key: K): Record<string, T[]> {
    return array.reduce((groups, item) => {
      const groupKey = String(item[key]);
      groups[groupKey] = groups[groupKey] || [];
      groups[groupKey].push(item);
      return groups;
    }, {} as Record<string, T[]>);
  },

  /**
   * 数组分块
   */
  chunk<T>(array: T[], size: number): T[][] {
    const chunks: T[][] = [];
    for (let i = 0; i < array.length; i += size) {
      chunks.push(array.slice(i, i + size));
    }
    return chunks;
  },

  /**
   * 随机打乱数组
   */
  shuffle<T>(array: T[]): T[] {
    const shuffled = [...array];
    for (let i = shuffled.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled;
  }
};

// 对象工具函数
export const objectUtils = {
  /**
   * 深拷贝对象
   */
  deepClone<T>(obj: T): T {
    return JSON.parse(JSON.stringify(obj));
  },

  /**
   * 合并对象
   */
  merge<T extends object, U extends object>(target: T, source: U): T & U {
    return { ...target, ...source };
  },

  /**
   * 获取嵌套属性值
   */
  get(obj: any, path: string, defaultValue?: any): any {
    const keys = path.split('.');
    let result = obj;
    for (const key of keys) {
      if (result === null || result === undefined) {
        return defaultValue;
      }
      result = result[key];
    }
    return result !== undefined ? result : defaultValue;
  },

  /**
   * 检查对象是否为空
   */
  isEmpty(obj: any): boolean {
    if (obj == null) return true;
    if (Array.isArray(obj) || typeof obj === 'string') return obj.length === 0;
    if (typeof obj === 'object') return Object.keys(obj).length === 0;
    return false;
  }
};

// 存储工具函数
export const storageUtils = {
  /**
   * 设置本地存储
   */
  set(key: string, value: any): void {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      console.error('Failed to save to localStorage:', error);
    }
  },

  /**
   * 获取本地存储
   */
  get<T>(key: string, defaultValue?: T): T | null {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : defaultValue || null;
    } catch (error) {
      console.error('Failed to read from localStorage:', error);
      return defaultValue || null;
    }
  },

  /**
   * 删除本地存储
   */
  remove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch (error) {
      console.error('Failed to remove from localStorage:', error);
    }
  },

  /**
   * 清空本地存储
   */
  clear(): void {
    try {
      localStorage.clear();
    } catch (error) {
      console.error('Failed to clear localStorage:', error);
    }
  }
};

// 导出所有工具
export default {
  dateUtils,
  priceUtils,
  validationUtils,
  stringUtils,
  arrayUtils,
  objectUtils,
  storageUtils
};