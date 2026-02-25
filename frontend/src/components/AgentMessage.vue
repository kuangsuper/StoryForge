<template>
  <div :class="['message', message.role]">
    <div class="message-avatar">
      <span v-if="message.role === 'user'">👤</span>
      <span v-else-if="message.role === 'assistant'">🤖</span>
      <span v-else-if="message.role === 'subAgent'">🔧</span>
      <span v-else>ℹ️</span>
    </div>
    <div class="message-body">
      <div v-if="message.agent" class="message-agent">{{ message.agent }}</div>
      <div class="message-content">
        <MarkdownRenderer v-if="message.role !== 'system'" :content="message.content" />
        <span v-else class="system-text">{{ message.content }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import MarkdownRenderer from './MarkdownRenderer.vue'
import type { AgentMessage } from '@/types/agent'

defineProps<{ message: AgentMessage }>()
</script>

<style scoped>
.message { display: flex; gap: 10px; margin-bottom: 16px; }
.message.user { flex-direction: row-reverse; }
.message-avatar { font-size: 24px; flex-shrink: 0; }
.message-body { max-width: 80%; }
.message-agent { font-size: 12px; color: #999; margin-bottom: 4px; }
.message-content {
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.6;
  word-break: break-word;
}
.message.user .message-content { background: #409eff; color: #fff; }
.message.assistant .message-content { background: #f0f7ff; }
.message.subAgent .message-content { background: #f0f9eb; }
.message.system .message-content { background: transparent; padding: 4px 0; }
.system-text { color: #999; font-size: 13px; }
</style>
