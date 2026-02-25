package com.toonflow.ai.provider;

import com.toonflow.ai.model.ImageRequest;

public interface ImageAiProvider {

    /** Generate image, returns base64 data or URL */
    String generate(ImageRequest request);
}
