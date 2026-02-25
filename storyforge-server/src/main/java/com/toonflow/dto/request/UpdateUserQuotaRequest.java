package com.toonflow.dto.request;

import lombok.Data;

@Data
public class UpdateUserQuotaRequest {
    private Integer dailyChapterLimit;
    private Integer dailyImageLimit;
    private Integer dailyVideoLimit;
}
