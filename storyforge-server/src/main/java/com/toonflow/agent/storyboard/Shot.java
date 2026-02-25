package com.toonflow.agent.storyboard;

import lombok.Data;
import java.util.List;

@Data
public class Shot {
    private Long id;
    private Integer segmentId;
    private String title;
    private Integer x;
    private Integer y;
    private List<Cell> cells;
    private String fragmentContent;
    private String cameraMotion;
    private String videoPrompt;
    private List<AssetTag> assetsTags;
}
