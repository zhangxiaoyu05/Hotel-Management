<template>
  <div class="room-search-container">
    <!-- 搜索表单区域 -->
    <div class="search-form-section">
      <div class="container">
        <div class="search-card">
          <h1 class="search-title">查找房间</h1>

          <form @submit.prevent="handleSearch" class="search-form">
            <!-- 基础搜索条件 -->
            <div class="row g-3 mb-4">
              <div class="col-md-3">
                <label class="form-label">入住日期</label>
                <input
                  v-model="searchForm.checkInDate"
                  type="date"
                  class="form-control"
                  :min="today"
                  required
                />
              </div>

              <div class="col-md-3">
                <label class="form-label">退房日期</label>
                <input
                  v-model="searchForm.checkOutDate"
                  type="date"
                  class="form-control"
                  :min="searchForm.checkInDate || today"
                  required
                />
              </div>

              <div class="col-md-2">
                <label class="form-label">客人数量</label>
                <select v-model="searchForm.guestCount" class="form-select" required>
                  <option value="1">1 人</option>
                  <option value="2">2 人</option>
                  <option value="3">3 人</option>
                  <option value="4">4 人</option>
                  <option value="5">5 人</option>
                  <option value="6">6 人</option>
                </select>
              </div>

              <div class="col-md-2">
                <label class="form-label">酒店</label>
                <select v-model="searchForm.hotelId" class="form-select">
                  <option value="">全部酒店</option>
                  <option
                    v-for="hotel in hotels"
                    :key="hotel.id"
                    :value="hotel.id"
                  >
                    {{ hotel.name }}
                  </option>
                </select>
              </div>

              <div class="col-md-2">
                <label class="form-label">房间类型</label>
                <select v-model="searchForm.roomTypeId" class="form-select">
                  <option value="">全部类型</option>
                  <option
                    v-for="roomType in roomTypes"
                    :key="roomType.id"
                    :value="roomType.id"
                  >
                    {{ roomType.name }}
                  </option>
                </select>
              </div>
            </div>

            <!-- 高级筛选选项 -->
            <div class="advanced-filters mb-4">
              <button
                type="button"
                class="btn btn-outline-secondary btn-sm"
                @click="toggleAdvancedFilters"
              >
                {{ showAdvancedFilters ? '隐藏高级筛选' : '显示高级筛选' }}
                <i class="bi" :class="showAdvancedFilters ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
              </button>

              <div v-show="showAdvancedFilters" class="mt-3">
                <div class="row g-3">
                  <div class="col-md-3">
                    <label class="form-label">最低价格</label>
                    <input
                      v-model.number="searchForm.priceMin"
                      type="number"
                      class="form-control"
                      min="0"
                      step="10"
                      placeholder="最低价格"
                    />
                  </div>

                  <div class="col-md-3">
                    <label class="form-label">最高价格</label>
                    <input
                      v-model.number="searchForm.priceMax"
                      type="number"
                      class="form-control"
                      min="0"
                      step="10"
                      placeholder="最高价格"
                    />
                  </div>

                  <div class="col-md-3">
                    <label class="form-label">排序方式</label>
                    <select v-model="searchForm.sortBy" class="form-select">
                      <option value="PRICE">按价格</option>
                      <option value="RATING">按评分</option>
                      <option value="DISTANCE">按距离</option>
                    </select>
                  </div>

                  <div class="col-md-3">
                    <label class="form-label">排序顺序</label>
                    <select v-model="searchForm.sortOrder" class="form-select">
                      <option value="ASC">升序</option>
                      <option value="DESC">降序</option>
                    </select>
                  </div>
                </div>

                <!-- 设施筛选 -->
                <div class="mt-3">
                  <label class="form-label">设施要求</label>
                  <div class="row g-2">
                    <div
                      v-for="facility in availableFacilities"
                      :key="facility"
                      class="col-md-2"
                    >
                      <div class="form-check">
                        <input
                          v-model="searchForm.facilities"
                          :value="facility"
                          type="checkbox"
                          class="form-check-input"
                          :id="`facility-${facility}`"
                        />
                        <label :for="`facility-${facility}`" class="form-check-label">
                          {{ facility }}
                        </label>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 搜索按钮 -->
            <div class="search-buttons">
              <button type="submit" class="btn btn-primary btn-lg" :disabled="searching">
                <i class="bi bi-search"></i>
                {{ searching ? '搜索中...' : '搜索房间' }}
              </button>

              <button
                type="button"
                class="btn btn-outline-secondary"
                @click="resetSearch"
              >
                <i class="bi bi-arrow-clockwise"></i>
                重置
              </button>

              <!-- 快速选择 -->
              <div class="quick-selects ms-auto">
                <span class="text-muted">快速选择：</span>
                <button
                  type="button"
                  class="btn btn-outline-primary btn-sm ms-2"
                  @click="setQuickDates(1)"
                >
                  今天
                </button>
                <button
                  type="button"
                  class="btn btn-outline-primary btn-sm"
                  @click="setQuickDates(7)"
                >
                  本周末
                </button>
                <button
                  type="button"
                  class="btn btn-outline-primary btn-sm"
                  @click="setQuickDates(30)"
                >
                  下个月
                </button>
              </div>
            </div>
          </form>

          <!-- 搜索历史 -->
          <div v-if="searchHistory.length > 0" class="search-history mt-3">
            <small class="text-muted">搜索历史：</small>
            <div class="mt-2">
              <button
                v-for="(history, index) in searchHistory"
                :key="index"
                type="button"
                class="btn btn-outline-secondary btn-sm me-2 mb-2"
                @click="applySearchHistory(history)"
              >
                {{ formatSearchHistory(history) }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索结果区域 -->
    <div class="search-results-section">
      <div class="container">
        <!-- 搜索结果头部 -->
        <div v-if="searchResults && searchResults.rooms" class="results-header">
          <div class="d-flex justify-content-between align-items-center">
            <h2>找到 {{ searchResults.total }} 个可用房间</h2>
            <div class="results-summary">
              第 {{ searchResults.page + 1 }} 页，共 {{ searchResults.totalPages }} 页
            </div>
          </div>
        </div>

        <!-- 搜索结果列表 -->
        <div v-if="searchResults && searchResults.rooms.length > 0" class="results-grid">
          <div
            v-for="room in searchResults.rooms"
            :key="room.id"
            class="room-card-wrapper"
          >
            <RoomCard
              :room="room"
              @view-details="viewRoomDetails"
              @book-now="bookRoom"
            />
          </div>
        </div>

        <!-- 无结果提示 -->
        <div v-else-if="hasSearched && (!searchResults || searchResults.rooms.length === 0)" class="no-results">
          <div class="text-center py-5">
            <i class="bi bi-search display-1 text-muted"></i>
            <h3 class="mt-3">未找到符合条件的房间</h3>
            <p class="text-muted">
              请尝试调整搜索条件或选择不同的日期范围
            </p>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="searchResults && searchResults.rooms.length > 0" class="pagination-wrapper">
          <nav>
            <ul class="pagination justify-content-center">
              <li class="page-item" :class="{ disabled: !hasPreviousPage }">
                <button
                  class="page-link"
                  @click="goToPage(searchResults.page - 1)"
                  :disabled="!hasPreviousPage"
                >
                  上一页
                </button>
              </li>

              <li
                v-for="page in visiblePages"
                :key="page"
                class="page-item"
                :class="{ active: page === searchResults.page }"
              >
                <button
                  class="page-link"
                  @click="goToPage(page)"
                  :disabled="page === searchResults.page"
                >
                  {{ page + 1 }}
                </button>
              </li>

              <li class="page-item" :class="{ disabled: !hasNextPage }">
                <button
                  class="page-link"
                  @click="goToPage(searchResults.page + 1)"
                  :disabled="!hasNextPage"
                >
                  下一页
                </button>
              </li>
            </ul>
          </nav>
        </div>
      </div>
    </div>

    <!-- 房间详情模态框 -->
    <RoomDetail
      v-if="selectedRoom"
      :room="selectedRoom"
      :checkInDate="searchForm.checkInDate"
      :checkOutDate="searchForm.checkOutDate"
      :guestCount="searchForm.guestCount"
      @close="closeRoomDetails"
    />
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRoomStore } from '@/stores/room'
import { useHotelStore } from '@/stores/hotel'
import RoomCard from '@/components/business/RoomCard.vue'
import RoomDetail from './RoomDetail.vue'

