import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import RoomTypeList from '../../../../../src/pages/admin/room-types/RoomTypeList.vue'
import RoomTypeCard from '../../../../../src/components/room-type/RoomTypeCard.vue'
import { useRoomTypeStore } from '../../../../../src/stores/roomTypeStore'
import { useHotelStore } from '../../../../../src/stores/hotelStore'

// Mock stores
vi.mock('../../../../../src/stores/roomTypeStore')
vi.mock('../../../../../src/stores/hotelStore')

// Mock components
vi.mock('../../../../../src/components/room-type/RoomTypeCard.vue', () => ({
  default: {
    name: 'RoomTypeCard',
    template: '<div class="mock-room-type-card" @edit="$emit(\'edit\', $event)" @delete="$emit(\'delete\', $event)" @toggle-status="$emit(\'toggleStatus\', $event)" @view="$emit(\'view\', $event)"></div>',
    props: ['roomType', 'adminMode'],
    emits: ['edit', 'delete', 'toggleStatus', 'view']
  }
}))

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      success: vi.fn()
    },
    ElMessageBox: {
      confirm: vi.fn()
    }
  }
})

describe('RoomTypeList.vue', () => {
  let router: any
  let wrapper: any
  let mockRoomTypeStore: any
  let mockHotelStore: any

  beforeEach(async () => {
    // 创建路由
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/admin/room-types', component: { template: '<div></div>' } },
        { path: '/admin/room-types/create', component: { template: '<div></div>' } },
        { path: '/admin/room-types/:id/edit', component: { template: '<div></div>' } },
        { path: '/admin/room-types/:id', component: { template: '<div></div>' } }
      ]
    })

    // Mock stores
    mockRoomTypeStore = {
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
          status: 'ACTIVE',
          createdAt: '2024-01-01T00:00:00Z'
        }
      ],
      pagination: {
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0
      },
      fetchRoomTypes: vi.fn(),
      deleteRoomType: vi.fn(),
      updateRoomTypeStatus: vi.fn()
    }

    mockHotelStore = {
      hotels: [
        { id: 1, name: '测试酒店' },
        { id: 2, name: '另一酒店' }
      ],
      fetchHotels: vi.fn()
    }

    ;(useRoomTypeStore as any).mockReturnValue(mockRoomTypeStore)
    ;(useHotelStore as any).mockReturnValue(mockHotelStore)

    wrapper = mount(RoomTypeList, {
      global: {
        plugins: [router],
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
          RoomTypeCard: true
        }
      }
    })

    await router.isReady()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('正确初始化页面状态', () => {
    expect(wrapper.vm.searchQuery).toBe('')
    expect(wrapper.vm.selectedStatus).toBe('')
    expect(wrapper.vm.selectedHotel).toBeUndefined()
    expect(wrapper.vm.currentPage).toBe(1)
    expect(wrapper.vm.pageSize).toBe(20)
    expect(wrapper.vm.sortBy).toBe('createdAt')
    expect(wrapper.vm.sortDirection).toBe('DESC')
  })

  it('在挂载时加载数据', () => {
    expect(mockHotelStore.fetchHotels).toHaveBeenCalledWith({ page: 0, size: 1000 })
    expect(mockRoomTypeStore.fetchRoomTypes).toHaveBeenCalled()
  })

  it('正确构建查询参数', () => {
    wrapper.vm.searchQuery = '测试'
    wrapper.vm.selectedHotel = 1
    wrapper.vm.selectedStatus = 'ACTIVE'
    wrapper.vm.currentPage = 2
    wrapper.vm.pageSize = 10
    wrapper.vm.sortBy = 'name'
    wrapper.vm.sortDirection = 'ASC'

    const query = wrapper.vm.query
    expect(query).toEqual({
      page: 1,
      size: 10,
      search: '测试',
      hotelId: 1,
      status: 'ACTIVE',
      sortBy: 'name',
      sortDir: 'ASC'
    })
  })

  it('处理搜索操作', () => {
    wrapper.vm.searchQuery = '新搜索'
    wrapper.vm.handleSearch()
    expect(wrapper.vm.currentPage).toBe(1)
    expect(mockRoomTypeStore.fetchRoomTypes).toHaveBeenCalled()
  })

  it('处理状态筛选', () => {
    wrapper.vm.selectedStatus = 'INACTIVE'
    wrapper.vm.handleStatusFilter()
    expect(wrapper.vm.currentPage).toBe(1)
    expect(mockRoomTypeStore.fetchRoomTypes).toHaveBeenCalled()
  })

  it('处理酒店筛选', () => {
    wrapper.vm.selectedHotel = 2
    wrapper.vm.handleHotelFilter()
    expect(wrapper.vm.currentPage).toBe(1)
    expect(mockRoomTypeStore.fetchRoomTypes).toHaveBeenCalled()
  })

  it('处理排序', () => {
    wrapper.vm.sortBy = 'basePrice'
    wrapper.vm.handleSort()
    expect(mockRoomTypeStore.fetchRoomTypes).toHaveBeenCalled()
  })

  it('处理分页变更', () => {
    wrapper.vm.handlePageChange(3)
    expect(wrapper.vm.currentPage).toBe(3)
    expect(mockRoomTypeStore.fetchRoomTypes).toHaveBeenCalled()
  })

  it('处理页面大小变更', () => {
    wrapper.vm.handleSizeChange(50)
    expect(wrapper.vm.pageSize).toBe(50)
    expect(wrapper.vm.currentPage).toBe(1)
    expect(mockRoomTypeStore.fetchRoomTypes).toHaveBeenCalled()
  })

  it('导航到创建页面', () => {
    wrapper.vm.handleCreateRoomType()
    expect(router.currentRoute.value.path).toBe('/admin/room-types/create')
  })

  it('导航到编辑页面', () => {
    const roomType = mockRoomTypeStore.roomTypes[0]
    wrapper.vm.handleEditRoomType(roomType)
    expect(router.currentRoute.value.path).toBe('/admin/room-types/1/edit')
  })

  it('导航到详情页面', () => {
    const roomType = mockRoomTypeStore.roomTypes[0]
    wrapper.vm.handleViewRoomType(roomType)
    expect(router.currentRoute.value.path).toBe('/admin/room-types/1')
  })

  describe('删除房间类型', () => {
    it('成功删除房间类型', async () => {
      ;(ElMessageBox.confirm as any).mockResolvedValue('confirm')
      mockRoomTypeStore.deleteRoomType.mockResolvedValue(undefined)

      const roomType = mockRoomTypeStore.roomTypes[0]
      await wrapper.vm.handleDeleteRoomType(roomType)

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        `确定要删除房间类型"${roomType.name}"吗？此操作不可恢复。`,
        '确认删除',
        expect.any(Object)
      )
      expect(mockRoomTypeStore.deleteRoomType).toHaveBeenCalledWith(roomType.id)
      expect(ElMessage.success).toHaveBeenCalledWith('房间类型删除成功')
      expect(mockRoomTypeStore.fetchRoomTypes).toHaveBeenCalled()
    })

    it('处理删除取消操作', async () => {
      ;(ElMessageBox.confirm as any).mockRejectedValue('cancel')

      const roomType = mockRoomTypeStore.roomTypes[0]
      await wrapper.vm.handleDeleteRoomType(roomType)

      expect(mockRoomTypeStore.deleteRoomType).not.toHaveBeenCalled()
      expect(ElMessage.error).not.toHaveBeenCalled()
    })

    it('处理删除失败', async () => {
      ;(ElMessageBox.confirm as any).mockResolvedValue('confirm')
      const error = new Error('删除失败')
      mockRoomTypeStore.deleteRoomType.mockRejectedValue(error)

      const roomType = mockRoomTypeStore.roomTypes[0]
      await wrapper.vm.handleDeleteRoomType(roomType)

      expect(ElMessage.error).toHaveBeenCalledWith('删除房间类型失败')
    })
  })

  describe('切换房间类型状态', () => {
    it('成功激活房间类型', async () => {
      ;(ElMessageBox.confirm as any).mockResolvedValue('confirm')
      mockRoomTypeStore.updateRoomTypeStatus.mockResolvedValue(undefined)

      const roomType = { ...mockRoomTypeStore.roomTypes[0], status: 'INACTIVE' }
      await wrapper.vm.handleToggleRoomTypeStatus(roomType)

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        `确定要激活房间类型"${roomType.name}"吗？`,
        '确认激活',
        expect.any(Object)
      )
      expect(mockRoomTypeStore.updateRoomTypeStatus).toHaveBeenCalledWith(roomType.id, 'ACTIVE')
      expect(ElMessage.success).toHaveBeenCalledWith('房间类型激活成功')
      expect(mockRoomTypeStore.fetchRoomTypes).toHaveBeenCalled()
    })

    it('成功停用房间类型', async () => {
      ;(ElMessageBox.confirm as any).mockResolvedValue('confirm')
      mockRoomTypeStore.updateRoomTypeStatus.mockResolvedValue(undefined)

      const roomType = mockRoomTypeStore.roomTypes[0]
      await wrapper.vm.handleToggleRoomTypeStatus(roomType)

      expect(mockRoomTypeStore.updateRoomTypeStatus).toHaveBeenCalledWith(roomType.id, 'INACTIVE')
      expect(ElMessage.success).toHaveBeenCalledWith('房间类型停用成功')
    })
  })

  it('处理加载酒店失败', async () => {
    mockHotelStore.fetchHotels.mockRejectedValue(new Error('加载失败'))
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    await wrapper.vm.loadHotels()

    expect(consoleSpy).toHaveBeenCalledWith('Failed to load hotels:', expect.any(Error))
    consoleSpy.mockRestore()
  })

  it('处理加载房间类型失败', async () => {
    mockRoomTypeStore.fetchRoomTypes.mockRejectedValue(new Error('加载失败'))

    await wrapper.vm.loadRoomTypes()

    expect(ElMessage.error).toHaveBeenCalledWith('加载房间类型列表失败')
  })
})