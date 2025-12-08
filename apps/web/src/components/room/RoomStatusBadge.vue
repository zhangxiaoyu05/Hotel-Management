<template>
  <el-tag :type="statusConfig.color" size="small">
    {{ statusConfig.label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ROOM_STATUS_OPTIONS } from '@/types/room';

interface Props {
  status: string;
}

const props = defineProps<Props>();

const statusConfig = computed(() => {
  return ROOM_STATUS_OPTIONS.find(option => option.value === props.status) || {
    value: props.status,
    label: props.status,
    color: 'default'
  };
});
</script>