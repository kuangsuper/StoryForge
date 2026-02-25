package com.toonflow.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTaskResult {
    private String taskId;
    private String state; // "pending","processing","completed","failed"
    private String videoUrl;
    private String lastFrameUrl;
    private String errorMessage;
    private Integer duration;
}
