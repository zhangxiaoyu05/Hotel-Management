import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElForm, ElFormItem, ElInput, ElInputNumber, ElSelect, ElOption, ElSwitch, ElButton, ElTag } from 'element-plus'
import RoomTypeForm from '../../../../src/components/room-type/RoomTypeForm.vue'
import RoomTypeIcon from '../../../../src/components/room-type/RoomTypeIcon.vue'
import type { RoomType } from '../../../../src/types/roomType'

// Mock RoomTypeIcon component
vi.mock('../../../../src/components/room-type/RoomTypeIcon.vue', () => ({
  default: {
    name: 'RoomTypeIcon',
    template: '<div class="mock-room-type-icon" @click="$emit(\'change\', \'test-url\')"></div>',
    props: ['iconUrl'],
    emits: ['change']
  }
}))

describe('RoomTypeForm.vue', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(RoomTypeForm, {
      props: {
        roomType: null,
        loading: false
      },
      global: {
        components: {
          ElForm,
          ElFormItem,
          ElInput,
          ElInputNumber,
          ElSelect,
          ElOption,
          ElSwitch,
          ElButton,
          ElTag,
          RoomTypeIcon
        },
        stubs: {
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-input-number': true,
          'el-select': true,
          'el-option': true,
          'el-switch': true,
          'el-button': true,
          'el-tag': true,
          'room-type-icon': true
        }
      }
    })
  })

  it('正确初始化创建模式的表单', () => {
    expect(wrapper.vm.isEdit).toBe(false)
    expect(wrapper.vm.form.name).toBe('')
    expect(wrapper.vm.form.capacity).toBe(1)
    expect(wrapper.vm.form.basePrice).toBe(0)
    expect(wrapper.vm.form.facilities).toEqual([])
    expect(wrapper.vm.form.description).toBe('')
    expect(wrapper.vm.form.iconUrl).toBe('')
  })

  it('正确初始化编辑模式的表单', async () => {
    const mockRoomType: RoomType = {
      id: 1,
      hotelId: 1,
      name: '豪华套房',
      capacity: 4,
      basePrice: 888,
      facilities: ['WiFi', '空调', '电视', '迷你吧'],
      description: '豪华的套房',
      iconUrl: 'https://example.com/icon.png',
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    }

    await wrapper.setProps({ roomType: mockRoomType })
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.isEdit).toBe(true)
    expect(wrapper.vm.form.name).toBe('豪华套房')
    expect(wrapper.vm.form.capacity).toBe(4)
    expect(wrapper.vm.form.basePrice).toBe(888)
    expect(wrapper.vm.form.facilities).toEqual(['WiFi', '空调', '电视', '迷你吧'])
    expect(wrapper.vm.form.description).toBe('豪华的套房')
    expect(wrapper.vm.form.iconUrl).toBe('https://example.com/icon.png')
    expect(wrapper.vm.status).toBe('ACTIVE')
  })

  it('验证必填字段', async () => {
    const formRef = {
      validate: vi.fn().mockRejectedValue(new Error('Validation failed'))
    }
    wrapper.vm.formRef = formRef

    await wrapper.vm.handleSubmit()
    expect(formRef.validate).toHaveBeenCalled()
  })

  it('成功提交创建表单', async () => {
    const formRef = {
      validate: vi.fn().mockResolvedValue(true)
    }
    wrapper.vm.formRef = formRef

    // 设置表单数据
    wrapper.vm.form = {
      name: '新房间类型',
      capacity: 2,
      basePrice: 299,
      facilities: ['WiFi'],
      description: '描述',
      iconUrl: ''
    }

    await wrapper.vm.handleSubmit()

    expect(wrapper.emitted('submit')).toBeTruthy()
    expect(wrapper.emitted('submit')[0][0]).toEqual({
      name: '新房间类型',
      capacity: 2,
      basePrice: 299,
      facilities: ['WiFi'],
      description: '描述',
      iconUrl: ''
    })
  })

  it('成功提交编辑表单', async () => {
    const formRef = {
      validate: vi.fn().mockResolvedValue(true)
    }
    wrapper.vm.formRef = formRef

    // 设置为编辑模式
    const mockRoomType: RoomType = {
      id: 1,
      hotelId: 1,
      name: '测试房间',
      capacity: 2,
      basePrice: 299,
      facilities: [],
      description: '',
      iconUrl: '',
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    }
    await wrapper.setProps({ roomType: mockRoomType })
    await wrapper.vm.$nextTick()

    wrapper.vm.status = 'INACTIVE'
    await wrapper.vm.handleSubmit()

    expect(wrapper.emitted('submit')).toBeTruthy()
    expect(wrapper.emitted('submit')[0][0]).toEqual({
      name: '测试房间',
      capacity: 2,
      basePrice: 299,
      facilities: [],
      description: '',
      iconUrl: '',
      status: 'INACTIVE'
    })
  })

  it('处理取消操作', () => {
    wrapper.vm.handleCancel()
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('处理图标变更', () => {
    wrapper.vm.handleIconChange('https://example.com/new-icon.png')
    expect(wrapper.vm.form.iconUrl).toBe('https://example.com/new-icon.png')
  })

  it('添加设施', () => {
    wrapper.vm.facilityInput = '空调'
    wrapper.vm.addFacility()
    expect(wrapper.vm.form.facilities).toContain('空调')
    expect(wrapper.vm.facilityInput).toBe('')
  })

  it('防止重复添加设施', () => {
    wrapper.vm.form.facilities = ['WiFi']
    wrapper.vm.facilityInput = 'WiFi'
    wrapper.vm.addFacility()
    expect(wrapper.vm.form.facilities.filter(f => f === 'WiFi').length).toBe(1)
  })

  it('不添加空设施', () => {
    wrapper.vm.facilityInput = '   '
    wrapper.vm.addFacility()
    expect(wrapper.vm.form.facilities).not.toContain('   ')
  })

  it('移除设施', () => {
    wrapper.vm.form.facilities = ['WiFi', '空调', '电视']
    wrapper.vm.removeFacility(1)
    expect(wrapper.vm.form.facilities).toEqual(['WiFi', '电视'])
  })

  it('处理回车键添加设施', () => {
    wrapper.vm.facilityInput = '新设施'
    const event = new KeyboardEvent('keydown', { key: 'Enter' })
    event.preventDefault = vi.fn()

    wrapper.vm.handleFacilityInputKeydown(event)

    expect(event.preventDefault).toHaveBeenCalled()
    expect(wrapper.vm.form.facilities).toContain('新设施')
  })

  it('重置表单', () => {
    // 修改表单数据
    wrapper.vm.form.name = '测试名称'
    wrapper.vm.form.capacity = 5
    wrapper.vm.form.basePrice = 999
    wrapper.vm.form.facilities = ['设施1', '设施2']
    wrapper.vm.form.description = '测试描述'
    wrapper.vm.form.iconUrl = 'test-url'
    wrapper.vm.status = 'INACTIVE'
    wrapper.vm.facilityInput = '未完成的设施'

    // 重置表单
    wrapper.vm.resetForm()

    expect(wrapper.vm.form.name).toBe('')
    expect(wrapper.vm.form.capacity).toBe(1)
    expect(wrapper.vm.form.basePrice).toBe(0)
    expect(wrapper.vm.form.facilities).toEqual([])
    expect(wrapper.vm.form.description).toBe('')
    expect(wrapper.vm.form.iconUrl).toBe('')
    expect(wrapper.vm.status).toBe('ACTIVE')
    expect(wrapper.vm.facilityInput).toBe('')
  })

  it('表单验证规则正确配置', () => {
    const rules = wrapper.vm.rules
    expect(rules.name).toBeDefined()
    expect(rules.capacity).toBeDefined()
    expect(rules.basePrice).toBeDefined()

    expect(rules.name[0].required).toBe(true)
    expect(rules.name[0].message).toBe('请输入房间类型名称')
    expect(rules.name[1].min).toBe(2)
    expect(rules.name[1].max).toBe(50)

    expect(rules.capacity[0].required).toBe(true)
    expect(rules.capacity[1].min).toBe(1)
    expect(rules.capacity[1].max).toBe(20)

    expect(rules.basePrice[0].required).toBe(true)
    expect(rules.basePrice[1].min).toBe(0)
  })
})