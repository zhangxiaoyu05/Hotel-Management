import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import RoomCard from '@/components/business/RoomCard.vue'

// Mock room data factory
const createMockRoom = (overrides = {}) => ({
  id: 1,
  roomNumber: '101',
  hotelName: '测试酒店',
  hotelAddress: '测试地址',
  hotelRating: 4.5,
  roomTypeName: '标准间',
  roomTypeCapacity: 2,
  area: 25.0,
  price: 299.0,
  images: ['/images/room1.jpg', '/images/room2.jpg'],
  facilities: ['WiFi', '空调', '电视'],
  status: 'AVAILABLE',
  ...overrides
})

describe('RoomCard.vue', () => {
  it('应该正确渲染房间基本信息', () => {
    // Given
    const mockRoom = createMockRoom()

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    expect(wrapper.find('.room-number').text()).toContain('101')
    expect(wrapper.find('.room-type').text()).toContain('标准间')
    expect(wrapper.find('.hotel-name').text()).toContain('测试酒店')
    expect(wrapper.find('.room-area').text()).toContain('25㎡')
    expect(wrapper.find('.room-price').text()).toContain('299')
  })

  it('应该显示房间设施', () => {
    // Given
    const mockRoom = createMockRoom({
      facilities: ['WiFi', '空调', '电视', '迷你吧']
    })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    const facilities = wrapper.findAll('.facility-tag')
    expect(facilities).toHaveLength(4)
    expect(facilities[0].text()).toBe('WiFi')
    expect(facilities[1].text()).toBe('空调')
    expect(facilities[2].text()).toBe('电视')
    expect(facilities[3].text()).toBe('迷你吧')
  })

  it('应该正确格式化价格显示', () => {
    // Given
    const mockRoom = createMockRoom({ price: 299.0 })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    expect(wrapper.find('.price-value').text()).toBe('299')
    expect(wrapper.find('.price-unit').text()).toBe('元/晚')
  })

  it('应该显示酒店评分', () => {
    // Given
    const mockRoom = createMockRoom({ hotelRating: 4.5 })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    expect(wrapper.find('.hotel-rating').text()).toContain('4.5')
  })

  it('应该显示房间状态', () => {
    // Given
    const mockRoom = createMockRoom({ status: 'AVAILABLE' })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    const statusBadge = wrapper.find('.room-status')
    expect(statusBadge.text()).toContain('可预订')
    expect(statusBadge.classes()).toContain('status-available')
  })

  it('应该显示维护状态', () => {
    // Given
    const mockRoom = createMockRoom({ status: 'MAINTENANCE' })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    const statusBadge = wrapper.find('.room-status')
    expect(statusBadge.text()).toContain('维护中')
    expect(statusBadge.classes()).toContain('status-maintenance')
  })

  it('应该在点击查看详情时触发事件', async () => {
    // Given
    const mockRoom = createMockRoom()
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // When
    await wrapper.find('.view-details-btn').trigger('click')

    // Then
    expect(wrapper.emitted('view-details')).toBeTruthy()
    expect(wrapper.emitted('view-details')![0]).toEqual([mockRoom])
  })

  it('应该在点击立即预订时触发事件', async () => {
    // Given
    const mockRoom = createMockRoom()
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // When
    await wrapper.find('.book-now-btn').trigger('click')

    // Then
    expect(wrapper.emitted('book-now')).toBeTruthy()
    expect(wrapper.emitted('book-now')![0]).toEqual([mockRoom])
  })

  it('应该在房间不可用时禁用预订按钮', () => {
    // Given
    const mockRoom = createMockRoom({ status: 'OCCUPIED' })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    const bookButton = wrapper.find('.book-now-btn')
    expect(bookButton.attributes('disabled')).toBeDefined()
  })

  it('应该处理空图片列表', () => {
    // Given
    const mockRoom = createMockRoom({ images: [] })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    expect(wrapper.find('.room-images').exists()).toBe(true)
  })

  it('应该处理空设施列表', () => {
    // Given
    const mockRoom = createMockRoom({ facilities: [] })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    expect(wrapper.findAll('.facility-tag')).toHaveLength(0)
  })

  it('应该限制显示的设施数量', () => {
    // Given
    const mockRoom = createMockRoom({
      facilities: ['WiFi', '空调', '电视', '迷你吧', '保险箱', '吹风机', '浴袍']
    })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    const facilities = wrapper.findAll('.facility-tag')
    expect(facilities.length).toBeLessThanOrEqual(5)
    // 应该显示"更多"指示器
    expect(wrapper.find('.more-facilities').exists()).toBe(true)
  })

  it('应该响应式更新当props改变时', async () => {
    // Given
    const wrapper = mount(RoomCard, {
      props: { room: createMockRoom() }
    })

    // When
    const newRoom = createMockRoom({
      roomNumber: '202',
      price: 599.0
    })
    await wrapper.setProps({ room: newRoom })

    // Then
    expect(wrapper.find('.room-number').text()).toContain('202')
    expect(wrapper.find('.price-value').text()).toContain('599')
  })

  it('应该显示房间容量信息', () => {
    // Given
    const mockRoom = createMockRoom({ roomTypeCapacity: 3 })

    // When
    const wrapper = mount(RoomCard, {
      props: { room: mockRoom }
    })

    // Then
    expect(wrapper.find('.room-capacity').text()).toContain('3人')
  })
})