<div align="center">
  <img src="docs/logo.png" alt="StoryForge" width="120" />
  <h1>🎬 StoryForge</h1>
  <p><strong>AI 全链路短剧内容生产平台 — 从一句话到一部剧</strong></p>
  <p>
  </p>
  <p>输入一句创作提示，全自动完成 <strong>小说创作 → 故事线 → 大纲 → 剧本 → 分镜 → 图片 → 配音 → 视频 → 成片</strong> 的完整流水线</p>
  <p><strong>集群化部署，每次产出理论无上限 — 加机器就加产能，无需改一行代码</strong></p>
</div>

---

## 为什么选择 StoryForge？

| 能力 | 说明 |
|------|------|
| 🚀 全链路自动化 | 11 步流水线一键启动，从文字到视频零人工干预 |
| 🧠 多智能体协作 | 3 套 Agent 系统、12 个专职 AI 角色，模拟真实影视制作团队 |
| 📈 产能无上限 | Java 集群水平扩展 + Redis 分布式协调，理论日产无上限，取决于集群规模 |
| 🔌 20+ AI 模型 | 文本/图片/视频/语音全覆盖，10+ 厂商即插即用，后台动态切换 |
| 🏭 企业级架构 | JDK 21 虚拟线程、分布式锁、配额管控、多租户隔离、生产监控 |
| 🎯 智能容错 | 自动重试、Prompt 润色重试、降级分辨率重试、断点续跑 |

---

## 核心流水线

```
创作提示 / 上传小说
    ↓
🧠 AI 小说生成（7 Agent 协作：世界观 → 角色 → 大纲 → 章概要 → 逐章正文）
    ↓
📖 故事线提取 → 分集大纲生成 → AI 导演审核
    ↓
🎭 资产提取（角色/道具/场景自动入库）→ 资产图片生成
    ↓
📝 剧本生成 → 分镜生成（片段师 + 分镜师）
    ↓
🖼️ 分镜图片生成 → 图片质量检测 → 超分辨率增强
    ↓
🎬 视频生成（10-15s 片段）→ 三级自动重试
    ↓
🎞️ 视频合成（拼接 + 转场 + 字幕 + BGM + AI 配音 + 水印）
    ↓
✅ 完整短剧视频
```

---

## 核心特性

### 🧠 AI 小说生成 — 多智能体协作引擎

这不是简单的"让 AI 写小说"，而是一套完整的 AI 影视编剧团队：

- **7 个专职 Agent**：世界架构师、角色设计师、情节架构师、章节规划师、小说写手、总编审、质检官
- **5 层递进生成**：世界观 → 角色群像 → 大纲分卷 → 章概要 → 逐章正文
- **四层记忆体系**：固定记忆 + 角色记忆 + 短期记忆 + 中长期记忆，突破 LLM 上下文窗口限制
- **伏笔追踪机制**：自动埋设、暗示、回收，防止长篇小说伏笔遗漏
- **角色状态快照**：位置、伤势、情绪、持有物品、已知/未知信息全程追踪
- **角色对话风格分化**：每个角色有独立的语气、口头禅、用词习惯
- **7 维度质检**：角色一致性、情节连贯性、世界观合规性、伏笔完整性、品类爽点密度、文笔质量、可读性
- **去 AI 味 + 品类特化**：系统级去 AI 味指令，支持系统流/无限流/都市/玄幻等网文品类
- **TOON 格式传输**：自研压缩格式，节省 30-60% token 消耗

### 🎬 全自动内容生产流水线

- **一键 11 步全链路**：小说 → 故事线 → 大纲 → 资产 → 剧本 → 分镜 → 图片 → 质检 → 配音 → 视频 → 合成
- **流水线状态机**：支持暂停、跳过、重试、断点续跑，状态持久化到 Redis，服务重启不丢失
- **可配置审核节点**：跳过 / AI 自动审核 / 人工审核，灵活控制质量关卡
- **批量生产模式**：一次提交多个生产任务，Semaphore 控制并发度，并行处理

