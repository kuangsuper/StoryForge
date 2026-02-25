package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String intro;
    private String type;
    private String artStyle;
    private String videoRatio;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
