package com.toonflow.agent.websocket;

import com.toonflow.agent.core.AgentSession;
import com.toonflow.agent.core.BaseAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AgentWebSocketHandler {

    private final AgentSession agentSession;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/agent/{agentType}/{projectId}/msg")
    public void handleMessage(@DestinationVariable String agentType,
                               @DestinationVariable Long projectId,
                               @Payload Map<String, Object> payload) {
        BaseAgent agent = agentSession.get(projectId, agentType);
        if (agent == null) {
            log.warn("No agent instance for type={} project={}", agentType, projectId);
            messagingTemplate.convertAndSend(
                    "/topic/agent/" + agentType + "/" + projectId + "/event",
                    Map.of("type", "error", "data", "Agent实例不存在，请刷新页面重新连接"));
            return;
        }
        String message = (String) payload.get("data");
        if (message != null && !message.isBlank()) {
            agent.onMessage(message);
        }
    }

    @MessageMapping("/agent/{agentType}/{projectId}/cleanHistory")
    public void handleCleanHistory(@DestinationVariable String agentType,
                                    @DestinationVariable Long projectId) {
        BaseAgent agent = agentSession.get(projectId, agentType);
        if (agent != null) {
            agent.cleanHistory();
            log.debug("Cleaned history for type={} project={}", agentType, projectId);
        }
    }
}
