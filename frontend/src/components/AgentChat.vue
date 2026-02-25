<template>
  <div class="agent-chat">
    <div ref="messagesRef" class="chat-messages">
      <AgentMessage v-for="(msg, i) in messages" :key="i" :message="msg" />
      <!-- 流式输出 -->
      <div v-if="isStreaming && currentStreamText" class="message assistant">
        <div class="message-content">
          <MarkdownRenderer :content="currentStreamText" />
          <span class="typing-cursor">▊</span>
        </div>
      </div>
    </div>
    <div class="chat-input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入消息..."
        @keydown.enter.exact.prevent="handleSend"
        :disabled="isStreaming"
      />
      <div class="input-actions">
        <el-button type="primary" :disabled="!inputText.trim() || isStreaming" @click="handleSend">发送</el-button>
        <el-button @click="$emit('clean')" :disabled="isStreaming">清空</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import AgentMessage from './AgentMessage.vue'
import MarkdownRenderer from './MarkdownRenderer.vue'
import type { AgentMessage as AgentMessageType } from '@/types/agent'

const props = defineProps<{
  messages: AgentMessageType[]
  isStreaming: boolean
  currentStreamText: string
}>()

const emit = defineEmits<{
  send: [text: string]
  clean: []
}>()

const inputText = ref('')
const messagesRef = ref<HTMLElement>()

function handleSend() {
  const text = inputText.value.trim()
  if (!text) return
  emit('send', text)
  inputText.value = ''
}

watch(() => [props.messages.length, props.currentStreamText], () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
})
</script>

<style scoped>
.agent-chat { display: flex; flex-direction: column; height: 100%; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; }
.chat-input { padding: 12px 16px; border-top: 1px solid #e8e8e8; background: #fff; }
.input-actions { display: flex; gap: 8px; margin-top: 8px; justify-content: flex-end; }
.message { margin-bottom: 12px; }
.message.assistant .message-content {
  background: #f0f7ff;
  padding: 12px;
  border-radius: 8px;
  line-height: 1.6;
}
.typing-cursor { animation: blink 1s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
</style>
