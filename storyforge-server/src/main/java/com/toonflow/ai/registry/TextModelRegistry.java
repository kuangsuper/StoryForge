package com.toonflow.ai.registry;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TextModelRegistry {

    @Data
    @AllArgsConstructor
    public static class ModelCapability {
        private String responseFormat; // "schema" | "object" | "none"
        private boolean image;
        private boolean think;
        private boolean tool;
    }

    private static final ModelCapability DEFAULT =
            new ModelCapability("object", false, false, false);

    private final Map<String, ModelCapability> registry = new HashMap<>();

    @PostConstruct
    void init() {
        // DeepSeek
        register("deepseek-chat",          "object", false, false, true);
        register("deepseek-reasoner",      "none",   false, true,  false);
        // Doubao
        register("doubao-seed-1.6",        "object", false, false, true);
        register("doubao-1.5-pro",         "object", false, false, true);
        register("doubao-1.5-thinking-pro","none",   false, true,  false);
        // Zhipu
        register("glm-4-plus",            "object", false, false, true);
        register("glm-4-flash",           "object", false, false, true);
        register("glm-4-long",            "object", false, false, true);
        // Qwen
        register("qwen-max",              "schema", false, false, true);
        register("qwen-plus",             "schema", false, false, true);
        register("qwen3-235b-a22b",       "schema", false, true,  true);
        register("qwen3-32b",             "schema", false, true,  true);
        register("qwen2.5-72b-instruct",  "schema", false, false, true);
        // OpenAI
        register("gpt-4o",                "schema", true,  false, true);
        register("gpt-4.1",               "schema", true,  false, true);
        register("gpt-4o-mini",           "schema", true,  false, true);
        register("gpt-4.1-mini",          "schema", true,  false, true);
        register("gpt-4.5-preview",       "schema", true,  false, true);
        register("o3",                    "schema", true,  true,  true);
        register("o4-mini",              "schema", true,  true,  true);
        // Gemini
        register("gemini-2.5-pro",        "schema", true,  true,  true);
        register("gemini-2.5-flash",      "schema", true,  true,  true);
        register("gemini-2.0-flash",      "schema", true,  false, true);
        // Anthropic
        register("claude-sonnet-4-5",     "schema", true,  true,  true);
        register("claude-haiku-4-5",      "schema", true,  false, true);
        register("claude-opus-4",         "schema", true,  true,  true);
        // Xai
        register("grok-3",                "object", true,  false, true);
        register("grok-4",                "object", true,  true,  true);
        register("grok-3-mini",           "object", false, true,  true);
    }

    public ModelCapability getCapability(String modelName) {
        return registry.getOrDefault(modelName, DEFAULT);
    }

    public Map<String, ModelCapability> getAll() {
        return Map.copyOf(registry);
    }

    private void register(String model, String responseFormat, boolean image, boolean think, boolean tool) {
        registry.put(model, new ModelCapability(responseFormat, image, think, tool));
    }
}
