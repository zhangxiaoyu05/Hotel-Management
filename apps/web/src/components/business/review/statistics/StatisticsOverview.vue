<template>
  <div class="statistics-overview">
    <div class="chart-container" v-if="viewType === 'radar'">
      <div ref="radarChart" class="chart"></div>
    </div>
    <div class="chart-container" v-else-if="viewType === 'bar'">
      <div ref="barChart" class="chart"></div>
    </div>
    <div class="dimension-details">
      <el-row :gutter="16">
        <el-col :span="6" v-for="(rating, dimension) in dimensionRatings" :key="dimension">
          <div class="dimension-item">
            <div class="dimension-label">{{ getDimensionLabel(dimension) }}</div>
            <el-progress
              :percentage="(rating / 5) * 100"
              :color="getProgressColor(rating)"
              :stroke-width="8"
            />
            <div class="dimension-value">{{ rating.toFixed(1) }}</div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

interface Props {
  data: {
    dimensionRatings?: Record<string, number>
    ratingDistribution?: Record<string, number>
  }
  viewType: 'radar' | 'bar'
}

const props = withDefaults(defineProps<Props>(), {
  data: () => ({
    dimensionRatings: {},
    ratingDistribution: {}
  }),
  viewType: 'radar'
})

const radarChart = ref<HTMLElement>()
const barChart = ref<HTMLElement>()
let radarChartInstance: echarts.ECharts | null = null
let barChartInstance: echarts.ECharts | null = null

const dimensionRatings = computed(() => {
  return props.data.dimensionRatings || {
    cleanliness: 0,
    service: 0,
    facilities: 0,
    location: 0
  }
})

const getDimensionLabel = (dimension: string) => {
  const labels: Record<string, string> = {
    cleanliness: '卫生',
    service: '服务',
    facilities: '设施',
    location: '位置'
  }
  return labels[dimension] || dimension
}

const getProgressColor = (rating: number) => {
  if (rating >= 4.5) return '#67C23A'
  if (rating >= 4.0) return '#E6A23C'
  if (rating >= 3.5) return '#F56C6C'
  return '#909399'
}

const initRadarChart = () => {
  if (!radarChart.value) return

  radarChartInstance = echarts.init(radarChart.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}'
    },
    radar: {
      indicator: [
        { name: '卫生', max: 5 },
        { name: '服务', max: 5 },
        { name: '设施', max: 5 },
        { name: '位置', max: 5 }
      ],
      center: ['50%', '50%'],
      radius: '70%'
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          dimensionRatings.value.cleanliness || 0,
          dimensionRatings.value.service || 0,
          dimensionRatings.value.facilities || 0,
          dimensionRatings.value.location || 0
        ],
        name: '评分',
        areaStyle: {
          color: 'rgba(64, 158, 255, 0.2)'
        },
        lineStyle: {
          color: '#409EFF',
          width: 2
        },
        itemStyle: {
          color: '#409EFF'
        }
      }]
    }]
  }

  radarChartInstance.setOption(option)
}

const initBarChart = () => {
  if (!barChart.value) return

  barChartInstance = echarts.init(barChart.value)

  const categories = ['卫生', '服务', '设施', '位置']
  const values = [
    dimensionRatings.value.cleanliness || 0,
    dimensionRatings.value.service || 0,
    dimensionRatings.value.facilities || 0,
    dimensionRatings.value.location || 0
  ]

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: '{b}: {c}分'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: {
        lineStyle: {
          color: '#DCDFE6'
        }
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#606266'
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 5,
      interval: 1,
      axisLine: {
        lineStyle: {
          color: '#DCDFE6'
        }
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#606266'
      },
      splitLine: {
        lineStyle: {
          color: '#EBEEF5',
          type: 'dashed'
        }
      }
    },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#2378f7' },
            { offset: 0.7, color: '#2378f7' },
            { offset: 1, color: '#83bff6' }
          ])
        }
      }
    }]
  }

  barChartInstance.setOption(option)
}

const updateCharts = () => {
  nextTick(() => {
    if (props.viewType === 'radar' && radarChartInstance) {
      initRadarChart()
    } else if (props.viewType === 'bar' && barChartInstance) {
      initBarChart()
    }
  })
}

onMounted(() => {
  nextTick(() => {
    if (props.viewType === 'radar') {
      initRadarChart()
    } else {
      initBarChart()
    }
  })

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    radarChartInstance?.resize()
    barChartInstance?.resize()
  })
})

watch(() => props.viewType, (newViewType) => {
  nextTick(() => {
    if (newViewType === 'radar') {
      initRadarChart()
    } else {
      initBarChart()
    }
  })
})

watch(() => props.data, () => {
  updateCharts()
}, { deep: true })
</script>

<style scoped lang="scss">
.statistics-overview {
  .chart-container {
    height: 400px;
    margin-bottom: 24px;

    .chart {
      width: 100%;
      height: 100%;
    }
  }

  .dimension-details {
    .dimension-item {
      text-align: center;
      padding: 16px;
      background: #f8f9fa;
      border-radius: 8px;

      .dimension-label {
        font-size: 14px;
        color: #606266;
        margin-bottom: 12px;
        font-weight: 500;
      }

      .dimension-value {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
        margin-top: 8px;
      }
    }
  }
}
</style>