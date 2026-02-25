package com.toonflow.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoRequest {
    private String prompt;
    private List<String> imageUrls;
    private String mode; // "text","singleImage","startEndRequired","endFrameOptional","startFrameOptional","multiImage","reference"
    private Integer duration;
    private String resolution;
    private Boolean audioEnabled;
    private Map<String, Object> vendorParams;
}
