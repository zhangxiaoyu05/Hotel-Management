import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/api'
import type {
  OperationLog,
  LoginLog,
  ErrorLog,
  LogSearchParams,
  LogExportRequest,
  ApiResponse,
  PageResponse
} from '@/types/log'

export const useLogsStore = defineStore('logs', () => {
  // 获取操作日志
  const getOperationLogs = async (params: LogSearchParams & { page: number; size: number }) => {
    const response = await api.get<ApiResponse<PageResponse<OperationLog>>>('/v1/admin/logs/operation', {
      params
    })
    return response.data
  }

  // 获取登录日志
  const getLoginLogs = async (params: LogSearchParams & { page: number; size: number }) => {
    const response = await api.get<ApiResponse<PageResponse<LoginLog>>>('/v1/admin/logs/login', {
      params
    })
    return response.data
  }

  // 获取错误日志
  const getErrorLogs = async (params: LogSearchParams & { page: number; size: number }) => {
    const response = await api.get<ApiResponse<PageResponse<ErrorLog>>>('/v1/admin/logs/error', {
      params
    })
    return response.data
  }

  // 导出日志
  const exportLogs = async (request: LogExportRequest) => {
    const response = await api.post('/v1/admin/logs/export', request, {
      responseType: 'blob'
    })

    // 处理文件下载
    const blob = new Blob([response.data], {
      type: request.exportFormat === 'csv' ? 'text/csv' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })

    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    // 生成文件名
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[:-]/g, '')
    const logTypeName = {
      operation: '操作日志',
      login: '登录日志',
      error: '错误日志'
    }[request.logType] || '日志'

    const extension = request.exportFormat === 'csv' ? '.csv' : '.xlsx'
    link.download = `${logTypeName}_${timestamp}${extension}`

    // 触发下载
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    return response
  }

  // 获取日志统计信息
  const getLogStatistics = async () => {
    const response = await api.get('/v1/admin/logs/statistics')
    return response.data
  }

  return {
    getOperationLogs,
    getLoginLogs,
    getErrorLogs,
    exportLogs,
    getLogStatistics
  }
})