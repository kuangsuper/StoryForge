<template>
  <div class="outline-agent">
    <div class="page-header">
      <h3>大纲 Agent 对话</h3>
      <el-tag :type="chat.isConnected.value ? 'success' : 'danger'" size="small">
        {{ chat.isConnected.value ? '已连接' : '未连接' }}
      </el-tag>
    </div>
    <div class="chat-container">
      <AgentChat
        :messages="chat.messages.value"
        :is-streaming="chat.isStreaming.value"
        :current-stream-text="chat.currentStreamText.value"
        @send="chat.sendMessage"
        @clean="chat.clearHistory"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAgentChat } from '@/composables/useAgentChat'
import AgentChat from '@/components/AgentChat.vue'

const route = useRoute()
const projectId = route.params.projectId as string

const chat = useAgentChat('outline', projectId)
chat.connect()
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.chat-container { background: #fff; border-radius: 8px; height: calc(100vh - 180px); overflow: hidden; }
</style>
