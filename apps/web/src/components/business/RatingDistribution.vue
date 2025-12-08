<template>
  <div class="rating-distribution">
    <div class="distribution-header">
      <h3 class="distribution-title">{{ title }}</h3>
      <div v-if="showTotal" class="total-count">
        总计: {{ totalCount }}条
      </div>
    </div>

    <div class="distribution-content">
      <!-- 柱状图模式 -->
      <div v-if="mode === 'bar'" class="bar-chart">
        <div
          v-for="rating in [5, 4, 3, 2, 1]"
          :key="rating"
          class="bar-item"
        >
          <div class="bar-info">
            <span class="rating-label">{{ rating }}星</span>
            <span class="rating-count">{{ getRatingCount(rating) }}</span>
          </div>
          <div class="bar-container">
            <div
              class="bar-fill"
              :style="{
                width: `${getPercentage(rating)}%`,
                backgroundColor: getBarColor(rating)
              }"
            ></div>
          </div>
          <div class="bar-percentage">
            {{ getPercentage(rating).toFixed(1) }}%
          </div>
        </div>
      </div>

      <!-- 饼图模式 -->
      <div v-else-if="mode === 'pie'" class="pie-chart">
        <div class="pie-container">
          <canvas ref="pieCanvas" :width="pieSize" :height="pieSize"></canvas>
        </div>
        <div class="pie-legend">
          <div
            v-for="rating in [5, 4, 3, 2, 1]"
            :key="rating"
            class="legend-item"
          >
            <div
              class="legend-color"
              :style="{ backgroundColor: getBarColor(rating) }"
            ></div>
            <span class="legend-label">{{ rating }}星</span>
            <span class="legend-value">{{ getRatingCount(rating) }}</span>
          </div>
        </div>
      </div>

      <!-- 雷达图模式 -->
      <div v-else-if="mode === 'radar'" class="radar-chart">
        <div class="radar-container">
          <canvas ref="radarCanvas" :width="radarSize" :height="radarSize"></canvas>
        </div>
        <div class="radar-labels">
          <div
            v-for="(label, index) in radarLabels"
            :key="index"
            class="radar-label"
            :style="getLabelPosition(index)"
          >
            {{ label }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'

interface Props {
  title?: string
  data: {
    rating5: number
    rating4: number
    rating3: number
    rating2: number
    rating1: number
  }
  mode?: 'bar' | 'pie' | 'radar'
  showTotal?: boolean
  colors?: Record<number, string>
  size?: 'small' | 'medium' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  title: '评分分布',
  mode: 'bar',
  showTotal: true,
  colors: () => ({
    5: '#22c55e',
    4: '#84cc16',
    3: '#eab308',
    2: '#f97316',
    1: '#ef4444'
  }),
  size: 'medium'
})

// 响应式数据
const pieCanvas = ref<HTMLCanvasElement>()
const radarCanvas = ref<HTMLCanvasElement>()

// 计算属性
const totalCount = computed(() => {
  return Object.values(props.data).reduce((sum, count) => sum + count, 0)
})

const pieSize = computed(() => {
  const sizes = { small: 200, medium: 250, large: 300 }
  return sizes[props.size]
})

const radarSize = computed(() => {
  const sizes = { small: 200, medium: 250, large: 300 }
  return sizes[props.size]
})

const radarLabels = ['总体', '清洁度', '服务', '设施', '位置']

// 方法
const getRatingCount = (rating: number): number => {
  const key = `rating${rating}` as keyof typeof props.data
  return props.data[key] || 0
}

const getPercentage = (rating: number): number => {
  if (totalCount.value === 0) return 0
  return (getRatingCount(rating) / totalCount.value) * 100
}

const getBarColor = (rating: number): string => {
  return props.colors[rating] || '#6b7280'
}

const getLabelPosition = (index: number) => {
  const angle = (index * 72 - 90) * Math.PI / 180
  const radius = radarSize.value / 2 + 20
  const x = Math.cos(angle) * radius + radarSize.value / 2
  const y = Math.sin(angle) * radius + radarSize.value / 2
  return {
    left: `${x}px`,
    top: `${y}px`,
    transform: 'translate(-50%, -50%)'
  }
}

