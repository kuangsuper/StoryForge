package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_version")
public class NovelVersion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long novelId;
    private Long projectId;
    private Integer chapterIndex;
    private String chapterData;
    private String summary;
    private String source;
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
