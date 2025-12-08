import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import OrderStatus from '@/components/business/OrderStatus.vue'

describe('OrderStatus', () => {
  it('renders correctly with pending status', () => {
    const wrapper = mount(OrderStatus, {
      props: {
        status: 'PENDING'
      }
    })

    expect(wrapper.find('.order-status').exists()).toBe(true)
    expect(wrapper.find('.status-pending').exists()).toBe(true)
    expect(wrapper.text()).toContain('待确认')
    expect(wrapper.find('i').classes()).toContain('fa-clock')
  })

  it('renders correctly with confirmed status', () => {
    const wrapper = mount(OrderStatus, {
      props: {
        status: 'CONFIRMED'
      }
    })

    expect(wrapper.find('.status-confirmed').exists()).toBe(true)
    expect(wrapper.text()).toContain('已确认')
    expect(wrapper.find('i').classes()).toContain('fa-check-circle')
  })

  it('renders correctly with completed status', () => {
    const wrapper = mount(OrderStatus, {
      props: {
        status: 'COMPLETED'
      }
    })

    expect(wrapper.find('.status-completed').exists()).toBe(true)
    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.find('i').classes()).toContain('fa-check-double')
  })

  it('renders correctly with cancelled status', () => {
    const wrapper = mount(OrderStatus, {
      props: {
        status: 'CANCELLED'
      }
    })

    expect(wrapper.find('.status-cancelled').exists()).toBe(true)
    expect(wrapper.text()).toContain('已取消')
    expect(wrapper.find('i').classes()).toContain('fa-times-circle')
  })

  it('applies size classes correctly', () => {
    const wrapper = mount(OrderStatus, {
      props: {
        status: 'CONFIRMED',
        size: 'large'
      }
    })

    expect(wrapper.find('.size-large').exists()).toBe(true)
  })

  it('has correct CSS classes for each status', () => {
    const testCases = [
      { status: 'PENDING', expectedClass: 'status-pending' },
      { status: 'CONFIRMED', expectedClass: 'status-confirmed' },
      { status: 'COMPLETED', expectedClass: 'status-completed' },
      { status: 'CANCELLED', expectedClass: 'status-cancelled' }
    ]

    testCases.forEach(({ status, expectedClass }) => {
      const wrapper = mount(OrderStatus, {
        props: { status }
      })

      expect(wrapper.find(`.${expectedClass}`).exists()).toBe(true)
    })
  })
})