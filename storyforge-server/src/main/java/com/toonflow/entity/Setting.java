package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_setting")
public class Setting {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String tokenKey;
    private String imageModel;
    private String languageModel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
