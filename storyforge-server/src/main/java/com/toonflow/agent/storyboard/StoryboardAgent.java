package com.toonflow.agent.storyboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.agent.core.*;
import com.toonflow.ai.model.ToolDefinition;
import com.toonflow.ai.retry.AiRetryTemplate;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.entity.Assets;
import com.toonflow.entity.Script;
import com.toonflow.mapper.*;
import com.toonflow.service.PromptService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class StoryboardAgent extends BaseAgent {

    private final Long scriptId;
    private final PromptService promptService;
    private final ScriptMapper scriptMapper;
    private final AssetsMapper assetsMapper;

    private final List<Segment> segments = Collections.synchronizedList(new ArrayList<>());
    private final List<Shot> shots = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong shotIdSeq = new AtomicLong(1);

    public StoryboardAgent(Long projectId, Long scriptId, AgentEmitter emitter,
                           CheckpointManager checkpointManager,
                           AiProviderService aiProviderService,
                           AiRetryTemplate aiRetryTemplate,
                           ChatHistoryMapper chatHistoryMapper,
                           AgentLogMapper agentLogMapper,
                           ObjectMapper objectMapper,
                           PromptService promptService,
                           ScriptMapper scriptMapper,
                           AssetsMapper assetsMapper) {
        super(projectId, emitter, checkpointManager, aiProviderService,
                aiRetryTemplate, chatHistoryMapper, agentLogMapper, objectMapper);
        this.scriptId = scriptId;
        this.promptService = promptService;
        this.scriptMapper = scriptMapper;
        this.assetsMapper = assetsMapper;
    }

    @Override
    protected String getAgentType() { return "storyboardAgent"; }

    @Override
    protected String getAiFunctionKey() { return "storyboard-main"; }

    @Override
    protected String buildSystemPrompt() {
        return promptService.getPromptValue("storyboard-main");
    }

    @Override
    protected String buildContextPrompt() {
        StringBuilder sb = new StringBuilder();
        Script script = scriptMapper.selectById(scriptId);
        if (script != null) {
            sb.append("【剧本】\n").append(script.getContent() != null ? script.getContent() : "暂无内容").append("\n\n");
        }
        List<Assets> assets = assetsMapper.selectList(
                new LambdaQueryWrapper<Assets>().eq(Assets::getProjectId, projectId));
        if (!assets.isEmpty()) {
            sb.append("【资产列表】\n");
            for (Assets a : assets) {
                sb.append("- [").append(a.getType()).append("] ").append(a.getName())
                  .append(": ").append(a.getIntro() != null ? a.getIntro() : "").append("\n");
            }
        }
        sb.append("\n【当前片段数】").append(segments.size());
        sb.append("\n【当前分镜数】").append(shots.size());
        return sb.toString();
    }

    @Override
    protected void registerTools(ToolRegistry registry) {
        registerGetScript(registry);
        registerGetAssets(registry);
        registerGetSegments(registry);
        registerUpdateSegments(registry);
        registerAddShots(registry);
        registerUpdateShots(registry);
        registerDeleteShots(registry);
    }

    // ---- Getters for persistence ----
    public List<Segment> getSegments() { return new ArrayList<>(segments); }
    public List<Shot> getShots() { return new ArrayList<>(shots); }

    private void registerGetScript(ToolRegistry registry) {
        registry.register("getScript", params -> {
            Script s = scriptMapper.selectById(scriptId);
            if (s == null) return Map.of("content", "");
            return Map.of("id", s.getId(), "name", s.getName() != null ? s.getName() : "",
                    "content", s.getContent() != null ? s.getContent() : "");
        }, ToolDefinition.builder()
                .name("getScript").description("获取当前剧本内容")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    private void registerGetAssets(ToolRegistry registry) {
        registry.register("getAssets", params -> {
            return assetsMapper.selectList(
                    new LambdaQueryWrapper<Assets>().eq(Assets::getProjectId, projectId));
        }, ToolDefinition.builder()
                .name("getAssets").description("获取项目所有资产")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    private void registerGetSegments(ToolRegistry registry) {
        registry.register("getSegments", params -> {
            return Map.of("segments", new ArrayList<>(segments), "shots", new ArrayList<>(shots));
        }, ToolDefinition.builder()
                .name("getSegments").description("获取当前所有片段和分镜数据")
                .parameters(Map.of("type", "object", "properties", Map.of()))
                .build());
    }

    private void registerUpdateSegments(ToolRegistry registry) {
        registry.register("updateSegments", params -> {
            List<?> raw = (List<?>) params.get("segments");
            if (raw != null) {
                segments.clear();
                for (Object item : raw) {
                    segments.add(objectMapper.convertValue(item, Segment.class));
                }
            }
            emitter.refresh("segmentsUpdated");
            return "ok";
        }, ToolDefinition.builder()
                .name("updateSegments").description("更新片段列表")
                .parameters(Map.of("type", "object",
                        "properties", Map.of("segments", Map.of("type", "array",
                                "description", "Segment 数组", "items", Map.of("type", "object"))),
                        "required", List.of("segments")))
                .build());
    }

    private void registerAddShots(ToolRegistry registry) {
        registry.register("addShots", params -> {
            List<?> raw = (List<?>) params.get("shots");
            if (raw != null) {
                for (Object item : raw) {
                    Shot shot = objectMapper.convertValue(item, Shot.class);
                    if (shot.getId() == null) shot.setId(shotIdSeq.getAndIncrement());
                    shots.add(shot);
                }
            }
            emitter.refresh("shotsUpdated");
            return "ok";
        }, ToolDefinition.builder()
                .name("addShots").description("添加分镜")
                .parameters(Map.of("type", "object",
                        "properties", Map.of("shots", Map.of("type", "array",
                                "description", "Shot 数组", "items", Map.of("type", "object"))),
                        "required", List.of("shots")))
                .build());
    }

    private void registerUpdateShots(ToolRegistry registry) {
        registry.register("updateShots", params -> {
            List<?> raw = (List<?>) params.get("shots");
            if (raw != null) {
                for (Object item : raw) {
                    Shot updated = objectMapper.convertValue(item, Shot.class);
                    for (int i = 0; i < shots.size(); i++) {
                        if (shots.get(i).getId().equals(updated.getId())) {
                            shots.set(i, updated);
                            break;
                        }
                    }
                }
            }
            emitter.refresh("shotsUpdated");
            return "ok";
        }, ToolDefinition.builder()
                .name("updateShots").description("更新分镜")
                .parameters(Map.of("type", "object",
                        "properties", Map.of("shots", Map.of("type", "array",
                                "description", "Shot 数组", "items", Map.of("type", "object"))),
                        "required", List.of("shots")))
                .build());
    }

    private void registerDeleteShots(ToolRegistry registry) {
        registry.register("deleteShots", params -> {
            List<?> idsRaw = (List<?>) params.get("shotIds");
            if (idsRaw != null) {
                Set<Long> ids = new HashSet<>();
                for (Object item : idsRaw) ids.add(((Number) item).longValue());
                shots.removeIf(s -> ids.contains(s.getId()));
            }
            emitter.refresh("shotsUpdated");
            return "ok";
        }, ToolDefinition.builder()
                .name("deleteShots").description("删除分镜")
                .parameters(Map.of("type", "object",
                        "properties", Map.of("shotIds", Map.of("type", "array",
                                "description", "要删除的 Shot ID 数组", "items", Map.of("type", "integer"))),
                        "required", List.of("shotIds")))
                .build());
    }
}
