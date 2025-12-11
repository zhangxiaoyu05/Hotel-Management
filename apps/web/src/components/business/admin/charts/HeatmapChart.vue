<template>
  <div class="heatmap-chart">
    <div class="chart-header">
      <h3 class="chart-title">{{ title }}</h3>
      <div class="chart-controls">
        <slot name="controls"></slot>
      </div>
    </div>
    <div class="chart-container" :ref="chartRef" v-loading="loading"></div>
    <div class="chart-legend" v-if="showLegend">
      <div class="legend-title">入住率</div>
      <div class="legend-scale">
        <div
          v-for="(item, index) in legendColors"
          :key="index"
          class="legend-item"
        >
          <div class="legend-color" :style="{ backgroundColor: item.color }"></div>
          <span class="legend-label">{{ item.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import type { HeatmapDataItem } from '@/types/dashboard'

interface Props {
  title: string
  chartData?: {
    data: HeatmapDataItem[]
    xAxis: string[]
    yAxis: string[]
  }
  height?: number
  loading?: boolean
  showLegend?: boolean
  visualMap?: {
    min: number
    max: number
    calculable?: boolean
    orient?: 'horizontal' | 'vertical'
    left?: string
    bottom?: string
  }
}

const props = withDefaults(defineProps<Props>(), {
  height: 300,
  loading: false,
  showLegend: true,
  visualMap: () => ({
    min: 0,
    max: 100,
    calculable: true,
    orient: 'horizontal',
    left: 'center',
    bottom: '15%'
  })
})

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const legendColors = computed(() => [
  { color: '#e6f3ff', label: '0%' },
  { color: '#99ccff', label: '25%' },
  { color: '#3399ff', label: '50%' },
  { color: '#0066cc', label: '75%' },
  { color: '#003d7a', label: '100%' }
])

const chartOptions = computed(() => {
  if (!props.chartData?.data || props.chartData.data.length === 0) return null

  const { data, xAxis, yAxis } = props.chartData

  return {
    title: {
      text: '',
      left: 'center'
    },
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        const [xIndex, yIndex, value] = params.data
        const xLabel = xAxis[xIndex]
        const yLabel = yAxis[yIndex]
        return `${xLabel} ${yLabel}<br/>入住率: ${value}%`
      }
    },
    grid: {
      height: '50%',
      top: '10%',
      left: '10%',
      right: '10%',
      bottom: '20%'
    },
    xAxis: {
      type: 'category',
      data: xAxis,
      splitArea: {
        show: true
      },
      axisLabel: {
        rotate: 45,
        fontSize: 12
      }
    },
    yAxis: {
      type: 'category',
      data: yAxis,
      splitArea: {
        show: true
      },
      axisLabel: {
        fontSize: 12
      }
    },
    visualMap: {
      ...props.visualMap,
      inRange: {
        color: ['#e6f3ff', '#99ccff', '#3399ff', '#0066cc', '#003d7a']
      },
      formatter: (value: number) => `${value}%`
    },
    series: [{
      name: props.title,
      type: 'heatmap',
      data: data.map(item => [
        xAxis.indexOf(item.x),
        yAxis.indexOf(item.y),
        item.value
      ]),
      label: {
        show: true,
        formatter: (params: any) => {
          const value = params.data[2]
          return value > 0 ? `${value}%` : ''
        },
        fontSize: 10,
        color: '#fff'
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
})

const renderChart = () => {
  if (!chartRef.value || !chartOptions.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(chartOptions.value)
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

// 生命周期
onMounted(() => {
  nextTick(() => {
    renderChart()
  })

  // 监听数据变化
  watch(() => props.chartData, () => {
    nextTick(() => {
      renderChart()
    })
  }, { deep: true })

  // 监听加载状态
  watch(() => props.loading, (loading) => {
    if (chartInstance) {
      if (loading) {
        chartInstance.showLoading({
          text: '加载中...',
          color: '#1890ff',
          textColor: '#666',
          maskColor: 'rgba(255, 255, 255, 0.8)'
        })
      } else {
        chartInstance.hideLoading()
      }
    }
  }, { immediate: true })

  // 监听窗口大小变化
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', resizeChart)
})

// 导出方法供父组件调用
defineExpose({
  resizeChart,
  renderChart
})
</script>

<style scoped>
.heatmap-chart {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.chart-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: v-bind('props.height + "px"');
  min-height: 300px;
}

.chart-legend {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.legend-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.legend-scale {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 2px;
  border: 1px solid #e0e0e0;
}

.legend-label {
  font-size: 12px;
  color: #666;
}

@media (max-width: 768px) {
  .chart-legend {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .legend-scale {
    width: 100%;
    justify-content: space-between;
  }
}
</style>