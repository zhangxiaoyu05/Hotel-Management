<template>
  <div class="metric-trend-card" :class="{ 'metric-clickable': clickable }" @click="handleClick">
    <div class="metric-header">
      <div class="metric-icon" :style="{ backgroundColor: iconColor + '20' }">
        <el-icon :size="24" :color="iconColor">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="metric-actions" v-if="$slots.actions">
        <slot name="actions"></slot>
      </div>
    </div>

    <div class="metric-body">
      <div class="metric-value" :class="{ 'metric-large': large }">
        <el-skeleton v-if="loading" animated :rows="1" />
        <span v-else>{{ formattedValue }}</span>
      </div>
      <div class="metric-label">{{ label }}</div>

      <!-- 趋势指示器 -->
      <div class="metric-trend" v-if="trend && !loading">
        <div class="trend-indicator" :class="trend.type">
          <el-icon :size="14">
            <component :is="trend.up ? ArrowUp : ArrowDown" />
          </el-icon>
          <span class="trend-value">{{ trend.displayValue }}</span>
        </div>
        <div class="trend-period">{{ trend.period }}</div>
      </div>

      <!-- 迷你图表 -->
      <div class="metric-sparkline" v-if="sparklineData && !loading">
        <div class="sparkline-container" :ref="sparklineRef"></div>
      </div>
    </div>

    <!-- 装饰性背景 -->
    <div class="metric-decoration" :style="{ backgroundColor: iconColor + '5' }"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { ChartSeries } from '@/types/dashboard'

interface Trend {
  up: boolean
  value: number | string
  displayValue: string
  type: 'success' | 'danger' | 'warning' | 'info'
  period: string
}

interface Props {
  label: string
  value: number | string
  icon: any
  color: string
  trend?: Trend
  format?: (value: number | string) => string
  large?: boolean
  loading?: boolean
  clickable?: boolean
  sparklineData?: number[]
  sparklineColor?: string
}

const props = withDefaults(defineProps<Props>(), {
  large: false,
  loading: false,
  clickable: false,
  format: (value: number | string) => value.toString(),
  sparklineColor: '#1890ff'
})

const emit = defineEmits<{
  click: []
}>()

const sparklineRef = ref<HTMLElement>()
let sparklineInstance: echarts.ECharts | null = null

const iconColor = computed(() => props.color)
const formattedValue = computed(() => props.format(props.value))

const handleClick = () => {
  if (props.clickable && !props.loading) {
    emit('click')
  }
}

const renderSparkline = () => {
  if (!sparklineRef.value || !props.sparklineData || props.sparklineData.length === 0) return

  if (sparklineInstance) {
    sparklineInstance.dispose()
  }

  sparklineInstance = echarts.init(sparklineRef.value)

  const option = {
    grid: {
      top: 0,
      left: 0,
      right: 0,
      bottom: 0
    },
    xAxis: {
      type: 'category',
      show: false,
      data: props.sparklineData.map((_, index) => index)
    },
    yAxis: {
      type: 'value',
      show: false
    },
    tooltip: {
      show: true,
      trigger: 'axis',
      formatter: (params: any) => {
        const value = params[0].value
        return `值: ${value}`
      }
    },
    series: [{
      type: 'line',
      data: props.sparklineData,
      smooth: true,
      symbol: 'none',
      lineStyle: {
        color: props.sparklineColor,
        width: 2
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [{
            offset: 0,
            color: props.sparklineColor + '40'
          }, {
            offset: 1,
            color: props.sparklineColor + '10'
          }]
        }
      }
    }]
  }

  sparklineInstance.setOption(option)
}

const resizeSparkline = () => {
  if (sparklineInstance) {
    sparklineInstance.resize()
  }
}

// 生命周期
onMounted(() => {
  nextTick(() => {
    renderSparkline()
  })

  // 监听sparkline数据变化
  watch(() => props.sparklineData, () => {
    nextTick(() => {
      renderSparkline()
    })
  }, { deep: true })

  // 监听窗口大小变化
  window.addEventListener('resize', resizeSparkline)
})

onUnmounted(() => {
  if (sparklineInstance) {
    sparklineInstance.dispose()
  }
  window.removeEventListener('resize', resizeSparkline)
})

// 导出方法供父组件调用
defineExpose({
  resizeSparkline,
  renderSparkline
})
</script>

<style scoped>
.metric-trend-card {
  position: relative;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  overflow: hidden;
  height: 140px;
}

.metric-trend-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.metric-clickable {
  cursor: pointer;
}

.metric-clickable:active {
  transform: translateY(0);
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}

.metric-trend-card:hover .metric-icon {
  transform: scale(1.05);
}

.metric-actions {
  display: flex;
  gap: 8px;
}

.metric-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
  margin-bottom: 4px;
}

.metric-value.metric-large {
  font-size: 32px;
}

.metric-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.metric-trend {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
}

.trend-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
}

.trend-indicator.success {
  background: #f6ffed;
  color: #52c41a;
}

.trend-indicator.danger {
  background: #fff2f0;
  color: #ff4d4f;
}

.trend-indicator.warning {
  background: #fffbe6;
  color: #faad14;
}

.trend-indicator.info {
  background: #f0f9ff;
  color: #1890ff;
}

.trend-value {
  font-size: 12px;
}

.trend-period {
  font-size: 11px;
  color: #9ca3af;
}

.metric-sparkline {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 80px;
  height: 30px;
  opacity: 0.8;
}

.sparkline-container {
  width: 100%;
  height: 100%;
}

.metric-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  transform: translate(40px, -40px);
  opacity: 0.05;
  transition: all 0.3s ease;
}

.metric-trend-card:hover .metric-decoration {
  transform: translate(50px, -50px);
  opacity: 0.1;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .metric-trend-card {
    padding: 16px;
    height: 120px;
  }

  .metric-icon {
    width: 40px;
    height: 40px;
  }

  .metric-value {
    font-size: 24px;
  }

  .metric-value.metric-large {
    font-size: 28px;
  }

  .metric-sparkline {
    width: 60px;
    height: 24px;
  }
}
</style>