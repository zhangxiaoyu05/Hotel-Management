import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElPagination, ElMessage } from 'element-plus'
import ReviewList from '@/components/business/ReviewList.vue'
import ReviewCard from '@/components/business/ReviewCard.vue'
import ReviewFilter from '@/components/business/ReviewFilter.vue'
import SortSelector from '@/components/business/SortSelector.vue'
import reviewService from '@/services/reviewService'

// Mock child components
vi.mock('@/components/business/ReviewCard.vue', () => ({
  default: {
    name: 'ReviewCard',
    template: '<div class="review-card-mock" @click="$emit(\'click\', review)"></div>',
    props: ['review'],
    emits: ['click', 'image-click', 'helpful-vote']
  }
}))

vi.mock('@/components/business/ReviewFilter.vue', () => ({
  default: {
    name: 'ReviewFilter',
    template: '<div class="review-filter-mock" @click="$emit(\'filter-change\', $event)"></div>',
    props: ['modelValue'],
    emits: ['update:modelValue', 'filter-change']
  }
}))

vi.mock('@/components/business/SortSelector.vue', () => ({
  default: {
    name: 'SortSelector',
    template: '<div class="sort-selector-mock" @click="$emit(\'sort-change\', $event)"></div>',
    props: ['modelValue'],
    emits: ['update:modelValue', 'sort-change']
  }
}))

// Mock review service
vi.mock('@/services/reviewService', () => ({
  default: {
    getReviews: vi.fn(),
    getHotelStatistics: vi.fn()
  }
}))

const mockReviews = [
  {
    id: 1,
    userId: 1,
    username: 'user1',
    overallRating: 5,
    cleanlinessRating: 5,
    serviceRating: 5,
    facilitiesRating: 5,
    locationRating: 5,
    comment: '很棒的酒店！',
    images: ['image1.jpg'],
    isAnonymous: false,
    createdAt: '2024-01-15T10:30:00Z'
  },
  {
    id: 2,
    userId: 2,
    username: 'user2',
    overallRating: 4,
    cleanlinessRating: 4,
    serviceRating: 4,
    facilitiesRating: 4,
    locationRating: 4,
    comment: '不错的体验',
    images: null,
    isAnonymous: false,
    createdAt: '2024-01-14T15:20:00Z'
  }
]

const mockReviewResponse = {
  reviews: mockReviews,
  total: 2,
  page: 0,
  size: 10,
  totalPages: 1
}

describe('ReviewList.vue', () => {
  let wrapper: any

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(ElMessage).mockImplementation({
      error: vi.fn(),
      success: vi.fn()
    } as any)
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  it('renders review list correctly', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        },
        stubs: {
          'el-pagination': true,
          'el-skeleton': true,
          'el-empty': true
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.find('.review-list').exists()).toBe(true)
    expect(wrapper.find('.review-controls').exists()).toBe(true)
    expect(wrapper.findAllComponents(ReviewCard)).toHaveLength(2)
  })

  it('loads reviews on mount', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(reviewService.getReviews).toHaveBeenCalledWith({
      hotelId: 1,
      page: 0,
      size: 10,
      sortBy: 'date',
      sortOrder: 'desc'
    })
  })

  it('shows loading skeleton while loading', () => {
    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      data() {
        return {
          loading: true
        }
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    expect(wrapper.find('.review-list__loading').exists()).toBe(true)
    expect(wrapper.findAll('.review-card__skeleton')).toHaveLength(4)
  })

  it('shows empty state when no reviews', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue({
      reviews: [],
      total: 0,
      page: 0,
      size: 10,
      totalPages: 0
    })

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.find('.review-list__empty').exists()).toBe(true)
    expect(wrapper.find('.empty-text').text()).toBe('暂无评价')
  })

  it('handles pagination change', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1,
        showPagination: true
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    // 模拟分页变化
    await wrapper.vm.handlePageChange(2)

    expect(wrapper.vm.currentPage).toBe(2)
    expect(reviewService.getReviews).toHaveBeenCalledWith({
      hotelId: 1,
      page: 1,
      size: 10,
      sortBy: 'date',
      sortOrder: 'desc'
    })
  })

  it('handles filter change', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    const newFilters = {
      minRating: 4,
      maxRating: 5,
      hasImages: true
    }

    await wrapper.vm.handleFilterChange(newFilters)

    expect(wrapper.vm.filters).toEqual(newFilters)
    expect(wrapper.vm.currentPage).toBe(0) // 重置到第一页
  })

  it('handles sort change', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    const newSort = {
      sortBy: 'rating',
      sortOrder: 'asc'
    }

    await wrapper.vm.handleSortChange(newSort)

    expect(wrapper.vm.sortOptions).toEqual(newSort)
  })

  it('emits review-click event when review card is clicked', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    const reviewCard = wrapper.findComponent(ReviewCard)
    await reviewCard.vm.$emit('click', mockReviews[0])

    expect(wrapper.emitted('review-click')).toBeTruthy()
    expect(wrapper.emitted('review-click')[0]).toEqual([mockReviews[0]])
  })

  it('handles load more functionality when enabled', async () => {
    const manyReviewsResponse = {
      reviews: mockReviews,
      total: 50,
      page: 0,
      size: 10,
      totalPages: 5
    }
    vi.mocked(reviewService.getReviews).mockResolvedValue(manyReviewsResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1,
        showLoadMore: true,
        showPagination: false
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.find('.review-list__loadmore').exists()).toBe(true)
    expect(wrapper.vm.hasMore).toBe(true)

    await wrapper.vm.loadMore()

    expect(wrapper.vm.loadingMore).toBe(true)
    expect(wrapper.vm.currentPage).toBe(1)
  })

  it('emits total-change event with total count', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.emitted('total-change')).toBeTruthy()
    expect(wrapper.emitted('total-change')[0]).toEqual([2])
  })

  it('handles API errors gracefully', async () => {
    vi.mocked(reviewService.getReviews).mockRejectedValue(new Error('API Error'))

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(ElMessage.error).toHaveBeenCalledWith('加载评价失败，请稍后重试')
    expect(wrapper.vm.loading).toBe(false)
  })

  it('resets current page when filters change', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1
      },
      data() {
        return {
          currentPage: 2
        }
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    const newFilters = { minRating: 3 }
    await wrapper.vm.handleFilterChange(newFilters)

    expect(wrapper.vm.currentPage).toBe(0)
  })

  it('accepts custom page size prop', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1,
        pageSize: 20
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(reviewService.getReviews).toHaveBeenCalledWith({
      hotelId: 1,
      page: 0,
      size: 20,
      sortBy: 'date',
      sortOrder: 'desc'
    })
  })

  it('hides pagination when showPagination is false', async () => {
    vi.mocked(reviewService.getReviews).mockResolvedValue(mockReviewResponse)

    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1,
        showPagination: false
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.find('.review-list__pagination').exists()).toBe(false)
  })

  it('shows loading more state', async () => {
    wrapper = mount(ReviewList, {
      props: {
        hotelId: 1,
        showLoadMore: true
      },
      data() {
        return {
          loadingMore: true
        }
      },
      global: {
        components: {
          ElPagination,
          ReviewCard,
          ReviewFilter,
          SortSelector
        }
      }
    })

    expect(wrapper.find('.review-list__loading-more').exists()).toBe(true)
    expect(wrapper.find('.loading-spinner').exists()).toBe(true)
  })
})