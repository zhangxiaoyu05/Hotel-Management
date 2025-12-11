import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { ElCard, ElInput, ElSelect, ElOption, ElButton, ElPagination, ElTag, ElMessage, ElMessageBox } from 'element-plus'
import HotelList from '@/pages/admin/hotels/HotelList.vue'
import { useHotelStore } from '@/stores/hotelStore'

// Mock components and stores
vi.mock('@/stores/hotelStore', () => ({
  useHotelStore: vi.fn(() => ({
    hotels: [
      {
        id: 1,
        name: 'Active Hotel',
        address: 'Test Address 1',
        status: 'ACTIVE',
        facilities: ['WiFi', 'Parking'],
        images: ['hotel1.jpg'],
        phone: '13800138001',
        description: 'Description 1',
        createdBy: 1,
        createdAt: '2025-12-06T10:00:00Z',
        updatedAt: '2025-12-06T10:00:00Z'
      },
      {
        id: 2,
        name: 'Inactive Hotel',
        address: 'Test Address 2',
        status: 'INACTIVE',
        facilities: ['WiFi'],
        images: ['hotel2.jpg'],
        phone: '13800138002',
        description: 'Description 2',
        createdBy: 1,
        createdAt: '2025-12-06T11:00:00Z',
        updatedAt: '2025-12-06T11:00:00Z'
      }
    ],
    loading: false,
    error: null,
    totalElements: 2,
    totalPages: 1,
    currentPage: 0,
    pageSize: 20,
    searchQuery: '',
    statusFilter: '',
    sortBy: 'createdAt',
    sortDir: 'DESC',
    hasNextPage: false,
    hasPrevPage: false,
    fetchHotels: vi.fn(),
    deleteHotel: vi.fn(),
    updateHotelStatus: vi.fn(),
    setSearchQuery: vi.fn(),
    setStatusFilter: vi.fn(),
    setSort: vi.fn(),
    resetFilters: vi.fn(),
    clearError: vi.fn()
  }))
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  })
}))

// Mock Element Plus components
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElCard: { template: '<div class="el-card"><slot /></div>' },
    ElInput: {
      template: '<input class="el-input" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
      props: ['modelValue', 'placeholder']
    },
    ElSelect: {
      template: '<select class="el-select" :value="modelValue" @change="$emit(\'update:modelValue\', $event)"><slot /></select>',
      props: ['modelValue', 'placeholder']
    },
    ElOption: {
      template: '<option class="el-option" :value="value"><slot /></option>',
      props: ['value', 'label']
    },
    ElButton: {
      template: '<button class="el-button" :class="`el-button--${type}`" @click="$emit(\'click\')"><slot /></button>',
      props: ['type', 'size', 'icon', 'loading']
    },
    ElPagination: {
      template: '<div class="el-pagination"></div>',
      props: ['currentPage', 'pageSize', 'total', 'pageSizes', 'layout']
    },
    ElTag: {
      template: '<span class="el-tag" :class="`el-tag--${type}`"><slot /></span>',
      props: ['type', 'size']
    },
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn()
    },
    ElMessageBox: {
      confirm: vi.fn(() => Promise.resolve())
    }
  }
})

