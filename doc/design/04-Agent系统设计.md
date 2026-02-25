# Toonflow Agent 系统设计

## 一、统一 Agent 框架（BaseAgent）

三个 Agent 系统（M4 小说 / M7 大纲 / M11 分镜）共享同一套核心框架。

### 1.1 BaseAgent 抽象基类

```java
public abstract class BaseAgent {

    protected final Long projectId;
    protected final AgentEmitter emitter;           // WebSocket 事件发射
    protected final List<ChatMessage> history;       // 对话历史
    protected final Map<String, AgentTool> tools;    // 工具注册表
    protected final MessageQueue messageQueue;       // 消息队列(FIFO)
    protected final CheckpointManager checkpoint;    // 断点管理
    protected volatile AgentState state;             // idle/running/paused

    // ---- 子类必须实现 ----
    protected abstract String getAgentType();        // "novelAgent" / "outlineAgent" / "storyboardAgent"
    protected abstract Map<String, AgentTool> registerTools();
    protected abstract String buildSystemPrompt();
    protected abstract String buildContextPrompt(); // 环境上下文(项目信息/章节列表等)

    // ---- 核心流程 ----

    /** 主入口：接收用户消息 */
    public void onMessage(String userMessage) {
        if (state == AgentState.RUNNING) {
            messageQueue.enqueue(userMessage); // 排队等待
            return;
        }
        state = AgentState.RUNNING;
        try {
            history.add(ChatMessage.user(userMessage));
            executeWithToolLoop();
        } finally {
            state = AgentState.IDLE;
            processNextInQueue(); // 处理队列中下一条
        }
    }

    /** 工具调用循环：LLM → 工具调用 → LLM → ... 直到无工具调用 */
    private void executeWithToolLoop() {
        while (true) {
            var response = callLlm(buildSystemPrompt(), buildContextPrompt(), history, tools);

            // 流式输出文本部分
            response.textStream().forEach(chunk -> emitter.stream(chunk));

            // 检查是否有工具调用
            if (response.toolCalls().isEmpty()) {
                emitter.responseEnd(response.fullText());
                history.add(ChatMessage.assistant(response.fullText()));
                break;
            }

            // 执行工具调用
            for (var toolCall : response.toolCalls()) {
                emitter.toolCall(getAgentType(), toolCall.name(), toolCall.args());
                var result = tools.get(toolCall.name()).execute(toolCall.args());
                history.add(ChatMessage.toolResult(toolCall.id(), result));
            }
            // 继续循环，让 LLM 处理工具结果
        }
    }

    /** 调用 Sub-Agent */
    protected void invokeSubAgent(String subAgentName, String task, String subSystemPrompt,
                                   Map<String, AgentTool> subTools) {
        emitter.transfer(subAgentName);
        var subHistory = List.of(ChatMessage.user(task));

        while (true) {
            var response = callLlm(subSystemPrompt, buildContextPrompt(), subHistory, subTools);
            response.textStream().forEach(chunk -> emitter.subAgentStream(subAgentName, chunk));

            if (response.toolCalls().isEmpty()) {
                emitter.subAgentEnd(subAgentName);
                break;
            }

            for (var toolCall : response.toolCalls()) {
                emitter.toolCall(subAgentName, toolCall.name(), toolCall.args());
                var result = subTools.get(toolCall.name()).execute(toolCall.args());
                subHistory.add(ChatMessage.toolResult(toolCall.id(), result));
            }
        }
    }

    // ---- 生命周期 ----
    public void onConnect()    { loadHistory(); }
    public void onDisconnect() { saveHistory(); }
    public void cleanHistory() { history.clear(); saveHistory(); }

    // ---- 中断控制 ----
    public void pause()  { state = AgentState.PAUSED; checkpoint.save(); }
    public void resume() { state = AgentState.IDLE; processNextInQueue(); }
    public void cancel() { state = AgentState.IDLE; messageQueue.clear(); }
}
```

### 1.2 AgentTool 接口

```java
@FunctionalInterface
public interface AgentTool {
    Object execute(Map<String, Object> params);
}

// 工具定义（传给 LLM 的 Function Calling schema）
public record ToolDefinition(
    String name,
    String description,
    Map<String, Object> parameters  // JSON Schema
) {}
```

