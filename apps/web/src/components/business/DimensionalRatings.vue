<template>
  <div class="dimensional-ratings">
    <div class="ratings-header">
      <h3 class="ratings-title">{{ title }}</h3>
      <div class="overall-avg" v-if="showAverage">
        平均分: {{ averageScore.toFixed(1) }}
      </div>
    </div>

    <div class="ratings-content">
      <!-- 图表模式 -->
      <div v-if="mode === 'chart'" class="chart-mode">
        <div class="chart-container">
          <canvas ref="chartCanvas" :width="chartSize" :height="chartSize"></canvas>
        </div>
        <div class="chart-legend">
          <div
            v-for="(item, index) in dimensionData"
            :key="index"
            class="legend-item"
          >
            <div
              class="legend-color"
              :style="{ backgroundColor: getDimensionColor(index) }"
            ></div>
            <span class="legend-label">{{ item.label }}</span>
            <span class="legend-score">{{ item.score.toFixed(1) }}</span>
          </div>
        </div>
      </div>

      <!-- 列表模式 -->
      <div v-else-if="mode === 'list'" class="list-mode">
        <div
          v-for="(item, index) in dimensionData"
          :key="index"
          class="rating-item"
        >
          <div class="rating-info">
            <div class="rating-icon">{{ item.icon }}</div>
            <div class="rating-details">
              <div class="rating-label">{{ item.label }}</div>
              <div class="rating-description">{{ item.description }}</div>
            </div>
          </div>
          <div class="rating-display">
            <div class="rating-bar">
              <div
                class="rating-fill"
                :style="{
                  width: `${(item.score / 5) * 100}%`,
                  backgroundColor: getDimensionColor(index)
                }"
              ></div>
            </div>
            <div class="rating-score">{{ item.score.toFixed(1) }}</div>
          </div>
        </div>
      </div>

      <!-- 网格模式 -->
      <div v-else-if="mode === 'grid'" class="grid-mode">
        <div class="grid-container">
          <div
            v-for="(item, index) in dimensionData"
            :key="index"
            class="grid-item"
          >
            <div class="grid-icon">{{ item.icon }}</div>
            <div class="grid-label">{{ item.label }}</div>
            <RatingStars :rating="Math.round(item.score)" :size="starSize" readonly />
            <div class="grid-score">{{ item.score.toFixed(1) }}</div>
          </div>
        </div>
      </div>

      <!-- 圆形进度条模式 -->
      <div v-else-if="mode === 'circular'" class="circular-mode">
        <div class="circular-container">
          <div
            v-for="(item, index) in dimensionData"
            :key="index"
            class="circular-item"
          >
            <div class="circular-progress">
              <svg :width="circularSize" :height="circularSize">
                <circle
                  :cx="circularSize / 2"
                  :cy="circularSize / 2"
                  :r="circularRadius"
                  fill="none"
                  stroke="#f0f0f0"
                  :stroke-width="strokeWidth"
                />
                <circle
                  :cx="circularSize / 2"
                  :cy="circularSize / 2"
                  :r="circularRadius"
                  fill="none"
                  :stroke="getDimensionColor(index)"
                  :stroke-width="strokeWidth"
                  stroke-linecap="round"
                  :stroke-dasharray="circumference"
                  :stroke-dashoffset="getCircularOffset(item.score)"
                  transform="rotate(-90, circularSize/2, circularSize/2)"
                />
              </svg>
              <div class="circular-content">
                <div class="circular-score">{{ item.score.toFixed(1) }}</div>
              </div>
            </div>
            <div class="circular-info">
              <div class="circular-icon">{{ item.icon }}</div>
              <div class="circular-label">{{ item.label }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import RatingStars from './RatingStars.vue'

interface DimensionData {
  label: string
  score: number
  icon: string
  description: string
}

interface Props {
  title?: string
  data?: {
    cleanlinessRating: number
    serviceRating: number
    facilitiesRating: number
    locationRating: number
    overallRating?: number
  }
  mode?: 'chart' | 'list' | 'grid' | 'circular'
  showAverage?: boolean
  colors?: string[]
  size?: 'small' | 'medium' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  title: '多维度评分',
  mode: 'list',
  showAverage: true,
  colors: () => ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6'],
  size: 'medium'
})

// 响应式数据
const chartCanvas = ref<HTMLCanvasElement>()

// 默认数据
const defaultData = {
  cleanlinessRating: 4.2,
  serviceRating: 4.5,
  facilitiesRating: 3.8,
  locationRating: 4.1,
  overallRating: 4.2
}

// 计算属性
const dimensionData = computed((): DimensionData[] => {
  const data = props.data || defaultData
  return [
    {
      label: '清洁度',
      score: data.cleanlinessRating,
      icon: '🧹',
      description: '房间卫生程度'
    },
    {
      label: '服务态度',
      score: data.serviceRating,
      icon: '🤝',
      description: '员工服务质量'
    },
    {
      label: '设施设备',
      score: data.facilitiesRating,
      icon: '🏨',
      description: '酒店设施完善度'
    },
    {
      label: '地理位置',
      score: data.locationRating,
      icon: '📍',
      description: '交通便利程度'
    }
  ]
})

const averageScore = computed(() => {
  const scores = dimensionData.value.map(item => item.score)
  return scores.reduce((sum, score) => sum + score, 0) / scores.length
})

