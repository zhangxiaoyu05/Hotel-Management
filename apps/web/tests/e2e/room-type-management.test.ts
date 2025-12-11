import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import RoomTypeList from '../../src/pages/admin/room-types/RoomTypeList.vue'
import RoomTypeForm from '../../src/pages/admin/room-types/RoomTypeForm.vue'
import { useRoomTypeStore } from '../../src/stores/roomTypeStore'
import { useHotelStore } from '../../src/stores/hotelStore'
import { ElMessage, ElMessageBox } from 'element-plus'

// Mock API
const mockApi = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn()
}

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn()
    },
    ElMessageBox: {
      confirm: vi.fn()
    }
  }
})

// Mock stores
vi.mock('../../src/stores/roomTypeStore', () => ({
  useRoomTypeStore: () => ({
    loading: false,
    error: null,
    roomTypes: [
      {
        id: 1,
        hotelId: 1,
        name: '标准间',
        capacity: 2,
        basePrice: 299,
        facilities: ['WiFi', '空调'],
        description: '舒适的标准间',
        iconUrl: 'https://example.com/standard-room.png',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 2,
        hotelId: 1,
        name: '豪华套房',
        capacity: 4,
        basePrice: 888,
        facilities: ['WiFi', '空调', '电视', '迷你吧'],
        description: '豪华的套房',
        iconUrl: 'https://example.com/luxury-suite.png',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z'
      }
    ],
    pagination: {
      totalElements: 2,
      totalPages: 1,
      size: 20,
      number: 0
    },
    fetchRoomTypes: vi.fn(),
    createRoomType: vi.fn(),
    updateRoomType: vi.fn(),
    deleteRoomType: vi.fn(),
    updateRoomTypeStatus: vi.fn(),
    getRoomTypesByHotelId: vi.fn()
  })
}))

vi.mock('../../src/stores/hotelStore', () => ({
  useHotelStore: () => ({
    hotels: [
      { id: 1, name: '测试酒店' },
      { id: 2, name: '另一酒店' }
    ],
    fetchHotels: vi.fn()
  })
}))

