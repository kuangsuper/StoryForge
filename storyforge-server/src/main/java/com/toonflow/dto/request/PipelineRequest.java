package com.toonflow.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class PipelineRequest {
    private Long projectId;
    private String genre;
    private String prompt;
    private Integer episodeCount = 5;
    private Integer chaptersPerEpisode = 2;
    private String videoMode = "singleImage";
    private Boolean autoCompose = true;
    private Boolean ttsEnabled = false;
    private Map<String, String> reviewConfig; // step → mode (skip/ai_auto/human_required)

    // Phase 9: 跳过控制
    private Boolean skipNovel = false;
    private Boolean skipStoryline = false;
    private Boolean skipOutline = false;
    private Boolean skipScript = false;
    private Boolean skipStoryboard = false;

    // Phase 9: 批量并发度（默认3）
    private Integer batchConcurrency = 3;
}
