# Toonflow 功能需求规格说明书

## 一、产品概述

Toonflow 是一个 AI 驱动的短剧/漫剧内容生产平台。核心能力是将小说（手动上传或 AI 生成）自动转化为可视化短剧内容，覆盖从文字创作到视频成片的全链路。

### 1.1 核心流水线

```
小说来源（二选一）
  ├── 手动上传小说章节
  └── AI 自动生成小说（新增核心功能）
        ↓
故事线生成（AI故事师分析小说 → 提炼故事线）
        ↓
导演审核（人工/AI导演审核，可打回修改）
        ↓
大纲生成（AI大纲师根据故事线 → 生成分集大纲）
        ↓
导演审核（同上）
        ↓
资产提取（从大纲自动提取角色/道具/场景，去重入库）
        ↓
资产图片生成（为每个角色/道具/场景生成参考图）
        ↓
剧本生成（单集大纲+原文 → AI生成500-800字剧本）
        ↓
分镜生成（片段师拆分剧本 → 分镜师生成镜头提示词）
        ↓
分镜图片生成（图片模型生成宫格图 → 自动分割单张镜头图）
        ↓
视频生成（分镜图+提示词 → 视频模型异步生成视频片段）
```

### 1.2 技术栈

| 层 | 选型 |
|---|------|
| 后端 | Spring Boot 3 + JDK 21 (Virtual Threads) |
| AI/图片处理 | Python (FastAPI) |
| 前端 | Vue 3 + TypeScript + Vite |
| 数据库 | MySQL 8.0+（上云后切分布式数据库，如 TiDB / PolarDB） |
| 缓存 | Redis 7+（会话、热数据缓存、分布式锁、任务队列） |
| 消息队列 | Redis Stream / RabbitMQ（异步任务调度） |
| 对象存储 | 云 OSS（S3 兼容，如 AWS S3 / 阿里云 OSS / 腾讯云 COS） |
| 容器化 | Docker + docker-compose（本地）/ Kubernetes（上云） |


---

## 二、功能模块总览

| 编号 | 模块 | 功能点数 | 优先级 |
|------|------|---------|--------|
| M1 | 用户与认证 | 6 | P0 |
| M2 | 项目管理 | 6 | P0 |
| M3 | 小说管理（手动上传） | 4 | P0 |
| M4 | AI 小说生成 Agent（新增·多智能体） | 15 | P0 |
| M5 | 故事线管理 | 4 | P0 |
| M6 | 大纲管理 | 6 | P0 |
| M7 | 大纲故事线 Agent（多智能体） | 5 | P0 |
| M8 | 资产管理 | 7 | P0 |
| M9 | 剧本管理 | 4 | P1 |
| M10 | 分镜管理 | 8 | P1 |
| M11 | 分镜 Agent（多智能体） | 5 | P1 |
| M12 | 图片生成 | 5 | P1 |
| M13 | 视频生成 | 11+3 | P1 |
| M14 | AI 模型配置 | 6 | P0 |
| M15 | Prompt 模板管理 | 3 | P0 |
| M16 | 任务管理 | 3 | P2 |
| M17 | 系统设置 | 4 | P2 |
| M18 | 小说导出 | 3 | P1 |
| M19 | 生产监控仪表盘 | 4 | P1 |
| M20 | 全自动内容生产流水线 | 6 | P0 |
| M21 | AI 配音（TTS） | 5 | P2 |
| M22 | 素材库管理 | 4 | P2 |

---

## 三、各模块功能需求详述

---

### M1：用户与认证

#### F1.1 用户登录
- 输入：用户名 + 密码
- 验证码校验（图形验证码）
- 成功后返回 JWT Token
- Token 中包含 userId，后续所有请求携带 Token

#### F1.2 Token 鉴权
- 除登录接口外，所有 API 需校验 JWT Token
- Token 密钥存储在 t_setting.tokenKey 中
- Token 无效或过期返回 401

#### F1.3 获取当前用户信息
- 根据 Token 解析 userId，返回用户基本信息

#### F1.4 密码安全
- 密码使用 BCrypt 加密存储（原项目明文，Java 版改进）

#### F1.5 用户管理（管理员）
- 创建/禁用/删除用户
- 分配角色（超级管理员/管理员/创作者/查看者）
- 配置用户配额（每日生成上限）

#### F1.6 配额管理
- 每个用户有每日生成配额（章节数/图片数/视频数）
- 配额每日零点自动重置
- 超出配额时返回 429 Too Many Requests
- 管理员可调整任意用户的配额

---

### M2：项目管理

项目是整个系统的顶层容器，所有小说、大纲、剧本、资产、视频都挂在项目下。

#### F2.1 创建项目
- 必填：项目名称(name)
- 可选：简介(intro)、类型(type)、画风(artStyle)、视频画幅比(videoRatio)
- type 示例：都市、玄幻、科幻、言情、悬疑、无限流、系统流...
- artStyle 示例：CG、二次元、水墨、写实、赛博朋克...
- videoRatio 示例：16:9、9:16、1:1、4:3
- 自动记录 createTime 和 userId

#### F2.2 项目列表
- 分页查询当前用户的项目
- 返回项目基本信息 + 统计数据（章节数、大纲集数等）

#### F2.3 项目详情
- 根据项目 ID 获取完整项目信息

#### F2.4 更新项目
- 可更新 name、intro、type、artStyle、videoRatio

#### F2.5 删除项目
- 级联删除项目下所有关联数据（小说、故事线、大纲、剧本、资产、图片、视频等）
- 需二次确认

#### F2.6 项目统计
- 返回用户的项目总数

---

### M3：小说管理（手动上传）

小说按章节存储，每章独立一条记录，chapterIndex 排序。

#### F3.1 添加章节
- 输入：projectId、chapterIndex（章节序号）、reel（分卷名，可选）、chapter（章节名）、chapterData（章节全文）、volumeIndex（卷序号，可选，AI生成时自动填充）
- 支持批量添加多个章节
- chapterData 存储原文全文，无字数限制
- summary 字段：章节摘要，AI 生成时自动填充，手动上传时可选填

#### F3.2 获取章节列表
- 根据 projectId 查询所有章节
- 按 chapterIndex 升序排列
- 返回：id、chapterIndex、reel、chapter（不含全文，减少传输量）

#### F3.3 更新章节
- 可更新 chapter（章节名）、chapterData（全文）、reel（分卷）

#### F3.4 删除章节
- 根据章节 ID 删除

---

### M4：AI 小说生成 Agent（新增核心功能·多智能体协作系统）

基于 Qwen3.5 等大模型，采用多 Agent 协作架构，支持日更万字级别的小说自动生成。覆盖无限流、系统流、都市、玄幻等热门网文品类。

生成层次：**大纲 → 卷 → 章 → 章概要 → 章正文**，每一层由专职 Agent 负责，层层递进，确保长篇小说的结构完整性和内容连贯性。

#### F4.1 Agent 架构

```
NovelMainAgent（总编/调度器）
│
├── WorldArchitect（世界架构师）
│   - 职责：构建世界观、力量体系、社会规则
│   - 可用工具：saveWorldSetting、getWorldSetting、updateWorldSetting
│   - 输出：结构化世界观 JSON → t_novel_world
│
├── CharacterDesigner（角色设计师）
│   - 职责：设计主角、配角、反派的完整人设
│   - 可用工具：saveCharacter、getCharacters、updateCharacter、deleteCharacter、getWorldSetting
│   - 输出：角色档案 JSON → t_novel_character
│
├── PlotArchitect（情节架构师）
│   - 职责：规划全书大纲、分卷结构、各卷主线
│   - 可用工具：saveNovelOutline、getNovelOutline、updateNovelOutline、getWorldSetting、getCharacters
│   - 输出：大纲+分卷结构 JSON → t_novel_outline
│
├── ChapterPlanner（章节规划师）
│   - 职责：在卷结构下规划每章的概要（标题、核心事件、情绪节奏、伏笔、悬念）
│   - 可用工具：saveChapterSummary、getChapterSummaries、updateChapterSummary、getNovelOutline、getCharacters、getWorldSetting
│   - 输出：章概要 JSON → t_novel_chapter_plan
│
├── NovelWriter（小说写手）
│   - 职责：根据章概要 + 上下文，逐章生成正文（流式输出）
│   - 可用工具：getChapterPlan、getPreviousChapters、getChapterSummaries、getCharacters、getWorldSetting、saveChapter
│   - 输出：章节正文 TEXT → t_novel（与手动上传格式一致）
│
├── Editor（总编审）
│   - 职责：审核任意层级的产出质量，可打回修改
│   - 审核维度：角色一致性、情节连贯性、伏笔回收、节奏合理性、品类爽点密度
│   - 可用工具：所有读取工具 + 所有更新工具
│   - 输出：审核意见 + 直接修改
│
└── QualityInspector（质检官）— 新增
    - 职责：对已生成的小说内容进行多维度深度质检，输出量化评分和具体问题清单
    - 与 Editor 的区别：Editor 是流水线内的轻量审核（通过/打回），QualityInspector 是独立的深度检测，可随时触发
    - 质检维度（7 大维度，每维度 0-100 分）：
      ① 角色一致性：姓名/外貌/性格/能力在全文中是否前后矛盾
      ② 情节连贯性：时间线是否合理、因果链是否断裂、场景转换是否突兀
      ③ 世界观合规性：正文是否违反已设定的世界观规则和禁忌
      ④ 伏笔完整性：已埋设的伏笔是否回收、回收是否合理
      ⑤ 品类爽点密度：是否符合该品类的节奏要求（如系统流每 N 章要有升级、无限流副本切换节奏）
      ⑥ 文笔质量：对话自然度、描写生动性、节奏感、避免重复用词/句式
      ⑦ 可读性：章节钩子强度、悬念设置、读者代入感
    - 可用工具：所有读取工具 + saveQualityReport、getQualityReport
    - 输出：质检报告 JSON → t_novel_quality_report
    - 支持三种质检模式：
      a. 单章质检：检查指定章节
      b. 卷质检：检查整卷的连贯性和节奏
      c. 全书质检：全局扫描，重点检查跨卷一致性
```

#### F4.2 NovelMainAgent 工具集

MainAgent 持有所有工具，包括 7 个 Sub-Agent 作为"工具"：

| 工具名 | 类型 | 描述 |
|--------|------|------|
| WorldArchitect | Sub-Agent | 调用世界架构师，传入创作需求 |
| CharacterDesigner | Sub-Agent | 调用角色设计师，传入角色需求 |
| PlotArchitect | Sub-Agent | 调用情节架构师，传入大纲需求 |
| ChapterPlanner | Sub-Agent | 调用章节规划师，传入分章需求 |
| NovelWriter | Sub-Agent | 调用小说写手，传入写作任务 |
| Editor | Sub-Agent | 调用总编审，传入审核任务 |
| QualityInspector | Sub-Agent | 调用质检官，传入质检任务（单章/卷/全书） |
| getWorldSetting | 数据工具 | 获取世界观设定 |
| saveWorldSetting | 数据工具 | 保存世界观设定 |
| updateWorldSetting | 数据工具 | 更新世界观设定 |
| getCharacters | 数据工具 | 获取所有角色档案 |
| saveCharacter | 数据工具 | 保存角色档案 |
| updateCharacter | 数据工具 | 更新角色档案 |
| deleteCharacter | 数据工具 | 删除角色 |
| getNovelOutline | 数据工具 | 获取全书大纲（含分卷结构） |
| saveNovelOutline | 数据工具 | 保存全书大纲 |
| updateNovelOutline | 数据工具 | 更新大纲/卷结构 |
| getChapterSummaries | 数据工具 | 获取章概要列表（支持按卷筛选） |
| saveChapterSummary | 数据工具 | 保存章概要 |
| updateChapterSummary | 数据工具 | 更新章概要 |
| getChapter | 数据工具 | 获取已生成的章节正文（支持批量） |
| saveChapter | 数据工具 | 保存章节正文到 t_novel，自动触发摘要生成和角色状态更新 |
| updateCharacterState | 数据工具 | 更新角色当前状态快照（位置、伤势、情绪、已知信息等） |
| getActiveForeshadowing | 数据工具 | 获取当前未回收的伏笔清单 |
| getGenerationProgress | 数据工具 | 获取当前生成进度 |
| saveQualityReport | 数据工具 | 保存质检报告到 t_novel_quality_report |
| getQualityReport | 数据工具 | 获取质检报告（支持按章/卷/全书筛选） |
| getQualityHistory | 数据工具 | 获取历次质检记录 |

#### F4.3 创作参数（用户输入）

用户通过对话或表单提供创作参数，MainAgent 据此调度各 Sub-Agent：

| 参数 | 必填 | 说明 |
|------|------|------|
| projectId | 是 | 关联项目 |
| genre | 是 | 题材：无限流、系统流、都市、玄幻、科幻、悬疑推理、言情、末日生存、历史架空、游戏竞技 |
| prompt | 是 | 创作提示，自由描述故事方向 |
| style | 否 | 风格：轻松搞笑、热血燃向、暗黑压抑、温馨治愈、悬疑烧脑 |
| volumeCount | 否 | 目标卷数，默认 3，范围 1-20 |
| chaptersPerVolume | 否 | 每卷章节数，默认 10，范围 3-50 |
| wordsPerChapter | 否 | 每章目标字数，默认 3000，范围 1000-10000 |
| protagonistSetting | 否 | 主角设定 JSON（name、gender、age、personality、background） |
| worldSetting | 否 | 世界观描述（自由文本） |
| toneKeywords | 否 | 基调关键词，如 ["反转", "打脸", "升级", "金手指"] |

#### F4.4 生成流水线（5 层递进）

用户发送创作需求后，MainAgent 按以下顺序调度 Sub-Agent，每层产出都经过 Editor 审核后才进入下一层：

