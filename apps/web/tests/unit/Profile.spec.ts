import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import Profile from '@/pages/Profile.vue'
import { useAuthStore } from '@/stores/auth'
import { profileService } from '@/services/profileService'
import type { User, UpdateProfileRequest, ChangePasswordRequest } from '@/types/user'

// Mock dependencies
vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn()
}))

vi.mock('@/services/profileService', () => ({
  profileService: {
    getCurrentUser: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn()
  }
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn()
  }
}))

describe('Profile.vue', () => {
  let mockAuthStore: any
  let mockProfileService: any

  beforeEach(() => {
    vi.clearAllMocks()

    // Setup mock auth store
    mockAuthStore = {
      user: {
        id: 1,
        username: 'testuser',
        email: 'test@example.com',
        phone: '13800138000'
      },
      token: 'test-token',
      setUser: vi.fn()
    }
    ;(useAuthStore as any).mockReturnValue(mockAuthStore)

    // Setup mock profile service
    mockProfileService = profileService
  })

  it('renders correctly', () => {
    const wrapper = mount(Profile, {
      global: {
        stubs: {
          'el-card': true,
          'el-tabs': true,
          'el-tab-pane': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-button': true,
          'el-upload': true,
          'el-avatar': true,
          'el-select': true,
          'el-option': true,
          'el-date-picker': true,
          'el-text': true
        }
      }
    })

    expect(wrapper.find('.profile-page').exists()).toBe(true)
    expect(wrapper.text()).toContain('个人中心')
  })

  it('loads user profile on mount', async () => {
    const mockUser: User = {
      id: 1,
      username: 'testuser',
      email: 'test@example.com',
      phone: '13800138000',
      role: 'USER',
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      nickname: '测试用户',
      realName: '张三',
      gender: 'MALE',
      birthDate: '1990-01-01',
      avatar: 'http://example.com/avatar.jpg'
    }

    mockProfileService.getCurrentUser.mockResolvedValue(mockUser)

    mount(Profile, {
      global: {
        stubs: {
          'el-card': true,
          'el-tabs': true,
          'el-tab-pane': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-button': true,
          'el-upload': true,
          'el-avatar': true,
          'el-select': true,
          'el-option': true,
          'el-date-picker': true,
          'el-text': true
        }
      }
    })

    // Wait for nextTick to allow async operations
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(mockProfileService.getCurrentUser).toHaveBeenCalled()
    expect(mockAuthStore.setUser).toHaveBeenCalledWith(mockUser)
  })

  it('handles profile update successfully', async () => {
    const mockUpdatedUser: User = {
      id: 1,
      username: 'testuser',
      email: 'test@example.com',
      phone: '13800138000',
      role: 'USER',
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      nickname: '新昵称'
    }

    mockProfileService.updateProfile.mockResolvedValue(mockUpdatedUser)

    const wrapper = mount(Profile, {
      global: {
        stubs: {
          'el-card': true,
          'el-tabs': true,
          'el-tab-pane': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-button': true,
          'el-upload': true,
          'el-avatar': true,
          'el-select': true,
          'el-option': true,
          'el-date-picker': true,
          'el-text': true
        }
      }
    })

    // Get component instance and access its methods
    const vm: any = wrapper.vm

    // Mock form validation
    vm.profileFormRef = {
      validate: vi.fn().mockResolvedValue(true)
    }

    // Set form data
    vm.profileForm.nickname = '新昵称'

    // Call updateProfile method
    await vm.updateProfile()

    expect(mockProfileService.updateProfile).toHaveBeenCalledWith({
      nickname: '新昵称'
    })
    expect(mockAuthStore.setUser).toHaveBeenCalledWith(mockUpdatedUser)
    expect(ElMessage.success).toHaveBeenCalledWith('基本信息更新成功')
  })

  it('handles password change successfully', async () => {
    mockProfileService.changePassword.mockResolvedValue(undefined)

    const wrapper = mount(Profile, {
      global: {
        stubs: {
          'el-card': true,
          'el-tabs': true,
          'el-tab-pane': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-button': true,
          'el-upload': true,
          'el-avatar': true,
          'el-select': true,
          'el-option': true,
          'el-date-picker': true,
          'el-text': true
        }
      }
    })

    const vm: any = wrapper.vm

    // Mock form validation
    vm.passwordFormRef = {
      validate: vi.fn().mockResolvedValue(true),
      resetFields: vi.fn()
    }

    // Set password form data
    vm.passwordForm.currentPassword = 'oldPassword'
    vm.passwordForm.newPassword = 'newPassword'
    vm.passwordForm.confirmPassword = 'newPassword'

    // Call changePassword method
    await vm.changePassword()

    expect(mockProfileService.changePassword).toHaveBeenCalledWith({
      currentPassword: 'oldPassword',
      newPassword: 'newPassword',
      confirmPassword: 'newPassword'
    })
    expect(ElMessage.success).toHaveBeenCalledWith('密码修改成功')
  })

  it('validates password confirmation', async () => {
    const wrapper = mount(Profile, {
      global: {
        stubs: {
          'el-card': true,
          'el-tabs': true,
          'el-tab-pane': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-button': true,
          'el-upload': true,
          'el-avatar': true,
          'el-select': true,
          'el-option': true,
          'el-date-picker': true,
          'el-text': true
        }
      }
    })

    const vm: any = wrapper.vm

    // Mock form validation
    vm.passwordFormRef = {
      validate: vi.fn().mockResolvedValue(true)
    }

    // Set mismatched passwords
    vm.passwordForm.currentPassword = 'oldPassword'
    vm.passwordForm.newPassword = 'newPassword'
    vm.passwordForm.confirmPassword = 'differentPassword'

    // Call changePassword method
    await vm.changePassword()

    expect(ElMessage.error).toHaveBeenCalledWith('两次输入的密码不一致')
  })

  it('validates avatar upload before upload', async () => {
    const wrapper = mount(Profile, {
      global: {
        stubs: {
          'el-card': true,
          'el-tabs': true,
          'el-tab-pane': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-button': true,
          'el-upload': true,
          'el-avatar': true,
          'el-select': true,
          'el-option': true,
          'el-date-picker': true,
          'el-text': true
        }
      }
    })

    const vm: any = wrapper.vm

    // Test invalid file type
    const invalidFile = new File(['content'], 'test.txt', { type: 'text/plain' })
    const result1 = vm.beforeAvatarUpload(invalidFile)
    expect(result1).toBe(false)
    expect(ElMessage.error).toHaveBeenCalledWith('头像只能是 JPG、PNG 格式!')

    // Test oversized file
    const largeContent = new Uint8Array(6 * 1024 * 1024) // 6MB
    const largeFile = new File([largeContent], 'large.jpg', { type: 'image/jpeg' })
    const result2 = vm.beforeAvatarUpload(largeFile)
    expect(result2).toBe(false)
    expect(ElMessage.error).toHaveBeenCalledWith('头像大小不能超过 5MB!')

    // Test valid file
    const validFile = new File(['content'], 'valid.jpg', { type: 'image/jpeg' })
    const result3 = vm.beforeAvatarUpload(validFile)
    expect(result3).toBe(true)
  })
})