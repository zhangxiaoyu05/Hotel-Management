<template>
  <div class="stat-card" :class="{ 'stat-loading': loading, 'stat-clickable': clickable }" @click="handleClick">
    <div class="stat-content">
      <!-- 主要内容 -->
      <div class="stat-main">
        <div class="stat-header">
          <div class="stat-icon" :style="{ backgroundColor: computedColor + '20' }">
            <el-icon :size="20" :color="computedColor">
              <component :is="icon" />
            </el-icon>
          </div>
          <div class="stat-actions" v-if="$slots.actions">
            <slot name="actions"></slot>
          </div>
        </div>
        <div class="stat-body">
          <div class="stat-value" :class="{ 'stat-large': large }">
            <el-skeleton v-if="loading" animated :rows="1" />
            <span v-else>{{ formattedValue }}</span>
          </div>
          <div class="stat-label">
            <el-skeleton v-if="loading" animated :rows="1" />
            <span v-else>{{ label }}</span>
          </div>
        </div>
      </div>

      <!-- 趋势指示器 -->
      <div class="stat-trend" v-if="trend && !loading">
        <div class="trend-indicator" :class="trend.type">
          <el-icon>
            <component :is="trend.up ? ArrowUp : ArrowDown" />
          </el-icon>
          <span class="trend-value">{{ trend.value }}</span>
        </div>
      </div>

      <!-- 附加信息 -->
      <div class="stat-extra" v-if="$slots.extra">
        <slot name="extra"></slot>
      </div>
    </div>

    <!-- 装饰性背景 -->
    <div class="stat-decoration" :style="{ backgroundColor: computedColor + '5' }"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

interface Trend {
  up: boolean
  value: string
  type: 'success' | 'danger' | 'warning' | 'info'
}

interface Props {
  label: string
  value: number | string
  icon: any
  color: string
  trend?: Trend
  format?: (value: number | string) => string
  large?: boolean
  loading?: boolean
  clickable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  large: false,
  loading: false,
  clickable: false,
  format: (value: number | string) => value.toString()
})

const emit = defineEmits<{
  click: []
}>()

const computedColor = computed(() => props.color)

const formattedValue = computed(() => {
  return props.format(props.value)
})

const handleClick = () => {
  if (props.clickable && !props.loading) {
    emit('click')
  }
}
</script>

<style scoped>
.stat-card {
  position: relative;
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.stat-clickable {
  cursor: pointer;
}

.stat-clickable:active {
  transform: translateY(0);
}

.stat-loading {
  pointer-events: none;
}

.stat-content {
  position: relative;
  z-index: 2;
}

.stat-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.stat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}

.stat-card:hover .stat-icon {
  transform: scale(1.05);
}

.stat-actions {
  margin-left: auto;
}

.stat-body {
  flex: 1;
  min-width: 0;
  margin-left: 12px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-value.stat-large {
  font-size: 32px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.4;
  word-break: break-word;
}

.stat-trend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 12px;
}

.trend-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 4px;
}

.trend-indicator.success {
  background: #f6ffed;
  color: #52c41a;
}

.trend-indicator.danger {
  background: #fff2f0;
  color: #ff4d4f;
}

.trend-indicator.warning {
  background: #fffbe6;
  color: #faad14;
}

.trend-indicator.info {
  background: #f0f9ff;
  color: #1890ff;
}

.trend-value {
  font-size: 12px;
  font-weight: 600;
}

.stat-extra {
  margin-top: 16px;
}

.stat-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  transform: translate(30px, -30px);
  opacity: 0.1;
  transition: all 0.3s ease;
}

.stat-card:hover .stat-decoration {
  transform: translate(40px, -40px);
  opacity: 0.15;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .stat-card {
    padding: 20px;
  }

  .stat-main {
    flex-direction: column;
    gap: 16px;
  }

  .stat-header {
    order: 2;
    align-self: flex-end;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
  }

  .stat-body {
    margin-left: 0;
  }

  .stat-value {
    font-size: 24px;
  }

  .stat-value.stat-large {
    font-size: 28px;
  }

  .stat-trend {
    justify-content: flex-start;
    margin-top: 8px;
  }
}

@media (max-width: 480px) {
  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 22px;
  }

  .stat-value.stat-large {
    font-size: 26px;
  }

  .stat-label {
    font-size: 13px;
  }
}
</style>