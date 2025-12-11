import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import Login from '@/pages/Login.vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

// Mock dependencies
vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn()
  }
}))

describe('Login.vue', () => {
  let mockAuthStore: any
  let mockRouter: any
  let wrapper: any

  beforeEach(() => {
    // Reset all mocks
    vi.clearAllMocks()

    // Setup mock auth store
    mockAuthStore = {
      login: vi.fn(),
      isAuthenticated: false,
      loading: false,
      error: null
    }
    ;(useAuthStore as any).mockReturnValue(mockAuthStore)

    // Setup mock router
    mockRouter = {
      push: vi.fn()
    }
    ;(useRouter as any).mockReturnValue(mockRouter)

    wrapper = mount(Login, {
      global: {
        stubs: {
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-checkbox': true,
          'el-button': true,
          'el-link': true,
          'el-icon': true,
          'router-link': true
        }
      }
    })
  })

  it('renders properly', () => {
    expect(wrapper.find('.login-container').exists()).toBe(true)
    expect(wrapper.find('.login-form').exists()).toBe(true)
    expect(wrapper.find('.login-title').text()).toBe('欢迎登录')
  })

  it('has initial form data', () => {
    const vm = wrapper.vm as any
    expect(vm.loginForm.identifier).toBe('')
    expect(vm.loginForm.password).toBe('')
    expect(vm.loginForm.rememberMe).toBe(false)
  })

  it('shows loading state when auth store is loading', async () => {
    mockAuthStore.loading = true
    await wrapper.vm.$nextTick()

    const submitButton = wrapper.find('.login-button')
    expect(submitButton.attributes('loading')).toBeDefined()
  })

  it('handles form submit successfully with username', async () => {
    const vm = wrapper.vm as any

    // Set form data
    vm.loginForm.identifier = 'testuser'
    vm.loginForm.password = 'password123'
    vm.loginForm.rememberMe = true

    // Mock successful login
    mockAuthStore.login.mockResolvedValue(undefined)
    mockAuthStore.isAuthenticated = true

    // Trigger form submit
    await vm.handleLogin()

    // Verify login was called with correct data
    expect(mockAuthStore.login).toHaveBeenCalledWith({
      identifier: 'testuser',
      password: 'password123',
      rememberMe: true
    })

    // Verify redirect to dashboard
    expect(mockRouter.push).toHaveBeenCalledWith('/dashboard')
    expect(ElMessage.success).toHaveBeenCalledWith('登录成功')
  })

  it('handles form submit successfully with email', async () => {
    const vm = wrapper.vm as any

    // Set form data with email
    vm.loginForm.identifier = 'test@example.com'
    vm.loginForm.password = 'password123'
    vm.loginForm.rememberMe = false

    // Mock successful login
    mockAuthStore.login.mockResolvedValue(undefined)
    mockAuthStore.isAuthenticated = true

    // Trigger form submit
    await vm.handleLogin()

    // Verify login was called with email
    expect(mockAuthStore.login).toHaveBeenCalledWith({
      identifier: 'test@example.com',
      password: 'password123',
      rememberMe: false
    })

    // Verify redirect
    expect(mockRouter.push).toHaveBeenCalledWith('/dashboard')
  })

  it('handles form submit successfully with phone', async () => {
    const vm = wrapper.vm as any

    // Set form data with phone
    vm.loginForm.identifier = '13800138000'
    vm.loginForm.password = 'password123'
    vm.loginForm.rememberMe = false

    // Mock successful login
    mockAuthStore.login.mockResolvedValue(undefined)
    mockAuthStore.isAuthenticated = true

    // Trigger form submit
    await vm.handleLogin()

    // Verify login was called with phone
    expect(mockAuthStore.login).toHaveBeenCalledWith({
      identifier: '13800138000',
      password: 'password123',
      rememberMe: false
    })

    // Verify redirect
    expect(mockRouter.push).toHaveBeenCalledWith('/dashboard')
  })

  it('handles login failure with error message', async () => {
    const vm = wrapper.vm as any

    // Set form data
    vm.loginForm.identifier = 'testuser'
    vm.loginForm.password = 'wrongpassword'

    // Mock login failure
    const errorMessage = '用户名或密码错误'
    mockAuthStore.login.mockRejectedValue(new Error(errorMessage))

    // Trigger form submit
    await vm.handleLogin()

    // Verify error message is shown
    expect(ElMessage.error).toHaveBeenCalledWith(errorMessage)
    expect(mockRouter.push).not.toHaveBeenCalled()
  })

  it('handles login failure with generic error', async () => {
    const vm = wrapper.vm as any

    // Set form data
    vm.loginForm.identifier = 'testuser'
    vm.loginForm.password = 'password123'

    // Mock login failure without specific error message
    mockAuthStore.login.mockRejectedValue(new Error('Network error'))
    mockAuthStore.error = null

    // Trigger form submit
    await vm.handleLogin()

    // Verify generic error message is shown
    expect(ElMessage.error).toHaveBeenCalledWith('登录失败，请检查网络连接')
    expect(mockRouter.push).not.toHaveBeenCalled()
  })

  it('uses auth store error message if available', async () => {
    const vm = wrapper.vm as any

    // Set form data
    vm.loginForm.identifier = 'testuser'
    vm.loginForm.password = 'password123'

    // Set auth store error
    mockAuthStore.error = '账户已被禁用'
    mockAuthStore.login.mockRejectedValue(new Error('Any error'))

    // Trigger form submit
    await vm.handleLogin()

    // Verify auth store error message is used
    expect(ElMessage.error).toHaveBeenCalledWith('账户已被禁用')
  })

  it('validates form before submission', async () => {
    const vm = wrapper.vm as any

    // Leave form empty
    vm.loginForm.identifier = ''
    vm.loginForm.password = ''

    // Mock form validation
    const mockValidate = vi.fn().mockResolvedValue(false)
    vm.loginFormRef = {
      validate: mockValidate
    }

    // Trigger form submit
    await vm.handleLogin()

    // Validate that login is not called if form validation fails
    expect(mockValidate).toHaveBeenCalled()
    expect(mockAuthStore.login).not.toHaveBeenCalled()
  })

  it('does not submit if already loading', async () => {
    mockAuthStore.loading = true
    const vm = wrapper.vm as any

    // Set form data
    vm.loginForm.identifier = 'testuser'
    vm.loginForm.password = 'password123'

    // Trigger form submit
    await vm.handleLogin()

    // Verify login is not called when loading
    expect(mockAuthStore.login).not.toHaveBeenCalled()
  })

  it('updates input fields correctly', async () => {
    const vm = wrapper.vm as any

    // Update identifier field
    await vm.handleIdentifierInput('test@example.com')
    expect(vm.loginForm.identifier).toBe('test@example.com')

    // Update password field
    await vm.handlePasswordInput('password123')
    expect(vm.loginForm.password).toBe('password123')

    // Update remember me checkbox
    await vm.handleRememberMeChange(true)
    expect(vm.loginForm.rememberMe).toBe(true)
  })

  it('handles enter key in password field', async () => {
    const vm = wrapper.vm as any

    // Set form data
    vm.loginForm.identifier = 'testuser'
    vm.loginForm.password = 'password123'

    // Mock successful login
    mockAuthStore.login.mockResolvedValue(undefined)
    mockAuthStore.isAuthenticated = true

    // Trigger enter key event
    await vm.handlePasswordEnter({
      key: 'Enter',
      preventDefault: vi.fn(),
      stopPropagation: vi.fn()
    })

    // Verify login was called
    expect(mockAuthStore.login).toHaveBeenCalled()
  })

  it('does not handle enter key for other keys', async () => {
    const vm = wrapper.vm as any

    // Set form data
    vm.loginForm.identifier = 'testuser'
    vm.loginForm.password = 'password123'

    // Trigger non-enter key event
    await vm.handlePasswordEnter({
      key: 'Tab',
      preventDefault: vi.fn(),
      stopPropagation: vi.fn()
    })

    // Verify login was not called
    expect(mockAuthStore.login).not.toHaveBeenCalled()
  })

  it('shows correct input placeholder based on identifier type', async () => {
    const vm = wrapper.vm as any

    // Default placeholder
    expect(vm.identifierPlaceholder).toBe('用户名/邮箱/手机号')

    // Email placeholder
    await vm.handleIdentifierInput('test@')
    expect(vm.identifierPlaceholder).toBe('请输入邮箱')

    // Phone placeholder
    await vm.handleIdentifierInput('138')
    expect(vm.identifierPlaceholder).toBe('请输入手机号')

    // Username placeholder (back to default)
    await vm.handleIdentifierInput('testuser')
    expect(vm.identifierPlaceholder).toBe('请输入用户名')
  })

  it('redirects to dashboard if already authenticated', async () => {
    mockAuthStore.isAuthenticated = true
    mockAuthStore.loading = false

    wrapper = mount(Login, {
      global: {
        stubs: {
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-checkbox': true,
          'el-button': true,
          'el-link': true,
          'el-icon': true,
          'router-link': true
        }
      }
    })

    await wrapper.vm.$nextTick()

    // Should redirect immediately
    expect(mockRouter.push).toHaveBeenCalledWith('/dashboard')
  })

  it('clears error when form is submitted', async () => {
    const vm = wrapper.vm as any

    // Set initial error
    mockAuthStore.error = 'Previous error'

    // Set form data
    vm.loginForm.identifier = 'testuser'
    vm.loginForm.password = 'password123'

    // Mock successful login
    mockAuthStore.login.mockResolvedValue(undefined)
    mockAuthStore.isAuthenticated = true

    // Trigger form submit
    await vm.handleLogin()

    // Verify error was cleared
    expect(mockAuthStore.clearError).toHaveBeenCalled()
  })

  it('handles forgot password link click', async () => {
    const vm = wrapper.vm as any

    await vm.handleForgotPassword()

    expect(mockRouter.push).toHaveBeenCalledWith('/auth/forgot-password')
  })
})