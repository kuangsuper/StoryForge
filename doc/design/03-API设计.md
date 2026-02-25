# Toonflow API 设计

## 一、通用规范

- 基础路径：`/api`
- 认证：除 `/api/auth/login` 外，所有接口需 `Authorization: Bearer {token}`
- 响应格式：`{"code": 200, "message": "success", "data": {...}}`
- 分页参数：`?page=1&size=20`，响应 `{"records": [], "total": 100, "page": 1, "size": 20}`
- 错误码：200=成功, 400=参数错误, 401=未认证, 403=无权限, 404=不存在, 429=配额超限, 500=服务异常, 510=AI调用失败

## 二、REST API 清单

### 2.1 认证模块

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | /api/auth/login | 登录 | {name, password, captcha} | {token, userId, name, role} |
| POST | /api/auth/logout | 登出 | - | - |
| GET | /api/auth/captcha | 获取图形验证码 | - | {captchaId, image(base64)} |

### 2.2 用户模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/users/me | 获取当前用户信息 |
| PUT | /api/users/me | 更新当前用户信息 |
| PUT | /api/users/me/password | 修改密码 |
| GET | /api/users | 用户列表(管理员) |
| POST | /api/users | 创建用户(管理员) |
| PUT | /api/users/{id}/status | 启用/禁用用户(管理员) |
| DELETE | /api/users/{id} | 删除用户(管理员) |
| PUT | /api/users/{id}/role | 分配角色(管理员) |
| GET | /api/users/{id}/quota | 获取用户配额 |
| PUT | /api/users/{id}/quota | 调整用户配额(管理员) |

### 2.3 项目模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects | 创建项目 |
| GET | /api/projects | 项目列表(分页) |
| GET | /api/projects/{id} | 项目详情 |
| PUT | /api/projects/{id} | 更新项目 |
| DELETE | /api/projects/{id} | 删除项目(级联) |
| GET | /api/projects/stats | 项目统计 |

### 2.4 小说章节模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects/{projectId}/novels | 添加章节(支持批量) |
| GET | /api/projects/{projectId}/novels | 章节列表(不含全文) |
| GET | /api/projects/{projectId}/novels/{id} | 章节详情(含全文) |
| PUT | /api/projects/{projectId}/novels/{id} | 更新章节 |
| DELETE | /api/projects/{projectId}/novels/{id} | 删除章节 |
| GET | /api/projects/{projectId}/novels/{id}/versions | 章节版本历史 |
| PUT | /api/projects/{projectId}/novels/{id}/rollback/{version} | 回滚到指定版本 |
| GET | /api/projects/{projectId}/novel/export | 导出小说(?format=txt/epub/pdf/docx&scope=chapter/volume/book&index=1) |

### 2.5 AI 小说生成模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/projects/{projectId}/novel/world | 获取世界观 |
| PUT | /api/projects/{projectId}/novel/world | 更新世界观 |
| GET | /api/projects/{projectId}/novel/characters | 获取角色列表 |
| GET | /api/projects/{projectId}/novel/characters/{id} | 获取角色详情 |
| PUT | /api/projects/{projectId}/novel/characters/{id} | 更新角色 |
| DELETE | /api/projects/{projectId}/novel/characters/{id} | 删除角色 |
| GET | /api/projects/{projectId}/novel/outline | 获取小说大纲(含分卷) |
| PUT | /api/projects/{projectId}/novel/outline/{id} | 更新大纲/卷 |
| GET | /api/projects/{projectId}/novel/chapter-plans | 获取章概要列表(?volumeIndex=1) |
| PUT | /api/projects/{projectId}/novel/chapter-plans/{id} | 更新章概要 |
| GET | /api/projects/{projectId}/novel/progress | 获取生成进度 |
| POST | /api/projects/{projectId}/novel/quality-check | 触发质检(?scope=chapter/volume/book&index=1) |
| GET | /api/projects/{projectId}/novel/quality-reports | 获取质检报告列表 |
| GET | /api/projects/{projectId}/novel/quality-reports/{id} | 获取质检报告详情 |

