package com.toonflow.agent.websocket;

import com.toonflow.agent.core.AgentSession;
import com.toonflow.agent.core.BaseAgent;
import com.toonflow.agent.novel.NovelAgentFactory;
import com.toonflow.agent.outline.OutlineAgentFactory;
import com.toonflow.agent.storyboard.StoryboardAgentFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final AgentSession agentSession;
    private final OutlineAgentFactory outlineAgentFactory;
    private final NovelAgentFactory novelAgentFactory;
    private final StoryboardAgentFactory storyboardAgentFactory;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String agentType = (String) sessionAttributes.get("agentType");
        Long projectId = (Long) sessionAttributes.get("projectId");

        if (agentType != null && projectId != null) {
            log.info("WebSocket connected: type={} project={}", agentType, projectId);

            BaseAgent agent;
            if ("outline".equals(agentType)) {
                agent = agentSession.getOrCreate(projectId, agentType,
                        () -> outlineAgentFactory.create(projectId));
            } else if ("novel".equals(agentType)) {
                agent = agentSession.getOrCreate(projectId, agentType,
                        () -> novelAgentFactory.create(projectId));
            } else if ("storyboard".equals(agentType)) {
                Long scriptId = (Long) sessionAttributes.get("scriptId");
                if (scriptId != null) {
                    agent = agentSession.getOrCreate(projectId, agentType,
                            () -> storyboardAgentFactory.create(projectId, scriptId));
                } else {
                    log.warn("Storyboard agent requires scriptId, project={}", projectId);
                    agent = null;
                }
            } else {
                agent = agentSession.get(projectId, agentType);
            }

            if (agent != null) {
                agent.onConnect();
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String agentType = (String) sessionAttributes.get("agentType");
        Long projectId = (Long) sessionAttributes.get("projectId");

        if (agentType != null && projectId != null) {
            log.info("WebSocket disconnected: type={} project={}", agentType, projectId);
            BaseAgent agent = agentSession.get(projectId, agentType);
            if (agent != null) {
                agent.onDisconnect();
            }
            agentSession.remove(projectId, agentType);
        }
    }
}
