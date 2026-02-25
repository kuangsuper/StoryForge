package com.toonflow.ai.provider.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.toonflow.ai.model.VideoRequest;
import com.toonflow.ai.model.VideoTaskResult;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import okhttp3.*;

import java.io.IOException;

public class GeminiVideoProvider extends AbstractVideoProvider {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    public GeminiVideoProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        super(config, httpClient, objectMapper);
    }

    @Override
    public String createTask(VideoRequest request) {
        String base = getBaseUrl(DEFAULT_BASE_URL);
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String url = base + "/models/" + config.getModel() + ":predictLongRunning?key=" + config.getApiKey();

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode instances = body.putArray("instances");
        ObjectNode instance = objectMapper.createObjectNode();
        instance.put("prompt", request.getPrompt());
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            instance.put("image", request.getImageUrls().get(0));
        }
        instances.add(instance);

        ObjectNode parameters = body.putObject("parameters");
        if (request.getDuration() != null) parameters.put("sampleCount", 1);

        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request req = new Request.Builder().url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) throw new BizException(ErrorCode.AI_CALL_FAILED, "Gemini video HTTP " + resp.code());
                JsonNode root = objectMapper.readTree(respBody);
                return root.path("name").asText();
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "Gemini video error: " + e.getMessage()); }
    }

    @Override
    public VideoTaskResult pollTask(String taskId) {
        String base = getBaseUrl(DEFAULT_BASE_URL);
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String url = base + "/" + taskId + "?key=" + config.getApiKey();
        try {
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                JsonNode root = objectMapper.readTree(respBody);
                boolean done = root.path("done").asBoolean(false);
                if (done) {
                    JsonNode response = root.path("response");
                    String videoUrl = response.path("predictions").get(0).path("videoUrl").asText(null);
                    return VideoTaskResult.builder()
                            .taskId(taskId).state("completed").videoUrl(videoUrl).build();
                }
                return VideoTaskResult.builder().taskId(taskId).state("processing").build();
            }
        } catch (IOException e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "Gemini video poll error: " + e.getMessage());
        }
    }
}