### 2.6 故事线模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/projects/{projectId}/storylines | 获取故事线 |
| PUT | /api/projects/{projectId}/storylines | 保存/更新故事线 |
| DELETE | /api/projects/{projectId}/storylines | 删除故事线 |

### 2.7 大纲模块（分集大纲）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/projects/{projectId}/outlines | 大纲列表(?mode=simple/full) |
| POST | /api/projects/{projectId}/outlines | 添加大纲 |
| PUT | /api/projects/{projectId}/outlines/{id} | 更新大纲 |
| DELETE | /api/projects/{projectId}/outlines | 批量删除大纲(?ids=1,2,3) |
| POST | /api/projects/{projectId}/outlines/extract-assets | 从大纲提取资产 |

### 2.8 剧本模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/projects/{projectId}/scripts | 剧本列表 |
| GET | /api/scripts/{scriptId} | 剧本详情 |
| PUT | /api/scripts/{scriptId} | 更新剧本 |
| POST | /api/scripts/{scriptId}/generate | AI生成剧本 |

### 2.9 资产模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/projects/{projectId}/assets | 资产列表(?type=role/props/scene) |
| POST | /api/projects/{projectId}/assets | 添加资产 |
| PUT | /api/projects/{projectId}/assets/{id} | 更新资产 |
| DELETE | /api/projects/{projectId}/assets/{id} | 删除资产 |
| POST | /api/projects/{projectId}/assets/batch | 批量保存资产 |
| POST | /api/projects/{projectId}/assets/{id}/generate-image | 生成资产图片 |
| POST | /api/projects/{projectId}/assets/{id}/polish-prompt | AI润色提示词 |

### 2.10 分镜模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/scripts/{scriptId}/storyboards | 获取分镜数据 |
| POST | /api/scripts/{scriptId}/storyboards | 保存分镜 |
| PUT | /api/scripts/{scriptId}/storyboards/retain | 保留(确认)分镜 |
| POST | /api/scripts/{scriptId}/storyboards/generate-images | 生成分镜图片 |
| POST | /api/scripts/{scriptId}/storyboards/super-resolution | 批量超分辨率 |
| PUT | /api/scripts/{scriptId}/storyboards/shots/{shotId}/image | 替换分镜图片 |
| POST | /api/scripts/{scriptId}/storyboards/upload-image | 上传自定义图片 |
| POST | /api/scripts/{scriptId}/storyboards/generate-video-prompts | 生成视频提示词 |

### 2.11 视频模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects/{projectId}/videos/generate | 生成视频 |
| GET | /api/projects/{projectId}/videos | 视频列表(?scriptId=) |
| GET | /api/projects/{projectId}/videos/versions | 视频版本列表(?shotId=) |
| PUT | /api/projects/{projectId}/videos/{id}/select | 选择视频版本 |
| POST | /api/projects/{projectId}/videos/batch-retry | 批量重试失败视频 |
| POST | /api/projects/{projectId}/images/quality-check | 图片质量检测 |
| GET | /api/videos/models | 视频模型列表 |
| GET | /api/videos/manufacturers | 视频厂商列表 |

### 2.12 视频合成模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects/{projectId}/videos/compose | 提交合成任务 |
| GET | /api/projects/{projectId}/videos/compose/{id} | 查询合成状态 |
| POST | /api/projects/{projectId}/videos/compose/{id}/retry | 重试合成 |
| GET | /api/projects/{projectId}/videos/compose/list | 合成视频列表 |
| GET | /api/projects/{projectId}/videos/compose/config | 获取合成配置 |
| PUT | /api/projects/{projectId}/videos/compose/config | 更新合成配置 |

### 2.13 视频配置模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/projects/{projectId}/video-configs | 视频配置列表 |
| POST | /api/projects/{projectId}/video-configs | 创建视频配置 |
| PUT | /api/projects/{projectId}/video-configs/{id} | 更新视频配置 |
| DELETE | /api/projects/{projectId}/video-configs/{id} | 删除视频配置 |

