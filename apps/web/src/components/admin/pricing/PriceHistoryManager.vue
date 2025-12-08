<template>
  <div class="price-history-manager">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon">
          <TrendCharts />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ statistics.totalChanges || 0 }}</div>
          <div class="stat-label">总变更次数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon increase">
          <ArrowUp />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ statistics.increaseCount || 0 }}</div>
          <div class="stat-label">涨价次数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon decrease">
          <ArrowDown />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ statistics.decreaseCount || 0 }}</div>
          <div class="stat-label">降价次数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">
          <Calendar />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ statistics.period || '30天' }}</div>
          <div class="stat-label">统计周期</div>
        </div>
      </div>
    </div>

    <!-- 筛选工具栏 -->
    <div class="filter-toolbar">
      <div class="filter-left">
        <el-select
          v-model="filterType"
          placeholder="变更类型"
          clearable
          style="width: 150px"
          @change="loadPriceHistory"
        >
          <el-option label="基础价格变更" value="BASE_PRICE" />
          <el-option label="动态规则变更" value="DYNAMIC_RULE" />
          <el-option label="特殊价格变更" value="SPECIAL_PRICE" />
          <el-option label="手动价格调整" value="MANUAL" />
        </el-select>

        <el-select
          v-model="selectedRoomType"
          placeholder="房间类型"
          clearable
          style="width: 150px"
          @change="loadPriceHistory"
        >
          <el-option
            v-for="roomType in roomTypes"
            :key="roomType.id"
            :label="roomType.name"
            :value="roomType.id"
          />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="loadPriceHistory"
        />
      </div>

      <div class="filter-right">
        <el-button @click="showTrendChart = !showTrendChart">
          {{ showTrendChart ? '隐藏趋势图' : '显示趋势图' }}
        </el-button>
        <el-button @click="exportHistory" :icon="Download">
          导出记录
        </el-button>
      </div>
    </div>

    <!-- 趋势图表 -->
    <div v-if="showTrendChart" class="trend-chart">
      <PriceTrendChart :trend-data="trendData" />
    </div>

    <!-- 历史记录列表 -->
    <div class="history-list">
      <el-table
        :data="priceHistory"
        v-loading="loading"
        row-key="id"
      >
        <el-table-column prop="createdAt" label="时间" width="160" sortable>
          <template #default="{ row }">
            <div class="time-cell">
              <div class="date">{{ formatDate(row.createdAt) }}</div>
              <div class="time">{{ formatTime(row.createdAt) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="房间信息" min-width="180">
          <template #default="{ row }">
            <div class="room-info">
              <div class="room-type">{{ row.roomTypeName }}</div>
              <div v-if="row.roomNumber" class="room-number">{{ row.roomNumber }}</div>
              <div v-else class="room-number">房间类型级别</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格变更" width="150">
          <template #default="{ row }">
            <div class="price-change">
              <div v-if="row.oldPrice" class="old-price">¥{{ row.oldPrice }}</div>
              <div class="new-price">¥{{ row.newPrice }}</div>
              <div v-if="row.oldPrice" class="change-info">
                <span :class="getChangeClass(row)">
                  {{ getChangeText(row) }}
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="changeType" label="变更类型" width="140">
          <template #default="{ row }">
            <el-tag :type="getChangeTypeTag(row.changeType)">
              {{ getChangeTypeName(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="changeReason" label="变更原因" min-width="200">
          <template #default="{ row }">
            <span v-if="row.changeReason" class="reason-text">
              {{ row.changeReason }}
            </span>
            <span v-else class="no-reason">无变更原因</span>
          </template>
        </el-table-column>

        <el-table-column prop="changedByName" label="操作人" width="120">
          <template #default="{ row }">
            {{ row.changedByName || '未知' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="viewHistoryDetail(row)"
              :icon="View"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadPriceHistory"
          @current-change="loadPriceHistory"
        />
      </div>
    </div>

    <!-- 详情对话框 -->
    <PriceHistoryDetailDialog
      v-model="showDetailDialog"
      :history-item="selectedHistoryItem"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  TrendCharts,
  ArrowUp,
  ArrowDown,
  Calendar,
  Download,
  View
} from '@element-plus/icons-vue'
import { usePricingStore } from '../../../stores/pricingStore'
import { useHotelStore } from '../../../stores/hotelStore'
import { useRoomTypeStore } from '../../../stores/roomTypeStore'
import type { PriceHistory } from '../../../types/pricing'
import PriceTrendChart from './PriceTrendChart.vue'
import PriceHistoryDetailDialog from './PriceHistoryDetailDialog.vue'

const pricingStore = usePricingStore()
const hotelStore = useHotelStore()
const roomTypeStore = useRoomTypeStore()

// 响应式数据
const loading = ref(false)
const filterType = ref('')
const selectedRoomType = ref<number | undefined>()
const dateRange = ref<[string, string] | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)
const showTrendChart = ref(false)
const showDetailDialog = ref(false)
const selectedHistoryItem = ref<PriceHistory | null>(null)

// 计算属性
const priceHistory = computed(() => pricingStore.priceHistory)
const total = computed(() => pricingStore.total)
const statistics = computed(() => pricingStore.priceChangeStatistics)
const trendData = computed(() => pricingStore.priceChangeTrend)
const roomTypes = computed(() => roomTypeStore.roomTypes)

// 生命周期
onMounted(async () => {
  await Promise.all([
    loadRoomTypes(),
    loadPriceHistory(),
    loadStatistics()
  ])
})

// 方法
async function loadRoomTypes() {
  try {
    await roomTypeStore.fetchRoomTypes({ page: 0, size: 1000 })
  } catch (error) {
    console.error('Failed to load room types:', error)
  }
}

async function loadPriceHistory() {
  loading.value = true
  try {
    const hotelId = hotelStore.currentHotel?.id
    if (!hotelId) {
      ElMessage.warning('请先选择酒店')
      return
    }

    await pricingStore.fetchPriceHistory({
      hotelId,
      changeType: filterType.value,
      roomTypeId: selectedRoomType.value,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1],
      page: currentPage.value - 1,
      size: pageSize.value
    })
  } catch (error) {
    ElMessage.error('加载价格历史失败')
    console.error('Failed to load price history:', error)
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  try {
    const hotelId = hotelStore.currentHotel?.id
    if (hotelId) {
      await pricingStore.fetchPriceChangeStatistics(hotelId, 30)
      await pricingStore.fetchPriceChangeTrend(hotelId, 30)
    }
  } catch (error) {
    console.error('Failed to load statistics:', error)
  }
}

function viewHistoryDetail(item: PriceHistory) {
  selectedHistoryItem.value = item
  showDetailDialog.value = true
}

async function exportHistory() {
  try {
    // 导出功能实现
    ElMessage.success('导出功能开发中...')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

// 辅助函数
function formatDate(dateTime: string) {
  return new Date(dateTime).toLocaleDateString('zh-CN')
}

function formatTime(dateTime: string) {
  return new Date(dateTime).toLocaleTimeString('zh-CN')
}

function getChangeClass(item: PriceHistory) {
  if (!item.oldPrice) return ''
  return item.newPrice > item.oldPrice ? 'price-increase' : 'price-decrease'
}

function getChangeText(item: PriceHistory) {
  if (!item.oldPrice) return '新建'
  const diff = item.newPrice - item.oldPrice
  const percentage = Math.abs((diff / item.oldPrice * 100)).toFixed(1)
  return `${diff > 0 ? '+' : ''}${diff.toFixed(2)} (${percentage}%)`
}

function getChangeTypeTag(type: string) {
  const tagMap: Record<string, string> = {
    'BASE_PRICE': 'primary',
    'DYNAMIC_RULE': 'warning',
    'SPECIAL_PRICE': 'success',
    'MANUAL': 'info'
  }
  return tagMap[type] || 'info'
}

function getChangeTypeName(type: string) {
  const nameMap: Record<string, string> = {
    'BASE_PRICE': '基础价格',
    'DYNAMIC_RULE': '动态规则',
    'SPECIAL_PRICE': '特殊价格',
    'MANUAL': '手动调整'
  }
  return nameMap[type] || type
}
</script>

<style scoped>
.price-history-manager {
  padding: 0;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 16px;
}

.stat-icon.increase {
  background: #ef4444;
}

.stat-icon.decrease {
  background: #10b981;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.filter-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-right {
  display: flex;
  gap: 12px;
}

.trend-chart {
  background: white;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.history-list {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.time-cell {
  line-height: 1.4;
}

.time-cell .date {
  font-weight: 500;
  color: #1f2937;
}

.time-cell .time {
  font-size: 12px;
  color: #6b7280;
}

.room-info {
  line-height: 1.4;
}

.room-type {
  font-weight: 500;
  color: #1f2937;
}

.room-number {
  font-size: 12px;
  color: #6b7280;
}

.price-change {
  line-height: 1.4;
}

.old-price {
  font-size: 12px;
  color: #6b7280;
  text-decoration: line-through;
}

.new-price {
  font-weight: 600;
  color: #1f2937;
}

.change-info {
  font-size: 12px;
  font-weight: 500;
}

.price-increase {
  color: #ef4444;
}

.price-decrease {
  color: #10b981;
}

.reason-text {
  color: #374151;
}

.no-reason {
  color: #9ca3af;
  font-style: italic;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid #e5e7eb;
}
</style>