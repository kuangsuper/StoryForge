<template>
  <el-image
    :src="src"
    :preview-src-list="previewList"
    fit="cover"
    :style="{ width: width, height: height }"
    class="image-preview"
  >
    <template #error>
      <div class="image-error">
        <el-icon><Picture /></el-icon>
      </div>
    </template>
  </el-image>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Picture } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  src: string
  width?: string
  height?: string
}>(), {
  width: '120px',
  height: '120px',
})

const previewList = computed(() => props.src ? [props.src] : [])
</script>

<style scoped>
.image-preview { border-radius: 6px; }
.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 24px;
}
</style>
