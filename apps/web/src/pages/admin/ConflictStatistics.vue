<template>
  <div class="conflict-statistics">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <i class="bi bi-graph-up"></i>
          冲突统计与分析
        </h1>
        <p class="page-description">监控和分析预订冲突情况，优化房间分配策略</p>
      </div>
      <div class="header-actions">
        <button
          class="btn btn-outline-primary"
          @click="exportData"
          :disabled="exporting"
        >
          <i class="bi bi-download"></i>
          <span v-if="exporting">导出中...</span>
          <span v-else>导出报表</span>
        </button>
        <button
          class="btn btn-secondary"
          @click="refreshData"
          :disabled="loading"
        >
          <i class="bi bi-arrow-clockwise" :class="{ rotating: loading }"></i>
          刷新数据
        </button>
      </div>
    </div>

    <!-- 时间范围过滤器 -->
    <div class="filter-section">
      <div class="filter-card">
        <h3 class="filter-title">时间范围</h3>
        <div class="filter-controls">
          <div class="filter-group">
            <label>开始日期</label>
            <input
              type="date"
              v-model="filters.startDate"
              @change="loadStatistics"
              class="form-control"
            />
          </div>
          <div class="filter-group">
            <label>结束日期</label>
            <input
              type="date"
              v-model="filters.endDate"
              @change="loadStatistics"
              class="form-control"
            />
          </div>
          <div class="filter-group">
            <label>房间</label>
            <select v-model="filters.roomId" @change="loadStatistics" class="form-select">
              <option value="">全部房间</option>
              <option v-for="room in availableRooms" :key="room.id" :value="room.id">
                {{ room.roomNumber }} - {{ room.name }}
              </option>
            </select>
          </div>
          <div class="filter-group">
            <label>分组方式</label>
            <select v-model="filters.groupBy" @change="loadStatistics" class="form-select">
              <option value="day">按天</option>
              <option value="week">按周</option>
              <option value="month">按月</option>
              <option value="conflictType">按冲突类型</option>
              <option value="roomId">按房间</option>
            </select>
          </div>
        </div>
        <div class="quick-filters">
          <button
            v-for="preset in datePresets"
            :key="preset.key"
            class="btn btn-outline-secondary btn-sm"
            @click="setDatePreset(preset)"
          >
            {{ preset.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 统计概览 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载统计数据中...</p>
    </div>

    <div v-else-if="statistics" class="statistics-content">
      <!-- 关键指标 -->
      <div class="overview-cards">
        <div class="stat-card primary">
          <div class="stat-icon">
            <i class="bi bi-exclamation-triangle"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ statistics.totalConflicts }}</div>
            <div class="stat-label">总冲突数</div>
            <div class="stat-change" :class="getChangeClass(conflictChange)">
              {{ formatChange(conflictChange) }}
            </div>
          </div>
        </div>

        <div class="stat-card warning">
          <div class="stat-icon">
            <i class="bi bi-calendar-x"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ statistics.timeOverlapConflicts }}</div>
            <div class="stat-label">时间重叠冲突</div>
            <div class="stat-percentage">
              {{ getPercentage(statistics.timeOverlapConflicts, statistics.totalConflicts) }}%
            </div>
          </div>
        </div>

        <div class="stat-card success">
          <div class="stat-icon">
            <i class="bi bi-check-circle"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ statistics.resolvedConflicts }}</div>
            <div class="stat-label">已解决冲突</div>
            <div class="stat-percentage">
              解决率 {{ statistics.resolutionRate }}%
            </div>
          </div>
        </div>

        <div class="stat-card info">
          <div class="stat-icon">
            <i class="bi bi-clock-history"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ statistics.waitingListSize }}</div>
            <div class="stat-label">等待列表人数</div>
            <div class="stat-change" :class="getChangeClass(waitingListChange)">
              {{ formatChange(waitingListChange) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-section">
        <!-- 冲突趋势图 -->
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">冲突趋势</h3>
            <div class="chart-actions">
              <select v-model="trendChartType" @change="updateTrendChart" class="form-select form-select-sm">
                <option value="line">折线图</option>
                <option value="bar">柱状图</option>
              </select>
            </div>
          </div>
          <div class="chart-container">
            <canvas ref="trendChart" width="400" height="200"></canvas>
          </div>
        </div>

        <!-- 冲突类型分布 -->
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">冲突类型分布</h3>
          </div>
          <div class="chart-container">
            <canvas ref="typeChart" width="400" height="200"></canvas>
          </div>
        </div>

        <!-- 房间冲突热力图 -->
        <div class="chart-card full-width">
          <div class="chart-header">
            <h3 class="chart-title">房间冲突热点</h3>
            <div class="chart-actions">
              <button class="btn btn-outline-primary btn-sm" @click="sortByConflictRate">
                按冲突率排序
              </button>
            </div>
          </div>
          <div class="room-hotspots">
            <div
              v-for="hotspot in sortedRoomHotspots"
              :key="hotspot.roomId"
              class="hotspot-item"
              :class="getHotspotClass(hotspot.conflictRate)"
            >
              <div class="hotspot-room">
                <span class="room-number">{{ hotspot.roomNumber }}</span>
                <span class="room-id">ID: {{ hotspot.roomId }}</span>
              </div>
              <div class="hotspot-stats">
                <div class="stat-item">
                  <span class="label">冲突次数</span>
                  <span class="value">{{ hotspot.conflictCount }}</span>
                </div>
                <div class="stat-item">
                  <span class="label">等待人数</span>
                  <span class="value">{{ hotspot.waitingListSize }}</span>
                </div>
                <div class="stat-item">
                  <span class="label">冲突率</span>
                  <span class="value highlight">{{ (hotspot.conflictRate * 100).toFixed(1) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 详细表格 -->
      <div class="data-table-section">
        <div class="table-card">
          <div class="table-header">
            <h3 class="table-title">冲突详情</h3>
            <div class="table-controls">
              <input
                type="text"
                v-model="searchTerm"
                placeholder="搜索..."
                class="form-control form-control-sm"
              />
              <select v-model="pageSize" @change="loadConflictDetails" class="form-select form-select-sm">
                <option :value="10">10条/页</option>
                <option :value="25">25条/页</option>
                <option :value="50">50条/页</option>
              </select>
            </div>
          </div>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th @click="sortConflicts('createdAt')" class="sortable">
                    冲突时间
                    <i class="bi" :class="getSortIcon('createdAt')"></i>
                  </th>
                  <th @click="sortConflicts('roomId')" class="sortable">
                    房间
                    <i class="bi" :class="getSortIcon('roomId')"></i>
                  </th>
                  <th @click="sortConflicts('conflictType')" class="sortable">
                    冲突类型
                    <i class="bi" :class="getSortIcon('conflictType')"></i>
                  </th>
                  <th @click="sortConflicts('status')" class="sortable">
                    状态
                    <i class="bi" :class="getSortIcon('status')"></i>
                  </th>
                  <th>解决方案</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="conflict in filteredConflicts" :key="conflict.id">
                  <td>{{ formatDateTime(conflict.createdAt) }}</td>
                  <td>
                    <span class="room-badge">{{ conflict.roomNumber || conflict.roomId }}</span>
                  </td>
                  <td>
                    <span class="conflict-type-badge" :class="conflict.conflictType?.toLowerCase()">
                      {{ getConflictTypeText(conflict.conflictType) }}
                    </span>
                  </td>
                  <td>
                    <span class="status-badge" :class="conflict.status?.toLowerCase()">
                      {{ getStatusText(conflict.status) }}
                    </span>
                  </td>
                  <td>
                    <span v-if="conflict.resolutionDetails" class="resolution-text">
                      {{ conflict.resolutionDetails }}
                    </span>
                    <span v-else class="text-muted">待处理</span>
                  </td>
                  <td>
                    <button
                      class="btn btn-outline-primary btn-sm"
                      @click="viewConflictDetails(conflict)"
                    >
                      <i class="bi bi-eye"></i>
                      查看
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 分页 -->
          <div class="table-pagination">
            <div class="pagination-info">
              显示 {{ (currentPage - 1) * pageSize + 1 }}-{{ Math.min(currentPage * pageSize, totalConflicts) }}
              共 {{ totalConflicts }} 条记录
            </div>
            <div class="pagination-controls">
              <button
                class="btn btn-outline-secondary btn-sm"
                @click="prevPage"
                :disabled="currentPage === 1"
              >
                <i class="bi bi-chevron-left"></i>
              </button>
              <span class="page-number">{{ currentPage }} / {{ totalPages }}</span>
              <button
                class="btn btn-outline-secondary btn-sm"
                @click="nextPage"
                :disabled="currentPage === totalPages"
              >
                <i class="bi bi-chevron-right"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, nextTick } from 'vue'
import { bookingConflictService } from '@/services/bookingConflictService'
import { formatDate, formatDateTime } from '@/utils/dateUtils'

export default {
  name: 'ConflictStatistics',
  setup() {
    const loading = ref(false)
    const exporting = ref(false)
    const statistics = ref(null)
    const conflictDetails = ref([])
    const availableRooms = ref([])
    const searchTerm = ref('')
    const currentPage = ref(1)
    const pageSize = ref(25)
    const totalConflicts = ref(0)
    const totalPages = ref(1)

    const filters = ref({
      startDate: '',
      endDate: '',
      roomId: '',
      groupBy: 'day'
    })

    const trendChartType = ref('line')
    const sortField = ref('createdAt')
    const sortOrder = ref('desc')

    const trendChart = ref(null)
    const typeChart = ref(null)

    // 日期预设
    const datePresets = [
      { key: 'today', label: '今天', days: 0 },
      { key: 'yesterday', label: '昨天', days: 1 },
      { key: 'week', label: '最近7天', days: 7 },
      { key: 'month', label: '最近30天', days: 30 },
      { key: 'quarter', label: '最近3个月', days: 90 }
    ]

    // 计算属性
    const sortedRoomHotspots = computed(() => {
      if (!statistics.value?.roomHotspots) return []
      return [...statistics.value.roomHotspots].sort((a, b) => b.conflictRate - a.conflictRate)
    })

    const filteredConflicts = computed(() => {
      if (!searchTerm.value) return conflictDetails.value
      const term = searchTerm.value.toLowerCase()
      return conflictDetails.value.filter(conflict =>
        (conflict.roomNumber && conflict.roomNumber.toLowerCase().includes(term)) ||
        (conflict.conflictType && conflict.conflictType.toLowerCase().includes(term)) ||
        (conflict.status && conflict.status.toLowerCase().includes(term))
      )
    })

    // 模拟变化数据（实际应用中应该从API获取历史数据进行对比）
    const conflictChange = ref(-12.5)
    const waitingListChange = ref(8.3)

    // 方法
    const loadStatistics = async () => {
      try {
        loading.value = true
        const response = await bookingConflictService.getConflictStatistics(filters.value)

        if (response.success) {
          statistics.value = response.data
          await nextTick()
          updateCharts()
        }
      } catch (error) {
        console.error('加载统计数据失败:', error)
      } finally {
        loading.value = false
      }
    }

    const loadConflictDetails = async () => {
      try {
        const response = await bookingConflictService.getConflicts({
          roomId: filters.value.roomId,
          page: currentPage.value,
          size: pageSize.value
        })

        if (response.success) {
          conflictDetails.value = response.data.records || []
          totalConflicts.value = response.data.total || 0
          totalPages.value = Math.ceil(totalConflicts.value / pageSize.value)
        }
      } catch (error) {
        console.error('加载冲突详情失败:', error)
      }
    }

    const setDatePreset = (preset) => {
      const endDate = new Date()
      const startDate = new Date()
      startDate.setDate(startDate.getDate() - preset.days)

      filters.value.endDate = endDate.toISOString().split('T')[0]
      filters.value.startDate = startDate.toISOString().split('T')[0]

      loadStatistics()
    }

    const refreshData = () => {
      loadStatistics()
      loadConflictDetails()
    }

    const exportData = async () => {
      try {
        exporting.value = true
        // 实现数据导出逻辑
        console.log('导出数据...')
        setTimeout(() => {
          exporting.value = false
        }, 2000)
      } catch (error) {
        console.error('导出数据失败:', error)
        exporting.value = false
      }
    }

    const getPercentage = (value, total) => {
      return total > 0 ? ((value / total) * 100).toFixed(1) : '0'
    }

    const getChangeClass = (change) => {
      return change > 0 ? 'positive' : change < 0 ? 'negative' : 'neutral'
    }

    const formatChange = (change) => {
      if (change > 0) return `↑ ${Math.abs(change)}%`
      if (change < 0) return `↓ ${Math.abs(change)}%`
      return '→ 0%'
    }

    const getHotspotClass = (rate) => {
      if (rate >= 0.8) return 'hotspot-critical'
      if (rate >= 0.5) return 'hotspot-warning'
      if (rate >= 0.2) return 'hotspot-caution'
      return 'hotspot-normal'
    }

    const getConflictTypeText = (type) => {
      const typeMap = {
        'TIME_OVERLAP': '时间重叠',
        'DOUBLE_BOOKING': '重复预订',
        'CONCURRENT_REQUEST': '并发请求'
      }
      return typeMap[type] || type
    }

    const getStatusText = (status) => {
      const statusMap = {
        'DETECTED': '已检测',
        'RESOLVED': '已解决',
        'WAITING_LIST': '等待列表'
      }
      return statusMap[status] || status
    }

    const getSortIcon = (field) => {
      if (sortField.value !== field) return 'bi-arrow-down-up'
      return sortOrder.value === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'
    }

    const sortConflicts = (field) => {
      if (sortField.value === field) {
        sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
      } else {
        sortField.value = field
        sortOrder.value = 'asc'
      }

      conflictDetails.value.sort((a, b) => {
        const aValue = a[field]
        const bValue = b[field]

        if (sortOrder.value === 'asc') {
          return aValue > bValue ? 1 : -1
        } else {
          return aValue < bValue ? 1 : -1
        }
      })
    }

    const sortByConflictRate = () => {
      if (statistics.value?.roomHotspots) {
        statistics.value.roomHotspots.sort((a, b) => b.conflictRate - a.conflictRate)
      }
    }

    const updateTrendChart = () => {
      // 实现趋势图表更新逻辑
      console.log('Update trend chart:', trendChartType.value)
    }

    const updateCharts = () => {
      // 实现图表更新逻辑
      console.log('Update charts with data:', statistics.value)
    }

    const viewConflictDetails = (conflict) => {
      // 跳转到冲突详情页面
      console.log('View conflict details:', conflict)
    }

    const prevPage = () => {
      if (currentPage.value > 1) {
        currentPage.value--
        loadConflictDetails()
      }
    }

    const nextPage = () => {
      if (currentPage.value < totalPages.value) {
        currentPage.value++
        loadConflictDetails()
      }
    }

    // 生命周期
    onMounted(() => {
      setDatePreset(datePresets[2]) // 默认显示最近7天
      loadConflictDetails()
    })

    return {
      loading,
      exporting,
      statistics,
      conflictDetails,
      availableRooms,
      searchTerm,
      currentPage,
      pageSize,
      totalPages,
      filters,
      trendChartType,
      trendChart,
      typeChart,
      datePresets,
      sortedRoomHotspots,
      filteredConflicts,
      conflictChange,
      waitingListChange,
      loadStatistics,
      loadConflictDetails,
      setDatePreset,
      refreshData,
      exportData,
      getPercentage,
      getChangeClass,
      formatChange,
      getHotspotClass,
      getConflictTypeText,
      getStatusText,
      getSortIcon,
      sortConflicts,
      sortByConflictRate,
      updateTrendChart,
      viewConflictDetails,
      prevPage,
      nextPage,
      formatDate,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.conflict-statistics {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.page-description {
  margin: 0;
  font-size: 16px;
  color: #666;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background-color: #1976d2;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #1565c0;
}

.btn-outline-primary {
  background-color: transparent;
  color: #1976d2;
  border: 1px solid #1976d2;
}

.btn-outline-primary:hover:not(:disabled) {
  background-color: #1976d2;
  color: white;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #5a6268;
}

.btn-outline-secondary {
  background-color: transparent;
  color: #6c757d;
  border: 1px solid #6c757d;
}

.btn-outline-secondary:hover:not(:disabled) {
  background-color: #6c757d;
  color: white;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.rotating {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.filter-section {
  margin-bottom: 32px;
}

.filter-card {
  background-color: white;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 24px;
}

.filter-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.filter-controls {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-group label {
  font-size: 14px;
  font-weight: 500;
  color: #495057;
}

.form-control, .form-select {
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 6px;
  font-size: 14px;
  background-color: white;
}

.form-select-sm {
  padding: 4px 8px;
  font-size: 13px;
}

.quick-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1976d2;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.stat-card {
  background-color: white;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.stat-card.primary { border-left: 4px solid #1976d2; }
.stat-card.warning { border-left: 4px solid #ff9800; }
.stat-card.success { border-left: 4px solid #4caf50; }
.stat-card.info { border-left: 4px solid #2196f3; }

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-card.primary .stat-icon { background-color: #1976d2; }
.stat-card.warning .stat-icon { background-color: #ff9800; }
.stat-card.success .stat-icon { background-color: #4caf50; }
.stat-card.info .stat-icon { background-color: #2196f3; }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #333;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-percentage {
  font-size: 12px;
  color: #666;
}

.stat-change {
  font-size: 12px;
  font-weight: 500;
}

.stat-change.positive { color: #4caf50; }
.stat-change.negative { color: #f44336; }
.stat-change.neutral { color: #666; }

.charts-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.chart-card {
  background-color: white;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 24px;
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.chart-actions {
  display: flex;
  gap: 8px;
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.room-hotspots {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.hotspot-item {
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.hotspot-critical { border-left: 4px solid #f44336; }
.hotspot-warning { border-left: 4px solid #ff9800; }
.hotspot-caution { border-left: 4px solid #ffc107; }
.hotspot-normal { border-left: 4px solid #4caf50; }

.hotspot-room {
  min-width: 100px;
}

.room-number {
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 4px;
}

.room-id {
  font-size: 12px;
  color: #666;
}

.hotspot-stats {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
}

.stat-item .label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.stat-item .value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.stat-item .value.highlight {
  color: #f44336;
}

.data-table-section {
  background-color: white;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  overflow: hidden;
}

.table-card {
  padding: 24px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.table-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.table-controls {
  display: flex;
  gap: 12px;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background-color: #f8f9fa;
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  color: #495057;
  border-bottom: 1px solid #e9ecef;
  font-size: 14px;
}

.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}

.data-table th.sortable:hover {
  background-color: #e9ecef;
}

.data-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #e9ecef;
  font-size: 14px;
}

.room-badge {
  background-color: #e3f2fd;
  color: #1976d2;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.conflict-type-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.conflict-type-badge.time_overlap {
  background-color: #fff8e6;
  color: #f57c00;
}

.conflict-type-badge.double_booking {
  background-color: #ffebee;
  color: #d32f2f;
}

.conflict-type-badge.concurrent_request {
  background-color: #e3f2fd;
  color: #1976d2;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.detected {
  background-color: #fff8e6;
  color: #f57c00;
}

.status-badge.resolved {
  background-color: #e8f5e8;
  color: #388e3c;
}

.status-badge.waiting_list {
  background-color: #e3f2fd;
  color: #1976d2;
}

.resolution-text {
  font-size: 13px;
  color: #333;
}

.text-muted {
  color: #999;
  font-style: italic;
}

.table-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e9ecef;
}

.pagination-info {
  font-size: 14px;
  color: #666;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-number {
  font-size: 14px;
  color: #333;
  min-width: 60px;
  text-align: center;
}

/* Responsive */
@media (max-width: 768px) {
  .conflict-statistics {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
  }

  .filter-controls {
    grid-template-columns: 1fr;
  }

  .overview-cards {
    grid-template-columns: 1fr;
  }

  .charts-section {
    grid-template-columns: 1fr;
  }

  .room-hotspots {
    grid-template-columns: 1fr;
  }

  .table-header {
    flex-direction: column;
    gap: 12px;
  }

  .table-pagination {
    flex-direction: column;
    gap: 12px;
  }

  .hotspot-item {
    flex-direction: column;
    gap: 12px;
  }

  .hotspot-stats {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>