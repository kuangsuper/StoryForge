package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_outline")
public class NovelOutline {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String mainPlot;
    private String theme;
    private Integer volumeIndex;
    private String volumeName;
    private String volumePlot;
    private Integer startChapter;
    private Integer endChapter;
    private String volumeClimax;
    private String volumeCliffhanger;
    private Integer state;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
