import { apiClient } from '@/utils/apiClient'
import { client } from '@/api/client'
import type {
  User,
  UpdateProfileRequest,
  ChangePasswordRequest,
  ApiResponse,
  UploadAvatarResponse
} from '@/types/user'

export const profileService = {
  // 获取当前用户信息
  async getCurrentUser(): Promise<User> {
    const response = await client.get<User>('/users/me')
    if (response.success && response.data) {
      return response.data
    }
    throw new Error(response.message || '获取用户信息失败')
  },

  // 更新用户基本信息
  async updateProfile(data: UpdateProfileRequest): Promise<User> {
    const response = await client.put<User>('/users/me', data)
    if (response.success && response.data) {
      return response.data
    }
    throw new Error(response.message || '更新用户信息失败')
  },

  // 修改密码
  async changePassword(data: ChangePasswordRequest): Promise<void> {
    const response = await client.put<null>('/users/me/password', data)
    if (!response.success) {
      throw new Error(response.message || '修改密码失败')
    }
  },

  // 上传头像
  async uploadAvatar(file: File): Promise<string> {
    const formData = new FormData()
    formData.append('file', file)

    // 使用原始 apiClient 处理文件上传
    const response = await apiClient.post<any>('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.data.success && response.data.data?.url) {
      return response.data.data.url
    }
    throw new Error(response.data.message || '头像上传失败')
  }
}