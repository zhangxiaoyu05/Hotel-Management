import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElRate, ElButton, ElTag } from 'element-plus'
import ReviewCard from '@/components/business/ReviewCard.vue'
import OptimizedImage from '@/components/business/OptimizedImage.vue'

// Mock OptimizedImage component
vi.mock('@/components/business/OptimizedImage.vue', () => ({
  default: {
    name: 'OptimizedImage',
    template: '<div class="optimized-image"><slot /></div>',
    props: ['src', 'alt', 'lazy', 'width', 'height']
  }
}))

const mockReview = {
  id: 1,
  userId: 1,
  username: 'testuser',
  overallRating: 4,
  cleanlinessRating: 5,
  serviceRating: 4,
  facilitiesRating: 3,
  locationRating: 5,
  comment: '这是一个很好的酒店，服务很棒，位置也很便利。',
  images: ['image1.jpg', 'image2.jpg'],
  isAnonymous: false,
  createdAt: '2024-01-15T10:30:00Z'
}

const mockAnonymousReview = {
  ...mockReview,
  isAnonymous: true,
  username: '匿名用户'
}

const mockReviewWithoutImages = {
  ...mockReview,
  images: null
}

describe('ReviewCard.vue', () => {
  it('renders review information correctly', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    // 检查用户名显示
    expect(wrapper.find('.review-card__username').text()).toBe('testuser')

    // 检查总体评分
    expect(wrapper.find('.review-card__overall-rating').exists()).toBe(true)

    // 检查评价内容
    expect(wrapper.find('.review-card__comment').text()).toBe(mockReview.comment)

    // 检查创建时间
    expect(wrapper.find('.review-card__date').exists()).toBe(true)
  })

  it('displays anonymous user correctly', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockAnonymousReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    expect(wrapper.find('.review-card__username').text()).toBe('匿名用户')
    expect(wrapper.find('.review-card__anonymous-badge').exists()).toBe(true)
  })

  it('displays dimensional ratings correctly', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    const ratings = wrapper.findAll('.dimensional-rating')
    expect(ratings).toHaveLength(4)

    // 检查各项评分
    const cleanlinessRating = wrapper.find('[data-testid="cleanliness-rating"]')
    const serviceRating = wrapper.find('[data-testid="service-rating"]')
    const facilitiesRating = wrapper.find('[data-testid="facilities-rating"]')
    const locationRating = wrapper.find('[data-testid="location-rating"]')

    expect(cleanlinessRating.exists()).toBe(true)
    expect(serviceRating.exists()).toBe(true)
    expect(facilitiesRating.exists()).toBe(true)
    expect(locationRating.exists()).toBe(true)
  })

  it('renders images when review has images', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    expect(wrapper.find('.review-card__images').exists()).toBe(true)
    expect(wrapper.findAllComponents(OptimizedImage)).toHaveLength(2)
  })

  it('does not render image section when review has no images', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockReviewWithoutImages },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    expect(wrapper.find('.review-card__images').exists()).toBe(false)
    expect(wrapper.findAllComponents(OptimizedImage)).toHaveLength(0)
  })

  it('formats date correctly', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    const dateElement = wrapper.find('.review-card__date')
    expect(dateElement.exists()).toBe(true)
    // 验证日期格式（根据实际格式化实现调整）
    expect(dateElement.text()).toMatch(/\d{4}年\d{1,2}月\d{1,2}日/)
  })

  it('truncates long comments and shows expand button', async () => {
    const longCommentReview = {
      ...mockReview,
      comment: '这是一个非常长的评价内容。'.repeat(20)
    }

    const wrapper = mount(ReviewCard, {
      props: { review: longCommentReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    const commentElement = wrapper.find('.review-card__comment')
    const expandButton = wrapper.find('.review-card__expand-btn')

    // 初始状态应该被截断
    expect(commentElement.classes()).toContain('truncated')
    expect(expandButton.exists()).toBe(true)
    expect(expandButton.text()).toContain('展开')

    // 点击展开按钮
    await expandButton.trigger('click')

    expect(commentElement.classes()).not.toContain('truncated')
    expect(expandButton.text()).toContain('收起')
  })

  it('emits click event when card is clicked', async () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    await wrapper.find('.review-card').trigger('click')

    expect(wrapper.emitted('click')).toBeTruthy()
    expect(wrapper.emitted('click')[0]).toEqual([mockReview])
  })

  it('handles image click and emits image-click event', async () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    const firstImage = wrapper.findComponent(OptimizedImage)
    await firstImage.trigger('click')

    expect(wrapper.emitted('image-click')).toBeTruthy()
    expect(wrapper.emitted('image-click')[0]).toEqual([0, mockReview.images])
  })

  it('displays verified badge for verified reviews', () => {
    const verifiedReview = {
      ...mockReview,
      isVerified: true
    }

    const wrapper = mount(ReviewCard, {
      props: { review: verifiedReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    expect(wrapper.find('.review-card__verified-badge').exists()).toBe(true)
  })

  it('applies correct CSS classes based on rating level', () => {
    const highRatingReview = { ...mockReview, overallRating: 5 }
    const lowRatingReview = { ...mockReview, overallRating: 2 }

    const highRatingWrapper = mount(ReviewCard, {
      props: { review: highRatingReview },
      global: { components: { ElRate, ElButton, ElTag, OptimizedImage } }
    })

    const lowRatingWrapper = mount(ReviewCard, {
      props: { review: lowRatingReview },
      global: { components: { ElRate, ElButton, ElTag, OptimizedImage } }
    })

    expect(highRatingWrapper.find('.review-card').classes()).toContain('rating-high')
    expect(lowRatingWrapper.find('.review-card').classes()).toContain('rating-low')
  })

  it('shows helpful section with vote buttons', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    expect(wrapper.find('.review-card__helpful').exists()).toBe(true)
    expect(wrapper.find('.helpful-btn--yes').exists()).toBe(true)
    expect(wrapper.find('.helpful-btn--no').exists()).toBe(true)
  })

  it('handles helpful vote buttons click', async () => {
    const wrapper = mount(ReviewCard, {
      props: { review: mockReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    const yesButton = wrapper.find('.helpful-btn--yes')
    const noButton = wrapper.find('.helpful-btn--no')

    await yesButton.trigger('click')
    expect(wrapper.emitted('helpful-vote')).toBeTruthy()
    expect(wrapper.emitted('helpful-vote')[0]).toEqual([mockReview.id, true])

    await noButton.trigger('click')
    expect(wrapper.emitted('helpful-vote')[1]).toEqual([mockReview.id, false])
  })

  it('displays mobile responsive layout correctly', async () => {
    // Mock mobile viewport
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 375,
    })

    const wrapper = mount(ReviewCard, {
      props: { review: mockReview },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    await wrapper.vm.$nextTick()

    expect(wrapper.find('.review-card').classes()).toContain('mobile')
  })

  it('shows loading skeleton when in loading state', () => {
    const wrapper = mount(ReviewCard, {
      props: {
        review: mockReview,
        loading: true
      },
      global: {
        components: {
          ElRate,
          ElButton,
          ElTag,
          OptimizedImage
        }
      }
    })

    expect(wrapper.find('.review-card__skeleton').exists()).toBe(true)
    expect(wrapper.find('.review-card__content').exists()).toBe(false)
  })
})