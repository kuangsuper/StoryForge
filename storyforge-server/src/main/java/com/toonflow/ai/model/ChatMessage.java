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
public class ChatMessage {
    private String role;       // "user" | "assistant" | "system" | "tool"
    private String content;
    private String toolCallId;
    private List<ToolCall> toolCalls;

    public static ChatMessage user(String content) {
        return ChatMessage.builder().role("user").content(content).build();
    }

    public static ChatMessage assistant(String content) {
        return ChatMessage.builder().role("assistant").content(content).build();
    }

    public static ChatMessage system(String content) {
        return ChatMessage.builder().role("system").content(content).build();
    }

    public static ChatMessage toolResult(String toolCallId, String content) {
        return ChatMessage.builder().role("tool").toolCallId(toolCallId).content(content).build();
    }
}
