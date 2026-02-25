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
 * Gemini REST API provider.
 * Uses generateContent / streamGenerateContent endpoints.
 */
@Slf4j
public class GeminiProvider implements TextAiProvider {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final Config config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TextModelRegistry registry;

    public GeminiProvider(Config config, OkHttpClient httpClient,
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

    private String getGenerateUrl() {
        String base = getBaseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/models/" + config.getModel() + ":generateContent?key=" + config.getApiKey();
    }

    private String getStreamUrl() {
        String base = getBaseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/models/" + config.getModel() + ":streamGenerateContent?alt=sse&key=" + config.getApiKey();
    }

    @Override
    public String invoke(AiRequest request) {
        ObjectNode body = buildRequestBody(request);
        String responseBody = doPost(getGenerateUrl(), body);
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
                    "Failed to parse Gemini response as " + responseType.getSimpleName());
        }
    }

    @Override
    public Flux<String> stream(AiRequest request) {
        ObjectNode body = buildRequestBody(request);
        return Flux.create(sink -> {
            try {
                doStreamPost(getStreamUrl(), body, sink);
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @Override
    public AiResponse invokeWithTools(AiRequest request) {
        ObjectNode body = buildRequestBody(request);
        String responseBody = doPost(getGenerateUrl(), body);
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            StringBuilder textContent = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();

            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray()) {
                    for (JsonNode part : parts) {
                        if (part.has("text")) {
                            textContent.append(part.path("text").asText(""));
                        } else if (part.has("functionCall")) {
                            JsonNode fc = part.path("functionCall");
                            toolCalls.add(ToolCall.builder()
                                    .id("gemini-" + System.nanoTime())
                                    .name(fc.path("name").asText())
                                    .arguments(objectMapper.writeValueAsString(fc.path("args")))
                                    .build());
                        }
                    }
                }
            }
            return AiResponse.builder()
                    .content(textContent.toString())
                    .toolCalls(toolCalls)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Gemini AiResponse, falling back: {}", e.getMessage());
            return AiResponse.builder()
                    .content(extractContent(responseBody))
                    .toolCalls(List.of())
                    .build();
        }
    }

    private ObjectNode buildRequestBody(AiRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");

        // System instruction
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            String sysPrompt = request.getSystemPrompt();
            // Inject schema into system prompt if needed
            if ("json_schema".equals(request.getResponseFormat()) && request.getJsonSchema() != null) {
                sysPrompt += "\n\nYou MUST respond with valid JSON matching this schema:\n" + request.getJsonSchema();
            }
            ObjectNode sysInstruction = objectMapper.createObjectNode();
            ObjectNode sysParts = objectMapper.createObjectNode();
            sysParts.put("text", sysPrompt);
            sysInstruction.set("parts", objectMapper.createArrayNode().add(sysParts));
            sysInstruction.put("role", "user");
            body.set("systemInstruction", sysInstruction);
        }

        // Messages
        if (request.getMessages() != null) {
            var capability = registry.getCapability(config.getModel());
            for (ChatMessage msg : request.getMessages()) {
                ObjectNode content = objectMapper.createObjectNode();
                String role = "user".equals(msg.getRole()) ? "user" : "model";
                content.put("role", role);
                ArrayNode parts = content.putArray("parts");

                if (msg.getContent() != null) {
                    ObjectNode textPart = objectMapper.createObjectNode();
                    textPart.put("text", msg.getContent());
                    parts.add(textPart);
                }

                // Multimodal images
                if ("user".equals(msg.getRole()) && request.getImageUrls() != null
                        && !request.getImageUrls().isEmpty() && capability.isImage()) {
                    for (String imageUrl : request.getImageUrls()) {
                        ObjectNode imgPart = objectMapper.createObjectNode();
                        ObjectNode fileData = objectMapper.createObjectNode();
                        fileData.put("fileUri", imageUrl);
                        fileData.put("mimeType", "image/jpeg");
                        imgPart.set("fileData", fileData);
                        parts.add(imgPart);
                    }
                }

                contents.add(content);
            }
        }

        // Generation config
        ObjectNode genConfig = objectMapper.createObjectNode();
        if (request.getTemperature() != null) genConfig.put("temperature", request.getTemperature());
        if (request.getMaxTokens() != null) genConfig.put("maxOutputTokens", request.getMaxTokens());

        // Structured output via responseMimeType
        if ("json_schema".equals(request.getResponseFormat())) {
            genConfig.put("responseMimeType", "application/json");
            if (request.getJsonSchema() != null) {
                try {
                    genConfig.set("responseSchema", objectMapper.readTree(request.getJsonSchema()));
                } catch (JsonProcessingException ignored) {}
            }
        }
        body.set("generationConfig", genConfig);

        // Tools (Function Calling)
        var capability = registry.getCapability(config.getModel());
        if (request.getTools() != null && !request.getTools().isEmpty() && capability.isTool()) {
            ArrayNode tools = body.putArray("tools");
            ObjectNode toolObj = objectMapper.createObjectNode();
            ArrayNode funcDecls = toolObj.putArray("functionDeclarations");
            for (ToolDefinition tool : request.getTools()) {
                ObjectNode fn = objectMapper.createObjectNode();
                fn.put("name", tool.getName());
                fn.put("description", tool.getDescription());
                if (tool.getParameters() != null) {
                    fn.set("parameters", objectMapper.valueToTree(tool.getParameters()));
                }
                funcDecls.add(fn);
            }
            tools.add(toolObj);
        }

        return body;
    }

    private String doPost(String url, ObjectNode body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    int code = response.code();
                    if (code == 429) throw new BizException(429, "Rate limited: " + respBody);
                    throw new BizException(ErrorCode.AI_CALL_FAILED, "Gemini HTTP " + code + ": " + respBody);
                }
                return respBody;
            }
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Gemini network error: " + e.getMessage());
        }
    }

    private void doStreamPost(String url, ObjectNode body, FluxSink<String> sink) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
            Response response = httpClient.newCall(httpRequest).execute();
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                response.close();
                sink.error(new BizException(ErrorCode.AI_CALL_FAILED, "Gemini stream HTTP " + response.code() + ": " + errBody));
                return;
            }
            try (var reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            JsonNode candidates = node.path("candidates");
                            if (candidates.isArray() && !candidates.isEmpty()) {
                                String text = candidates.get(0).path("content").path("parts")
                                        .get(0).path("text").asText(null);
                                if (text != null) sink.next(text);
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
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText("");
                }
            }
            return "";
        } catch (Exception e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Failed to parse Gemini response");
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
