package com.toonflow.ai.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.ai.provider.ImageAiProvider;
import com.toonflow.ai.provider.image.*;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import com.toonflow.util.AesUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageAiProviderFactory {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ImageAiProvider create(Config config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "API Key 不能为空");
        }
        Config decrypted = copyWithDecryptedKey(config);

        return switch (config.getManufacturer()) {
            case "gemini"      -> new GeminiImageProvider(decrypted, httpClient, objectMapper);
            case "volcengine"  -> new VolcengineImageProvider(decrypted, httpClient, objectMapper);
            case "kling"       -> new KlingImageProvider(decrypted, httpClient, objectMapper);
            case "vidu"        -> new ViduImageProvider(decrypted, httpClient, objectMapper);
            case "runninghub"  -> new RunningHubImageProvider(decrypted, httpClient, objectMapper);
            default -> throw new BizException(ErrorCode.AI_CALL_FAILED,
                    "Unsupported image manufacturer: " + config.getManufacturer());
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
