import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElButton, ElTag, ElCard, ElImage } from 'element-plus'
import RoomTypeCard from '../../../../src/components/room-type/RoomTypeCard.vue'
import type { RoomType } from '../../../../src/types/roomType'

// Mock Element Plus icons
vi.mock('@element-plus/icons-vue', () => ({
  Edit: { name: 'Edit' },
  Delete: { name: 'Delete' },
  View: { name: 'View' },
  Promotion: { name: 'Promotion' }
}))

describe('RoomTypeCard.vue', () => {
  let mockRoomType: RoomType

  beforeEach(() => {
    mockRoomType = {
      id: 1,
      hotelId: 1,
      name: '标准间',
      capacity: 2,
      basePrice: 299,
      facilities: ['WiFi', '空调', '电视'],
      description: '舒适的标准间',
      iconUrl: 'https://example.com/room-icon.png',
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    }
  })

  it('正确渲染房间类型信息', () => {
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: mockRoomType,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    expect(wrapper.find('.room-type-name').text()).toBe('标准间')
    expect(wrapper.find('.room-type-capacity').text()).toContain('2人')
    expect(wrapper.find('.room-type-price').text()).toContain('¥299')
    expect(wrapper.find('.room-type-description').text()).toBe('舒适的标准间')
  })

  it('显示正确的状态标签', () => {
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: mockRoomType,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    const statusTag = wrapper.find('.status-tag')
    expect(statusTag.exists()).toBe(true)
    expect(statusTag.text()).toBe('营业中')
  })

  it('在非管理员模式下不显示操作按钮', () => {
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: mockRoomType,
        adminMode: false
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    const actionButtons = wrapper.findAll('.action-buttons button')
    expect(actionButtons.length).toBe(0)
  })

  it('在管理员模式下显示所有操作按钮', () => {
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: mockRoomType,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    const actionButtons = wrapper.findAll('.action-buttons button')
    expect(actionButtons.length).toBe(4) // 查看、编辑、删除、切换状态
  })

  it('正确渲染设施标签', async () => {
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: mockRoomType,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    await wrapper.vm.$nextTick()
    const facilityTags = wrapper.findAll('.facility-tag')
    expect(facilityTags.length).toBe(3)
    expect(facilityTags[0].text()).toBe('WiFi')
    expect(facilityTags[1].text()).toBe('空调')
    expect(facilityTags[2].text()).toBe('电视')
  })

  it('处理编辑按钮点击事件', async () => {
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: mockRoomType,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    await wrapper.vm.$nextTick()
    const editButton = wrapper.find('[data-test="edit-button"]')
    await editButton.trigger('click')

    expect(wrapper.emitted('edit')).toBeTruthy()
    expect(wrapper.emitted('edit')[0]).toEqual([mockRoomType])
  })

  it('处理删除按钮点击事件', async () => {
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: mockRoomType,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    await wrapper.vm.$nextTick()
    const deleteButton = wrapper.find('[data-test="delete-button"]')
    await deleteButton.trigger('click')

    expect(wrapper.emitted('delete')).toBeTruthy()
    expect(wrapper.emitted('delete')[0]).toEqual([mockRoomType])
  })

  it('处理状态切换按钮点击事件', async () => {
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: mockRoomType,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    await wrapper.vm.$nextTick()
    const toggleButton = wrapper.find('[data-test="toggle-status-button"]')
    await toggleButton.trigger('click')

    expect(wrapper.emitted('toggle-status')).toBeTruthy()
    expect(wrapper.emitted('toggle-status')[0]).toEqual([mockRoomType])
  })

  it('处理查看按钮点击事件', async () => {
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: mockRoomType,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    await wrapper.vm.$nextTick()
    const viewButton = wrapper.find('[data-test="view-button"]')
    await viewButton.trigger('click')

    expect(wrapper.emitted('view')).toBeTruthy()
    expect(wrapper.emitted('view')[0]).toEqual([mockRoomType])
  })

  it('在没有图标时显示默认图标', () => {
    const roomTypeWithoutIcon = { ...mockRoomType, iconUrl: '' }
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: roomTypeWithoutIcon,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    const defaultIcon = wrapper.find('.default-icon')
    expect(defaultIcon.exists()).toBe(true)
  })

  it('正确格式化价格显示', () => {
    const roomTypeWithDecimalPrice = { ...mockRoomType, basePrice: 299.5 }
    const wrapper = mount(RoomTypeCard, {
      props: {
        roomType: roomTypeWithDecimalPrice,
        adminMode: true
      },
      global: {
        components: {
          ElCard,
          ElImage,
          ElTag,
          ElButton
        }
      }
    })

    expect(wrapper.find('.room-type-price').text()).toContain('¥299.50')
  })
})