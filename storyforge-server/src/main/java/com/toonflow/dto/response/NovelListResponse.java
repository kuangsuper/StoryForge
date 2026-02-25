package com.toonflow.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NovelListResponse {
    private Long id;
    private Integer chapterIndex;
    private Integer volumeIndex;
    private String reel;
    private String chapter;
    private LocalDateTime createTime;
}
