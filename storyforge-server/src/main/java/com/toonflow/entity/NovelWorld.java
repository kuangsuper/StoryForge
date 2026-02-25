package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_world")
public class NovelWorld {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String background;
    private String powerSystem;
    private String socialStructure;
    private String coreRules;
    private String taboos;
    private Integer state;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
