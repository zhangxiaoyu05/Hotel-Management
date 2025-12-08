<template>
  <div class="review-count-chart">
    <el-card>
      <template #header>
        <div class="flex justify-between items-center">
          <h3 class="text-lg font-medium">{{ title }}</h3>
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
              <DataAnalysis />
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
import { Loading, DataAnalysis } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

interface Props {
  title: string
  data: Array<{ date: string; count: number }>
  loading?: boolean
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  height: '300px'
})

const chartContainer = ref<HTMLElement>()
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
        return `${param.name}<br/>评价数量: ${param.value}`
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
      axisLine: {
        lineStyle: {
          color: '#e5e7eb'
        }
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6'
        }
      }
    },
    series: [
      {
        name: '评价数量',
        type: 'bar',
        data: props.data.map(item => item.count),
        barWidth: '60%',
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: '#10b981'
              },
              {
                offset: 1,
                color: '#059669'
              }
            ]
          },
          borderRadius: [4, 4, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: '#059669'
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
.review-count-chart {
  width: 100%;
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>