export default {
  name: 'RoomSearch',
  components: {
    RoomCard,
    RoomDetail
  },
  setup() {
    const router = useRouter()
    const roomStore = useRoomStore()
    const hotelStore = useHotelStore()

    // 响应式数据
    const searching = ref(false)
    const hasSearched = ref(false)
    const showAdvancedFilters = ref(false)
    const selectedRoom = ref(null)
    const searchResults = ref(null)

    // 基础数据
    const hotels = ref([])
    const roomTypes = ref([])
    const availableFacilities = ref([
      'WiFi',
      '空调',
      '电视',
      '热水',
      '迷你吧',
      '保险箱',
      '吹风机',
      '熨衣设备',
      '办公桌',
      '阳台'
    ])

    // 搜索表单
    const searchForm = reactive({
      checkInDate: '',
      checkOutDate: '',
      guestCount: 2,
      hotelId: null,
      roomTypeId: null,
      priceMin: null,
      priceMax: null,
      facilities: [],
      sortBy: 'PRICE',
      sortOrder: 'ASC',
      page: 0,
      size: 20
    })

    // 计算属性
    const today = computed(() => {
      const date = new Date()
      return date.toISOString().split('T')[0]
    })

    const hasPreviousPage = computed(() => {
      return searchResults.value && searchResults.value.page > 0
    })

    const hasNextPage = computed(() => {
      return searchResults.value && searchResults.value.page < searchResults.value.totalPages - 1
    })

    const visiblePages = computed(() => {
      if (!searchResults.value) return []

      const currentPage = searchResults.value.page
      const totalPages = searchResults.value.totalPages
      const start = Math.max(0, currentPage - 2)
      const end = Math.min(totalPages, currentPage + 3)

      const pages = []
      for (let i = start; i < end; i++) {
        pages.push(i)
      }
      return pages
    })

    // 方法
    const toggleAdvancedFilters = () => {
      showAdvancedFilters.value = !showAdvancedFilters.value
    }

    const handleSearch = async () => {
      if (searching.value) return

      searching.value = true
      hasSearched.value = true

      try {
        const result = await roomStore.searchAvailableRooms(searchForm)
        searchResults.value = result

        // 保存搜索历史
        saveSearchHistory()
      } catch (error) {
        console.error('搜索房间失败:', error)
        // 这里可以显示错误消息
      } finally {
        searching.value = false
      }
    }

    const resetSearch = () => {
      searchForm.checkInDate = ''
      searchForm.checkOutDate = ''
      searchForm.guestCount = 2
      searchForm.hotelId = null
      searchForm.roomTypeId = null
      searchForm.priceMin = null
      searchForm.priceMax = null
      searchForm.facilities = []
      searchForm.sortBy = 'PRICE'
      searchForm.sortOrder = 'ASC'
      searchForm.page = 0

      searchResults.value = null
      hasSearched.value = false
    }

    const setQuickDates = (days) => {
      const today = new Date()
      const checkIn = new Date(today)
      const checkOut = new Date(today)

      if (days === 1) {
        // 今天入住，明天退房
        checkOut.setDate(checkOut.getDate() + 1)
      } else if (days === 7) {
        // 本周末
        const dayOfWeek = today.getDay()
        const daysUntilSaturday = (6 - dayOfWeek + 7) % 7
        checkIn.setDate(today.getDate() + daysUntilSaturday)
        checkOut.setDate(checkIn.getDate() + 1)
      } else if (days === 30) {
        // 下个月同一天
        checkIn.setMonth(checkIn.getMonth() + 1)
        checkOut.setDate(checkIn.getDate() + 1)
      }

      searchForm.checkInDate = checkIn.toISOString().split('T')[0]
      searchForm.checkOutDate = checkOut.toISOString().split('T')[0]
    }

    const goToPage = (page) => {
      if (page < 0 || page >= searchResults.value.totalPages) return

      searchForm.page = page
      handleSearch()
    }

    const viewRoomDetails = (room) => {
      selectedRoom.value = room
    }

    const closeRoomDetails = () => {
      selectedRoom.value = null
    }

    const bookRoom = (room) => {
      // 导航到预订页面
      router.push({
        name: 'Booking',
        query: {
          roomId: room.id,
          checkInDate: searchForm.checkInDate,
          checkOutDate: searchForm.checkOutDate,
          guestCount: searchForm.guestCount
        }
      })
    }

    const saveSearchHistory = () => {
      const history = {
        checkInDate: searchForm.checkInDate,
        checkOutDate: searchForm.checkOutDate,
        guestCount: searchForm.guestCount,
        hotelId: searchForm.hotelId,
        roomTypeId: searchForm.roomTypeId,
        timestamp: Date.now()
      }

      roomStore.saveSearchHistory(history)
    }

    const applySearchHistory = (history) => {
      Object.assign(searchForm, history)
      searchForm.page = 0
      handleSearch()
    }

    const formatSearchHistory = (history) => {
      const hotel = hotels.value.find(h => h.id === history.hotelId)
      const hotelName = hotel ? hotel.name : '全部酒店'

      const checkIn = new Date(history.checkInDate)
      const checkOut = new Date(history.checkOutDate)

      const formatDate = (date) => {
        return `${date.getMonth() + 1}/${date.getDate()}`
      }

      return `${hotelName} ${formatDate(checkIn)}-${formatDate(checkOut)} ${history.guestCount}人`
    }

    const searchHistory = computed(() => {
      return roomStore.searchHistory
    })

    // 生命周期
    onMounted(async () => {
      try {
        // 加载基础数据
        hotels.value = await hotelStore.fetchHotels()
        roomTypes.value = await roomStore.fetchRoomTypes()

        // 从URL参数恢复搜索条件
        const urlParams = new URLSearchParams(window.location.search)
        if (urlParams.has('checkInDate')) {
          searchForm.checkInDate = urlParams.get('checkInDate')
          searchForm.checkOutDate = urlParams.get('checkOutDate')
          searchForm.guestCount = parseInt(urlParams.get('guestCount')) || 2
          searchForm.hotelId = urlParams.get('hotelId') ? parseInt(urlParams.get('hotelId')) : null
          searchForm.roomTypeId = urlParams.get('roomTypeId') ? parseInt(urlParams.get('roomTypeId')) : null

          // 自动执行搜索
          handleSearch()
        }
      } catch (error) {
        console.error('加载基础数据失败:', error)
      }
    })

    return {
      searching,
      hasSearched,
      showAdvancedFilters,
      selectedRoom,
      searchResults,
      hotels,
      roomTypes,
      availableFacilities,
      searchForm,
      today,
      hasPreviousPage,
      hasNextPage,
      visiblePages,
      searchHistory,
      toggleAdvancedFilters,
      handleSearch,
      resetSearch,
      setQuickDates,
      goToPage,
      viewRoomDetails,
      closeRoomDetails,
      bookRoom,
      applySearchHistory,
      formatSearchHistory
    }
  }
}
</script>

