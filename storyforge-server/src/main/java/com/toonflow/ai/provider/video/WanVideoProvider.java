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

/**
 * Wan (通义万相) video provider via DashScope API.
 */
public class WanVideoProvider extends AbstractVideoProvider {

    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/api/v1";

    public WanVideoProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        super(config, httpClient, objectMapper);
    }

    @Override
    public String createTask(VideoRequest request) {
        String url = getBaseUrl(DEFAULT_BASE_URL) + "/services/aigc/video-generation/generation";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModel());
        ObjectNode input = body.putObject("input");
        input.put("prompt", request.getPrompt());
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            input.put("img_url", request.getImageUrls().get(0));
        }
        return doPostAndGetTaskId(url, body);
    }

    @Override
    public VideoTaskResult pollTask(String taskId) {
        String url = getBaseUrl(DEFAULT_BASE_URL) + "/tasks/" + taskId;
        return doPollGet(url, taskId);
    }

    private String doPostAndGetTaskId(String url, ObjectNode body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request req = new Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-DashScope-Async", "enable")
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) throw new BizException(ErrorCode.AI_CALL_FAILED, "Wan video HTTP " + resp.code());
                JsonNode root = objectMapper.readTree(respBody);
                return root.path("output").path("task_id").asText();
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "Wan video error: " + e.getMessage()); }
    }

    private VideoTaskResult doPollGet(String url, String taskId) {
        try {
            Request req = new Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                JsonNode root = objectMapper.readTree(respBody);
                String status = root.path("output").path("task_status").asText("PENDING");
                String videoUrl = null;
                JsonNode results = root.path("output").path("results");
                if (results.isArray() && !results.isEmpty()) {
                    videoUrl = results.get(0).path("url").asText(null);
                }
                return VideoTaskResult.builder()
                        .taskId(taskId).state(mapStatus(status))
                        .videoUrl(videoUrl)
                        .build();
            }
        } catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Wan video poll error: " + e.getMessage());
        }
    }

    private String mapStatus(String s) {
        return switch (s) { case "SUCCEEDED" -> "completed"; case "FAILED" -> "failed"; case "RUNNING" -> "processing"; default -> "pending"; };
    }
}
