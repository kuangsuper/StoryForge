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

public class VolcengineTtsProvider implements TtsAiProvider {

    private static final String DEFAULT_BASE_URL = "https://openspeech.bytedance.com/api/v1/tts";

    private final Config config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VolcengineTtsProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] synthesize(TtsRequest request) {
        String url = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
                ? config.getBaseUrl() : DEFAULT_BASE_URL;
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode app = body.putObject("app");
        app.put("appid", config.getModel());
        ObjectNode user = body.putObject("user");
        user.put("uid", "storyforge");
        ObjectNode audio = body.putObject("audio");
        audio.put("voice_type", request.getVoiceId() != null ? request.getVoiceId() : "zh_female_cancan");
        audio.put("encoding", "wav".equals(request.getOutputFormat()) ? "wav" : "mp3");
        if (request.getSpeed() != null) audio.put("speed_ratio", request.getSpeed());
        ObjectNode reqNode = body.putObject("request");
        reqNode.put("text", request.getText());
        reqNode.put("operation", "query");

        try {
            String json = objectMapper.writeValueAsString(body);
            RequestBody reqBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request httpReq = new Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer; " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(httpReq).execute()) {
                if (!resp.isSuccessful()) {
                    throw new BizException(ErrorCode.AI_CALL_FAILED, "Volcengine TTS HTTP " + resp.code());
                }
                return resp.body() != null ? resp.body().bytes() : new byte[0];
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "Volcengine TTS error: " + e.getMessage()); }
    }
}