// 绘制饼图
const drawPieChart = () => {
  if (!pieCanvas.value) return

  const canvas = pieCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const centerX = canvas.width / 2
  const centerY = canvas.height / 2
  const radius = Math.min(centerX, centerY) - 10

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  let currentAngle = -Math.PI / 2

  Object.entries(props.data).forEach(([rating, count]) => {
    if (count === 0) return

    const percentage = count / totalCount.value
    const angle = percentage * Math.PI * 2

    // 绘制扇形
    ctx.beginPath()
    ctx.moveTo(centerX, centerY)
    ctx.arc(centerX, centerY, radius, currentAngle, currentAngle + angle)
    ctx.closePath()
    ctx.fillStyle = props.colors[parseInt(rating.replace('rating', ''))] || '#6b7280'
    ctx.fill()

    // 绘制边框
    ctx.strokeStyle = 'white'
    ctx.lineWidth = 2
    ctx.stroke()

    currentAngle += angle
  })
}

// 绘制雷达图
const drawRadarChart = () => {
  if (!radarCanvas.value) return

  const canvas = radarCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const centerX = canvas.width / 2
  const centerY = canvas.height / 2
  const radius = Math.min(centerX, centerY) - 40
  const levels = 5

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // 绘制网格
  for (let level = 1; level <= levels; level++) {
    ctx.beginPath()
    ctx.strokeStyle = '#e0e0e0'
    ctx.lineWidth = 1

    for (let i = 0; i <= 5; i++) {
      const angle = (i * 72 - 90) * Math.PI / 180
      const x = centerX + Math.cos(angle) * (radius * level / levels)
      const y = centerY + Math.sin(angle) * (radius * level / levels)

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
  for (let i = 0; i <= 5; i++) {
    const angle = (i * 72 - 90) * Math.PI / 180
    ctx.beginPath()
    ctx.moveTo(centerX, centerY)
    ctx.lineTo(
      centerX + Math.cos(angle) * radius,
      centerY + Math.sin(angle) * radius
    )
    ctx.strokeStyle = '#d0d0d0'
    ctx.stroke()
  }

  // 绘制数据区域（使用评分数据）
  const data = [
    4.5, // 总体（模拟数据）
    4.2, // 清洁度（模拟数据）
    4.0, // 服务（模拟数据）
    3.8, // 设施（模拟数据）
    4.1  // 位置（模拟数据）
  ]

  ctx.beginPath()
  ctx.fillStyle = 'rgba(0, 123, 255, 0.3)'
  ctx.strokeStyle = '#007bff'
  ctx.lineWidth = 2

  for (let i = 0; i <= 5; i++) {
    const angle = (i * 72 - 90) * Math.PI / 180
    const value = data[i] || 0
    const r = (value / 5) * radius
    const x = centerX + Math.cos(angle) * r
    const y = centerY + Math.sin(angle) * r

    if (i === 0) {
      ctx.moveTo(x, y)
    } else {
      ctx.lineTo(x, y)
    }
  }
  ctx.closePath()
  ctx.fill()
  ctx.stroke()
}

// 绘制图表
const drawChart = () => {
  nextTick(() => {
    if (props.mode === 'pie') {
      drawPieChart()
    } else if (props.mode === 'radar') {
      drawRadarChart()
    }
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
.rating-distribution {
  background: white;
  border-radius: 8px;
  border: 1px solid #eee;
  overflow: hidden;
}

.distribution-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.distribution-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.total-count {
  font-size: 14px;
  color: #666;
}

.distribution-content {
  padding: 24px;
}

/* 柱状图样式 */
.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bar-item {
  display: grid;
  grid-template-columns: 50px 1fr 60px;
  align-items: center;
  gap: 12px;
}

.bar-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.rating-label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.rating-count {
  font-size: 12px;
  color: #666;
}

.bar-container {
  height: 24px;
  background: #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
}

.bar-fill {
  height: 100%;
  border-radius: 12px;
  transition: width 1s ease-out;
  position: relative;
}

.bar-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, rgba(255,255,255,0.2) 0%, transparent 100%);
  border-radius: inherit;
}

.bar-percentage {
  font-size: 12px;
  color: #666;
  text-align: right;
  font-weight: 500;
}

/* 饼图样式 */
.pie-chart {
  display: flex;
  align-items: center;
  gap: 32px;
}

.pie-container {
  flex-shrink: 0;
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
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
  flex-shrink: 0;
}

.legend-label {
  font-size: 14px;
  color: #333;
  min-width: 30px;
}

.legend-value {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  margin-left: auto;
}

/* 雷达图样式 */
.radar-chart {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
}

.radar-container {
  position: relative;
}

.radar-labels {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.radar-label {
  position: absolute;
  font-size: 12px;
  color: #666;
  font-weight: 500;
  white-space: nowrap;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .distribution-header {
    padding: 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .distribution-content {
    padding: 16px;
  }

  .bar-item {
    grid-template-columns: 40px 1fr 50px;
    gap: 8px;
  }

  .pie-chart {
    flex-direction: column;
    gap: 20px;
  }

  .pie-legend {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>