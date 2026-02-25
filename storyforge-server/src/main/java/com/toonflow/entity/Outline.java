package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.toonflow.entity.json.EpisodeData;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "t_outline", autoResultMap = true)
public class Outline {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Integer episode;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private EpisodeData data;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