describe('HotelList.vue', () => {
  let hotelStore: ReturnType<typeof useHotelStore>
  let router: any

  beforeEach(() => {
    vi.clearAllMocks()
    hotelStore = useHotelStore()
    router = { push: vi.fn() }
  })

  const createWrapper = () => {
    return mount(HotelList, {
      global: {
        components: {
          ElCard,
          ElInput,
          ElSelect,
          ElOption,
          ElButton,
          ElPagination,
          ElTag
        },
        mocks: {
          $router: router
        },
        stubs: {
          'el-icon': true
        }
      }
    })
  }

  it('renders hotel list page correctly', () => {
    const wrapper = createWrapper()

    expect(wrapper.find('.hotel-list__header').text()).toContain('酒店管理')
    expect(wrapper.find('.hotel-list__search').exists()).toBe(true)
    expect(wrapper.find('.hotel-list__filter').exists()).toBe(true)
    expect(wrapper.find('.hotel-list__actions').exists()).toBe(true)
  })

  it('displays hotels correctly', () => {
    const wrapper = createWrapper()

    const hotelCards = wrapper.findAll('.hotel-card')
    expect(hotelCards).toHaveLength(2)
  })

  it('shows loading state correctly', async () => {
    const mockStore = useHotelStore()
    mockStore.loading = true

    const wrapper = createWrapper()

    const loadingElement = wrapper.find('.hotel-list__loading')
    expect(loadingElement.exists()).toBe(true)
  })

  it('shows error state correctly', async () => {
    const mockStore = useHotelStore()
    mockStore.error = '获取酒店列表失败'

    const wrapper = createWrapper()

    const errorElement = wrapper.find('.hotel-list__error')
    expect(errorElement.exists()).toBe(true)
    expect(errorElement.text()).toContain('获取酒店列表失败')
  })

  it('shows empty state when no hotels', async () => {
    const mockStore = useHotelStore()
    mockStore.hotels = []
    mockStore.totalElements = 0

    const wrapper = createWrapper()

    const emptyElement = wrapper.find('.hotel-list__empty')
    expect(emptyElement.exists()).toBe(true)
    expect(emptyElement.text()).toContain('暂无酒店数据')
  })

  it('handles search input correctly', async () => {
    const wrapper = createWrapper()
    const searchInput = wrapper.find('.el-input')

    await searchInput.setValue('Test Hotel')
    await nextTick()

    expect(hotelStore.setSearchQuery).toHaveBeenCalledWith('Test Hotel')
  })

  it('handles status filter correctly', async () => {
    const wrapper = createWrapper()
    const statusFilter = wrapper.find('.el-select')

    await statusFilter.setValue('ACTIVE')
    await nextTick()

    expect(hotelStore.setStatusFilter).toHaveBeenCalledWith('ACTIVE')
  })

  it('handles create hotel button click', async () => {
    const wrapper = createWrapper()
    const createButton = wrapper.find('.hotel-list__create')

    await createButton.trigger('click')

    expect(router.push).toHaveBeenCalledWith('/admin/hotels/create')
  })

  it('handles refresh button click', async () => {
    const wrapper = createWrapper()
    const refreshButton = wrapper.find('.hotel-list__refresh')

    await refreshButton.trigger('click')

    expect(hotelStore.fetchHotels).toHaveBeenCalled()
  })

  it('handles reset filters button click', async () => {
    const wrapper = createWrapper()
    const resetButton = wrapper.find('.hotel-list__reset')

    await resetButton.trigger('click')

    expect(hotelStore.resetFilters).toHaveBeenCalled()
    expect(hotelStore.fetchHotels).toHaveBeenCalled()
  })

  it('handles hotel edit correctly', async () => {
    const wrapper = createWrapper()
    const mockHotel = hotelStore.hotels[0]

    // Simulate hotel edit event
    await wrapper.vm.handleEdit(mockHotel)

    expect(router.push).toHaveBeenCalledWith(`/admin/hotels/${mockHotel.id}/edit`)
  })

  it('handles hotel delete correctly', async () => {
    const wrapper = createWrapper()
    const mockHotel = hotelStore.hotels[0]

    vi.mocked(ElMessageBox.confirm).mockResolvedValueOnce(undefined)
    hotelStore.deleteHotel.mockResolvedValueOnce({ success: true, message: '酒店删除成功' } as any)

    await wrapper.vm.handleDelete(mockHotel)

    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(hotelStore.deleteHotel).toHaveBeenCalledWith(mockHotel.id)
  })

  it('handles hotel delete cancellation correctly', async () => {
    const wrapper = createWrapper()
    const mockHotel = hotelStore.hotels[0]

    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce(new Error('cancel'))

    await wrapper.vm.handleDelete(mockHotel)

    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(hotelStore.deleteHotel).not.toHaveBeenCalled()
  })

  it('handles hotel status change correctly', async () => {
    const wrapper = createWrapper()
    const mockHotel = hotelStore.hotels[0]

    hotelStore.updateHotelStatus.mockResolvedValueOnce({ success: true, message: '状态更新成功' } as any)

    await wrapper.vm.handleStatusChange(mockHotel)

    const newStatus = mockHotel.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    expect(hotelStore.updateHotelStatus).toHaveBeenCalledWith(mockHotel.id, newStatus)
  })

  it('handles pagination change correctly', async () => {
    const wrapper = createWrapper()

    await wrapper.vm.handlePageChange(2)

    expect(hotelStore.fetchHotels).toHaveBeenCalled()
  })

  it('handles page size change correctly', async () => {
    const wrapper = createWrapper()

    await wrapper.vm.handlePageSizeChange(50)

    expect(hotelStore.fetchHotels).toHaveBeenCalled()
  })

  it('debounces search input', async () => {
    const wrapper = createWrapper()
    const searchInput = wrapper.find('.el-input')

    await searchInput.setValue('Test')
    await searchInput.setValue('Test Hotel')

    // Should only call setSearchQuery once due to debouncing
    await new Promise(resolve => setTimeout(resolve, 600))

    expect(hotelStore.setSearchQuery).toHaveBeenCalledTimes(1)
    expect(hotelStore.setSearchQuery).toHaveBeenCalledWith('Test Hotel')
  })

  it('computes pagination layout correctly', () => {
    const wrapper = createWrapper()

    expect(wrapper.vm.paginationLayout).toBe('total, sizes, prev, pager, next, jumper')
  })

  it('computes page sizes correctly', () => {
    const wrapper = createWrapper()

    expect(wrapper.vm.pageSizes).toEqual([10, 20, 50, 100])
  })

  it('shows hotel count correctly', () => {
    const wrapper = createWrapper()

    expect(wrapper.text()).toContain(`共 ${hotelStore.totalElements} 个酒店`)
  })

  it('handles error retry correctly', async () => {
    const mockStore = useHotelStore()
    mockStore.error = '网络错误'

    const wrapper = createWrapper()
    const retryButton = wrapper.find('.hotel-list__retry')

    await retryButton.trigger('click')

    expect(mockStore.clearError).toHaveBeenCalled()
    expect(mockStore.fetchHotels).toHaveBeenCalled()
  })
})