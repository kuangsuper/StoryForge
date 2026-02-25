package com.toonflow.dto.response;

import com.toonflow.service.pipeline.PipelineState;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PipelineStatus {
    private PipelineState currentState;
    private String currentStepName;
    private Integer totalSteps;
    private Integer completedSteps;
    private String detail;
    private LocalDateTime startTime;
}