### 🎥 视频生成与合成

- **7+ 厂商视频模型**：火山引擎、可灵、Vidu、万象、Gemini Veo、Sora 等
- **三级自动重试**：相同参数重试 → AI 润色 Prompt 重试 → 降级分辨率重试
- **首尾帧衔接**：前一个片段的最后一帧作为下一个片段的首帧，保证视觉连续性
- **FFmpeg 专业合成**：转场效果、SRT 字幕烧录、BGM 混音、AI 配音音轨、水印叠加、片头片尾

### 📈 集群化部署 — 每次产出理论无上限

StoryForge 从架构层面彻底解决了 AI 内容生产的产能瓶颈。**加机器 = 加产能**，没有任何代码层面的上限。

```
                              ┌──────────────────────┐
                              │    Nginx / SLB / CDN  │
                              └──────────┬───────────┘
                                         │ 负载均衡
              ┌──────────────────────────┼──────────────────────────┐
              │                          │                           │
   ┌──────────▼─────────┐   ┌───────────▼────────┐   ┌─────────────▼──────┐
   │    Java Node 1      │   │    Java Node 2      │   │    Java Node N      │
   │   Spring Boot 3.3   │   │   Spring Boot 3.3   │   │   Spring Boot 3.3   │
   │   JDK 21 虚拟线程   │   │   JDK 21 虚拟线程   │   │   JDK 21 虚拟线程   │
   │   并发流水线 × 50+  │   │   并发流水线 × 50+  │   │   并发流水线 × 50+  │
   └──────────┬──────────┘   └───────────┬────────┘   └─────────────┬──────┘
              │                          │                           │
              └──────────────────────────┼──────────────────────────┘
                                         │
              ┌──────────────────────────┼──────────────────────────┐
              │                          │                           │
   ┌──────────▼─────────┐   ┌───────────▼────────┐   ┌─────────────▼──────┐
   │   MySQL 8.0         │   │   Redis Cluster     │   │   Python Pool       │
   │   主库 + 只读副本   │   │   分布式锁           │   │   FFmpeg 合成节点   │
   │   30 张业务表       │   │   流水线状态持久化   │   │   图片超分处理      │
   └────────────────────┘   │   任务队列           │   └────────────────────┘
                             └────────────────────┘
                                         │
                             ┌───────────▼────────┐
                             │   云 OSS (S3 兼容)  │
                             │   图片/视频/配音    │
                             └────────────────────┘
```

**为什么能做到产能无上限：**

| 设计决策 | 效果 |
|---------|------|
| Java 服务完全无状态 | 流水线状态写 Redis，任意节点可接手任意任务，横向加节点零改造 |
| JDK 21 虚拟线程 | 单节点可同时跑数千条流水线，不受传统线程池数量限制 |
| Redis 分布式协调 | 分布式锁保证配额/Agent 并发安全，多节点协作无冲突 |
| 批量生产 + Semaphore | 一次提交 N 个项目，并发度可配置，充分压榨集群算力 |
| Python 服务独立扩容 | FFmpeg 合成节点单独部署，视频合成不拖累 AI 生成吞吐 |
| AI 模型并发调用 | 每个分镜独立调用 AI 模型，N 个分镜 = N 路并发，不串行等待 |

**产能公式：**

```
每次产出 = 节点数(N) × 单节点并发流水线数 × AI 模型并发吞吐
         = 无上限（取决于集群规模和 AI 模型配额）
```

**参考基准（单次批量提交）：**

| 集群规模 | 单节点并发 | 单次可并行产出 | 适用场景 |
|---------|-----------|-------------|---------|
| 1 节点（8C16G） | 10-20 条流水线 | 10-20 部短剧/批次 | 个人/小团队 |
| 5 节点 | 10-20 条/节点 | 50-100 部短剧/批次 | 中型工作室 |
| 10 节点 | 10-20 条/节点 | 100-200 部短剧/批次 | 规模化生产 |
| N 节点 | 可配置 | **理论无上限** | 企业级产线 |

