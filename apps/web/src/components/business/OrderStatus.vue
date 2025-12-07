<template>
  <div :class="['order-status', statusClass]">
    <i :class="statusIcon"></i>
    <span>{{ statusText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'
  size?: 'small' | 'medium' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium'
})

const statusClass = computed(() => {
  const baseClass = `status-${props.status.toLowerCase()}`
  return `${baseClass} size-${props.size}`
})

const statusIcon = computed(() => {
  const icons = {
    'PENDING': 'fas fa-clock',
    'CONFIRMED': 'fas fa-check-circle',
    'COMPLETED': 'fas fa-check-double',
    'CANCELLED': 'fas fa-times-circle'
  }
  return icons[props.status]
})

const statusText = computed(() => {
  const texts = {
    'PENDING': '待确认',
    'CONFIRMED': '已确认',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[props.status]
})
</script>

<style scoped>
.order-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.order-status i {
  font-size: 10px;
}

.size-small {
  padding: 4px 8px;
  font-size: 11px;
}

.size-small i {
  font-size: 9px;
}

.size-medium {
  padding: 6px 12px;
  font-size: 12px;
}

.size-medium i {
  font-size: 10px;
}

.size-large {
  padding: 8px 16px;
  font-size: 14px;
}

.size-large i {
  font-size: 12px;
}

.status-pending {
  background-color: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

.status-confirmed {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-completed {
  background-color: #cce5ff;
  color: #004085;
  border: 1px solid #b3d9ff;
}

.status-cancelled {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.status-pending i {
  color: #856404;
}

.status-confirmed i {
  color: #155724;
}

.status-completed i {
  color: #004085;
}

.status-cancelled i {
  color: #721c24;
}
</style>