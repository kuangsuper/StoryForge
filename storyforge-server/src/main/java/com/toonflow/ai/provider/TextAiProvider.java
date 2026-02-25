package com.toonflow.ai.provider;

import com.toonflow.ai.model.AiRequest;
import com.toonflow.ai.model.AiResponse;
import reactor.core.publisher.Flux;

public interface TextAiProvider {

    /** Invoke and return plain text */
    String invoke(AiRequest request);

    /** Invoke and return structured object (JSON deserialization) */
    <T> T invoke(AiRequest request, Class<T> responseType);

    /** Stream text chunks */
    Flux<String> stream(AiRequest request);

    /** Invoke with tool-calling support, return content + toolCalls */
    AiResponse invokeWithTools(AiRequest request);
}
