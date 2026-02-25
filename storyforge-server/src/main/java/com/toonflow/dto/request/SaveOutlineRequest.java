package com.toonflow.dto.request;

import com.toonflow.entity.json.EpisodeData;
import lombok.Data;

@Data
public class SaveOutlineRequest {
    private Integer episode;
    private EpisodeData data;
}
