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
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractOpenAiCompatibleProvider implements TextAiProvider {

    protected final Config config;
    protected final OkHttpClient httpClient;
    protected final ObjectMapper objectMapper;
    protected final TextModelRegistry registry;

    protected AbstractOpenAiCompatibleProvider(Config config, OkHttpClient httpClient,
                                                ObjectMapper objectMapper, TextModelRegistry registry) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    protected String getBaseUrl() {
        return config.getBaseUrl();
    }

    protected String getChatCompletionsUrl() {
        String base = getBaseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/chat/completions";
    }

    @Override
    public String invoke(AiRequest request) {
        ObjectNode body = buildRequestBody(request, false);
        String responseBody = doPost(getChatCompletionsUrl(), body);
        return extractContent(responseBody);
    }

    @Override
    public <T> T invoke(AiRequest request, Class<T> responseType) {
        String text = invoke(request);
        // Extract JSON from response (may be wrapped in markdown code block)
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED,
                    "Failed to parse AI response as " + responseType.getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public Flux<String> stream(AiRequest request) {
        ObjectNode body = buildRequestBody(request, true);
        return Flux.create(sink -> {
            try {
                doStreamPost(getChatCompletionsUrl(), body, sink);
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @Override
    public AiResponse invokeWithTools(AiRequest request) {
        ObjectNode body = buildRequestBody(request, false);
        String responseBody = doPost(getChatCompletionsUrl(), body);
        return extractAiResponse(responseBody);
    }

    // ---- Request body building ----

    public ObjectNode buildRequestBody(AiRequest request, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModel());
        body.put("stream", stream);

        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
        if (request.getMaxTokens() != null) body.put("max_tokens", request.getMaxTokens());

        // Build messages array
        ArrayNode messages = body.putArray("messages");

        // System prompt
        String systemPrompt = request.getSystemPrompt();
        var capability = registry.getCapability(config.getModel());

        // Handle structured output: if model doesn't support native schema, inject into system prompt
        if ("json_schema".equals(request.getResponseFormat()) && request.getJsonSchema() != null) {
            if ("schema".equals(capability.getResponseFormat())) {
                // Native structured output support
                ObjectNode responseFormat = objectMapper.createObjectNode();
                responseFormat.put("type", "json_schema");
                ObjectNode jsonSchema = objectMapper.createObjectNode();
                jsonSchema.put("name", "response");
                jsonSchema.put("strict", true);
                try {
                    jsonSchema.set("schema", objectMapper.readTree(request.getJsonSchema()));
                } catch (JsonProcessingException e) {
                    jsonSchema.put("schema", request.getJsonSchema());
                }
                responseFormat.set("json_schema", jsonSchema);
                body.set("response_format", responseFormat);
            } else if ("object".equals(capability.getResponseFormat())) {
                // JSON object mode + schema in prompt
                ObjectNode responseFormat = objectMapper.createObjectNode();
                responseFormat.put("type", "json_object");
                body.set("response_format", responseFormat);
                systemPrompt = appendSchemaToPrompt(systemPrompt, request.getJsonSchema());
            } else {
                // No JSON support, inject schema into prompt
                systemPrompt = appendSchemaToPrompt(systemPrompt, request.getJsonSchema());
            }
        }

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }

        // User/assistant messages
        if (request.getMessages() != null) {
            for (ChatMessage msg : request.getMessages()) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.getRole());

                if ("tool".equals(msg.getRole()) && msg.getToolCallId() != null) {
                    msgNode.put("tool_call_id", msg.getToolCallId());
                    msgNode.put("content", msg.getContent());
                } else if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()
                        && "user".equals(msg.getRole()) && capability.isImage()) {
                    // Multimodal: image + text content
                    ArrayNode contentArray = objectMapper.createArrayNode();
                    ObjectNode textPart = objectMapper.createObjectNode();
                    textPart.put("type", "text");
                    textPart.put("text", msg.getContent());
                    contentArray.add(textPart);
                    for (String imageUrl : request.getImageUrls()) {
                        ObjectNode imagePart = objectMapper.createObjectNode();
                        imagePart.put("type", "image_url");
                        ObjectNode urlObj = objectMapper.createObjectNode();
                        urlObj.put("url", imageUrl);
                        imagePart.set("image_url", urlObj);
                        contentArray.add(imagePart);
                    }
                    msgNode.set("content", contentArray);
                } else {
                    msgNode.put("content", msg.getContent());
                }

                // Tool calls in assistant message
                if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                    ArrayNode toolCallsNode = objectMapper.createArrayNode();
                    for (ToolCall tc : msg.getToolCalls()) {
                        ObjectNode tcNode = objectMapper.createObjectNode();
                        tcNode.put("id", tc.getId());
                        tcNode.put("type", "function");
                        ObjectNode fn = objectMapper.createObjectNode();
                        fn.put("name", tc.getName());
                        fn.put("arguments", tc.getArguments());
                        tcNode.set("function", fn);
                        toolCallsNode.add(tcNode);
                    }
                    msgNode.set("tool_calls", toolCallsNode);
                }

                messages.add(msgNode);
            }
        }

        // Tools (Function Calling)
        if (request.getTools() != null && !request.getTools().isEmpty() && capability.isTool()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (ToolDefinition tool : request.getTools()) {
                ObjectNode toolNode = objectMapper.createObjectNode();
                toolNode.put("type", "function");
                ObjectNode fn = objectMapper.createObjectNode();
                fn.put("name", tool.getName());
                fn.put("description", tool.getDescription());
                if (tool.getParameters() != null) {
                    fn.set("parameters", objectMapper.valueToTree(tool.getParameters()));
                }
                toolNode.set("function", fn);
                toolsArray.add(toolNode);
            }
        }

        if (stream) {
            ObjectNode streamOptions = objectMapper.createObjectNode();
            streamOptions.put("include_usage", true);
            body.set("stream_options", streamOptions);
        }

        return body;
    }

    // ---- HTTP execution ----

    protected String doPost(String url, ObjectNode body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    int code = response.code();
                    if (code == 429) throw new BizException(429, "Rate limited: " + responseBody);
                    if (code >= 500) throw new BizException(code, "Server error: " + responseBody);
                    throw new BizException(ErrorCode.AI_CALL_FAILED,
                            "HTTP " + code + ": " + responseBody);
                }
                return responseBody;
            }
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Network error: " + e.getMessage());
        } catch (Exception e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Request failed: " + e.getMessage());
        }
    }

    protected void doStreamPost(String url, ObjectNode body, FluxSink<String> sink) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .post(requestBody)
                    .build();

            Response response = httpClient.newCall(httpRequest).execute();
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                response.close();
                sink.error(new BizException(ErrorCode.AI_CALL_FAILED,
                        "Stream HTTP " + response.code() + ": " + errBody));
                return;
            }

            try (var reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            String content = extractStreamContent(node);
                            if (content != null && !content.isEmpty()) {
                                sink.next(content);
                            }
                        } catch (Exception e) {
                            // skip malformed SSE lines
                        }
                    }
                }
                sink.complete();
            }
        } catch (Exception e) {
            sink.error(e);
        }
    }

    // ---- Response parsing ----

    protected String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).path("message");
                return message.path("content").asText("");
            }
            return "";
        } catch (Exception e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Failed to parse response: " + e.getMessage());
        }
    }

    protected String extractStreamContent(JsonNode node) {
        JsonNode choices = node.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode delta = choices.get(0).path("delta");
            return delta.path("content").asText(null);
        }
        return null;
    }

    protected AiResponse extractAiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return AiResponse.builder().content("").toolCalls(List.of()).build();
            }
            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText(null);

            List<ToolCall> toolCalls = new ArrayList<>();
            JsonNode toolCallsNode = message.path("tool_calls");
            if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                for (JsonNode tcNode : toolCallsNode) {
                    String id = tcNode.path("id").asText();
                    JsonNode fn = tcNode.path("function");
                    String name = fn.path("name").asText();
                    String arguments = fn.path("arguments").asText("");
                    toolCalls.add(ToolCall.builder().id(id).name(name).arguments(arguments).build());
                }
            }
            return AiResponse.builder().content(content).toolCalls(toolCalls).build();
        } catch (Exception e) {
            log.warn("Failed to parse AiResponse, falling back to plain text: {}", e.getMessage());
            // Degrade: try to extract content only
            try {
                String content = extractContent(responseBody);
                return AiResponse.builder().content(content).toolCalls(List.of()).build();
            } catch (Exception ex) {
                return AiResponse.builder().content("").toolCalls(List.of()).build();
            }
        }
    }

    // ---- Helpers ----

    protected String appendSchemaToPrompt(String systemPrompt, String jsonSchema) {
        String prefix = systemPrompt != null ? systemPrompt + "\n\n" : "";
        return prefix + "You MUST respond with valid JSON matching this schema:\n" + jsonSchema;
    }

    protected String extractJson(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        // Remove markdown code block if present
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
