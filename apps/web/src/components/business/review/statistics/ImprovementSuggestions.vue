<template>
  <div class="improvement-suggestions">
    <div v-if="loading" v-loading="true" class="loading-container">
      <div class="loading-text">加载改进建议中...</div>
    </div>
    <div v-else-if="suggestions.length === 0" class="empty-state">
      <el-empty description="暂无改进建议" />
    </div>
    <div v-else class="suggestions-list">
      <div
        v-for="suggestion in suggestions"
        :key="suggestion.id"
        class="suggestion-card"
        :class="getPriorityClass(suggestion.priority)"
      >
        <div class="suggestion-header">
          <div class="suggestion-title">
            <el-icon class="category-icon" :color="getCategoryColor(suggestion.category)">
              <component :is="getCategoryIcon(suggestion.category)" />
            </el-icon>
            <span>{{ suggestion.title }}</span>
          </div>
          <div class="suggestion-meta">
            <el-tag :type="getPriorityType(suggestion.priority)" size="small">
              {{ getPriorityText(suggestion.priority) }}
            </el-tag>
          </div>
        </div>

        <div class="suggestion-description">
          {{ suggestion.description }}
        </div>

        <div class="suggestion-details">
          <div class="detail-row">
            <span class="detail-label">相关评价：</span>
            <span class="detail-value">{{ suggestion.relatedReviewCount }}条</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">预期提升：</span>
            <span class="detail-value improvement">+{{ suggestion.expectedRatingImprovement?.toFixed(1) }}分</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">实施难度：</span>
            <el-tag :type="getDifficultyType(suggestion.difficulty)" size="small">
              {{ suggestion.difficulty }}
            </el-tag>
          </div>
          <div class="detail-row" v-if="suggestion.estimatedCost">
            <span class="detail-label">预估成本：</span>
            <span class="detail-value">{{ suggestion.estimatedCost }}</span>
          </div>
          <div class="detail-row" v-if="suggestion.implementationTime">
            <span class="detail-label">实施时间：</span>
            <span class="detail-value">{{ suggestion.implementationTime }}</span>
          </div>
        </div>

        <div class="suggestion-keywords" v-if="suggestion.keywords && suggestion.keywords.length > 0">
          <div class="keywords-label">关键词：</div>
          <div class="keywords-list">
            <el-tag
              v-for="keyword in suggestion.keywords"
              :key="keyword"
              size="small"
              type="info"
              class="keyword-tag"
            >
              {{ keyword }}
            </el-tag>
          </div>
        </div>

        <div class="suggestion-actions">
          <el-button type="primary" size="small" @click="viewDetails(suggestion)">
            查看详情
          </el-button>
          <el-button size="small" @click="createTask(suggestion)">
            创建任务
          </el-button>
        </div>
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="改进建议详情"
      width="600px"
    >
      <div v-if="selectedSuggestion" class="suggestion-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">类别：</span>
              <span>{{ getCategoryText(selectedSuggestion.category) }}</span>
            </div>
            <div class="info-item">
              <span class="label">优先级：</span>
              <el-tag :type="getPriorityType(selectedSuggestion.priority)">
                {{ getPriorityText(selectedSuggestion.priority) }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">相关评价：</span>
              <span>{{ selectedSuggestion.relatedReviewCount }}条</span>
            </div>
            <div class="info-item">
              <span class="label">预期提升：</span>
              <span class="improvement">+{{ selectedSuggestion.expectedRatingImprovement?.toFixed(1) }}分</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4>建议描述</h4>
          <p>{{ selectedSuggestion.description }}</p>
        </div>

        <div class="detail-section" v-if="selectedSuggestion.analysisResult">
          <h4>数据分析</h4>
          <p>{{ selectedSuggestion.analysisResult }}</p>
        </div>

        <div class="detail-section">
          <h4>实施信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">实施难度：</span>
              <el-tag :type="getDifficultyType(selectedSuggestion.difficulty)">
                {{ selectedSuggestion.difficulty }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">预估成本：</span>
              <span>{{ selectedSuggestion.estimatedCost }}</span>
            </div>
            <div class="info-item">
              <span class="label">实施时间：</span>
              <span>{{ selectedSuggestion.implementationTime }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="selectedSuggestion.keywords">
          <h4>相关关键词</h4>
          <div class="keywords-detail">
            <el-tag
              v-for="keyword in selectedSuggestion.keywords"
              :key="keyword"
              class="keyword-tag"
            >
              {{ keyword }}
            </el-tag>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="createTask(selectedSuggestion)">
          创建改进任务
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Tools,
  Brush,
  House,
  Location,
  Money,
  Coffee,
  Setting
} from '@element-plus/icons-vue'

interface SuggestionData {
  id: number
  category: string
  title: string
  description: string
  priority: string
  keywords: string[]
  relatedReviewCount: number
  expectedRatingImprovement: number
  difficulty: string
  estimatedCost: string
  implementationTime: string
  analysisResult: string
}

interface Props {
  data: SuggestionData[]
  loading: boolean
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  loading: false
})

const suggestions = computed(() => props.data)
const detailDialogVisible = ref(false)
const selectedSuggestion = ref<SuggestionData | null>(null)

const getPriorityClass = (priority: string) => {
  return `priority-${priority.toLowerCase()}`
}

const getPriorityType = (priority: string) => {
  const types: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'info'
  }
  return types[priority.toLowerCase()] || 'info'
}

