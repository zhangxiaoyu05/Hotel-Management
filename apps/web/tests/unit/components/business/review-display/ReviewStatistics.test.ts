import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ReviewStatistics from '@/components/business/ReviewStatistics.vue'
import RatingDistribution from '@/components/business/RatingDistribution.vue'
import DimensionalRatings from '@/components/business/DimensionalRatings.vue'

// Mock child components
vi.mock('@/components/business/RatingDistribution.vue', () => ({
  default: {
    name: 'RatingDistribution',
    template: '<div class="rating-distribution-mock"></div>',
    props: ['distribution', 'total']
  }
}))

vi.mock('@/components/business/DimensionalRatings.vue', () => ({
  default: {
    name: 'DimensionalRatings',
    template: '<div class="dimensional-ratings-mock"></div>',
    props: ['ratings']
  }
}))

const mockStatistics = {
  hotelId: 1,
  totalReviews: 100,
  overallRating: 4.2,
  cleanlinessRating: 4.5,
  serviceRating: 4.3,
  facilitiesRating: 4.0,
  locationRating: 4.1,
  ratingDistribution: {
    rating5: 40,
    rating4: 30,
    rating3: 15,
    rating2: 10,
    rating1: 5
  },
  reviewsWithImages: 45,
  averageCommentLength: 120
}

const mockEmptyStatistics = {
  hotelId: 1,
  totalReviews: 0,
  overallRating: 0.0,
  cleanlinessRating: 0.0,
  serviceRating: 0.0,
  facilitiesRating: 0.0,
  locationRating: 0.0,
  ratingDistribution: {
    rating5: 0,
    rating4: 0,
    rating3: 0,
    rating2: 0,
    rating1: 0
  },
  reviewsWithImages: 0,
  averageCommentLength: 0.0
}

