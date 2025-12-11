<template>
  <div class="review-word-cloud">
    <div class="word-cloud-container" v-loading="loading">
      <div class="word-cloud" ref="wordCloudRef">
        <div
          v-for="(word, index) in displayWords"
          :key="word.word"
          class="word-item"
          :style="getWordStyle(word, index)"
          @click="onWordClick(word)"
          @mouseover="onWordHover(word)"
          @mouseleave="onWordLeave"
        >
          <span class="word-text">{{ word.word }}</span>
          <span class="word-count" v-if="showDetails">({{ word.count }})</span>
        </div>
      </div>
      <div class="word-details" v-if="selectedWord">
        <el-card>
          <template #header>
            <span>{{ selectedWord.word }}</span>
          </template>
          <div class="detail-item">
            <span class="label">出现次数：</span>
            <span class="value">{{ selectedWord.count }}</span>
          </div>
          <div class="detail-item">
            <span class="label">情感倾向：</span>
            <el-tag :type="getSentimentType(selectedWord.sentiment)">
              {{ getSentimentText(selectedWord.sentiment) }}
            </el-tag>
          </div>
          <div class="detail-item" v-if="selectedWord.category">
            <span class="label">相关类别：</span>
            <span class="value">{{ getCategoryText(selectedWord.category) }}</span>
          </div>
          <div class="detail-item" v-if="selectedWord.averageRating">
            <span class="label">相关评分：</span>
            <el-rate
              v-model="selectedWord.averageRating"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}分"
            />
          </div>
        </el-card>
      </div>
    </div>
    <div class="word-categories" v-if="!loading && displayWords.length > 0">
      <div class="category-legend">
        <span class="legend-item" v-for="category in categories" :key="category.key">
          <span class="legend-color" :style="{ backgroundColor: category.color }"></span>
          <span class="legend-label">{{ category.label }}</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'

interface WordData {
  word: string
  count: number
  weight: number
  sentiment?: string
  category?: string
  averageRating?: number
}

interface Props {
  data: WordData[]
  loading: boolean
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  loading: false
})

const wordCloudRef = ref<HTMLElement>()
const selectedWord = ref<WordData | null>(null)
const showDetails = ref(false)

const categories = [
  { key: 'service', label: '服务', color: '#409EFF' },
  { key: 'cleanliness', label: '卫生', color: '#67C23A' },
  { key: 'facilities', label: '设施', color: '#E6A23C' },
  { key: 'location', label: '位置', color: '#F56C6C' },
  { key: 'value', label: '性价比', color: '#909399' },
  { key: 'breakfast', label: '早餐', color: '#B37FEB' },
  { key: 'other', label: '其他', color: '#C0C4CC' }
]

const displayWords = computed(() => {
  return props.data.slice(0, 50) // 限制显示50个词
})

const getWordStyle = (word: WordData, index: number) => {
  const baseSize = 12
  const maxSize = 32
  const size = baseSize + (word.weight * (maxSize - baseSize))

  const colors = {
    positive: '#67C23A',
    negative: '#F56C6C',
    neutral: '#909399'
  }

  const category = categories.find(c => c.key === word.category)
  const color = category ? category.color : colors[word.sentiment as keyof typeof colors] || '#606266'

  return {
    fontSize: `${size}px`,
    color: color,
    fontWeight: word.weight > 0.7 ? 'bold' : 'normal',
    opacity: 0.7 + (word.weight * 0.3),
    transform: `rotate(${(Math.random() - 0.5) * 10}deg)`,
    margin: '8px',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    display: 'inline-block'
  }
}

const getSentimentType = (sentiment?: string) => {
  const types: Record<string, string> = {
    positive: 'success',
    negative: 'danger',
    neutral: 'info'
  }
  return types[sentiment || 'neutral'] || 'info'
}

