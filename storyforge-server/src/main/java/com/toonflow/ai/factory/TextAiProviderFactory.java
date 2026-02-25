package com.toonflow.ai.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.ai.provider.TextAiProvider;
import com.toonflow.ai.provider.text.*;
import com.toonflow.ai.registry.TextModelRegistry;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import com.toonflow.util.AesUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TextAiProviderFactory {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TextModelRegistry registry;

    public TextAiProvider create(Config config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "API Key 不能为空");
        }
        Config decrypted = copyWithDecryptedKey(config);

        return switch (config.getManufacturer()) {
            case "deepseek"   -> new DeepSeekProvider(decrypted, httpClient, objectMapper, registry);
            case "qwen"       -> new QwenProvider(decrypted, httpClient, objectMapper, registry);
            case "doubao"     -> new DoubaoProvider(decrypted, httpClient, objectMapper, registry);
            case "zhipu"      -> new ZhipuProvider(decrypted, httpClient, objectMapper, registry);
            case "openai"     -> new OpenAiProvider(decrypted, httpClient, objectMapper, registry);
            case "gemini"     -> new GeminiProvider(decrypted, httpClient, objectMapper, registry);
            case "anthropic"  -> new AnthropicProvider(decrypted, httpClient, objectMapper, registry);
            case "xai"        -> new XaiProvider(decrypted, httpClient, objectMapper, registry);
            default           -> new OpenAiCompatibleProvider(decrypted, httpClient, objectMapper, registry);
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
