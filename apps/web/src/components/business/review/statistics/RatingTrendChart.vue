<template>
  <div class="rating-trend-chart">
    <div ref="trendChart" class="chart" v-loading="loading"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

interface TrendData {
  period: string
  overallRating: number
  dimensionRatings?: Record<string, number>
  reviewCount: number
  nps?: number
  recommendationRate?: number
}

interface Props {
  data: TrendData[]
  loading: boolean
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  loading: false
})

const trendChart = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const initChart = () => {
  if (!trendChart.value || props.data.length === 0) return

  chartInstance = echarts.init(trendChart.value)

  const periods = props.data.map(item => item.period)
  const overallRatings = props.data.map(item => item.overallRating)
  const reviewCounts = props.data.map(item => item.reviewCount)
  const cleanlinessRatings = props.data.map(item => item.dimensionRatings?.cleanliness || 0)
  const serviceRatings = props.data.map(item => item.dimensionRatings?.service || 0)

  const option = {
    title: {
      text: '评分趋势分析',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal',
        color: '#303133'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        crossStyle: {
          color: '#999'
        }
      },
      formatter: (params: any) => {
        let tooltip = `<div style="font-weight: bold; margin-bottom: 8px;">${params[0].axisValue}</div>`
        params.forEach((param: any) => {
          if (param.seriesName !== '评价数量') {
            tooltip += `<div style="display: flex; align-items: center; margin-bottom: 4px;">
              <span style="display: inline-block; width: 10px; height: 10px; background: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>
              <span>${param.seriesName}: ${param.value}分</span>
            </div>`
          } else {
            tooltip += `<div style="display: flex; align-items: center; margin-bottom: 4px;">
              <span style="display: inline-block; width: 10px; height: 10px; background: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>
              <span>${param.seriesName}: ${param.value}条</span>
            </div>`
          }
        })
        return tooltip
      }
    },
    legend: {
      data: ['综合评分', '卫生评分', '服务评分', '评价数量'],
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 80,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: periods,
      axisPointer: {
        type: 'shadow'
      },
      axisLine: {
        lineStyle: {
          color: '#DCDFE6'
        }
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#606266',
        rotate: periods.length > 10 ? 45 : 0
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '评分',
        min: 0,
        max: 5,
        interval: 1,
        axisLabel: {
          formatter: '{value}分',
          color: '#606266'
        },
        axisLine: {
          lineStyle: {
            color: '#DCDFE6'
          }
        },
        axisTick: {
          show: false
        },
        splitLine: {
          lineStyle: {
            color: '#EBEEF5',
            type: 'dashed'
          }
        }
      },
      {
        type: 'value',
        name: '数量',
        min: 0,
        axisLabel: {
          formatter: '{value}',
          color: '#606266'
        },
        axisLine: {
          lineStyle: {
            color: '#DCDFE6'
          }
        },
        axisTick: {
          show: false
        }
      }
    ],
    series: [
      {
        name: '综合评分',
        type: 'line',
        data: overallRatings,
        smooth: true,
        lineStyle: {
          width: 3,
          color: '#409EFF'
        },
        itemStyle: {
          color: '#409EFF',
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
          ])
        },
        emphasis: {
          focus: 'series'
        }
      },
      {
        name: '卫生评分',
        type: 'line',
        data: cleanlinessRatings,
        smooth: true,
        lineStyle: {
          width: 2,
          color: '#67C23A'
        },
        itemStyle: {
          color: '#67C23A'
        },
        emphasis: {
          focus: 'series'
        }
      },
      {
        name: '服务评分',
        type: 'line',
        data: serviceRatings,
        smooth: true,
        lineStyle: {
          width: 2,
          color: '#E6A23C'
        },
        itemStyle: {
          color: '#E6A23C'
        },
        emphasis: {
          focus: 'series'
        }
      },
      {
        name: '评价数量',
        type: 'bar',
        yAxisIndex: 1,
        data: reviewCounts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(144, 147, 153, 0.6)' },
            { offset: 1, color: 'rgba(144, 147, 153, 0.3)' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        emphasis: {
          focus: 'series'
        }
      }
    ]
  }

  chartInstance.setOption(option)
}

const updateChart = () => {
  nextTick(() => {
    if (chartInstance) {
      chartInstance.dispose()
    }
    initChart()
  })
}

onMounted(() => {
  nextTick(() => {
    initChart()
  })

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})

watch(() => props.data, () => {
  updateChart()
}, { deep: true })

watch(() => props.loading, (loading) => {
  if (!loading && props.data.length > 0) {
    updateChart()
  }
})
</script>

<style scoped lang="scss">
.rating-trend-chart {
  .chart {
    width: 100%;
    height: 400px;
  }
}
</style>