<template>
  <div class="room-availability-calendar">
    <el-card class="calendar-card">
      <template #header>
        <div class="calendar-header">
          <h3>房间可用性日历</h3>
          <div class="header-controls">
            <el-button-group>
              <el-button
                size="small"
                @click="previousMonth"
                :disabled="loading"
              >
                <i class="fas fa-chevron-left"></i>
              </el-button>
              <el-button
                size="small"
                @click="currentMonth"
                :disabled="loading"
              >
                今天
              </el-button>
              <el-button
                size="small"
                @click="nextMonth"
                :disabled="loading"
              >
                <i class="fas fa-chevron-right"></i>
              </el-button>
            </el-button-group>
          </div>
        </div>
      </template>

      <div class="calendar-container">
        <div class="month-year-display">
          {{ currentMonthYear }}
        </div>

        <el-calendar v-model="currentDate" v-loading="loading">
          <template #date-cell="{ data }">
            <div
              class="calendar-cell"
              :class="{
                'available': isDateAvailable(data.day),
                'unavailable': !isDateAvailable(data.day),
                'today': isToday(data.day),
                'selected': isSelected(data.day)
              }"
              @click="selectDate(data.day)"
            >
              <div class="day-number">{{ data.day.split('-')[2] }}</div>
              <div class="availability-status" v-if="hasAvailabilityData(data.day)">
                <el-tooltip
                  :content="getAvailabilityTooltip(data.day)"
                  placement="top"
                >
                  <div class="status-indicator">
                    <i
                      :class="isDateAvailable(data.day) ? 'fas fa-check-circle available-icon' : 'fas fa-times-circle unavailable-icon'"
                    ></i>
                  </div>
                </el-tooltip>
              </div>
            </div>
          </template>
        </el-calendar>

        <div class="calendar-legend">
          <div class="legend-item">
            <div class="legend-indicator available"></div>
            <span>可预订</span>
          </div>
          <div class="legend-item">
            <div class="legend-indicator unavailable"></div>
            <span>已预订</span>
          </div>
          <div class="legend-item">
            <div class="legend-indicator maintenance"></div>
            <span>维护中</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 日期选择范围显示 -->
    <el-card v-if="selectedDateRange.start && selectedDateRange.end" class="selection-info">
      <template #header>
        <h4>选择的时间范围</h4>
      </template>
      <div class="range-display">
        <div class="range-item">
          <label>入住日期：</label>
          <span>{{ formatDate(selectedDateRange.start) }}</span>
        </div>
        <div class="range-item">
          <label>退房日期：</label>
          <span>{{ formatDate(selectedDateRange.end) }}</span>
        </div>
        <div class="range-item">
          <label>住宿天数：</label>
          <el-tag type="success">{{ calculateNights() }}晚</el-tag>
        </div>
        <div class="range-actions">
          <el-button
            type="primary"
            size="small"
            @click="confirmSelection"
            :disabled="!isRangeAvailable()"
          >
            确认选择
          </el-button>
          <el-button
            size="small"
            @click="clearSelection"
          >
            清除选择
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { roomApi } from '@/api/room'

interface Props {
  roomId: number
  initialStartDate?: string
  initialEndDate?: string
}

interface Emits {
  (e: 'date-selected', range: { start: string; end: string }): void
  (e: 'availability-updated', availability: Map<string, boolean>): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const currentDate = ref(new Date())
const availabilityData = ref<Map<string, boolean>>(new Map())
const selectedDateRange = ref<{ start?: string; end?: string }>({})
const loading = ref(false)
const selectionMode = ref<'start' | 'end'>('start')

// 计算属性
const currentMonthYear = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth() + 1
  return `${year}年${month}月`
})

