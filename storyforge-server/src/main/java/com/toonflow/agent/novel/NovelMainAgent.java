package com.toonflow.agent.novel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.agent.core.*;
import com.toonflow.ai.model.ToolDefinition;
import com.toonflow.ai.retry.AiRetryTemplate;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.entity.*;
import com.toonflow.mapper.*;
import com.toonflow.service.PromptService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class NovelMainAgent extends BaseAgent {

    private final PromptService promptService;
    private final NovelMapper novelMapper;
    private final NovelWorldMapper novelWorldMapper;
    private final NovelCharacterMapper novelCharacterMapper;
    private final NovelOutlineMapper novelOutlineMapper;
    private final NovelChapterPlanMapper novelChapterPlanMapper;
    private final NovelQualityReportMapper novelQualityReportMapper;
    private final NovelVersionMapper novelVersionMapper;
    private final ProjectMapper projectMapper;
    private final ContextAssembler contextAssembler;

    public NovelMainAgent(Long projectId,
                          AgentEmitter emitter,
                          CheckpointManager checkpointManager,
                          AiProviderService aiProviderService,
                          AiRetryTemplate aiRetryTemplate,
                          ChatHistoryMapper chatHistoryMapper,
                          AgentLogMapper agentLogMapper,
                          ObjectMapper objectMapper,
                          PromptService promptService,
                          NovelMapper novelMapper,
                          NovelWorldMapper novelWorldMapper,
                          NovelCharacterMapper novelCharacterMapper,
                          NovelOutlineMapper novelOutlineMapper,
                          NovelChapterPlanMapper novelChapterPlanMapper,
                          NovelQualityReportMapper novelQualityReportMapper,
                          NovelVersionMapper novelVersionMapper,
                          ProjectMapper projectMapper,
                          ContextAssembler contextAssembler) {
        super(projectId, emitter, checkpointManager, aiProviderService,
                aiRetryTemplate, chatHistoryMapper, agentLogMapper, objectMapper);
        this.promptService = promptService;
        this.novelMapper = novelMapper;
        this.novelWorldMapper = novelWorldMapper;
        this.novelCharacterMapper = novelCharacterMapper;
        this.novelOutlineMapper = novelOutlineMapper;
        this.novelChapterPlanMapper = novelChapterPlanMapper;
        this.novelQualityReportMapper = novelQualityReportMapper;
        this.novelVersionMapper = novelVersionMapper;
        this.projectMapper = projectMapper;
        this.contextAssembler = contextAssembler;
    }

    @Override
    protected String getAgentType() {
        return "novelAgent";
    }

    @Override
    protected String getAiFunctionKey() {
        return "novel-main";
    }

    @Override
    protected String buildSystemPrompt() {
        return promptService.getPromptValue("novel-main");
    }

    @Override
    protected String buildContextPrompt() {
        StringBuilder sb = new StringBuilder();

        // 项目信息
        Project project = projectMapper.selectById(projectId);
        if (project != null) {
            sb.append("【项目信息】\n");
            sb.append("名称: ").append(project.getName()).append("\n");
            sb.append("类型: ").append(project.getType()).append("\n");
            sb.append("美术风格: ").append(project.getArtStyle()).append("\n\n");
        }

        // 世界观状态
        NovelWorld world = novelWorldMapper.selectOne(
                new LambdaQueryWrapper<NovelWorld>()
                        .eq(NovelWorld::getProjectId, projectId));
        if (world != null) {
            sb.append("【世界观】已创建\n\n");
        } else {
            sb.append("【世界观】暂无\n\n");
        }

        // 角色数量
        Long charCount = novelCharacterMapper.selectCount(
                new LambdaQueryWrapper<NovelCharacter>()
                        .eq(NovelCharacter::getProjectId, projectId));
        sb.append("【角色】").append(charCount).append(" 个\n\n");

        // 大纲状态
        List<NovelOutline> outlines = novelOutlineMapper.selectList(
                new LambdaQueryWrapper<NovelOutline>()
                        .eq(NovelOutline::getProjectId, projectId)
                        .orderByAsc(NovelOutline::getVolumeIndex));
        if (!outlines.isEmpty()) {
            sb.append("【大纲】").append(outlines.size()).append(" 卷\n");
            for (NovelOutline o : outlines) {
                sb.append("  第").append(o.getVolumeIndex()).append("卷: ").append(o.getVolumeName())
                  .append(" (第").append(o.getStartChapter()).append("-").append(o.getEndChapter()).append("章)\n");
            }
            sb.append("\n");
        } else {
            sb.append("【大纲】暂无\n\n");
        }

        // 章概要状态
        Long planCount = novelChapterPlanMapper.selectCount(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getProjectId, projectId));
        sb.append("【章概要】").append(planCount).append(" 章\n\n");

        // 已生成章节
        List<Novel> novels = novelMapper.selectList(
                new LambdaQueryWrapper<Novel>()
                        .eq(Novel::getProjectId, projectId)
                        .select(Novel::getChapterIndex, Novel::getChapter, Novel::getChapterData)
                        .orderByAsc(Novel::getChapterIndex));
        int totalWords = novels.stream()
                .mapToInt(n -> n.getChapterData() != null ? n.getChapterData().length() : 0)
                .sum();
        sb.append("【已生成章节】").append(novels.size()).append(" 章，共 ").append(totalWords).append(" 字\n");

        return sb.toString();
    }

    @Override
    protected void registerTools(ToolRegistry registry) {
        // ---- 世界观工具 (3) ----
        registerGetWorldSetting(registry);
        registerSaveWorldSetting(registry);
        registerUpdateWorldSetting(registry);
        // ---- 角色工具 (5) ----
        registerGetCharacters(registry);
        registerSaveCharacter(registry);
        registerUpdateCharacter(registry);
        registerDeleteCharacter(registry);
        registerUpdateCharacterState(registry);
        // ---- 大纲工具 (3) ----
        registerGetNovelOutline(registry);
        registerSaveNovelOutline(registry);
        registerUpdateNovelOutline(registry);
        // ---- 章概要工具 (3) ----
        registerGetChapterSummaries(registry);
        registerSaveChapterSummary(registry);
        registerUpdateChapterSummary(registry);
        // ---- 章节正文工具 (4) ----
        registerGetChapter(registry);
        registerSaveChapter(registry);
        registerGetPreviousChapters(registry);
        registerGetChapterPlan(registry);
        // ---- 伏笔与进度工具 (2) ----
        registerGetActiveForeshadowing(registry);
        registerGetGenerationProgress(registry);
        // ---- 质检报告工具 (3) ----
        registerSaveQualityReport(registry);
        registerGetQualityReport(registry);
        registerGetQualityHistory(registry);
        // ---- Sub-Agent 工具 (7) ----
        registerCallWorldArchitect(registry);
        registerCallCharacterDesigner(registry);
        registerCallPlotArchitect(registry);
        registerCallChapterPlanner(registry);
        registerCallNovelWriter(registry);
        registerCallEditor(registry);
        registerCallQualityInspector(registry);
    }

    // ========== 世界观工具 ==========

    private void registerGetWorldSetting(ToolRegistry registry) {
        registry.register("getWorldSetting", params -> {
            NovelWorld w = novelWorldMapper.selectOne(
                    new LambdaQueryWrapper<NovelWorld>()
                            .eq(NovelWorld::getProjectId, projectId));
            if (w == null) return Map.of();
            Map<String, Object> result = new HashMap<>();
            result.put("id", w.getId());
            result.put("background", w.getBackground());
            result.put("powerSystem", w.getPowerSystem());
            result.put("socialStructure", w.getSocialStructure());
            result.put("coreRules", w.getCoreRules());
            result.put("taboos", w.getTaboos());
            return result;
        }, ToolDefinition.builder()
                .name("getWorldSetting")
                .description("获取当前项目的世界观设定（背景、力量体系、社会结构、核心规则、禁忌）")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    private void registerSaveWorldSetting(ToolRegistry registry) {
        registry.register("saveWorldSetting", params -> {
            NovelWorld existing = novelWorldMapper.selectOne(
                    new LambdaQueryWrapper<NovelWorld>()
                            .eq(NovelWorld::getProjectId, projectId));
            if (existing != null) {
                existing.setBackground((String) params.get("background"));
                existing.setPowerSystem((String) params.get("powerSystem"));
                existing.setSocialStructure((String) params.get("socialStructure"));
                existing.setCoreRules((String) params.get("coreRules"));
                existing.setTaboos((String) params.get("taboos"));
                novelWorldMapper.updateById(existing);
            } else {
                NovelWorld w = new NovelWorld();
                w.setProjectId(projectId);
                w.setBackground((String) params.get("background"));
                w.setPowerSystem((String) params.get("powerSystem"));
                w.setSocialStructure((String) params.get("socialStructure"));
                w.setCoreRules((String) params.get("coreRules"));
                w.setTaboos((String) params.get("taboos"));
                w.setState(1);
                novelWorldMapper.insert(w);
            }
            emitter.refresh("world");
            return "ok";
        }, ToolDefinition.builder()
                .name("saveWorldSetting")
                .description("保存或覆盖世界观设定")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "background", Map.of("type", "string", "description", "世界背景描述"),
                                "powerSystem", Map.of("type", "string", "description", "力量体系（JSON或文本）"),
                                "socialStructure", Map.of("type", "string", "description", "社会结构（JSON或文本）"),
                                "coreRules", Map.of("type", "string", "description", "核心规则（JSON或文本）"),
                                "taboos", Map.of("type", "string", "description", "禁忌设定（JSON或文本）")),
                        "required", List.of("background")))
                .build());
    }

    private void registerUpdateWorldSetting(ToolRegistry registry) {
        registry.register("updateWorldSetting", params -> {
            NovelWorld w = novelWorldMapper.selectOne(
                    new LambdaQueryWrapper<NovelWorld>()
                            .eq(NovelWorld::getProjectId, projectId));
            if (w == null) return "error: world setting not found";
            if (params.containsKey("background")) w.setBackground((String) params.get("background"));
            if (params.containsKey("powerSystem")) w.setPowerSystem((String) params.get("powerSystem"));
            if (params.containsKey("socialStructure")) w.setSocialStructure((String) params.get("socialStructure"));
            if (params.containsKey("coreRules")) w.setCoreRules((String) params.get("coreRules"));
            if (params.containsKey("taboos")) w.setTaboos((String) params.get("taboos"));
            novelWorldMapper.updateById(w);
            emitter.refresh("world");
            return "ok";
        }, ToolDefinition.builder()
                .name("updateWorldSetting")
                .description("更新世界观的部分字段")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "background", Map.of("type", "string", "description", "世界背景"),
                                "powerSystem", Map.of("type", "string", "description", "力量体系"),
                                "socialStructure", Map.of("type", "string", "description", "社会结构"),
                                "coreRules", Map.of("type", "string", "description", "核心规则"),
                                "taboos", Map.of("type", "string", "description", "禁忌设定"))))
                .build());
    }

    // ========== 角色工具 ==========

    private void registerGetCharacters(ToolRegistry registry) {
        registry.register("getCharacters", params -> {
            LambdaQueryWrapper<NovelCharacter> qw = new LambdaQueryWrapper<NovelCharacter>()
                    .eq(NovelCharacter::getProjectId, projectId);
            String name = (String) params.get("name");
            if (name != null && !name.isEmpty()) {
                qw.like(NovelCharacter::getName, name);
            }
            List<NovelCharacter> list = novelCharacterMapper.selectList(qw);
            return list.stream().map(this::characterToMap).collect(Collectors.toList());
        }, ToolDefinition.builder()
                .name("getCharacters")
                .description("获取角色列表，可按名称筛选")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string", "description", "角色名称（可选，模糊匹配）"))))
                .build());
    }

    private void registerSaveCharacter(ToolRegistry registry) {
        registry.register("saveCharacter", params -> {
            NovelCharacter c = new NovelCharacter();
            c.setProjectId(projectId);
            c.setName((String) params.get("name"));
            c.setRole((String) params.get("role"));
            if (params.get("age") instanceof Number num) c.setAge(num.intValue());
            c.setAppearance((String) params.get("appearance"));
            c.setPersonality((String) params.get("personality"));
            c.setAbility((String) params.get("ability"));
            c.setRelationships(toJsonString(params.get("relationships")));
            c.setGrowthArc((String) params.get("growthArc"));
            c.setSpeechStyle(toJsonString(params.get("speechStyle")));
            c.setState(1);
            novelCharacterMapper.insert(c);
            emitter.refresh("character");
            return "ok";
        }, ToolDefinition.builder()
                .name("saveCharacter")
                .description("新增一个角色")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string", "description", "角色名"),
                                "role", Map.of("type", "string", "description", "角色类型: protagonist/supporting/antagonist"),
                                "age", Map.of("type", "integer", "description", "年龄"),
                                "appearance", Map.of("type", "string", "description", "外貌描述"),
                                "personality", Map.of("type", "string", "description", "性格描述"),
                                "ability", Map.of("type", "string", "description", "能力描述"),
                                "relationships", Map.of("type", "object", "description", "关系网 JSON"),
                                "growthArc", Map.of("type", "string", "description", "成长弧线"),
                                "speechStyle", Map.of("type", "object", "description", "说话风格 JSON")),
                        "required", List.of("name", "role")))
                .build());
    }

    private void registerUpdateCharacter(ToolRegistry registry) {
        registry.register("updateCharacter", params -> {
            Long characterId = ((Number) params.get("characterId")).longValue();
            NovelCharacter c = novelCharacterMapper.selectById(characterId);
            if (c == null) return "error: character not found";
            if (params.containsKey("name")) c.setName((String) params.get("name"));
            if (params.containsKey("role")) c.setRole((String) params.get("role"));
            if (params.get("age") instanceof Number num) c.setAge(num.intValue());
            if (params.containsKey("appearance")) c.setAppearance((String) params.get("appearance"));
            if (params.containsKey("personality")) c.setPersonality((String) params.get("personality"));
            if (params.containsKey("ability")) c.setAbility((String) params.get("ability"));
            if (params.containsKey("relationships")) c.setRelationships(toJsonString(params.get("relationships")));
            if (params.containsKey("growthArc")) c.setGrowthArc((String) params.get("growthArc"));
            if (params.containsKey("speechStyle")) c.setSpeechStyle(toJsonString(params.get("speechStyle")));
            novelCharacterMapper.updateById(c);
            emitter.refresh("character");
            return "ok";
        }, ToolDefinition.builder()
                .name("updateCharacter")
                .description("更新角色信息")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "characterId", Map.of("type", "integer", "description", "角色ID"),
                                "name", Map.of("type", "string"), "role", Map.of("type", "string"),
                                "age", Map.of("type", "integer"), "appearance", Map.of("type", "string"),
                                "personality", Map.of("type", "string"), "ability", Map.of("type", "string"),
                                "relationships", Map.of("type", "object"), "growthArc", Map.of("type", "string"),
                                "speechStyle", Map.of("type", "object")),
                        "required", List.of("characterId")))
                .build());
    }

    private void registerDeleteCharacter(ToolRegistry registry) {
        registry.register("deleteCharacter", params -> {
            Long characterId = ((Number) params.get("characterId")).longValue();
            novelCharacterMapper.deleteById(characterId);
            emitter.refresh("character");
            return "ok";
        }, ToolDefinition.builder()
                .name("deleteCharacter")
                .description("删除角色")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "characterId", Map.of("type", "integer", "description", "角色ID")),
                        "required", List.of("characterId")))
                .build());
    }

    private void registerUpdateCharacterState(ToolRegistry registry) {
        registry.register("updateCharacterState", params -> {
            Long characterId = ((Number) params.get("characterId")).longValue();
            NovelCharacter c = novelCharacterMapper.selectById(characterId);
            if (c == null) return "error: character not found";
            c.setCurrentState(toJsonString(params.get("currentState")));
            novelCharacterMapper.updateById(c);
            emitter.refresh("character");
            return "ok";
        }, ToolDefinition.builder()
                .name("updateCharacterState")
                .description("更新角色当前状态快照（位置、伤势、情绪、能力等级、持有物品、已知信息等）")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "characterId", Map.of("type", "integer", "description", "角色ID"),
                                "currentState", Map.of("type", "object", "description", "当前状态JSON")),
                        "required", List.of("characterId", "currentState")))
                .build());
    }

    // ========== 大纲工具 ==========

    private void registerGetNovelOutline(ToolRegistry registry) {
        registry.register("getNovelOutline", params -> {
            LambdaQueryWrapper<NovelOutline> qw = new LambdaQueryWrapper<NovelOutline>()
                    .eq(NovelOutline::getProjectId, projectId)
                    .orderByAsc(NovelOutline::getVolumeIndex);
            if (params.get("volumeIndex") instanceof Number num) {
                qw.eq(NovelOutline::getVolumeIndex, num.intValue());
            }
            return novelOutlineMapper.selectList(qw);
        }, ToolDefinition.builder()
                .name("getNovelOutline")
                .description("获取全书大纲（含分卷结构），可按卷筛选")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "volumeIndex", Map.of("type", "integer", "description", "卷序号（可选）"))))
                .build());
    }

    private void registerSaveNovelOutline(ToolRegistry registry) {
        registry.register("saveNovelOutline", params -> {
            String mainPlot = (String) params.get("mainPlot");
            String theme = (String) params.get("theme");
            List<?> volumesRaw = (List<?>) params.get("volumes");

            // 删除已有大纲
            novelOutlineMapper.delete(
                    new LambdaQueryWrapper<NovelOutline>()
                            .eq(NovelOutline::getProjectId, projectId));

            if (volumesRaw != null) {
                for (int i = 0; i < volumesRaw.size(); i++) {
                    Map<?, ?> vol = objectMapper.convertValue(volumesRaw.get(i), Map.class);
                    NovelOutline o = new NovelOutline();
                    o.setProjectId(projectId);
                    o.setMainPlot(mainPlot);
                    o.setTheme(theme);
                    o.setVolumeIndex(i + 1);
                    o.setVolumeName((String) vol.get("volumeName"));
                    o.setVolumePlot((String) vol.get("volumePlot"));
                    if (vol.get("startChapter") instanceof Number n) o.setStartChapter(n.intValue());
                    if (vol.get("endChapter") instanceof Number n) o.setEndChapter(n.intValue());
                    o.setVolumeClimax((String) vol.get("volumeClimax"));
                    o.setVolumeCliffhanger((String) vol.get("volumeCliffhanger"));
                    o.setState(1);
                    novelOutlineMapper.insert(o);
                }
            }
            emitter.refresh("novelOutline");
            return "ok";
        }, ToolDefinition.builder()
                .name("saveNovelOutline")
                .description("保存全书大纲和分卷结构（覆盖已有）")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "mainPlot", Map.of("type", "string", "description", "全书主线"),
                                "theme", Map.of("type", "string", "description", "主题"),
                                "volumes", Map.of("type", "array", "description", "分卷数组",
                                        "items", Map.of("type", "object"))),
                        "required", List.of("mainPlot", "volumes")))
                .build());
    }

    private void registerUpdateNovelOutline(ToolRegistry registry) {
        registry.register("updateNovelOutline", params -> {
            Long outlineId = ((Number) params.get("outlineId")).longValue();
            NovelOutline o = novelOutlineMapper.selectById(outlineId);
            if (o == null) return "error: outline not found";
            if (params.containsKey("mainPlot")) o.setMainPlot((String) params.get("mainPlot"));
            if (params.containsKey("theme")) o.setTheme((String) params.get("theme"));
            if (params.containsKey("volumeName")) o.setVolumeName((String) params.get("volumeName"));
            if (params.containsKey("volumePlot")) o.setVolumePlot((String) params.get("volumePlot"));
            if (params.get("startChapter") instanceof Number n) o.setStartChapter(n.intValue());
            if (params.get("endChapter") instanceof Number n) o.setEndChapter(n.intValue());
            if (params.containsKey("volumeClimax")) o.setVolumeClimax((String) params.get("volumeClimax"));
            if (params.containsKey("volumeCliffhanger")) o.setVolumeCliffhanger((String) params.get("volumeCliffhanger"));
            novelOutlineMapper.updateById(o);
            emitter.refresh("novelOutline");
            return "ok";
        }, ToolDefinition.builder()
                .name("updateNovelOutline")
                .description("更新大纲/卷结构的部分字段")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "outlineId", Map.of("type", "integer", "description", "大纲记录ID"),
                                "mainPlot", Map.of("type", "string"), "theme", Map.of("type", "string"),
                                "volumeName", Map.of("type", "string"), "volumePlot", Map.of("type", "string"),
                                "startChapter", Map.of("type", "integer"), "endChapter", Map.of("type", "integer"),
                                "volumeClimax", Map.of("type", "string"), "volumeCliffhanger", Map.of("type", "string")),
                        "required", List.of("outlineId")))
                .build());
    }

    // ========== 章概要工具 ==========

    private void registerGetChapterSummaries(ToolRegistry registry) {
        registry.register("getChapterSummaries", params -> {
            LambdaQueryWrapper<NovelChapterPlan> qw = new LambdaQueryWrapper<NovelChapterPlan>()
                    .eq(NovelChapterPlan::getProjectId, projectId)
                    .orderByAsc(NovelChapterPlan::getChapterIndex);
            if (params.get("volumeIndex") instanceof Number num) {
                qw.eq(NovelChapterPlan::getVolumeIndex, num.intValue());
            }
            return novelChapterPlanMapper.selectList(qw);
        }, ToolDefinition.builder()
                .name("getChapterSummaries")
                .description("获取章概要列表，可按卷筛选")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "volumeIndex", Map.of("type", "integer", "description", "卷序号（可选）"))))
                .build());
    }

    private void registerSaveChapterSummary(ToolRegistry registry) {
        registry.register("saveChapterSummary", params -> {
            NovelChapterPlan p = new NovelChapterPlan();
            p.setProjectId(projectId);
            if (params.get("volumeIndex") instanceof Number n) p.setVolumeIndex(n.intValue());
            if (params.get("chapterIndex") instanceof Number n) p.setChapterIndex(n.intValue());
            p.setTitle((String) params.get("title"));
            p.setSummary((String) params.get("summary"));
            p.setKeyEvents(toJsonString(params.get("keyEvents")));
            p.setCharacters(toJsonString(params.get("characters")));
            p.setEmotionCurve((String) params.get("emotionCurve"));
            p.setForeshadowing(toJsonString(params.get("foreshadowing")));
            p.setPayoff(toJsonString(params.get("payoff")));
            p.setCliffhanger((String) params.get("cliffhanger"));
            if (params.get("wordTarget") instanceof Number n) p.setWordTarget(n.intValue());
            p.setState(1);
            novelChapterPlanMapper.insert(p);
            emitter.refresh("chapterPlan");
            return "ok";
        }, ToolDefinition.builder()
                .name("saveChapterSummary")
                .description("保存一章的概要")
                .parameters(buildSaveChapterSummarySchema())
                .build());
    }

    private void registerUpdateChapterSummary(ToolRegistry registry) {
        registry.register("updateChapterSummary", params -> {
            Long planId = ((Number) params.get("planId")).longValue();
            NovelChapterPlan p = novelChapterPlanMapper.selectById(planId);
            if (p == null) return "error: chapter plan not found";
            if (params.containsKey("title")) p.setTitle((String) params.get("title"));
            if (params.containsKey("summary")) p.setSummary((String) params.get("summary"));
            if (params.containsKey("keyEvents")) p.setKeyEvents(toJsonString(params.get("keyEvents")));
            if (params.containsKey("characters")) p.setCharacters(toJsonString(params.get("characters")));
            if (params.containsKey("emotionCurve")) p.setEmotionCurve((String) params.get("emotionCurve"));
            if (params.containsKey("foreshadowing")) p.setForeshadowing(toJsonString(params.get("foreshadowing")));
            if (params.containsKey("payoff")) p.setPayoff(toJsonString(params.get("payoff")));
            if (params.containsKey("cliffhanger")) p.setCliffhanger((String) params.get("cliffhanger"));
            if (params.get("wordTarget") instanceof Number n) p.setWordTarget(n.intValue());
            novelChapterPlanMapper.updateById(p);
            emitter.refresh("chapterPlan");
            return "ok";
        }, ToolDefinition.builder()
                .name("updateChapterSummary")
                .description("更新章概要的部分字段")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "planId", Map.of("type", "integer", "description", "章概要记录ID"),
                                "title", Map.of("type", "string"), "summary", Map.of("type", "string"),
                                "keyEvents", Map.of("type", "array", "items", Map.of("type", "string")),
                                "characters", Map.of("type", "array", "items", Map.of("type", "string")),
                                "emotionCurve", Map.of("type", "string"),
                                "foreshadowing", Map.of("type", "array", "items", Map.of("type", "string")),
                                "payoff", Map.of("type", "array", "items", Map.of("type", "string")),
                                "cliffhanger", Map.of("type", "string"),
                                "wordTarget", Map.of("type", "integer")),
                        "required", List.of("planId")))
                .build());
    }

    // ========== 章节正文工具 ==========

    private void registerGetChapter(ToolRegistry registry) {
        registry.register("getChapter", params -> {
            Object idx = params.get("chapterIndex");
            List<Integer> indices = new ArrayList<>();
            if (idx instanceof List<?> list) {
                for (Object item : list) indices.add(((Number) item).intValue());
            } else if (idx instanceof Number num) {
                indices.add(num.intValue());
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Integer chapterIndex : indices) {
                Novel novel = novelMapper.selectOne(
                        new LambdaQueryWrapper<Novel>()
                                .eq(Novel::getProjectId, projectId)
                                .eq(Novel::getChapterIndex, chapterIndex));
                if (novel != null) {
                    result.add(Map.of(
                            "chapterIndex", novel.getChapterIndex(),
                            "volumeIndex", novel.getVolumeIndex() != null ? novel.getVolumeIndex() : 1,
                            "chapter", novel.getChapter() != null ? novel.getChapter() : "",
                            "chapterData", novel.getChapterData() != null ? novel.getChapterData() : "",
                            "summary", novel.getSummary() != null ? novel.getSummary() : ""));
                }
            }
            return result;
        }, ToolDefinition.builder()
                .name("getChapter")
                .description("获取小说章节全文，支持单章或批量查询")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "chapterIndex", Map.of(
                                        "description", "章节序号，可以是单个数字或数字数组",
                                        "oneOf", List.of(
                                                Map.of("type", "integer"),
                                                Map.of("type", "array", "items", Map.of("type", "integer"))))),
                        "required", List.of("chapterIndex")))
                .build());
    }

    private void registerSaveChapter(ToolRegistry registry) {
        registry.register("saveChapter", params -> {
            int chapterIndex = ((Number) params.get("chapterIndex")).intValue();
            int volumeIndex = params.get("volumeIndex") instanceof Number n ? n.intValue() : 1;
            String reel = (String) params.get("reel");
            String chapter = (String) params.get("chapter");
            String chapterData = (String) params.get("chapterData");
            String summary = (String) params.get("summary");

            // 查找已有章节
            Novel existing = novelMapper.selectOne(
                    new LambdaQueryWrapper<Novel>()
                            .eq(Novel::getProjectId, projectId)
                            .eq(Novel::getChapterIndex, chapterIndex));

            if (existing != null) {
                // 保存旧版本到 t_novel_version
                NovelVersion version = new NovelVersion();
                version.setNovelId(existing.getId());
                version.setProjectId(projectId);
                version.setChapterIndex(chapterIndex);
                version.setChapterData(existing.getChapterData());
                version.setSummary(existing.getSummary());
                version.setSource("ai");
                // 计算版本号
                Long versionCount = novelVersionMapper.selectCount(
                        new LambdaQueryWrapper<NovelVersion>()
                                .eq(NovelVersion::getNovelId, existing.getId()));
                version.setVersion(versionCount.intValue() + 1);
                novelVersionMapper.insert(version);

                // 更新现有章节
                existing.setVolumeIndex(volumeIndex);
                existing.setReel(reel);
                existing.setChapter(chapter);
                existing.setChapterData(chapterData);
                existing.setSummary(summary);
                novelMapper.updateById(existing);
            } else {
                // 新增章节
                Novel novel = new Novel();
                novel.setProjectId(projectId);
                novel.setChapterIndex(chapterIndex);
                novel.setVolumeIndex(volumeIndex);
                novel.setReel(reel);
                novel.setChapter(chapter);
                novel.setChapterData(chapterData);
                novel.setSummary(summary);
                novelMapper.insert(novel);
            }
            emitter.refresh("chapter");
            return "ok";
        }, ToolDefinition.builder()
                .name("saveChapter")
                .description("保存章节正文到数据库，已有章节会创建版本记录后覆盖")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "chapterIndex", Map.of("type", "integer", "description", "章节序号"),
                                "volumeIndex", Map.of("type", "integer", "description", "卷序号"),
                                "reel", Map.of("type", "string", "description", "分卷名"),
                                "chapter", Map.of("type", "string", "description", "章节名"),
                                "chapterData", Map.of("type", "string", "description", "章节正文"),
                                "summary", Map.of("type", "string", "description", "章节摘要JSON")),
                        "required", List.of("chapterIndex", "chapter", "chapterData")))
                .build());
    }

    private void registerGetPreviousChapters(ToolRegistry registry) {
        registry.register("getPreviousChapters", params -> {
            int chapterIndex = ((Number) params.get("chapterIndex")).intValue();
            int count = params.get("count") instanceof Number n ? n.intValue() : 2;
            int fromIndex = Math.max(1, chapterIndex - count);
            List<Novel> chapters = novelMapper.selectList(
                    new LambdaQueryWrapper<Novel>()
                            .eq(Novel::getProjectId, projectId)
                            .ge(Novel::getChapterIndex, fromIndex)
                            .lt(Novel::getChapterIndex, chapterIndex)
                            .orderByAsc(Novel::getChapterIndex));
            return chapters.stream().map(n -> Map.of(
                    "chapterIndex", n.getChapterIndex(),
                    "chapter", n.getChapter() != null ? n.getChapter() : "",
                    "chapterData", n.getChapterData() != null ? n.getChapterData() : ""
            )).collect(Collectors.toList());
        }, ToolDefinition.builder()
                .name("getPreviousChapters")
                .description("获取前N章正文全文（用于短期记忆上下文）")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "chapterIndex", Map.of("type", "integer", "description", "当前章节序号"),
                                "count", Map.of("type", "integer", "description", "获取前几章，默认2")),
                        "required", List.of("chapterIndex")))
                .build());
    }

    private void registerGetChapterPlan(ToolRegistry registry) {
        registry.register("getChapterPlan", params -> {
            int chapterIndex = ((Number) params.get("chapterIndex")).intValue();
            NovelChapterPlan plan = novelChapterPlanMapper.selectOne(
                    new LambdaQueryWrapper<NovelChapterPlan>()
                            .eq(NovelChapterPlan::getProjectId, projectId)
                            .eq(NovelChapterPlan::getChapterIndex, chapterIndex));
            if (plan == null) return Map.of();
            return plan;
        }, ToolDefinition.builder()
                .name("getChapterPlan")
                .description("获取指定章的概要详情")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "chapterIndex", Map.of("type", "integer", "description", "章节序号")),
                        "required", List.of("chapterIndex")))
                .build());
    }

    // ========== 伏笔与进度工具 ==========

    private void registerGetActiveForeshadowing(ToolRegistry registry) {
        registry.register("getActiveForeshadowing", params -> {
            // 获取最大章节序号
            List<Novel> novels = novelMapper.selectList(
                    new LambdaQueryWrapper<Novel>()
                            .eq(Novel::getProjectId, projectId)
                            .select(Novel::getChapterIndex)
                            .orderByDesc(Novel::getChapterIndex)
                            .last("LIMIT 1"));
            int maxChapter = novels.isEmpty() ? Integer.MAX_VALUE : novels.get(0).getChapterIndex() + 1;
            return contextAssembler.getActiveForeshadowing(projectId, maxChapter);
        }, ToolDefinition.builder()
                .name("getActiveForeshadowing")
                .description("获取当前未回收的伏笔清单")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    private void registerGetGenerationProgress(ToolRegistry registry) {
        registry.register("getGenerationProgress", params -> {
            Map<String, Object> progress = new HashMap<>();

            // 世界观
            Long worldCount = novelWorldMapper.selectCount(
                    new LambdaQueryWrapper<NovelWorld>().eq(NovelWorld::getProjectId, projectId));
            progress.put("worldCreated", worldCount > 0);

            // 角色数
            Long charCount = novelCharacterMapper.selectCount(
                    new LambdaQueryWrapper<NovelCharacter>().eq(NovelCharacter::getProjectId, projectId));
            progress.put("characterCount", charCount);

            // 大纲卷数
            Long volumeCount = novelOutlineMapper.selectCount(
                    new LambdaQueryWrapper<NovelOutline>().eq(NovelOutline::getProjectId, projectId));
            progress.put("volumeCount", volumeCount);

            // 章概要数
            Long planCount = novelChapterPlanMapper.selectCount(
                    new LambdaQueryWrapper<NovelChapterPlan>().eq(NovelChapterPlan::getProjectId, projectId));
            progress.put("chapterPlanCount", planCount);

            // 已生成章节数和总字数
            List<Novel> novels = novelMapper.selectList(
                    new LambdaQueryWrapper<Novel>()
                            .eq(Novel::getProjectId, projectId)
                            .select(Novel::getChapterData));
            progress.put("completedChapters", novels.size());
            int totalWords = novels.stream()
                    .mapToInt(n -> n.getChapterData() != null ? n.getChapterData().length() : 0)
                    .sum();
            progress.put("totalWords", totalWords);

            return progress;
        }, ToolDefinition.builder()
                .name("getGenerationProgress")
                .description("获取当前项目的生成进度统计")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    // ========== 质检报告工具 ==========

    private void registerSaveQualityReport(ToolRegistry registry) {
        registry.register("saveQualityReport", params -> {
            NovelQualityReport r = new NovelQualityReport();
            r.setProjectId(projectId);
            r.setScope((String) params.get("scope"));
            if (params.get("scopeIndex") instanceof Number n) r.setScopeIndex(n.intValue());
            if (params.get("overallScore") instanceof Number n) r.setOverallScore(n.intValue());
            r.setDimensions(toJsonString(params.get("dimensions")));
            r.setSummary((String) params.get("summary"));
            r.setAutoFixSuggestions(toJsonString(params.get("autoFixSuggestions")));
            r.setState("completed");
            novelQualityReportMapper.insert(r);
            emitter.refresh("qualityReport");
            return "ok";
        }, ToolDefinition.builder()
                .name("saveQualityReport")
                .description("保存质检报告")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "scope", Map.of("type", "string", "description", "质检范围: chapter/volume/book"),
                                "scopeIndex", Map.of("type", "integer", "description", "范围索引（章号或卷号）"),
                                "overallScore", Map.of("type", "integer", "description", "总分(0-100)"),
                                "dimensions", Map.of("type", "object", "description", "7维度评分JSON"),
                                "summary", Map.of("type", "string", "description", "质检总结"),
                                "autoFixSuggestions", Map.of("type", "array", "description", "自动修复建议",
                                        "items", Map.of("type", "object"))),
                        "required", List.of("scope", "overallScore", "summary")))
                .build());
    }

    private void registerGetQualityReport(ToolRegistry registry) {
        registry.register("getQualityReport", params -> {
            LambdaQueryWrapper<NovelQualityReport> qw = new LambdaQueryWrapper<NovelQualityReport>()
                    .eq(NovelQualityReport::getProjectId, projectId)
                    .orderByDesc(NovelQualityReport::getCreateTime);
            if (params.get("scope") instanceof String scope) {
                qw.eq(NovelQualityReport::getScope, scope);
            }
            if (params.get("scopeIndex") instanceof Number n) {
                qw.eq(NovelQualityReport::getScopeIndex, n.intValue());
            }
            return novelQualityReportMapper.selectList(qw);
        }, ToolDefinition.builder()
                .name("getQualityReport")
                .description("获取质检报告，可按范围和索引筛选")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "scope", Map.of("type", "string", "description", "质检范围: chapter/volume/book"),
                                "scopeIndex", Map.of("type", "integer", "description", "范围索引"))))
                .build());
    }

    private void registerGetQualityHistory(ToolRegistry registry) {
        registry.register("getQualityHistory", params -> {
            return novelQualityReportMapper.selectList(
                    new LambdaQueryWrapper<NovelQualityReport>()
                            .eq(NovelQualityReport::getProjectId, projectId)
                            .select(NovelQualityReport::getId, NovelQualityReport::getScope,
                                    NovelQualityReport::getScopeIndex, NovelQualityReport::getOverallScore,
                                    NovelQualityReport::getSummary, NovelQualityReport::getCreateTime)
                            .orderByDesc(NovelQualityReport::getCreateTime));
        }, ToolDefinition.builder()
                .name("getQualityHistory")
                .description("获取所有历次质检记录列表")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    // ========== Sub-Agent 工具 ==========

    private ToolRegistry buildWorldArchitectTools() {
        ToolRegistry sub = new ToolRegistry();
        registerSaveWorldSetting(sub);
        registerGetWorldSetting(sub);
        registerUpdateWorldSetting(sub);
        return sub;
    }

    private ToolRegistry buildCharacterDesignerTools() {
        ToolRegistry sub = new ToolRegistry();
        registerSaveCharacter(sub);
        registerGetCharacters(sub);
        registerUpdateCharacter(sub);
        registerDeleteCharacter(sub);
        registerGetWorldSetting(sub);
        return sub;
    }

    private ToolRegistry buildPlotArchitectTools() {
        ToolRegistry sub = new ToolRegistry();
        registerSaveNovelOutline(sub);
        registerGetNovelOutline(sub);
        registerUpdateNovelOutline(sub);
        registerGetWorldSetting(sub);
        registerGetCharacters(sub);
        return sub;
    }

    private ToolRegistry buildChapterPlannerTools() {
        ToolRegistry sub = new ToolRegistry();
        registerSaveChapterSummary(sub);
        registerGetChapterSummaries(sub);
        registerUpdateChapterSummary(sub);
        registerGetNovelOutline(sub);
        registerGetCharacters(sub);
        registerGetWorldSetting(sub);
        return sub;
    }

    private ToolRegistry buildNovelWriterTools() {
        ToolRegistry sub = new ToolRegistry();
        registerGetChapterPlan(sub);
        registerGetPreviousChapters(sub);
        registerGetChapterSummaries(sub);
        registerGetCharacters(sub);
        registerGetWorldSetting(sub);
        registerSaveChapter(sub);
        return sub;
    }

    private ToolRegistry buildEditorTools() {
        ToolRegistry sub = new ToolRegistry();
        // 所有读取工具
        registerGetWorldSetting(sub);
        registerGetCharacters(sub);
        registerGetNovelOutline(sub);
        registerGetChapterSummaries(sub);
        registerGetChapter(sub);
        registerGetPreviousChapters(sub);
        registerGetChapterPlan(sub);
        registerGetActiveForeshadowing(sub);
        registerGetGenerationProgress(sub);
        registerGetQualityReport(sub);
        // 所有更新工具
        registerUpdateWorldSetting(sub);
        registerUpdateCharacter(sub);
        registerUpdateNovelOutline(sub);
        registerUpdateChapterSummary(sub);
        registerUpdateCharacterState(sub);
        return sub;
    }

    private ToolRegistry buildQualityInspectorTools() {
        ToolRegistry sub = new ToolRegistry();
        // 所有读取工具
        registerGetWorldSetting(sub);
        registerGetCharacters(sub);
        registerGetNovelOutline(sub);
        registerGetChapterSummaries(sub);
        registerGetChapter(sub);
        registerGetPreviousChapters(sub);
        registerGetChapterPlan(sub);
        registerGetActiveForeshadowing(sub);
        registerGetGenerationProgress(sub);
        // 质检专用
        registerSaveQualityReport(sub);
        registerGetQualityReport(sub);
        return sub;
    }

    private void registerCallWorldArchitect(ToolRegistry registry) {
        registry.register("callWorldArchitect", params -> {
            String task = (String) params.get("task");
            String subPrompt = promptService.getPromptValue("novel-world-architect");
            return invokeSubAgent("worldArchitect", task, subPrompt, buildWorldArchitectTools());
        }, ToolDefinition.builder()
                .name("callWorldArchitect")
                .description("调用世界架构师，构建世界观、力量体系、社会规则")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给世界架构师的任务描述")),
                        "required", List.of("task")))
                .build());
    }

    private void registerCallCharacterDesigner(ToolRegistry registry) {
        registry.register("callCharacterDesigner", params -> {
            String task = (String) params.get("task");
            String subPrompt = promptService.getPromptValue("novel-character-designer");
            return invokeSubAgent("characterDesigner", task, subPrompt, buildCharacterDesignerTools());
        }, ToolDefinition.builder()
                .name("callCharacterDesigner")
                .description("调用角色设计师，设计主角、配角、反派的完整人设")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给角色设计师的任务描述")),
                        "required", List.of("task")))
                .build());
    }

    private void registerCallPlotArchitect(ToolRegistry registry) {
        registry.register("callPlotArchitect", params -> {
            String task = (String) params.get("task");
            String subPrompt = promptService.getPromptValue("novel-plot-architect");
            return invokeSubAgent("plotArchitect", task, subPrompt, buildPlotArchitectTools());
        }, ToolDefinition.builder()
                .name("callPlotArchitect")
                .description("调用情节架构师，规划全书大纲、分卷结构、各卷主线")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给情节架构师的任务描述")),
                        "required", List.of("task")))
                .build());
    }

    private void registerCallChapterPlanner(ToolRegistry registry) {
        registry.register("callChapterPlanner", params -> {
            String task = (String) params.get("task");
            String subPrompt = promptService.getPromptValue("novel-chapter-planner");
            return invokeSubAgent("chapterPlanner", task, subPrompt, buildChapterPlannerTools());
        }, ToolDefinition.builder()
                .name("callChapterPlanner")
                .description("调用章节规划师，规划每章的概要（标题、核心事件、情绪节奏、伏笔、悬念）")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给章节规划师的任务描述")),
                        "required", List.of("task")))
                .build());
    }

    private void registerCallNovelWriter(ToolRegistry registry) {
        registry.register("callNovelWriter", params -> {
            String task = (String) params.get("task");
            // 构建 NovelWriter 的 Prompt：基础 Prompt + 写作质量指令 + 品类特化
            String basePrompt = promptService.getPromptValue("novel-writer");
            String qualityPrompt = promptService.getPromptValue("novel-writing-quality");
            String fullPrompt = basePrompt;
            if (qualityPrompt != null && !qualityPrompt.isEmpty()) {
                fullPrompt += "\n\n=== 系统级写作质量指令（强制遵守）===\n" + qualityPrompt;
            }
            // 品类特化 Prompt
            Project project = projectMapper.selectById(projectId);
            if (project != null && project.getType() != null) {
                String genreCode = "novel-gen-" + mapGenreToCode(project.getType());
                String genrePrompt = promptService.getPromptValue(genreCode);
                if (genrePrompt != null && !genrePrompt.isEmpty()) {
                    fullPrompt += "\n\n=== 品类特化指令 ===\n" + genrePrompt;
                }
            }
            return invokeSubAgent("novelWriter", task, fullPrompt, buildNovelWriterTools());
        }, ToolDefinition.builder()
                .name("callNovelWriter")
                .description("调用小说写手，根据章概要+上下文逐章生成正文")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给小说写手的任务描述")),
                        "required", List.of("task")))
                .build());
    }

    private void registerCallEditor(ToolRegistry registry) {
        registry.register("callEditor", params -> {
            String task = (String) params.get("task");
            String subPrompt = promptService.getPromptValue("novel-editor");
            return invokeSubAgent("editor", task, subPrompt, buildEditorTools());
        }, ToolDefinition.builder()
                .name("callEditor")
                .description("调用总编审，审核任意层级的产出质量，可打回修改")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给总编审的任务描述")),
                        "required", List.of("task")))
                .build());
    }

    private void registerCallQualityInspector(ToolRegistry registry) {
        registry.register("callQualityInspector", params -> {
            String task = (String) params.get("task");
            String subPrompt = promptService.getPromptValue("novel-quality-inspector");
            return invokeSubAgent("qualityInspector", task, subPrompt, buildQualityInspectorTools());
        }, ToolDefinition.builder()
                .name("callQualityInspector")
                .description("调用质检官，对已生成内容进行多维度深度质检（单章/卷/全书）")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给质检官的任务描述")),
                        "required", List.of("task")))
                .build());
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> buildSaveChapterSummarySchema() {
        Map<String, Object> props = new HashMap<>();
        props.put("volumeIndex", Map.of("type", "integer", "description", "卷序号"));
        props.put("chapterIndex", Map.of("type", "integer", "description", "章序号"));
        props.put("title", Map.of("type", "string", "description", "章节标题"));
        props.put("summary", Map.of("type", "string", "description", "200-500字情节概要"));
        props.put("keyEvents", Map.of("type", "array", "description", "核心事件列表", "items", Map.of("type", "string")));
        props.put("characters", Map.of("type", "array", "description", "出场角色名列表", "items", Map.of("type", "string")));
        props.put("emotionCurve", Map.of("type", "string", "description", "情绪曲线"));
        props.put("foreshadowing", Map.of("type", "array", "description", "本章伏笔", "items", Map.of("type", "string")));
        props.put("payoff", Map.of("type", "array", "description", "回收伏笔", "items", Map.of("type", "string")));
        props.put("cliffhanger", Map.of("type", "string", "description", "章末悬念"));
        props.put("wordTarget", Map.of("type", "integer", "description", "目标字数"));

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("volumeIndex", "chapterIndex", "title", "summary"));
        return schema;
    }

    private Map<String, Object> characterToMap(NovelCharacter c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("role", c.getRole());
        map.put("age", c.getAge());
        map.put("appearance", c.getAppearance());
        map.put("personality", c.getPersonality());
        map.put("ability", c.getAbility());
        map.put("relationships", c.getRelationships());
        map.put("growthArc", c.getGrowthArc());
        map.put("currentState", c.getCurrentState());
        map.put("speechStyle", c.getSpeechStyle());
        return map;
    }

    private String toJsonString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String mapGenreToCode(String genre) {
        if (genre == null) return "";
        return switch (genre) {
            case "系统流", "系统" -> "system-flow";
            case "无限流", "无限" -> "infinite-flow";
            case "都市" -> "urban";
            case "玄幻" -> "xuanhuan";
            default -> genre.toLowerCase().replace(" ", "-");
        };
    }
}
