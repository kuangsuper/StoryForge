package com.toonflow.ai.provider;

import com.toonflow.ai.model.TtsRequest;

public interface TtsAiProvider {

    /** Synthesize text to speech, returns audio data */
    byte[] synthesize(TtsRequest request);
}
