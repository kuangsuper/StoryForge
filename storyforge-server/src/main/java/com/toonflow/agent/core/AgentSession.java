package com.toonflow.agent.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Component
public class AgentSession {

    private static final long MAX_IDLE_MILLIS = 30 * 60 * 1000L; // 30 minutes

    private final ConcurrentHashMap<String, AgentEntry> agents = new ConcurrentHashMap<>();

    public BaseAgent getOrCreate(Long projectId, String agentType, Supplier<BaseAgent> factory) {
        AgentEntry entry = agents.computeIfAbsent(key(projectId, agentType),
                k -> new AgentEntry(factory.get()));
        entry.touch();
        return entry.agent;
    }

    public BaseAgent get(Long projectId, String agentType) {
        AgentEntry entry = agents.get(key(projectId, agentType));
        if (entry != null) {
            entry.touch();
            return entry.agent;
        }
        return null;
    }

    public void remove(Long projectId, String agentType) {
        AgentEntry entry = agents.remove(key(projectId, agentType));
        if (entry != null) {
            try { entry.agent.onDisconnect(); } catch (Exception ignored) {}
        }
    }

    public int size() {
        return agents.size();
    }

    /**
     * 每5分钟清理空闲超过30分钟的 Agent，防止内存泄漏
     */
    @Scheduled(fixedDelay = 300_000)
    public void evictIdle() {
        long now = Instant.now().toEpochMilli();
        int evicted = 0;
        for (Map.Entry<String, AgentEntry> e : agents.entrySet()) {
            if (now - e.getValue().lastAccessTime > MAX_IDLE_MILLIS
                    && e.getValue().agent.getState() == AgentState.IDLE) {
                AgentEntry removed = agents.remove(e.getKey());
                if (removed != null) {
                    try { removed.agent.onDisconnect(); } catch (Exception ignored) {}
                    evicted++;
                }
            }
        }
        if (evicted > 0) {
            log.info("[AgentSession] Evicted {} idle agents, remaining: {}", evicted, agents.size());
        }
    }

    private String key(Long projectId, String agentType) {
        return agentType + ":" + projectId;
    }

    private static class AgentEntry {
        final BaseAgent agent;
        volatile long lastAccessTime;

        AgentEntry(BaseAgent agent) {
            this.agent = agent;
            this.lastAccessTime = Instant.now().toEpochMilli();
        }

        void touch() {
            this.lastAccessTime = Instant.now().toEpochMilli();
        }
    }
}
