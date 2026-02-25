package com.toonflow.entity.json;

import lombok.Data;
import java.util.List;

@Data
public class QualityDimensions {
    private DimensionScore characterConsistency;
    private DimensionScore plotCoherence;
    private DimensionScore worldCompliance;
    private DimensionScore foreshadowIntegrity;
    private DimensionScore genreSatisfaction;
    private DimensionScore writingQuality;
    private DimensionScore readability;

    @Data
    public static class DimensionScore {
        private int score;
        private List<QualityIssue> issues;
    }

    @Data
    public static class QualityIssue {
        private String severity;
        private String location;
        private String description;
        private String suggestion;
    }
}
