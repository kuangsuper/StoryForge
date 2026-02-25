/** Agent 消息 */
export interface AgentMessage {
  role: 'user' | 'assistant' | 'system' | 'subAgent'
  content: string
  agent?: string
  timestamp?: number
}

/** WebSocket 事件 */
export interface AgentEvent {
  type: AgentEventType
  data: any
}

export type AgentEventType =
  | 'init'
  | 'stream'
  | 'response_end'
  | 'subAgentStream'
  | 'subAgentEnd'
  | 'toolCall'
  | 'transfer'
  | 'refresh'
  | 'chapterStart'
  | 'chapterDelta'
  | 'chapterEnd'
  | 'progress'
  | 'allComplete'
  | 'layerStart'
  | 'layerComplete'
  | 'reviewStart'
  | 'reviewResult'
  | 'qualityCheckStart'
  | 'qualityCheckProgress'
  | 'qualityCheckComplete'
  | 'error'