```
Layer 1：世界观构建
  MainAgent → WorldArchitect → 生成世界观 → Editor 审核 → 通过/打回修改
  产出：t_novel_world（世界背景、力量体系、社会结构、核心规则、禁忌设定）

Layer 2：角色设计
  MainAgent → CharacterDesigner → 设计角色群像 → Editor 审核 → 通过/打回修改
  产出：t_novel_character（主角+配角+反派，每人含姓名、外貌、性格、能力、关系网、成长弧线）

Layer 3：全书大纲 + 分卷
  MainAgent → PlotArchitect → 规划大纲和分卷 → Editor 审核 → 通过/打回修改
  产出：t_novel_outline
    ├── 全书主线：核心冲突、终极目标、主题
    ├── 卷 1：卷名、卷主线、起止章节、卷高潮、卷悬念
    ├── 卷 2：...
    └── 卷 N：...

Layer 4：章概要
  MainAgent → ChapterPlanner → 逐卷规划章概要 → Editor 审核 → 通过/打回修改
  产出：t_novel_chapter_plan（每章一条）
    每章概要包含：
    - chapterIndex：全书章节序号
    - volumeIndex：所属卷序号
    - title：章节标题
    - summary：200-500字情节概要
    - keyEvents：本章核心事件列表
    - characters：本章出场角色
    - emotionCurve：情绪节奏（如 "平静→紧张→爆发→释然"）
    - foreshadowing：本章埋设的伏笔
    - payoff：本章回收的伏笔
    - cliffhanger：章末悬念/钩子
    - wordTarget：目标字数

Layer 5：逐章正文生成
  MainAgent → NovelWriter → 逐章生成正文（流式输出）
  每章生成前，按 F4.5 上下文管理策略自动组装 Prompt：
    - Layer A 固定记忆：世界观摘要 + 力量体系 + 卷大纲 + 当前章概要
    - Layer B 角色记忆：本章出场角色档案 + 当前状态快照 + 对话风格
    - Layer C 短期记忆：前 2 章正文全文（保持文风衔接）
    - Layer D 中长期记忆：本卷已写章摘要 + 前卷卷摘要 + 活跃伏笔清单
  每章生成完毕后：
    - 自动生成结构化章节摘要（情节、角色变化、新伏笔、回收伏笔、世界状态变更、角色位置、时间线标记）
    - 自动更新出场角色的 currentState 快照
    - 自动更新伏笔追踪状态（新埋设 / 已回收）
    - 存入 t_novel 表（chapterIndex、reel=卷名、chapter=章节名、chapterData=正文、summary=结构化摘要JSON）
    - WebSocket 推送进度
  可选：每章生成后 Editor 快速审核（可配置跳过以提高速度）
```

#### F4.5 长篇上下文管理策略

长篇小说（几十万字、上百角色、跨几十卷）的核心挑战是：LLM 单次调用的上下文窗口有限，不可能把全文塞进去。系统采用「分层压缩 + 动态召回」策略，让 NovelWriter 在任何一章都能拿到足够且精准的上下文，写出前后连贯、不崩不矛盾的内容。

**a. 四层记忆体系**

```
┌─────────────────────────────────────────────────────┐
│  Layer A：固定记忆（每次必带，压缩后常驻）              │
│  - 世界观摘要（从 t_novel_world 提取核心规则，≤2000字） │
│  - 力量体系等级表（结构化数据，≤500字）                 │
│  - 全书主线一句话概括（≤200字）                        │
│  - 当前卷的卷大纲（≤1000字）                          │
│  - 当前章概要（≤500字，来自 t_novel_chapter_plan）      │
├─────────────────────────────────────────────────────┤
│  Layer B：角色记忆（动态召回，只带本章相关角色）          │
│  - 根据当前章概要的 characters 字段，召回对应角色档案    │
│  - 每个角色档案包含：外貌、性格、能力、关系网、当前状态   │
│  - 非出场角色不加载，避免浪费上下文                      │
│  - 角色状态快照：记录角色在当前章时间点的最新状态          │
│    （位置、情绪、能力等级、持有物品、已知信息）           │
├─────────────────────────────────────────────────────┤
│  Layer C：短期记忆（最近 N 章正文，保证文风衔接）         │
│  - 前 2 章正文全文（默认，可配置 1-5 章）               │
│  - 保证对话风格、叙事节奏、场景衔接的连续性              │
├─────────────────────────────────────────────────────┤
│  Layer D：中长期记忆（压缩摘要，保证情节连贯）           │
│  - 本卷已写章节的摘要列表（每章 100-200 字摘要）         │
│  - 前面所有卷的卷摘要（每卷 300-500 字）                │
│  - 活跃伏笔清单：当前未回收的伏笔列表                   │
│    （来源章、伏笔内容、计划回收章）                      │
│  - 角色关系变更日志：只保留最近的关系变化事件             │
└─────────────────────────────────────────────────────┘
```

**b. 章节摘要自动生成**

每章正文生成完毕后，系统自动调用 LLM 生成结构化摘要，存入 t_novel.summary：

```json
{
  "plot": "本章核心情节（100字内）",
  "characterChanges": [
    {"name": "陈默", "change": "时间能力突破到凝源者，左眼疤痕开始隐隐发光"},
    {"name": "林晚", "change": "发现陈默隐瞒了觉醒的事，产生信任裂痕"}
  ],
  "newForeshadowing": [
    {"id": "fs-012", "content": "裂缝中闪过的人影与沈墨的身形相似", "plantedAt": 12}
  ],
  "resolvedForeshadowing": [
    {"id": "fs-003", "resolvedAt": 12, "resolution": "神秘徽章被鉴定为暗影组织的通行令牌"}
  ],
  "worldStateChanges": ["异空间裂缝扩张速度加快，联邦发布二级警报"],
  "locationEnd": {"陈默": "猎人公会总部", "林晚": "医疗区"},
  "timelineMarker": "大崩塌第7年·秋·第3周"
}
```

**c. 伏笔追踪机制**

系统维护一张全局伏笔追踪表（内存 + 持久化到 t_novel_chapter_plan 的 foreshadowing/payoff 字段）：

- 每章生成后，从摘要中提取新埋设的伏笔，加入追踪列表
- 每章生成后，检查是否有伏笔被回收，更新追踪状态
- ChapterPlanner 规划章概要时，会参考「未回收伏笔清单」，主动安排回收时机
- QualityInspector 质检时，检查伏笔回收率，超过 N 章未回收的伏笔标记为 warning
- 伏笔状态：planted（已埋设）→ hinted（已暗示/推进）→ resolved（已回收）→ abandoned（已放弃，需标注原因）

**d. 角色状态快照**

解决「角色不该知道的事情他知道了」「角色明明受伤了下一章生龙活虎」等问题：

- 每章生成后，从摘要的 characterChanges 中更新角色的「当前状态快照」
- 状态快照存储在 t_novel_character 的 currentState(JSON) 字段中：

```json
{
  "location": "猎人公会总部",
  "physicalState": "左臂轻伤，已包扎",
  "emotionalState": "警惕、压抑怒火",
  "powerLevel": "凝源者·中期",
  "inventory": ["暗影徽章", "C级源晶×3"],
  "knownInfo": ["沈墨还活着", "暗影组织在渗透公会"],
  "unknownInfo": ["林晚的真实身份", "源力起源的秘密"],
  "lastUpdatedChapter": 12
}
```

- NovelWriter 生成正文时，角色的行为、对话、能力表现必须与当前状态快照一致
- 角色不能使用 unknownInfo 中的信息，不能无视 physicalState 的限制

**e. 角色对话风格分化**

确保每个角色说话有辨识度，不会「所有人说话一个味」：

- t_novel_character 增加 speechStyle(JSON) 字段：

```json
{
  "tone": "冷淡、简短",
  "habits": ["很少用感叹号", "喜欢用反问句", "紧张时会沉默而非废话"],
  "vocabulary": ["口头禅：'无所谓'", "从不说脏话", "用词偏书面"],
  "exampleDialogues": [
    "「走。」（典型的一字回复）",
    "「你觉得呢？」（标志性反问）",
    "「……」（沉默代替回答）"
  ]
}
```

- NovelWriter 的 Prompt 中会注入当前出场角色的 speechStyle
- Editor 审核时会检查对话是否符合角色语言风格

**f. 上下文组装流程（每章生成前自动执行）**

```
1. 读取当前章概要 → 提取出场角色列表
2. 召回出场角色档案 + 当前状态快照 + 对话风格     ← Layer B
3. 加载世界观摘要 + 力量体系 + 卷大纲 + 章概要    ← Layer A
4. 加载前 2 章正文全文                            ← Layer C
5. 加载本卷已写章摘要 + 前卷卷摘要 + 活跃伏笔清单  ← Layer D
6. 将结构化数据转为 TOON 格式，组装为 Prompt       ← 省 token
7. 送入 NovelWriter
```

**g. TOON 格式传输（省 token 30-60%）**

