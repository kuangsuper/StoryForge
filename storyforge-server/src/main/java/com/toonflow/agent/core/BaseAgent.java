package com.toonflow.agent.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.ai.model.AiRequest;
import com.toonflow.ai.model.AiResponse;
import com.toonflow.ai.model.ChatMessage;
import com.toonflow.ai.model.ToolCall;
import com.toonflow.ai.provider.TextAiProvider;
import com.toonflow.ai.retry.AiRetryTemplate;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.entity.AgentLog;
import com.toonflow.entity.ChatHistory;
import com.toonflow.mapper.AgentLogMapper;
import com.toonflow.mapper.ChatHistoryMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public abstract class BaseAgent {

    protected final Long projectId;
    protected final AgentEmitter emitter;
    protected final List<ChatMessage> history;
    protected final ToolRegistry toolRegistry;
    protected final MessageQueue messageQueue;
    protected final CheckpointManager checkpointManager;
    protected final AiProviderService aiProviderService;
    protected final AiRetryTemplate aiRetryTemplate;
    protected final ChatHistoryMapper chatHistoryMapper;
    protected final AgentLogMapper agentLogMapper;
    protected final ObjectMapper objectMapper;
    protected volatile AgentState state;
    protected String sessionId;

    protected BaseAgent(Long projectId,
                        AgentEmitter emitter,
                        CheckpointManager checkpointManager,
                        AiProviderService aiProviderService,
                        AiRetryTemplate aiRetryTemplate,
                        ChatHistoryMapper chatHistoryMapper,
                        AgentLogMapper agentLogMapper,
                        ObjectMapper objectMapper) {
        this.projectId = projectId;
        this.emitter = emitter;
        this.checkpointManager = checkpointManager;
        this.aiProviderService = aiProviderService;
        this.aiRetryTemplate = aiRetryTemplate;
        this.chatHistoryMapper = chatHistoryMapper;
        this.agentLogMapper = agentLogMapper;
        this.objectMapper = objectMapper;
        this.history = new ArrayList<>();
        this.toolRegistry = new ToolRegistry();
        this.messageQueue = new MessageQueue();
        this.state = AgentState.IDLE;
        this.sessionId = UUID.randomUUID().toString();
        registerTools(toolRegistry);
    }

    // ---- 子类必须实现 ----
    protected abstract String getAgentType();
    protected abstract void registerTools(ToolRegistry registry);
    protected abstract String buildSystemPrompt();
    protected abstract String buildContextPrompt();
    protected abstract String getAiFunctionKey();

    // ---- 核心流程 ----
    public void onMessage(String userMessage) {
        synchronized (this) {
            if (state == AgentState.RUNNING) {
                messageQueue.enqueue(userMessage);
                return;
            }
            state = AgentState.RUNNING;
        }
        try {
            history.add(ChatMessage.user(userMessage));
            executeWithToolLoop(history, toolRegistry, false);
        } catch (Exception e) {
            log.error("[{}] onMessage error for project={}", getAgentType(), projectId, e);
            emitter.error(e.getMessage());
        } finally {
            synchronized (this) {
                state = AgentState.IDLE;
            }
            processNextInQueue();
        }
    }

    private void executeWithToolLoop(List<ChatMessage> messages,
                                      ToolRegistry tools,
                                      boolean isSubAgent) {
        int maxIterations = 20;
        for (int i = 0; i < maxIterations; i++) {
            AiRequest request = buildAiRequest(messages, tools);
            long startTime = System.currentTimeMillis();

            TextAiProvider provider = aiProviderService.getTextProvider(getAiFunctionKey());

            // 使用 invokeWithTools 获取结构化响应（content + toolCalls）
            AiResponse response;
            try {
                response = aiRetryTemplate.execute(
                        () -> provider.invokeWithTools(request), getAgentType() + ".llmCall");
            } catch (Exception e) {
                log.error("[{}] LLM call failed for project={}", getAgentType(), projectId, e);
                emitter.error("AI调用失败: " + e.getMessage());
                logAction("llmCall", null, truncate(serializeMessages(messages), 500),
                        null, System.currentTimeMillis() - startTime, 0, "failed", e.getMessage(), null);
                return;
            }

            long duration = System.currentTimeMillis() - startTime;
            String fullText = response.getContent();
            logAction("llmCall", null, truncate(serializeMessages(messages), 500),
                    truncate(fullText, 500), duration, 0, "success", null, null);

            // 流式输出文本部分
            if (fullText != null && !fullText.isEmpty()) {
                if (isSubAgent) {
                    emitter.subAgentStream(getAgentType(), fullText);
                } else {
                    emitter.stream(fullText);
                }
            }

            if (!response.hasToolCalls()) {
                // 无工具调用，结束循环
                messages.add(ChatMessage.assistant(fullText));
                if (isSubAgent) {
                    emitter.subAgentEnd(getAgentType());
                } else {
                    emitter.responseEnd(fullText);
                }
                return;
            }

            // 有工具调用 — 执行每个工具
            List<ToolCall> toolCalls = response.getToolCalls();
            messages.add(ChatMessage.builder()
                    .role("assistant")
                    .content(fullText)
                    .toolCalls(toolCalls)
                    .build());

            for (ToolCall toolCall : toolCalls) {
                long toolStart = System.currentTimeMillis();
                String toolResult;
                String toolStatus = "success";
                String toolError = null;

                try {
                    AgentTool tool = tools.getTool(toolCall.getName());
                    if (tool == null) {
                        toolResult = "Error: unknown tool '" + toolCall.getName() + "'";
                        toolStatus = "failed";
                        toolError = toolResult;
                    } else {
                        emitter.toolCall(getAgentType(), toolCall.getName(), toolCall.getArguments());
                        Map<String, Object> args = objectMapper.readValue(
                                toolCall.getArguments(), new TypeReference<>() {});
                        Object result = tool.execute(args);
                        toolResult = result != null ? objectMapper.writeValueAsString(result) : "null";
                    }
                } catch (Exception e) {
                    log.warn("[{}] Tool '{}' execution failed: {}", getAgentType(), toolCall.getName(), e.getMessage());
                    toolResult = "Error executing tool '" + toolCall.getName() + "': " + e.getMessage();
                    toolStatus = "failed";
                    toolError = e.getMessage();
                }

                long toolDuration = System.currentTimeMillis() - toolStart;
                logAction("toolCall", toolCall.getName(),
                        truncate(toolCall.getArguments(), 500),
                        truncate(toolResult, 500),
                        toolDuration, 0, toolStatus, toolError, null);

                messages.add(ChatMessage.toolResult(toolCall.getId(), toolResult));
            }
            // continue loop — LLM will process tool results
        }
        log.warn("[{}] Tool loop reached max iterations for project={}", getAgentType(), projectId);
    }

    // ---- Sub-Agent ----
    protected String invokeSubAgent(String subAgentName, String task,
                                     String subSystemPrompt, ToolRegistry subTools) {
        emitter.transfer(subAgentName);
        Long parentLogId = logAction("invokeSubAgent", null,
                truncate(task, 500), null, 0, 0, "success", null, null);

        List<ChatMessage> subHistory = new ArrayList<>();
        subHistory.add(ChatMessage.system(subSystemPrompt));
        subHistory.add(ChatMessage.user(task));

        long startTime = System.currentTimeMillis();
        executeWithToolLoop(subHistory, subTools, true);
        long duration = System.currentTimeMillis() - startTime;

        // 获取最后一条 assistant 消息作为结果
        String result = "";
        for (int i = subHistory.size() - 1; i >= 0; i--) {
            if ("assistant".equals(subHistory.get(i).getRole())) {
                result = subHistory.get(i).getContent();
                break;
            }
        }

        logAction("invokeSubAgent", subAgentName,
                truncate(task, 500), truncate(result, 500),
                duration, 0, "success", null, parentLogId);

        return result;
    }

    // ---- 生命周期 ----
    public void onConnect() {
        loadHistory();
    }

    public void onDisconnect() {
        saveHistory();
    }

    public void cleanHistory() {
        history.clear();
        saveHistory();
    }

    // ---- 中断控制 ----
    public void pause() {
        state = AgentState.PAUSED;
        checkpointManager.save(projectId, getAgentType(), buildCurrentCheckpoint());
    }

    public void resume() {
        state = AgentState.IDLE;
        processNextInQueue();
    }

    public void cancel() {
        state = AgentState.IDLE;
        messageQueue.clear();
    }

    // ---- Getters ----
    public AgentState getState() {
        return state;
    }

    public Long getProjectId() {
        return projectId;
    }

    // ---- 内部方法 ----
    private void processNextInQueue() {
        if (!messageQueue.isEmpty()) {
            String next = messageQueue.dequeue();
            if (next != null) {
                onMessage(next);
            }
        }
    }

    private void loadHistory() {
        try {
            ChatHistory record = chatHistoryMapper.selectOne(
                    new LambdaQueryWrapper<ChatHistory>()
                            .eq(ChatHistory::getProjectId, projectId)
                            .eq(ChatHistory::getType, getAgentType()));
            if (record != null && record.getData() != null && !record.getData().isEmpty()) {
                List<ChatMessage> loaded = objectMapper.readValue(
                        record.getData(), new TypeReference<>() {});
                history.clear();
                history.addAll(loaded);
                log.debug("[{}] Loaded {} history messages for project={}",
                        getAgentType(), history.size(), projectId);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to load history for project={}", getAgentType(), projectId, e);
        }
    }

    private void saveHistory() {
        try {
            String json = objectMapper.writeValueAsString(history);
            ChatHistory existing = chatHistoryMapper.selectOne(
                    new LambdaQueryWrapper<ChatHistory>()
                            .eq(ChatHistory::getProjectId, projectId)
                            .eq(ChatHistory::getType, getAgentType()));
            if (existing != null) {
                existing.setData(json);
                chatHistoryMapper.updateById(existing);
            } else {
                ChatHistory record = new ChatHistory();
                record.setProjectId(projectId);
                record.setType(getAgentType());
                record.setData(json);
                chatHistoryMapper.insert(record);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to save history for project={}", getAgentType(), projectId, e);
        }
    }

    private AiRequest buildAiRequest(List<ChatMessage> messages, ToolRegistry tools) {
        List<ChatMessage> fullMessages = new ArrayList<>();
        String contextPrompt = buildContextPrompt();
        if (contextPrompt != null && !contextPrompt.isEmpty()) {
            fullMessages.add(ChatMessage.system(contextPrompt));
        }
        fullMessages.addAll(messages);
        return AiRequest.builder()
                .systemPrompt(buildSystemPrompt())
                .messages(fullMessages)
                .tools(tools.getAllDefinitions())
                .build();
    }

    private Checkpoint buildCurrentCheckpoint() {
        return Checkpoint.builder()
                .lastAgentState(state.name())
                .savedAt(LocalDateTime.now())
                .completedList(new ArrayList<>())
                .pendingList(new ArrayList<>())
                .build();
    }

    private Long logAction(String action, String toolName, String input, String output,
                           long duration, int tokenUsed, String status, String errorMessage,
                           Long parentLogId) {
        try {
            AgentLog agentLog = new AgentLog();
            agentLog.setProjectId(projectId);
            agentLog.setAgentType(getAgentType());
            agentLog.setSessionId(sessionId);
            agentLog.setAction(action);
            agentLog.setAgentName(getAgentType());
            agentLog.setToolName(toolName);
            agentLog.setInput(input);
            agentLog.setOutput(output);
            agentLog.setDuration(duration);
            agentLog.setTokenUsed(tokenUsed);
            agentLog.setStatus(status);
            agentLog.setErrorMessage(errorMessage);
            agentLog.setParentLogId(parentLogId);
            agentLogMapper.insert(agentLog);
            return agentLog.getId();
        } catch (Exception e) {
            log.error("[{}] Failed to log action={} for project={}", getAgentType(), action, projectId, e);
            return null;
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String serializeMessages(List<ChatMessage> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            return "[serialization error]";
        }
    }
}
