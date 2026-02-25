package com.toonflow.dto.request;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class SaveModelMapRequest {
    @Valid
    private List<ModelMapItem> mappings;

    @Data
    public static class ModelMapItem {
        private String key;
        private String name;
        private Long configId;
    }
}
