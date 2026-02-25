package com.toonflow.ai.provider.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.toonflow.ai.model.ImageRequest;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

@Slf4j
public class VolcengineImageProvider extends AbstractAsyncImageProvider {

    private static final String DEFAULT_BASE_URL = "https://visual.volcengineapi.com";

    public VolcengineImageProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        super(config, httpClient, objectMapper);
    }

    private String getBaseUrl() {
        String url = config.getBaseUrl();
        return (url != null && !url.isBlank()) ? url : DEFAULT_BASE_URL;
    }

    @Override
    protected String createTask(ImageRequest request) {
        String url = getBaseUrl() + "/v1/image/generate";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModel());
        body.put("prompt", request.getPrompt());
        if (request.getWidth() != null) body.put("width", request.getWidth());
        if (request.getHeight() != null) body.put("height", request.getHeight());
        if (request.getReferenceImageUrls() != null && !request.getReferenceImageUrls().isEmpty()) {
            body.put("image_url", request.getReferenceImageUrls().get(0));
        }
        return doPostAndGetTaskId(url, body);
    }

    @Override
    protected String pollTask(String taskId) {
        String url = getBaseUrl() + "/v1/image/task/" + taskId;
        try {
            Request req = new Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                JsonNode root = objectMapper.readTree(respBody);
                String status = root.path("status").asText("");
                if ("completed".equals(status) || "succeeded".equals(status)) {
                    return root.path("data").path("image_url").asText(null);
                }
                if ("failed".equals(status)) {
                    throw new BizException(ErrorCode.AI_CALL_FAILED,
                            "Volcengine image task failed: " + root.path("message").asText(""));
                }
                return null;
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Volcengine poll error: " + e.getMessage());
        }
    }

    private String doPostAndGetTaskId(String url, ObjectNode body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request req = new Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    throw new BizException(ErrorCode.AI_CALL_FAILED, "Volcengine image HTTP " + resp.code());
                }
                JsonNode root = objectMapper.readTree(respBody);
                return root.path("data").path("task_id").asText();
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Volcengine image error: " + e.getMessage());
        }
    }
}
