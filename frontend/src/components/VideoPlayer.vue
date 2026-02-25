<template>
  <div class="video-player">
    <video
      ref="videoRef"
      :src="src"
      controls
      :style="{ width: width, maxHeight: maxHeight }"
      preload="metadata"
    >
      您的浏览器不支持视频播放
    </video>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{
  src: string
  width?: string
  maxHeight?: string
}>(), {
  width: '100%',
  maxHeight: '400px',
})

const videoRef = ref<HTMLVideoElement>()
</script>

<style scoped>
.video-player { border-radius: 8px; overflow: hidden; background: #000; }
.video-player video { display: block; }
</style>
