import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSystemSettingsStore } from '@/stores/systemSettings'
import api from '@/api'

// Mock the API module
vi.mock('@/api', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn()
  }
}))

describe('SystemSettings Store', () => {
  let store: ReturnType<typeof useSystemSettingsStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useSystemSettingsStore()
    vi.clearAllMocks()
  })

  describe('Initial State', () => {
    it('has correct initial basic settings', () => {
      expect(store.basicSettings.systemName).toBe('')
      expect(store.basicSettings.contactPhone).toBe('')
      expect(store.basicSettings.contactEmail).toBe('')
    })

    it('has correct initial business rules', () => {
      expect(store.businessRules.minBookingDays).toBe(1)
      expect(store.businessRules.maxBookingDays).toBe(30)
      expect(store.businessRules.enableFreeCancel).toBe(true)
    })

    it('has correct initial security settings', () => {
      expect(store.securitySettings.passwordMinLength).toBe(8)
      expect(store.securitySettings.maxLoginAttempts).toBe(5)
      expect(store.securitySettings.enableAuditLog).toBe(true)
    })

    it('has correct initial backup settings', () => {
      expect(store.backupSettings.enableAutoBackup).toBe(true)
      expect(store.backupSettings.backupIntervalDays).toBe(7)
      expect(store.backupSettings.enableDatabaseBackup).toBe(true)
    })

    it('has correct initial loading and error states', () => {
      expect(store.loading).toBe(false)
      expect(store.error).toBe(null)
    })
  })

  describe('Basic Settings Actions', () => {
    it('fetches basic settings successfully', async () => {
      const mockData = {
        data: {
          data: {
            systemName: '测试系统',
            contactPhone: '13800138000',
            contactEmail: 'test@example.com'
          }
        }
      }

      vi.mocked(api.get).mockResolvedValue(mockData)

      await store.fetchBasicSettings()

      expect(api.get).toHaveBeenCalledWith('/v1/admin/settings/basic')
      expect(store.basicSettings.systemName).toBe('测试系统')
      expect(store.basicSettings.contactPhone).toBe('13800138000')
      expect(store.basicSettings.contactEmail).toBe('test@example.com')
      expect(store.loading).toBe(false)
      expect(store.error).toBe(null)
    })

    it('handles basic settings fetch error', async () => {
      const error = {
        response: {
          data: {
            message: '获取基础设置失败'
          }
        }
      }

      vi.mocked(api.get).mockRejectedValue(error)

      await expect(store.fetchBasicSettings()).rejects.toThrow()
      expect(store.loading).toBe(false)
      expect(store.error).toBe('获取基础设置失败')
    })

    it('updates basic settings successfully', async () => {
      const updateData = {
        systemName: '更新后的系统名称'
      }

      const mockResponse = {
        data: {
          data: updateData
        }
      }

      vi.mocked(api.put).mockResolvedValue(mockResponse)

      await store.updateBasicSettings(updateData)

      expect(api.put).toHaveBeenCalledWith('/v1/admin/settings/basic', updateData)
      expect(store.basicSettings.systemName).toBe('更新后的系统名称')
    })
  })

  describe('Business Rules Actions', () => {
    it('fetches business rules successfully', async () => {
      const mockData = {
        data: {
          data: {
            minBookingDays: 2,
            maxBookingDays: 60,
            enableFreeCancel: false
          }
        }
      }

      vi.mocked(api.get).mockResolvedValue(mockData)

      await store.fetchBusinessRules()

      expect(api.get).toHaveBeenCalledWith('/v1/admin/settings/business-rules')
      expect(store.businessRules.minBookingDays).toBe(2)
      expect(store.businessRules.maxBookingDays).toBe(60)
      expect(store.businessRules.enableFreeCancel).toBe(false)
    })

    it('updates business rules successfully', async () => {
      const updateData = {
        minBookingDays: 3,
        cancelFeePercentage: 10
      }

      const mockResponse = {
        data: {
          data: updateData
        }
      }

      vi.mocked(api.put).mockResolvedValue(mockResponse)

      await store.updateBusinessRules(updateData)

      expect(api.put).toHaveBeenCalledWith('/v1/admin/settings/business-rules', updateData)
      expect(store.businessRules.minBookingDays).toBe(3)
    })
  })

  describe('Backup Settings Actions', () => {
    it('fetches backup settings successfully', async () => {
      const mockData = {
        data: {
          data: {
            enableAutoBackup: true,
            backupIntervalDays: 7,
            backupRetentionDays: 30
          }
        }
      }

      vi.mocked(api.get).mockResolvedValue(mockData)

      await store.fetchBackupSettings()

      expect(api.get).toHaveBeenCalledWith('/v1/admin/settings/backup')
      expect(store.backupSettings.enableAutoBackup).toBe(true)
      expect(store.backupSettings.backupIntervalDays).toBe(7)
    })

    it('executes backup successfully', async () => {
      const mockResponse = {
        data: {
          data: '备份执行成功'
        }
      }

      vi.mocked(api.post).mockResolvedValue(mockResponse)

      await store.executeBackup()

      expect(api.post).toHaveBeenCalledWith('/v1/admin/settings/backup/execute')
      expect(store.loading).toBe(false)
    })

    it('handles backup execution error', async () => {
      const error = {
        response: {
          data: {
            message: '备份执行失败'
          }
        }
      }

      vi.mocked(api.post).mockRejectedValue(error)

      await expect(store.executeBackup()).rejects.toThrow()
      expect(store.error).toBe('备份执行失败')
    })
  })

  describe('Other Settings Actions', () => {
    it('fetches notification settings', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { data: {} } })

      await store.fetchNotificationSettings()

      expect(api.get).toHaveBeenCalledWith('/v1/admin/settings/notifications')
    })

    it('fetches security settings', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: { data: {} } })

      await store.fetchSecuritySettings()

      expect(api.get).toHaveBeenCalledWith('/v1/admin/settings/security')
    })
  })

  describe('Loading State Management', () => {
    it('sets loading to true during API calls', async () => {
      // Create a promise that resolves slowly
      let resolvePromise: (value: any) => void
      const slowPromise = new Promise(resolve => {
        resolvePromise = resolve
      })

      vi.mocked(api.get).mockReturnValue(slowPromise)

      // Start the API call
      const fetchPromise = store.fetchBasicSettings()

      // Loading should be true immediately
      expect(store.loading).toBe(true)

      // Resolve the promise
      resolvePromise!({ data: { data: {} } })
      await fetchPromise

      // Loading should be false after completion
      expect(store.loading).toBe(false)
    })
  })

  describe('Error State Management', () => {
    it('clears error on successful API call', async () => {
      // Set initial error state
      store.error = 'Previous error'

      vi.mocked(api.get).mockResolvedValue({ data: { data: {} } })

      await store.fetchBasicSettings()

      expect(store.error).toBe(null)
    })

    it('sets error on API failure', async () => {
      vi.mocked(api.get).mockRejectedValue({
        response: { data: { message: 'API Error' } }
      })

      try {
        await store.fetchBasicSettings()
      } catch (error) {
        // Expected to throw
      }

      expect(store.error).toBe('API Error')
    })
  })
})