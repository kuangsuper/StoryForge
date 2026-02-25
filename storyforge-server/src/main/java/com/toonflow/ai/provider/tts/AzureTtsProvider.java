package com.toonflow.ai.provider.tts;

import com.toonflow.ai.model.TtsRequest;
import com.toonflow.ai.provider.TtsAiProvider;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import okhttp3.*;

import java.io.IOException;

public class AzureTtsProvider implements TtsAiProvider {

    private final Config config;
    private final OkHttpClient httpClient;

    public AzureTtsProvider(Config config, OkHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public byte[] synthesize(TtsRequest request) {
        // Azure TTS uses SSML format
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
                ? config.getBaseUrl()
                : "https://" + config.getModel() + ".tts.speech.microsoft.com";
        String url = baseUrl + "/cognitiveservices/v1";

        String voiceName = request.getVoiceId() != null ? request.getVoiceId() : "zh-CN-XiaoxiaoNeural";
        String ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='zh-CN'>"
                + "<voice name='" + voiceName + "'>"
                + escapeXml(request.getText())
                + "</voice></speak>";

        String outputFormat = "wav".equals(request.getOutputFormat())
                ? "riff-24khz-16bit-mono-pcm" : "audio-24khz-96kbitrate-mono-mp3";

        try {
            RequestBody reqBody = RequestBody.create(ssml, MediaType.parse("application/ssml+xml"));
            Request httpReq = new Request.Builder().url(url)
                    .addHeader("Ocp-Apim-Subscription-Key", config.getApiKey())
                    .addHeader("Content-Type", "application/ssml+xml")
                    .addHeader("X-Microsoft-OutputFormat", outputFormat)
                    .post(reqBody).build();
            try (Response resp = httpClient.newCall(httpReq).execute()) {
                if (!resp.isSuccessful()) {
                    throw new BizException(ErrorCode.AI_CALL_FAILED, "Azure TTS HTTP " + resp.code());
                }
                return resp.body() != null ? resp.body().bytes() : new byte[0];
            }
        } catch (BizException e) { throw e; }
        catch (IOException e) { throw new BizException(ErrorCode.AI_CALL_FAILED, "Azure TTS error: " + e.getMessage()); }
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("'", "&apos;").replace("\"", "&quot;");
    }
}
