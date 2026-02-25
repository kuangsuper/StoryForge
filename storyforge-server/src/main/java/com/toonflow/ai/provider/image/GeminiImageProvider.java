package com.toonflow.ai.provider.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.toonflow.ai.model.ImageRequest;
import com.toonflow.ai.provider.ImageAiProvider;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

/**
 * Gemini image generation (synchronous).
 */
@Slf4j
public class GeminiImageProvider implements ImageAiProvider {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final Config config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiImageProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generate(ImageRequest request) {
        String base = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
                ? config.getBaseUrl() : DEFAULT_BASE_URL;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String url = base + "/models/" + config.getModel() + ":generateContent?key=" + config.getApiKey();

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        ArrayNode parts = content.putArray("parts");
        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("text", request.getPrompt());
        parts.add(textPart);
        contents.add(content);

        ObjectNode genConfig = objectMapper.createObjectNode();
        genConfig.put("responseMimeType", "image/png");
        body.set("generationConfig", genConfig);

        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpReq = new Request.Builder().url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(httpReq).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    throw new BizException(ErrorCode.AI_CALL_FAILED, "Gemini image HTTP " + resp.code());
                }
                JsonNode root = objectMapper.readTree(respBody);
                JsonNode inlineData = root.path("candidates").get(0)
                        .path("content").path("parts").get(0).path("inlineData");
                return "data:" + inlineData.path("mimeType").asText("image/png")
                        + ";base64," + inlineData.path("data").asText();
            }
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Gemini image network error: " + e.getMessage());
        }
    }
}
