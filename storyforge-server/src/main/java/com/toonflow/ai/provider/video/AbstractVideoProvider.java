package com.toonflow.ai.provider.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.ai.provider.VideoAiProvider;
import com.toonflow.entity.Config;
import okhttp3.OkHttpClient;

public abstract class AbstractVideoProvider implements VideoAiProvider {

    protected final Config config;
    protected final OkHttpClient httpClient;
    protected final ObjectMapper objectMapper;

    protected AbstractVideoProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    protected String getBaseUrl(String defaultUrl) {
        String url = config.getBaseUrl();
        return (url != null && !url.isBlank()) ? url : defaultUrl;
    }
}
