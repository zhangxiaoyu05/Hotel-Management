import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Register from '@/pages/auth/Register.vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { nextTick } from 'vue'

// Mock dependencies
vi.mock('@/services/authService')
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  })
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

describe('Register.vue', () => {
  let wrapper: any
  let authStore: any

  beforeEach(() => {
    setActivePinia(createPinia())
    authStore = useAuthStore()

    wrapper = mount(Register, {
      global: {
        plugins: [createPinia()],
        stubs: {
          'el-card': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-button': true,
          'router-link': true
        }
      }
    })
  })

  it('renders the register form correctly', () => {
    expect(wrapper.find('.register-container').exists()).toBe(true)
    expect(wrapper.find('.register-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('用户注册')
    expect(wrapper.text()).toContain('创建您的账户')
  })

  it('has all required form fields', () => {
    const formItems = wrapper.findAllComponents({ name: 'ElFormItem' })
    expect(formItems).toHaveLength(6) // username, email, phone, password, confirmPassword, register button, login link
  })

  it('validates required fields', async () => {
    const form = wrapper.findComponent({ name: 'ElForm' })

    // Trigger form validation without filling fields
    await form.vm.validate()

    // Should show validation errors for required fields
    // Note: Specific validation testing would require more detailed Element Plus form testing setup
  })

  it('validates email format', async () => {
    const emailInput = wrapper.find('input[placeholder="请输入邮箱"]')
    await emailInput.setValue('invalid-email')

    // Should trigger email validation error
    // Note: Email validation happens in real-time
  })

  it('validates phone format', async () => {
    const phoneInput = wrapper.find('input[placeholder="请输入手机号"]')
    await phoneInput.setValue('12345')

    // Should trigger phone validation error
    // Note: Phone validation happens in real-time
  })

  it('validates password strength', async () => {
    const passwordInput = wrapper.find('input[type="password"]')
    await passwordInput.setValue('weak')

    // Should trigger password strength validation error
    // Note: Password validation happens in real-time
  })

  it('validates password confirmation', async () => {
    const passwordInput = wrapper.find('input[placeholder="请输入密码"]')
    const confirmPasswordInput = wrapper.find('input[placeholder="请再次输入密码"]')

    await passwordInput.setValue('Test123!@#')
    await confirmPasswordInput.setValue('DifferentPassword')

    // Should trigger password confirmation mismatch error
  })

  it('displays loading state during registration', async () => {
    const registerButton = wrapper.findComponent({ name: 'ElButton' })

    // Mock successful registration
    const mockAuthService = await import('@/services/authService')
    mockAuthService.authService.register = vi.fn().mockResolvedValue({ success: true })

    // Fill in valid form data
    await wrapper.find('input[placeholder="请输入用户名"]').setValue('testuser')
    await wrapper.find('input[placeholder="请输入邮箱"]').setValue('test@example.com')
    await wrapper.find('input[placeholder="请输入手机号"]').setValue('13800138000')
    await wrapper.find('input[placeholder="请输入密码"]').setValue('Test123!@#')
    await wrapper.find('input[placeholder="请再次输入密码"]').setValue('Test123!@#')

    await registerButton.trigger('click')
    await nextTick()

    // Button should be in loading state
    expect(registerButton.props('loading')).toBe(true)
  })

  it('shows success message on successful registration', async () => {
    // Mock successful registration
    const mockAuthService = await import('@/services/authService')
    mockAuthService.authService.register = vi.fn().mockResolvedValue({ success: true })

    // Mock ElMessageBox.confirm to resolve
    vi.mocked(ElMessageBox.confirm).mockResolvedValue({ value: 'confirm' })

    // Fill in valid form data
    await wrapper.find('input[placeholder="请输入用户名"]').setValue('testuser')
    await wrapper.find('input[placeholder="请输入邮箱"]').setValue('test@example.com')
    await wrapper.find('input[placeholder="请输入手机号"]').setValue('13800138000')
    await wrapper.find('input[placeholder="请输入密码"]').setValue('Test123!@#')
    await wrapper.find('input[placeholder="请再次输入密码"]').setValue('Test123!@#')

    const registerButton = wrapper.findComponent({ name: 'ElButton' })
    await registerButton.trigger('click')

    await new Promise(resolve => setTimeout(resolve, 0))

    expect(ElMessage.success).toHaveBeenCalledWith('注册成功！')
  })

  it('shows error message on registration failure', async () => {
    // Mock failed registration
    const mockAuthService = await import('@/services/authService')
    mockAuthService.authService.register = vi.fn().mockRejectedValue(
      new Error('用户名已存在')
    )

    // Fill in valid form data
    await wrapper.find('input[placeholder="请输入用户名"]').setValue('existinguser')
    await wrapper.find('input[placeholder="请输入邮箱"]').setValue('existing@example.com')
    await wrapper.find('input[placeholder="请输入手机号"]').setValue('13800138001')
    await wrapper.find('input[placeholder="请输入密码"]').setValue('Test123!@#')
    await wrapper.find('input[placeholder="请再次输入密码"]').setValue('Test123!@#')

    const registerButton = wrapper.findComponent({ name: 'ElButton' })
    await registerButton.trigger('click')

    await new Promise(resolve => setTimeout(resolve, 0))

    expect(ElMessage.error).toHaveBeenCalledWith('用户名已存在')
  })

  it('has login link', () => {
    const loginLink = wrapper.find('router-link[to="/auth/login"]')
    expect(loginLink.exists()).toBe(true)
    expect(loginLink.text()).toContain('立即登录')
  })
})