> 实际产出受 AI 模型 API 并发配额影响，提升模型配额即可线性提升产能。  
> 加机器不需要改任何代码，`docker compose --scale` 或 `kubectl scale` 一条命令完成扩容。

---

## 技术架构

```
┌──────────────────────────────────────────────────────────────┐
│                     Vue 3 前端 (Nginx)                        │
│          Vite + TypeScript + Pinia + Element Plus             │
│          UnoCSS + WebSocket STOMP + 响应式布局                 │
└───────────────┬──────────────────────┬───────────────────────┘
                │ REST API             │ WebSocket (STOMP)
                ▼                      ▼
┌──────────────────────────────────────────────────────────────┐
│               Spring Boot 3.3 主服务 (JDK 21)                │
│                                                               │
│  ┌───────────┐ ┌────────────┐ ┌───────────┐ ┌─────────────┐ │
│  │ 21+ REST  │ │ 3 套 Agent │ │ AI 统一   │ │ Security    │ │
│  │ Controller│ │ 系统(12角色)│ │ Provider  │ │ JWT + RBAC  │ │
│  └─────┬─────┘ └─────┬──────┘ └─────┬─────┘ └─────────────┘ │
│        │              │              │                         │
│  ┌─────▼─────┐ ┌──────▼─────┐ ┌─────▼──────┐                │
│  │ Service   │ │ Tool       │ │ 10+ 厂商   │                │
│  │ Layer     │ │ Registry   │ │ 模型适配器  │                │
│  └─────┬─────┘ └────────────┘ └────────────┘                 │
│        │                                                      │
│  ┌─────▼───────────────┐  ┌────────────────────┐             │
│  │ MyBatis-Plus        │  │    Redis 7+        │             │
│  │ + MySQL 8.0 (30表)  │  │ 分布式锁/状态/缓存  │             │
│  └─────────────────────┘  └────────────────────┘             │
└───────────────┬──────────────────────────────────────────────┘
                │ HTTP
                ▼
┌──────────────────────────────────────────────────────────────┐
│            Python FastAPI 微服务                              │
│   图片超分 · 宫格图分割 · FFmpeg 视频合成 · 文档导出            │
└──────────────────────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│            云 OSS (S3 兼容)                                   │
│   图片 · 视频 · 配音 · 素材 · 导出文件                         │
└──────────────────────────────────────────────────────────────┘
```

---

## 技术栈

| 层级 | 技术选型 | 亮点 |
|------|---------|------|
| 后端框架 | Spring Boot 3.3 / JDK 21 | 虚拟线程，单节点数千并发 |
| 数据库 | MySQL 8.0+（可切 TiDB / PolarDB） | 30 张表，全链路数据建模 |
| ORM | MyBatis-Plus 3.5 | 零 SQL 开发，自动分页 |
| 缓存/协调 | Redis 7+ | 分布式锁、流水线状态持久化、任务队列 |
| 认证 | Spring Security + JWT + Redis | Token 主动失效，多角色 RBAC |
| 前端 | Vue 3 + TypeScript + Vite | Pinia 状态管理，WebSocket 实时通信 |
| UI | Element Plus + UnoCSS | 原子化 CSS，响应式布局 |
| Python 服务 | FastAPI + FFmpeg + NumPy | 视频合成、图片超分、宫格图分割 |
| 对象存储 | AWS S3 / 阿里云 OSS / 腾讯云 COS | S3 兼容协议，一套代码多云适配 |
| 容器化 | Docker + docker-compose / K8s | 一键部署，集群编排 |
| 文档导出 | Apache POI + 自定义样式 | TXT / DOCX，带目录结构 |

---

## 支持的 AI 模型（20+ 模型，10+ 厂商）

所有模型均可在后台动态配置，每个功能点可独立绑定不同模型，运行时热切换。

