package com.toonflow.ai.provider;

import com.toonflow.ai.model.VideoRequest;
import com.toonflow.ai.model.VideoTaskResult;

public interface VideoAiProvider {

    /** Create async video generation task, returns taskId */
    String createTask(VideoRequest request);

    /** Poll task status */
    VideoTaskResult pollTask(String taskId);
}
