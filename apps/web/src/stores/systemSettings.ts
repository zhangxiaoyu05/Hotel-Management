import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export interface BasicSettings {
  id?: number
  systemName: string
  systemLogo?: string
  contactPhone?: string
  contactEmail?: string
  contactAddress?: string
  systemDescription?: string
  businessHours?: string
  updatedAt?: string
  updatedBy?: string
}

export interface BusinessRules {
  id?: number
  minBookingDays: number
  maxBookingDays: number
  advanceBookingLimitDays: number
  cancelBeforeHours: number
  cancelFeePercentage: number
  enableFreeCancel: boolean
  freeCancelHours: number
  defaultRoomPrice: number
  enableDynamicPricing: boolean
  peakSeasonPriceMultiplier: number
  offSeasonPriceMultiplier: number
  weekendPriceMultiplier: number
  updatedAt?: string
  updatedBy?: string
}

export interface NotificationSettings {
  id?: number
  enableEmailNotifications?: boolean
  emailFromAddress?: string
  emailFromName?: string
  smtpHost?: string
  smtpPort?: number
  smtpSslEnabled?: boolean
  smtpUsername?: string
  enableSmsNotifications?: boolean
  smsProvider?: string
  smsApiKey?: string
  smsApiSecret?: string
  smsSignName?: string
  enableBookingNotifications?: boolean
  enableCancelNotifications?: boolean
  enablePaymentNotifications?: boolean
  enableCheckInNotifications?: boolean
  enableCheckOutNotifications?: boolean
  updatedAt?: string
  updatedBy?: string
}

export interface SecuritySettings {
  id?: number
  passwordMinLength: number
  passwordRequireUppercase: boolean
  passwordRequireLowercase: boolean
  passwordRequireNumbers: boolean
  passwordRequireSpecialChars: boolean
  passwordExpiryDays: number
  passwordHistoryCount: number
  maxLoginAttempts: number
  accountLockoutMinutes: number
  sessionTimeoutMinutes: number
  enableTwoFactorAuth: boolean
  forceTwoFactorAuth: boolean
  enableCaptcha: boolean
  enableIpWhitelist: boolean
  allowedIpRanges?: string
  enableAuditLog: boolean
  updatedAt?: string
  updatedBy?: string
}

export interface BackupSettings {
  id?: number
  enableAutoBackup: boolean
  backupIntervalDays: number
  backupRetentionDays: number
  backupTime: string
  backupStorageType: string
  localBackupPath?: string
  cloudBackupProvider?: string
  cloudBackupBucket?: string
  cloudBackupRegion?: string
  cloudAccessKey?: string
  cloudSecretKey?: string
  enableBackupEncryption: boolean
  backupEncryptionKey?: string
  enableDatabaseBackup: boolean
  enableFileBackup: boolean
  backupCompressionType: string
  lastBackupTime?: string
  nextBackupTime?: string
  updatedAt?: string
  updatedBy?: string
}

