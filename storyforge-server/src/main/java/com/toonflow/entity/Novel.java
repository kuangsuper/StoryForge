package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel")
public class Novel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Integer chapterIndex;
    private Integer volumeIndex;
    private String reel;
    private String chapter;
    private String chapterData;
    private String summary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
