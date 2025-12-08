<template>
  <div class="order-timeline">
    <div class="timeline-item" v-for="(item, index) in timelineItems" :key="index">
      <div :class="['timeline-dot', item.status, { active: item.completed }]">
        <i :class="item.icon"></i>
      </div>
      <div class="timeline-content">
        <div class="timeline-title">{{ item.title }}</div>
        <div class="timeline-description">{{ item.description }}</div>
        <div v-if="item.time" class="timeline-time">{{ formatDateTime(item.time) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { OrderResponse } from '@/types/order'

interface Props {
  order: OrderResponse
}

const props = defineProps<Props>()

interface TimelineItem {
  title: string
  description: string
  time?: string
  icon: string
  status: string
  completed: boolean
}

const timelineItems = computed((): TimelineItem[] => {
  const items: TimelineItem[] = [
    {
      title: '订单创建',
      description: '订单已创建，等待确认',
      time: props.order.createdAt,
      icon: 'fas fa-plus',
      status: 'created',
      completed: true
    }
  ]

  // 根据订单状态添加不同的时间线项目
  if (props.order.status === 'CONFIRMED' || props.order.status === 'COMPLETED' || props.order.status === 'CANCELLED') {
    items.push({
      title: '订单确认',
      description: '订单已确认，预订成功',
      icon: 'fas fa-check-circle',
      status: 'confirmed',
      completed: true
    })
  } else if (props.order.status === 'PENDING') {
    items.push({
      title: '等待确认',
      description: '订单正在等待确认',
      icon: 'fas fa-clock',
      status: 'pending',
      completed: false
    })
  }

  // 如果订单已取消
  if (props.order.status === 'CANCELLED') {
    items.push({
      title: '订单取消',
      description: props.order.refundInfo?.cancelReason || '订单已取消',
      time: props.order.modifiedAt,
      icon: 'fas fa-times-circle',
      status: 'cancelled',
      completed: true
    })
  }

  // 如果订单已完成
  if (props.order.status === 'COMPLETED') {
    items.push({
      title: '入住完成',
      description: '客人已成功入住并完成住宿',
      time: props.order.updatedAt,
      icon: 'fas fa-home',
      status: 'completed',
      completed: true
    })
  }

  return items
})

const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.order-timeline {
  position: relative;
  padding-left: 30px;
}

.order-timeline::before {
  content: '';
  position: absolute;
  left: 11px;
  top: 20px;
  bottom: 20px;
  width: 2px;
  background-color: #e9ecef;
}

.timeline-item {
  position: relative;
  margin-bottom: 30px;
}

.timeline-item:last-child {
  margin-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: -30px;
  top: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
  border: 2px solid #dee2e6;
  z-index: 1;
}

.timeline-dot i {
  font-size: 10px;
  color: #6c757d;
}

.timeline-dot.active {
  background-color: #28a745;
  border-color: #28a745;
}

.timeline-dot.active i {
  color: white;
}

.timeline-dot.created.active {
  background-color: #007bff;
  border-color: #007bff;
}

.timeline-dot.confirmed.active {
  background-color: #28a745;
  border-color: #28a745;
}

.timeline-dot.pending {
  background-color: #ffc107;
  border-color: #ffc107;
}

.timeline-dot.pending i {
  color: white;
}

.timeline-dot.cancelled.active {
  background-color: #dc3545;
  border-color: #dc3545;
}

.timeline-dot.completed.active {
  background-color: #6f42c1;
  border-color: #6f42c1;
}

.timeline-content {
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-left: 4px solid #007bff;
}

.timeline-title {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 5px;
  font-size: 16px;
}

.timeline-description {
  color: #666;
  margin-bottom: 8px;
  font-size: 14px;
}

.timeline-time {
  color: #999;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .order-timeline {
    padding-left: 20px;
  }

  .timeline-dot {
    left: -20px;
    width: 20px;
    height: 20px;
  }

  .timeline-content {
    padding: 12px 15px;
  }

  .timeline-title {
    font-size: 14px;
  }

  .timeline-description {
    font-size: 13px;
  }
}
</style>