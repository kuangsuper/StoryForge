/** 通用 API 响应 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** 分页响应 */
export interface PageResult<T = any> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 用户 */
export interface User {
  id: number
  name: string
  role: string
  status: number
  createTime: string
}

/** 登录请求 */
export interface LoginRequest {
  name: string
  password: string
  captcha: string
  captchaId: string
}

/** 登录响应 */
export interface LoginResult {
  token: string
  userId: number
  name: string
  role: string
}

/** 验证码响应 */
export interface CaptchaResult {
  captchaId: string
  image: string
}

/** 项目 */
export interface Project {
  id: number
  name: string
  intro: string
  type: string
  artStyle: string
  videoRatio: string
  userId: number
  createTime: string
  updateTime: string
}

/** 创建/更新项目请求 */
export interface ProjectForm {
  name: string
  intro?: string
  type?: string
  artStyle?: string
  videoRatio?: string
}

/** 配额 */
export interface Quota {
  dailyChapters: number
  dailyImages: number
  dailyVideos: number
  usedChapters: number
  usedImages: number
  usedVideos: number
}

/** 系统设置 */
export interface SystemSetting {
  id: number
  key: string
  value: string
}
