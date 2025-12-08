<template>
  <div class="review-trend-chart">
    <el-card>
      <template #header>
        <div class="flex justify-between items-center">
          <h3 class="text-lg font-medium">{{ title }}</h3>
          <el-select v-model="chartPeriod" size="small" style="width: 120px">
            <el-option label="7天" value="7d" />
            <el-option label="30天" value="30d" />
            <el-option label="90天" value="90d" />
          </el-select>
        </div>
      </template>

      <div ref="chartContainer" :style="{ height: height || '300px' }">
        <div v-if="loading" class="flex items-center justify-center h-full">
          <el-icon class="animate-spin text-4xl text-gray-400">
            <Loading />
          </el-icon>
        </div>
        <div v-else-if="!hasData" class="flex items-center justify-center h-full text-gray-500">
          <div class="text-center">
            <el-icon class="text-4xl mb-2">
              <TrendCharts />
            </el-icon>
            <p>暂无数据</p>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Loading, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

interface Props {
  title: string
  data: Array<{ date: string; rating: number }>
  loading?: boolean
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  height: '300px'
})

const chartContainer = ref<HTMLElement>()
const chartPeriod = ref('30d')
let chartInstance: echarts.ECharts | null = null

const hasData = computed(() => {
  return props.data && props.data.length > 0
})

const initChart = () => {
  if (!chartContainer.value) return

  chartInstance = echarts.init(chartContainer.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance || !hasData.value) return

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const param = params[0]
        return `${param.name}<br/>平均评分: ${param.value.toFixed(1)}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: props.data.map(item => item.date),
      axisLine: {
        lineStyle: {
          color: '#e5e7eb'
        }
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 5,
      interval: 1,
      axisLine: {
        lineStyle: {
          color: '#e5e7eb'
        }
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12,
        formatter: '{value}.0'
      },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6'
        }
      }
    },
    series: [
      {
        name: '平均评分',
        type: 'line',
        data: props.data.map(item => item.rating),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          width: 3,
          color: '#3b82f6'
        },
        itemStyle: {
          color: '#3b82f6',
          borderColor: '#fff',
          borderWidth: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgba(59, 130, 246, 0.3)'
              },
              {
                offset: 1,
                color: 'rgba(59, 130, 246, 0.05)'
              }
            ]
          }
        }
      }
    ]
  }

  chartInstance.setOption(option)
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

watch(() => props.data, () => {
  nextTick(() => {
    updateChart()
  })
}, { deep: true })

watch(() => props.loading, (loading) => {
  if (!loading && hasData.value) {
    nextTick(() => {
      updateChart()
    })
  }
})

onMounted(() => {
  nextTick(() => {
    initChart()
    window.addEventListener('resize', resizeChart)
  })
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', resizeChart)
})
</script>

<style scoped>
.review-trend-chart {
  width: 100%;
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>