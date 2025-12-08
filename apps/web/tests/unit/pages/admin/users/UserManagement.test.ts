import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserManagement from '@/pages/admin/users/UserManagement.vue'
import { authStore } from '@/stores/auth'
import { adminApi } from '@/api/admin'
import type { User } from '@/types/user'

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn()
    },
    ElMessageBox: {
      confirm: vi.fn()
    }
  }
})

// Mock authStore
vi.mock('@/stores/auth', () => ({
  authStore: {
    user: {
      id: 1,
      username: 'admin',
      role: 'ADMIN'
    }
  }
}))

// Mock adminApi
vi.mock('@/api/admin', () => ({
  adminApi: {
    getUsers: vi.fn(),
    updateUserRole: vi.fn(),
    updateUserStatus: vi.fn()
  }
}))

describe('UserManagement', () => {
  let wrapper: any
  const mockUsers: User[] = [
    {
      id: 1,
      username: 'admin',
      email: 'admin@example.com',
      phone: '1234567890',
      role: 'ADMIN',
      status: 'ACTIVE',
      createdAt: '2023-01-01T00:00:00Z',
      updatedAt: '2023-01-01T00:00:00Z'
    },
    {
      id: 2,
      username: 'user',
      email: 'user@example.com',
      phone: '0987654321',
      role: 'USER',
      status: 'ACTIVE',
      createdAt: '2023-01-01T00:00:00Z',
      updatedAt: '2023-01-01T00:00:00Z'
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()

    // Mock adminApi响应
    vi.mocked(adminApi.getUsers).mockResolvedValue({
      success: true,
      data: {
        records: mockUsers,
        total: 2,
        size: 20,
        current: 0,
        pages: 1
      }
    })

    vi.mocked(adminApi.updateUserRole).mockResolvedValue({
      success: true,
      data: mockUsers[1]
    })

    vi.mocked(adminApi.updateUserStatus).mockResolvedValue({
      success: true,
      data: mockUsers[1]
    })

    wrapper = mount(UserManagement, {
      global: {
        components: {
          'el-table': true,
          'el-table-column': true,
          'el-button': true,
          'el-input': true,
          'el-select': true,
          'el-option': true,
          'el-switch': true,
          'el-pagination': true,
          'el-dialog': true,
          'el-form': true,
          'el-form-item': true,
          'el-row': true,
          'el-col': true,
          'el-icon': true
        }
      }
    })
  })

  it('应该正确渲染用户管理页面', () => {
    expect(wrapper.find('.page-header h1').text()).toBe('用户管理')
    expect(wrapper.find('.page-header p').text()).toBe('管理系统中的所有用户账户')
  })

  it('应该包含搜索和筛选表单', () => {
    expect(wrapper.find('.filter-section').exists()).toBe(true)
  })

  it('应该包含用户列表表格', () => {
    expect(wrapper.find('.table-section').exists()).toBe(true)
  })

  it('应该包含分页组件', () => {
    expect(wrapper.find('.pagination-section').exists()).toBe(true)
  })

  it('应该在组件挂载时获取用户列表', async () => {
    await wrapper.vm.$nextTick()

    expect(adminApi.getUsers).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      username: undefined,
      role: undefined,
      status: undefined
    })
  })

  it('应该正确处理搜索', async () => {
    const searchForm = wrapper.vm.searchForm
    searchForm.username = 'test'
    searchForm.role = 'USER'

    await wrapper.vm.handleSearch()

    expect(adminApi.getUsers).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      username: 'test',
      role: 'USER',
      status: undefined
    })
  })

  it('应该正确处理角色编辑对话框', async () => {
    const user = mockUsers[1]

    await wrapper.vm.handleEditRole(user)

    expect(wrapper.vm.roleDialog.visible).toBe(true)
    expect(wrapper.vm.roleDialog.form.id).toBe(user.id)
    expect(wrapper.vm.roleDialog.form.username).toBe(user.username)
    expect(wrapper.vm.roleDialog.form.role).toBe(user.role)
  })

  it('应该正确保存用户角色', async () => {
    const user = mockUsers[1]

    // 打开编辑对话框
    await wrapper.vm.handleEditRole(user)

    // 修改角色
    wrapper.vm.roleDialog.form.role = 'ADMIN'

    // 保存角色
    await wrapper.vm.handleSaveRole()

    expect(adminApi.updateUserRole).toHaveBeenCalledWith(user.id, {
      role: 'ADMIN'
    })
    expect(wrapper.vm.roleDialog.visible).toBe(false)
  })

  it('应该正确处理用户状态变更', async () => {
    const user = mockUsers[1]

    // Mock确认对话框
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm')

    await wrapper.vm.handleStatusChange(user, 'INACTIVE')

    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(adminApi.updateUserStatus).toHaveBeenCalledWith(user.id, {
      status: 'INACTIVE'
    })
  })

  it('应该在取消状态变更时不更新状态', async () => {
    const user = mockUsers[1]
    const originalStatus = user.status

    // Mock取消对话框
    vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

    await wrapper.vm.handleStatusChange(user, 'INACTIVE')

    expect(user.status).toBe(originalStatus)
    expect(adminApi.updateUserStatus).not.toHaveBeenCalled()
  })

  it('应该正确格式化日期', () => {
    const dateString = '2023-01-01T00:00:00Z'
    const formattedDate = wrapper.vm.formatDate(dateString)

    expect(formattedDate).toMatch(/\d{4}\/\d{2}\/\d{2}/) // 匹配日期格式
  })

  it('应该正确处理分页大小变更', async () => {
    await wrapper.vm.handleSizeChange(50)

    expect(wrapper.vm.pagination.size).toBe(50)
    expect(wrapper.vm.pagination.page).toBe(1)
  })

  it('应该正确处理页码变更', async () => {
    await wrapper.vm.handleCurrentChange(2)

    expect(wrapper.vm.pagination.page).toBe(2)
  })

  it('应该禁用当前用户的状态切换开关', () => {
    const currentUser = authStore.user
    const userIsCurrent = mockUsers.find(user => user.id === currentUser.id)

    if (userIsCurrent) {
      // 在实际组件中，这会通过模板中的disabled属性处理
      expect(wrapper.vm.userIsCurrent(currentUser.id)).toBe(true)
    }
  })
})