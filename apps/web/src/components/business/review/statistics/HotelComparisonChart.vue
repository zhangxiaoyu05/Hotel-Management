<template>
  <div class="hotel-comparison-chart">
    <div class="chart-container" v-loading="loading">
      <div ref="comparisonChart" class="chart"></div>
    </div>
    <div class="comparison-table" v-if="!loading && comparisonData.length > 0">
      <el-table :data="comparisonData" style="width: 100%">
        <el-table-column prop="ranking" label="排名" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.isCurrentHotel ? 'primary' : 'info'"
              :effect="row.isCurrentHotel ? 'dark' : 'plain'"
            >
              {{ row.ranking }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hotelName" label="酒店名称" min-width="150">
          <template #default="{ row }">
            <span v-if="row.isCurrentHotel" class="current-hotel">
              {{ row.hotelName }}
              <el-tag type="primary" size="small" class="current-tag">当前</el-tag>
            </span>
            <span v-else>{{ row.hotelName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="overallRating" label="综合评分" width="100" align="center">
          <template #default="{ row }">
            <el-rate
              v-model="row.overallRating"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
          </template>
        </el-table-column>
        <el-table-column prop="dimensionRatings.cleanliness" label="卫生" width="80" align="center">
          <template #default="{ row }">
            {{ row.dimensionRatings?.cleanliness?.toFixed(1) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="dimensionRatings.service" label="服务" width="80" align="center">
          <template #default="{ row }">
            {{ row.dimensionRatings?.service?.toFixed(1) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="dimensionRatings.facilities" label="设施" width="80" align="center">
          <template #default="{ row }">
            {{ row.dimensionRatings?.facilities?.toFixed(1) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="dimensionRatings.location" label="位置" width="80" align="center">
          <template #default="{ row }">
            {{ row.dimensionRatings?.location?.toFixed(1) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="reviewCount" label="评价数量" width="100" align="center">
          <template #default="{ row }">
            {{ row.reviewCount }}
          </template>
        </el-table-column>
        <el-table-column prop="deviationFromAverage.overall" label="vs平均" width="100" align="center">
          <template #default="{ row }">
            <span
              :class="getDeviationClass(row.deviationFromAverage?.overall)"
            >
              {{ formatDeviation(row.deviationFromAverage?.overall) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

interface ComparisonData {
  hotelId: number
  hotelName: string
  overallRating: number
  dimensionRatings?: Record<string, number>
  reviewCount: number
  ranking: number
  isCurrentHotel: boolean
  deviationFromAverage?: Record<string, number>
}

interface Props {
  data: ComparisonData[]
  loading: boolean
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  loading: false
})

const comparisonChart = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const getDeviationClass = (deviation?: number) => {
  if (!deviation) return ''
  return deviation > 0 ? 'positive-deviation' : deviation < 0 ? 'negative-deviation' : 'neutral-deviation'
}

const formatDeviation = (deviation?: number) => {
  if (!deviation) return '-'
  const sign = deviation > 0 ? '+' : ''
  return `${sign}${deviation.toFixed(1)}%`
}

const initChart = () => {
  if (!comparisonChart.value || props.data.length === 0) return

  chartInstance = echarts.init(comparisonChart.value)

  const hotels = props.data.map(item => item.hotelName)
  const overallRatings = props.data.map(item => item.overallRating)
  const cleanlinessRatings = props.data.map(item => item.dimensionRatings?.cleanliness || 0)
  const serviceRatings = props.data.map(item => item.dimensionRatings?.service || 0)
  const facilitiesRatings = props.data.map(item => item.dimensionRatings?.facilities || 0)
  const locationRatings = props.data.map(item => item.dimensionRatings?.location || 0)

  // 高亮当前酒店的数据
  const emphasisStyle = props.data.map(item => item.isCurrentHotel)

  const option = {
    title: {
      text: '酒店评分对比分析',
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
        type: 'shadow'
      },
      formatter: (params: any) => {
        let tooltip = `<div style="font-weight: bold; margin-bottom: 8px;">${params[0].axisValue}</div>`
        params.forEach((param: any) => {
          tooltip += `<div style="display: flex; align-items: center; margin-bottom: 4px;">
            <span style="display: inline-block; width: 10px; height: 10px; background: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>
            <span>${param.seriesName}: ${param.value}分</span>
          </div>`
        })
        return tooltip
      }
    },
    legend: {
      data: ['综合评分', '卫生', '服务', '设施', '位置'],
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
      data: hotels,
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
        interval: 0,
        rotate: hotels.length > 5 ? 45 : 0,
        formatter: (value: string, index: number) => {
          if (emphasisStyle[index]) {
            return `{a|${value}}`
          }
          return value
        },
        rich: {
          a: {
            color: '#409EFF',
            fontWeight: 'bold'
          }
        }
      }
    },
    yAxis: {
      type: 'value',
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
    series: [
      {
        name: '综合评分',
        type: 'bar',
        data: overallRatings.map((rating, index) => ({
          value: rating,
          itemStyle: {
            color: emphasisStyle[index] ? '#409EFF' : '#5DADE2',
            borderWidth: emphasisStyle[index] ? 2 : 0,
            borderColor: emphasisStyle[index] ? '#2E86C1' : 'transparent'
          }
        })),
        barWidth: '15%',
        emphasis: {
          focus: 'series'
        }
      },
      {
        name: '卫生',
        type: 'bar',
        data: cleanlinessRatings,
        itemStyle: {
          color: '#67C23A'
        },
        barWidth: '15%',
        emphasis: {
          focus: 'series'
        }
      },
      {
        name: '服务',
        type: 'bar',
        data: serviceRatings,
        itemStyle: {
          color: '#E6A23C'
        },
        barWidth: '15%',
        emphasis: {
          focus: 'series'
        }
      },
      {
        name: '设施',
        type: 'bar',
        data: facilitiesRatings,
        itemStyle: {
          color: '#F56C6C'
        },
        barWidth: '15%',
        emphasis: {
          focus: 'series'
        }
      },
      {
        name: '位置',
        type: 'bar',
        data: locationRatings,
        itemStyle: {
          color: '#909399'
        },
        barWidth: '15%',
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
.hotel-comparison-chart {
  .chart-container {
    margin-bottom: 24px;

    .chart {
      width: 100%;
      height: 400px;
    }
  }

  .comparison-table {
    .current-hotel {
      display: flex;
      align-items: center;
      font-weight: 600;
      color: #409EFF;

      .current-tag {
        margin-left: 8px;
      }
    }

    .positive-deviation {
      color: #67C23A;
      font-weight: 600;
    }

    .negative-deviation {
      color: #F56C6C;
      font-weight: 600;
    }

    .neutral-deviation {
      color: #909399;
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .hotel-comparison-chart {
    .chart-container {
      .chart {
        height: 300px;
      }
    }
  }
}
</style>