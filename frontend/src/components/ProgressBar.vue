<template>
  <div class="progress-bar">
    <div class="progress-info">
      <span>{{ label }}</span>
      <span>{{ current }}/{{ total }}</span>
    </div>
    <el-progress :percentage="percentage" :status="status" :stroke-width="8" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  current: number
  total: number
  label?: string
  status?: '' | 'success' | 'warning' | 'exception'
}>(), {
  label: '进度',
  status: '',
})

const percentage = computed(() => {
  if (props.total <= 0) return 0
  return Math.round((props.current / props.total) * 100)
})
</script>

<style scoped>
.progress-bar { margin-bottom: 12px; }
.progress-info { display: flex; justify-content: space-between; margin-bottom: 4px; font-size: 13px; color: #666; }
</style>
