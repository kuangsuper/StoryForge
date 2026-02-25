package com.toonflow.dto.request;

import lombok.Data;

@Data
public class SaveStorylineRequest {
    private String name;
    private String content;
    private String novelIds;
}
