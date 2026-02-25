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
public class RunningHubImageProvider extends AbstractAsyncImageProvider {

    private static final String DEFAULT_BASE_URL = "https://api.runninghub.cn";

    public RunningHubImageProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        super(config, httpClient, objectMapper);
    }

    private String getBaseUrl() {
        String url = config.getBaseUrl();
        return (url != null && !url.isBlank()) ? url : DEFAULT_BASE_URL;
    }

    @Override
    protected String createTask(ImageRequest request) {
        String url = getBaseUrl() + "/task/openapi/create";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("apiKey", config.getApiKey());
        body.put("workflowId", config.getModel());
        ObjectNode nodeInfos = objectMapper.createObjectNode();
        nodeInfos.put("prompt", request.getPrompt());
        if (request.getReferenceImageUrls() != null && !request.getReferenceImageUrls().isEmpty()) {
            nodeInfos.put("image", request.getReferenceImageUrls().get(0));
        }
        body.set("nodeInfoList", objectMapper.createArrayNode().add(nodeInfos));
        return doPostAndGetTaskId(url, body);
    }

    @Override
    protected String pollTask(String taskId) {
        String url = getBaseUrl() + "/task/openapi/status";
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
                String status = root.path("data").path("taskStatus").asText("");
                if ("COMPLETED".equals(status)) {
                    return root.path("data").path("outputs").get(0).path("fileUrl").asText(null);
                }
                if ("FAILED".equals(status)) {
                    throw new BizException(ErrorCode.AI_CALL_FAILED, "RunningHub image task failed");
                }
                return null;
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "RunningHub poll error: " + e.getMessage()); }
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
                if (!resp.isSuccessful()) throw new BizException(ErrorCode.AI_CALL_FAILED, "RunningHub image HTTP " + resp.code());
                JsonNode root = objectMapper.readTree(respBody);
                return root.path("data").path("taskId").asText();
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "RunningHub image error: " + e.getMessage()); }
    }
}
