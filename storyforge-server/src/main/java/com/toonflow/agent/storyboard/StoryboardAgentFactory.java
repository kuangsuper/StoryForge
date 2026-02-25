package com.toonflow.agent.storyboard;

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
public class StoryboardAgentFactory {

    private final AiProviderService aiProviderService;
    private final AiRetryTemplate aiRetryTemplate;
    private final ChatHistoryMapper chatHistoryMapper;
    private final AgentLogMapper agentLogMapper;
    private final CheckpointManager checkpointManager;
    private final ScriptMapper scriptMapper;
    private final AssetsMapper assetsMapper;
    private final PromptService promptService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public StoryboardAgent create(Long projectId, Long scriptId) {
        AgentEmitter emitter = new AgentEmitter(messagingTemplate, "storyboard", projectId);
        return new StoryboardAgent(projectId, scriptId, emitter, checkpointManager,
                aiProviderService, aiRetryTemplate, chatHistoryMapper, agentLogMapper,
                objectMapper, promptService, scriptMapper, assetsMapper);
    }
}
