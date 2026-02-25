package com.toonflow.ai.retry;

import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
public class AiRetryTemplate {

    private static final int MAX_RETRIES = 3;
    private static final long[] BACKOFF_MS = {2000, 4000, 8000};

    public <T> T execute(Supplier<T> action, String operationName) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES && isRetryable(e)) {
                    long waitMs = BACKOFF_MS[attempt];
                    log.warn("AI call retry {}/{} for {}: {}, waiting {}ms",
                            attempt + 1, MAX_RETRIES, operationName, e.getMessage(), waitMs);
                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BizException(ErrorCode.AI_CALL_FAILED,
                                operationName + " interrupted");
                    }
                } else if (!isRetryable(e)) {
                    break; // non-retryable, fail immediately
                }
            }
        }
        throw new BizException(ErrorCode.AI_CALL_FAILED,
                operationName + " failed after retries: " +
                        (lastException != null ? lastException.getMessage() : "unknown"));
    }

    public Flux<String> executeStream(Supplier<Flux<String>> action, String operationName) {
        return Flux.defer(action)
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(8))
                        .filter(this::isRetryable)
                        .onRetryExhaustedThrow((spec, signal) ->
                                new BizException(ErrorCode.AI_CALL_FAILED,
                                        operationName + " stream failed after retries: " +
                                                signal.failure().getMessage())));
    }

    public boolean isRetryable(Throwable e) {
        if (e instanceof IOException || e instanceof SocketTimeoutException) return true;
        if (e instanceof BizException biz && biz.getCode() == 429) return true;
        if (e instanceof BizException biz && biz.getCode() >= 500 && biz.getCode() < 510) return true;
        return false;
    }
}
