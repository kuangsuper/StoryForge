package com.toonflow.agent.novel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.agent.core.AgentEmitter;
import com.toonflow.agent.core.CheckpointManager;
import com.toonflow.ai.retry.AiRetryTemplate;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.mapper.*;
import com.toonflow.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NovelAgentFactory {

    private final AiProviderService aiProviderService;
    private final AiRetryTemplate aiRetryTemplate;
    private final ChatHistoryMapper chatHistoryMapper;
    private final AgentLogMapper agentLogMapper;
    private final CheckpointManager checkpointManager;
    private final NovelMapper novelMapper;
    private final NovelWorldMapper novelWorldMapper;
    private final NovelCharacterMapper novelCharacterMapper;
    private final NovelOutlineMapper novelOutlineMapper;
    private final NovelChapterPlanMapper novelChapterPlanMapper;
    private final NovelQualityReportMapper novelQualityReportMapper;
    private final NovelVersionMapper novelVersionMapper;
    private final ProjectMapper projectMapper;
    private final PromptService promptService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NovelMainAgent create(Long projectId) {
        AgentEmitter emitter = new AgentEmitter(messagingTemplate, "novel", projectId);
        ContextAssembler contextAssembler = new ContextAssembler(
                novelWorldMapper, novelCharacterMapper, novelOutlineMapper,
                novelChapterPlanMapper, novelMapper);
        return new NovelMainAgent(
                projectId, emitter, checkpointManager, aiProviderService,
                aiRetryTemplate, chatHistoryMapper, agentLogMapper, objectMapper,
                promptService, novelMapper, novelWorldMapper, novelCharacterMapper,
                novelOutlineMapper, novelChapterPlanMapper, novelQualityReportMapper,
                novelVersionMapper, projectMapper, contextAssembler);
    }
}