const getPriorityText = (priority: string) => {
  const texts: Record<string, string> = {
    high: '高优先级',
    medium: '中优先级',
    low: '低优先级'
  }
  return texts[priority.toLowerCase()] || '未知'
}

const getDifficultyType = (difficulty: string) => {
  const types: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'success'
  }
  return types[difficulty.toLowerCase()] || 'info'
}

const getCategoryIcon = (category: string) => {
  const icons: Record<string, any> = {
    service: Tools,
    cleanliness: Brush,
    facilities: House,
    location: Location,
    value: Money,
    breakfast: Coffee,
    general: Setting
  }
  return icons[category] || Setting
}

const getCategoryColor = (category: string) => {
  const colors: Record<string, string> = {
    service: '#409EFF',
    cleanliness: '#67C23A',
    facilities: '#E6A23C',
    location: '#F56C6C',
    value: '#909399',
    breakfast: '#B37FEB',
    general: '#C0C4CC'
  }
  return colors[category] || '#606266'
}

const getCategoryText = (category: string) => {
  const texts: Record<string, string> = {
    service: '服务',
    cleanliness: '卫生',
    facilities: '设施',
    location: '位置',
    value: '性价比',
    breakfast: '早餐',
    general: '其他'
  }
  return texts[category] || '其他'
}

const viewDetails = (suggestion: SuggestionData) => {
  selectedSuggestion.value = suggestion
  detailDialogVisible.value = true
}

const createTask = (suggestion: SuggestionData | null) => {
  if (!suggestion) return

  ElMessage.success(`已为建议"${suggestion.title}"创建改进任务`)
  detailDialogVisible.value = false

  // 这里可以调用创建任务的API
  console.log('创建任务:', suggestion)
}
</script>

<style scoped lang="scss">
.improvement-suggestions {
  .loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 200px;

    .loading-text {
      margin-top: 16px;
      color: #606266;
    }
  }

  .empty-state {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
  }

  .suggestions-list {
    max-height: 600px;
    overflow-y: auto;

    .suggestion-card {
      background: white;
      border: 1px solid #EBEEF5;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 16px;
      transition: all 0.3s ease;

      &:hover {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        border-color: #C0C4CC;
      }

      &.priority-high {
        border-left: 4px solid #F56C6C;
      }

      &.priority-medium {
        border-left: 4px solid #E6A23C;
      }

      &.priority-low {
        border-left: 4px solid #909399;
      }

      .suggestion-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12px;

        .suggestion-title {
          display: flex;
          align-items: center;
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          flex: 1;

          .category-icon {
            margin-right: 8px;
          }
        }

        .suggestion-meta {
          flex-shrink: 0;
        }
      }

      .suggestion-description {
        color: #606266;
        line-height: 1.6;
        margin-bottom: 16px;
      }

      .suggestion-details {
        margin-bottom: 16px;

        .detail-row {
          display: flex;
          align-items: center;
          margin-bottom: 8px;
          font-size: 14px;

          .detail-label {
            color: #909399;
            min-width: 80px;
            margin-right: 8px;
          }

          .detail-value {
            color: #303133;
            font-weight: 500;

            &.improvement {
              color: #67C23A;
              font-weight: 600;
            }
          }
        }
      }

      .suggestion-keywords {
        display: flex;
        align-items: flex-start;
        margin-bottom: 16px;

        .keywords-label {
          color: #909399;
          font-size: 14px;
          min-width: 80px;
          margin-right: 8px;
          margin-top: 2px;
        }

        .keywords-list {
          flex: 1;

          .keyword-tag {
            margin-right: 8px;
            margin-bottom: 4px;
          }
        }
      }

      .suggestion-actions {
        display: flex;
        justify-content: flex-end;
        gap: 12px;
      }
    }
  }

  .suggestion-detail {
    .detail-section {
      margin-bottom: 24px;

      h4 {
        margin: 0 0 12px 0;
        color: #303133;
        font-size: 16px;
        font-weight: 600;
      }

      p {
        margin: 0;
        color: #606266;
        line-height: 1.6;
      }

      .info-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;

        .info-item {
          display: flex;
          align-items: center;

          .label {
            color: #909399;
            min-width: 80px;
            margin-right: 8px;
          }

          .improvement {
            color: #67C23A;
            font-weight: 600;
          }
        }
      }

      .keywords-detail {
        .keyword-tag {
          margin-right: 8px;
          margin-bottom: 8px;
        }
      }
    }
  }
}

// 滚动条样式
.suggestions-list::-webkit-scrollbar {
  width: 6px;
}

.suggestions-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.suggestions-list::-webkit-scrollbar-thumb {
  background: #C0C4CC;
  border-radius: 3px;
}

.suggestions-list::-webkit-scrollbar-thumb:hover {
  background: #909399;
}
</style>