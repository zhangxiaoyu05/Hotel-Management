import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ReviewForm from '@/components/business/ReviewForm.vue'
import { createTestingPinia } from '@pinia/testing'

// Mock the review service
vi.mock('@/services/reviewService', () => ({
  default: {
    submitReview: vi.fn(),
    canReviewOrder: vi.fn()
  }
}))

describe('ReviewForm.vue', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(ReviewForm, {
      global: {
        plugins: [createTestingPinia()]
      },
      props: {
        orderId: 1,
        hotelName: '测试酒店'
      }
    })
  })

  it('renders form correctly', () => {
    expect(wrapper.find('.review-form').exists()).toBe(true)
    expect(wrapper.find('h3').text()).toContain('测试酒店')
    expect(wrapper.findAllComponents({ name: 'RatingStars' })).toHaveLength(5) // 5 rating categories
    expect(wrapper.find('textarea[placeholder="请分享您的入住体验..."]').exists()).toBe(true)
  })

  it('validates required fields on submit', async () => {
    const submitButton = wrapper.find('button[type="submit"]')
    await submitButton.trigger('click')

    // Should show validation errors
    expect(wrapper.find('.error-message').exists()).toBe(true)
  })

  it('updates rating values when stars are clicked', async () => {
    const overallRating = wrapper.findAllComponents({ name: 'RatingStars' })[0]
    await overallRating.vm.$emit('update:modelValue', 5)

    expect(wrapper.vm.form.overallRating).toBe(5)
  })

  it('updates comment when textarea changes', async () => {
    const textarea = wrapper.find('textarea')
    await textarea.setValue('很好的入住体验')

    expect(wrapper.vm.form.comment).toBe('很好的入住体验')
  })

  it('toggles anonymous option', async () => {
    const anonymousCheckbox = wrapper.find('input[type="checkbox"]')
    await anonymousCheckbox.setChecked()

    expect(wrapper.vm.form.isAnonymous).toBe(true)
  })

  it('emits submit event with valid data', async () => {
    // Fill in form with valid data
    const ratings = wrapper.findAllComponents({ name: 'RatingStars' })
    for (let i = 0; i < ratings.length; i++) {
      await ratings[i].vm.$emit('update:modelValue', 5)
    }

    const textarea = wrapper.find('textarea')
    await textarea.setValue('非常好的体验')

    const submitButton = wrapper.find('button[type="submit"]')
    await submitButton.trigger('click')

    expect(wrapper.emitted('submit')).toBeTruthy()
    expect(wrapper.emitted('submit')[0][0]).toMatchObject({
      orderId: 1,
      overallRating: 5,
      cleanlinessRating: 5,
      serviceRating: 5,
      facilitiesRating: 5,
      locationRating: 5,
      comment: '非常好的体验',
      isAnonymous: false,
      images: []
    })
  })

  it('prevents submission with invalid ratings', async () => {
    // Set invalid rating (0)
    const ratings = wrapper.findAllComponents({ name: 'RatingStars' })
    await ratings[0].vm.$emit('update:modelValue', 0)

    const textarea = wrapper.find('textarea')
    await textarea.setValue('测试评价')

    const submitButton = wrapper.find('button[type="submit"]')
    await submitButton.trigger('click')

    expect(wrapper.emitted('submit')).toBeFalsy()
    expect(wrapper.find('.error-message').text()).toContain('请为所有维度评分')
  })

  it('prevents submission with empty comment', async () => {
    const ratings = wrapper.findAllComponents({ name: 'RatingStars' })
    for (let i = 0; i < ratings.length; i++) {
      await ratings[i].vm.$emit('update:modelValue', 4)
    }

    const submitButton = wrapper.find('button[type="submit"]')
    await submitButton.trigger('click')

    expect(wrapper.emitted('submit')).toBeFalsy()
    expect(wrapper.find('.error-message').text()).toContain('请输入评价内容')
  })

  it('handles image uploads', async () => {
    const imageUpload = wrapper.findComponent({ name: 'ImageUpload' })
    const mockImages = [
      'data:image/jpeg;base64,testimage1',
      'data:image/jpeg;base64,testimage2'
    ]

    await imageUpload.vm.$emit('update:images', mockImages)

    expect(wrapper.vm.form.images).toEqual(mockImages)
  })

  it('shows loading state during submission', async () => {
    // Fill valid form
    const ratings = wrapper.findAllComponents({ name: 'RatingStars' })
    for (let i = 0; i < ratings.length; i++) {
      await ratings[i].vm.$emit('update:modelValue', 5)
    }

    const textarea = wrapper.find('textarea')
    await textarea.setValue('测试评价')

    // Mock loading state
    wrapper.setData({ loading: true })

    const submitButton = wrapper.find('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()
    expect(submitButton.text()).toContain('提交中...')
  })

  it('resets form after successful submission', async () => {
    // Fill and submit form
    const ratings = wrapper.findAllComponents({ name: 'RatingStars' })
    for (let i = 0; i < ratings.length; i++) {
      await ratings[i].vm.$emit('update:modelValue', 5)
    }

    const textarea = wrapper.find('textarea')
    await textarea.setValue('测试评价')

    // Mock successful submission
    wrapper.vm.resetForm()

    expect(wrapper.vm.form.overallRating).toBe(0)
    expect(wrapper.vm.form.comment).toBe('')
    expect(wrapper.vm.form.images).toEqual([])
  })

  it('validates comment length', async () => {
    const longComment = 'a'.repeat(1001) // Exceeds 1000 character limit
    const textarea = wrapper.find('textarea')
    await textarea.setValue(longComment)

    const submitButton = wrapper.find('button[type="submit"]')
    await submitButton.trigger('click')

    expect(wrapper.emitted('submit')).toBeFalsy()
    expect(wrapper.find('.error-message').text()).toContain('评价内容不能超过1000字符')
  })

  it('shows preview mode when enabled', async () => {
    await wrapper.setProps({ previewMode: true })

    expect(wrapper.find('.review-preview').exists()).toBe(true)
    expect(wrapper.find('.review-form').exists()).toBe(false)
  })

  it('emits cancel event when cancel button is clicked', async () => {
    const cancelButton = wrapper.find('button[type="button"]')
    await cancelButton.trigger('click')

    expect(wrapper.emitted('cancel')).toBeTruthy()
  })
})