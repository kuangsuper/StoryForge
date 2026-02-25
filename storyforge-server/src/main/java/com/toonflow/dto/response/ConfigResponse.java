package com.toonflow.dto.response;

import lombok.Data;

@Data
public class ConfigResponse {
    private Long id;
    private String type;
    private String name;
    private String manufacturer;
    private String model;
    private String maskedApiKey;
    private String baseUrl;
}
