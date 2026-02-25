package com.toonflow.ai.provider.tts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.toonflow.ai.model.TtsRequest;
import com.toonflow.ai.provider.TtsAiProvider;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import okhttp3.*;

import java.io.IOException;

public class AliyunTtsProvider implements TtsAiProvider {

    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2audio/generation";

    private final Config config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AliyunTtsProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] synthesize(TtsRequest request) {
        String url = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
                ? config.getBaseUrl() : DEFAULT_BASE_URL;
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModel());
        ObjectNode input = body.putObject("input");
        input.put("text", request.getText());
        ObjectNode parameters = body.putObject("parameters");
        if (request.getVoiceId() != null) parameters.put("voice", request.getVoiceId());
        if (request.getSpeed() != null) parameters.put("rate", request.getSpeed());
        parameters.put("format", request.getOutputFormat() != null ? request.getOutputFormat() : "mp3");

        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpReq = new Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(httpReq).execute()) {
                if (!resp.isSuccessful()) {
                    throw new BizException(ErrorCode.AI_CALL_FAILED, "Aliyun TTS HTTP " + resp.code());
                }
                return resp.body() != null ? resp.body().bytes() : new byte[0];
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "Aliyun TTS error: " + e.getMessage()); }
    }
}
