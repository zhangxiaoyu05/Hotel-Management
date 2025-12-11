<template>
  <div class="pie-chart">
    <div class="chart-header">
      <h3 class="chart-title">{{ title }}</h3>
      <div class="chart-controls">
        <slot name="controls"></slot>
      </div>
    </div>
    <div class="chart-container" :ref="chartRef"></div>
    <div class="chart-legend" v-if="showLegend">
      <div
        v-for="(item, index) in legendData"
        :key="index"
        class="legend-item"
      >
        <div class="legend-color" :style="{ backgroundColor: item.color }"></div>
        <div class="legend-info">
          <div class="legend-name">{{ item.name }}</div>
          <div class="legend-value">{{ formatValue(item.value) }}</div>
        </div>
        <div class="legend-percent">{{ getPercent(item.value) }}%</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import type { PieDataItem } from '@/types/dashboard'

interface Props {
  title: string
  chartData?: {
    data: PieDataItem[]
  }
  height?: number
  loading?: boolean
  showLegend?: boolean
  donut?: boolean
  innerRadius?: string
  outerRadius?: string
  format?: (value: number) => string
}

const props = withDefaults(defineProps<Props>(), {
  height: 300,
  loading: false,
  showLegend: true,
  donut: false,
  innerRadius: '0%',
  outerRadius: '70%',
  format: (value: number) => value.toString()
})

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const totalValue = computed(() => {
  return props.chartData?.data?.reduce((sum, item) => sum + item.value, 0) || 0
})

const legendData = computed(() => {
  return props.chartData?.data || []
})

const chartOptions = computed(() => {
  if (!props.chartData?.data || props.chartData.data.length === 0) return null

  return {
    title: {
      text: '',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      show: false
    },
    series: [{
      name: props.title,
      type: 'pie',
      radius: props.donut ? [props.innerRadius, props.outerRadius] : props.outerRadius,
      center: ['50%', '50%'],
      data: props.chartData.data.map(item => ({
        value: item.value,
        name: item.name,
        itemStyle: {
          color: item.color,
          borderWidth: 0
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          show: false
        },
        labelLine: {
          show: false
        }
      })),
      emphasis: {
        scale: true,
        scaleSize: 5
      },
      animationType: 'scale',
      animationEasing: 'elasticOut',
      animationDelay: (idx: number) => Math.random() * 200
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

const formatValue = (value: number) => {
  return props.format(value)
}

const getPercent = (value: number) => {
  if (totalValue.value === 0) return 0
  return ((value / totalValue.value) * 100).toFixed(1)
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
.pie-chart {
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
}

.legend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  margin-right: 12px;
  flex-shrink: 0;
}

.legend-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.legend-name {
  font-size: 14px;
  color: #1f2937;
}

.legend-value {
  font-size: 12px;
  color: #6b7280;
}

.legend-percent {
  font-size: 14px;
  font-weight: 600;
  color: #1890ff;
}

@media (max-width: 576px) {
  .legend-item {
    flex-wrap: wrap;
    gap: 8px;
  }

  .legend-percent {
    width: 100%;
    text-align: right;
  }
}
</style>