### 1.3 AgentEmitter（WebSocket 事件发射）

```java
public class AgentEmitter {
    private final SimpMessagingTemplate messaging;
    private final String agentType;
    private final Long projectId;

    private String topic() {
        return "/topic/agent/" + agentType + "/" + projectId;
    }

    public void stream(String text)           { send("stream", text); }
    public void responseEnd(String text)      { send("response_end", text); }
    public void subAgentStream(String agent, String text) { send("subAgentStream", Map.of("agent", agent, "text", text)); }
    public void subAgentEnd(String agent)     { send("subAgentEnd", Map.of("agent", agent)); }
    public void toolCall(String agent, String name, Object args) { send("toolCall", Map.of("agent", agent, "name", name, "args", args)); }
    public void transfer(String to)           { send("transfer", Map.of("to", to)); }
    public void refresh(String type)          { send("refresh", type); }
    public void error(String message)         { send("error", message); }

    private void send(String type, Object data) {
        messaging.convertAndSend(topic() + "/event", Map.of("type", type, "data", data));
    }
}
```

### 1.4 CheckpointManager（断点续写）

```java
public class CheckpointManager {
    // 每完成一个原子单元（一章/一集大纲/一个片段），自动保存检查点
    // 检查点存入 t_task_list.checkpoint JSON 字段
    // 内容: {currentLayer, currentStep, completedList, pendingList, lastAgentState}

    public void save();
    public Checkpoint load(Long projectId, String agentType);
    public boolean hasUnfinished(Long projectId, String agentType);
}
```

## 二、大纲故事线 Agent（M7）

### 2.1 架构

```
OutlineMainAgent extends BaseAgent
├── StorytellerAgent (AI1 故事师) — Sub-Agent
├── OutlinerAgent   (AI2 大纲师) — Sub-Agent
└── DirectorAgent   (导演)       — Sub-Agent
```

### 2.2 工具集

| 工具名 | 类型 | 描述 |
|--------|------|------|
| callStoryteller | Sub-Agent | 调用故事师 |
| callOutliner | Sub-Agent | 调用大纲师 |
| callDirector | Sub-Agent | 调用导演 |
| getChapter | 数据 | 获取小说章节(支持批量) |
| getStoryline | 数据 | 获取故事线 |
| saveStoryline | 数据 | 保存故事线 |
| deleteStoryline | 数据 | 删除故事线 |
| getOutline | 数据 | 获取大纲(simple/full) |
| saveOutline | 数据 | 保存大纲(覆盖/追加) |
| updateOutline | 数据 | 更新单集大纲 |
| deleteOutline | 数据 | 删除大纲(批量) |
| generateAssets | 数据 | 从大纲提取资产 |

### 2.3 上下文构建

```
SystemPrompt: 从 t_prompts(code=outlineScript-main) 读取
ContextPrompt:
  - 项目信息: name, type, artStyle
  - 章节列表: chapterIndex + chapter (不含全文)
  - 故事线状态: 有/无, 摘要
  - 大纲状态: 已有N集, 最新集标题
  - 可用工具列表
```

Sub-Agent 的 SystemPrompt 分别从 `outlineScript-a1`、`outlineScript-a2`、`outlineScript-director` 读取。

---

## 三、小说生成 Agent（M4）

### 3.1 架构

```
NovelMainAgent extends BaseAgent
├── WorldArchitect    (世界架构师)   — Sub-Agent
├── CharacterDesigner (角色设计师)   — Sub-Agent
├── PlotArchitect     (情节架构师)   — Sub-Agent
├── ChapterPlanner    (章节规划师)   — Sub-Agent
├── NovelWriter       (小说写手)     — Sub-Agent
├── EditorAgent       (总编审)       — Sub-Agent
└── QualityInspector  (质检官)       — Sub-Agent
```

### 3.2 工具集

**MainAgent 持有全部工具（7 个 Sub-Agent + 26 个数据工具）：**

Sub-Agent 工具:
| 工具名 | 描述 |
|--------|------|
| callWorldArchitect | 调用世界架构师 |
| callCharacterDesigner | 调用角色设计师 |
| callPlotArchitect | 调用情节架构师 |
| callChapterPlanner | 调用章节规划师 |
| callNovelWriter | 调用小说写手 |
| callEditor | 调用总编审 |
| callQualityInspector | 调用质检官 |