### 文本模型（小说生成 / Agent 对话 / 剧本生成）

| 厂商 | 模型 |
|------|------|
| 通义千问 | qwen-max、qwen-plus、qwen2.5-72b |
| DeepSeek | deepseek-chat、deepseek-reasoner |
| 豆包 | doubao-seed 系列 |
| 智谱 | glm-4.5 / glm-4.6 / glm-4.7 系列 |
| OpenAI | gpt-4o、gpt-4.1、gpt-5 系列 |
| Gemini | gemini-2.5-pro、gemini-2.5-flash |
| Anthropic | claude-opus-4-5、claude-sonnet-4-5 |
| XAI | grok-3、grok-4 系列 |
| 其他 | 任意 OpenAI 兼容接口均可接入 |

### 图片模型

| 厂商 | 模型 |
|------|------|
| Gemini | gemini-2.5-flash-image、gemini-3-pro-image |
| 火山引擎 | doubao-seedream-4-5 |
| 可灵 | kling-image-o1 |
| Vidu | viduq1、viduq2 |
| RunningHub | nanobanana |

### 视频模型

| 厂商 | 模型 | 时长 |
|------|------|------|
| 火山引擎 | doubao-seedance-1-5-pro | 2-12s |
| 可灵 | kling-v2-6(PRO) | 5-10s |
| Vidu | viduq3-pro | 1-16s |
| 万象 | wan2.6-t2v/i2v | 2-15s |
| Gemini | veo-3.1 | 4-8s |
| Sora | sora-2 (RunningHub/Apimart) | 10-25s |

### TTS 语音模型

| 厂商 | 能力 |
|------|------|
| 火山引擎 | 多音色、情感控制 |
| 其他 | 可扩展接入任意 TTS 服务 |

---

## 多智能体系统（3 套系统，12 个 AI 角色）

### 小说生成 Agent — 7 角色协作

```
NovelMainAgent（总编/调度器）
├── WorldArchitect（世界架构师）— 世界观、力量体系、社会规则
├── CharacterDesigner（角色设计师）— 角色群像、人设档案
├── PlotArchitect（情节架构师）— 全书大纲、分卷结构
├── ChapterPlanner（章节规划师）— 章概要、伏笔规划
├── NovelWriter（小说写手）— 逐章正文生成（流式输出）
├── Editor（总编审）— 质量审核、打回修改
└── QualityInspector（质检官）— 7 维度深度质检
```

### 大纲故事线 Agent — 3 角色协作

```
MainAgent（协调者）
├── AI1（故事师）— 分析小说、生成故事线
├── AI2（大纲师）— 生成分集大纲
└── Director（导演）— 审核质量、提出修改意见
```

### 分镜 Agent — 2 角色协作

```
MainAgent（协调者）
├── SegmentAgent（片段师）— 拆分剧本为片段
└── ShotAgent（分镜师）— 生成镜头提示词
```

所有 Agent 共享统一的 BaseAgent 基础框架：WebSocket 实时通信、工具注册、消息队列、断点恢复、日志追踪。

---

## 22 大功能模块

