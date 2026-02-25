package com.toonflow.agent.outline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.agent.core.*;
import com.toonflow.ai.model.ToolDefinition;
import com.toonflow.ai.retry.AiRetryTemplate;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.dto.request.SaveOutlineRequest;
import com.toonflow.dto.request.SaveStorylineRequest;
import com.toonflow.entity.Novel;
import com.toonflow.entity.Outline;
import com.toonflow.entity.Project;
import com.toonflow.entity.Storyline;
import com.toonflow.entity.json.EpisodeData;
import com.toonflow.mapper.*;
import com.toonflow.service.OutlineService;
import com.toonflow.service.PromptService;
import com.toonflow.service.StorylineService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class OutlineMainAgent extends BaseAgent {

    private final PromptService promptService;
    private final NovelMapper novelMapper;
    private final StorylineService storylineService;
    private final OutlineService outlineService;
    private final OutlineMapper outlineMapper;
    private final ProjectMapper projectMapper;
    private final ScriptMapper scriptMapper;

    public OutlineMainAgent(Long projectId,
                            AgentEmitter emitter,
                            CheckpointManager checkpointManager,
                            AiProviderService aiProviderService,
                            AiRetryTemplate aiRetryTemplate,
                            ChatHistoryMapper chatHistoryMapper,
                            AgentLogMapper agentLogMapper,
                            ObjectMapper objectMapper,
                            PromptService promptService,
                            NovelMapper novelMapper,
                            StorylineService storylineService,
                            OutlineService outlineService,
                            OutlineMapper outlineMapper,
                            ProjectMapper projectMapper,
                            ScriptMapper scriptMapper) {
        super(projectId, emitter, checkpointManager, aiProviderService,
                aiRetryTemplate, chatHistoryMapper, agentLogMapper, objectMapper);
        this.promptService = promptService;
        this.novelMapper = novelMapper;
        this.storylineService = storylineService;
        this.outlineService = outlineService;
        this.outlineMapper = outlineMapper;
        this.projectMapper = projectMapper;
        this.scriptMapper = scriptMapper;
    }

    @Override
    protected String getAgentType() {
        return "outlineAgent";
    }

    @Override
    protected String getAiFunctionKey() {
        return "outlineScript-main";
    }

    @Override
    protected String buildSystemPrompt() {
        return promptService.getPromptValue("outlineScript-main");
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

        // 章节列表（不含全文）
        List<Novel> novels = novelMapper.selectList(
                new LambdaQueryWrapper<Novel>()
                        .eq(Novel::getProjectId, projectId)
                        .select(Novel::getChapterIndex, Novel::getChapter)
                        .orderByAsc(Novel::getChapterIndex));
        if (!novels.isEmpty()) {
            sb.append("【章节列表】共 ").append(novels.size()).append(" 章\n");
            for (Novel n : novels) {
                sb.append("第").append(n.getChapterIndex()).append("章: ").append(n.getChapter()).append("\n");
            }
            sb.append("\n");
        } else {
            sb.append("【章节列表】暂无章节\n\n");
        }

        // 故事线状态
        Storyline storyline = storylineService.get(projectId);
        if (storyline != null) {
            sb.append("【故事线】已有\n");
            sb.append("名称: ").append(storyline.getName()).append("\n");
            String content = storyline.getContent();
            if (content != null && content.length() > 200) {
                sb.append("摘要: ").append(content, 0, 200).append("...\n\n");
            } else {
                sb.append("内容: ").append(content).append("\n\n");
            }
        } else {
            sb.append("【故事线】暂无\n\n");
        }

        // 大纲状态
        List<Outline> outlines = outlineService.list(projectId, "simple");
        if (!outlines.isEmpty()) {
            sb.append("【大纲】已有 ").append(outlines.size()).append(" 集\n");
        } else {
            sb.append("【大纲】暂无\n");
        }

        return sb.toString();
    }

    @Override
    protected void registerTools(ToolRegistry registry) {
        // ---- 数据工具 ----
        registerGetChapter(registry);
        registerGetStoryline(registry);
        registerSaveStoryline(registry);
        registerDeleteStoryline(registry);
        registerGetOutline(registry);
        registerSaveOutline(registry);
        registerUpdateOutline(registry);
        registerDeleteOutline(registry);
        registerGenerateAssets(registry);

        // ---- Sub-Agent 工具 ----
        registerCallStoryteller(registry);
        registerCallOutliner(registry);
        registerCallDirector(registry);
    }

    // ========== 数据工具注册 ==========

    private void registerGetChapter(ToolRegistry registry) {
        registry.register("getChapter", params -> {
            Object idx = params.get("chapterIndex");
            List<Integer> indices = new ArrayList<>();
            if (idx instanceof List<?> list) {
                for (Object item : list) {
                    indices.add(((Number) item).intValue());
                }
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
                            "chapter", novel.getChapter() != null ? novel.getChapter() : "",
                            "chapterData", novel.getChapterData() != null ? novel.getChapterData() : ""));
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
                                                Map.of("type", "array", "items", Map.of("type", "integer"))
                                        ))),
                        "required", List.of("chapterIndex")))
                .build());
    }

    private void registerGetStoryline(ToolRegistry registry) {
        registry.register("getStoryline", params -> {
            Storyline s = storylineService.get(projectId);
            if (s == null) return Map.of("name", "", "content", "");
            return Map.of(
                    "name", s.getName() != null ? s.getName() : "",
                    "content", s.getContent() != null ? s.getContent() : "");
        }, ToolDefinition.builder()
                .name("getStoryline")
                .description("获取当前项目的故事线")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    private void registerSaveStoryline(ToolRegistry registry) {
        registry.register("saveStoryline", params -> {
            SaveStorylineRequest req = new SaveStorylineRequest();
            req.setName((String) params.get("name"));
            req.setContent((String) params.get("content"));
            storylineService.saveOrUpdate(projectId, req);
            emitter.refresh("storyline");
            return "ok";
        }, ToolDefinition.builder()
                .name("saveStoryline")
                .description("保存或更新故事线")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string", "description", "故事线名称"),
                                "content", Map.of("type", "string", "description", "故事线内容")),
                        "required", List.of("name", "content")))
                .build());
    }

    private void registerDeleteStoryline(ToolRegistry registry) {
        registry.register("deleteStoryline", params -> {
            storylineService.delete(projectId);
            emitter.refresh("storyline");
            return "ok";
        }, ToolDefinition.builder()
                .name("deleteStoryline")
                .description("删除当前项目的故事线")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    private void registerGetOutline(ToolRegistry registry) {
        registry.register("getOutline", params -> {
            String mode = (String) params.getOrDefault("mode", "full");
            return outlineService.list(projectId, mode);
        }, ToolDefinition.builder()
                .name("getOutline")
                .description("获取大纲列表，mode=simple 只返回集号，mode=full 返回完整数据")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "mode", Map.of("type", "string", "enum", List.of("simple", "full"),
                                        "description", "查询模式，默认 full"))))
                .build());
    }

    private void registerSaveOutline(ToolRegistry registry) {
        registry.register("saveOutline", params -> {
            String mode = (String) params.getOrDefault("mode", "overwrite");
            List<?> episodesRaw = (List<?>) params.get("episodes");
            List<EpisodeData> episodes = new ArrayList<>();
            if (episodesRaw != null) {
                for (Object item : episodesRaw) {
                    EpisodeData ep = objectMapper.convertValue(item, EpisodeData.class);
                    episodes.add(ep);
                }
            }

            if ("overwrite".equals(mode)) {
                // 删除已有大纲
                List<Outline> existing = outlineService.list(projectId, "simple");
                if (!existing.isEmpty()) {
                    List<Long> ids = existing.stream().map(Outline::getId).collect(Collectors.toList());
                    outlineService.batchDelete(projectId, ids);
                }
            }

            // 插入新大纲
            int startEpisode = 1;
            if ("append".equals(mode)) {
                List<Outline> existing = outlineService.list(projectId, "simple");
                startEpisode = existing.size() + 1;
            }
            for (int i = 0; i < episodes.size(); i++) {
                SaveOutlineRequest req = new SaveOutlineRequest();
                req.setEpisode(startEpisode + i);
                req.setData(episodes.get(i));
                outlineService.create(projectId, req);
            }
            emitter.refresh("outline");
            return "ok";
        }, ToolDefinition.builder()
                .name("saveOutline")
                .description("保存大纲，mode=overwrite 覆盖已有大纲，mode=append 在末尾追加")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "episodes", Map.of("type", "array", "description", "EpisodeData 数组",
                                        "items", Map.of("type", "object")),
                                "mode", Map.of("type", "string", "enum", List.of("overwrite", "append"),
                                        "description", "保存模式，默认 overwrite")),
                        "required", List.of("episodes")))
                .build());
    }

    private void registerUpdateOutline(ToolRegistry registry) {
        registry.register("updateOutline", params -> {
            Long outlineId = ((Number) params.get("outlineId")).longValue();
            Object dataRaw = params.get("data");
            EpisodeData data = objectMapper.convertValue(dataRaw, EpisodeData.class);
            SaveOutlineRequest req = new SaveOutlineRequest();
            req.setData(data);
            outlineService.update(outlineId, req);
            emitter.refresh("outline");
            return "ok";
        }, ToolDefinition.builder()
                .name("updateOutline")
                .description("更新单集大纲")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "outlineId", Map.of("type", "integer", "description", "大纲记录ID"),
                                "data", Map.of("type", "object", "description", "EpisodeData JSON")),
                        "required", List.of("outlineId", "data")))
                .build());
    }

    private void registerDeleteOutline(ToolRegistry registry) {
        registry.register("deleteOutline", params -> {
            List<?> idsRaw = (List<?>) params.get("ids");
            List<Long> ids = new ArrayList<>();
            if (idsRaw != null) {
                for (Object item : idsRaw) {
                    ids.add(((Number) item).longValue());
                }
            }
            outlineService.batchDelete(projectId, ids);
            emitter.refresh("outline");
            return "ok";
        }, ToolDefinition.builder()
                .name("deleteOutline")
                .description("批量删除大纲及关联剧本")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "ids", Map.of("type", "array", "description", "大纲ID数组",
                                        "items", Map.of("type", "integer"))),
                        "required", List.of("ids")))
                .build());
    }

    private void registerGenerateAssets(ToolRegistry registry) {
        registry.register("generateAssets", params -> {
            outlineService.extractAssets(projectId);
            emitter.refresh("assets");
            return "ok";
        }, ToolDefinition.builder()
                .name("generateAssets")
                .description("从大纲中提取角色、场景、道具等资产")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    // ========== Sub-Agent 工具注册 ==========

    private ToolRegistry buildSubAgentTools() {
        ToolRegistry subTools = new ToolRegistry();
        registerGetChapter(subTools);
        registerGetStoryline(subTools);
        registerSaveStoryline(subTools);
        registerGetOutline(subTools);
        registerSaveOutline(subTools);
        registerUpdateOutline(subTools);
        return subTools;
    }

    private void registerCallStoryteller(ToolRegistry registry) {
        registry.register("callStoryteller", params -> {
            String task = (String) params.get("task");
            String subPrompt = promptService.getPromptValue("outlineScript-a1");
            return invokeSubAgent("storyteller", task, subPrompt, buildSubAgentTools());
        }, ToolDefinition.builder()
                .name("callStoryteller")
                .description("调用故事师AI，分析小说原文生成或修改故事线")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给故事师的任务描述")),
                        "required", List.of("task")))
                .build());
    }

    private void registerCallOutliner(ToolRegistry registry) {
        registry.register("callOutliner", params -> {
            String task = (String) params.get("task");
            String subPrompt = promptService.getPromptValue("outlineScript-a2");
            return invokeSubAgent("outliner", task, subPrompt, buildSubAgentTools());
        }, ToolDefinition.builder()
                .name("callOutliner")
                .description("调用大纲师AI，根据故事线和原文生成分集大纲")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给大纲师的任务描述")),
                        "required", List.of("task")))
                .build());
    }

    private void registerCallDirector(ToolRegistry registry) {
        registry.register("callDirector", params -> {
            String task = (String) params.get("task");
            String subPrompt = promptService.getPromptValue("outlineScript-director");
            return invokeSubAgent("director", task, subPrompt, buildSubAgentTools());
        }, ToolDefinition.builder()
                .name("callDirector")
                .description("调用导演AI，审核故事线和大纲质量，可直接修改")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "交给导演的任务描述")),
                        "required", List.of("task")))
                .build());
    }
}
