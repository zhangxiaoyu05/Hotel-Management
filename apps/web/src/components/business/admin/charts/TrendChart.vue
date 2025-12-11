<template>
  <div class="trend-chart">
    <div class="chart-header">
      <h3 class="chart-title">{{ title }}</h3>
      <div class="chart-controls">
        <!-- 日期范围选择器 -->
        <el-date-picker
          v-if="showDateRange"
          v-model="localDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateRangeChange"
          size="small"
        />

        <!-- 图表类型选择器 -->
        <el-select
          v-if="chartTypeOptions && chartTypeOptions.length > 0"
          v-model="localChartType"
          @change="handleChartTypeChange"
          size="small"
        >
          <el-option
            v-for="option in chartTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>

        <slot name="controls"></slot>
      </div>
    </div>
    <div class="chart-container" :ref="chartRef" v-loading="loading"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import type { ChartSeries } from '@/types/dashboard'

interface ChartTypeOption {
  label: string
  value: string
}

interface Props {
  title: string
  chartData?: {
    categories: string[]
    series: ChartSeries[]
    subtitle?: string
  }
  height?: number
  loading?: boolean
  showDateRange?: boolean
  dateRange?: [string, string]
  chartType?: string
  chartTypeOptions?: ChartTypeOption[]
}

const props = withDefaults(defineProps<Props>(), {
  height: 300,
  loading: false,
  showDateRange: false,
  chartTypeOptions: () => []
})

const emit = defineEmits<{
  'date-range-change': [dateRange: [string, string]]
  'chart-type-change': [chartType: string]
}>()

// 本地状态
const localDateRange = ref<[string, string] | null>(props.dateRange || null)
const localChartType = ref<string>(props.chartType || 'trends')

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// 方法
const handleDateRangeChange = (dateRange: [string, string]) => {
  localDateRange.value = dateRange
  emit('date-range-change', dateRange)
}

const handleChartTypeChange = (chartType: string) => {
  localChartType.value = chartType
  emit('chart-type-change', chartType)
}

const chartOptions = computed(() => {
  if (!props.chartData) return null

  const { categories, series, subtitle } = props.chartData

  return {
    title: subtitle ? {
      text: subtitle,
      left: 'center',
      textStyle: {
        fontSize: 14,
        color: '#666'
      }
    } : undefined,
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      },
      formatter: (params: any) => {
        let result = params[0].axisValue + '<br/>'
        params.forEach((item: any) => {
          result += `${item.marker}${item.seriesName}: ${item.value}<br/>`
        })
        return result
      }
    },
    legend: {
      data: series.map(s => s.name),
      bottom: 0,
      type: 'scroll'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: subtitle ? '15%' : '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      },
      axisLabel: {
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#666'
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: series.map(s => ({
      name: s.name,
      type: s.type,
      data: s.data,
      smooth: s.smooth !== false,
      symbol: s.type === 'line' ? 'circle' : 'rect',
      symbolSize: 6,
      lineStyle: {
        width: 2,
        color: s.color
      },
      itemStyle: {
        color: s.color
      },
      areaStyle: s.type === 'line' ? {
        opacity: 0.1,
        color: s.color
      } : undefined,
      barWidth: s.type === 'column' ? '60%' : undefined,
      yAxisIndex: s.yAxis || 0
    }))
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

  // 监听dateRange属性变化
  watch(() => props.dateRange, (newDateRange) => {
    if (newDateRange) {
      localDateRange.value = newDateRange
    }
  })

  // 监听chartType属性变化
  watch(() => props.chartType, (newChartType) => {
    if (newChartType) {
      localChartType.value = newChartType
    }
  })

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
.trend-chart {
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
</style>