describe('ReviewStatistics.vue', () => {
  let wrapper: any

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  it('renders statistics correctly when data is available', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.review-statistics').exists()).toBe(true)
    expect(wrapper.find('.overall-rating').exists()).toBe(true)
    expect(wrapper.find('.overall-rating__value').text()).toBe('4.2')
    expect(wrapper.find('.total-reviews').text()).toContain('100')
    expect(wrapper.find('.reviews-with-images').text()).toContain('45%')
  })

  it('shows loading skeleton when loading', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        loading: true
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.review-statistics__skeleton').exists()).toBe(true)
    expect(wrapper.find('.skeleton-rating').exists()).toBe(true)
    expect(wrapper.find('.skeleton-stats').exists()).toBe(true)
  })

  it('shows empty state when no reviews', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockEmptyStatistics,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.empty-statistics').exists()).toBe(true)
    expect(wrapper.find('.empty-text').text()).toBe('暂无评价')
    expect(wrapper.find('.overall-rating__value').text()).toBe('0.0')
  })

  it('calculates image review percentage correctly', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.vm.imageReviewPercentage).toBe(45)
  })

  it('handles zero division in image percentage calculation', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockEmptyStatistics,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.vm.imageReviewPercentage).toBe(0)
  })

  it('passes correct props to child components', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    const ratingDistribution = wrapper.findComponent(RatingDistribution)
    const dimensionalRatings = wrapper.findComponent(DimensionalRatings)

    expect(ratingDistribution.props('distribution')).toEqual(mockStatistics.ratingDistribution)
    expect(ratingDistribution.props('total')).toBe(mockStatistics.totalReviews)

    expect(dimensionalRatings.props('ratings')).toEqual({
      cleanliness: mockStatistics.cleanlinessRating,
      service: mockStatistics.serviceRating,
      facilities: mockStatistics.facilitiesRating,
      location: mockStatistics.locationRating
    })
  })

  it('displays rating stars correctly', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    const stars = wrapper.findAll('.rating-star')
    expect(stars).toHaveLength(5)

    // 4.2星应该显示4个满星和1个部分填充的星
    expect(wrapper.find('.rating-star--full').exists()).toBe(true)
    expect(wrapper.find('.rating-star--partial').exists()).toBe(true)
    expect(wrapper.vm.starPercentage(0.2)).toBe(20)
  })

  it('shows rating breakdown section', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.rating-breakdown').exists()).toBe(true)
    expect(wrapper.find('.dimensional-ratings').exists()).toBe(true)
    expect(wrapper.find('.rating-distribution').exists()).toBe(true)
  })

  it('formats average comment length correctly', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.comment-statistics').exists()).toBe(true)
    expect(wrapper.find('.average-comment-length').text()).toContain('120')
  })

  it('shows last updated timestamp when provided', () => {
    const statisticsWithTimestamp = {
      ...mockStatistics,
      lastUpdated: '2024-01-15T10:30:00Z'
    }

    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: statisticsWithTimestamp,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.last-updated').exists()).toBe(true)
    expect(wrapper.find('.last-updated').text()).toContain('更新于')
  })

  it('applies compact variant correctly', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false,
        compact: true
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.review-statistics').classes()).toContain('compact')
    expect(wrapper.find('.overall-rating').classes()).toContain('compact')
    expect(wrapper.find('.rating-breakdown').exists()).toBe(false) // 紧凑模式下隐藏详细分解
  })

  it('handles different rating precision levels', () => {
    const statisticsWithDecimal = {
      ...mockStatistics,
      overallRating: 4.67,
      cleanlinessRating: 4.83
    }

    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: statisticsWithDecimal,
        loading: false,
        precision: 2
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.overall-rating__value').text()).toBe('4.67')
  })

  it('shows trend indicators when previous data is available', () => {
    const statisticsWithTrend = {
      ...mockStatistics,
      trend: {
        overall: 0.2,
        cleanliness: 0.1,
        service: -0.1,
        facilities: 0.0,
        location: 0.3
      }
    }

    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: statisticsWithTrend,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.rating-trend').exists()).toBe(true)
    expect(wrapper.find('.trend-up').exists()).toBe(true)
    expect(wrapper.find('.trend-down').exists()).toBe(true)
    expect(wrapper.find('.trend-stable').exists()).toBe(true)
  })

  it('emits refresh event when refresh button is clicked', async () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false,
        showRefresh: true
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    const refreshButton = wrapper.find('[data-testid="refresh-button"]')
    expect(refreshButton.exists()).toBe(true)

    await refreshButton.trigger('click')

    expect(wrapper.emitted('refresh')).toBeTruthy()
  })

  it('displays comparison with hotel average when available', () => {
    const statisticsWithComparison = {
      ...mockStatistics,
      hotelComparison: {
        overallRating: 4.0,
        cleanlinessRating: 4.2,
        serviceRating: 4.1,
        facilitiesRating: 3.9,
        locationRating: 4.0
      }
    }

    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: statisticsWithComparison,
        loading: false,
        showComparison: true
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.rating-comparison').exists()).toBe(true)
    expect(wrapper.find('.comparison-value').text()).toContain('4.0')
  })

  it('supports custom color scheme', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false,
        colorScheme: 'dark'
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.review-statistics').classes()).toContain('dark-theme')
  })

  it('handles responsive layout changes', async () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        statistics: mockStatistics,
        loading: false
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    // Mock mobile viewport
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 375,
    })

    await wrapper.vm.$nextTick()
    await wrapper.vm.handleResize()

    expect(wrapper.find('.review-statistics').classes()).toContain('mobile')
  })

  it('shows loading error message when error prop is provided', () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        error: 'Failed to load statistics'
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    expect(wrapper.find('.error-message').exists()).toBe(true)
    expect(wrapper.find('.error-message').text()).toBe('Failed to load statistics')
    expect(wrapper.find('.retry-button').exists()).toBe(true)
  })

  it('emits retry event when retry button is clicked', async () => {
    wrapper = mount(ReviewStatistics, {
      props: {
        error: 'Failed to load statistics'
      },
      global: {
        components: {
          RatingDistribution,
          DimensionalRatings
        }
      }
    })

    const retryButton = wrapper.find('.retry-button')
    await retryButton.trigger('click')

    expect(wrapper.emitted('retry')).toBeTruthy()
  })
})