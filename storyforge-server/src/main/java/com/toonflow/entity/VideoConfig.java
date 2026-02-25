package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_video_config")
public class VideoConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long scriptId;
    private String manufacturer;
    private String mode;
    private String resolution;
    private BigDecimal duration;
    private Integer audioEnabled;
    private String stylePrefix;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
