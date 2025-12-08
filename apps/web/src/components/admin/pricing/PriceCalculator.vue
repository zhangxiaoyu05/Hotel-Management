<template>
  <div class="price-calculator">
    <div class="calculator-layout">
      <!-- 左侧：输入表单 -->
      <div class="calculator-form">
        <el-card title="价格计算器" class="form-card">
          <el-form
            ref="formRef"
            :model="calculationForm"
            :rules="formRules"
            label-width="120px"
          >
            <el-form-item label="酒店" prop="hotelId">
              <el-select
                v-model="calculationForm.hotelId"
                placeholder="请选择酒店"
                style="width: 100%"
                @change="handleHotelChange"
              >
                <el-option
                  v-for="hotel in hotels"
                  :key="hotel.id"
                  :label="hotel.name"
                  :value="hotel.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="房间类型" prop="roomTypeId">
              <el-select
                v-model="calculationForm.roomTypeId"
                placeholder="请选择房间类型"
                style="width: 100%"
                @change="handleRoomTypeChange"
              >
                <el-option
                  v-for="roomType in roomTypes"
                  :key="roomType.id"
                  :label="`${roomType.name} (基础价: ¥${roomType.basePrice})`"
                  :value="roomType.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="具体房间">
              <el-select
                v-model="calculationForm.roomId"
                placeholder="可选，不选择则计算房间类型价格"
                clearable
                style="width: 100%"
                @change="handleRoomChange"
              >
                <el-option
                  v-for="room in rooms"
                  :key="room.id"
                  :label="`${room.roomNumber} (当前价: ¥${room.price})`"
                  :value="room.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="入住日期" prop="startDate">
              <el-date-picker
                v-model="calculationForm.startDate"
                type="date"
                placeholder="选择入住日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledDate"
                style="width: 100%"
                @change="handleDateChange"
              />
            </el-form-item>

            <el-form-item label="退房日期" prop="endDate">
              <el-date-picker
                v-model="calculationForm.endDate"
                type="date"
                placeholder="选择退房日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledEndDate"
                style="width: 100%"
                @change="handleDateChange"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                @click="calculatePrice"
                :loading="calculating"
                :disabled="!canCalculate"
              >
                计算价格
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 快速设置 -->
        <el-card title="快速设置" class="quick-settings">
          <div class="quick-dates">
            <el-button
              v-for="preset in datePresets"
              :key="preset.label"
              size="small"
              @click="setDatePreset(preset)"
            >
              {{ preset.label }}
            </el-button>
          </div>
        </el-card>
      </div>

      <!-- 右侧：结果展示 -->
      <div class="calculator-result">
        <el-card v-if="!calculationResult" title="计算结果" class="result-card">
          <div class="empty-result">
            <el-icon size="64" color="#d1d5db">
              <Calculator />
            </el-icon>
            <p>请填写表单并点击"计算价格"按钮</p>
          </div>
        </el-card>

        <el-card v-else title="计算结果" class="result-card">
          <!-- 价格总览 -->
          <div class="price-overview">
            <div class="overview-item">
              <div class="overview-label">总价格</div>
              <div class="overview-value total">¥{{ calculationResult.totalPrice }}</div>
            </div>
            <div class="overview-item">
              <div class="overview-label">平均每日价格</div>
              <div class="overview-value">¥{{ calculationResult.averageDailyPrice }}</div>
            </div>
            <div class="overview-item">
              <div class="overview-label">入住天数</div>
              <div class="overview-value">{{ calculationResult.totalDays }}晚</div>
            </div>
          </div>

          <el-divider />

          <!-- 每日价格明细 -->
          <div class="daily-prices">
            <h4>每日价格明细</h4>
            <div class="price-list">
              <div
                v-for="(price, date) in calculationResult.dailyPrices"
                :key="date"
                class="price-item"
              >
                <div class="price-date">{{ formatDate(date) }}</div>
                <div class="price-value">¥{{ price }}</div>
                <div class="price-tags">
                  <el-tag
                    v-for="tag in getDateTags(date, price)"
                    :key="tag.text"
                    :type="tag.type"
                    size="small"
                  >
                    {{ tag.text }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>

          <el-divider />

          <!-- 应用的规则 -->
          <div v-if="calculationResult.appliedRules?.length" class="applied-rules">
            <h4>应用的价格规则</h4>
            <div class="rules-list">
              <div
                v-for="rule in calculationResult.appliedRules"
                :key="rule.ruleId"
                class="rule-item"
              >
                <div class="rule-name">{{ rule.ruleName }}</div>
                <div class="rule-detail">
                  {{ rule.adjustmentType === 'PERCENTAGE' ? '百分比' : '固定金额' }}
                  调整 {{ rule.adjustmentValue
                  }}{{ rule.adjustmentType === 'PERCENTAGE' ? '%' : '元' }}
                </div>
              </div>
            </div>
          </div>

          <!-- 特殊价格 -->
          <div v-if="calculationResult.specialPrices?.length" class="special-prices">
            <h4>特殊价格日期</h4>
            <div class="special-list">
              <div
                v-for="special in calculationResult.specialPrices"
                :key="special.date"
                class="special-item"
              >
                <div class="special-date">{{ formatDate(special.date) }}</div>
                <div class="special-price">¥{{ special.specialPrice }}</div>
                <div class="special-reason">{{ special.reason || '无原因' }}</div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="result-actions">
            <el-button @click="exportResult" :icon="Download">
              导出结果
            </el-button>
            <el-button @click="saveAsSpecialPrice" type="success">
              设置为特殊价格
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Calculator, Download } from '@element-plus/icons-vue'
import { useHotelStore } from '../../../stores/hotelStore'
import { useRoomTypeStore } from '../../../stores/roomTypeStore'
import { useRoomStore } from '../../../stores/roomStore'
import { usePricingStore } from '../../../stores/pricingStore'
import type { PriceCalculationRequest, PriceCalculationResponse } from '../../../types/pricing'

