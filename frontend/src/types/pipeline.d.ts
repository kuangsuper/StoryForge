/** 流水线请求 */
export interface PipelineRequest {
  projectId?: number
  genre: string
  prompt: string
  episodeCount: number
  chaptersPerEpisode: number
  videoMode: string
  autoCompose: boolean
  ttsEnabled: boolean
  reviewConfig: Record<string, ReviewMode>
}

export type ReviewMode = 'SKIP' | 'AI_AUTO' | 'HUMAN_REQUIRED'

/** 流水线状态 */
export interface PipelineStatus {
  projectId: number
  state: string
  currentStep: string
  currentStepIndex: number
  totalSteps: number
  steps: PipelineStepStatus[]
  startTime: string
  error: string
}

/** 流水线步骤状态 */
export interface PipelineStepStatus {
  name: string
  displayName: string
  state: 'pending' | 'running' | 'success' | 'failed' | 'skipped'
  duration: number
  message: string
}

/** 流水线进度事件 */
export interface PipelineProgressEvent {
  type: 'stepStart' | 'stepProgress' | 'stepComplete' | 'stepFailed' | 'pipelineComplete' | 'reviewRequired'
  step: string
  displayName: string
  stepIndex: number
  totalSteps: number
  message: string
  progress: number
}

/** 监控概览 */
export interface MonitorOverview {
  projectCount: number
  chapterCount: number
  videoCount: number
  totalWords: number
}
