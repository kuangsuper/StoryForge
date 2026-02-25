package com.toonflow.ai.provider.text;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.ai.registry.TextModelRegistry;
import com.toonflow.entity.Config;
import okhttp3.OkHttpClient;

/**
 * Generic OpenAI-compatible provider. Uses baseUrl from Config directly.
 */
public class OpenAiCompatibleProvider extends AbstractOpenAiCompatibleProvider {

    public OpenAiCompatibleProvider(Config config, OkHttpClient httpClient,
                                     ObjectMapper objectMapper, TextModelRegistry registry) {
        super(config, httpClient, objectMapper, registry);
    }
}
