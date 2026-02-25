package com.toonflow.ai.provider.text;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.toonflow.ai.model.*;
import com.toonflow.ai.provider.TextAiProvider;
import com.toonflow.ai.registry.TextModelRegistry;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic Messages API provider.
 */
@Slf4j
public class AnthropicProvider implements TextAiProvider {

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String API_VERSION = "2023-06-01";

    private final Config config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TextModelRegistry registry;

    public AnthropicProvider(Config config, OkHttpClient httpClient,
                             ObjectMapper objectMapper, TextModelRegistry registry) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    private String getBaseUrl() {
        String url = config.getBaseUrl();
        return (url != null && !url.isBlank()) ? url : DEFAULT_BASE_URL;
    }

    private String getMessagesUrl() {
        String base = getBaseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/v1/messages";
    }

    @Override
    public String invoke(AiRequest request) {
        ObjectNode body = buildRequestBody(request, false);
        String responseBody = doPost(getMessagesUrl(), body);
        return extractContent(responseBody);
    }

    @Override
    public <T> T invoke(AiRequest request, Class<T> responseType) {
        String text = invoke(request);
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED,
                    "Failed to parse Anthropic response as " + responseType.getSimpleName());
        }
    }

    @Override
    public Flux<String> stream(AiRequest request) {
        ObjectNode body = buildRequestBody(request, true);
        return Flux.create(sink -> {
            try {
                doStreamPost(getMessagesUrl(), body, sink);
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @Override
    public AiResponse invokeWithTools(AiRequest request) {
        ObjectNode body = buildRequestBody(request, false);
        String responseBody = doPost(getMessagesUrl(), body);
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentArr = root.path("content");
            StringBuilder textContent = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();

            if (contentArr.isArray()) {
                for (JsonNode block : contentArr) {
                    String type = block.path("type").asText("");
                    if ("text".equals(type)) {
                        textContent.append(block.path("text").asText(""));
                    } else if ("tool_use".equals(type)) {
                        toolCalls.add(ToolCall.builder()
                                .id(block.path("id").asText())
                                .name(block.path("name").asText())
                                .arguments(objectMapper.writeValueAsString(block.path("input")))
                                .build());
                    }
                }
            }
            return AiResponse.builder()
                    .content(textContent.toString())
                    .toolCalls(toolCalls)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Anthropic AiResponse, falling back: {}", e.getMessage());
            return AiResponse.builder()
                    .content(extractContent(responseBody))
                    .toolCalls(List.of())
                    .build();
        }
    }

    private ObjectNode buildRequestBody(AiRequest request, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModel());
        if (stream) body.put("stream", true);
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        } else {
            body.put("max_tokens", 4096);
        }
        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());

        // System prompt
        String systemPrompt = request.getSystemPrompt();
        if ("json_schema".equals(request.getResponseFormat()) && request.getJsonSchema() != null) {
            String schemaNote = "\n\nYou MUST respond with valid JSON matching this schema:\n" + request.getJsonSchema();
            systemPrompt = (systemPrompt != null ? systemPrompt : "") + schemaNote;
        }
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        // Messages
        ArrayNode messages = body.putArray("messages");
        var capability = registry.getCapability(config.getModel());
        if (request.getMessages() != null) {
            for (ChatMessage msg : request.getMessages()) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", "assistant".equals(msg.getRole()) ? "assistant" : "user");

                // Multimodal images
                if ("user".equals(msg.getRole()) && request.getImageUrls() != null
                        && !request.getImageUrls().isEmpty() && capability.isImage()) {
                    ArrayNode contentArr = objectMapper.createArrayNode();
                    if (msg.getContent() != null) {
                        ObjectNode textBlock = objectMapper.createObjectNode();
                        textBlock.put("type", "text");
                        textBlock.put("text", msg.getContent());
                        contentArr.add(textBlock);
                    }
                    for (String imageUrl : request.getImageUrls()) {
                        ObjectNode imgBlock = objectMapper.createObjectNode();
                        imgBlock.put("type", "image");
                        ObjectNode source = objectMapper.createObjectNode();
                        source.put("type", "url");
                        source.put("url", imageUrl);
                        imgBlock.set("source", source);
                        contentArr.add(imgBlock);
                    }
                    msgNode.set("content", contentArr);
                } else {
                    msgNode.put("content", msg.getContent() != null ? msg.getContent() : "");
                }
                messages.add(msgNode);
            }
        }

        // Tools
        if (request.getTools() != null && !request.getTools().isEmpty() && capability.isTool()) {
            ArrayNode tools = body.putArray("tools");
            for (ToolDefinition tool : request.getTools()) {
                ObjectNode toolNode = objectMapper.createObjectNode();
                toolNode.put("name", tool.getName());
                toolNode.put("description", tool.getDescription());
                if (tool.getParameters() != null) {
                    toolNode.set("input_schema", objectMapper.valueToTree(tool.getParameters()));
                }
                tools.add(toolNode);
            }
        }

        return body;
    }

    private String doPost(String url, ObjectNode body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("x-api-key", config.getApiKey())
                    .addHeader("anthropic-version", API_VERSION)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    int code = response.code();
                    if (code == 429) throw new BizException(429, "Rate limited: " + respBody);
                    throw new BizException(ErrorCode.AI_CALL_FAILED, "Anthropic HTTP " + code + ": " + respBody);
                }
                return respBody;
            }
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Anthropic network error: " + e.getMessage());
        }
    }

    private void doStreamPost(String url, ObjectNode body, FluxSink<String> sink) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("x-api-key", config.getApiKey())
                    .addHeader("anthropic-version", API_VERSION)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
            Response response = httpClient.newCall(httpRequest).execute();
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                response.close();
                sink.error(new BizException(ErrorCode.AI_CALL_FAILED,
                        "Anthropic stream HTTP " + response.code() + ": " + errBody));
                return;
            }
            try (var reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            String type = node.path("type").asText("");
                            if ("content_block_delta".equals(type)) {
                                String text = node.path("delta").path("text").asText(null);
                                if (text != null) sink.next(text);
                            } else if ("message_stop".equals(type)) {
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                sink.complete();
            }
        } catch (Exception e) {
            sink.error(e);
        }
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("content");
            if (content.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText())) {
                        sb.append(block.path("text").asText(""));
                    }
                }
                return sb.toString();
            }
            return "";
        } catch (Exception e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Failed to parse Anthropic response");
        }
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
            int end = trimmed.lastIndexOf("```");
            if (end > 0) trimmed = trimmed.substring(0, end);
            return trimmed.trim();
        }
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
            int end = trimmed.lastIndexOf("```");
            if (end > 0) trimmed = trimmed.substring(0, end);
            return trimmed.trim();
        }
        return trimmed;
    }
}
