package com.toonflow.ai.provider.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.ai.model.ImageRequest;
import com.toonflow.ai.provider.ImageAiProvider;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Config;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

/**
 * Base class for async image providers that use createTask + poll pattern.
 */
@Slf4j
public abstract class AbstractAsyncImageProvider implements ImageAiProvider {

    protected static final int MAX_POLL_ATTEMPTS = 60;
    protected static final long POLL_INTERVAL_MS = 3000;

    protected final Config config;
    protected final OkHttpClient httpClient;
    protected final ObjectMapper objectMapper;

    protected AbstractAsyncImageProvider(Config config, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generate(ImageRequest request) {
        String taskId = createTask(request);
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BizException(ErrorCode.AI_CALL_FAILED, "Image generation interrupted");
            }
            String result = pollTask(taskId);
            if (result != null) return result;
        }
        throw new BizException(ErrorCode.AI_TIMEOUT, "Image generation timed out for task: " + taskId);
    }

    protected abstract String createTask(ImageRequest request);

    /** Returns image URL if completed, null if still processing */
    protected abstract String pollTask(String taskId);
}
