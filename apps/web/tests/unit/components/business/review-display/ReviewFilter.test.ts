import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import {
  ElSelect,
  ElOption,
  ElCheckbox,
  ElButton,
  ElSlider,
  ElDatePicker
} from 'element-plus'
import ReviewFilter from '@/components/business/ReviewFilter.vue'

describe('ReviewFilter.vue', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(ReviewFilter, {
      props: {
        modelValue: {
          minRating: null,
          maxRating: null,
          hasImages: null,
          dateRange: null
        }
      },
      global: {
        components: {
          ElSelect,
          ElOption,
          ElCheckbox,
          ElButton,
          ElSlider,
          ElDatePicker
        }
      }
    })
  })

  it('renders filter options correctly', () => {
    expect(wrapper.find('.review-filter').exists()).toBe(true)
    expect(wrapper.find('.filter-section--rating').exists()).toBe(true)
    expect(wrapper.find('.filter-section--images').exists()).toBe(true)
    expect(wrapper.find('.filter-section--date').exists()).toBe(true)
    expect(wrapper.find('.filter-actions').exists()).toBe(true)
  })

  it('emits update:modelValue when rating changes', async () => {
    const minRatingSelect = wrapper.find('[data-testid="min-rating"]')
    await minRatingSelect.setValue(4)

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0][0]).toMatchObject({
      minRating: 4
    })
  })

  it('emits update:modelValue when max rating changes', async () => {
    const maxRatingSelect = wrapper.find('[data-testid="max-rating"]')
    await maxRatingSelect.setValue(5)

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0][0]).toMatchObject({
      maxRating: 5
    })
  })

  it('emits update:modelValue when hasImages checkbox changes', async () => {
    const hasImagesCheckbox = wrapper.find('[data-testid="has-images"]')
    await hasImagesCheckbox.setChecked(true)

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0][0]).toMatchObject({
      hasImages: true
    })
  })

  it('emits update:modelValue when date range changes', async () => {
    const dateRangePicker = wrapper.find('[data-testid="date-range"]')
    const testDateRange = ['2024-01-01', '2024-12-31']

    await dateRangePicker.setValue(testDateRange)

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0][0]).toMatchObject({
      dateRange: testDateRange
    })
  })

  it('emits filter-change event when apply button is clicked', async () => {
    const applyButton = wrapper.find('[data-testid="apply-filters"]')

    // 修改一些过滤器值
    await wrapper.setData({
      internalFilters: {
        minRating: 4,
        maxRating: 5,
        hasImages: true,
        dateRange: null
      }
    })

    await applyButton.trigger('click')

    expect(wrapper.emitted('filter-change')).toBeTruthy()
    expect(wrapper.emitted('filter-change')[0][0]).toMatchObject({
      minRating: 4,
      maxRating: 5,
      hasImages: true
    })
  })

  it('resets filters when reset button is clicked', async () => {
    const resetButton = wrapper.find('[data-testid="reset-filters"]')

    // 先设置一些过滤器值
    await wrapper.setData({
      internalFilters: {
        minRating: 4,
        maxRating: 5,
        hasImages: true,
        dateRange: ['2024-01-01', '2024-12-31']
      }
    })

    await resetButton.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0][0]).toMatchObject({
      minRating: null,
      maxRating: null,
      hasImages: null,
      dateRange: null
    })
  })

  it('shows active filter count when filters are applied', async () => {
    // 设置一些过滤器值
    await wrapper.setData({
      internalFilters: {
        minRating: 4,
        maxRating: null,
        hasImages: true,
        dateRange: null
      }
    })

    expect(wrapper.vm.activeFilterCount).toBe(2)
    expect(wrapper.find('.filter-badge').exists()).toBe(true)
    expect(wrapper.find('.filter-badge').text()).toBe('2')
  })

  it('hides filter badge when no filters are active', () => {
    expect(wrapper.vm.activeFilterCount).toBe(0)
    expect(wrapper.find('.filter-badge').exists()).toBe(false)
  })

  it('renders rating slider as alternative to select dropdown', async () => {
    wrapper = mount(ReviewFilter, {
      props: {
        modelValue: {
          minRating: null,
          maxRating: null,
          hasImages: null,
          dateRange: null
        },
        useSlider: true
      },
      global: {
        components: {
          ElSelect,
          ElOption,
          ElCheckbox,
          ElButton,
          ElSlider,
          ElDatePicker
        }
      }
    })

    expect(wrapper.find('[data-testid="rating-slider"]').exists()).toBe(true)
    expect(wrapper.findAllComponents(ElSlider)).toHaveLength(1)
  })

  it('handles rating slider changes', async () => {
    wrapper = mount(ReviewFilter, {
      props: {
        modelValue: {
          minRating: null,
          maxRating: null,
          hasImages: null,
          dateRange: null
        },
        useSlider: true
      },
      global: {
        components: {
          ElSelect,
          ElOption,
          ElCheckbox,
          ElButton,
          ElSlider,
          ElDatePicker
        }
      }
    })

    const ratingSlider = wrapper.findComponent(ElSlider)
    await ratingSlider.vm.$emit('input', [3, 5])

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0][0]).toMatchObject({
      minRating: 3,
      maxRating: 5
    })
  })

  it('collapses and expands filter sections', async () => {
    const collapseButton = wrapper.find('[data-testid="collapse-toggle"]')

    // 初始状态应该是展开的
    expect(wrapper.find('.filter-sections').classes()).not.toContain('collapsed')

    await collapseButton.trigger('click')

    expect(wrapper.find('.filter-sections').classes()).toContain('collapsed')
    expect(collapseButton.find('.collapse-icon').classes()).toContain('collapsed')
  })

  it('shows quick filter presets', async () => {
    const presetButtons = wrapper.findAll('.filter-preset')
    expect(presetButtons.length).toBeGreaterThan(0)

    // 测试"最近一周"预设
    const recentButton = wrapper.find('[data-testid="preset-recent"]')
    await recentButton.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    const emittedValue = wrapper.emitted('update:modelValue')[0][0]
    expect(emittedValue.dateRange).toBeTruthy()
    expect(emittedValue.dateRange).toHaveLength(2)
  })

  it('validates rating range when filters are applied', async () => {
    // 设置最小评分大于最大评分
    await wrapper.setData({
      internalFilters: {
        minRating: 5,
        maxRating: 3,
        hasImages: null,
        dateRange: null
      }
    })

    const applyButton = wrapper.find('[data-testid="apply-filters"]')
    await applyButton.trigger('click')

    // 应该显示错误消息
    expect(wrapper.find('.filter-error').exists()).toBe(true)
    expect(wrapper.find('.filter-error').text()).toContain('最小评分不能大于最大评分')
  })

  it('handles mobile responsive layout', async () => {
    // Mock mobile viewport
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 375,
    })

    wrapper = mount(ReviewFilter, {
      props: {
        modelValue: {
          minRating: null,
          maxRating: null,
          hasImages: null,
          dateRange: null
        }
      },
      global: {
        components: {
          ElSelect,
          ElOption,
          ElCheckbox,
          ElButton,
          ElSlider,
          ElDatePicker
        }
      }
    })

    await wrapper.vm.$nextTick()

    expect(wrapper.find('.review-filter').classes()).toContain('mobile')
    expect(wrapper.find('.filter-actions').classes()).toContain('mobile-layout')
  })

  it('syncs with prop changes', async () => {
    const newFilters = {
      minRating: 3,
      maxRating: 5,
      hasImages: false,
      dateRange: ['2024-01-01', '2024-01-31']
    }

    await wrapper.setProps({ modelValue: newFilters })

    expect(wrapper.vm.internalFilters).toEqual(newFilters)
  })

  it('preserves filters when collapsed', async () => {
    // 设置一些过滤器
    await wrapper.setData({
      internalFilters: {
        minRating: 4,
        maxRating: 5,
        hasImages: true,
        dateRange: null
      }
    })

    // 折叠过滤器
    const collapseButton = wrapper.find('[data-testid="collapse-toggle"]')
    await collapseButton.trigger('click')

    expect(wrapper.find('.filter-sections').classes()).toContain('collapsed')
    expect(wrapper.vm.internalFilters.minRating).toBe(4)
    expect(wrapper.vm.internalFilters.hasImages).toBe(true)
  })

  it('shows clear all button when filters are active', async () => {
    // 设置一些过滤器值
    await wrapper.setData({
      internalFilters: {
        minRating: 4,
        hasImages: true,
        dateRange: null
      }
    })

    const clearButton = wrapper.find('[data-testid="clear-all"]')
    expect(clearButton.exists()).toBe(true)

    await clearButton.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0][0]).toMatchObject({
      minRating: null,
      maxRating: null,
      hasImages: null,
      dateRange: null
    })
  })

  it('supports custom filter options', async () => {
    wrapper = mount(ReviewFilter, {
      props: {
        modelValue: {
          minRating: null,
          maxRating: null,
          hasImages: null,
          dateRange: null
        },
        customFilters: [
          {
            key: 'verified',
            label: '已验证评价',
            type: 'checkbox',
            options: [
              { label: '仅显示已验证评价', value: true }
            ]
          }
        ]
      },
      global: {
        components: {
          ElSelect,
          ElOption,
          ElCheckbox,
          ElButton,
          ElSlider,
          ElDatePicker
        }
      }
    })

    expect(wrapper.find('[data-testid="filter-verified"]').exists()).toBe(true)
  })
})