const hotelStore = useHotelStore()
const roomTypeStore = useRoomTypeStore()
const roomStore = useRoomStore()
const pricingStore = usePricingStore()

// 响应式数据
const formRef = ref<FormInstance>()
const calculating = ref(false)
const calculationResult = ref<PriceCalculationResponse | null>(null)

const calculationForm = ref<PriceCalculationRequest>({
  hotelId: undefined,
  roomTypeId: undefined,
  roomId: undefined,
  startDate: '',
  endDate: ''
})

// 计算属性
const hotels = computed(() => hotelStore.hotels)
const roomTypes = computed(() => roomTypeStore.roomTypes)
const rooms = computed(() => roomStore.rooms)

const canCalculate = computed(() => {
  return calculationForm.value.hotelId &&
         calculationForm.value.roomTypeId &&
         calculationForm.value.startDate &&
         calculationForm.value.endDate &&
         calculationForm.value.startDate < calculationForm.value.endDate
})

// 表单验证规则
const formRules: FormRules = {
  hotelId: [
    { required: true, message: '请选择酒店', trigger: 'change' }
  ],
  roomTypeId: [
    { required: true, message: '请选择房间类型', trigger: 'change' }
  ],
  startDate: [
    { required: true, message: '请选择入住日期', trigger: 'change' }
  ],
  endDate: [
    { required: true, message: '请选择退房日期', trigger: 'change' }
  ]
}

// 日期预设
const datePresets = [
  { label: '今晚', start: 0, end: 1 },
  { label: '周末', start: getWeekendStart(), end: getWeekendEnd() },
  { label: '3天2晚', start: 0, end: 3 },
  { label: '一周', start: 0, end: 7 },
  { label: '下个月', start: 30, end: 37 }
]

// 生命周期
onMounted(async () => {
  await loadHotels()
})

// 方法
async function loadHotels() {
  try {
    await hotelStore.fetchHotels({ page: 0, size: 100 })
  } catch (error) {
    console.error('Failed to load hotels:', error)
  }
}

async function handleHotelChange() {
  if (calculationForm.value.hotelId) {
    await loadRoomTypes()
    calculationForm.value.roomTypeId = undefined
    calculationForm.value.roomId = undefined
  }
}

async function loadRoomTypes() {
  try {
    await roomTypeStore.fetchRoomTypes({
      hotelId: calculationForm.value.hotelId,
      page: 0,
      size: 100
    })
  } catch (error) {
    console.error('Failed to load room types:', error)
  }
}

async function handleRoomTypeChange() {
  if (calculationForm.value.roomTypeId) {
    await loadRooms()
    calculationForm.value.roomId = undefined
  }
}

async function loadRooms() {
  try {
    await roomStore.fetchRooms({
      hotelId: calculationForm.value.hotelId,
      roomTypeId: calculationForm.value.roomTypeId,
      page: 0,
      size: 100
    })
  } catch (error) {
    console.error('Failed to load rooms:', error)
  }
}

function handleRoomChange() {
  // 房间变化时不需要特殊处理
}

function handleDateChange() {
  // 日期变化时清除之前的计算结果
  calculationResult.value = null
}

async function calculatePrice() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    calculating.value = true

    const result = await pricingService.calculatePriceRange(calculationForm.value)
    calculationResult.value = result

    ElMessage.success('价格计算成功')
  } catch (error) {
    console.error('Failed to calculate price:', error)
    ElMessage.error('价格计算失败')
  } finally {
    calculating.value = false
  }
}

function resetForm() {
  calculationForm.value = {
    hotelId: undefined,
    roomTypeId: undefined,
    roomId: undefined,
    startDate: '',
    endDate: ''
  }
  calculationResult.value = null
}

