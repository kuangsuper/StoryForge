package com.toonflow.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserQuotaResponse {
    private Integer dailyChapterLimit;
    private Integer dailyImageLimit;
    private Integer dailyVideoLimit;
    private Integer usedChapters;
    private Integer usedImages;
    private Integer usedVideos;
    private LocalDate resetDate;
}
