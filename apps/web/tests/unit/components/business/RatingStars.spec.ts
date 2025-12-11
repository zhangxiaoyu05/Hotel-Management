import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import RatingStars from '@/components/business/RatingStars.vue'

describe('RatingStars.vue', () => {
  it('renders correct number of stars', () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 3
      }
    })

    const stars = wrapper.findAll('.star')
    expect(stars).toHaveLength(5)
  })

  it('highlights correct number of filled stars', () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 4
      }
    })

    const filledStars = wrapper.findAll('.star.filled')
    expect(filledStars).toHaveLength(4)
  })

  it('displays correct rating labels', () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 5,
        label: '总体评分'
      }
    })

    expect(wrapper.find('.rating-label').text()).toBe('总体评分')
  })

  it('emits update:modelValue when star is clicked', async () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 0
      }
    })

    const stars = wrapper.findAll('.star')
    await stars[2].trigger('click') // Click third star

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([3])
  })

  it('shows hover effect on mouseenter', async () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 0
      }
    })

    const stars = wrapper.findAll('.star')
    await stars[1].trigger('mouseenter')

    expect(stars[0].classes()).toContain('hover')
    expect(stars[1].classes()).toContain('hover')
    expect(stars[2].classes()).not.toContain('hover')
  })

  it('removes hover effect on mouseleave', async () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 0
      }
    })

    const stars = wrapper.findAll('.star')
    await stars[1].trigger('mouseenter')
    await wrapper.trigger('mouseleave')

    stars.forEach(star => {
      expect(star.classes()).not.toContain('hover')
    })
  })

  it('displays value text when showValue is true', () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 4,
        showValue: true
      }
    })

    expect(wrapper.find('.rating-value').text()).toBe('4.0')
  })

  it('does not display value text when showValue is false', () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 4,
        showValue: false
      }
    })

    expect(wrapper.find('.rating-value').exists()).toBe(false)
  })

  it('is readonly when disabled prop is true', async () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 0,
        disabled: true
      }
    })

    const stars = wrapper.findAll('.star')
    await stars[2].trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeFalsy()
  })

  it('applies disabled styling when disabled', () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 3,
        disabled: true
      }
    })

    expect(wrapper.find('.rating-stars').classes()).toContain('disabled')
  })

  it('handles decimal ratings correctly', () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 3.7
      }
    })

    // Should show 3 full stars and 1 partial star
    expect(wrapper.find('.rating-value').text()).toBe('3.7')
  })

  it('validates rating bounds', async () => {
    const wrapper = mount(RatingStars, {
      props: {
        modelValue: 0
      }
    })

    // Test minimum value
    await wrapper.setProps({ modelValue: -1 })
    expect(wrapper.emitted('update:modelValue')).toBeFalsy()

    // Test maximum value
    await wrapper.setProps({ modelValue: 6 })
    expect(wrapper.emitted('update:modelValue')).toBeFalsy()
  })
})