import { useWebSocket } from './useWebSocket'

export function useAgentChat(agentType: string, projectId: string, extra?: Record<string, string>) {
  const ws = useWebSocket(agentType, projectId, extra)

  function sendMessage(text: string) {
    ws.send(text)
  }

  function clearHistory() {
    ws.cleanHistory()
  }

  return {
    ...ws,
    sendMessage,
    clearHistory,
  }
}
