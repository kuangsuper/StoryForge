package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_config")
public class Config {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String name;
    private String manufacturer;
    private String model;
    private String apiKey;
    private String baseUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