<style scoped>
.room-search-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}

.search-form-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem 0;
  color: white;
}

.search-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 2rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  color: #333;
}

.search-title {
  color: #667eea;
  margin-bottom: 1.5rem;
  font-weight: 600;
  text-align: center;
}

.advanced-filters {
  border-top: 1px solid #dee2e6;
  padding-top: 1rem;
}

.search-buttons {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.quick-selects {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-history {
  border-top: 1px solid #dee2e6;
  padding-top: 1rem;
  margin-top: 1rem;
}

.search-results-section {
  padding: 2rem 0;
}

.results-header {
  margin-bottom: 2rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.results-header h2 {
  color: #333;
  margin: 0;
  font-size: 1.5rem;
}

.results-summary {
  color: #666;
  font-size: 0.9rem;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 2rem;
  margin-bottom: 3rem;
}

.no-results {
  background: white;
  border-radius: 8px;
  padding: 3rem;
  margin: 2rem 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.pagination-wrapper {
  margin-top: 3rem;
}

.pagination .page-link {
  color: #667eea;
  border-color: #dee2e6;
}

.pagination .page-link:hover {
  color: #5a67d8;
  background-color: #f8f9fa;
}

.pagination .page-item.active .page-link {
  background-color: #667eea;
  border-color: #667eea;
}

@media (max-width: 768px) {
  .search-form-section {
    padding: 1rem 0;
  }

  .search-card {
    padding: 1rem;
  }

  .search-buttons {
    flex-direction: column;
    align-items: stretch;
  }

  .quick-selects {
    margin-top: 1rem;
    justify-content: center;
  }

  .results-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .search-buttons .btn {
    width: 100%;
  }
}
</style>