数据工具:
| 工具名 | 描述 |
|--------|------|
| getWorldSetting / saveWorldSetting / updateWorldSetting | 世界观 CRUD |
| getCharacters / saveCharacter / updateCharacter / deleteCharacter | 角色 CRUD |
| getNovelOutline / saveNovelOutline / updateNovelOutline | 大纲 CRUD |
| getChapterSummaries / saveChapterSummary / updateChapterSummary | 章概要 CRUD |
| getChapter / saveChapter | 章节读写 |
| updateCharacterState | 更新角色状态快照 |
| getActiveForeshadowing | 获取未回收伏笔清单 |
| getGenerationProgress | 获取生成进度 |
| saveQualityReport / getQualityReport / getQualityHistory | 质检报告 CRUD |

### 3.3 五层生成流水线

```
用户发送创作需求 → NovelMainAgent 调度

Layer 1: 世界观构建
  MainAgent → callWorldArchitect(创作需求)
  WorldArchitect 调用 saveWorldSetting 保存
  MainAgent → callEditor(审核世界观)
  通过 → 进入 Layer 2 / 打回 → WorldArchitect 修改(最多3轮)

Layer 2: 角色设计
  MainAgent → callCharacterDesigner(世界观 + 创作需求)
  CharacterDesigner 调用 saveCharacter 保存每个角色
  MainAgent → callEditor(审核角色群像)
  通过 → 进入 Layer 3

Layer 3: 全书大纲 + 分卷
  MainAgent → callPlotArchitect(世界观 + 角色 + 创作需求)
  PlotArchitect 调用 saveNovelOutline 保存
  MainAgent → callEditor(审核大纲)
  通过 → 进入 Layer 4

Layer 4: 章概要
  MainAgent → callChapterPlanner(大纲 + 角色 + 世界观, 逐卷规划)
  ChapterPlanner 调用 saveChapterSummary 保存每章概要
  MainAgent → callEditor(审核章概要)
  通过 → 进入 Layer 5

Layer 5: 逐章正文
  for each chapter:
    1. ContextAssembler 组装四层记忆 Prompt
    2. MainAgent → callNovelWriter(组装后的 Prompt)
    3. NovelWriter 流式输出正文 → emitter.chapterDelta
    4. 正文完成 → 自动生成章节摘要
    5. 自动更新角色状态快照
    6. 自动更新伏笔追踪
    7. saveChapter 存入 t_novel
    8. emitter.chapterEnd
    9. (可选) callEditor 快速审核
```

### 3.4 ContextAssembler（四层记忆上下文组装器）

这是小说 Agent 最核心的组件，负责在每章生成前组装精准的上下文。

```java
public class ContextAssembler {

    /**
     * 为指定章节组装完整的写作上下文
     * @param projectId 项目ID
     * @param chapterIndex 当前要写的章节序号
     * @return 组装好的 Prompt 字符串
     */
    public String assemble(Long projectId, int chapterIndex) {
        var plan = getChapterPlan(projectId, chapterIndex);

        // Layer A: 固定记忆（每次必带）
        String layerA = buildFixedMemory(projectId, plan);
        // - 世界观摘要 (≤2000字)
        // - 力量体系等级表 (≤500字)
        // - 全书主线一句话 (≤200字)
        // - 当前卷大纲 (≤1000字)
        // - 当前章概要 (≤500字)

        // Layer B: 角色记忆（只召回本章出场角色）
        String layerB = buildCharacterMemory(projectId, plan.characters());
        // - 根据 plan.characters 召回角色档案
        // - 每个角色: 外貌 + 性格 + 能力 + 关系网 + currentState + speechStyle
        // - 非出场角色不加载

        // Layer C: 短期记忆（前N章正文）
        String layerC = buildShortTermMemory(projectId, chapterIndex, 2);
        // - 前2章正文全文（可配置1-5章）

        // Layer D: 中长期记忆（压缩摘要）
        String layerD = buildLongTermMemory(projectId, chapterIndex);
        // - 本卷已写章摘要列表（每章100-200字）
        // - 前卷卷摘要（每卷300-500字）
        // - 活跃伏笔清单（未回收的伏笔）
        // - 角色关系变更日志（最近变化）

        // 结构化数据转 TOON 格式（省 token 30-60%）
        return ToonSerializer.serialize(layerA, layerB, layerC, layerD);
    }
}
```

