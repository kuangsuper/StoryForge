/** 小说章节 */
export interface Novel {
  id: number
  projectId: number
  chapterIndex: number
  volumeIndex: number
  reel: string
  chapter: string
  chapterData: string
  summary: string
  createTime: string
  updateTime: string
}

/** 故事线 */
export interface Storyline {
  id: number
  projectId: number
  content: string
  createTime: string
}

/** 大纲 */
export interface Outline {
  id: number
  projectId: number
  episode: number
  data: EpisodeData
  createTime: string
}

/** 大纲集数据 */
export interface EpisodeData {
  episodeIndex: number
  title: string
  chapterRange: number[]
  scenes: { name: string; description: string }[]
  characters: { name: string; description: string }[]
  props: { name: string; description: string }[]
  coreConflict: string
  outline: string
  openingHook: string
  keyEvents: string[]
  emotionalCurve: string
  visualHighlights: string[]
  endingHook: string
  classicQuotes: string[]
}

/** 剧本 */
export interface Script {
  id: number
  projectId: number
  outlineId: number
  name: string
  content: string
  createTime: string
}

/** 资产 */
export interface Asset {
  id: number
  projectId: number
  name: string
  intro: string
  prompt: string
  type: 'role' | 'props' | 'scene'
  filePath: string
  createTime: string
}

/** 分镜片段 */
export interface Segment {
  id: number
  scriptId: number
  segmentIndex: number
  description: string
  emotion: string
  action: string
}

/** 分镜镜头 */
export interface Shot {
  id: number
  segmentId: number
  shotIndex: number
  prompt: string
  videoPrompt: string
  cameraMotion: string
  imagePath: string
  videoPath: string
  status: string
}

/** Prompt 模板 */
export interface PromptTemplate {
  id: number
  code: string
  name: string
  defaultValue: string
  customValue: string
}

/** AI 模型配置 */
export interface ModelConfig {
  id: number
  name: string
  provider: string
  modelName: string
  apiKey: string
  baseUrl: string
  type: string
  status: number
}

/** 素材 */
export interface Material {
  id: number
  name: string
  type: string
  filePath: string
  userId: number
  createTime: string
}

/** 任务 */
export interface Task {
  id: number
  projectId: number
  type: string
  state: string
  progress: number
  result: string
  errorMsg: string
  createTime: string
  updateTime: string
}
