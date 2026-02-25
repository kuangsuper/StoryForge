package com.toonflow.ai.provider.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.toonflow.ai.model.VideoRequest;
import com.toonflow.ai.model.VideoTaskResult;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import okhttp3.*;

import java.io.IOException;

public class VolcengineVideoProvider extends AbstractVideoProvider {

    private static final String DEFAULT_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    public VolcengineVideoProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        super(config, httpClient, objectMapper);
    }

    @Override
    public String createTask(VideoRequest request) {
        String url = getBaseUrl(DEFAULT_BASE_URL) + "/video/generations";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModel());
        body.put("prompt", request.getPrompt());
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            body.put("image_url", request.getImageUrls().get(0));
        }
        if (request.getDuration() != null) body.put("duration", request.getDuration());
        return doPostAndGetTaskId(url, body);
    }

    @Override
    public VideoTaskResult pollTask(String taskId) {
        String url = getBaseUrl(DEFAULT_BASE_URL) + "/video/generations/" + taskId;
        return doPollGet(url, taskId);
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
                if (!resp.isSuccessful()) throw new BizException(ErrorCode.AI_CALL_FAILED, "Volcengine video HTTP " + resp.code());
                JsonNode root = objectMapper.readTree(respBody);
                return root.path("data").path("task_id").asText();
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "Volcengine video error: " + e.getMessage()); }
    }

    private VideoTaskResult doPollGet(String url, String taskId) {
        try {
            Request req = new Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                JsonNode root = objectMapper.readTree(respBody);
                String status = root.path("data").path("task_status").asText("pending");
                return VideoTaskResult.builder()
                        .taskId(taskId)
                        .state(mapStatus(status))
                        .videoUrl(root.path("data").path("video_url").asText(null))
                        .errorMessage(root.path("data").path("error_message").asText(null))
                        .build();
            }
        } catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Volcengine video poll error: " + e.getMessage());
        }
    }

    private String mapStatus(String s) {
        return switch (s) {
            case "succeed", "completed" -> "completed";
            case "failed" -> "failed";
            case "processing", "running" -> "processing";
            default -> "pending";
        };
    }
}
