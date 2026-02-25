/** 视频记录 */
export interface Video {
  id: number
  projectId: number
  scriptId: number
  shotId: number
  provider: string
  taskId: string
  videoUrl: string
  status: string
  version: number
  selected: boolean
  createTime: string
}

/** 视频配置 */
export interface VideoConfig {
  id: number
  projectId: number
  provider: string
  model: string
  mode: string
  duration: number
  resolution: string
  fps: number
}

/** 视频合成配置 */
export interface VideoComposeConfig {
  id: number
  projectId: number
  transition: string
  transitionDuration: number
  bgmPath: string
  bgmVolume: number
  subtitleEnabled: boolean
  subtitleStyle: string
  watermarkText: string
  watermarkPosition: string
  watermarkOpacity: number
  introText: string
  introDuration: number
  outroText: string
  outroDuration: number
  outputResolution: string
  outputFps: number
}

/** 视频合成任务 */
export interface VideoComposeTask {
  id: number
  projectId: number
  scriptId: number
  status: string
  outputUrl: string
  errorMsg: string
  createTime: string
}

/** TTS 配音配置 */
export interface TtsConfig {
  id: number
  projectId: number
  provider: string
  voiceId: string
  speed: number
  volume: number
}

/** TTS 音色 */
export interface TtsVoice {
  id: string
  name: string
  gender: string
  language: string
  provider: string
  sampleUrl: string
}
