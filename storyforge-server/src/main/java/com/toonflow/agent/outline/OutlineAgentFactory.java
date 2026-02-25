package com.toonflow.agent.outline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.agent.core.AgentEmitter;
import com.toonflow.agent.core.CheckpointManager;
import com.toonflow.ai.retry.AiRetryTemplate;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.mapper.*;
import com.toonflow.service.OutlineService;
import com.toonflow.service.PromptService;
import com.toonflow.service.StorylineService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutlineAgentFactory {

    private final AiProviderService aiProviderService;
    private final AiRetryTemplate aiRetryTemplate;
    private final ChatHistoryMapper chatHistoryMapper;
    private final AgentLogMapper agentLogMapper;
    private final CheckpointManager checkpointManager;
    private final NovelMapper novelMapper;
    private final StorylineService storylineService;
    private final OutlineService outlineService;
    private final OutlineMapper outlineMapper;
    private final ScriptMapper scriptMapper;
    private final ProjectMapper projectMapper;
    private final PromptService promptService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public OutlineMainAgent create(Long projectId) {
        AgentEmitter emitter = new AgentEmitter(messagingTemplate, "outline", projectId);
        return new OutlineMainAgent(
                projectId, emitter, checkpointManager, aiProviderService,
                aiRetryTemplate, chatHistoryMapper, agentLogMapper, objectMapper,
                promptService, novelMapper, storylineService, outlineService,
                outlineMapper, projectMapper, scriptMapper);
    }
}