// 方法
const formatDate = (date: string): string => {
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const isToday = (date: string): boolean => {
  const today = new Date()
  const checkDate = new Date(date)
  return today.toDateString() === checkDate.toDateString()
}

const isSelected = (date: string): boolean => {
  return date === selectedDateRange.value.start || date === selectedDateRange.value.end
}

const hasAvailabilityData = (date: string): boolean => {
  return availabilityData.value.has(date)
}

const isDateAvailable = (date: string): boolean => {
  return availabilityData.value.get(date) || false
}

const getAvailabilityTooltip = (date: string): string => {
  const available = isDateAvailable(date)
  return available ? '该日期可预订' : '该日期已预订或不可用'
}

const isRangeAvailable = (): boolean => {
  if (!selectedDateRange.value.start || !selectedDateRange.value.end) {
    return false
  }

  const start = new Date(selectedDateRange.value.start)
  const end = new Date(selectedDateRange.value.end)

  for (let d = new Date(start); d < end; d.setDate(d.getDate() + 1)) {
    const dateStr = d.toISOString().split('T')[0]
    if (!isDateAvailable(dateStr)) {
      return false
    }
  }

  return true
}

const calculateNights = (): number => {
  if (!selectedDateRange.value.start || !selectedDateRange.value.end) {
    return 0
  }

  const start = new Date(selectedDateRange.value.start)
  const end = new Date(selectedDateRange.value.end)
  const timeDiff = end.getTime() - start.getTime()
  return Math.ceil(timeDiff / (1000 * 3600 * 24))
}

const selectDate = (date: string) => {
  const selectedDate = new Date(date)
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  // 不能选择过去的日期
  if (selectedDate < today) {
    ElMessage.warning('不能选择过去的日期')
    return
  }

  if (selectionMode.value === 'start') {
    selectedDateRange.value = { start: date }
    selectionMode.value = 'end'
  } else {
    if (selectedDateRange.value.start && date <= selectedDateRange.value.start) {
      // 如果选择的结束日期早于或等于开始日期，重新设置开始日期
      selectedDateRange.value = { start: date }
      selectionMode.value = 'end'
    } else {
      selectedDateRange.value.end = date
      selectionMode.value = 'start'
    }
  }
}

const clearSelection = () => {
  selectedDateRange.value = {}
  selectionMode.value = 'start'
}

const confirmSelection = () => {
  if (selectedDateRange.value.start && selectedDateRange.value.end) {
    if (!isRangeAvailable()) {
      ElMessage.error('选择的时间范围内有不可预订的日期')
      return
    }

    emit('date-selected', {
      start: selectedDateRange.value.start,
      end: selectedDateRange.value.end
    })

    ElMessage.success('日期选择成功')
  }
}

const previousMonth = () => {
  const newDate = new Date(currentDate.value)
  newDate.setMonth(newDate.getMonth() - 1)
  currentDate.value = newDate
  loadMonthAvailability()
}

const currentMonth = () => {
  currentDate.value = new Date()
  loadMonthAvailability()
}

const nextMonth = () => {
  const newDate = new Date(currentDate.value)
  newDate.setMonth(newDate.getMonth() + 1)
  currentDate.value = newDate
  loadMonthAvailability()
}

const loadMonthAvailability = async () => {
  loading.value = true
  try {
    const year = currentDate.value.getFullYear()
    const month = currentDate.value.getMonth()

    // 获取当月第一天和最后一天
    const firstDay = new Date(year, month, 1)
    const lastDay = new Date(year, month + 1, 0)

    // 加载前后几天的数据以显示完整
    const startDate = new Date(firstDay)
    startDate.setDate(startDate.getDate() - 7)
    const endDate = new Date(lastDay)
    endDate.setDate(endDate.getDate() + 7)

    const response = await roomApi.getRoomAvailability(props.roomId, {
      startDate: startDate.toISOString().split('T')[0],
      endDate: endDate.toISOString().split('T')[0]
    })

    if (response.data.success) {
      const availability = new Map<string, boolean>()
      response.data.data.forEach((item: any) => {
        availability.set(item.date, item.available)
      })

      availabilityData.value = availability
      emit('availability-updated', availability)
    }
  } catch (error) {
    console.error('加载房间可用性失败:', error)
    ElMessage.error('加载房间可用性失败')
  } finally {
    loading.value = false
  }
}

// 监听日期变化
watch(currentDate, () => {
  loadMonthAvailability()
})

// 初始化
onMounted(() => {
  loadMonthAvailability()

  // 如果有初始日期，设置选择范围
  if (props.initialStartDate) {
    selectedDateRange.value.start = props.initialStartDate
  }
  if (props.initialEndDate) {
    selectedDateRange.value.end = props.initialEndDate
    selectionMode.value = 'start'
  }
})
</script>

<style scoped>
.room-availability-calendar {
  width: 100%;
}

.calendar-card {
  margin-bottom: 20px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.calendar-header h3 {
  margin: 0;
  color: #303133;
}

.month-year-display {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
}

.calendar-container {
  padding: 0 10px;
}

.calendar-cell {
  height: 60px;
  padding: 4px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
}

.calendar-cell:hover {
  background-color: #f5f7fa;
}

.calendar-cell.available {
  background-color: #f0f9ff;
  border: 1px solid #409eff;
}

.calendar-cell.unavailable {
  background-color: #fef0f0;
  border: 1px solid #f56c6c;
}

.calendar-cell.today {
  background-color: #ecf5ff;
  border: 2px solid #409eff;
}

.calendar-cell.selected {
  background-color: #409eff;
  color: white;
}

.day-number {
  font-size: 14px;
  font-weight: 500;
}

.availability-status {
  position: absolute;
  bottom: 4px;
  right: 4px;
}

.status-indicator {
  font-size: 16px;
}

.available-icon {
  color: #67c23a;
}

.unavailable-icon {
  color: #f56c6c;
}

.calendar-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-indicator {
  width: 16px;
  height: 16px;
  border-radius: 50%;
}

.legend-indicator.available {
  background-color: #67c23a;
}

.legend-indicator.unavailable {
  background-color: #f56c6c;
}

.legend-indicator.maintenance {
  background-color: #e6a23c;
}

.selection-info {
  margin-top: 20px;
}

.range-display {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.range-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-item label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
}

.range-actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 10px;
}

/* Element Plus 日历组件样式覆盖 */
:deep(.el-calendar-table .el-calendar-day) {
  height: 60px;
}

:deep(.el-calendar-table .el-calendar-day:hover) {
  background-color: transparent;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .calendar-header {
    flex-direction: column;
    gap: 10px;
  }

  .calendar-legend {
    flex-wrap: wrap;
  }

  .range-display {
    grid-template-columns: 1fr;
  }
}
</style>