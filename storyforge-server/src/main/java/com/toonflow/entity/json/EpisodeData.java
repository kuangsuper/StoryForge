package com.toonflow.entity.json;

import lombok.Data;
import java.util.List;

@Data
public class EpisodeData {
    private int episodeIndex;
    private String title;
    private List<Integer> chapterRange;
    private List<AssetItem> scenes;
    private List<AssetItem> characters;
    private List<AssetItem> props;
    private String coreConflict;
    private String outline;
    private String openingHook;
    private List<String> keyEvents;
    private String emotionalCurve;
    private List<String> visualHighlights;
    private String endingHook;
    private List<String> classicQuotes;
}
