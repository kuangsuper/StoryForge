package com.toonflow.agent.core;

import java.util.Map;

@FunctionalInterface
public interface AgentTool {
    Object execute(Map<String, Object> params);
}