### 2.14 TTS 配音模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects/{projectId}/tts/generate | 生成配音 |
| GET | /api/projects/{projectId}/tts/voices | 获取可用音色列表 |
| POST | /api/projects/{projectId}/tts/preview | 试听(指定文本+音色) |
| GET | /api/projects/{projectId}/tts/configs | 获取配音配置列表 |
| PUT | /api/projects/{projectId}/tts/configs/{id} | 更新配音配置 |

### 2.15 素材库模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/materials | 上传素材 |
| GET | /api/materials | 素材列表(?type=bgm/sfx/intro/outro/watermark) |
| DELETE | /api/materials/{id} | 删除素材 |

### 2.16 AI 模型配置模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/settings/models | 模型配置列表 |
| POST | /api/settings/models | 添加模型配置 |
| PUT | /api/settings/models/{id} | 更新模型配置 |
| DELETE | /api/settings/models/{id} | 删除模型配置 |
| POST | /api/settings/models/{id}/test | 测试模型连通性 |
| GET | /api/settings/model-maps | 获取功能→模型映射 |
| PUT | /api/settings/model-maps | 更新功能→模型映射 |

### 2.17 Prompt 模板模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/prompts | Prompt列表 |
| PUT | /api/prompts/{id} | 更新Prompt(customValue) |

### 2.18 任务模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/tasks | 任务列表(?state=pending/running/success/failed) |
| GET | /api/tasks/{id} | 任务详情 |

### 2.19 系统设置模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/settings | 获取系统设置 |
| PUT | /api/settings | 更新系统设置 |

### 2.20 全自动流水线模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects/{projectId}/pipeline/start | 启动全自动流水线 |
| GET | /api/projects/{projectId}/pipeline/status | 获取流水线状态 |
| POST | /api/projects/{projectId}/pipeline/pause | 暂停流水线 |
| POST | /api/projects/{projectId}/pipeline/resume | 恢复流水线 |
| POST | /api/projects/{projectId}/pipeline/cancel | 取消流水线 |
| POST | /api/projects/{projectId}/pipeline/retry-step | 重试当前步骤 |
| POST | /api/projects/{projectId}/pipeline/skip-step | 跳过当前步骤 |
| POST | /api/projects/{projectId}/pipeline/review-pass | 审核通过(人工审核节点) |
| POST | /api/batch/full-pipeline | 批量提交生产任务 |

### 2.21 监控仪表盘模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/monitor/overview | 总览统计(项目数/章节数/视频数/总字数) |
| GET | /api/monitor/model-stats | 模型调用统计(成功率/平均耗时/token消耗) |
| GET | /api/monitor/production-stats | 产量统计(日/周/月) |
| GET | /api/monitor/alerts | 告警列表 |

## 三、WebSocket 端点

| 端点 | 协议 | 用途 |
|------|------|------|
| /ws/agent/outline/{projectId} | STOMP | 大纲故事线Agent对话 |
| /ws/agent/storyboard/{projectId}?scriptId={scriptId} | STOMP | 分镜Agent对话 |
| /ws/agent/novel/{projectId} | STOMP | 小说生成Agent对话 |
| /ws/pipeline/{projectId} | STOMP | 全自动流水线进度推送 |

### WebSocket 消息格式

客户端发送目标（STOMP destination）：
- `/app/agent/{agentType}/{projectId}/send` — 发送消息给Agent
- `/app/agent/{agentType}/{projectId}/clean` — 清空对话历史

服务端推送主题（STOMP topic）：
- `/topic/agent/{agentType}/{projectId}/stream` — 流式文本
- `/topic/agent/{agentType}/{projectId}/event` — 事件通知(toolCall/transfer/refresh/subAgent等)
- `/topic/pipeline/{projectId}/progress` — 流水线进度

事件消息统一结构：
```json
{
  "type": "stream|toolCall|transfer|refresh|subAgentStream|subAgentEnd|chapterStart|chapterDelta|chapterEnd|progress|error|...",
  "data": { ... }
}
```
