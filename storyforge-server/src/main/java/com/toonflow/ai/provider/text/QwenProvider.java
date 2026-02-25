package com.toonflow.ai.provider.text;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.ai.registry.TextModelRegistry;
import com.toonflow.entity.Config;
import okhttp3.OkHttpClient;

public class QwenProvider extends AbstractOpenAiCompatibleProvider {

    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    public QwenProvider(Config config, OkHttpClient httpClient,
                        ObjectMapper objectMapper, TextModelRegistry registry) {
        super(config, httpClient, objectMapper, registry);
    }

    @Override
    protected String getBaseUrl() {
        String url = config.getBaseUrl();
        return (url != null && !url.isBlank()) ? url : DEFAULT_BASE_URL;
    }
}