function setDatePreset(preset: any) {
  const today = new Date()
  const startDate = new Date(today.getTime() + preset.start * 24 * 60 * 60 * 1000)
  const endDate = new Date(today.getTime() + preset.end * 24 * 60 * 60 * 1000)

  calculationForm.value.startDate = startDate.toISOString().split('T')[0]
  calculationForm.value.endDate = endDate.toISOString().split('T')[0]

  handleDateChange()
}

function disabledDate(date: Date) {
  // 不能选择过去的日期
  return date < new Date(new Date().setHours(0, 0, 0, 0))
}

function disabledEndDate(date: Date) {
  if (!calculationForm.value.startDate) {
    return disabledDate(date)
  }
  const startDate = new Date(calculationForm.value.startDate)
  return date <= startDate
}

function exportResult() {
  // 导出功能实现
  ElMessage.success('导出功能开发中...')
}

function saveAsSpecialPrice() {
  // 保存为特殊价格功能实现
  ElMessage.success('设置特殊价格功能开发中...')
}

// 辅助函数
function formatDate(date: string) {
  return new Date(date).toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    weekday: 'short'
  })
}

function getDateTags(date: string, price: number) {
  const tags = []
  const dateObj = new Date(date)

  // 检查是否为周末
  if (dateObj.getDay() === 0 || dateObj.getDay() === 6) {
    tags.push({ type: 'warning', text: '周末' })
  }

  // 检查是否为节假日
  if (calculationResult.value?.holidayDates?.includes(date)) {
    tags.push({ type: 'danger', text: '节假日' })
  }

  // 检查是否为特殊价格
  const specialPrice = calculationResult.value?.specialPrices?.find(s => s.date === date)
  if (specialPrice) {
    tags.push({ type: 'success', text: '特殊价' })
  }

  return tags
}

function getWeekendStart() {
  const today = new Date()
  const daysUntilSaturday = (6 - today.getDay() + 7) % 7
  return daysUntilSaturday || 7 // 如果今天是周六，则指向下个周六
}

function getWeekendEnd() {
  return getWeekendStart() + 1 // 周日
}

// 模拟价格服务
const pricingService = {
  async calculatePriceRange(request: PriceCalculationRequest) {
    // 这里应该调用真实的API
    // 模拟返回数据
    const startDate = new Date(request.startDate!)
    const endDate = new Date(request.endDate!)
    const days = Math.ceil((endDate.getTime() - startDate.getTime()) / (24 * 60 * 60 * 1000))

    const dailyPrices: Record<string, number> = {}
    let totalPrice = 0

    for (let i = 0; i < days; i++) {
      const currentDate = new Date(startDate.getTime() + i * 24 * 60 * 60 * 1000)
      const dateStr = currentDate.toISOString().split('T')[0]

      // 模拟价格计算逻辑
      let price = 200 // 基础价格

      // 周末加价
      if (currentDate.getDay() === 0 || currentDate.getDay() === 6) {
        price *= 1.2
      }

      dailyPrices[dateStr] = price
      totalPrice += price
    }

    return {
      hotelId: request.hotelId!,
      roomId: request.roomId,
      roomTypeId: request.roomTypeId!,
      basePrice: 200,
      dailyPrices,
      totalPrice,
      averageDailyPrice: Math.round(totalPrice / days * 100) / 100,
      minDailyPrice: Math.min(...Object.values(dailyPrices)),
      maxDailyPrice: Math.max(...Object.values(dailyPrices)),
      totalDays: days,
      appliedRules: [],
      specialPrices: [],
      containsHolidays: false,
      holidayDates: []
    } as PriceCalculationResponse
  }
}
</script>

<style scoped>
.price-calculator {
  height: 100%;
}

.calculator-layout {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 24px;
  height: 100%;
}

.calculator-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-card {
  flex: 1;
}

.quick-settings {
  .quick-dates {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.calculator-result {
  min-height: 600px;
}

.result-card {
  height: 100%;
}

.empty-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #6b7280;
}

.empty-result p {
  margin-top: 16px;
  font-size: 16px;
}

.price-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.overview-item {
  text-align: center;
}

.overview-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.overview-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.overview-value.total {
  color: #3b82f6;
  font-size: 28px;
}

.daily-prices h4,
.applied-rules h4,
.special-prices h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.price-list {
  max-height: 300px;
  overflow-y: auto;
}

.price-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.price-item:last-child {
  border-bottom: none;
}

.price-date {
  font-weight: 500;
  color: #374151;
  min-width: 100px;
}

.price-value {
  font-weight: 600;
  color: #1f2937;
  min-width: 80px;
  text-align: right;
}

.price-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.rules-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
}

.rule-name {
  font-weight: 500;
  color: #1f2937;
}

.rule-detail {
  font-size: 14px;
  color: #6b7280;
}

.special-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.special-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
}

.special-date {
  font-weight: 500;
  color: #1f2937;
}

.special-price {
  font-weight: 600;
  color: #059669;
}

.special-reason {
  font-size: 14px;
  color: #6b7280;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}
</style>