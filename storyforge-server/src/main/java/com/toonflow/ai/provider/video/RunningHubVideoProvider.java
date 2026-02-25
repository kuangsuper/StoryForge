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

public class RunningHubVideoProvider extends AbstractVideoProvider {

    private static final String DEFAULT_BASE_URL = "https://api.runninghub.cn";

    public RunningHubVideoProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        super(config, httpClient, objectMapper);
    }

    @Override
    public String createTask(VideoRequest request) {
        String url = getBaseUrl(DEFAULT_BASE_URL) + "/task/openapi/create";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("apiKey", config.getApiKey());
        body.put("workflowId", config.getModel());
        ObjectNode nodeInfo = objectMapper.createObjectNode();
        nodeInfo.put("prompt", request.getPrompt());
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            nodeInfo.put("image", request.getImageUrls().get(0));
        }
        body.set("nodeInfoList", objectMapper.createArrayNode().add(nodeInfo));
        return doPostAndGetTaskId(url, body);
    }

    @Override
    public VideoTaskResult pollTask(String taskId) {
        String url = getBaseUrl(DEFAULT_BASE_URL) + "/task/openapi/status";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("apiKey", config.getApiKey());
        body.put("taskId", taskId);
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request req = new Request.Builder().url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                JsonNode root = objectMapper.readTree(respBody);
                String status = root.path("data").path("taskStatus").asText("PENDING");
                String videoUrl = null;
                JsonNode outputs = root.path("data").path("outputs");
                if (outputs.isArray() && !outputs.isEmpty()) {
                    videoUrl = outputs.get(0).path("fileUrl").asText(null);
                }
                return VideoTaskResult.builder()
                        .taskId(taskId)
                        .state("COMPLETED".equals(status) ? "completed" : "FAILED".equals(status) ? "failed" : "processing")
                        .videoUrl(videoUrl)
                        .build();
            }
        } catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "RunningHub video poll error: " + e.getMessage());
        }
    }

    private String doPostAndGetTaskId(String url, ObjectNode body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request req = new Request.Builder().url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) throw new BizException(ErrorCode.AI_CALL_FAILED, "RunningHub video HTTP " + resp.code());
                JsonNode root = objectMapper.readTree(respBody);
                return root.path("data").path("taskId").asText();
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "RunningHub video error: " + e.getMessage()); }
    }
}
