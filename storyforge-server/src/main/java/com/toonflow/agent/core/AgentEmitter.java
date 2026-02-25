package com.toonflow.agent.core;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

@RequiredArgsConstructor
public class AgentEmitter {

    private final SimpMessagingTemplate messaging;
    private final String agentType;
    private final Long projectId;

    private String topic() {
        return "/topic/agent/" + agentType + "/" + projectId + "/event";
    }

    public void stream(String text) {
        send("stream", text);
    }

    public void responseEnd(String text) {
        send("response_end", text);
    }

    public void subAgentStream(String agent, String text) {
        send("subAgentStream", Map.of("agent", agent, "text", text));
    }

    public void subAgentEnd(String agent) {
        send("subAgentEnd", Map.of("agent", agent));
    }

    public void toolCall(String agent, String name, Object args) {
        send("toolCall", Map.of("agent", agent, "name", name, "args", args));
    }

    public void transfer(String to) {
        send("transfer", Map.of("to", to));
    }

    public void refresh(String type) {
        send("refresh", type);
    }

    public void error(String message) {
        send("error", message);
    }

    // ---- 小说生成专用事件 ----

    public void layerStart(int layer, String name) {
        send("layerStart", Map.of("layer", layer, "name", name));
    }

    public void layerComplete(int layer, String name) {
        send("layerComplete", Map.of("layer", layer, "name", name));
    }

    public void chapterStart(int volumeIndex, int chapterIndex, String title) {
        send("chapterStart", Map.of("volumeIndex", volumeIndex, "chapterIndex", chapterIndex, "title", title));
    }

    public void chapterDelta(String text) {
        send("chapterDelta", text);
    }

    public void chapterEnd(int chapterIndex, int wordCount) {
        send("chapterEnd", Map.of("chapterIndex", chapterIndex, "wordCount", wordCount));
    }

    public void progress(int totalChapters, int completedChapters, int currentChapter, String currentLayer) {
        send("progress", Map.of("totalChapters", totalChapters, "completedChapters", completedChapters,
                "currentChapter", currentChapter, "currentLayer", currentLayer));
    }

    private void send(String type, Object data) {
        messaging.convertAndSend(topic(), Map.of("type", type, "data", data));
    }
}
