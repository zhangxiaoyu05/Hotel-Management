import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElCard, ElTag, ElButton, ElImage } from 'element-plus'
import HotelCard from '@/components/hotel/HotelCard.vue'
import type { Hotel } from '@/services/hotelService'

// Mock Element Plus components
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElCard: { template: '<div class="el-card"><slot /></div>' },
    ElTag: {
      template: '<span class="el-tag" :class="`el-tag--${type}`"><slot /></span>',
      props: ['type', 'size']
    },
    ElButton: {
      template: '<button class="el-button" :class="`el-button--${type}`" @click="$emit(\'click\')"><slot /></button>',
      props: ['type', 'size', 'icon']
    },
    ElImage: {
      template: '<img class="el-image" :src="src" :alt="alt" />',
      props: ['src', 'alt', 'fit']
    }
  }
})

describe('HotelCard.vue', () => {
  const mockHotel: Hotel = {
    id: 1,
    name: 'Test Hotel',
    address: '123 Test Street, Test City',
    phone: '13800138001',
    description: 'This is a beautiful test hotel with all modern amenities.',
    facilities: ['WiFi', 'Parking', 'Pool', 'Gym'],
    images: ['hotel1.jpg', 'hotel2.jpg'],
    status: 'ACTIVE',
    createdBy: 1,
    createdAt: '2025-12-06T10:00:00Z',
    updatedAt: '2025-12-06T10:00:00Z'
  }

  it('renders hotel information correctly', () => {
    const wrapper = mount(HotelCard, {
      props: {
        hotel: mockHotel
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    expect(wrapper.text()).toContain(mockHotel.name)
    expect(wrapper.text()).toContain(mockHotel.address)
    expect(wrapper.text()).toContain(mockHotel.phone)
    expect(wrapper.text()).toContain(mockHotel.description)
  })

  it('renders hotel status correctly', () => {
    const wrapper = mount(HotelCard, {
      props: {
        hotel: mockHotel
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const statusTag = wrapper.find('.el-tag--success')
    expect(statusTag.exists()).toBe(true)
    expect(statusTag.text()).toContain('运营中')
  })

  it('renders inactive status correctly', () => {
    const inactiveHotel = { ...mockHotel, status: 'INACTIVE' as const }

    const wrapper = mount(HotelCard, {
      props: {
        hotel: inactiveHotel
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const statusTag = wrapper.find('.el-tag--danger')
    expect(statusTag.exists()).toBe(true)
    expect(statusTag.text()).toContain('已停业')
  })

  it('renders hotel facilities correctly', () => {
    const wrapper = mount(HotelCard, {
      props: {
        hotel: mockHotel
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const facilityTags = wrapper.findAll('.el-tag--info')
    expect(facilityTags).toHaveLength(mockHotel.facilities.length)

    mockHotel.facilities.forEach((facility, index) => {
      expect(facilityTags[index].text()).toContain(facility)
    })
  })

  it('renders hotel images correctly', () => {
    const wrapper = mount(HotelCard, {
      props: {
        hotel: mockHotel
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const images = wrapper.findAll('.el-image')
    expect(images).toHaveLength(mockHotel.images.length)

    mockHotel.images.forEach((image, index) => {
      expect(images[index].attributes('src')).toBe(image)
    })
  })

  it('renders placeholder when no images', () => {
    const hotelWithoutImages = { ...mockHotel, images: [] }

    const wrapper = mount(HotelCard, {
      props: {
        hotel: hotelWithoutImages
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const placeholder = wrapper.find('.hotel-card__no-image')
    expect(placeholder.exists()).toBe(true)
    expect(placeholder.text()).toContain('暂无图片')
  })

  it('emits edit event when edit button clicked', async () => {
    const wrapper = mount(HotelCard, {
      props: {
        hotel: mockHotel
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const editButton = wrapper.find('.el-button--primary')
    await editButton.trigger('click')

    expect(wrapper.emitted('edit')).toBeTruthy()
    expect(wrapper.emitted('edit')?.[0]).toEqual([mockHotel])
  })

  it('emits delete event when delete button clicked', async () => {
    const wrapper = mount(HotelCard, {
      props: {
        hotel: mockHotel
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const deleteButton = wrapper.find('.el-button--danger')
    await deleteButton.trigger('click')

    expect(wrapper.emitted('delete')).toBeTruthy()
    expect(wrapper.emitted('delete')?.[0]).toEqual([mockHotel])
  })

  it('emits status-change event when status toggle clicked', async () => {
    const wrapper = mount(HotelCard, {
      props: {
        hotel: mockHotel
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const statusButton = wrapper.find('.hotel-card__status-toggle')
    await statusButton.trigger('click')

    expect(wrapper.emitted('status-change')).toBeTruthy()
    expect(wrapper.emitted('status-change')?.[0]).toEqual([mockHotel])
  })

  it('formats date correctly', () => {
    const wrapper = mount(HotelCard, {
      props: {
        hotel: mockHotel
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    expect(wrapper.text()).toContain('2025-12-06')
  })

  it('limits facilities display', () => {
    const hotelWithManyFacilities = {
      ...mockHotel,
      facilities: ['WiFi', 'Parking', 'Pool', 'Gym', 'Spa', 'Restaurant', 'Bar', 'Laundry']
    }

    const wrapper = mount(HotelCard, {
      props: {
        hotel: hotelWithManyFacilities
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const facilityTags = wrapper.findAll('.el-tag--info')
    // Should show max 4 facilities
    expect(facilityTags.length).toBeLessThanOrEqual(4)
  })

  it('shows more facilities indicator when there are more facilities', () => {
    const hotelWithManyFacilities = {
      ...mockHotel,
      facilities: ['WiFi', 'Parking', 'Pool', 'Gym', 'Spa']
    }

    const wrapper = mount(HotelCard, {
      props: {
        hotel: hotelWithManyFacilities
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    expect(wrapper.text()).toContain('+1') // Shows there's 1 more facility
  })

  it('truncates long description', () => {
    const hotelWithLongDescription = {
      ...mockHotel,
      description: 'This is a very long description that should be truncated because it exceeds the maximum length allowed for display in the hotel card component. '.repeat(5)
    }

    const wrapper = mount(HotelCard, {
      props: {
        hotel: hotelWithLongDescription
      },
      global: {
        components: {
          ElCard,
          ElTag,
          ElButton,
          ElImage
        }
      }
    })

    const descriptionElement = wrapper.find('.hotel-card__description')
    expect(descriptionElement.text().length).toBeLessThan(hotelWithLongDescription.description.length)
    expect(descriptionElement.text()).toContain('...')
  })
})