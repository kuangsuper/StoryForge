package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_video_compose")
public class VideoCompose {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long scriptId;
    private Long configId;
    private String videoIds;
    private String filePath;
    private BigDecimal duration;
    private Integer state;
    private Integer retryCount;
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
