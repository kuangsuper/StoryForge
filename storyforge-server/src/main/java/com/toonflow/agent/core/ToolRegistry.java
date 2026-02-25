package com.toonflow.agent.core;

import com.toonflow.ai.model.ToolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();
    private final Map<String, ToolDefinition> definitions = new LinkedHashMap<>();

    public void register(String name, AgentTool tool, ToolDefinition definition) {
        tools.put(name, tool);
        definitions.put(name, definition);
    }

    public AgentTool getTool(String name) {
        return tools.get(name);
    }

    public List<ToolDefinition> getAllDefinitions() {
        return new ArrayList<>(definitions.values());
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
