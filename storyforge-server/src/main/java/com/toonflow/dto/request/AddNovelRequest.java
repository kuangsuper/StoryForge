package com.toonflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddNovelRequest {
    @NotEmpty(message = "章节列表不能为空")
    @Valid
    private List<NovelChapterItem> chapters;

    @Data
    public static class NovelChapterItem {
        private Integer chapterIndex;
        private String reel;
        private String chapter;
        private String chapterData;
    }
}
