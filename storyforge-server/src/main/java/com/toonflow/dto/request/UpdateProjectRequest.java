package com.toonflow.dto.request;

import lombok.Data;

@Data
public class UpdateProjectRequest {
    private String name;
    private String intro;
    private String type;
    private String artStyle;
    private String videoRatio;
}