### 3.5 审核循环控制

```
Editor 审核流程:
  round = 0, maxRound = 3 (可配置)

  loop:
    Editor 审核当前层产出
    if 通过:
      break → 进入下一层
    if 打回:
      round++
      if round >= maxRound:
        推送给用户: {layer, round, maxRound, allFeedback}
        等待用户决定: 强制通过 / 手动修改 / 换模型重试
        break
      else:
        将 Editor 意见累积传给 Sub-Agent
        Sub-Agent 根据意见修改
        continue loop
```

### 3.6 系统级写作质量指令

NovelWriter 的 SystemPrompt 强制注入（从 `t_prompts(code=novel-writing-quality)` 读取，不可被用户覆盖）：

- 去 AI 味指令（禁用词/句式黑名单）
- 高质量写作标准（叙事节奏/感官描写/冲突密度/钩子强度）
- 网文特化标准（爽点节奏/代入感/章末钩子分级/金手指克制）

品类特化 Prompt 从 `novel-gen-{genre}` 读取，叠加到 SystemPrompt 中。

---

## 四、分镜 Agent（M11）

### 4.1 架构

```
StoryboardMainAgent extends BaseAgent
├── SegmentAgent (片段师) — Sub-Agent
└── ShotAgent    (分镜师) — Sub-Agent
```

### 4.2 工具集

| 工具名 | 类型 | 描述 |
|--------|------|------|
| callSegmentAgent | Sub-Agent | 调用片段师 |
| callShotAgent | Sub-Agent | 调用分镜师 |
| getScript | 数据 | 获取剧本内容 |
| getAssets | 数据 | 获取项目资产 |
| getSegments | 数据 | 获取片段列表 |
| updateSegments | 数据 | 更新片段数据 |
| addShots | 数据 | 添加分镜 |
| updateShots | 数据 | 更新分镜 |
| deleteShots | 数据 | 删除分镜 |
| generateShotImage | 数据 | 触发分镜图片生成(异步) |

### 4.3 流程

```
1. 用户发送消息(如 "帮我生成分镜")
2. MainAgent → callSegmentAgent("拆分剧本为片段")
3. SegmentAgent 读取剧本 → 拆分为 Segment[] → updateSegments
4. emitter.refresh("segments")
5. MainAgent → callShotAgent("为每个片段生成镜头")
6. ShotAgent 逐片段生成 Shot[]（含镜头提示词 + 运镜指令 + 视频提示词）→ addShots
7. emitter.refresh("shots")
8. (可选) 自动触发分镜图片生成 → generateShotImage(异步)
```

分镜数据在 Agent 会话期间存内存，用户确认后通过 REST API 持久化到数据库。

---

## 五、并发与锁机制

```
同一 projectId 同一时间:
  - 只能有一个 Agent 实例执行写操作（Redis 分布式锁）
  - 读操作不受限制
  - 锁粒度: toonflow:agent:lock:{projectId}
  - 锁超时: 30分钟（长篇生成场景）
  - 防止: 用户同时在小说Agent和大纲Agent中修改同一份数据
```

## 六、LLM 调用重试策略

```
单次 LLM 调用失败:
  重试 1: 2s 后，相同参数
  重试 2: 4s 后，相同参数
  重试 3: 8s 后，相同参数
  3次都失败:
    标记当前步骤 failed
    通知用户（WebSocket error 事件）
    等待用户决定: 重试 / 跳过 / 终止 / 换模型
```

## 七、Agent 执行日志

每次 LLM 调用和工具调用都记录到 `t_agent_log`：

```java
// 自动记录，通过 AOP 或手动埋点
AgentLog log = AgentLog.builder()
    .projectId(projectId)
    .agentType("novelAgent")
    .sessionId(sessionId)
    .action("llmCall")
    .agentName("NovelWriter")
    .input(truncate(prompt, 500))
    .output(truncate(response, 500))
    .duration(elapsed)
    .tokenUsed(tokenCount)
    .status("success")
    .build();
agentLogMapper.insert(log);
```
