package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_chapter_plan")
public class NovelChapterPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Integer volumeIndex;
    private Integer chapterIndex;
    private String title;
    private String summary;
    private String keyEvents;
    private String characters;
    private String emotionCurve;
    private String foreshadowing;
    private String payoff;
    private String cliffhanger;
    private Integer wordTarget;
    private Integer state;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
