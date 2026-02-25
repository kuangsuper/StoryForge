package com.toonflow.ai.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.ai.provider.VideoAiProvider;
import com.toonflow.ai.provider.video.*;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import com.toonflow.util.AesUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoAiProviderFactory {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VideoAiProvider create(Config config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "API Key 不能为空");
        }
        Config decrypted = copyWithDecryptedKey(config);

        return switch (config.getManufacturer()) {
            case "volcengine"  -> new VolcengineVideoProvider(decrypted, httpClient, objectMapper);
            case "kling"       -> new KlingVideoProvider(decrypted, httpClient, objectMapper);
            case "vidu"        -> new ViduVideoProvider(decrypted, httpClient, objectMapper);
            case "wan"         -> new WanVideoProvider(decrypted, httpClient, objectMapper);
            case "gemini"      -> new GeminiVideoProvider(decrypted, httpClient, objectMapper);
            case "runninghub"  -> new RunningHubVideoProvider(decrypted, httpClient, objectMapper);
            case "apimart"     -> new ApimartVideoProvider(decrypted, httpClient, objectMapper);
            default -> throw new BizException(ErrorCode.AI_CALL_FAILED,
                    "Unsupported video manufacturer: " + config.getManufacturer());
        };
    }

    private Config copyWithDecryptedKey(Config config) {
        Config copy = new Config();
        copy.setId(config.getId());
        copy.setType(config.getType());
        copy.setName(config.getName());
        copy.setManufacturer(config.getManufacturer());
        copy.setModel(config.getModel());
        copy.setApiKey(AesUtil.decrypt(config.getApiKey()));
        copy.setBaseUrl(config.getBaseUrl());
        return copy;
    }
}
