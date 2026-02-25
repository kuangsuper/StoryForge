package com.toonflow.dto.request;

import lombok.Data;

@Data
public class UpdateSettingRequest {
    private String tokenKey;
    private String imageModel;
    private String languageModel;
}
