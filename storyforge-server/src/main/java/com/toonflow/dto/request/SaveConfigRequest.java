package com.toonflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveConfigRequest {
    @NotBlank(message = "类型不能为空")
    private String type;
    private String name;
    @NotBlank(message = "厂商不能为空")
    private String manufacturer;
    @NotBlank(message = "模型不能为空")
    private String model;
    private String apiKey;
    private String baseUrl;
}
