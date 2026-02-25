package com.toonflow.agent.websocket;

import com.toonflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        HandshakeInterceptor jwtInterceptor = new JwtHandshakeInterceptor();

        registry.addEndpoint("/ws/agent/outline/{projectId}")
                .addInterceptors(jwtInterceptor)
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/agent/novel/{projectId}")
                .addInterceptors(jwtInterceptor)
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/agent/storyboard/{projectId}")
                .addInterceptors(jwtInterceptor)
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/pipeline/{projectId}")
                .addInterceptors(jwtInterceptor)
                .setAllowedOriginPatterns("*");
    }

    private class JwtHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                String token = servletRequest.getServletRequest().getParameter("token");
                if (token != null && jwtTokenProvider.validateToken(token)) {
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);
                    attributes.put("userId", userId);

                    // 从 URI 路径中提取 agentType 和 projectId
                    String path = request.getURI().getPath();
                    parsePathAttributes(path, attributes);

                    // 解析 query 参数（如 scriptId）
                    String scriptIdStr = servletRequest.getServletRequest().getParameter("scriptId");
                    if (scriptIdStr != null) {
                        try { attributes.put("scriptId", Long.parseLong(scriptIdStr)); }
                        catch (NumberFormatException ignored) {}
                    }
                    return true;
                }
                log.warn("WebSocket handshake rejected: invalid or missing token");
            }
            return false;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Exception exception) {
            // no-op
        }

        private void parsePathAttributes(String path, Map<String, Object> attributes) {
            // path: /ws/agent/{type}/{projectId} or /ws/pipeline/{projectId}
            String[] parts = path.split("/");
            if (parts.length >= 5 && "agent".equals(parts[2])) {
                attributes.put("agentType", parts[3]);
                attributes.put("projectId", Long.parseLong(parts[4]));
            } else if (parts.length >= 4 && "pipeline".equals(parts[2])) {
                attributes.put("agentType", "pipeline");
                attributes.put("projectId", Long.parseLong(parts[3]));
            }
        }
    }
}