const chartSize = computed(() => {
  const sizes = { small: 200, medium: 250, large: 300 }
  return sizes[props.size]
})

const circularSize = computed(() => {
  const sizes = { small: 80, medium: 100, large: 120 }
  return sizes[props.size]
})

const circularRadius = computed(() => (circularSize.value / 2) - 10)
const strokeWidth = computed(() => props.size === 'small' ? 4 : 6)
const circumference = computed(() => 2 * Math.PI * circularRadius.value)
const starSize = computed(() => props.size === 'small' ? 'small' : props.size === 'medium' ? 'medium' : 'large')

// 方法
const getDimensionColor = (index: number): string => {
  return props.colors[index % props.colors.length] || '#6b7280'
}

const getCircularOffset = (score: number): number => {
  const percentage = score / 5
  return circumference.value - (percentage * circumference.value)
}

const drawChart = () => {
  if (!chartCanvas.value || props.mode !== 'chart') return

  const canvas = chartCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const centerX = canvas.width / 2
  const centerY = canvas.height / 2
  const radius = Math.min(centerX, centerY) - 40
  const angleStep = (Math.PI * 2) / dimensionData.value.length

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // 绘制网格
  for (let level = 1; level <= 5; level++) {
    ctx.beginPath()
    ctx.strokeStyle = '#e0e0e0'
    ctx.lineWidth = 1

    for (let i = 0; i <= dimensionData.value.length; i++) {
      const angle = i * angleStep - Math.PI / 2
      const x = centerX + Math.cos(angle) * (radius * level / 5)
      const y = centerY + Math.sin(angle) * (radius * level / 5)

      if (i === 0) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }
    }
    ctx.closePath()
    ctx.stroke()
  }

  // 绘制轴线
  for (let i = 0; i <= dimensionData.value.length; i++) {
    const angle = i * angleStep - Math.PI / 2
    ctx.beginPath()
    ctx.moveTo(centerX, centerY)
    ctx.lineTo(
      centerX + Math.cos(angle) * radius,
      centerY + Math.sin(angle) * radius
    )
    ctx.strokeStyle = '#d0d0d0'
    ctx.stroke()
  }

  // 绘制数据区域
  ctx.beginPath()
  ctx.fillStyle = 'rgba(0, 123, 255, 0.3)'
  ctx.strokeStyle = '#007bff'
  ctx.lineWidth = 2

  dimensionData.value.forEach((item, index) => {
    const angle = index * angleStep - Math.PI / 2
    const value = item.score / 5
    const x = centerX + Math.cos(angle) * (radius * value)
    const y = centerY + Math.sin(angle) * (radius * value)

    if (index === 0) {
      ctx.moveTo(x, y)
    } else {
      ctx.lineTo(x, y)
    }
  })

  ctx.closePath()
  ctx.fill()
  ctx.stroke()

  // 绘制数据点
  dimensionData.value.forEach((item, index) => {
    const angle = index * angleStep - Math.PI / 2
    const value = item.score / 5
    const x = centerX + Math.cos(angle) * (radius * value)
    const y = centerY + Math.sin(angle) * (radius * value)

    ctx.beginPath()
    ctx.arc(x, y, 4, 0, Math.PI * 2)
    ctx.fillStyle = getDimensionColor(index)
    ctx.fill()
    ctx.strokeStyle = 'white'
    ctx.lineWidth = 2
    ctx.stroke()
  })
}

// 监听数据变化
watch(() => props.data, () => {
  drawChart()
}, { deep: true })

// 生命周期
onMounted(() => {
  drawChart()
})
</script>

<style scoped>
.dimensional-ratings {
  background: white;
  border-radius: 8px;
  border: 1px solid #eee;
  overflow: hidden;
}

.ratings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.ratings-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.overall-avg {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.ratings-content {
  padding: 24px;
}

/* 图表模式 */
.chart-mode {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.chart-container {
  position: relative;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-label {
  font-size: 14px;
  color: #333;
}

.legend-score {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

/* 列表模式 */
.list-mode {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rating-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.rating-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-icon {
  font-size: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 50%;
  flex-shrink: 0;
}

.rating-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rating-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.rating-description {
  font-size: 12px;
  color: #666;
}

.rating-display {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  max-width: 200px;
}

.rating-bar {
  flex: 1;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.rating-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 1s ease-out;
}

.rating-score {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  min-width: 32px;
  text-align: right;
}

/* 网格模式 */
.grid-mode {
  display: flex;
  justify-content: center;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
  width: 100%;
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  text-align: center;
}

.grid-icon {
  font-size: 28px;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 50%;
  margin-bottom: 4px;
}

.grid-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.grid-score {
  font-size: 18px;
  font-weight: 600;
  color: #ff9800;
}

/* 圆形模式 */
.circular-mode {
  display: flex;
  justify-content: center;
}

.circular-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 24px;
  width: 100%;
}

.circular-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
}

.circular-progress {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.circular-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.circular-score {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.circular-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.circular-icon {
  font-size: 20px;
}

.circular-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ratings-header {
    padding: 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .ratings-content {
    padding: 16px;
  }

  .rating-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .rating-display {
    align-self: stretch;
    max-width: none;
  }

  .grid-container {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .circular-container {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }
}
</style>