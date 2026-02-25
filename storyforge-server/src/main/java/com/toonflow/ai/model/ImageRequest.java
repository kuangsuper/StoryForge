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
public class ImageRequest {
    private String prompt;
    private List<String> referenceImageUrls;
    private String mode;    // "t2i" | "ti2i" | "i2i"
    private Integer width;
    private Integer height;
    private Map<String, Object> vendorParams;
}