所有传给 LLM 的结构化数据（角色档案、章概要、伏笔清单、世界观规则等）统一使用 [TOON（Token-Oriented Object Notation）](https://toonformat.dev) 格式代替 JSON。TOON 是专为 LLM 设计的数据序列化格式，与 JSON 数据模型完全兼容，但通过去除大括号、引号、逗号等冗余符号，节省 30-60% 的 token 消耗。

TOON 示例 — 角色档案传入 LLM 时的格式：

```toon
characters[3]{name,role,powerLevel,location,physicalState,emotionalState,speechTone}:
  陈默,protagonist,凝源者·中期,猎人公会总部,左臂轻伤,警惕压抑,冷淡简短
  林晚,supporting,凝源者·初期,医疗区,健康,担忧焦虑,温柔关切
  赵铁柱,supporting,觉醒者·后期,猎人公会总部,健康,兴奋紧张,大大咧咧
```

等价 JSON 需要约 2 倍 token。对于长篇小说生成场景（每章都要带大量上下文），TOON 格式能显著降低上下文占用，留更多空间给正文生成。

适用范围：
- 角色档案列表 → TOON 表格格式
- 章概要列表 → TOON 表格格式
- 伏笔追踪清单 → TOON 表格格式
- 世界观规则/禁忌 → TOON 对象格式
- 角色关系网 → TOON 对象格式
- 正文内容（前 N 章全文）→ 保持纯文本，不转 TOON

技术实现：
- Java 后端使用 TOON 官方 TypeScript 实现的 Java 移植版，或自行实现 JSON→TOON 转换器
- 转换在上下文组装阶段自动完成，对 Agent 和用户透明
- LLM 输出仍使用 JSON（结构化输出场景）或纯文本（正文生成场景），不要求 LLM 输出 TOON

#### F4.6 WebSocket 通信协议

连接端点：`/ws/agent/novel/{projectId}`

**前端 → 后端消息：**

| type | data | 说明 |
|------|------|------|
| msg | {type: "user", data: "消息"} | 发送创作需求或修改指令 |
| startGeneration | {genre, prompt, style, ...} | 启动完整生成流水线 |
| regenerateChapter | {chapterIndex, instruction} | 重新生成指定章节 |
| continueWriting | {fromChapter} | 从指定章节续写 |
| polishChapter | {chapterIndex, direction} | 润色指定章节 |
| cleanHistory | 无 | 清空对话历史 |

**后端 → 前端消息：**

| type | data | 说明 |
|------|------|------|
| init | {projectId} | 连接初始化完成 |
| stream | text | MainAgent 流式文本输出 |
| response_end | text | MainAgent 完整响应结束 |
| subAgentStream | {agent, text} | Sub-Agent 流式输出 |
| subAgentEnd | {agent} | Sub-Agent 完成 |
| toolCall | {agent, name, args} | 工具调用通知 |
| transfer | {to} | Agent 切换通知（如 "正在调用世界架构师..."） |
| refresh | type | 数据刷新通知（world/character/outline/chapterPlan/chapter） |
| layerStart | {layer, name} | 开始某一层（如 "Layer 1: 世界观构建"） |
| layerComplete | {layer, name} | 某一层完成 |
| reviewStart | {layer, target} | Editor 开始审核 |
| reviewResult | {layer, passed, feedback} | 审核结果（通过/打回+意见） |
| qualityCheckStart | {scope, scopeIndex} | 开始质检 |
| qualityCheckProgress | {dimension, status} | 正在检查某个维度 |
| qualityCheckComplete | {reportId, overallScore, summary} | 质检完成 |
| autoFixStart | {issueCount} | 开始自动修复 |
| autoFixProgress | {fixed, total, currentIssue} | 修复进度 |
| autoFixComplete | {fixedCount, newScore} | 修复完成 |
| chapterStart | {volumeIndex, chapterIndex, title} | 开始生成第 N 章 |
| chapterDelta | text | 章节正文流式增量 |
| chapterEnd | {chapterIndex, wordCount} | 第 N 章生成完毕 |
| progress | {totalChapters, completedChapters, currentChapter, currentLayer} | 整体进度 |
| allComplete | {totalWords, totalChapters, totalVolumes} | 全部生成完毕 |
| error | message | 错误信息 |

#### F4.7 对话式交互

除了一键启动完整流水线，用户也可以通过自然语言对话逐步引导生成：

- "帮我构建一个末日废土的世界观，有变异兽和觉醒者"
  → MainAgent 调用 WorldArchitect
- "主角叫陈默，25岁，性格冷漠但内心善良，能力是时间减速"
  → MainAgent 调用 CharacterDesigner
- "再加一个反派，是主角的发小，黑化了"
  → MainAgent 调用 CharacterDesigner（追加角色）
- "规划一下前三卷的大纲"
  → MainAgent 调用 PlotArchitect
- "第一卷第3章的概要太平淡了，加点反转"
  → MainAgent 调用 ChapterPlanner（更新指定章概要）
- "开始写第一卷"
  → MainAgent 调用 NovelWriter（逐章生成）
- "第5章的打斗写得不够燃，重写"
  → MainAgent 调用 NovelWriter（重新生成指定章）
- "让总编审检查一下前三章的质量"
  → MainAgent 调用 Editor
- "质检一下第5章"
  → MainAgent 调用 QualityInspector（单章质检）
- "第一卷写完了，做个全面质检"
  → MainAgent 调用 QualityInspector（卷质检，输出 7 维度评分 + 问题清单）
- "自动修复质检报告里的所有问题"
  → MainAgent 调用 NovelWriter，根据 autoFixSuggestions 逐项修改
- "做一次全书质检，重点看角色一致性"
  → MainAgent 调用 QualityInspector（全书质检，侧重角色维度）

这种对话式交互和 M7 大纲故事线 Agent 的体验完全一致：用户发消息，MainAgent 决定调用哪个 Sub-Agent，Sub-Agent 自行调用工具完成任务，全程流式输出。

#### F4.8 单章重新生成
- 对已生成的某一章不满意，可以重新生成
- 重新生成时保持前后章节的上下文一致性（读取前后章正文+概要）
- 支持用户附加修改指令，如 "这章的打斗场面要更激烈"、"把对话改成更幽默的风格"
- 重新生成后自动更新本章摘要

#### F4.9 续写章节
- 在已有章节基础上继续生成新章节
- 如果当前卷的章概要已用完，ChapterPlanner 自动规划新的章概要
- 如果当前卷已写完，PlotArchitect 自动规划下一卷
- 自动衔接上文，保持风格和情节连贯

#### F4.10 章节润色/改写
- 对已生成的章节进行 AI 润色
- 支持指定修改方向：加强对话、增加描写、调整节奏、修改结局、增加爽点等
- 润色后更新 t_novel 和章节摘要

#### F4.11 品类特化 Prompt 模板
- 不同品类有不同的生成策略和 Prompt 模板
- 存储在 t_prompts 表中，code 前缀为 novel-gen-
- 品类模板影响所有层级（世界观构建风格、角色设计偏好、情节节奏、正文文风）
- 示例：
  - novel-gen-system-flow：系统流专用，强调升级节奏、系统面板描写、任务触发、数据化战斗
  - novel-gen-infinite-flow：无限流专用，强调副本切换、规则解读、团队配合、生死抉择
  - novel-gen-urban：都市流专用，强调打脸爽感、身份反转、商战博弈、装逼打脸节奏
  - novel-gen-xuanhuan：玄幻专用，强调修炼体系、战斗描写、境界突破、天材地宝
- 用户可自定义覆盖默认模板

#### F4.12 系统级写作质量指令

所有 NovelWriter 的 System Prompt 中强制注入以下写作质量约束，不可被用户覆盖：

**a. 去 AI 味指令**
- 禁止使用以下 AI 典型句式/词汇：
  - "值得注意的是"、"需要指出的是"、"总而言之"、"综上所述"
  - "在这个充满XXX的世界里"、"命运的齿轮开始转动"
  - "他/她不禁XXX"（过度使用）
  - "仿佛"、"宛如"、"犹如"连续出现超过 2 次/千字
  - 排比句连续超过 3 组
  - 每段开头用相同句式
- 禁止"总结式"段落（AI 喜欢在每段末尾总结升华）
- 禁止过度使用心理独白解释角色动机（show, don't tell）
- 对话要自然口语化，不能像书面报告
- 避免所有角色说话风格趋同（配合 speechStyle 机制）

**b. 高质量写作标准**
- 叙事节奏：动作场景短句快节奏，情感场景长句慢节奏，张弛有度
- 感官描写：每个重要场景至少覆盖 2 种以上感官（视觉、听觉、触觉、嗅觉、味觉）
- 冲突密度：每章至少 1 个明确冲突（可以是外部冲突或内心冲突）
- 钩子强度：每章结尾必须有悬念或情绪钩子，让读者想看下一章
- 对话推动情节：对话不能是废话，每段对话要么推进情节、要么揭示角色、要么制造冲突
- 环境不是背景板：环境描写要与角色情绪或情节发展呼应
- 避免信息倾倒（info dump）：世界观设定通过情节自然展现，不要大段解说

**c. 网文特化标准**
- 爽点节奏：根据品类要求保证爽点密度（系统流每 2-3 章一次升级/奖励，都市流每章至少一次打脸/反转）
- 代入感：第一人称或强代入的第三人称，读者能代入主角视角
- 章末钩子分级：S级（重大反转/生死悬念）、A级（新信息揭露/关系变化）、B级（小悬念/伏笔暗示），每卷至少 2 个 S 级钩子
- 金手指克制：主角能力强但不无敌，每次使用有代价或限制
- 配角有记忆点：重要配角至少有 1 个标志性特征（口头禅、习惯动作、外貌特征）

#### F4.13 生成进度查询（REST API）
- 根据 projectId 查询当前生成状态
- 返回：
  - currentLayer：当前所在层级（world/character/outline/chapterPlan/chapter）
  - totalVolumes / completedVolumes：卷进度
  - totalChapters / completedChapters：章进度
  - currentChapter：当前正在生成的章节信息
  - totalWords：已生成总字数
  - state：pending / generating / reviewing / completed / failed

#### F4.14 对话历史管理
- 对话历史存储在 t_chat_history（type="novelAgent"）
- WebSocket 断开时自动保存
- 支持清空历史重新开始
- 历史中包含所有 Agent 的交互记录

#### F4.15 AI 质检系统

独立于生成流水线的深度质量检测机制，可在任意时机触发。

**三种质检模式：**

**a. 单章质检**
- 触发方式：用户指定章节，或每章生成后自动触发（可配置）
- 质检官读取：当前章正文 + 章概要 + 角色档案 + 世界观
- 检查项：
  - 角色描写是否与档案一致（外貌、性格、说话风格）
  - 是否违反世界观规则（如角色使用了不该有的能力）
  - 章概要中的核心事件是否都体现在正文中
  - 伏笔是否按计划埋设
  - 章末钩子是否足够吸引人
  - 文笔：是否有重复句式、口水话、逻辑不通的对话
- 输出：单章质检报告（7 维度评分 + 具体问题列表 + 修改建议）

**b. 卷质检**
- 触发方式：一卷写完后自动触发，或用户手动触发
- 质检官读取：整卷所有章节摘要 + 关键章节正文 + 卷大纲 + 角色档案
- 额外检查项（在单章基础上增加）：
  - 卷内情节节奏曲线是否合理（不能一直高潮或一直平淡）
  - 角色成长弧线是否体现
  - 卷高潮是否足够震撼
  - 卷末悬念是否能勾住读者
  - 品类爽点密度：如系统流是否每 2-3 章有一次升级/奖励，无限流副本内是否有足够的危机感
- 输出：卷质检报告

**c. 全书质检**
- 触发方式：全书写完后，或用户手动触发
- 质检官读取：全部卷摘要 + 全部章节摘要 + 角色档案 + 世界观 + 抽样章节正文
- 额外检查项（在卷质检基础上增加）：
  - 跨卷角色一致性（第 1 卷和第 5 卷的同一角色是否矛盾）
  - 全书伏笔回收率（埋了多少伏笔、回收了多少、遗漏了哪些）
  - 主线推进节奏（是否有拖沓或跳跃）
  - 全书情绪曲线（是否有足够的起伏）
  - 角色关系网演变是否合理
- 输出：全书质检报告

**质检报告结构（t_novel_quality_report）：**

```json
{
  "id": 1,
  "projectId": 100,
  "scope": "chapter",
  "scopeIndex": 5,
  "overallScore": 82,
  "dimensions": {
    "characterConsistency": {
      "score": 90,
      "issues": [
        {
          "severity": "warning",
          "location": "第5章第3段",
          "description": "陈默被描述为'笑着说'，但角色设定为冷漠寡言，此处情绪表达偏离人设",
          "suggestion": "改为'嘴角微微上扬'或'淡淡地说'，更符合角色性格"
        }
      ]
    },
    "plotCoherence": {
      "score": 85,
      "issues": [
        {
          "severity": "error",
          "location": "第5章开头",
          "description": "第4章末尾主角在A城，第5章开头直接出现在B城，缺少转场",
          "suggestion": "在章首补充一段简短的赶路/传送描写"
        }
      ]
    },
    "worldCompliance": {
      "score": 95,
      "issues": []
    },
    "foreshadowIntegrity": {
      "score": 70,
      "issues": [
        {
          "severity": "info",
          "location": "第3章",
          "description": "第3章埋设的'神秘徽章'伏笔，截至第5章尚未有任何呼应",
          "suggestion": "建议在第6-8章安排一次与徽章相关的情节推进"
        }
      ]
    },
    "genreSatisfaction": {
      "score": 78,
      "issues": [
        {
          "severity": "warning",
          "location": "第3-5章",
          "description": "连续3章没有系统升级/奖励事件，系统流读者可能感到乏味",
          "suggestion": "在第5章末或第6章初安排一次系统任务完成奖励"
        }
      ]
    },
    "writingQuality": {
      "score": 80,
      "issues": [
        {
          "severity": "warning",
          "location": "第5章",
          "description": "'他看着她，她看着他'类似句式在本章出现3次",
          "suggestion": "变换描写方式，如用动作、神态、环境烘托代替直白的对视描写"
        }
      ]
    },
    "readability": {
      "score": 85,
      "issues": []
    }
  },
  "summary": "第5章整体质量良好(82分)。主要问题：角色情绪表达有1处偏离人设，场景转换缺少过渡，系统流爽点密度偏低。建议重点修改场景转场和补充系统奖励情节。",
  "autoFixSuggestions": [
    {
      "chapterIndex": 5,
      "type": "rewrite_paragraph",
      "location": "第3段",
      "original": "陈默笑着说：'没什么大不了的。'",
      "suggested": "陈默面无表情，语气平淡得像在说今天的天气：'没什么大不了的。'"
    }
  ],
  "createTime": "2026-02-24T10:30:00"
}
```

**质检触发方式：**
- 对话触发："帮我质检一下第5章" / "检查一下第一卷的质量" / "做一次全书质检"
- 自动触发（可配置）：
  - 每章生成后自动单章质检（默认关闭，开启会降低生成速度）
  - 每卷完成后自动卷质检（默认开启）
  - 全书完成后自动全书质检（默认开启）
- REST API 触发：POST /api/projects/{projectId}/novel/quality-check

**质检后的修复流程：**
1. 质检报告生成后，通过 WebSocket 推送给前端
2. 用户查看报告，可选择：
   a. 一键自动修复：MainAgent 调用 NovelWriter 根据 autoFixSuggestions 逐项修改
   b. 选择性修复：用户勾选要修复的问题，只修改选中项
   c. 忽略：标记问题为"已忽略"
3. 修复完成后可再次质检，对比前后评分变化

**WebSocket 质检相关消息：**

| type | data | 说明 |
|------|------|------|
| qualityCheckStart | {scope, scopeIndex} | 开始质检 |
| qualityCheckProgress | {dimension, status} | 正在检查某个维度 |
| qualityCheckComplete | {reportId, overallScore, summary} | 质检完成 |
| autoFixStart | {issueCount} | 开始自动修复 |
| autoFixProgress | {fixed, total, currentIssue} | 修复进度 |
| autoFixComplete | {fixedCount, newScore} | 修复完成 |

---

### M5：故事线管理

故事线是对小说内容的高层次提炼，是生成大纲的基础。

#### F5.1 获取故事线
- 根据 projectId 获取当前项目的故事线
- 故事线是一段结构化文本，包含主线、支线、人物关系等

#### F5.2 保存/更新故事线
- 覆盖式保存，一个项目只有一条故事线
- content 字段存储故事线全文

#### F5.3 删除故事线
- 删除当前项目的故事线

#### F5.4 AI 生成故事线
- 通过大纲故事线 Agent（M7）中的 AI1（故事师）自动生成
- 故事师读取小说原文，分析后生成结构化故事线

---

### M6：大纲管理

大纲是分集的结构化数据，每集一条记录，data 字段存储 EpisodeData JSON。

#### F6.1 获取大纲列表
- 根据 projectId 查询所有大纲，按 episode 升序
- 支持简化模式（只返回集数和标题）和完整模式

#### F6.2 添加大纲
- 手动添加单集大纲
- 自动分配 episode 序号

#### F6.3 更新大纲
- 根据大纲 ID 更新 EpisodeData
- EpisodeData 结构：

```json
{
  "episodeIndex": 1,
  "title": "8字内标题",
  "chapterRange": [1, 2, 3],
  "scenes": [{"name": "场景名", "description": "环境描写"}],
  "characters": [{"name": "角色名", "description": "人设样貌"}],
  "props": [{"name": "道具名", "description": "样式描写"}],
  "coreConflict": "核心矛盾：A想要X vs B阻碍X",
  "outline": "100-300字剧情主干（最高优先级）",
  "openingHook": "开场镜头描述",
  "keyEvents": ["起", "承", "转", "合"],
  "emotionalCurve": "2(压抑)→5(反抗)→9(爆发)→3(余波)",
  "visualHighlights": ["标志性镜头1", "镜头2", "镜头3"],
  "endingHook": "结尾悬念",
  "classicQuotes": ["金句1", "金句2"]
}
```

#### F6.4 删除大纲
- 支持批量删除
- 级联删除关联的剧本记录

#### F6.5 AI 生成大纲
- 通过大纲故事线 Agent（M7）中的 AI2（大纲师）自动生成
- 支持覆盖模式（清空重写）和追加模式（在末尾追加新集）

#### F6.6 资产自动提取
- 从所有大纲的 EpisodeData 中提取 characters/props/scenes
- 按 name 去重后写入 t_assets 表
- 已存在的资产更新描述，不存在的新增


---

### M7：大纲故事线 Agent（多智能体协作系统）

这是系统的核心 AI 模块之一。采用多 Agent 协作架构，通过 WebSocket 实现实时交互。

#### F7.1 Agent 架构

```
MainAgent（协调者/调度器）
├── AI1（故事师）
│   - 职责：分析小说原文，生成故事线
│   - 可用工具：getChapter、getStoryline、saveStoryline、getOutline、saveOutline、updateOutline
│   - 输出：结构化故事线文本
│
├── AI2（大纲师）
│   - 职责：根据故事线 + 原文，生成分集大纲
│   - 可用工具：同 AI1
│   - 输出：EpisodeData[] JSON 数组
│
└── Director（导演）
    - 职责：审核故事线和大纲质量，提出修改意见
    - 可用工具：同 AI1
    - 可直接调用 updateOutline/saveStoryline 进行修改
```

#### F7.2 MainAgent 工具集

MainAgent 持有所有工具，包括三个 Sub-Agent 作为"工具"：

| 工具名 | 描述 |
|--------|------|
| AI1 | 调用故事师，传入任务描述 |
| AI2 | 调用大纲师，传入任务描述 |
| director | 调用导演，传入审核任务 |
| getChapter | 根据章节号获取小说原文（支持批量） |
| getStoryline | 获取当前故事线 |
| saveStoryline | 保存故事线 |
| deleteStoryline | 删除故事线 |
| getOutline | 获取大纲（支持简化/完整模式） |
| saveOutline | 保存大纲（支持覆盖/追加模式） |
| updateOutline | 更新单集大纲 |
| deleteOutline | 删除大纲（支持批量） |
| generateAssets | 从大纲提取资产 |

#### F7.3 WebSocket 通信协议

连接端点：`/ws/agent/outline/{projectId}`

**前端 → 后端消息：**

| type | data | 说明 |
|------|------|------|
| msg | {type: "user", data: "用户消息"} | 发送用户消息给 Agent |
| cleanHistory | 无 | 清空对话历史 |

**后端 → 前端消息：**

| type | data | 说明 |
|------|------|------|
| init | {projectId} | 连接初始化完成 |
| stream | text | MainAgent 流式文本输出 |
| response_end | text | MainAgent 完整响应结束 |
| subAgentStream | {agent, text} | Sub-Agent 流式输出 |
| subAgentEnd | {agent} | Sub-Agent 完成 |
| toolCall | {agent, name, args} | 工具调用通知 |
| transfer | {to} | Agent 切换通知 |
| refresh | type | 数据刷新通知（storyline/outline/assets） |
| error | message | 错误信息 |
| notice | message | 系统通知 |

#### F7.4 对话历史管理
- 对话历史存储在 t_chat_history 表（type="outlineAgent"）
- WebSocket 断开时自动保存
- 支持清空历史重新开始

#### F7.5 上下文构建
- 每次调用 LLM 时，自动构建完整上下文：
  - 环境信息：项目信息、章节列表、故事线状态、大纲状态
  - 对话历史
  - 当前任务描述
  - 可用工具列表
- Sub-Agent 调用时，额外注入 Sub-Agent 专属的 System Prompt（从 t_prompts 表读取）

---

### M8：资产管理

资产包括三类：角色(role)、道具(props)、场景(scene)。每个资产有名称、描述、提示词、参考图片。

#### F8.1 获取资产列表
- 根据 projectId 查询所有资产
- 支持按 type 筛选
- 返回：id、name、intro、prompt、type、filePath

#### F8.2 添加资产
- 手动添加单个资产
- 输入：name、intro、prompt、type、projectId

#### F8.3 更新资产
- 更新资产的 name、intro、prompt

#### F8.4 删除资产
- 根据资产 ID 删除

#### F8.5 保存资产（批量）
- 批量保存/更新资产数据

#### F8.6 生成资产图片
- 根据资产类型调用不同的 Prompt 模板：
  - 角色(role)：生成角色标准四视图
  - 场景(scene)：生成标准场景图
  - 道具(props)：生成标准道具图
  - 分镜(storyboard)：生成标准分镜图
- 生成流程：
  1. 读取项目的 artStyle（画风）
  2. 从 t_prompts 读取对应类型的 system prompt
  3. 构建 user prompt（包含画风 + 资产名称 + 描述提示词）
  4. 调用图片模型生成
  5. 保存图片到云 OSS
  6. 更新 t_image 记录

#### F8.7 提示词润色
- 对资产的 prompt 进行 AI 润色优化
- 使生成的图片更符合预期

---

### M9：剧本管理

每集大纲对应一个剧本，剧本是 500-800 字的短剧文本。

#### F9.1 获取剧本
- 根据 scriptId 或 projectId 查询剧本
- 返回：id、name、content、outlineId

#### F9.2 AI 生成剧本
- 输入：outlineId（大纲ID）、scriptId（剧本ID）
- 生成流程：
  1. 读取大纲的 EpisodeData
  2. 根据 chapterRange 获取对应章节原文
  3. 构建结构化提示（包含场景、角色、道具、核心矛盾、剧情主干、开场镜头、剧情节点、情绪曲线、视觉重点、结尾悬念、金句）
  4. 从 t_prompts 读取剧本生成的 system prompt
  5. 调用文本模型生成
  6. 更新 t_script.content
- 强制要求：
  - 开场镜头必须是剧本第一个镜头
  - 严格按剧情主干顺序展开
  - 剧情节点四步按顺序：起→承→转→合
  - 所有场景/角色/道具必须全部使用
  - 500-800 字
  - 以【黑屏】结尾

#### F9.3 更新剧本
- 手动编辑剧本内容

#### F9.4 保存剧本
- 保存编辑后的剧本

---

### M10：分镜管理

分镜是将剧本拆分为可视化片段，每个片段包含多个镜头。

#### F10.1 获取分镜数据
- 根据 scriptId 获取分镜列表
- 返回片段(segments)和分镜(shots)数据

#### F10.2 保存分镜
- 将 Agent 生成的分镜数据持久化到数据库

#### F10.3 保留分镜
- 标记分镜为"已确认"状态

#### F10.4 上传自定义图片
- 用户可上传自己的图片替换 AI 生成的分镜图

#### F10.5 生成分镜图片
- 输入：cells（镜头提示词数组）、scriptId、projectId
- 生成流程：
  1. 获取剧本关联的大纲，提取资产信息
  2. 获取资产的参考图片
  3. AI 筛选与当前分镜相关的资产图片
  4. 润色镜头提示词（加入画风、资产名称映射）
  5. 将资产图片作为参考，调用图片模型生成宫格图
  6. 自动分割宫格图为单张镜头图
  7. 保存并返回图片路径

#### F10.6 生成视频提示词

分镜的镜头提示词（给图片模型）和视频提示词（给视频模型）是两套不同的东西。图片提示词描述画面内容，视频提示词描述画面如何动起来。

**a. 视频提示词生成流程**
- 输入：分镜镜头提示词 + 片段描述 + 角色动作 + 情绪 + 剧本上下文
- AI 自动生成视频提示词，包含：
  - 动作描述：角色做什么动作、物体如何运动
  - 镜头运动：推/拉/摇/移/跟/升/降/旋转/固定
  - 节奏感：快速剪辑 / 慢动作 / 正常速度
  - 氛围：光影变化、天气变化、情绪渲染
- 输出存储在 t_assets 的 videoPrompt 字段（分镜类型资产）

**b. 镜头运动指令（运镜）**

分镜师生成镜头时，同时输出运镜指令：

| 运镜类型 | 英文 | 说明 | 适用场景 |
|---------|------|------|---------|
| 推 | push in / zoom in | 镜头向主体推进 | 强调细节、制造紧张感 |
| 拉 | pull out / zoom out | 镜头远离主体 | 揭示全景、制造孤独感 |
| 摇 | pan left/right | 镜头水平旋转 | 展示环境、跟随视线 |
| 移 | truck left/right | 镜头水平平移 | 跟随角色行走 |
| 升 | crane up / tilt up | 镜头向上 | 仰视、展示高大建筑 |
| 降 | crane down / tilt down | 镜头向下 | 俯视、揭示地面细节 |
| 跟 | follow / tracking | 跟随主体运动 | 追逐、行走场景 |
| 旋转 | orbit / rotate | 环绕主体旋转 | 展示角色、制造戏剧感 |
| 固定 | static / locked | 镜头不动 | 对话、静态场景 |
| 手持 | handheld | 轻微晃动 | 纪实感、紧张感 |

- 运镜指令写入 Shot 数据的 cameraMotion 字段
- 视频提示词生成时自动融合运镜指令
- 分镜师 Prompt 中注入运镜知识，根据情绪和动作自动选择合适的运镜

**c. 视频提示词模板**

不同厂商对视频提示词的格式要求不同，系统自动适配：
- 通用格式：自然语言描述（动作 + 运镜 + 氛围）
- 可灵/火山：支持 camera_control 参数，运镜指令可结构化传入
- Sora：纯自然语言，运镜描述融入提示词文本

视频提示词 Prompt 模板存储在 t_prompts（code: `video-prompt-generate`），可自定义覆盖。

#### F10.7 批量超分辨率
- 对分镜图片进行超分辨率处理，提升画质

#### F10.8 修改分镜图片
- 替换单个镜头的图片（手动上传或重新生成）
- 重新生成时可修改提示词
- 支持批量重新生成一个分镜的所有镜头图片
- 保留历史生成结果，用户可对比选择

---

### M11：分镜 Agent（多智能体协作系统）

#### F11.1 Agent 架构

```
MainAgent（协调者）
├── segmentAgent（片段师）
│   - 职责：将剧本拆分为多个片段
│   - 可用工具：getScript、getAssets、updateSegments
│   - 输出：Segment[]（每个片段包含 index、description、emotion、action）
│
└── shotAgent（分镜师）
    - 职责：为每个片段生成镜头提示词 + 运镜指令 + 视频提示词
    - 可用工具：getScript、getAssets、getSegments、addShots、updateShots、deleteShots、generateShotImage
    - 输出：Shot[]（每个分镜包含镜头图片提示词、运镜指令 cameraMotion、视频提示词 videoPrompt）
```

#### F11.2 WebSocket 通信协议

连接端点：`/ws/agent/storyboard/{projectId}?scriptId={scriptId}`

**前端 → 后端消息：**

| type | data | 说明 |
|------|------|------|
| msg | {type: "user", data: "消息"} | 用户消息 |
| cleanHistory | 无 | 清空历史 |
| replaceShot | {segmentId, cellId, cell} | 替换单个镜头 |

**后端 → 前端消息：**

| type | data | 说明 |
|------|------|------|
| init | {projectId, scriptId} | 初始化完成 |
| stream / response_end | text | MainAgent 输出 |
| subAgentStream / subAgentEnd | {agent, text} | Sub-Agent 输出 |
| toolCall | {agent, name} | 工具调用 |
| transfer | {to} | Agent 切换 |
| segmentsUpdated | Segment[] | 片段数据更新 |
| shotsUpdated | Shot[] | 分镜数据更新 |
| shotImageGenerateStart | {shotIds} | 开始生成分镜图 |
| shotImageGenerateProgress | {shotId, status, message, progress} | 生成进度 |
| shotImageGenerateComplete | {shotId, shot, imagePaths} | 生成完成 |
| shotImageGenerateError | {shotId, error} | 生成失败 |

#### F11.3 分镜图生成流程（异步）
- 分镜图生成是异步的，不阻塞 Agent 对话流程
- 每个分镜的所有镜头提示词合并生成一张宫格图
- 宫格图自动分割为单张镜头图片
- 通过 WebSocket 实时推送生成进度

#### F11.4 片段和分镜数据
- 片段和分镜数据在 Agent 会话期间存储在内存中
- 用户确认后通过"保存分镜"接口持久化到数据库

#### F11.5 对话历史
- 存储在 t_chat_history（type="storyboardAgent"）
- 断开连接时自动保存

---

### M20：全自动内容生产流水线

这是系统的终极能力：从一句创作提示开始，全自动走完「小说 → 故事线 → 大纲 → 资产 → 剧本 → 分镜 → 图片 → 视频 → 合成」全链路，产出可直接发布的短剧视频。

#### F20.1 一键全自动生产

用户只需提供最基本的输入，系统自动完成全部流程：

**输入参数：**

| 参数 | 必填 | 说明 |
|------|------|------|
| projectId | 是 | 关联项目（项目中已配置 artStyle、videoRatio） |
| genre | 是 | 题材 |
| prompt | 是 | 创作提示 |
| episodeCount | 否 | 目标集数，默认 5 |
| chaptersPerEpisode | 否 | 每集对应的章节数，默认 2 |
| videoMode | 否 | 视频生成模式，默认 singleImage |
| autoCompose | 否 | 是否自动合成长视频，默认 true |
| ttsEnabled | 否 | 是否生成 AI 配音，默认 false |

**全自动流水线：**

```
Step 1：AI 小说生成（M4 流水线）
  创作提示 → 世界观 → 角色 → 大纲 → 章概要 → 逐章正文
  ↓ 全部章节生成完毕

Step 2：故事线提取（M7）
  小说全文 → AI 故事师分析 → 生成故事线
  ↓

Step 3：分集大纲生成（M7）
  故事线 + 小说原文 → AI 大纲师 → 生成 EpisodeData[]
  → AI 导演审核 → 通过/打回修改
  ↓

Step 4：资产自动提取（M6/M8）
  大纲 → 提取角色/道具/场景 → 去重入库 t_assets
  ↓

Step 5：资产图片生成（M8/M12）
  每个资产 → 生成参考图（角色四视图/场景图/道具图）
  ↓ 所有资产图片就绪

Step 6：逐集剧本生成（M9）
  每集大纲 + 对应章节原文 → AI 生成 500-800 字剧本
  ↓

Step 7：逐集分镜生成（M11）
  剧本 → 片段师拆分 → 分镜师生成镜头提示词
  ↓

Step 8：分镜图片生成 + 质量检测（M10/M12/M13.5）
  镜头提示词 + 资产参考图 → 图片模型生成宫格图 → 自动分割
  → 图片质量检测（评分 < 60 自动重新生成）
  ↓ 所有分镜图就绪且质量达标

Step 9：AI 配音生成（M21，可选）
  剧本台词 → 角色音色匹配 → TTS 生成语音片段
  ↓

Step 10：逐分镜视频生成（M13）
  分镜图 + 视频提示词 → 视频模型异步生成（10-15s 片段）
  → 失败自动重试（最多 3 次）
  ↓ 所有视频片段就绪

Step 11：视频合成（M13.6）
  每集的所有视频片段 → 拼接 + 转场 + 字幕 + BGM + 配音 + 水印 → 单集完整视频
  ↓

完成：N 集短剧视频 + 完整小说文本 + 全部资产
```

#### F20.2 流水线状态机

全自动流水线的每一步都有明确的状态，支持暂停、跳过、重试：

```
pending → novel_generating → novel_complete
       → storyline_generating → storyline_complete
       → outline_generating → outline_reviewing → outline_complete
       → assets_extracting → assets_image_generating → assets_complete
       → script_generating → script_complete
       → storyboard_generating → storyboard_complete
       → storyboard_image_generating → image_quality_checking → storyboard_image_complete
       → tts_generating → tts_complete（可选，跳过则直接进入下一步）
       → video_generating → video_complete
       → composing → compose_complete
       → all_complete
```

任意步骤失败 → `step_failed`，可选择：重试当前步骤 / 跳过 / 终止

#### F20.3 审核节点配置

全自动不代表没有审核。用户可配置在哪些节点插入审核（人工或 AI）：

| 审核节点 | 默认 | 说明 |
|---------|------|------|
| 小说生成后 | AI 自动质检 | 全书质检，低于 70 分自动修复 |
| 故事线生成后 | 跳过 | 可开启人工审核 |
| 大纲生成后 | AI 导演审核 | 自动审核，打回自动修改 |
| 剧本生成后 | 跳过 | 可开启人工审核 |
| 分镜图生成后 | AI 图片质检 | 自动检测图片质量，低分自动重新生成 |
| 配音生成后 | 跳过 | 可开启人工试听审核 |
| 视频片段生成后 | 跳过 | 可开启人工审核（检查视频质量） |
| 合成视频后 | 跳过 | 可开启人工审核（最终成片检查） |

审核模式：
- `skip`：跳过，直接进入下一步
- `ai_auto`：AI 自动审核，通过则继续，不通过自动修改
- `human_required`：暂停流水线，等待人工确认后继续

#### F20.4 批量生产模式

支持一次提交多个生产任务，系统并行处理：

- REST API：`POST /api/batch/full-pipeline`
- 入参：生产参数数组，每个元素对应一个完整的短剧项目
- 系统自动为每个任务创建项目，按队列调度执行
- 任务之间互不阻塞，共享模型资源池
- 前端任务中心可监控所有生产任务的进度

#### F20.5 WebSocket 全流程进度推送

连接端点：`/ws/pipeline/{projectId}`

| type | data | 说明 |
|------|------|------|
| pipelineStart | {totalSteps: 11} | 流水线启动 |
| stepStart | {step, name, index} | 开始某一步 |
| stepProgress | {step, progress, detail} | 步骤内进度（如 "正在生成第3/10章"） |
| stepComplete | {step, name, duration} | 某一步完成 |
| stepFailed | {step, error, canRetry} | 某一步失败 |
| reviewRequired | {step, data} | 需要人工审核（暂停等待） |
| reviewPassed | {step} | 审核通过，继续 |
| pipelineComplete | {totalDuration, outputs} | 全部完成 |
| pipelineError | {step, error} | 流水线异常终止 |

#### F20.6 产出物清单

全自动流水线完成后，一个项目包含以下完整产出：

| 产出 | 存储位置 | 说明 |
|------|---------|------|
| 小说全文 | t_novel | N 章完整小说 |
| 世界观设定 | t_novel_world | 结构化世界观 |
| 角色档案 | t_novel_character | 含状态快照和对话风格 |
| 故事线 | t_storyline | 结构化故事线 |
| 分集大纲 | t_outline | EpisodeData[] |
| 资产列表 | t_assets | 角色/道具/场景 + 参考图 |
| 分集剧本 | t_script | 每集 500-800 字 |
| 分镜数据 | 内存 → DB | 片段 + 镜头提示词 |
| 分镜图片 | 云 OSS | 每个镜头的 AI 生成图 |
| 配音音频 | 云 OSS | 每句台词的 TTS 语音（可选） |
| 视频片段 | 云 OSS | 每个分镜的 10-15s 视频 |
| 合成视频 | 云 OSS | 每集 60-180s 完整视频（含字幕+BGM+配音+水印） |
| 质检报告 | t_novel_quality_report | 7 维度评分 |

---

### M21：AI 配音（TTS）

为短剧视频添加 AI 语音配音，支持旁白和角色对话。

#### F21.1 TTS 厂商集成

| 厂商 | 说明 |
|------|------|
| 火山引擎 | 豆包语音合成，支持多音色、情感控制 |
| 阿里云 | 通义语音合成，中文效果好 |
| 微软 Azure | Azure Speech，多语言、多音色 |
| Fish Audio | 开源 TTS，支持声音克隆 |
| 其他 | OpenAI TTS 兼容接口 |

#### F21.2 角色音色配置
- 每个角色可绑定一个 TTS 音色（在 t_novel_character 或 t_assets 中配置）
- 旁白使用独立的默认音色
- 支持试听音色后再绑定
- 音色参数：语速、音调、情感（平静/激动/悲伤/愤怒）

#### F21.3 剧本台词提取与配音生成
- 从剧本中自动提取对话台词和旁白文本
- 根据角色匹配对应音色
- 逐句生成语音片段（WAV/MP3）
- 支持手动调整台词文本后重新生成

#### F21.4 配音与视频合成
- 配音音频按时间轴对齐到视频片段
- 在 F13.6 视频合成时，可选择叠加配音轨道
- 配音音量与 BGM 音量自动平衡（配音优先，BGM 降低）

#### F21.5 REST API
- `POST /api/projects/{projectId}/tts/generate` — 生成配音
- `GET /api/projects/{projectId}/tts/voices` — 获取可用音色列表
- `POST /api/projects/{projectId}/tts/preview` — 试听指定文本+音色

---

### M22：素材库管理

管理 BGM、音效、片头片尾模板、水印等可复用素材。

#### F22.1 BGM 素材库
- 上传/管理背景音乐文件（MP3/WAV）
- 按风格分类：紧张、温馨、热血、悲伤、搞笑、日常
- 支持试听、收藏
- 视频合成时从素材库选择 BGM

#### F22.2 片头片尾模板
- 预设多套片头/片尾模板（标题卡样式、字体、动画效果）
- 支持自定义上传片头/片尾视频
- 模板参数：背景色、字体、Logo 位置、动画类型、时长

#### F22.3 水印与品牌
- 配置全局水印（文字水印或图片水印）
- 水印参数：内容、位置（四角/居中）、透明度、大小
- 视频合成时自动叠加水印
- 支持按项目覆盖全局水印配置

#### F22.4 素材 CRUD
- 统一的素材上传/删除/分类管理接口
- 素材存储在云 OSS，按类型分目录：`/materials/{type}/{uuid}.ext`
- REST API：
  - `POST /api/materials` — 上传素材
  - `GET /api/materials?type=bgm` — 按类型查询
  - `DELETE /api/materials/{id}` — 删除素材

---

### Agent 通用基础设施（适用于 M4 / M7 / M11）

以下机制为三个多智能体系统（小说 Agent、大纲故事线 Agent、分镜 Agent）的共享基础设施，统一设计、统一实现。

#### AG.1 统一 Agent 抽象框架

三个 Agent 系统（M4/M7/M11）共享同一套基础架构，避免重复实现：

```
BaseAgent（抽象基类）
├── 属性：projectId、emitter、history、tools
├── 核心方法：
│   ├── call(msg)              → 主入口，接收用户消息
│   ├── invokeSubAgent(type, task) → 调用 Sub-Agent（流式）
│   ├── buildContext(task)     → 构建上下文（可重写）
│   ├── getAllTools()          → 获取工具集（可重写）
│   └── getSubAgentTools()    → 获取 Sub-Agent 可用工具（可重写）
├── 内置能力：
│   ├── WebSocket 事件发射（stream/toolCall/transfer/refresh/error）
│   ├── 对话历史管理（自动保存/加载/清空）
│   ├── Prompt 模板加载（从 t_prompts 读取，支持自定义覆盖）
│   └── 执行日志记录
│
├── NovelMainAgent extends BaseAgent     → M4
├── OutlineScriptAgent extends BaseAgent → M7
└── StoryboardAgent extends BaseAgent    → M11
```

Java 实现建议：
- BaseAgent 定义为抽象类，核心流程用模板方法模式
- Sub-Agent 调用通过策略模式，每个 Sub-Agent 是一个 Strategy 实现
- 工具注册用 Spring 的依赖注入，`@AgentTool` 自定义注解标记工具方法
- WebSocket 事件用 Spring 的 `ApplicationEventPublisher` 统一发布

#### AG.2 错误恢复与断点续写

长时间运行的生成任务（尤其是 M4 的逐章生成）需要容错机制：

**a. 生成检查点（Checkpoint）**
- 每完成一个原子单元（一章正文 / 一集大纲 / 一个分镜片段），自动保存检查点
- 检查点记录：当前 Layer、当前步骤、已完成列表、待处理列表
- 存储在 t_task_list 的扩展字段中（新增 checkpoint JSON 字段）

**b. 自动恢复**
- Agent 异常中断后（网络断开、服务重启、LLM 超时），用户重新连接 WebSocket 时：
  1. 检测到未完成的生成任务（state=generating）
  2. 读取最近的检查点
  3. 提示用户："检测到上次生成在第15章中断，是否从第15章继续？"
  4. 用户确认后从检查点恢复，跳过已完成的步骤

**c. LLM 调用重试**
- 单次 LLM 调用失败（超时/限流/网络错误）自动重试，最多 3 次，指数退避（2s → 4s → 8s）
- 3 次都失败则标记当前步骤为 failed，通知用户，等待用户决定（重试/跳过/终止）

#### AG.3 并发控制与任务队列

**a. 消息队列**
- 每个 Agent 实例维护一个消息队列（FIFO）
- 当 Agent 正在执行任务时，新消息入队等待，不会打断当前执行
- 当前任务完成后自动处理队列中的下一条消息

**b. 用户中断**
- 用户发送特殊指令可中断当前执行：
  - `stop` / `暂停`：暂停当前生成，保存检查点，等待用户指令
  - `skip`：跳过当前步骤（如跳过 Editor 审核）
  - `cancel`：取消整个生成任务
- 中断后 Agent 进入 idle 状态，可接收新指令

**c. 锁机制**
- 同一个 projectId 同一时间只能有一个 Agent 实例在执行写操作
- 读操作（getChapter、getOutline 等）不受锁限制
- 防止用户同时在小说 Agent 和大纲 Agent 中修改同一份数据

#### AG.4 审核循环控制

Editor 打回 → Sub-Agent 修改 → Editor 再审 的循环需要有上限：

- 每层审核最多打回 3 次（可配置）
- 超过 3 次仍未通过：
  1. 将当前产出 + Editor 的所有意见一起推送给用户
  2. 用户可选择：强制通过 / 手动修改后继续 / 换一个模型重新生成
- 每次打回时，Editor 的修改意见会累积传给 Sub-Agent，避免重复犯同样的错误
- WebSocket 推送审核轮次：`{layer, round, maxRound, feedback}`

#### AG.5 用户介入点

自动流水线运行时，用户可以在关键节点介入：

**a. 自动模式（默认）**
- 流水线全自动运行，每层完成后 WebSocket 通知前端
- Editor 审核自动执行，通过则继续，打回则自动修改

**b. 半自动模式（可配置）**
- 每层完成后暂停，等待用户确认后再进入下一层
- 用户可以在暂停时：
  - 查看当前层产出，手动微调
  - 对某个角色/章概要提出修改意见
  - 跳过 Editor 审核直接进入下一层
  - 调整后续层的参数（如修改每章字数、增减角色）

**c. 配置项**

| 参数 | 默认值 | 说明 |
|------|--------|------|
| autoMode | true | 是否全自动运行 |
| pauseAfterLayer | [] | 在哪些层后暂停（如 [1,2] 表示世界观和角色设计后暂停） |
| skipEditorReview | false | 跳过 Editor 审核（提高速度，降低质量） |
| editorMaxRetry | 3 | Editor 最大打回次数 |
| parallelChapters | 1 | 同时生成的章节数（1=串行，>1=并行，需模型支持） |

#### AG.6 Agent 可观测性

**a. 执行日志**
- 每次 Agent 调用记录完整执行链路：
  - 时间戳、Agent 类型、工具调用、输入摘要、输出摘要、耗时、token 消耗
- 日志存储在 t_agent_log 表（新增）：

| 字段 | 说明 |
|------|------|
| id | 主键 |
| projectId | 项目 ID |
| agentType | novelAgent / outlineAgent / storyboardAgent |
| sessionId | 会话 ID（同一次 WebSocket 连接） |
| parentLogId | 父日志 ID（Sub-Agent 调用时关联 MainAgent 的日志） |
| action | call / invokeSubAgent / toolCall / llmCall |
| agentName | MainAgent / WorldArchitect / NovelWriter / ... |
| toolName | 工具名（toolCall 时） |
| input | 输入摘要（截断到 500 字） |
| output | 输出摘要（截断到 500 字） |
| duration | 耗时（ms） |
| status | success / failed / timeout |
| errorMessage | 错误信息（失败时） |
| createTime | 创建时间 |

**b. 前端调试面板（可选）**
- 开发模式下，前端可展开查看 Agent 执行链路
- 显示：MainAgent → 调用了哪个 Sub-Agent → Sub-Agent 调用了哪些工具 → 每步耗时
- 方便调试 Prompt 效果和排查问题

---

### M12：图片生成

#### F12.1 多厂商图片模型支持

| 厂商 | 模型示例 | 调用方式 | 特点 |
|------|---------|---------|------|
| Gemini | gemini-2.5-flash-image, gemini-3-pro-image | Google API 直调 | 支持宫格图 |
| 火山引擎 | doubao-seedream-4-5 | REST API + 轮询 | 文生图/图生图 |
| 可灵 | kling-image-o1 | REST API + 轮询 | 文生图/图生图 |
| Vidu | viduq1, viduq2 | REST API + 轮询 | 图生图 |
| RunningHub | nanobanana | REST API + 轮询（需先上传图片） | 宫格图 |
| 其他 | OpenAI 兼容接口 | 标准 OpenAI API | 通用 |

#### F12.2 图片生成类型
- t2i：纯文本生成图片
- ti2i：文本+图片参考生成图片
- i2i：图片到图片转换

#### F12.3 图片处理能力（Python 服务）
- 宫格图分割：将 AI 生成的宫格图按行列分割为单张图片
- 图片拼接：将多张图片拼接为一张（用于视频生成的多图输入）
- 图片压缩：压缩到指定大小（单张 ≤ 3MB，总计 ≤ 10MB）
- 图片缩放：按比例缩放
- 超分辨率：可选，使用 Real-ESRGAN 等模型

#### F12.4 图片存储
- 按项目/类型组织目录：`/{projectId}/{type}/{uuid}.jpg`
- type 包括：role、scene、props、storyboard、video、chat
- 统一通过云 OSS（S3 兼容接口）存储，Java 后端使用 AWS S3 SDK 封装统一 OssClient

#### F12.5 资产图片智能筛选
- 生成分镜图时，AI 自动从项目资产中筛选与当前分镜相关的角色/场景/道具图片
- 作为参考图传给图片模型，保证风格一致性

---

### M13：视频生成

#### F13.1 多厂商视频模型支持

| 厂商 | 模型示例 | 生成类型 | 时长 | 分辨率 | 音频 |
|------|---------|---------|------|--------|------|
| 火山引擎 | doubao-seedance-1-5-pro | 文生视频/图生视频 | 2-12s | 480p-1080p | 支持 |
| 可灵 | kling-v2-6(PRO) | 文生视频/图生视频 | 5-10s | 720p-1080p | 不支持 |
| Vidu | viduq3-pro | 文生视频/图生视频 | 1-16s | 540p-1080p | 支持 |
| 万象 | wan2.6-t2v/i2v | 文生视频/图生视频 | 2-15s | 720p-1080p | 支持 |
| Gemini | veo-3.1 | 文生视频/图生视频 | 4-8s | 720p-1080p | 支持 |
| RunningHub | sora-2 | 文生视频/图生视频 | 10-25s | - | 不支持 |
| Apimart | sora-2 | 文生视频/图生视频 | 10-25s | - | 不支持 |

#### F13.2 视频生成模式
- text：纯文本生成视频
- singleImage：单张图片 + 文本生成视频
- startEndRequired：首尾帧（两张图片都必须提供）
- endFrameOptional：首帧必须 + 尾帧可选
- startFrameOptional：尾帧必须 + 首帧可选
- multiImage：多图模式
- reference：参考图模式

#### F13.3 视频生成流程
1. 前端提交生成请求（projectId、scriptId、configId、分镜图路径、提示词、时长、分辨率、是否带音频）
2. 后端立即创建 t_video 记录（state=0 生成中），返回 videoId
3. 后台异步执行：
   a. 读取分镜图片，转为 base64
   b. 构建视频提示词（融合运镜指令 + 画风要求 + 人物一致性要求）
   c. 如果启用首尾帧衔接：读取前一个分镜视频的最后一帧作为当前视频的首帧参考
   d. 调用视频模型 API 创建任务
   e. 轮询任务状态（最多 500 次，每次间隔 2 秒）
   f. 任务完成后下载视频文件保存到云 OSS
   g. 提取视频最后一帧，保存为下一个分镜的首帧参考（lastFrame）
   h. 更新 t_video（state、filePath、lastFrame、duration、errorMessage）
4. 通过 WebSocket 实时推送生成进度

#### F13.3.1 视频片段间画面连续性

6 个独立生成的 10s 片段拼成 1 分钟视频，如果不做处理，角色外貌、场景色调、光影风格可能每段都不一样。系统采用以下策略保证连续性：

**a. 首尾帧衔接（推荐）**
- 生成第 N 个视频片段时，将第 N-1 个片段的最后一帧作为首帧参考传入
- 支持的模式：startEndRequired、endFrameOptional（大部分厂商支持）
- 第 1 个片段使用分镜图作为首帧，后续片段使用前一个视频的 lastFrame
- t_video 新增 lastFrame 字段：存储该视频最后一帧的 OSS 路径

**b. 统一风格提示词**
- 所有同一集的视频片段共享相同的风格前缀提示词（画风、色调、光影风格）
- 风格前缀从项目的 artStyle + 第一个片段的视觉特征中提取
- 存储在 t_video_config 的 stylePrefix 字段

**c. 角色一致性参考图**
- 视频提示词中注入当前分镜出场角色的资产参考图
- 部分厂商（可灵、Vidu）支持 reference 模式，可传入角色参考图保持一致性

**d. 生成顺序**
- 同一集的视频片段必须按分镜顺序串行生成（保证首尾帧衔接）
- 不同集之间可以并行生成

#### F13.4 视频重试与重新生成

云厂商通常提供免费重试次数（如 3 次），系统需要充分利用：

**a. 自动重试**
- 视频生成失败后自动重试，最多重试 maxRetry 次（默认 3，可配置）
- 每次重试记录在 t_video 的 retryCount 字段
- 重试策略：
  - 第 1 次重试：相同参数直接重试（可能是临时故障）
  - 第 2 次重试：微调提示词（AI 自动润色，避免触发内容审核）
  - 第 3 次重试：降低分辨率或时长（降级策略，确保出片）
- 每次重试的结果都保留，用户可选择任意一次的结果

**b. 手动重新生成**
- 用户对生成结果不满意，可手动触发重新生成
- 支持修改提示词后重新生成
- 支持更换模型/厂商后重新生成
- 旧视频保留，不覆盖（用户可对比选择）

**c. 批量重试**
- 一集中有多个分镜视频失败时，支持一键批量重试所有失败的视频
- REST API：`POST /api/projects/{projectId}/videos/batch-retry`

#### F13.4.1 视频生成 WebSocket 进度推送

视频生成是异步长耗时操作，需要实时推送进度给前端：

连接端点：复用 `/ws/pipeline/{projectId}` 或独立 `/ws/video/{projectId}`

| type | data | 说明 |
|------|------|------|
| videoGenerateStart | {scriptId, totalVideos} | 开始批量生成视频 |
| videoGenerateProgress | {videoId, shotId, state, progress, message} | 单个视频生成进度（轮询中/完成/失败） |
| videoGenerateComplete | {videoId, shotId, filePath, duration} | 单个视频生成完成 |
| videoGenerateError | {videoId, shotId, error, retryCount, canRetry} | 单个视频生成失败 |
| videoRetryStart | {videoId, retryCount} | 开始重试 |
| videoBatchComplete | {scriptId, successCount, failCount, totalDuration} | 一集所有视频生成完毕 |

#### F13.4.2 视频版本管理与预览选择

重试和重新生成会产生同一个分镜的多个视频版本，用户需要能预览对比并选择最终版本：

**a. 版本管理**
- 同一个 shotId 可关联多条 t_video 记录（不同版本）
- t_video 新增 version 字段（自增）和 selected 字段（boolean，标记为最终选择）
- 默认最新生成的版本为 selected
- 用户可手动切换 selected 版本

**b. 预览对比**
- 前端支持同一分镜的多个视频版本并排预览
- 显示每个版本的：生成时间、使用的模型/厂商、提示词、分辨率、时长
- 一键选择某个版本作为最终版本

**c. REST API**
- `GET /api/projects/{projectId}/videos/versions?shotId={shotId}` — 获取某分镜的所有视频版本
- `PUT /api/projects/{projectId}/videos/{videoId}/select` — 选择某个版本为最终版本
- 合成视频时只使用 selected=true 的版本

#### F13.5 图片质量检测（视频生成前置校验）

分镜图片质量直接决定视频质量，差图生成的视频必然是废片。系统在视频生成前自动进行图片质量检测：

**a. 自动检测维度**
- 画面完整性：是否有明显的 AI 生成瑕疵（多余的手指、扭曲的面部、断裂的肢体）
- 风格一致性：与项目 artStyle 和同集其他分镜图的风格是否统一
- 角色一致性：角色外貌是否与资产参考图匹配（发色、服装、体型）
- 构图合理性：是否有明显的构图问题（主体偏移、画面空洞、元素重叠）
- 分辨率达标：是否满足视频生成的最低分辨率要求

**b. 检测方式**
- 方式一：调用多模态 LLM（如 GPT-4o / Gemini）对图片进行评分（0-100），附带问题描述
- 方式二：简单规则检测（分辨率、文件大小、宽高比是否匹配 videoRatio）

**c. 检测结果处理**
- 评分 ≥ 80：通过，可进入视频生成
- 评分 60-79：警告，标记为"建议重新生成"，用户可选择继续或重新生成
- 评分 < 60：阻断，自动触发图片重新生成（最多重试 2 次）
- 全自动流水线（M20）中：低于阈值的图片自动重新生成，超过重试次数则跳过该分镜并记录

**d. REST API**
- `POST /api/projects/{projectId}/images/quality-check` — 批量检测图片质量
- 返回：每张图片的评分 + 问题列表 + 建议

#### F13.6 视频片段合成（短片段 → 长视频）

云厂商单次生成的视频时长有限（通常 10-15s），但一集短剧需要 1-3 分钟。系统需要将多个短片段拼接合成为完整的单集视频。

**a. 合成流程**
```
分镜 1 → 视频片段 1 (10s)  ─┐
分镜 2 → 视频片段 2 (10s)  ─┤
分镜 3 → 视频片段 3 (10s)  ─┼→ 合成引擎 → 单集完整视频 (60-180s)
分镜 4 → 视频片段 4 (10s)  ─┤
分镜 5 → 视频片段 5 (10s)  ─┤
分镜 6 → 视频片段 6 (10s)  ─┘
```

**b. 合成能力（Python 服务，使用 FFmpeg）**
- 视频拼接：按分镜顺序将多个短视频片段无缝拼接
- 转场效果：片段之间可配置转场（淡入淡出、黑屏过渡、交叉溶解、无转场直切）
- 音频处理：
  - 保留原始视频音频
  - 可选：添加背景音乐（BGM），从素材库选择或上传，自动调节音量平衡
  - 可选：AI 配音（M21 TTS），根据剧本台词生成旁白/角色对话，按时间轴对齐
  - 多轨混音：原始音频 + BGM + 配音，自动音量平衡（配音优先）
- 字幕叠加：根据剧本内容自动生成字幕轨道（SRT 格式），烧录到视频中
- 片头片尾：可配置片头（标题卡）和片尾（制作信息）
- 输出格式：MP4 (H.264)，支持 720p / 1080p

**c. 合成配置（t_video_compose_config 新增表）**

| 字段 | 说明 |
|------|------|
| id | 主键 |
| scriptId | 关联剧本 |
| projectId | 关联项目 |
| transition | 转场类型：none / fadeInOut / crossDissolve / blackScreen |
| transitionDuration | 转场时长（ms），默认 500 |
| bgmPath | 背景音乐文件路径（可选，可从素材库选择） |
| bgmVolume | BGM 音量（0-100），默认 30 |
| ttsEnabled | 是否叠加 AI 配音 |
| ttsVolume | 配音音量（0-100），默认 80 |
| subtitleEnabled | 是否叠加字幕 |
| subtitleStyle | 字幕样式 JSON（字体、大小、颜色、位置、描边） |
| watermarkEnabled | 是否叠加水印 |
| watermarkType | 水印类型：text / image |
| watermarkContent | 水印内容（文字或图片路径） |
| watermarkPosition | 水印位置：topLeft / topRight / bottomLeft / bottomRight / center |
| watermarkOpacity | 水印透明度（0-100），默认 30 |
| introEnabled | 是否添加片头 |
| introText | 片头文字 |
| introDuration | 片头时长（s） |
| outroEnabled | 是否添加片尾 |
| outroText | 片尾文字 |
| outroDuration | 片尾时长（s） |
| outputResolution | 输出分辨率 |
| outputFps | 输出帧率，默认 30 |

**d. 合成任务（t_video_compose 新增表）**

| 字段 | 说明 |
|------|------|
| id | 主键 |
| scriptId | 关联剧本 |
| projectId | 关联项目 |
| configId | 合成配置 ID |
| videoIds | 参与合成的视频片段 ID 列表（JSON） |
| filePath | 合成后的视频文件路径 |
| duration | 合成后总时长（s） |
| state | 0=合成中 / 1=成功 / -1=失败 |
| retryCount | 重试次数 |
| errorMessage | 失败原因 |
| createTime | 创建时间 |

**e. REST API**
- `POST /api/projects/{projectId}/videos/compose` — 提交合成任务
- `GET /api/projects/{projectId}/videos/compose/{composeId}` — 查询合成状态
- `POST /api/projects/{projectId}/videos/compose/{composeId}/retry` — 重试合成
- `GET /api/projects/{projectId}/videos/compose/list` — 获取合成视频列表

#### F13.7 视频配置管理
- 每个剧本可配置多套视频生成参数
- 配置项：manufacturer、mode、resolution、duration、audioEnabled、startFrame、endFrame
- CRUD 操作

#### F13.8 获取视频列表
- 根据 scriptId 获取所有视频记录
- 包含状态、文件路径、提示词等

#### F13.9 获取视频模型列表
- 返回所有支持的视频模型及其能力参数（时长、分辨率、画幅比、生成类型、是否支持音频）

#### F13.10 获取厂商列表
- 返回所有支持的视频厂商

#### F13.11 视频分镜图管理
- 获取视频关联的分镜图列表
- 修改视频关联的分镜图

---

### M14：AI 模型配置

这是整个系统的 AI 能力底座，所有 AI 调用都通过这里的配置获取模型信息。

#### F14.1 模型配置 CRUD

t_config 表存储所有 AI 模型配置：

| 字段 | 说明 |
|------|------|
| type | 模型类型：text（文本）、image（图片）、video（视频） |
| name | 配置名称（用户自定义） |
| manufacturer | 厂商标识 |
| model | 模型名称 |
| apiKey | API 密钥 |
| baseUrl | API 地址（部分厂商需要） |

支持的文本模型厂商：
- DeepSeek：deepseek-chat、deepseek-reasoner
- 豆包(doubao)：doubao-seed-1-8、doubao-seed-1-6 等
- 智谱(zhipu)：glm-4.7、glm-4.6、glm-4.5 系列
- 通义千问(qwen)：qwen-max、qwen-plus、qwen2.5-72b 等
- OpenAI：gpt-4o、gpt-4.1、gpt-5.1、gpt-5.2
- Gemini：gemini-2.5-pro、gemini-2.5-flash、gemini-2.0-flash
- Anthropic：claude-opus-4-5、claude-sonnet-4-5、claude-haiku-4-5
- XAI：grok-3、grok-4、grok-4.1
- 其他：任意 OpenAI 兼容接口

#### F14.2 功能→模型映射（t_ai_model_map）

系统有 8+ 个 AI 功能点，每个功能点可独立绑定一个模型配置：

| key | 功能说明 |
|-----|---------|
| outlineScriptAgent | 大纲故事线 Agent |
| storyboardAgent | 分镜 Agent |
| novelAgent | AI 小说生成 Agent（新增） |
| storyboardImage | 分镜图片生成 |
| assetsImage | 资产图片生成 |
| generateScript | 剧本生成 |
| imageQualityCheck | 图片质量检测（新增） |
| tts | AI 配音（新增） |
| ... | 可扩展 |

这意味着用户可以：
- 大纲生成用 Qwen3.5（便宜、中文好）
- 剧本生成用 Claude（创意好）
- 图片生成用 Gemini（质量高）
- 视频生成用火山引擎（性价比高）

#### F14.3 模型连通性测试
- 测试文本模型：发送简单 prompt，验证 API 连通
- 测试图片模型：生成一张测试图片
- 测试视频模型：创建一个测试任务

#### F14.4 模型能力属性

每个模型有以下能力标记：
- responseFormat：schema（原生 JSON Schema 输出）或 object（prompt 附加 schema）
- image：是否支持图片输入（多模态）
- think：是否支持思考模式
- tool：是否支持工具调用（Function Calling）

#### F14.5 配置模型映射
- 将功能点绑定到具体的模型配置

#### F14.6 获取模型映射
- 查询当前所有功能点的模型绑定情况

---

### M15：Prompt 模板管理

所有 AI 调用的 System Prompt 都存储在数据库中，支持用户自定义覆盖。

#### F15.1 获取 Prompt 列表
- 返回所有 Prompt 模板
- 每条记录包含：code（唯一标识）、name（显示名）、type（分类）、defaultValue（系统默认）、customValue（用户自定义）

#### F15.2 更新 Prompt
- 用户可修改 customValue 覆盖默认 Prompt
- 清空 customValue 则恢复使用 defaultValue

#### F15.3 Prompt 模板清单

| code | 用途 |
|------|------|
| outlineScript-main | 大纲 Agent 主协调者 Prompt |
| outlineScript-a1 | 故事师 Prompt |
| outlineScript-a2 | 大纲师 Prompt |
| outlineScript-director | 导演 Prompt |
| storyboard-main | 分镜 Agent 主协调者 Prompt |
| storyboard-segment | 片段师 Prompt |
| storyboard-shot | 分镜师 Prompt |
| script | 剧本生成 Prompt |
| role-generateImage | 角色图片生成 Prompt |
| scene-generateImage | 场景图片生成 Prompt |
| tool-generateImage | 道具图片生成 Prompt |
| storyboard-generateImage | 分镜图片生成 Prompt |
| novel-main | 小说 Agent 主协调者/总编 Prompt（新增） |
| novel-world-architect | 世界架构师 Prompt（新增） |
| novel-character-designer | 角色设计师 Prompt（新增） |
| novel-plot-architect | 情节架构师 Prompt（新增） |
| novel-chapter-planner | 章节规划师 Prompt（新增） |
| novel-writer | 小说写手 Prompt（新增） |
| novel-editor | 总编审 Prompt（新增） |
| novel-quality-inspector | 质检官 Prompt（新增） |
| novel-writing-quality | 系统级写作质量指令（新增·强制注入·不可覆盖） |
| novel-gen-system-flow | 系统流品类特化 Prompt（新增） |
| novel-gen-infinite-flow | 无限流品类特化 Prompt（新增） |
| novel-gen-urban | 都市品类特化 Prompt（新增） |
| novel-gen-xuanhuan | 玄幻品类特化 Prompt（新增） |
| tts-dialogue-extract | 台词提取 Prompt（新增） |
| video-prompt-generate | 视频提示词生成 Prompt（新增·融合运镜指令） |


---

### M16：任务管理

#### F16.1 获取任务列表
- 查询所有后台任务（视频生成、小说生成等）
- 返回：id、name、prompt、state、startTime、endTime

#### F16.2 任务详情
- 查询单个任务的详细信息和执行日志

#### F16.3 任务状态
- state：pending / running / success / failed
- 支持按状态筛选

---

### M17：系统设置

#### F17.1 获取系统设置
- 返回当前系统配置

#### F17.2 更新系统设置
- 更新 tokenKey、默认模型等

#### F17.3 获取系统日志
- 查看最近的系统运行日志

#### F17.4 数据管理
- 清空数据库（危险操作，需二次确认）
- 删除所有项目数据

---

## 四、数据模型

### 4.1 表结构总览（30 张表）

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| t_user | 用户表 | id, name, password(BCrypt) |
| t_project | 项目主表 | id, name, intro, type, artStyle, videoRatio, createTime, userId |
| t_novel | 小说章节 | id, chapterIndex, reel, chapter, chapterData(TEXT), summary(TEXT), volumeIndex, projectId |
| t_novel_world | 小说世界观(新增) | id, projectId, background(TEXT), powerSystem(JSON), socialStructure(JSON), coreRules(JSON), taboos(JSON), state |
| t_novel_character | 小说角色档案(新增) | id, projectId, name, role(protagonist/supporting/antagonist), age, appearance(TEXT), personality(TEXT), ability(TEXT), relationships(JSON), growthArc(TEXT), currentState(JSON), speechStyle(JSON), state |
| t_novel_outline | 小说大纲+分卷(新增) | id, projectId, mainPlot(TEXT), theme, volumeIndex, volumeName, volumePlot(TEXT), startChapter, endChapter, volumeClimax(TEXT), volumeCliffhanger(TEXT), state |
| t_novel_chapter_plan | 章概要(新增) | id, projectId, volumeIndex, chapterIndex, title, summary(TEXT), keyEvents(JSON), characters(JSON), emotionCurve, foreshadowing(JSON), payoff(JSON), cliffhanger(TEXT), wordTarget, state |
| t_novel_quality_report | 小说质检报告(新增) | id, projectId, scope(chapter/volume/book), scopeIndex, overallScore, dimensions(JSON), summary(TEXT), autoFixSuggestions(JSON), state(pending/completed/fixed), createTime |
| t_storyline | 故事线 | id, name, content(TEXT), novelIds, projectId |
| t_outline | 大纲 | id, episode, data(JSON/EpisodeData), projectId |
| t_script | 剧本 | id, name, content(TEXT), projectId, outlineId |
| t_assets | 资产 | id, name, intro, prompt, type, filePath, projectId |
| t_image | 图片记录 | id, filePath, type, state, assetsId, scriptId, projectId |
| t_video | 视频记录 | id, projectId, scriptId, shotId, segmentId, configId, prompt, videoPrompt, cameraMotion, filePath, lastFrame, state(0/1/-1), duration, resolution, manufacturer, model, taskId, retryCount, maxRetry, version, selected, errorMessage, createTime |
| t_video_config | 视频配置 | id, scriptId, projectId, manufacturer, mode, resolution, duration, audioEnabled, stylePrefix |
| t_config | AI模型配置 | id, type(text/image/video), name, model, apiKey, baseUrl, manufacturer |
| t_ai_model_map | 功能模型映射 | id, configId, name, key |
| t_setting | 系统设置 | id, userId, tokenKey, imageModel, languageModel |
| t_prompts | Prompt模板 | id, code, name, type, defaultValue, customValue, parentCode |
| t_chat_history | 对话历史 | id, type(outlineAgent/storyboardAgent/novelAgent), data(JSON), novel, projectId |
| t_task_list | 任务列表 | id, name, prompt, state, startTime, endTime, checkpoint(JSON) |
| t_agent_log | Agent执行日志(新增) | id, projectId, agentType, sessionId, parentLogId, action, agentName, toolName, input(TEXT), output(TEXT), duration, status, errorMessage, createTime |
| t_user_role | 用户角色(新增) | id, userId, role(admin/creator/viewer), createTime |
| t_user_quota | 用户配额(新增) | id, userId, dailyChapterLimit, dailyImageLimit, dailyVideoLimit, usedChapters, usedImages, usedVideos, resetDate |
| t_novel_version | 章节版本历史(新增) | id, novelId, projectId, chapterIndex, chapterData(TEXT), summary(TEXT), source, version, createTime |
| t_video_compose_config | 视频合成配置(新增) | id, scriptId, projectId, transition, transitionDuration, bgmPath, bgmVolume, ttsEnabled, ttsVolume, subtitleEnabled, subtitleStyle(JSON), watermarkEnabled, watermarkType, watermarkContent, watermarkPosition, watermarkOpacity, introEnabled, introText, introDuration, outroEnabled, outroText, outroDuration, outputResolution, outputFps |
| t_video_compose | 视频合成任务(新增) | id, scriptId, projectId, configId, videoIds(JSON), filePath, duration, state, retryCount, errorMessage, createTime |
| t_tts_config | TTS配音配置(新增) | id, projectId, characterId, voiceId, manufacturer, speed, pitch, emotion, state |
| t_tts_audio | TTS音频记录(新增) | id, projectId, scriptId, text, characterName, voiceId, filePath, duration, state, createTime |
| t_material | 素材库(新增) | id, name, type(bgm/sfx/intro/outro/watermark), filePath, category, duration, tags(JSON), userId, createTime |

### 4.2 关键 JSON 结构

#### EpisodeData（大纲数据）
```json
{
  "episodeIndex": 1,
  "title": "命运的转折",
  "chapterRange": [1, 2],
  "scenes": [{"name": "...", "description": "..."}],
  "characters": [{"name": "...", "description": "..."}],
  "props": [{"name": "...", "description": "..."}],
  "coreConflict": "...",
  "outline": "100-300字剧情主干",
  "openingHook": "...",
  "keyEvents": ["起", "承", "转", "合"],
  "emotionalCurve": "2→5→9→3",
  "visualHighlights": ["镜头1", "镜头2"],
  "endingHook": "...",
  "classicQuotes": ["金句1"]
}
```

#### Shot（分镜数据）
```json
{
  "id": 1,
  "segmentId": 0,
  "title": "分镜 1",
  "x": 0, "y": 0,
  "cells": [
    {"id": "uuid", "prompt": "镜头提示词", "src": "图片路径"}
  ],
  "fragmentContent": "片段描述",
  "cameraMotion": "push in slowly",
  "videoPrompt": "角色缓缓推开铁门，镜头从中景推向角色面部特写，光线从门缝透入逐渐变亮",
  "assetsTags": [
    {"type": "role", "text": "角色名"},
    {"type": "scene", "text": "场景名"}
  ]
}
```

#### Segment（片段数据）
```json
{
  "index": 1,
  "description": "片段描述",
  "emotion": "紧张",
  "action": "主角推开门"
}
```

#### NovelWorld（世界观数据，t_novel_world）
```json
{
  "id": 1,
  "projectId": 100,
  "background": "公元2147年，一场被称为'大崩塌'的灾变撕裂了现实与异空间的壁障...",
  "powerSystem": {
    "name": "源力觉醒体系",
    "levels": ["觉醒者", "凝源者", "源师", "大源师", "源王", "源皇", "源帝"],
    "rules": "每个觉醒者只能掌握一种源力属性，突破需要对应的源晶...",
    "specialAbilities": ["时间减速", "空间折叠", "元素操控", "精神侵入"]
  },
  "socialStructure": {
    "factions": ["联邦政府", "猎人公会", "暗影组织", "自由佣兵团"],
    "hierarchy": "觉醒者等级决定社会地位，普通人沦为底层..."
  },
  "coreRules": ["源力不可转让", "异空间每72小时刷新一次", "S级副本需5人以上组队"],
  "taboos": ["禁止吞噬同类源核", "禁止私自开启异空间裂缝"]
}
```

#### NovelCharacter（角色档案，t_novel_character）
```json
{
  "id": 1,
  "projectId": 100,
  "name": "陈默",
  "role": "protagonist",
  "age": 25,
  "appearance": "身高182cm，短发微乱，左眼角有一道淡疤，体型偏瘦但肌肉线条分明...",
  "personality": "表面冷漠寡言，实则内心柔软。极度理性，危机时刻冷静到近乎冷血...",
  "ability": "时间减速：可将自身周围5米范围内的时间流速降低至1/10，持续时间随等级提升...",
  "relationships": {
    "林晚": "青梅竹马，唯一信任的人",
    "赵铁柱": "搭档，性格互补",
    "沈墨": "宿敌，曾经的发小，因源核事件黑化"
  },
  "growthArc": "从底层觉醒者 → 被迫卷入阴谋 → 发现自身能力的真正秘密 → 成为改变世界格局的关键人物",
  "currentState": {
    "location": "猎人公会总部",
    "physicalState": "左臂轻伤，已包扎",
    "emotionalState": "警惕、压抑怒火",
    "powerLevel": "凝源者·中期",
    "inventory": ["暗影徽章", "C级源晶×3"],
    "knownInfo": ["沈墨还活着", "暗影组织在渗透公会"],
    "unknownInfo": ["林晚的真实身份", "源力起源的秘密"],
    "lastUpdatedChapter": 12
  },
  "speechStyle": {
    "tone": "冷淡、简短",
    "habits": ["很少用感叹号", "喜欢用反问句", "紧张时会沉默而非废话"],
    "vocabulary": ["口头禅：'无所谓'", "从不说脏话", "用词偏书面"],
    "exampleDialogues": [
      "「走。」",
      "「你觉得呢？」",
      "「……」"
    ]
  }
}
```

#### NovelOutline（大纲+分卷，t_novel_outline）
```json
{
  "id": 1,
  "projectId": 100,
  "mainPlot": "陈默在一次异空间探索中意外觉醒了被禁忌的'时间'源力，由此卷入一场关于源力起源真相的惊天阴谋...",
  "theme": "在绝对力量面前，人性的选择",
  "volumeIndex": 1,
  "volumeName": "觉醒",
  "volumePlot": "陈默从普通人觉醒为时间系觉醒者，加入猎人公会，在新手副本中崭露头角，同时发现公会内部的腐败...",
  "startChapter": 1,
  "endChapter": 10,
  "volumeClimax": "第一卷高潮：S级副本突然降临，陈默被迫暴露时间能力救下队友，引起暗影组织注意",
  "volumeCliffhanger": "沈墨出现在暗影组织的名单上，陈默发现发小还活着——但已经站在了对立面"
}
```

#### ChapterPlan（章概要，t_novel_chapter_plan）
```json
{
  "id": 1,
  "projectId": 100,
  "volumeIndex": 1,
  "chapterIndex": 1,
  "title": "大崩塌的第七年",
  "summary": "陈默在废墟区做着清理异兽的底层工作，一次例行任务中误入未记录的微型裂缝，体内沉睡的源力被激活。觉醒的瞬间他看到了时间的纹理——周围的一切都慢了下来...",
  "keyEvents": ["例行清理任务", "误入微型裂缝", "源力觉醒", "时间减速首次触发"],
  "characters": ["陈默", "赵铁柱"],
  "emotionCurve": "平淡(日常) → 紧张(遇险) → 震撼(觉醒) → 迷茫(不知所措)",
  "foreshadowing": ["陈默左眼的疤痕在觉醒时发光", "裂缝中闪过一个模糊的人影"],
  "payoff": [],
  "cliffhanger": "猎人公会的探测器捕捉到了异常的时间波动信号，一支精英小队已经出发...",
  "wordTarget": 3000
}
```

---

## 五、前端页面规划（Vue 3）

### 5.1 页面清单

| 页面 | 路由 | 说明 |
|------|------|------|
| 登录页 | /login | 用户名+密码+验证码 |
| 项目列表 | /projects | 卡片式展示，支持新建/删除 |
| 项目工作台 | /projects/:id | 项目主界面，左侧导航切换子模块 |
| ├ 小说管理 | /projects/:id/novel | 章节列表+编辑器+AI生成入口 |
| ├ AI小说生成 | /projects/:id/novel/generate | 创作参数配置+实时生成预览 |
| ├ 故事线 | /projects/:id/storyline | 故事线查看/编辑 |
| ├ 大纲管理 | /projects/:id/outline | 分集大纲卡片+详情编辑 |
| ├ Agent对话 | /projects/:id/agent | 与大纲Agent对话的聊天界面 |
| ├ 资产管理 | /projects/:id/assets | 角色/道具/场景列表+图片生成 |
| ├ 剧本管理 | /projects/:id/scripts | 分集剧本列表+编辑器+AI生成 |
| ├ 分镜工作台 | /projects/:id/storyboard/:scriptId | 分镜Agent对话+分镜预览+图片生成 |
| ├ 视频生成 | /projects/:id/video/:scriptId | 视频配置+生成+预览 |
| ├ 视频合成 | /projects/:id/video/:scriptId/compose | 合成配置+片段排列+预览成片 |
| ├ AI配音 | /projects/:id/tts/:scriptId | 台词提取+音色配置+配音生成 |
| ├ 全自动流水线 | /projects/:id/pipeline | 一键生产+流水线状态+审核节点 |
| 系统设置 | /settings | 模型配置+Prompt管理+系统设置 |
| ├ 模型配置 | /settings/models | AI模型CRUD+功能映射 |
| ├ Prompt管理 | /settings/prompts | Prompt模板查看/编辑 |
| 素材库 | /materials | BGM/片头片尾/水印素材管理 |
| 任务中心 | /tasks | 后台任务列表+状态监控 |
| 生产监控 | /dashboard | 产量统计+模型监控+告警（管理员） |
| 用户管理 | /settings/users | 用户CRUD+角色分配+配额管理（管理员） |

### 5.2 关键交互

#### AI 小说生成页
- 左侧：创作参数表单（题材选择、风格、提示词、主角设定、章节数等）
- 右侧：实时生成预览区
  - 顶部进度条：显示总进度（第 N/M 章）
  - 主区域：当前章节的流式文本输出
  - 底部：已完成章节的缩略列表，点击可展开查看
- 生成完成后可逐章编辑、重新生成、续写

#### Agent 对话页
- 左侧：聊天界面（类 ChatGPT）
  - 显示 MainAgent 输出
  - 显示 Sub-Agent 切换和输出（不同颜色区分）
  - 显示工具调用状态
- 右侧：数据面板
  - 故事线预览
  - 大纲列表预览
  - 资产列表预览
  - 数据变更时自动刷新（通过 refresh 事件）

#### 分镜工作台
- 上方：Agent 对话区（与分镜 Agent 交互）
- 下方：分镜预览区
  - 片段卡片列表
  - 每个片段下展示镜头图片网格
  - 图片生成进度实时显示
  - 支持拖拽排序、替换图片

---

## 六、非功能需求

### 6.1 性能
- 文本模型流式输出延迟 < 500ms（首 token）
- 图片生成超时 < 120s
- 视频生成轮询最长 1000s（500次 × 2s）
- WebSocket 消息延迟 < 100ms
- API 接口响应时间 P99 < 200ms（不含 AI 调用）
- 单实例支持 200+ 并发 WebSocket 连接（JDK 21 Virtual Threads）
- 数据库查询 P99 < 50ms（热数据走 Redis 缓存）

### 6.2 安全
- JWT Token 鉴权，Token 存 Redis 支持主动失效和续期
- 密码 BCrypt 加密
- API Key 加密存储（AES-256，密钥与数据库分离）
- OSS Key 路径安全校验（防目录穿越、禁止 `..` 路径）
- 请求体大小限制 100MB（支持大文件上传）
- 接口限流：基于 Redis 的滑动窗口限流，按用户/IP 维度
- SQL 注入防护：MyBatis-Plus 参数化查询
- XSS 防护：输入过滤 + 输出转义
- CORS 白名单配置（非 * 通配）

### 6.3 可靠性
- AI 调用失败自动重试 3 次，指数退避（2s → 4s → 8s）
- 视频/图片生成失败自动重试 1 次
- WebSocket 断线自动重连（前端实现，指数退避）
- 对话历史自动保存（断开连接时 + 定时保存）
- 生成任务断点续写（检查点机制，见 AG.2）
- 数据库连接池健康检查（HikariCP）
- Redis 连接哨兵/集群模式（上云后）

### 6.4 可扩展性
- AI Provider 策略模式，新增厂商只需实现接口
- Prompt 模板数据库管理，无需改代码
- 功能→模型映射可动态调整
- 品类 Prompt 模板可自由扩展
- 水平扩展：无状态服务设计，Session 存 Redis，多实例部署
- 数据库：MySQL 单机 → 读写分离 → TiDB/PolarDB 分布式（按阶段演进）

### 6.5 Redis 使用规划

| 用途 | Key 模式 | 数据类型 | TTL | 说明 |
|------|---------|---------|-----|------|
| 用户 Token | `auth:token:{userId}` | String | 24h | JWT Token 存储，支持主动失效 |
| 接口限流 | `ratelimit:{userId}:{api}` | String | 1min | 滑动窗口计数 |
| 验证码 | `captcha:{uuid}` | String | 5min | 图形验证码答案 |
| 生成任务锁 | `lock:agent:{projectId}` | String | 自动续期 | 分布式锁，防止同项目并发写入 |
| 生成进度 | `progress:novel:{projectId}` | Hash | 任务结束清除 | 实时进度缓存，前端轮询用 |
| 模型配置缓存 | `cache:config:{configId}` | String | 10min | 热数据缓存，减少 DB 查询 |
| Prompt 缓存 | `cache:prompt:{code}` | String | 10min | Prompt 模板缓存 |
| 任务队列 | `queue:agent:{type}` | Stream | 持久化 | 异步任务队列（Redis Stream） |
| WebSocket 会话 | `ws:session:{projectId}:{agentType}` | Hash | 连接断开清除 | 多实例下 WebSocket 会话路由 |
| 角色状态快照缓存 | `cache:character:state:{projectId}` | Hash | 生成期间 | 高频读取的角色状态 |

### 6.6 企业级生产能力（日产几百本）

**a. 任务调度与并行生产**
- 多项目并行：不同项目的生成任务互不阻塞，通过 Redis Stream 分发到不同 Worker
- 单项目内串行：同一本小说的章节按顺序生成（保证上下文连贯）
- 任务优先级：支持 P0（紧急）/ P1（正常）/ P2（低优先级）三级队列
- Worker 池：可配置 Worker 数量，每个 Worker 是一个 Virtual Thread，独立处理一个生成任务
- 上云后：Worker 可水平扩展到多台机器，通过 Redis Stream 消费同一队列

**b. 批量创建任务**
- 支持批量提交生成任务：一次提交 N 本小说的创作参数
- 任务入队后异步执行，前端通过任务中心监控进度
- REST API：`POST /api/batch/novel-generation`
- 入参：创作参数数组，每个元素对应一本小说

**c. 模型负载均衡**
- 同一个功能点可配置多个模型（如 novelAgent 绑定 3 个 Qwen 实例）
- 请求按轮询/权重/最少连接分发到不同模型实例
- 某个模型限流/故障时自动切换到备用模型
- 配置存储在 t_ai_model_map 的扩展字段中

**d. 生产监控仪表盘**
- 实时数据：
  - 当前运行中的生成任务数
  - 队列中等待的任务数
  - 今日已完成的小说数 / 章节数 / 总字数
  - 各模型的调用次数 / 平均耗时 / 错误率
  - 各品类的生成数量分布
- 历史趋势：
  - 日/周/月产量曲线
  - 质检平均分趋势
  - 模型成本趋势
- 告警规则：
  - 任务队列积压超过 N 个 → 告警
  - 模型错误率超过 10% → 告警
  - 单任务耗时超过预期 2 倍 → 告警

### 6.7 多租户与权限

**a. 用户角色**

| 角色 | 权限 |
|------|------|
| 超级管理员 | 全部权限，管理用户、模型配置、系统设置 |
| 管理员 | 管理项目、查看所有用户的项目、配置 Prompt |
| 创作者 | 创建/管理自己的项目，使用 AI 生成功能 |
| 查看者 | 只读权限，查看项目内容 |

**b. 资源隔离**
- 数据隔离：所有查询自动附加 userId 条件（MyBatis-Plus 拦截器）
- 存储隔离：OSS 文件按 `{userId}/{projectId}/` 前缀隔离
- 配额管理：每个用户可配置每日生成上限（章节数/字数/图片数/视频数）

**c. 新增表：t_user_role、t_user_quota**

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| t_user_role | 用户角色 | id, userId, role(admin/creator/viewer), createTime |
| t_user_quota | 用户配额 | id, userId, dailyChapterLimit, dailyImageLimit, dailyVideoLimit, usedChapters, usedImages, usedVideos, resetDate |

### 6.8 数据备份与版本管理

**a. 章节版本历史**
- 每次重新生成/润色/自动修复章节时，旧版本自动存入版本历史
- 支持查看历史版本、对比差异、回滚到任意版本
- 新增表 t_novel_version：

| 字段 | 说明 |
|------|------|
| id | 主键 |
| novelId | 关联 t_novel.id |
| projectId | 项目 ID |
| chapterIndex | 章节序号 |
| chapterData | 该版本的正文全文 |
| summary | 该版本的摘要 |
| source | 版本来源：generate / regenerate / polish / autofix / manual |
| version | 版本号（自增） |
| createTime | 创建时间 |

**b. 数据库备份**
- 定时全量备份：每日凌晨自动 mysqldump
- 增量备份：binlog 实时同步（上云后由云数据库自动管理）
- 备份保留策略：日备份保留 7 天，周备份保留 4 周，月备份保留 12 个月

### 6.9 小说导出

- 支持导出格式：TXT、EPUB、PDF、Word（docx）
- 导出范围：单章 / 单卷 / 全书
- 导出内容可选：纯正文 / 正文+章节目录 / 正文+角色表+世界观设定
- TXT/EPUB 由 Java 后端直接生成
- PDF/Word 由 Python 服务生成（使用 python-docx / reportlab）
- REST API：`GET /api/projects/{projectId}/novel/export?format=epub&scope=book`