export const useSystemSettingsStore = defineStore('systemSettings', () => {
  // State
  const basicSettings = ref<BasicSettings>({
    systemName: '',
    systemLogo: '',
    contactPhone: '',
    contactEmail: '',
    contactAddress: '',
    systemDescription: '',
    businessHours: ''
  })

  const businessRules = ref<BusinessRules>({
    minBookingDays: 1,
    maxBookingDays: 30,
    advanceBookingLimitDays: 365,
    cancelBeforeHours: 24,
    cancelFeePercentage: 0,
    enableFreeCancel: true,
    freeCancelHours: 24,
    defaultRoomPrice: 299,
    enableDynamicPricing: false,
    peakSeasonPriceMultiplier: 1.5,
    offSeasonPriceMultiplier: 0.8,
    weekendPriceMultiplier: 1.2
  })

  const notificationSettings = ref<NotificationSettings>({
    enableEmailNotifications: false,
    enableSmsNotifications: false,
    enableBookingNotifications: true,
    enableCancelNotifications: true,
    enablePaymentNotifications: true,
    enableCheckInNotifications: true,
    enableCheckOutNotifications: true
  })

  const securitySettings = ref<SecuritySettings>({
    passwordMinLength: 8,
    passwordRequireUppercase: true,
    passwordRequireLowercase: true,
    passwordRequireNumbers: true,
    passwordRequireSpecialChars: true,
    passwordExpiryDays: 90,
    passwordHistoryCount: 5,
    maxLoginAttempts: 5,
    accountLockoutMinutes: 30,
    sessionTimeoutMinutes: 120,
    enableTwoFactorAuth: false,
    forceTwoFactorAuth: false,
    enableCaptcha: false,
    enableIpWhitelist: false,
    enableAuditLog: true
  })

  const backupSettings = ref<BackupSettings>({
    enableAutoBackup: true,
    backupIntervalDays: 7,
    backupRetentionDays: 30,
    backupTime: '02:00',
    backupStorageType: 'local',
    localBackupPath: '/backups',
    enableBackupEncryption: true,
    enableDatabaseBackup: true,
    enableFileBackup: true,
    backupCompressionType: 'gzip'
  })

  const loading = ref(false)
  const error = ref<string | null>(null)

  // Actions
  const fetchBasicSettings = async () => {
    try {
      loading.value = true
      error.value = null
      const response = await api.get('/v1/admin/settings/basic')
      basicSettings.value = response.data.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取基础设置失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateBasicSettings = async (settings: Partial<BasicSettings>) => {
    try {
      loading.value = true
      error.value = null
      const response = await api.put('/v1/admin/settings/basic', settings)
      basicSettings.value = response.data.data
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '更新基础设置失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchBusinessRules = async () => {
    try {
      loading.value = true
      error.value = null
      const response = await api.get('/v1/admin/settings/business-rules')
      businessRules.value = response.data.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取业务规则失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateBusinessRules = async (rules: Partial<BusinessRules>) => {
    try {
      loading.value = true
      error.value = null
      const response = await api.put('/v1/admin/settings/business-rules', rules)
      businessRules.value = response.data.data
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '更新业务规则失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchNotificationSettings = async () => {
    try {
      loading.value = true
      error.value = null
      const response = await api.get('/v1/admin/settings/notifications')
      notificationSettings.value = response.data.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取通知设置失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateNotificationSettings = async (settings: Partial<NotificationSettings>) => {
    try {
      loading.value = true
      error.value = null
      const response = await api.put('/v1/admin/settings/notifications', settings)
      notificationSettings.value = response.data.data
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '更新通知设置失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchSecuritySettings = async () => {
    try {
      loading.value = true
      error.value = null
      const response = await api.get('/v1/admin/settings/security')
      securitySettings.value = response.data.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取安全设置失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateSecuritySettings = async (settings: Partial<SecuritySettings>) => {
    try {
      loading.value = true
      error.value = null
      const response = await api.put('/v1/admin/settings/security', settings)
      securitySettings.value = response.data.data
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '更新安全设置失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchBackupSettings = async () => {
    try {
      loading.value = true
      error.value = null
      const response = await api.get('/v1/admin/settings/backup')
      backupSettings.value = response.data.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '获取备份设置失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateBackupSettings = async (settings: Partial<BackupSettings>) => {
    try {
      loading.value = true
      error.value = null
      const response = await api.put('/v1/admin/settings/backup', settings)
      backupSettings.value = response.data.data
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '更新备份设置失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const executeBackup = async () => {
    try {
      loading.value = true
      error.value = null
      const response = await api.post('/v1/admin/settings/backup/execute')
      // Refresh backup settings after execution
      await fetchBackupSettings()
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || '执行备份失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    // State
    basicSettings,
    businessRules,
    notificationSettings,
    securitySettings,
    backupSettings,
    loading,
    error,

    // Actions
    fetchBasicSettings,
    updateBasicSettings,
    fetchBusinessRules,
    updateBusinessRules,
    fetchNotificationSettings,
    updateNotificationSettings,
    fetchSecuritySettings,
    updateSecuritySettings,
    fetchBackupSettings,
    updateBackupSettings,
    executeBackup
  }
})