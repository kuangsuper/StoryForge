package com.toonflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProjectRequest {
    @NotBlank(message = "项目名称不能为空")
    private String name;
    private String intro;
    private String type;
    private String artStyle;
    private String videoRatio;
}
