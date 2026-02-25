package com.toonflow.ai.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.ai.provider.TtsAiProvider;
import com.toonflow.ai.provider.tts.*;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import com.toonflow.util.AesUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TtsAiProviderFactory {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TtsAiProvider create(Config config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "API Key 不能为空");
        }
        Config decrypted = copyWithDecryptedKey(config);

        return switch (config.getManufacturer()) {
            case "volcengine"  -> new VolcengineTtsProvider(decrypted, httpClient, objectMapper);
            case "aliyun"      -> new AliyunTtsProvider(decrypted, httpClient, objectMapper);
            case "azure"       -> new AzureTtsProvider(decrypted, httpClient);
            case "fishaudio"   -> new FishAudioTtsProvider(decrypted, httpClient, objectMapper);
            default -> throw new BizException(ErrorCode.AI_CALL_FAILED,
                    "Unsupported TTS manufacturer: " + config.getManufacturer());
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
