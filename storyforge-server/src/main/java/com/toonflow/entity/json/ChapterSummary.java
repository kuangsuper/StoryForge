package com.toonflow.entity.json;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChapterSummary {
    private String plot;
    private List<CharacterChange> characterChanges;
    private List<Foreshadowing> newForeshadowing;
    private List<ForeshadowingResolution> resolvedForeshadowing;
    private List<String> worldStateChanges;
    private Map<String, String> locationEnd;
    private String timelineMarker;

    @Data
    public static class CharacterChange {
        private String name;
        private String change;
    }

    @Data
    public static class Foreshadowing {
        private String id;
        private String content;
        private int plantedAt;
    }

    @Data
    public static class ForeshadowingResolution {
        private String id;
        private int resolvedAt;
        private String resolution;
    }
}