describe('房间类型管理 - 端到端测试', () => {
  let router: any
  let pinia: any

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)

    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/admin/room-types', component: RoomTypeList },
        { path: '/admin/room-types/create', component: RoomTypeForm },
        { path: '/admin/room-types/:id/edit', component: RoomTypeForm }
      ]
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('房间类型列表页面', () => {
    it('正确显示房间类型列表', async () => {
      const wrapper = mount(RoomTypeList, {
        global: {
          plugins: [router, pinia],
          components: { ElementPlus },
          stubs: {
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-row': true,
            'el-col': true,
            'el-skeleton': true,
            'el-alert': true,
            'el-empty': true,
            'el-pagination': true,
            RoomTypeCard: {
              template: '<div class="mock-room-type-card">{{ roomType.name }}</div>',
              props: ['roomType', 'adminMode'],
              emits: ['edit', 'delete', 'toggle-status', 'view']
            }
          }
        }
      })

      await wrapper.vm.$nextTick()
      await router.isReady()

      expect(wrapper.find('.page-header h1').text()).toBe('房间类型管理')
      expect(wrapper.findAll('.mock-room-type-card').length).toBe(2)
    })

    it('搜索功能正常工作', async () => {
      const roomTypeStore = useRoomTypeStore()
      const wrapper = mount(RoomTypeList, {
        global: {
          plugins: [router, pinia],
          stubs: {
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-row': true,
            'el-col': true,
            'el-skeleton': true,
            'el-alert': true,
            'el-empty': true,
            'el-pagination': true,
            RoomTypeCard: {
              template: '<div class="mock-room-type-card">{{ roomType.name }}</div>',
              props: ['roomType', 'adminMode'],
              emits: ['edit', 'delete', 'toggle-status', 'view']
            }
          }
        }
      })

      await wrapper.vm.$nextTick()
      await router.isReady()

      // 搜索"豪华"
      await wrapper.find('[placeholder="搜索房间类型名称或描述"]').setValue('豪华')
      await wrapper.find('[data-test="search-button"]').trigger('click')

      expect(roomTypeStore.fetchRoomTypes).toHaveBeenCalledWith(
        expect.objectContaining({
          search: '豪华'
        })
      )
    })

    it('分页功能正常工作', async () => {
      const roomTypeStore = useRoomTypeStore()
      const wrapper = mount(RoomTypeList, {
        global: {
          plugins: [router, pinia],
          stubs: {
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-row': true,
            'el-col': true,
            'el-skeleton': true,
            'el-alert': true,
            'el-empty': true,
            'el-pagination': true,
            RoomTypeCard: {
              template: '<div class="mock-room-type-card">{{ roomType.name }}</div>',
              props: ['roomType', 'adminMode'],
              emits: ['edit', 'delete', 'toggle-status', 'view']
            }
          }
        }
      })

      await wrapper.vm.$nextTick()
      await router.isReady()

      // 切换到第2页
      await wrapper.vm.handlePageChange(2)

      expect(roomTypeStore.fetchRoomTypes).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 1
        })
      )
    })
  })

  describe('房间类型表单页面', () => {
    it('创建房间类型表单验证', async () => {
      const roomTypeStore = useRoomTypeStore()
      const wrapper = mount(RoomTypeForm, {
        props: {
          roomType: null,
          loading: false,
          hotelId: 1
        },
        global: {
          plugins: [router, pinia],
          components: { ElementPlus },
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
            RoomTypeIcon: {
              template: '<div class="mock-icon-upload"></div>',
              props: ['iconUrl'],
              emits: ['change']
            }
          }
        }
      })

      await wrapper.vm.$nextTick()

      // 提交空表单应该触发验证
      await wrapper.vm.handleSubmit()
      expect(wrapper.emitted('submit')).toBeFalsy()

      // 填写有效数据
      wrapper.vm.form.name = '新房间类型'
      wrapper.vm.form.capacity = 2
      wrapper.vm.form.basePrice = 299

      // Mock表单验证通过
      const formRef = {
        validate: vi.fn().mockResolvedValue(true)
      }
      wrapper.vm.formRef = formRef

      await wrapper.vm.handleSubmit()
      expect(wrapper.emitted('submit')).toBeTruthy()
    })

    it('编辑房间类型表单预填充', async () => {
      const roomType = {
        id: 1,
        hotelId: 1,
        name: '标准间',
        capacity: 2,
        basePrice: 299,
        facilities: ['WiFi', '空调'],
        description: '舒适的标准间',
        iconUrl: 'https://example.com/icon.png',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z'
      }

      const wrapper = mount(RoomTypeForm, {
        props: {
          roomType: roomType,
          loading: false
        },
        global: {
          plugins: [router, pinia],
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
            RoomTypeIcon: {
              template: '<div class="mock-icon-upload"></div>',
              props: ['iconUrl'],
              emits: ['change']
            }
          }
        }
      })

      await wrapper.vm.$nextTick()

      expect(wrapper.vm.isEdit).toBe(true)
      expect(wrapper.vm.form.name).toBe('标准间')
      expect(wrapper.vm.form.capacity).toBe(2)
      expect(wrapper.vm.form.basePrice).toBe(299)
      expect(wrapper.vm.status).toBe('ACTIVE')
    })

    it('设施管理功能', async () => {
      const wrapper = mount(RoomTypeForm, {
        props: {
          roomType: null,
          loading: false
        },
        global: {
          plugins: [router, pinia],
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
            RoomTypeIcon: {
              template: '<div class="mock-icon-upload"></div>',
              props: ['iconUrl'],
              emits: ['change']
            }
          }
        }
      })

      await wrapper.vm.$nextTick()

      // 添加设施
      wrapper.vm.facilityInput = 'WiFi'
      wrapper.vm.addFacility()
      expect(wrapper.vm.form.facilities).toContain('WiFi')
      expect(wrapper.vm.facilityInput).toBe('')

      // 添加重复设施
      wrapper.vm.facilityInput = 'WiFi'
      wrapper.vm.addFacility()
      expect(wrapper.vm.form.facilities.filter(f => f === 'WiFi').length).toBe(1)

      // 移除设施
      wrapper.vm.removeFacility(0)
      expect(wrapper.vm.form.facilities).not.toContain('WiFi')
    })
  })

  describe('完整的房间类型管理工作流', () => {
    it('从列表到创建到查看的完整流程', async () => {
      const roomTypeStore = useRoomTypeStore()

      // 1. 访问列表页面
      const listWrapper = mount(RoomTypeList, {
        global: {
          plugins: [router, pinia],
          stubs: {
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-row': true,
            'el-col': true,
            'el-skeleton': true,
            'el-alert': true,
            'el-empty': true,
            'el-pagination': true,
            RoomTypeCard: {
              template: '<div class="mock-room-type-card">{{ roomType.name }}</div>',
              props: ['roomType', 'adminMode'],
              emits: ['edit', 'delete', 'toggle-status', 'view']
            }
          }
        }
      })

      await listWrapper.vm.$nextTick()
      await router.isReady()

      // 2. 点击创建按钮
      await listWrapper.vm.handleCreateRoomType()
      expect(router.currentRoute.value.path).toBe('/admin/room-types/create')

      // 3. 在创建表单中填写数据
      const formWrapper = mount(RoomTypeForm, {
        props: {
          roomType: null,
          loading: false,
          hotelId: 1
        },
        global: {
          plugins: [router, pinia],
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
            RoomTypeIcon: {
              template: '<div class="mock-icon-upload"></div>',
              props: ['iconUrl'],
              emits: ['change']
            }
          }
        }
      })

      await formWrapper.vm.$nextTick()

      formWrapper.vm.form.name = '商务间'
      formWrapper.vm.form.capacity = 2
      formWrapper.vm.form.basePrice = 399

      const formRef = {
        validate: vi.fn().mockResolvedValue(true)
      }
      formWrapper.vm.formRef = formRef

      // 4. 提交表单
      await formWrapper.vm.handleSubmit()

      expect(formWrapper.emitted('submit')).toBeTruthy()
      expect(formWrapper.emitted('submit')[0][0]).toEqual({
        name: '商务间',
        capacity: 2,
        basePrice: 399,
        facilities: [],
        description: '',
        iconUrl: ''
      })

      // 5. Mock创建成功后返回列表
      roomTypeStore.roomTypes.push({
        id: 3,
        hotelId: 1,
        name: '商务间',
        capacity: 2,
        basePrice: 399,
        facilities: [],
        description: '',
        iconUrl: '',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z'
      })

      // 验证新房间类型出现在列表中
      await listWrapper.vm.$nextTick()
      expect(listWrapper.findAll('.mock-room-type-card').length).toBe(3)
    })
  })
})