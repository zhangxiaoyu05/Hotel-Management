import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import RoomSearch from '@/pages/rooms/RoomSearch.vue'
import { createPinia, setActivePinia } from 'pinia'

// Mock room store
vi.mock('@/stores/roomStore', () => ({
  useRoomStore: () => ({
    searchResults: [],
    searchHistory: [],
    loading: false,
    roomTypes: [],
    searchAvailableRooms: vi.fn(),
    addToSearchHistory: vi.fn(),
    clearSearchHistory: vi.fn(),
    getRoomTypes: vi.fn()
  })
}))

// Mock room API
vi.mock('@/api/room', () => ({
  roomApi: {
    getRoomTypes: vi.fn().mockResolvedValue([])
  }
}))

describe('RoomSearch.vue', () => {
  let wrapper: any

  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('应该正确渲染搜索表单', () => {
    // When
    wrapper = mount(RoomSearch)

    // Then
    expect(wrapper.find('.search-form').exists()).toBe(true)
    expect(wrapper.find('[data-test="check-in-date"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="check-out-date"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="guest-count"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="room-type"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="search-button"]').exists()).toBe(true)
  })

  it('应该初始化默认搜索值', () => {
    // When
    wrapper = mount(RoomSearch)

    // Then
    const vm = wrapper.vm as any
    expect(vm.searchForm.guestCount).toBe(2)
    expect(vm.searchForm.checkInDate).toBeDefined()
    expect(vm.searchForm.checkOutDate).toBeDefined()
  })

  it('应该验证入住日期和退房日期', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When - 设置退房日期早于入住日期
    vm.searchForm.checkInDate = '2024-12-10'
    vm.searchForm.checkOutDate = '2024-12-08'
    await vm.$nextTick()

    // Then
    expect(vm.validateDates()).toBe(false)
  })

  it('应该验证入住日期不能早于今天', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When - 设置入住日期为昨天
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)
    vm.searchForm.checkInDate = yesterday.toISOString().split('T')[0]
    await vm.$nextTick()

    // Then
    expect(vm.validateDates()).toBe(false)
  })

  it('应该在点击搜索时触发搜索', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any
    const store = vm.roomStore

    // When
    vm.searchForm.checkInDate = '2024-12-10'
    vm.searchForm.checkOutDate = '2024-12-11'
    vm.searchForm.guestCount = 2
    await vm.handleSearch()

    // Then
    expect(store.searchAvailableRooms).toHaveBeenCalled()
  })

  it('应该添加搜索条件到历史记录', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any
    const store = vm.roomStore

    // When
    vm.searchForm.checkInDate = '2024-12-10'
    vm.searchForm.checkOutDate = '2024-12-11'
    vm.searchForm.guestCount = 2
    await vm.handleSearch()

    // Then
    expect(store.addToSearchHistory).toHaveBeenCalled()
  })

  it('应该显示加载状态', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When
    vm.roomStore.loading = true
    await vm.$nextTick()

    // Then
    expect(wrapper.find('.search-loading').exists()).toBe(true)
    expect(wrapper.find('[data-test="search-button"]').attributes('loading')).toBeDefined()
  })

  it('应该显示搜索结果', async () => {
    // Given
    const mockResults = [
      { id: 1, roomNumber: '101', price: 299 },
      { id: 2, roomNumber: '102', price: 399 }
    ]

    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When
    vm.roomStore.searchResults = mockResults
    await vm.$nextTick()

    // Then
    expect(wrapper.findAll('.room-card')).toHaveLength(2)
  })

  it('应该显示空结果状态', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When
    vm.roomStore.searchResults = []
    vm.roomStore.loading = false
    await vm.$nextTick()

    // Then
    expect(wrapper.find('.empty-result').exists()).toBe(true)
  })

  it('应该显示分页组件', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When
    vm.roomStore.searchResults = Array(25).fill({ id: 1 }) // 25个结果
    await vm.$nextTick()

    // Then
    expect(wrapper.find('.pagination').exists()).toBe(true)
  })

  it('应该处理页码变化', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When
    await vm.handlePageChange(2)

    // Then
    expect(vm.searchForm.page).toBe(2)
    expect(vm.roomStore.searchAvailableRooms).toHaveBeenCalled()
  })

  it('应该显示搜索历史', async () => {
    // Given
    const mockHistory = [
      { checkInDate: '2024-12-10', checkOutDate: '2024-12-11', guestCount: 2 }
    ]

    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When
    vm.roomStore.searchHistory = mockHistory
    await vm.$nextTick()

    // Then
    expect(wrapper.find('.search-history').exists()).toBe(true)
    expect(wrapper.findAll('.history-item')).toHaveLength(1)
  })

  it('应该点击历史记录快速填充搜索表单', async () => {
    // Given
    const mockHistory = [
      {
        checkInDate: '2024-12-10',
        checkOutDate: '2024-12-11',
        guestCount: 2,
        hotelId: 1,
        roomTypeId: 1
      }
    ]

    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    vm.roomStore.searchHistory = mockHistory
    await vm.$nextTick()

    // When
    await wrapper.find('.history-item').trigger('click')

    // Then
    expect(vm.searchForm.checkInDate).toBe('2024-12-10')
    expect(vm.searchForm.checkOutDate).toBe('2024-12-11')
    expect(vm.searchForm.guestCount).toBe(2)
  })

  it('应该清除搜索历史', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any
    const store = vm.roomStore

    // When
    await vm.clearHistory()

    // Then
    expect(store.clearSearchHistory).toHaveBeenCalled()
  })

  it('应该显示高级筛选选项', async () => {
    // Given
    wrapper = mount(RoomSearch)

    // When
    await wrapper.find('.toggle-filters').trigger('click')

    // Then
    expect(wrapper.find('.advanced-filters').isVisible()).toBe(true)
  })

  it('应该应用价格筛选', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When
    await wrapper.find('[data-test="price-min"]').setValue(200)
    await wrapper.find('[data-test="price-max"]').setValue(500)

    // Then
    expect(vm.searchForm.priceMin).toBe(200)
    expect(vm.searchForm.priceMax).toBe(500)
  })

  it('应该应用设施筛选', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When
    await wrapper.find('[data-test="facility-wifi"]').setChecked(true)
    await wrapper.find('[data-test="facility-ac"]').setChecked(true)

    // Then
    expect(vm.searchForm.facilities).toContain('WiFi')
    expect(vm.searchForm.facilities).toContain('空调')
  })

  it('应该重置筛选条件', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // 设置一些筛选条件
    vm.searchForm.priceMin = 200
    vm.searchForm.priceMax = 500
    vm.searchForm.facilities = ['WiFi']

    // When
    await vm.resetFilters()

    // Then
    expect(vm.searchForm.priceMin).toBeNull()
    expect(vm.searchForm.priceMax).toBeNull()
    expect(vm.searchForm.facilities).toEqual([])
  })

  it('应该处理排序变化', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When
    await wrapper.find('[data-test="sort-by"]').setValue('PRICE')
    await wrapper.find('[data-test="sort-order"]').setValue('DESC')

    // Then
    expect(vm.searchForm.sortBy).toBe('PRICE')
    expect(vm.searchForm.sortOrder).toBe('DESC')
  })

  it('应该设置快速日期选择', async () => {
    // Given
    wrapper = mount(RoomSearch)
    const vm = wrapper.vm as any

    // When - 点击"本周末"
    await vm.setQuickDates('weekend')

    // Then
    expect(vm.searchForm.checkInDate).toBeDefined()
    expect(vm.searchForm.checkOutDate).toBeDefined()
  })
})