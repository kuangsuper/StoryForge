package com.toonflow.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequest {
    private String systemPrompt;
    private List<ChatMessage> messages;
    private List<ToolDefinition> tools;
    private String responseFormat;       // "text" | "json_schema"
    private String jsonSchema;           // JSON Schema string
    private Double temperature;
    private Integer maxTokens;
    private List<String> imageUrls;      // multimodal image input
}
