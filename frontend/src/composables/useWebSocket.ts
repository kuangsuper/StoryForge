import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import type { AgentMessage, AgentEvent } from '@/types/agent'

export function useWebSocket(agentType: string, projectId: string, extra?: Record<string, string>) {
  const messages = ref<AgentMessage[]>([])
  const isStreaming = ref(false)
  const currentStreamText = ref('')
  const isConnected = ref(false)

  let queryStr = ''
  if (extra) {
    const params = new URLSearchParams(extra).toString()
    if (params) queryStr = '?' + params
  }

  const client = new Client({
    brokerURL: `ws://${location.host}/ws/agent/${agentType}/${projectId}${queryStr}`,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  })

  client.onConnect = () => {
    isConnected.value = true
    client.subscribe(`/topic/agent/${agentType}/${projectId}/event`, (msg) => {
      const event: AgentEvent = JSON.parse(msg.body)
      handleEvent(event)
    })
  }

  client.onDisconnect = () => {
    isConnected.value = false
  }

  client.onStompError = () => {
    isConnected.value = false
  }

  function handleEvent(event: AgentEvent) {
    switch (event.type) {
      case 'stream':
        isStreaming.value = true
        currentStreamText.value += event.data
        break
      case 'response_end':
        isStreaming.value = false
        if (currentStreamText.value) {
          messages.value.push({ role: 'assistant', content: currentStreamText.value, timestamp: Date.now() })
        }
        currentStreamText.value = ''
        break
      case 'subAgentStream':
        isStreaming.value = true
        currentStreamText.value += event.data.text
        break
      case 'subAgentEnd':
        isStreaming.value = false
        if (currentStreamText.value) {
          messages.value.push({ role: 'subAgent', content: currentStreamText.value, agent: event.data.agent, timestamp: Date.now() })
        }
        currentStreamText.value = ''
        break
      case 'toolCall':
        messages.value.push({ role: 'system', content: `🔧 调用工具: ${event.data.name}`, timestamp: Date.now() })
        break
      case 'transfer':
        messages.value.push({ role: 'system', content: `🔄 切换到: ${event.data.to}`, timestamp: Date.now() })
        break
      case 'error':
        messages.value.push({ role: 'system', content: `❌ ${event.data}`, timestamp: Date.now() })
        isStreaming.value = false
        currentStreamText.value = ''
        break
      default:
        // refresh, chapterDelta, progress 等由具体页面处理
        break
    }
  }

  function send(message: string) {
    messages.value.push({ role: 'user', content: message, timestamp: Date.now() })
    client.publish({
      destination: `/app/agent/${agentType}/${projectId}/msg`,
      body: JSON.stringify({ type: 'user', data: message }),
    })
  }

  function cleanHistory() {
    messages.value = []
    client.publish({
      destination: `/app/agent/${agentType}/${projectId}/cleanHistory`,
      body: '',
    })
  }

  function connect() {
    client.activate()
  }

  function disconnect() {
    client.deactivate()
  }

  onUnmounted(() => {
    client.deactivate()
  })

  return {
    messages,
    isStreaming,
    currentStreamText,
    isConnected,
    send,
    cleanHistory,
    connect,
    disconnect,
    client,
  }
}