| 模块 | 说明 |
|------|------|
| 用户与认证 | JWT 鉴权、4 级角色权限（超管/管理员/创作者/查看者）、数据隔离 |
| 项目管理 | 项目 CRUD、统计、完整级联删除（清理 16 张关联表） |
| 小说管理 | 手动上传章节、批量导入、版本管理 |
| AI 小说生成 | 7 Agent 多智能体协作、5 层递进生成、四层记忆、质检系统 |
| 故事线管理 | AI 提取故事线、手动编辑 |
| 大纲管理 | 分集大纲、资产自动提取 |
| 大纲故事线 Agent | 故事师 + 大纲师 + 导演，WebSocket 实时对话 |
| 资产管理 | 角色/道具/场景自动入库、图片生成、提示词润色 |
| 剧本管理 | AI 生成 500-800 字短剧剧本 |
| 分镜管理 | 分镜图生成、宫格图分割、批量超分辨率增强 |
| 分镜 Agent | 片段师 + 分镜师，WebSocket 实时对话 |
| 图片生成 | 5+ 厂商、文生图/图生图、质量检测、超分辨率 |
| 视频生成 | 7+ 厂商、三级自动重试、首尾帧衔接、多版本管理 |
| 视频合成 | FFmpeg 拼接、转场、字幕、BGM、配音、水印 |
| AI 配音 | 多厂商 TTS、角色音色配置、台词级配音 |
| AI 模型配置 | 多厂商模型管理、功能→模型映射、连通性测试、运行时热切换 |
| Prompt 模板 | 数据库管理、用户自定义覆盖 |
| 素材库 | BGM、片头片尾模板、水印管理 |
| 小说导出 | TXT / DOCX（带目录结构） |
| 生产监控 | 实时产量、模型调用统计（按模型分组）、流水线状态 |
| 全自动流水线 | 一键 11 步全链路、批量生产、审核节点配置、断点续跑 |
| 系统设置 | 全局配置、日志查看 |

---

## 企业级能力

| 能力 | 实现方式 |
|------|---------|
| 分布式锁 | Redis SETNX，配额扣减、Agent 会话均有锁保护 |
| 流水线状态持久化 | Redis + MySQL 双写，服务重启不丢失进度 |
| Agent 内存管理 | 30 分钟空闲自动回收，定时清理防止内存泄漏 |
| 配额管控 | 每日自动重置，分布式锁防并发超额 |
| 异步任务池 | 自定义 ThreadPoolTaskExecutor + ScheduledExecutorService |
| 多租户隔离 | 项目级数据隔离，角色级功能隔离 |
| 智能重试 | 视频生成三级重试：原参数 → AI 润色 Prompt → 降级分辨率 |
| 非阻塞轮询 | 视频生成轮询使用独立调度线程池，不阻塞业务线程 |
| OkHttp 连接池 | 共享 HTTP 客户端，避免连接池泄漏 |
| 优雅降级 | DOCX 导出失败自动降级为 TXT |

---

## 数据模型（30 张表）

| 分类 | 表 |
|------|---|
| 用户 | t_user、t_user_role、t_user_quota |
| 项目 | t_project |
| 小说 | t_novel、t_novel_world、t_novel_character、t_novel_outline、t_novel_chapter_plan、t_novel_quality_report、t_novel_version |
| 内容 | t_storyline、t_outline、t_script、t_assets |
| 媒体 | t_image、t_video、t_video_config、t_video_compose_config、t_video_compose |
| 配音 | t_tts_config、t_tts_audio |
| 素材 | t_material |
| AI 配置 | t_config、t_ai_model_map、t_prompts |
| 系统 | t_setting、t_chat_history、t_task_list、t_agent_log |

---

## 项目结构

