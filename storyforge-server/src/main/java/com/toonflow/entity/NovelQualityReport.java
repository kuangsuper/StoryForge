package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_quality_report")
public class NovelQualityReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String scope;
    private Integer scopeIndex;
    private Integer overallScore;
    private String dimensions;
    private String summary;
    private String autoFixSuggestions;
    private String state;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