const getSentimentText = (sentiment?: string) => {
  const texts: Record<string, string> = {
    positive: '积极',
    negative: '消极',
    neutral: '中性'
  }
  return texts[sentiment || 'neutral'] || '中性'
}

const getCategoryText = (category?: string) => {
  const found = categories.find(c => c.key === category)
  return found ? found.label : '其他'
}

const onWordClick = (word: WordData) => {
  selectedWord.value = word
  showDetails.value = true
}

const onWordHover = (word: WordData) => {
  // 可以在这里添加悬停效果
}

const onWordLeave = () => {
  // 可以在这里添加离开效果
}

// 生成词云布局
const generateWordCloudLayout = () => {
  if (!wordCloudRef.value) return

  const container = wordCloudRef.value
  const containerWidth = container.offsetWidth
  const containerHeight = 400
  const centerX = containerWidth / 2
  const centerY = containerHeight / 2

  const words = container.querySelectorAll('.word-item')
  const placedPositions: Array<{x: number, y: number, width: number, height: number}> = []

  words.forEach((wordElement, index) => {
    const word = displayWords.value[index]
    if (!word) return

    let placed = false
    let attempts = 0
    const maxAttempts = 100

    while (!placed && attempts < maxAttempts) {
      const angle = (attempts / maxAttempts) * Math.PI * 2
      const radius = (attempts / maxAttempts) * Math.min(centerX, centerY) * 0.8
      const x = centerX + Math.cos(angle) * radius
      const y = centerY + Math.sin(angle) * radius

      const rect = {
        x: x - 50,
        y: y - 20,
        width: 100,
        height: 40
      }

      const overlaps = placedPositions.some(pos =>
        rect.x < pos.x + pos.width &&
        rect.x + rect.width > pos.x &&
        rect.y < pos.y + pos.height &&
        rect.y + rect.height > pos.y
      )

      if (!overlaps) {
        wordElement.style.position = 'absolute'
        wordElement.style.left = `${x}px`
        wordElement.style.top = `${y}px`
        wordElement.style.transform = 'translate(-50%, -50%)'
        placedPositions.push(rect)
        placed = true
      }

      attempts++
    }
  })
}

onMounted(() => {
  if (!props.loading && displayWords.value.length > 0) {
    setTimeout(generateWordCloudLayout, 100)
  }
})

watch(() => props.data, () => {
  if (!props.loading && displayWords.value.length > 0) {
    setTimeout(generateWordCloudLayout, 100)
  }
}, { deep: true })
</script>

<style scoped lang="scss">
.review-word-cloud {
  .word-cloud-container {
    display: flex;
    gap: 20px;

    .word-cloud {
      flex: 1;
      height: 400px;
      position: relative;
      border: 1px dashed #DCDFE6;
      border-radius: 8px;
      overflow: hidden;
      background: #fafafa;

      .word-item {
        position: absolute;
        white-space: nowrap;
        user-select: none;

        &:hover {
          transform: scale(1.1) !important;
          z-index: 10;
        }

        .word-count {
          font-size: 12px;
          opacity: 0.7;
          margin-left: 4px;
        }
      }
    }

    .word-details {
      width: 300px;
      flex-shrink: 0;

      .detail-item {
        display: flex;
        align-items: center;
        margin-bottom: 12px;

        .label {
          font-size: 14px;
          color: #606266;
          margin-right: 8px;
          min-width: 80px;
        }

        .value {
          font-size: 14px;
          color: #303133;
          font-weight: 500;
        }
      }
    }
  }

  .word-categories {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #EBEEF5;

    .category-legend {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;

      .legend-item {
        display: flex;
        align-items: center;
        font-size: 12px;
        color: #606266;

        .legend-color {
          width: 12px;
          height: 12px;
          border-radius: 2px;
          margin-right: 6px;
        }

        .legend-label {
          color: #303133;
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .review-word-cloud {
    .word-cloud-container {
      flex-direction: column;

      .word-details {
        width: 100%;
      }
    }
  }
}
</style>