```
storyforge/
├── storyforge-server/              # Spring Boot 3 主服务
│   └── src/main/java/com/toonflow/
│       ├── config/                 # Security、CORS、Async、WebSocket
│       ├── controller/             # 21+ REST Controller
│       ├── entity/                 # 30 张表 Entity
│       ├── mapper/                 # MyBatis-Plus Mapper
│       ├── service/                # 业务逻辑 + 流水线引擎
│       │   └── pipeline/           # 流水线状态机 + Redis 持久化
│       ├── agent/                  # 多智能体系统
│       │   ├── core/               # BaseAgent、AgentSession、ToolRegistry
│       │   ├── novel/              # 小说生成 Agent（7 Sub-Agent）
│       │   ├── outline/            # 大纲故事线 Agent（3 Sub-Agent）
│       │   └── storyboard/         # 分镜 Agent（2 Sub-Agent）
│       ├── ai/                     # AI Provider 统一抽象层
│       │   ├── provider/           # Text / Image / Video Provider
│       │   ├── model/              # AiRequest / AiResponse / ToolCall
│       │   └── retry/              # 重试模板
│       ├── security/               # JWT + RBAC
│       └── common/                 # ApiResponse、BizException、ErrorCode
├── python-service/                 # Python FastAPI 微服务
│   ├── main.py                     # 入口（CORS、日志、异常处理）
│   ├── routers/                    # 图片/视频路由
│   ├── services/                   # 图片超分、视频合成
│   └── utils/                      # FFmpeg 工具封装
├── frontend/                       # Vue 3 前端
│   └── src/
│       ├── views/                  # 页面（项目/小说/分镜/视频/监控...）
│       ├── components/             # 通用组件
│       ├── api/                    # API 封装（与后端 1:1 对应）
│       ├── stores/                 # Pinia 状态管理
│       ├── composables/            # WebSocket STOMP hooks
│       └── types/                  # TypeScript 类型定义
└── doc/
    ├── PRD.md                      # 功能需求规格说明书（22 模块）
    ├── 系统设计与技术选型.md         # 技术架构文档
    └── design/                     # 6 份详细设计文档
        ├── 01-数据库设计.md
        ├── 02-后端架构设计.md
        ├── 03-API设计.md
        ├── 04-Agent系统设计.md
        ├── 05-流水线与Python服务设计.md
        └── 06-前端设计.md
```

---

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- Python 3.10+
- MySQL 8.0+
- Redis 7+
- FFmpeg 6+（视频合成）

### 本地启动

```bash
# 1. 克隆项目
git clone https://github.com/your-org/storyforge.git
cd storyforge

# 2. 启动基础设施
docker compose up -d mysql redis

# 3. 启动 Java 主服务
cd storyforge-server
mvn spring-boot:run

# 4. 启动 Python 服务
cd python-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001

# 5. 启动前端
cd frontend
npm install
npm run dev
```

### Docker 一键部署

```bash
docker compose up -d
```

### 集群部署 — 一条命令扩容

```bash
# Docker Compose 水平扩展（无状态，直接加节点）
docker compose up -d --scale storyforge-server=5
docker compose up -d --scale python-service=3   # 同步扩容 FFmpeg 合成节点

# Kubernetes 部署
kubectl apply -f k8s/
kubectl scale deployment storyforge-server --replicas=10
kubectl scale deployment python-service --replicas=5

# 验证扩容效果
kubectl get pods -l app=storyforge-server
```

> 扩容不需要改任何代码或配置，Redis 自动协调多节点任务分发。

---

## 开发路线图

- [x] Phase 1-3：基础骨架 — 用户认证、项目管理、数据库 30 表
- [x] Phase 4-5：AI 小说生成 — 7 Agent 多智能体、四层记忆、质检系统
- [x] Phase 6-8：内容生产 — 大纲/剧本/分镜 Agent、图片生成、资产管理
- [x] Phase 9-11：视频生产 — 多厂商视频生成、FFmpeg 合成、TTS 配音
- [x] Phase 12-13：企业级 — 全自动流水线、批量生产、监控仪表盘、素材库
- [ ] Next：Kubernetes 编排、多语言支持、移动端适配

---

## 文档

- [功能需求规格说明书 (PRD)](doc/PRD.md) — 22 个模块完整功能设计
- [系统设计与技术选型](doc/系统设计与技术选型.md) — 技术架构、部署方案
- [数据库设计](doc/design/01-数据库设计.md) — 30 张表详细设计
- [后端架构设计](doc/design/02-后端架构设计.md) — 分层架构、Agent 系统
- [API 设计](doc/design/03-API设计.md) — RESTful API 规范
- [Agent 系统设计](doc/design/04-Agent系统设计.md) — 多智能体协作架构
- [流水线设计](doc/design/05-流水线与Python服务设计.md) — 状态机、Python 服务
- [前端设计](doc/design/06-前端设计.md) — Vue 3 组件架构

---

## License

[MIT](LICENSE)

作者联系方式WX：kuang293
