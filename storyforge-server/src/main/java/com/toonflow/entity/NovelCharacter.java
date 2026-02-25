package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_character")
public class NovelCharacter {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String role;
    private Integer age;
    private String appearance;
    private String personality;
    private String ability;
    private String relationships;
    private String growthArc;
    private String currentState;
    private String speechStyle;
    private String voiceId;
    private Integer state;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
