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

public class FishAudioTtsProvider implements TtsAiProvider {

    private static final String DEFAULT_BASE_URL = "https://api.fish.audio/v1/tts";

    private final Config config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FishAudioTtsProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] synthesize(TtsRequest request) {
        String url = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
                ? config.getBaseUrl() : DEFAULT_BASE_URL;
        ObjectNode body = objectMapper.createObjectNode();
        body.put("text", request.getText());
        body.put("reference_id", request.getVoiceId() != null ? request.getVoiceId() : config.getModel());
        if (request.getSpeed() != null) body.put("speed", request.getSpeed());
        body.put("format", request.getOutputFormat() != null ? request.getOutputFormat() : "mp3");

        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpReq = new Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(httpReq).execute()) {
                if (!resp.isSuccessful()) {
                    throw new BizException(ErrorCode.AI_CALL_FAILED, "FishAudio TTS HTTP " + resp.code());
                }
                return resp.body() != null ? resp.body().bytes() : new byte[0];
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "FishAudio TTS error: " + e.getMessage()); }
    }
}
