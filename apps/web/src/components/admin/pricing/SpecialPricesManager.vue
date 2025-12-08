<template>
  <div class="special-prices-manager">
    <!-- 页面工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button
          type="primary"
          @click="showCreateDialog = true"
          :icon="Plus"
        >
          设置特殊价格
        </el-button>

        <el-button @click="showCalendarView = !showCalendarView">
          {{ showCalendarView ? '列表视图' : '日历视图' }}
        </el-button>
      </div>

      <div class="toolbar-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateFilter"
        />

        <el-select
          v-model="selectedRoomType"
          placeholder="房间类型"
          clearable
          style="width: 150px"
          @change="loadSpecialPrices"
        >
          <el-option
            v-for="roomType in roomTypes"
            :key="roomType.id"
            :label="roomType.name"
            :value="roomType.id"
          />
        </el-select>

        <el-input
          v-model="searchQuery"
          placeholder="搜索房间号..."
          prefix-icon="Search"
          clearable
          style="width: 200px"
          @input="handleSearch"
        />
      </div>
    </div>

    <!-- 日历视图 -->
    <div v-if="showCalendarView" class="calendar-view">
      <SpecialPriceCalendar
        :special-prices="specialPrices"
        :room-types="roomTypes"
        :date-range="calendarDateRange"
        @date-click="handleDateClick"
        @price-edit="editSpecialPrice"
      />
    </div>

    <!-- 列表视图 -->
    <div v-else class="list-view">
      <el-table
        :data="specialPrices"
        v-loading="loading"
        row-key="id"
      >
        <el-table-column prop="date" label="日期" width="120" sortable>
          <template #default="{ row }">
            <span class="date-cell">{{ formatDate(row.date) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="房间信息" min-width="180">
          <template #default="{ row }">
            <div class="room-info">
              <div class="room-type">{{ row.roomTypeName }}</div>
              <div v-if="row.roomNumber" class="room-number">房间号: {{ row.roomNumber }}</div>
              <div v-else class="room-number">该类型所有房间</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格信息" width="150">
          <template #default="{ row }">
            <div class="price-info">
              <div class="special-price">¥{{ row.price }}</div>
              <div v-if="row.basePrice" class="base-price">
                原价: ¥{{ row.basePrice }}
              </div>
              <div v-if="row.basePrice" class="price-change">
                {{ getPriceChangeText(row.price, row.basePrice) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="reason" label="设置原因" min-width="150">
          <template #default="{ row }">
            <span v-if="row.reason" class="reason-text">{{ row.reason }}</span>
            <span v-else class="no-reason">未填写原因</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.date)">
              {{ getStatusText(row.date) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="editSpecialPrice(row)"
              :icon="Edit"
            >
              编辑
            </el-button>
            <el-button
              link
              @click="copySpecialPrice(row)"
              :icon="CopyDocument"
            >
              复制
            </el-button>
            <el-button
              link
              type="danger"
              @click="deleteSpecialPrice(row)"
              :icon="Delete"
            >
              删除
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
          @size-change="loadSpecialPrices"
          @current-change="loadSpecialPrices"
        />
      </div>
    </div>

    <!-- 创建/编辑特殊价格对话框 -->
    <SpecialPriceDialog
      v-model="showCreateDialog"
      :special-price="editingSpecialPrice"
      :room-types="roomTypes"
      @success="handleDialogSuccess"
    />

    <!-- 复制特殊价格对话框 -->
    <CopySpecialPriceDialog
      v-model="showCopyDialog"
      :special-price="copyingSpecialPrice"
      @success="handleCopySuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, CopyDocument } from '@element-plus/icons-vue'
import { usePricingStore } from '../../../stores/pricingStore'
import { useHotelStore } from '../../../stores/hotelStore'
import { useRoomTypeStore } from '../../../stores/roomTypeStore'
import type { SpecialPrice } from '../../../types/pricing'
import SpecialPriceDialog from './SpecialPriceDialog.vue'
import SpecialPriceCalendar from './SpecialPriceCalendar.vue'
import CopySpecialPriceDialog from './CopySpecialPriceDialog.vue'

const pricingStore = usePricingStore()
const hotelStore = useHotelStore()
const roomTypeStore = useRoomTypeStore()

// 响应式数据
const loading = ref(false)
const searchQuery = ref('')
const selectedRoomType = ref<number | undefined>()
const dateRange = ref<[string, string] | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)
const showCalendarView = ref(false)
const showCreateDialog = ref(false)
const showCopyDialog = ref(false)
const editingSpecialPrice = ref<SpecialPrice | null>(null)
const copyingSpecialPrice = ref<SpecialPrice | null>(null)

// 计算属性
const specialPrices = computed(() => pricingStore.specialPrices)
const total = computed(() => pricingStore.total)
const roomTypes = computed(() => roomTypeStore.roomTypes)

const calendarDateRange = computed(() => {
  if (dateRange.value) {
    return {
      start: dateRange.value[0],
      end: dateRange.value[1]
    }
  }
  // 默认显示当前月份
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return {
    start: `${year}-${month}-01`,
    end: `${year}-${month}-31`
  }
})

// 生命周期
onMounted(async () => {
  await Promise.all([
    loadRoomTypes(),
    loadSpecialPrices()
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

async function loadSpecialPrices() {
  loading.value = true
  try {
    const hotelId = hotelStore.currentHotel?.id
    if (!hotelId) {
      ElMessage.warning('请先选择酒店')
      return
    }

    await pricingStore.fetchSpecialPrices({
      hotelId,
      roomTypeId: selectedRoomType.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      page: currentPage.value - 1,
      size: pageSize.value
    })
  } catch (error) {
    ElMessage.error('加载特殊价格失败')
    console.error('Failed to load special prices:', error)
  } finally {
    loading.value = false
  }
}

function handleDateFilter() {
  currentPage.value = 1
  loadSpecialPrices()
}

function handleSearch() {
  currentPage.value = 1
  // 这里可以实现搜索逻辑
  loadSpecialPrices()
}

function handleDateClick(date: string) {
  // 点击日历日期时创建特殊价格
  editingSpecialPrice.value = {
    hotelId: hotelStore.currentHotel?.id || 0,
    roomTypeId: undefined,
    roomId: undefined,
    date,
    price: 0,
    reason: ''
  } as SpecialPrice
  showCreateDialog.value = true
}

function editSpecialPrice(specialPrice: SpecialPrice) {
  editingSpecialPrice.value = { ...specialPrice }
  showCreateDialog.value = true
}

function copySpecialPrice(specialPrice: SpecialPrice) {
  copyingSpecialPrice.value = specialPrice
  showCopyDialog.value = true
}

async function deleteSpecialPrice(specialPrice: SpecialPrice) {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${formatDate(specialPrice.date)} 的特殊价格吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await pricingStore.deleteSpecialPrice(specialPrice.id!)
    ElMessage.success('删除成功')
    loadSpecialPrices()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function handleDialogSuccess() {
  showCreateDialog.value = false
  editingSpecialPrice.value = null
  loadSpecialPrices()
}

function handleCopySuccess() {
  showCopyDialog.value = false
  copyingSpecialPrice.value = null
  loadSpecialPrices()
}

// 辅助函数
function formatDate(date: string) {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

function formatDateTime(dateTime: string) {
  return new Date(dateTime).toLocaleString('zh-CN')
}

function getStatusTag(date: string) {
  const today = new Date()
  const targetDate = new Date(date)

  if (targetDate < today) {
    return 'info' // 过去
  } else if (targetDate.toDateString() === today.toDateString()) {
    return 'success' // 今天
  } else {
    return 'warning' // 未来
  }
}

function getStatusText(date: string) {
  const today = new Date()
  const targetDate = new Date(date)

  if (targetDate < today) {
    return '已过期'
  } else if (targetDate.toDateString() === today.toDateString()) {
    return '生效中'
  } else {
    return '未生效'
  }
}

function getPriceChangeText(specialPrice: number, basePrice: number) {
  const diff = specialPrice - basePrice
  const percentage = (diff / basePrice * 100).toFixed(1)

  if (diff > 0) {
    return <span class="price-increase">+{percentage}%</span>
  } else if (diff < 0) {
    return <span class="price-decrease">{percentage}%</span>
  } else {
    return <span class="price-same">0%</span>
  }
}
</script>

<style scoped>
.special-prices-manager {
  height: 100%;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.calendar-view {
  background: white;
  border-radius: 8px;
  padding: 24px;
  min-height: 600px;
}

.list-view {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.date-cell {
  font-weight: 500;
  color: #1f2937;
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

.price-info {
  line-height: 1.4;
}

.special-price {
  font-weight: 600;
  color: #1f2937;
  font-size: 16px;
}

.base-price {
  font-size: 12px;
  color: #6b7280;
  text-decoration: line-through;
}

.price-change {
  font-size: 12px;
  font-weight: 500;
}

.price-increase {
  color: #ef4444;
}

.price-decrease {
  color: #10b981;
}

.price-same {
  color: #6b7280;
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