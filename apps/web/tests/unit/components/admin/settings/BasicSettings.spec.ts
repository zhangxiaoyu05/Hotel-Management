import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import BasicSettings from '@/components/admin/settings/BasicSettings.vue'
import type { BasicSettings as BasicSettingsType } from '@/stores/systemSettings'

// Mock Element Plus components
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

describe('BasicSettings.vue', () => {
  let wrapper: any
  let mockBasicSettings: BasicSettingsType

  beforeEach(() => {
    mockBasicSettings = {
      systemName: '测试酒店管理系统',
      systemLogo: '',
      contactPhone: '13800138000',
      contactEmail: 'test@example.com',
      contactAddress: '测试地址',
      systemDescription: '测试描述',
      businessHours: '24小时服务'
    }

    wrapper = mount(BasicSettings, {
      props: {
        modelValue: { ...mockBasicSettings }
      },
      global: {
        stubs: {
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-input-number': true,
          'el-upload': true,
          'el-button': true,
          'el-row': true,
          'el-col': true,
          'el-icon': true,
          'el-message': true,
          'el-image': true
        }
      }
    })
  })

  it('renders correctly with initial props', () => {
    expect(wrapper.exists()).toBe(true)

    // Check if sections are rendered
    const sections = wrapper.findAll('.settings-section')
    expect(sections.length).toBe(3) // 系统信息, 联系信息, 效果预览
  })

  it('emits update:modelValue when form data changes', async () => {
    const newSystemName = '新的系统名称'

    // Simulate form data change
    await wrapper.vm.$nextTick()
    wrapper.vm.formData.systemName = newSystemName
    await wrapper.vm.$nextTick()

    // Check if emit was called with updated data
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    const lastEmit = wrapper.emitted('update:modelValue')[wrapper.emitted('update:modelValue').length - 1][0]
    expect(lastEmit.systemName).toBe(newSystemName)
  })

  it('validates required fields', async () => {
    const formRef = {
      validate: vi.fn().mockRejectedValue(new Error('Validation failed'))
    }
    wrapper.vm.formRef = formRef

    await wrapper.vm.handleSave()

    expect(formRef.validate).toHaveBeenCalled()
    expect(ElMessage.error).toHaveBeenCalledWith('请检查表单数据')
  })

  it('calls save method on valid form', async () => {
    const formRef = {
      validate: vi.fn().mockResolvedValue(true)
    }
    wrapper.vm.formRef = formRef

    await wrapper.vm.handleSave()

    expect(formRef.validate).toHaveBeenCalled()
    expect(wrapper.emitted('save')).toBeTruthy()
  })

  it('handles reset functionality', async () => {
    // Mock ElMessageBox.confirm to resolve
    vi.mocked(ElMessageBox.confirm).mockResolvedValue(true)
    vi.mocked(ElMessage.success).mockImplementation(() => {})

    await wrapper.vm.handleReset()

    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(ElMessage.success).toHaveBeenCalledWith('已重置到上次保存的状态')
  })

  it('validates logo upload', () => {
    const validFile = new File([''], 'logo.png', { type: 'image/png' })
    Object.defineProperty(validFile, 'size', { value: 1024 * 1024 }) // 1MB

    const result = wrapper.vm.beforeLogoUpload(validFile)
    expect(result).toBe(true)

    const invalidFile = new File([''], 'logo.pdf', { type: 'application/pdf' })
    const invalidResult = wrapper.vm.beforeLogoUpload(invalidFile)
    expect(invalidResult).toBe(false)
  })

  it('updates original data on prop change', async () => {
    const newSettings = {
      systemName: '更新的系统名称',
      contactPhone: '13900139000'
    }

    await wrapper.setProps({ modelValue: newSettings })
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.originalData.systemName).toBe('更新的系统名称')
    expect(wrapper.vm.originalData.contactPhone).toBe('13900139000')
  })

  it('loads initial data on mount', async () => {
    const wrapper = mount(BasicSettings, {
      props: {
        modelValue: mockBasicSettings
      },
      global: {
        stubs: {
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-upload': true,
          'el-button': true,
          'el-row': true,
          'el-col': true,
          'el-icon': true
        }
      }
    })

    await wrapper.vm.$nextTick()

    expect(wrapper.vm.formData.systemName).toBe(mockBasicSettings.systemName)
    expect(wrapper.vm.formData.contactPhone).toBe(mockBasicSettings.contactPhone)
    expect(wrapper.vm.formData.contactEmail).toBe(mockBasicSettings.contactEmail)
  })

  it('emits loading event during save', async () => {
    const formRef = {
      validate: vi.fn().mockResolvedValue(true)
    }
    wrapper.vm.formRef = formRef

    await wrapper.vm.handleSave()

    expect(wrapper.emitted('loading')).toBeTruthy()
  })

  it('emits loading event during file upload', async () => {
    const file = new File([''], 'logo.png', { type: 'image/png' })
    Object.defineProperty(file, 'size', { value: 1024 * 1024 })

    const options = { file }
    await wrapper.vm.handleLogoUpload(options)

    expect(wrapper.emitted('loading')).toBeTruthy()
  })
})