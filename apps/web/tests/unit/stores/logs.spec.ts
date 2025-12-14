import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useLogsStore } from '@/stores/logs'
import { logApi } from '@/api/log'
import type { LogSearchParams, OperationLog, LoginLog, ErrorLog } from '@/types/log'

/**
 * 日志状态管理单元测试
 * 测试日志数据的获取、管理和导出功能
 */

// Mock API
vi.mock('@/api/log', () => ({
  logApi: {
    getOperationLogs: vi.fn(),
    getLoginLogs: vi.fn(),
    getErrorLogs: vi.fn(),
    exportLogs: vi.fn(),
    getLogStatistics: vi.fn()
  }
}))

describe('Logs Store', () => {
  let logsStore: ReturnType<typeof useLogsStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    logsStore = useLogsStore()

    // 重置所有mock
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      expect(logsStore.operationLogs).toEqual([])
      expect(logsStore.loginLogs).toEqual([])
      expect(logsStore.errorLogs).toEqual([])
      expect(logsStore.currentOperationPage).toBe(1)
      expect(logsStore.currentLoginPage).toBe(1)
      expect(logsStore.currentErrorPage).toBe(1)
      expect(logsStore.operationTotal).toBe(0)
      expect(logsStore.loginTotal).toBe(0)
      expect(logsStore.errorTotal).toBe(0)
      expect(logsStore.loading).toBe(false)
      expect(logsStore.exporting).toBe(false)
      expect(logsStore.statistics).toBeNull()
    })
  })

  describe('操作日志管理', () => {
    it('应该成功获取操作日志', async () => {
      // Given
      const mockParams: LogSearchParams = {
        page: 1,
        size: 10,
        username: 'testuser',
        operation: 'CREATE_USER'
      }

      const mockResponse = {
        success: true,
        data: {
          records: [
            {
              id: 1,
              userId: 1,
              username: 'testuser',
              operation: 'CREATE_USER',
              method: 'POST',
              params: '{"username":"test"}',
              time: 150,
              ip: '127.0.0.1',
              userAgent: 'Mozilla/5.0',
              status: 'SUCCESS',
              createTime: '2024-01-01 10:00:00'
            } as OperationLog
          ],
          total: 1,
          current: 1,
          size: 10
        }
      }

      vi.mocked(logApi.getOperationLogs).mockResolvedValue(mockResponse)

      // When
      await logsStore.fetchOperationLogs(mockParams)

      // Then
      expect(logsStore.loading).toBe(false)
      expect(logsStore.operationLogs).toHaveLength(1)
      expect(logsStore.operationLogs[0].username).toBe('testuser')
      expect(logsStore.operationLogs[0].operation).toBe('CREATE_USER')
      expect(logsStore.operationTotal).toBe(1)
      expect(logApi.getOperationLogs).toHaveBeenCalledWith(mockParams)
    })

    it('应该处理获取操作日志失败的情况', async () => {
      // Given
      const mockParams: LogSearchParams = { page: 1, size: 10 }
      const errorMessage = '网络错误'

      vi.mocked(logApi.getOperationLogs).mockRejectedValue(new Error(errorMessage))

      // When
      await logsStore.fetchOperationLogs(mockParams)

      // Then
      expect(logsStore.loading).toBe(false)
      expect(logsStore.operationLogs).toEqual([])
      expect(logsStore.error).toContain(errorMessage)
    })

    it('应该支持分页加载操作日志', async () => {
      // Given
      const firstPageParams: LogSearchParams = { page: 1, size: 5 }
      const secondPageParams: LogSearchParams = { page: 2, size: 5 }

      const firstPageResponse = {
        success: true,
        data: {
          records: Array.from({ length: 5 }, (_, i) => ({
            id: i + 1,
            username: `user${i}`,
            operation: 'CREATE_USER'
          })) as OperationLog[],
          total: 10,
          current: 1,
          size: 5
        }
      }

      const secondPageResponse = {
        success: true,
        data: {
          records: Array.from({ length: 5 }, (_, i) => ({
            id: i + 6,
            username: `user${i + 5}`,
            operation: 'CREATE_USER'
          })) as OperationLog[],
          total: 10,
          current: 2,
          size: 5
        }
      }

      vi.mocked(logApi.getOperationLogs)
        .mockResolvedValueOnce(firstPageResponse)
        .mockResolvedValueOnce(secondPageResponse)

      // When - 加载第一页
      await logsStore.fetchOperationLogs(firstPageParams)

      // Then
      expect(logsStore.currentOperationPage).toBe(1)
      expect(logsStore.operationLogs).toHaveLength(5)

      // When - 加载第二页
      await logsStore.fetchOperationLogs(secondPageParams)

      // Then
      expect(logsStore.currentOperationPage).toBe(2)
      expect(logsStore.operationLogs).toHaveLength(5)
      expect(logsStore.operationLogs[0].id).toBe(6)
    })
  })

  describe('登录日志管理', () => {
    it('应该成功获取登录日志', async () => {
      // Given
      const mockParams: LogSearchParams = {
        page: 1,
        size: 10,
        username: 'testuser',
        loginType: 'PASSWORD'
      }

      const mockResponse = {
        success: true,
        data: {
          records: [
            {
              id: 1,
              username: 'testuser',
              loginType: 'PASSWORD',
              ip: '127.0.0.1',
              location: '本地',
              browser: 'Chrome',
              os: 'Windows',
              status: 'SUCCESS',
              message: '登录成功',
              createTime: '2024-01-01 10:00:00'
            } as LoginLog
          ],
          total: 1,
          current: 1,
          size: 10
        }
      }

      vi.mocked(logApi.getLoginLogs).mockResolvedValue(mockResponse)

      // When
      await logsStore.fetchLoginLogs(mockParams)

      // Then
      expect(logsStore.loading).toBe(false)
      expect(logsStore.loginLogs).toHaveLength(1)
      expect(logsStore.loginLogs[0].username).toBe('testuser')
      expect(logsStore.loginLogs[0].loginType).toBe('PASSWORD')
      expect(logsStore.loginTotal).toBe(1)
      expect(logApi.getLoginLogs).toHaveBeenCalledWith(mockParams)
    })
  })

  describe('错误日志管理', () => {
    it('应该成功获取错误日志', async () => {
      // Given
      const mockParams: LogSearchParams = {
        page: 1,
        size: 10,
        level: 'ERROR',
        module: 'UserService'
      }

      const mockResponse = {
        success: true,
        data: {
          records: [
            {
              id: 1,
              exceptionType: 'NullPointerException',
              message: '空指针异常',
              stackTrace: 'java.lang.NullPointerException',
              module: 'UserService',
              level: 'ERROR',
              className: 'com.example.UserService',
              methodName: 'createUser',
              fileName: 'UserService.java',
              lineNumber: 123,
              url: '/api/users',
              params: '{"id":null}',
              ip: '127.0.0.1',
              userAgent: 'Mozilla/5.0',
              username: 'testuser',
              createTime: '2024-01-01 10:00:00'
            } as ErrorLog
          ],
          total: 1,
          current: 1,
          size: 10
        }
      }

      vi.mocked(logApi.getErrorLogs).mockResolvedValue(mockResponse)

      // When
      await logsStore.fetchErrorLogs(mockParams)

      // Then
      expect(logsStore.loading).toBe(false)
      expect(logsStore.errorLogs).toHaveLength(1)
      expect(logsStore.errorLogs[0].exceptionType).toBe('NullPointerException')
      expect(logsStore.errorLogs[0].level).toBe('ERROR')
      expect(logsStore.errorTotal).toBe(1)
      expect(logApi.getErrorLogs).toHaveBeenCalledWith(mockParams)
    })
  })

  describe('日志导出功能', () => {
    it('应该成功导出操作日志', async () => {
      // Given
      const exportParams = {
        logType: 'operation',
        exportFormat: 'csv',
        includeSensitiveInfo: false
      }

      const mockBlob = new Blob(['test,csv,data'], { type: 'text/csv' })
      vi.mocked(logApi.exportLogs).mockResolvedValue(mockBlob)

      // 模拟创建下载链接
      const mockCreateObjectURL = vi.fn(() => 'mock-url')
      const mockRevokeObjectURL = vi.fn()
      Object.defineProperty(URL, 'createObjectURL', { value: mockCreateObjectURL })
      Object.defineProperty(URL, 'revokeObjectURL', { value: mockRevokeObjectURL })

      // 模拟点击下载
      const mockLink = {
        click: vi.fn(),
        href: '',
        download: ''
      }
      const mockCreateElement = vi.fn(() => mockLink)
      Object.defineProperty(document, 'createElement', { value: mockCreateElement })
      Object.defineProperty(document, 'body', { value: { appendChild: vi.fn(), removeChild: vi.fn() } })

      // When
      await logsStore.exportLogs(exportParams)

      // Then
      expect(logsStore.exporting).toBe(false)
      expect(logApi.exportLogs).toHaveBeenCalledWith(exportParams)
      expect(mockCreateObjectURL).toHaveBeenCalledWith(mockBlob)
      expect(mockLink.click).toHaveBeenCalled()
      expect(mockRevokeObjectURL).toHaveBeenCalledWith('mock-url')
    })

    it('应该处理导出失败的情况', async () => {
      // Given
      const exportParams = {
        logType: 'operation',
        exportFormat: 'csv'
      }

      const errorMessage = '导出失败'
      vi.mocked(logApi.exportLogs).mockRejectedValue(new Error(errorMessage))

      // When
      await logsStore.exportLogs(exportParams)

      // Then
      expect(logsStore.exporting).toBe(false)
      expect(logsStore.error).toContain(errorMessage)
    })
  })

  describe('日志统计功能', () => {
    it('应该成功获取日志统计信息', async () => {
      // Given
      const mockStatistics = {
        operationLogCount: 100,
        loginLogCount: 200,
        errorLogCount: 50,
        trends: [
          { date: '2024-01-01', count: 10 },
          { date: '2024-01-02', count: 15 }
        ]
      }

      const mockResponse = {
        success: true,
        data: mockStatistics
      }

      vi.mocked(logApi.getLogStatistics).mockResolvedValue(mockResponse)

      // When
      await logsStore.fetchLogStatistics()

      // Then
      expect(logsStore.statistics).toEqual(mockStatistics)
      expect(logApi.getLogStatistics).toHaveBeenCalled()
    })
  })

  describe('状态管理', () => {
    it('应该正确重置状态', () => {
      // Given - 设置一些状态
      logsStore.operationLogs = [{ id: 1, username: 'test' } as OperationLog]
      logsStore.error = 'Some error'
      logsStore.loading = true

      // When
      logsStore.$reset()

      // Then
      expect(logsStore.operationLogs).toEqual([])
      expect(logsStore.loginLogs).toEqual([])
      expect(logsStore.errorLogs).toEqual([])
      expect(logsStore.error).toBeNull()
      expect(logsStore.loading).toBe(false)
      expect(logsStore.exporting).toBe(false)
      expect(logsStore.currentOperationPage).toBe(1)
      expect(logsStore.currentLoginPage).toBe(1)
      expect(logsStore.currentErrorPage).toBe(1)
    })

    it('应该正确清除错误信息', () => {
      // Given
      logsStore.error = 'Some error message'

      // When
      logsStore.clearError()

      // Then
      expect(logsStore.error).toBeNull()
    })
  })

  describe('并发请求处理', () => {
    it('应该避免重复请求', async () => {
      // Given
      logsStore.loading = true
      const mockParams: LogSearchParams = { page: 1, size: 10 }

      // When
      await logsStore.fetchOperationLogs(mockParams)

      // Then
      expect(logApi.getOperationLogs).not.toHaveBeenCalled()
    })

    it('应该正确处理多个并发请求', async () => {
      // Given
      const mockParams: LogSearchParams = { page: 1, size: 10 }

      const mockResponse = {
        success: true,
        data: {
          records: [],
          total: 0,
          current: 1,
          size: 10
        }
      }

      vi.mocked(logApi.getOperationLogs).mockResolvedValue(mockResponse)
      vi.mocked(logApi.getLoginLogs).mockResolvedValue(mockResponse)
      vi.mocked(logApi.getErrorLogs).mockResolvedValue(mockResponse)

      // When - 并发请求
      const promises = [
        logsStore.fetchOperationLogs(mockParams),
        logsStore.fetchLoginLogs(mockParams),
        logsStore.fetchErrorLogs(mockParams)
      ]

      await Promise.all(promises)

      // Then
      expect(logsStore.loading).toBe(false)
      expect(logApi.getOperationLogs).toHaveBeenCalledTimes(1)
      expect(logApi.getLoginLogs).toHaveBeenCalledTimes(1)
      expect(logApi.getErrorLogs).toHaveBeenCalledTimes(1)
    })
  })

  describe('错误边界处理', () => {
    it('应该处理API响应格式错误', async () => {
      // Given
      vi.mocked(logApi.getOperationLogs).mockResolvedValue({
        success: false,
        message: '服务器错误'
      } as any)

      // When
      await logsStore.fetchOperationLogs({ page: 1, size: 10 })

      // Then
      expect(logsStore.loading).toBe(false)
      expect(logsStore.operationLogs).toEqual([])
      expect(logsStore.error).toContain('服务器错误')
    })

    it('应该处理网络超时', async () => {
      // Given
      const timeoutError = new Error('Request timeout')
      timeoutError.name = 'TimeoutError'
      vi.mocked(logApi.getOperationLogs).mockRejectedValue(timeoutError)

      // When
      await logsStore.fetchOperationLogs({ page: 1, size: 10 })

      // Then
      expect(logsStore.loading).toBe(false)
      expect(logsStore.error).toContain('Request timeout')
    })
  })
})